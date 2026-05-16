package userInterface.feedback;

import android.content.ContentResolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.LoggerUtils;
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
	
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public boolean sendFeedbackToServer(String reaction, String subject, String email,
	                                    String message, @Nullable DocumentFile screenshot) {
		try {
			MultipartBody.Builder builder = new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(FIELD_REACTION, reaction)
				.addFormDataPart(FIELD_SUBJECT, subject)
				.addFormDataPart(FIELD_MESSAGE, message)
				.addFormDataPart(FIELD_EMAIL, email);
			
			if (screenshot != null && screenshot.exists()) {
				byte[] bytes = readDocumentFile(screenshot);
				if (bytes != null) {
					String mimeType = screenshot.getType();
					MediaType contentType = MediaType.parse(mimeType != null ? mimeType : "image/*");
					RequestBody fileBody = RequestBody.create(bytes, contentType);
					builder.addFormDataPart(FIELD_SCREENSHOT, screenshot.getName(), fileBody);
				}
			}
			
			return post(builder.build()) != null;
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
