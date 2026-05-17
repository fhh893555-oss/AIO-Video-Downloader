package userInterface.feedback;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nextgen.R;

import java.lang.ref.WeakReference;

import javax.annotation.Nullable;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.strings.StringHelper;

/**
 * ViewModel responsible for managing feedback data and the submission process.
 * <p>
 * This class handles the state for user-selected reactions, optional screenshots,
 * and the asynchronous submission of feedback to the server. It provides
 * {@link LiveData} streams to observe submission progress, success, and errors.
 * </p>
 */
public class FeedbackViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(FeedbackViewModel.class);
	private final MutableLiveData<String> selectedReaction;
	
	{
		String defaultReaction = FeedbackReactions.Excellent.name();
		selectedReaction = new MutableLiveData<>(defaultReaction);
	}
	
	private final MutableLiveData<DocumentFile> selectedScreenshot = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> submissionSuccess = new MutableLiveData<>();
	private final MutableLiveData<String> submissionError = new MutableLiveData<>();
	
	/**
	 * Gets the observable LiveData containing the currently selected feedback reaction.
	 *
	 * @return A {@link LiveData} object holding the string name of the selected reaction.
	 */
	public LiveData<String> getSelectedReaction() {
		return selectedReaction;
	}
	
	/**
	 * Sets the currently selected feedback reaction.
	 *
	 * @param reaction The name of the reaction to be selected.
	 */
	public void setSelectedReaction(String reaction) {
		selectedReaction.setValue(reaction);
	}
	
	/**
	 * Gets the observable LiveData containing the currently selected screenshot file.
	 *
	 * @return A {@link LiveData} object holding the {@link DocumentFile} of the screenshot,
	 * which may be null if no screenshot has been selected.
	 */
	public LiveData<DocumentFile> getSelectedScreenshot() {
		return selectedScreenshot;
	}
	
	/**
	 * Sets the selected screenshot file to be included with the feedback submission.
	 *
	 * @param file The {@link DocumentFile} representing the screenshot, or {@code null} if
	 *             no screenshot is selected.
	 */
	public void setSelectedScreenshot(@Nullable DocumentFile file) {
		selectedScreenshot.setValue(file);
	}
	
	/**
	 * Gets an observable LiveData that indicates whether the feedback submission process
	 * is currently in progress.
	 *
	 * @return A {@link LiveData} containing {@code true} if the submission is ongoing,
	 * {@code false} otherwise.
	 */
	public LiveData<Boolean> getIsSubmitting() {
		return isSubmitting;
	}
	
	/**
	 * Returns an observable {@link LiveData} indicating whether the feedback submission
	 * was successful.
	 *
	 * @return A {@link LiveData} containing {@code true} if the submission succeeded,
	 * {@code false} if it failed, or {@code null} if no attempt has been made yet.
	 */
	public LiveData<Boolean> getSubmissionSuccess() {
		return submissionSuccess;
	}
	
	/**
	 * Gets the LiveData containing the error message if the feedback submission process fails.
	 *
	 * @return A {@link LiveData} object holding the error message string.
	 */
	public LiveData<String> getSubmissionError() {
		return submissionError;
	}
	
	/**
	 * Sends the feedback data to the server asynchronously.
	 * <p>
	 * This method prevents duplicate submissions by checking the {@code isSubmitting} state.
	 * It executes the network request on a background thread and updates the LiveData
	 * constants ({@code isSubmitting}, {@code submissionSuccess}, {@code submissionError})
	 * on the main thread based on the result.
	 * </p>
	 *
	 * @param lifecycleOwner The lifecycle owner used to observe the background task and
	 *                       prevent memory leaks.
	 * @param reaction       The user's selected reaction/rating category.
	 * @param subject        The subject or title of the feedback.
	 * @param email          The contact email address of the sender.
	 * @param message        The detailed feedback message content.
	 * @param screenshot     An optional image file attached to the feedback, or {@code null}.
	 */
	public void sendFeedback(LifecycleOwner lifecycleOwner, String reaction,
	                         String subject, String email, String message, DocumentFile screenshot) {
		if (isSubmitting.getValue() != null && isSubmitting.getValue()) return;
		isSubmitting.setValue(true);
		
		WeakReference<FeedbackViewModel> weakViewModel = new WeakReference<>(this);
		ThreadTask<Void, Void> sender = new ThreadTask<>();
		sender.observeLifecycle(lifecycleOwner);
		
		sender.setBackgroundTask(callback -> {
			try {
				FeedbackPocketbase server = new FeedbackPocketbase();
				server.setCustomOKHttpClient(HttpClientProvider.getOkHttpClient(5, 10));
				
				boolean isSuccessful = server.sendFeedbackToServer(
					reaction, subject, email, message, screenshot);
				
				logger.debug("Feedback submit report received, result: " +
					(isSuccessful ? "Successful" : "Failed"));
				ThreadTask.executeOnMainThread(() -> {
					FeedbackViewModel vm = weakViewModel.get();
					if (vm != null) {
						vm.isSubmitting.setValue(false);
						vm.submissionSuccess.setValue(isSuccessful);
						if (!isSuccessful) {
							vm.handleSubmissionError();
						}
					}
				});
				
			} catch (Exception error) {
				ThreadTask.executeOnMainThread(() -> {
					FeedbackViewModel vm = weakViewModel.get();
					if (vm != null) {
						vm.isSubmitting.setValue(false);
						vm.handleSubmissionError();
					}
				});
			}
			return null;
		});
		
		sender.start();
	}
	
	/**
	 * Handles the error state when a feedback submission fails.
	 * Retrieves a localized error message from the resources and updates the
	 * {@code submissionError} LiveData to notify the user interface.
	 */
	private void handleSubmissionError() {
		int resId = R.string.hint_failed_to_submit_feedback;
		String failedMessage = StringHelper.getText(resId);
		submissionError.setValue(failedMessage);
	}
}
