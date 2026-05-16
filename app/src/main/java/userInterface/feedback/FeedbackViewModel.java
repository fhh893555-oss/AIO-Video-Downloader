package userInterface.feedback;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class FeedbackViewModel extends ViewModel {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackViewModel.class);
	private final ThreadTask<Boolean, Boolean> sender = new ThreadTask<>();
	
	public void setFeedbackMessage(MessageDeliveryCallback listener,
	                               LifecycleOwner lifecycleOwner,
	                               String reaction, String subject, String email,
	                               String message, @Nullable DocumentFile screenshot) {
		if (!sender.isCancelled()) return;
		sender.observeLifecycle(lifecycleOwner);
		sender.setBackgroundTask(callback -> {
			try {
				FeedbackPocketbase feedbackPocketbase = new FeedbackPocketbase();
				return feedbackPocketbase.sendFeedbackToServer(
					reaction, subject, email, message, screenshot);
			} catch (Exception error) {
				logger.error("Failed sending feedback to server: ", error);
				return false;
			}
		});
		
		sender.setResultTask(submissionReport -> {
			logger.debug("Feedback sending delivery report: " +
				(submissionReport ? "Successful" : "Failed"));
			Exception error = new Exception("Failed on submitting feedback to the could.");
			if (submissionReport) listener.onSuccessful();
			else {listener.onFailed(error);}
		});
		
		sender.setErrorTask(error -> {
			logger.error("Error in sending message to server: ", error);
			listener.onFailed((Exception) error);
		});
		
		sender.start();
	}
	
	public interface MessageDeliveryCallback {
		void onSuccessful();
		void onFailed(Exception error);
	}
	
}
