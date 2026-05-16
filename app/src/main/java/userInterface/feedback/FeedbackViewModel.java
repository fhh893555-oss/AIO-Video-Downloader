package userInterface.feedback;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class FeedbackViewModel extends ViewModel {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackViewModel.class);
	ThreadTask<String, String> sender = new ThreadTask<>();
	
	public void setFeedbackMessage(MessageDeliveryCallback listener,
	                               LifecycleOwner lifecycleOwner) {
		if (!sender.isCancelled()) return;
		sender.observeLifecycle(lifecycleOwner);
		sender.setBackgroundTask(callback -> {
			return "";
		});
		
		sender.setProgressUpdateTask(progressReport -> {
		
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
	
	private boolean sendMessageToSever() {
	
	}
}
