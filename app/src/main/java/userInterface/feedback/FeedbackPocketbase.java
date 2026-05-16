package userInterface.feedback;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;

public final class FeedbackPocketbase extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackPocketbase.class);
	private final String COLLECTION_NAME = "feedbacks";
	private final String FIELD_REACTION = "reaction";
	private final String FIELD_SUBJECT = "subject";
	private final String FIELD_MESSAGE = "message";
	private final String FIELD_EMAIL = "email";
	private final String FIELD_SCREENSHOT = "screenshot";
	
	
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public boolean sendFeedbackToServer(String reaction, String subject, String email,
	                                    String message, DocumentFile screenshot) {
		JSONObject payload = new JSONObject();
		payload.put("")
		post(payload);
		
	}
}
