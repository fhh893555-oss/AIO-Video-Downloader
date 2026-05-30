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
 * <p>
 * This class extends {@link PocketBaseClient} and facilitates the creation of feedback records
 * including metadata such as device identifiers, application version, user comments, and optional
 * screenshot attachments via multipart/form-data requests. It provides automatic image compression
 * for large attachments to optimize upload performance and prevent server errors.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Multipart form-data request construction for feedback submission</li>
 *   <li>Automatic image compression for screenshots exceeding 1MB</li>
 *   <li>Device signature and app version injection for analytics</li>
 *   <li>Synchronous HTTP POST requests with proper resource cleanup</li>
 *   <li>Error handling and logging for debugging</li>
 * </ul>
 * </p>
 *
 * <p><b>Feedback Fields Submitted:</b>
 * <ul>
 *   <li>reaction - User sentiment (Excellent, Good, Average, Poor, Angry)</li>
 *   <li>subject - Feedback subject/title</li>
 *   <li>message - Detailed feedback description</li>
 *   <li>email - Optional user email</li>
 *   <li>deviceId - Unique device identifier</li>
 *   <li>appVersion - Current app version name</li>
 *   <li>screenshot - Optional compressed image attachment</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * FeedbackPocketbase client = new FeedbackPocketbase();
 * boolean success = client.sendFeedbackToServer(
 *     "Good",
 *     "Feature Request",
 *     "user@example.com",
 *     "Great app! Would love to see dark mode.",
 *     screenshotDocumentFile
 * );
 * </pre>
 * </p>
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
	 * <p>
	 * The request contains textual feedback fields such as reaction, subject,
	 * email, and message, along with device metadata for diagnostics and tracking.
	 * If a screenshot is provided, the image is read from the supplied
	 * {@link DocumentFile} and attached as a multipart file upload. Large images
	 * (>1MB) are automatically compressed to reduce upload size and prevent
	 * server rejection (413 errors).
	 * </p>
	 *
	 * <p><b>Request Fields:</b>
	 * <ul>
	 *   <li>reaction - User sentiment (Excellent, Good, Average, Poor, Angry)</li>
	 *   <li>subject - Feedback category or title</li>
	 *   <li>message - Detailed feedback description</li>
	 *   <li>email - Optional user contact email</li>
	 *   <li>deviceId - Unique device identifier for analytics</li>
	 *   <li>appVersion - Current app version name</li>
	 *   <li>screenshot - Optional image attachment (compressed if >1MB)</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Image Compression:</b>
	 * Images larger than 1MB are automatically compressed using {@link #compressImage(byte[])},
	 * which scales dimensions to max 1280px and compresses to JPEG at 80% quality.
	 * Compressed files are renamed with "_compressed.jpg" suffix.
	 * </p>
	 *
	 * <p><b>Resource Management:</b>
	 * This method safely handles stream cleanup to avoid resource leaks, closing
	 * the input stream in a finally block regardless of success or failure.
	 * </p>
	 *
	 * @param reaction   The selected feedback reaction type (Excellent, Good, Average, Poor, Angry)
	 * @param subject    The subject or category of the feedback (required)
	 * @param email      The optional user email address for follow-up contact
	 * @param message    The detailed feedback message body (required)
	 * @param screenshot Optional screenshot attachment. Can be null if no image is attached
	 * @return {@code true} if the server successfully accepted the feedback and returned
	 * a valid record ID; {@code false} otherwise (network error, validation failure,
	 * or server rejection)
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
	 * <p>
	 * This utility method efficiently reads an entire input stream into a byte array using
	 * a 4KB buffer. It continuously reads chunks of data until the end of the stream is
	 * reached (-1), writing each chunk to a {@link ByteArrayOutputStream}. The method
	 * automatically handles large streams and ensures all data is read before returning.
	 * </p>
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * try (InputStream inputStream = contentResolver.openInputStream(uri)) {
	 *     byte[] imageData = readBytes(inputStream);
	 *     // Process the byte array
	 * }
	 * </pre>
	 * </p>
	 *
	 * <p><b>Performance Notes:</b>
	 * <ul>
	 *   <li>Buffer size: 4KB (4096 bytes) - optimal for most I/O operations</li>
	 *   <li>Uses {@link ByteArrayOutputStream} which grows dynamically as needed</li>
	 *   <li>Caller is responsible for closing the input stream</li>
	 * </ul>
	 * </p>
	 *
	 * @param inputStream The source stream to read data from (should not be null)
	 * @return A byte array containing the complete content of the stream
	 * @throws IOException If an I/O error occurs while reading from the stream,
	 *                     such as a network failure or file access error
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
	 * used to prevent "413 Request Entity Too Large" errors during upload by reducing
	 * both the resolution and file size of attached screenshots.
	 * </p>
	 *
	 * <p><b>Compression Steps:</b>
	 * <ol>
	 *   <li>Decodes image bounds without loading full bitmap to determine dimensions</li>
	 *   <li>Calculates sample size to scale down images exceeding 1280px on any side</li>
	 *   <li>Decodes bitmap with calculated sample size for memory efficiency</li>
	 *   <li>Compresses bitmap to JPEG format at 80% quality</li>
	 *   <li>Recycles bitmap to free native memory</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Compression Limits:</b>
	 * <ul>
	 *   <li>Maximum dimension: 1280 pixels (maintains aspect ratio)</li>
	 *   <li>Output format: JPEG</li>
	 *   <li>JPEG quality: 80% (balance between quality and file size)</li>
	 * </ul>
	 * </p>
	 *
	 * @param inputBytes The raw image data as a byte array (PNG, JPEG, or other supported format)
	 * @return A compressed JPEG image byte array, or the original {@code inputBytes}
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
	 * the PocketBase records endpoint.
	 * <p>
	 * This method is responsible for submitting feedback data including text fields
	 * (subject, message, email, reaction) and optional file attachments (screenshots)
	 * as a multipart/form-data request. The request is executed synchronously, so it
	 * should be called from a background thread to avoid blocking the UI.
	 * </p>
	 *
	 * <p><b>Request Details:</b>
	 * <ul>
	 *   <li>Method: POST</li>
	 *   <li>Content-Type: multipart/form-data</li>
	 *   <li>Accept Header: application/json</li>
	 *   <li>Endpoint: PocketBase records URL</li>
	 * </ul>
	 * </p>
	 *
	 * @param requestBody The multipart/form-data body containing feedback fields
	 *                    (subject, message, email, reaction) and optional screenshot attachments
	 * @param deviceId    the device identifier sent in the {@code X-Device-Id} header
	 *                    for request contextualization
	 * @return A {@link JSONObject} containing the server response if the request was successful;
	 * {@code null} if the request failed, the response body was empty, an exception occurred,
	 * or the HTTP status code indicates an error (non-2xx)
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
