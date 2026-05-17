package userInterface.feedback;

import android.content.ContentResolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.manager.PocketBaseClient;

@SuppressWarnings("ALL")
public final class FeedbackPocketbase extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackPocketbase.class);
	private final String COLLECTION_NAME = "feedbacks";
	private final String FIELD_REACTION = "reaction";
	private final String FIELD_SUBJECT = "subject";
	private final String FIELD_MESSAGE = "message";
	private final String FIELD_EMAIL = "email";
	private final String FIELD_SCREENSHOT = "screenshot";
	private final String FIELD_DEVICE_ID = "deviceId";
	private final String FIELD_APP_VERSION = "appVersion";
	
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public boolean sendFeedbackToServer(String reaction, String subject, String email,
	                                    String message, @Nullable DocumentFile screenshot) {
		try {
			logger.debug("Sending feedback - reaction: " + reaction + ", subject: " + subject +
				", email: " + email + ", message length: " + message.length());
			
			BaseApplication appContext = BaseApplication.AppContext;
			String deviceId = DeviceSignature.getInstance(appContext).generate();
			String appVersion = VersionInfo.getVersionName(appContext);
			
			JSONObject json = new JSONObject();
			json.put(FIELD_REACTION, reaction);
			json.put(FIELD_SUBJECT, subject);
			json.put(FIELD_MESSAGE, message);
			json.put(FIELD_EMAIL, email);
			json.put(FIELD_DEVICE_ID, deviceId);
			json.put(FIELD_APP_VERSION, appVersion);
			JSONObject response = post(json);
			
			if (response != null) {
				String serverId = response.optString("id");
				if (!serverId.isEmpty()) {
					logger.debug("Feedback created succesfully: " + serverId);
					return true;
				} else {
					logger.debug("Failed to send feedback to server.");
					return false;
				}
			} else {
				return false;
			}
		} catch (Exception error) {
			logger.error("Failed to send feedback to server", error);
			return false;
		}
	}
	
	@Nullable
	private byte[] readDocumentFile(@NonNull DocumentFile file) {
		BaseApplication appContext = BaseApplication.AppContext;
		ContentResolver contentResolver = appContext.getContentResolver();
		try (InputStream inputStream = contentResolver.openInputStream(file.getUri());
		     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			if (inputStream == null) return null;
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, length);
			}
			return outputStream.toByteArray();
		} catch (Exception error) {
			logger.error("Failed to read document file", error);
			return null;
		}
	}
}
