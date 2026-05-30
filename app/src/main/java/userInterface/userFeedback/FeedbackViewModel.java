package userInterface.userFeedback;

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
 * This class handles user-selected reactions, optional screenshots, and the
 * asynchronous submission of feedback to the server. It provides LiveData
 * streams to observe submission progress, success, and errors, surviving
 * configuration changes like screen rotations.
 *
 * <p><strong>Managed state:</strong>
 * selectedReaction (user sentiment), selectedScreenshot (optional attachment),
 * isSubmitting (submission in progress flag), submissionSuccess (result of
 * last submission), submissionError (error message on failure).</p>
 *
 * <p>UI components observe these LiveData to update reaction highlighting,
 * show/hide loading indicators, display success/error messages, and reset
 * the form after successful submission.</p>
 *
 * @see ViewModel
 * @see LiveData
 * @see FeedbackReactions
 * @see FeedbackPocketbase
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
	 * <p>
	 * This LiveData holds the string value of the reaction selected by the user,
	 * which corresponds to one of the constants defined in {@link FeedbackReactions}
	 * (Excellent, Good, Average, Poor, or Angry). UI components can observe this
	 * LiveData to highlight the selected reaction and persist the selection across
	 * configuration changes.
	 * </p>
	 *
	 * @return A {@link LiveData} object holding the string name of the selected reaction,
	 * or null if no reaction has been selected yet
	 */
	public LiveData<String> getSelectedReaction() {
		return selectedReaction;
	}
	
	/**
	 * Sets the currently selected feedback reaction.
	 * <p>
	 * This method updates the LiveData with the user's chosen reaction. The reaction
	 * value is typically one of the constants from {@link FeedbackReactions}. When
	 * this value changes, any UI components observing this LiveData will be notified
	 * and can update their visual state (e.g., applying bold text and primary color
	 * to the selected reaction button).
	 * </p>
	 *
	 * @param reaction The name of the reaction to be selected, typically one of the
	 *                 values from {@link FeedbackReactions} (Excellent, Good, Average,
	 *                 Poor, or Angry)
	 */
	public void setSelectedReaction(String reaction) {
		selectedReaction.setValue(reaction);
	}
	
	/**
	 * Gets the observable LiveData containing the currently selected screenshot file.
	 * <p>
	 * This LiveData holds the DocumentFile reference of the screenshot image attached
	 * to the feedback submission. UI components can observe this LiveData to display
	 * the selected file name, file size, and toggle between the upload prompt and
	 * file details views. The value will be null when no screenshot is selected.
	 * </p>
	 *
	 * @return A {@link LiveData} object holding the {@link DocumentFile} of the screenshot,
	 * which may be null if no screenshot has been selected
	 */
	public LiveData<DocumentFile> getSelectedScreenshot() {
		return selectedScreenshot;
	}
	
	/**
	 * Sets the selected screenshot file to be included with the feedback submission.
	 * <p>
	 * This method updates the LiveData holding the screenshot reference. UI components
	 * observing this LiveData will be notified of the change and can update the UI
	 * accordingly (e.g., showing the selected file name and size).
	 * </p>
	 *
	 * @param file The {@link DocumentFile} representing the screenshot, or {@code null} if
	 *             no screenshot is selected (clears the selection)
	 */
	public void setSelectedScreenshot(@Nullable DocumentFile file) {
		selectedScreenshot.setValue(file);
	}
	
	/**
	 * Gets an observable LiveData that indicates whether the feedback submission process
	 * is currently in progress.
	 * <p>
	 * This LiveData is used by the UI to disable the submit button, show a loading indicator,
	 * and prevent duplicate submissions while a network request is ongoing. The value is
	 * true during submission and false when the request completes (successfully or with error).
	 * </p>
	 *
	 * @return A {@link LiveData} containing {@code true} if the submission is ongoing,
	 * {@code false} otherwise
	 */
	public LiveData<Boolean> getIsSubmitting() {
		return isSubmitting;
	}
	
	/**
	 * Returns an observable {@link LiveData} indicating whether the feedback submission
	 * was successful.
	 * <p>
	 * This LiveData is updated after each submission attempt. A value of true indicates
	 * the server successfully accepted the feedback. False indicates a failure, in which
	 * case the error message can be retrieved via {@link #getSubmissionError()}.
	 * The value may be null if no submission attempt has been made yet.
	 * </p>
	 *
	 * @return A {@link LiveData} containing {@code true} if the submission succeeded,
	 * {@code false} if it failed, or {@code null} if no attempt has been made yet
	 */
	public LiveData<Boolean> getSubmissionSuccess() {
		return submissionSuccess;
	}
	
	/**
	 * Gets the LiveData containing the error message if the feedback submission fails.
	 * This LiveData holds a localized error message that can be displayed to the user
	 * when a submission fails. It is updated automatically when an error occurs.
	 *
	 * @return A {@link LiveData} object holding the error message string,
	 * or {@code null} if no error has occurred
	 */
	public LiveData<String> getSubmissionError() {
		return submissionError;
	}
	
	/**
	 * Sends the feedback data to the server asynchronously. This method prevents
	 * duplicate submissions by checking the {@code isSubmitting} state. The network
	 * request executes on a background thread, and LiveData constants
	 * ({@code isSubmitting}, {@code submissionSuccess}, {@code submissionError})
	 * are updated on the main thread based on the result.
	 *
	 * <p><strong>Submission flow:</strong>
	 * Check if submission in progress (return early if true) → set isSubmitting = true
	 * → execute network request on background → update LiveData on main thread
	 * → reset isSubmitting to false.
	 *
	 * @param lifecycleOwner The lifecycle owner used to bind the background task
	 *                       (typically Activity or Fragment)
	 * @param reaction       The user's selected reaction/rating category
	 * @param subject        The subject or title of the feedback
	 * @param email          The contact email address of the sender (optional)
	 * @param message        The detailed feedback message content
	 * @param screenshot     An optional image file attached to the feedback, or null
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
	 * <p>
	 * This method is called when an error occurs during the feedback submission process,
	 * such as network failures, server errors, or request validation issues. It retrieves
	 * a localized error message from the string resources and updates the
	 * {@code submissionError} LiveData to notify the user interface, allowing the UI
	 * to display an appropriate error toast or dialog to the user.
	 * </p>
	 *
	 * <p><b>Error Message Used:</b>
	 * "Failed to submit feedback"
	 * </p>
	 */
	private void handleSubmissionError() {
		int resId = R.string.hint_failed_to_submit_feedback;
		String failedMessage = StringHelper.getText(resId);
		submissionError.setValue(failedMessage);
	}
}
