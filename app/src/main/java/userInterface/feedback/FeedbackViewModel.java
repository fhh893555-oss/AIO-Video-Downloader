package userInterface.feedback;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nextgen.R;

import javax.annotation.Nullable;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.strings.StringHelper;

public class FeedbackViewModel extends ViewModel {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackViewModel.class);
	private final ThreadTask<Boolean, Boolean> sender = new ThreadTask<>();
	
	private final MutableLiveData<String> selectedReaction = new MutableLiveData<>(FeedbackReactions.Excellent.name());
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
	
	public void sendFeedback(LifecycleOwner lifecycleOwner,
	                         String subject, String email, String message) {
		if (isSubmitting.getValue() != null && isSubmitting.getValue()) return;
		
		isSubmitting.setValue(true);
		sender.observeLifecycle(lifecycleOwner);
		sender.setBackgroundTask(callback -> {
			try {
				FeedbackPocketbase server = new FeedbackPocketbase();
				server.setCustomOKHttpClient(HttpClientProvider.getOkHttpClient(5, 10));
				return server.sendFeedbackToServer(
					selectedReaction.getValue(), subject, email,
					message, selectedScreenshot.getValue()
				);
			} catch (Exception error) {
				logger.error("Failed sending feedback to server: ", error);
				return false;
			}
		});
		
		sender.setResultTask(submissionReport -> {
			logger.debug("Feedback submit report received, result: " +
				(submissionReport ? "Successful" : "Failed"));
			isSubmitting.setValue(false);
			submissionSuccess.setValue(submissionReport);
			if (!submissionReport) {
				handleSubmissionError();
			}
		});
		
		sender.setErrorTask(error -> {
			logger.error("Error in sending message to server: ", error);
			isSubmitting.setValue(false);
			handleSubmissionError();
		});
		
		sender.start();
	}
	
	private void handleSubmissionError() {
		int resId = R.string.hint_failed_to_submit_feedback;
		String failedMessage = StringHelper.getText(resId);
		submissionError.setValue(failedMessage);
	}
}
