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
import dataRepo.dbManager.PocketBaseClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * A client class responsible for handling the submission of user feedback to a PocketBase backend.
 * This class extends {@link PocketBaseClient} and facilitates the creation of feedback records
 * including metadata such as device identifiers, application version, user comments, and optional
 * screenshot attachments via multipart/form-data requests. It provides automatic image compression
 * for large attachments to optimize upload performance and prevent server errors.
 *
 * <p><strong>Key features:</strong>
 * <ul>
 * <li>Multipart form-data request construction for feedback submission.</li>
 * <li>Automatic image compression for screenshots exceeding 1MB.</li>
 * <li>Device signature and app version injection for analytics.</li>
 * <li>Synchronous HTTP POST requests with proper resource cleanup.</li>
 * </ul>
 *
 * <p><strong>Feedback fields submitted:</strong>
 * reaction (user sentiment), subject, message, email (optional), deviceId,
 * appVersion, and optional compressed screenshot attachment.
 *
 * @see PocketBaseClient
 * @see MultipartBody
 * @see DeviceSignature
 * @see VersionInfo
 */
@SuppressWarnings("ALL")
public final class FeedbackPocketbase extends PocketBaseClient {
	
	private final LoggerUtils logger = LoggerUtils.from(FeedbackPocketbase.class);
	private final String COLLECTION_NAME = "userFeedbacks";
	private final String FIELD_REACTION = "reaction";
	private final String FIELD_SUBJECT = "subject";
	private final String FIELD_MESSAGE = "message";
	private final String FIELD_EMAIL = "email";
	private final String FIELD_SCREENSHOT = "screenshot";
	private final String FIELD_DEVICE_ID = "deviceId";
	private final String FIELD_APP_VERSION = "appVersion";
	
	/**
	 * Returns the name of the PocketBase collection associated with feedback entries.
	 * <p>
	 * This method overrides the abstract {@link PocketBaseClient#getCollectionName()}
	 * method to provide the specific collection name where feedback records are stored.
	 * The collection name is used for constructing API endpoints for creating,
	 * reading, updating, and deleting feedback entries in the PocketBase backend.
	 * </p>
	 *
	 * <p><b>Collection Purpose:</b>
	 * The "feedbacks" collection stores user-submitted feedback including reaction
	 * ratings, subject lines, detailed messages, email addresses, device identifiers,
	 * app version information, and optional screenshot attachments.
	 * </p>
	 *
	 * @return A non-null string representing the "feedbacks" collection name
	 */
	@NonNull
	@Override
	protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	/**
	 * Sends user feedback data to the backend server using a multipart form request.
	 * The request contains textual feedback fields (reaction, subject, email, message)
	 * along with device metadata for diagnostics. If a screenshot is provided, the
	 * image is attached as a multipart file upload. Large images (>1MB) are
	 * automatically compressed to reduce upload size and prevent server errors.
	 *
	 * <p>Request fields: reaction, subject, message, email, deviceId, appVersion,
	 * and optional screenshot (compressed if >1MB).</p>
	 *
	 * @param reaction   The selected feedback reaction (Excellent, Good, Average,
	 *                   Poor, Angry)
	 * @param subject    The subject or category of the feedback (required)
	 * @param email      Optional user email address for follow-up contact
	 * @param message    The detailed feedback message body (required)
	 * @param screenshot Optional screenshot attachment (can be null)
	 * @return {@code true} if server successfully accepted the feedback;
	 * {@code false} otherwise (network error or server rejection)
	 */
	public boolean sendFeedbackToServer(String reaction,
	                                    String email, String message,
	                                    @Nullable DocumentFile screenshot) {
		InputStream inputStream = null;
		try {
			logger.debug("Sending feedback - reaction: " + reaction +
				"," + "email: " + email +
				", message length: " + message.length()
			);
			
			BaseApplication appContext = BaseApplication.AppContext;
			String deviceId = DeviceSignature.getInstance(appContext).generate();
			String appVersion = VersionInfo.getVersionName(appContext);
			
			MultipartBody.Builder multipartBuilder = new MultipartBody
				.Builder().setType(MultipartBody.FORM);
			
			multipartBuilder.addFormDataPart(FIELD_REACTION, reaction);
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
			JSONObject response = postMultipart(requestBody, deviceId);
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
	 * This utility method efficiently reads an entire input stream using a 4KB buffer and
	 * writes each chunk to a {@link ByteArrayOutputStream} until the stream ends.
	 *
	 * <p>Buffer size is 4KB (4096 bytes), which is optimal for most I/O operations.
	 * The caller is responsible for closing the input stream.</p>
	 *
	 * @param inputStream The source stream to read data from (must not be null)
	 * @return A byte array containing the complete content of the stream
	 * @throws IOException If an I/O error occurs while reading from the stream
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
	 * This method decodes the byte array into a {@link Bitmap}, scales it down if
	 * the largest dimension exceeds 1280 pixels (maintaining aspect ratio), and
	 * compresses the result to JPEG at 80% quality. This prevents "413 Request
	 * Entity Too Large" errors during upload of attached screenshots.
	 *
	 * <p><strong>Compression steps:</strong>
	 * Decode image bounds without loading full bitmap → calculate sample size for
	 * scaling → decode with sample size → compress to JPEG at 80% quality → recycle bitmap.
	 *
	 * @param inputBytes The raw image data as a byte array (PNG, JPEG, or other)
	 * @return A compressed JPEG byte array, or the original {@code inputBytes}
	 * if decoding fails or an error occurs during processing
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
	 * the PocketBase records endpoint. This method submits feedback data including
	 * text fields (subject, message, email, reaction) and optional screenshot
	 * attachments as multipart/form-data. The request is synchronous and should be
	 * called from a background thread to avoid blocking the UI.
	 *
	 * <p>Method: POST | Content-Type: multipart/form-data | Accept: application/json</p>
	 *
	 * @param requestBody The multipart/form-data body containing feedback fields
	 *                    and optional screenshot attachments
	 * @param deviceId    The device identifier sent in the {@code X-Device-Id} header
	 * @return A {@link JSONObject} containing the server response if successful;
	 * {@code null} if request failed or HTTP status indicates an error
	 */
	private JSONObject postMultipart(RequestBody requestBody, String deviceId) {
		Response response = null;
		try {
			Request request = new Request.Builder()
				.url(getRecordsUrl())
				.post(requestBody)
				.addHeader("Accept", "application/json")
				.addHeader("X-Device-Id", deviceId)
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
