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
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

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
	private final String FIELD_PLATFORM = "platform";
	
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
			
			MultipartBody.Builder builder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(FIELD_REACTION, reaction != null ? reaction : "Excellent")
				.addFormDataPart(FIELD_SUBJECT, subject != null ? subject : "")
				.addFormDataPart(FIELD_MESSAGE, message)
				.addFormDataPart(FIELD_EMAIL, email != null ? email : "")
				.addFormDataPart(FIELD_DEVICE_ID, deviceId)
				.addFormDataPart(FIELD_APP_VERSION, appVersion != null ? appVersion : "unknown")
				.addFormDataPart(FIELD_PLATFORM, "Android");
			
			logger.debug("Added device info - device_id: " + deviceId + ", app_version: " + appVersion);
			if (screenshot != null && screenshot.exists()) {
				logger.debug("Attaching screenshot: " + screenshot.getName());
				byte[] bytes = readDocumentFile(screenshot);
				if (bytes != null) {
					logger.debug("Screenshot size: " + bytes.length + " bytes");
					String mimeType = screenshot.getType();
					
					if (mimeType == null || mimeType.isEmpty()) {
						String fileName = screenshot.getName();
						
						if (fileName != null && fileName.toLowerCase().endsWith(".png")) {
							mimeType = "image/png";
							
						} else if (fileName != null &&
							(fileName.toLowerCase().endsWith(".jpg") ||
								fileName.toLowerCase().endsWith(".jpeg"))) {
							mimeType = "image/jpeg";
							
						} else {
							mimeType = "image/jpeg";
						}
					}
					
					MediaType contentType = MediaType.parse(mimeType);
					RequestBody fileBody = RequestBody.create(bytes, contentType);
					builder.addFormDataPart(FIELD_SCREENSHOT, screenshot.getName(), fileBody);
				}
			}
			
			MultipartBody requestBody = builder.build();
			logger.debug("Request content-type: " + requestBody.contentType());
			
			JSONObject result = post(requestBody);
			if (result != null) {
				logger.debug("Feedback sent successfully, response: " + result.toString());
				return true;
				
			} else {
				logger.debug("Feedback submission returned null response");
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
