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
	
	public LiveData<String> getSelectedReaction() {
		return selectedReaction;
	}
	
	public void setSelectedReaction(String reaction) {
		selectedReaction.setValue(reaction);
	}
	
	public LiveData<DocumentFile> getSelectedScreenshot() {
		return selectedScreenshot;
	}
	
	public void setSelectedScreenshot(@Nullable DocumentFile file) {
		selectedScreenshot.setValue(file);
	}
	
	public LiveData<Boolean> getIsSubmitting() {
		return isSubmitting;
	}
	
	public LiveData<Boolean> getSubmissionSuccess() {
		return submissionSuccess;
	}
	
	public LiveData<String> getSubmissionError() {
		return submissionError;
	}
	
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
	
	private void handleSubmissionError() {
		int resId = R.string.hint_failed_to_submit_feedback;
		String failedMessage = StringHelper.getText(resId);
		submissionError.setValue(failedMessage);
	}
}
