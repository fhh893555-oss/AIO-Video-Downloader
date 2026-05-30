package dataRepo.userDetails;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

import coreUtils.library.process.LoggerUtils;
import dataRepo.dbManager.PocketBaseClient;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Cloud repository client for synchronizing user data with a remote PocketBase
 * backend. This class extends {@link PocketBaseClient} and provides specialized
 * methods for performing CRUD operations on user records stored in the cloud.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Creates new user records via {@link #createUser(JSONObject, String)}.</li>
 * <li>Updates existing user records via {@link #updateUser(String, JSONObject, String)}
 *     and {@link #updateField(String, String, Object, String)}.</li>
 * <li>Uploads user profile photos via {@link #uploadUserPhoto(String, Bitmap, String)}.</li>
 * <li>Queries user records by device ID via {@link #getUserByDeviceId(String)}.</li>
 * <li>Retrieves specific field values via {@link #getField(String, String, String)}.</li>
 * </ul>
 *
 * <p>The class uses the collection name defined in {@link AppUser#POCKETBASE_COLLECTION_NAME}
 * and follows the singleton or static utility pattern as implemented in the parent
 * {@link PocketBaseClient} class. All methods include error handling and return
 * {@code null} for failed operations rather than throwing exceptions.
 *
 * <p><strong>Network requirements:</strong>
 * Requires an active internet connection. Operations may block the calling thread;
 * invoke from a background thread (e.g., using {@link coreUtils.library.process.ThreadTask},
 * {@link java.util.concurrent.Executor}, or coroutines) to avoid blocking the UI thread.
 *
 * @see PocketBaseClient
 * @see AppUser
 * @see #createUser(JSONObject, String)
 * @see #updateUser(String, JSONObject, String)
 * @see #uploadUserPhoto(String, Bitmap, String)
 * @see #getUserByDeviceId(String)
 */
public final class AppUserCloud extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Returns the PocketBase collection name used for user records. This method
	 * overrides the abstract method from the parent class and provides the specific
	 * collection identifier defined in {@link AppUser#POCKETBASE_COLLECTION_NAME}.
	 *
	 * <p>The collection name is required for constructing API endpoints for CRUD
	 * operations (create, read, update, delete) targeting user data on the PocketBase
	 * server.
	 *
	 * @return The collection name string, always non-null. Matches the constant
	 * defined in the {@link AppUser} entity class.
	 * @see AppUser#POCKETBASE_COLLECTION_NAME
	 * @see #getCollectionName()
	 */
	@NonNull
	@Override
	protected String getCollectionName() {
		return AppUser.POCKETBASE_COLLECTION_NAME;
	}
	
	/**
	 * Creates a new user record on the PocketBase server. This method delegates to
	 * {@link #post(JSONObject, String)} to perform an HTTP POST request to the collection's
	 * endpoint with the provided user data.
	 *
	 * <p><strong>Expected usage:</strong>
	 * This method is typically called after app installation or first launch to
	 * register the device with the backend. The {@code data} parameter should
	 * include required fields such as device ID, installation timestamp, and any
	 * initial user preferences.
	 *
	 * @param data     A {@link JSONObject} containing the user data to be stored.
	 *                 Must not be null. Required fields should match the server's
	 *                 collection schema.
	 * @param deviceId The device identifier used for authentication or tracking.
	 *                 Must not be null.
	 * @return The parsed JSON response from the server containing the newly created
	 * record (including the server-assigned ID and timestamps), or
	 * {@code null} if the request failed (e.g., network error, duplicate
	 * device ID, or server validation error).
	 * @see #post(JSONObject, String)
	 * @see #updateUser(String, JSONObject, String)
	 */
	@Nullable
	public JSONObject createUser(@NonNull JSONObject data,
	                             @NonNull String deviceId) {
		return post(data, deviceId);
	}
	
	/**
	 * Updates a user record on the PocketBase server by performing a PATCH request
	 * with the provided JSON data. This method is a pass-through convenience wrapper
	 * that delegates directly to {@link #patch(String, JSONObject, String)}.
	 *
	 * <p>Use this method when you need to update multiple fields simultaneously or
	 * when you already have a constructed JSON object containing all changes.
	 *
	 * @param serverId The PocketBase record ID of the user to update. Must not be null.
	 * @param data     A {@link JSONObject} containing the fields to update and their
	 *                 new values. Must not be null.
	 * @param deviceId The device identifier used for authentication or tracking.
	 *                 Must not be null.
	 * @return The parsed JSON response from the server after a successful update,
	 * or {@code null} if the request failed (e.g., network error,
	 * authentication failure, or non-2xx HTTP response).
	 * @see #patch(String, JSONObject, String)
	 * @see #updateField(String, String, Object, String)
	 */
	@Nullable
	public JSONObject updateUser(@NonNull String serverId,
	                             @NonNull JSONObject data,
	                             @NonNull String deviceId) {
		return patch(serverId, data, deviceId);
	}
	
	/**
	 * Updates a single field of a user record on the PocketBase server. This method
	 * constructs a JSON object containing the specified field-value pair and delegates
	 * to {@link #updateUser(String, JSONObject, String)}  for the actual network operation.
	 *
	 * <p><strong>Example usage:</strong>
	 * <pre>
	 * boolean success = pocketBaseRepo.updateField(userId, "username", "newUsername");
	 * if (success) {
	 *     // Field updated successfully
	 * }
	 * </pre>
	 *
	 * @param serverId The PocketBase record ID of the user to update. Must not be null.
	 * @param field    The name of the field to update. Must not be null.
	 * @param value    The new value to assign to the specified field. Must not be null.
	 *                 The type should match the field's expected type on the server
	 *                 (e.g., String, Integer, Boolean).
	 * @param deviceId The device identifier used for authentication or tracking.
	 *                 Must not be null.
	 * @return {@code true} if the update was successful (server responded with 2xx and
	 * returned a non-null response), {@code false} otherwise.
	 * @throws JSONException If constructing the JSON object fails (e.g., invalid value type).
	 * @see #updateUser(String, JSONObject, String)
	 * @see #patch(String, JSONObject, String)
	 */
	public boolean updateField(@NonNull String serverId,
	                           @NonNull String field,
	                           @NonNull Object value,
	                           @NonNull String deviceId) throws JSONException {
		JSONObject json = new JSONObject();
		json.put(field, value);
		return patch(serverId, json, deviceId) != null;
	}
	
	/**
	 * Constructs the full publicly accessible URL for a user's uploaded profile image
	 * stored on the PocketBase server. The URL follows the PocketBase file serving
	 * pattern: {@code /api/files/{collection}/{recordId}/{fileName}}.
	 *
	 * @param recordId The PocketBase record identifier of the user. Must not be null.
	 * @param fileName The stored filename returned by the server after upload.
	 *                 Must not be null.
	 * @return The complete file URL as a non-null string.
	 * @see #uploadUserPhoto(String, Bitmap, String)
	 * @see AppUser#POCKETBASE_COLLECTION_NAME
	 */
	@NonNull
	public String buildFileUrl(@NonNull String recordId,
	                           @NonNull String fileName) {
		return API_ENDPOINT + "/api/files/" +
			AppUser.POCKETBASE_COLLECTION_NAME +
			"/" + recordId + "/" + fileName;
	}
	
	/**
	 * Retrieves a specific field value from a user record by querying the PocketBase
	 * server. This method executes a query with the given filter condition and returns
	 * the value of the requested field from the first matching record.
	 *
	 * @param serverId The record ID of the target user. Used to build filter
	 *                 condition: {@code "id='serverId'"}. Must not be null.
	 * @param field    The name of the field to extract from the response JSON.
	 *                 Must not be null.
	 * @param deviceId The device identifier used for authentication or tracking.
	 *                 Must not be null.
	 * @return The field value as an {@link Object} (type depends on the field),
	 * or {@code null} if no matching record is found or the field is absent.
	 * @see #query(String, String, String)
	 */
	@Nullable
	public Object getField(@NonNull String serverId,
	                       @NonNull String field,
	                       @NonNull String deviceId) {
		JSONObject result = query("id='" + serverId + "'", field, deviceId);
		return result != null ? result.opt(field) : null;
	}
	
	/**
	 * Retrieves a complete user record from PocketBase by searching for a matching
	 * device identifier. This method queries the PocketBase collection using the
	 * field {@link AppUser#POCKETBASE_REMOTE_DEVICE_ID_FIELD} as the filter criterion.
	 *
	 * <p>The returned {@link JSONObject} contains all fields of the matching user
	 * record. If multiple records match the same device ID (which should not occur
	 * under normal conditions), only the first result is returned.
	 *
	 * @param deviceId The unique device identifier used to locate the user record.
	 *                 Must not be null.
	 * @return A {@link JSONObject} representing the full user record, or {@code null}
	 * if no record with the given device ID exists.
	 * @see #query(String, String, String)
	 * @see AppUser#POCKETBASE_REMOTE_DEVICE_ID_FIELD
	 */
	@Nullable
	public JSONObject getUserByDeviceId(@NonNull String deviceId) {
		String deviceIdField = AppUser.POCKETBASE_REMOTE_DEVICE_ID_FIELD;
		return query(deviceIdField + "='" + deviceId + "'", null, deviceId);
	}
	
	/**
	 * Uploads a user profile photo to the PocketBase server. This method compresses
	 * the provided bitmap as a PNG image, constructs a multipart form-data request,
	 * and sends a PATCH request to update the user record with the new photo.
	 *
	 * <p><strong>Upload flow:</strong>
	 * The bitmap is compressed to PNG format with 100% quality, converted to a byte
	 * array, and wrapped in a {@link RequestBody} with image/png media type. A multipart
	 * body is built with the profile image field name from {@link AppUser} constants,
	 * a timestamp-based filename, and the image body. On success, the server returns
	 * the stored filename, which is used to construct the full accessible URL.
	 *
	 * <p><strong>Error handling:</strong>
	 * Any exception during compression, request building, network I/O, or JSON parsing
	 * is caught and logged. The method returns {@code null} for all failure cases,
	 * including non-successful HTTP responses (e.g., 400, 401, 500).
	 *
	 * @param serverId The PocketBase record ID of the user to update. Must not be
	 *                 {@code null} and must correspond to an existing user record.
	 * @param bitmap   The user profile image as a {@link Bitmap}. Must not be
	 *                 {@code null}. The bitmap is compressed to PNG format before upload.
	 * @param deviceId the device identifier sent in the {@code X-Device-Id} header
	 *                 for request contextualization
	 * @return The fully constructed URL to the uploaded profile image, or {@code null}
	 * if the upload failed for any reason (network error, server rejection,
	 * parsing failure, etc.).
	 * @see #getRecordsUrl()
	 * @see #buildFileUrl(String, String)
	 * @see AppUser#POCKETBASE_REMOTE_PROFILE_IMAGE_FIELD
	 */
	@Nullable
	public String uploadUserPhoto(@NonNull String serverId,
	                              @NonNull Bitmap bitmap,
	                              @NonNull String deviceId) {
		try {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
			MediaType contentType = MediaType.parse("image/png");
			byte[] byteArray = output.toByteArray();
			RequestBody imageBody = RequestBody.create(byteArray, contentType);
			
			String profilePicField = AppUser.POCKETBASE_REMOTE_PROFILE_IMAGE_FIELD;
			String filename = "profile_" + System.currentTimeMillis() + ".png";
			MultipartBody multipart =
				new MultipartBody.Builder()
					.setType(MultipartBody.FORM)
					.addFormDataPart(profilePicField, filename, imageBody)
					.build();
			
			Request request = new Request.Builder()
				.url(getRecordsUrl() + "/" + serverId)
				.patch(multipart)
				.addHeader("X-Device-Id", deviceId)
				.build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Upload failed code=" + response.code());
					return null;
				}
				
				String body = response.body().string();
				JSONObject json = new JSONObject(body);
				
				String fileName = json.getString(profilePicField);
				return buildFileUrl(serverId, fileName);
			}
		} catch (Exception error) {
			logger.error("Photo upload failed", error);
			return null;
		}
	}
}
