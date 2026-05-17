package userInterface.feedback;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import coreUtils.base.BaseApplication;
import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.manager.PocketBaseClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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
	
	public boolean sendFeedbackToServer(String reaction, String subject,
	                                    String email, String message,
	                                    @Nullable DocumentFile screenshot) {
		InputStream inputStream = null;
		try {
			logger.debug("Sending feedback - reaction: " + reaction +
				", subject: " + subject + ", email: " + email +
				", message length: " + message.length()
			);
			
			BaseApplication appContext = BaseApplication.AppContext;
			String deviceId = DeviceSignature.getInstance(appContext).generate();
			String appVersion = VersionInfo.getVersionName(appContext);
			
			MultipartBody.Builder multipartBuilder = new MultipartBody
				.Builder().setType(MultipartBody.FORM);
			
			multipartBuilder.addFormDataPart(FIELD_REACTION, reaction);
			multipartBuilder.addFormDataPart(FIELD_SUBJECT, subject);
			multipartBuilder.addFormDataPart(FIELD_MESSAGE, message);
			multipartBuilder.addFormDataPart(FIELD_EMAIL, email);
			multipartBuilder.addFormDataPart(FIELD_DEVICE_ID, deviceId);
			multipartBuilder.addFormDataPart(FIELD_APP_VERSION, appVersion);
			
			if (screenshot != null && screenshot.exists()) {
				inputStream = appContext
					.getContentResolver()
					.openInputStream(screenshot.getUri());
				
				if (inputStream != null) {
					byte[] fileBytes = readBytes(inputStream);
					String mimeType =
						appContext.getContentResolver()
							.getType(screenshot.getUri());
					
					if (mimeType == null || mimeType.trim().isEmpty()) {
						mimeType = "image/*";
					}
					
					RequestBody fileBody = RequestBody.create(
						fileBytes, MediaType.parse(mimeType)
					);
					
					multipartBuilder.addFormDataPart(
						"screenshot", screenshot.getName(), fileBody
					);
				}
			}
			
			RequestBody requestBody = multipartBuilder.build();
			JSONObject response = postMultipart(requestBody);
			if (response != null) {
				String serverId = response.optString("id");
				if (!serverId.isEmpty()) {
					logger.debug("Feedback created successfully: " + serverId);
					return true;
				}
			}
			
			logger.debug("Failed to send feedback to server.");
			return false;
		} catch (Exception error) {
			logger.error("Failed to send feedback to server", error);
			return false;
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
			} catch (Exception ignored) {
				logger.error("Error ignored: ", ignored);
			}
		}
	}
	
	private byte[] readBytes(InputStream inputStream) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		byte[] data = new byte[4096];
		int nRead;
		
		while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
		
		buffer.flush();
		return buffer.toByteArray();
	}
	
	private JSONObject postMultipart(RequestBody requestBody) {
		Response response = null;
		try {
			OkHttpClient client = HttpClientProvider.getOkHttpClient(20, 20);
			Request request = new Request.Builder()
				.url(getRecordsUrl())
				.post(requestBody)
				.addHeader("Accept", "application/json")
				.build();
			
			response = client.newCall(request).execute();
			if (response.body() == null) {
				logger.error("Multipart response body is null");
				return null;
			}
			
			String responseString = response.body().string();
			logger.debug("Multipart response: " + responseString);
			
			if (!response.isSuccessful()) {
				logger.error("Multipart request failed. Code: " + response.code());
				return null;
			}
			
			return new JSONObject(responseString);
		} catch (Exception error) {
			logger.error("Failed to execute multipart request", error);
			return null;
		} finally {
			try {
				if (response != null) {
					response.close();
				}
			} catch (Exception ignored) {
				logger.error("Error ignored: ", ignored);
			}
		}
	}
}
