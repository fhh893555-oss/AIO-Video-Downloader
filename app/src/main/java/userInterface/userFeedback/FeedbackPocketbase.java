package userInterface.userFeedback;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.manager.PocketBaseClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A client class responsible for handling the submission of user feedback to a PocketBase backend.
 * This class extends {@link PocketBaseClient} and facilitates the creation of feedback records
 * including metadata such as device identifiers, application version, user comments, and optional
 * screenshot attachments via multipart/form-data requests.
 *
 * @see PocketBaseClient
 */
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
	
	/**
	 * Returns the name of the PocketBase collection associated with feedback entries.
	 *
	 * @return A non-null string representing the "feedbacks" collection name.
	 */
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	/**
	 * Sends user feedback data to the backend server using a multipart form request.
	 * <p>
	 * The request contains textual feedback fields such as reaction, subject,
	 * email, and message, along with device metadata for diagnostics and tracking.
	 * If a screenshot is provided, the image is read from the supplied
	 * {@link DocumentFile} and attached as a multipart file upload.
	 * </p>
	 * <p>
	 * This method performs validation indirectly through request construction
	 * and safely handles stream cleanup to avoid resource leaks.
	 * </p>
	 *
	 * @param reaction   The selected feedback reaction type.
	 * @param subject    The subject or category of the feedback.
	 * @param email      The optional user email address.
	 * @param message    The detailed feedback message body.
	 * @param screenshot Optional screenshot attachment. Can be null.
	 * @return {@code true} if the server successfully accepted the feedback
	 * and returned a valid record ID, otherwise {@code false}.
	 */
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
				ContentResolver contentResolver = appContext.getContentResolver();
				inputStream = contentResolver
					.openInputStream(screenshot.getUri());
				
				if (inputStream != null) {
					byte[] fileBytes = readBytes(inputStream);
					String mimeType;
					String fileName = screenshot.getName() != null ?
						screenshot.getName() : "screenshot.png";
					
					if (fileBytes.length > 1024 * 1024) {
						logger.debug("Compressing large screenshot: " +
							fileBytes.length + " bytes");
						fileBytes = compressImage(fileBytes);
						
						logger.debug("Compressed screenshot size: " +
							fileBytes.length + " bytes");
						mimeType = "image/jpeg";
						
						fileName = "screenshot_compressed.jpg";
					} else {
						mimeType = contentResolver.getType(screenshot.getUri());
						if (mimeType == null || mimeType.trim().isEmpty()) {
							mimeType = "image/png";
						}
					}
					
					RequestBody fileBody = RequestBody.create(
						fileBytes, MediaType.parse(mimeType));
					
					multipartBuilder.addFormDataPart(
						FIELD_SCREENSHOT, fileName, fileBody);
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
	
	/**
	 * Reads all bytes from the provided {@link InputStream} and returns them as a byte array.
	 *
	 * @param inputStream The source stream to read data from.
	 * @return A byte array containing the complete content of the stream.
	 * @throws IOException If an I/O error occurs while reading from the stream.
	 */
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
	
	/**
	 * Compresses an image byte array to reduce its file size and dimensions.
	 * <p>
	 * This method decodes the byte array into a {@link Bitmap}, scales it down if its
	 * largest dimension exceeds 1280 pixels while maintaining aspect ratio, and
	 * compresses the result into a JPEG format with 80% quality. This is primarily
	 * used to prevent "413 Request Entity Too Large" errors during upload.
	 * </p>
	 *
	 * @param inputBytes The raw image data as a byte array.
	 * @return A compressed JPEG image byte array, or the original {@code inputBytes}
	 * if decoding fails or an error occurs during processing.
	 */
	private byte[] compressImage(byte[] inputBytes) {
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.length, options);
			
			int srcWidth = options.outWidth;
			int srcHeight = options.outHeight;
			int maxSide = 1280;
			
			int inSampleSize = 1;
			if (srcWidth > maxSide || srcHeight > maxSide) {
				int halfWidth = srcWidth / 2;
				int halfHeight = srcHeight / 2;
				while ((halfWidth / inSampleSize) >=
					maxSide && (halfHeight / inSampleSize) >= maxSide) {
					inSampleSize *= 2;
				}
			}
			
			options.inJustDecodeBounds = false;
			options.inSampleSize = inSampleSize;
			Bitmap scaledBitmap = BitmapFactory.decodeByteArray(
				inputBytes, 0, inputBytes.length, options);
			
			if (scaledBitmap == null) return inputBytes;
			
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
			byte[] result = outputStream.toByteArray();
			
			scaledBitmap.recycle();
			return result;
		} catch (Exception error) {
			logger.error("Failed to safely compress image streams cleanly", error);
			return inputBytes;
		}
	}
	
	/**
	 * Executes a synchronous HTTP POST request with a multipart request body to
	 * the PocketBase records endpoint.
	 *
	 * @param requestBody The multipart/form-data body containing feedback fields
	 *                    and optional attachments.
	 * @return A {@link JSONObject} containing the server response if the request
	 * was successful;
	 * <p>
	 * {@code null} if the request failed, the response body was empty, or an
	 * exception occurred.
	 */
	private JSONObject postMultipart(RequestBody requestBody) {
		Response response = null;
		try {
			Request request = new Request.Builder()
				.url(getRecordsUrl())
				.post(requestBody)
				.addHeader("Accept", "application/json")
				.build();
			
			response = getHttpClient().newCall(request).execute();
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
