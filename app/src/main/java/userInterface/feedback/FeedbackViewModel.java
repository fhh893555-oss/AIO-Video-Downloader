package userInterface.feedback;

import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class FeedbackViewModel extends ViewModel {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackViewModel.class);
	private final ThreadTask<Boolean, Boolean> sender = new ThreadTask<>();
	
	private final MutableLiveData<String> selectedReaction = new MutableLiveData<>("Excellent");
	private final MutableLiveData<DocumentFile> selectedScreenshot = new MutableLiveData<>();
	private final MutableLiveData<Boolean> isSubmitting = new MutableLiveData<>(false);
	private final MutableLiveData<Boolean> submissionSuccess = new MutableLiveData<>();
	private final MutableLiveData<String> submissionError = new MutableLiveData<>();

	public LiveData<String> getSelectedReaction() { return selectedReaction; }
	public void setSelectedReaction(String reaction) { selectedReaction.setValue(reaction); }

	public LiveData<DocumentFile> getSelectedScreenshot() { return selectedScreenshot; }
	public void setSelectedScreenshot(DocumentFile file) { selectedScreenshot.setValue(file); }

	public LiveData<Boolean> getIsSubmitting() { return isSubmitting; }
	public LiveData<Boolean> getSubmissionSuccess() { return submissionSuccess; }
	public LiveData<String> getSubmissionError() { return submissionError; }

	public void sendFeedback(LifecycleOwner lifecycleOwner, String subject, String email, String message) {
		if (isSubmitting.getValue() != null && isSubmitting.getValue()) return;
		
		isSubmitting.setValue(true);
		sender.observeLifecycle(lifecycleOwner);
		sender.setBackgroundTask(callback -> {
			try {
				FeedbackPocketbase feedbackPocketbase = new FeedbackPocketbase();
				return feedbackPocketbase.sendFeedbackToServer(
					selectedReaction.getValue(), subject, email,
					message, selectedScreenshot.getValue()
				);
			} catch (Exception error) {
				logger.error("Failed sending feedback to server: ", error);
				return false;
			}
		});
		
		sender.setResultTask(submissionReport -> {
			isSubmitting.setValue(false);
			if (submissionReport) {
				submissionSuccess.setValue(true);
			} else {
				submissionError.setValue("Failed to submit feedback. Please try again.");
			}
		});
		
		sender.setErrorTask(error -> {
			isSubmitting.setValue(false);
			logger.error("Error in sending message to server: ", error);
			submissionError.setValue(error.getMessage());
		});
		
		sender.start();
	}
}
