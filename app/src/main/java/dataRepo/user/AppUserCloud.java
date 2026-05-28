package dataRepo.user;

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

public class AppUserCloud extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@NonNull
	@Override
	protected String getCollectionName() {
		return AppUser.POCKETBASE_COLLECTION_NAME;
	}
	
	@Nullable
	public JSONObject createUser(@NonNull JSONObject data) {
		return post(data);
	}
	
	@Nullable
	public JSONObject updateUser(@NonNull String serverId,
	                             @NonNull JSONObject data) {
		return patch(serverId, data);
	}
	
	public boolean updateField(@NonNull String serverId,
	                           @NonNull String field,
	                           @NonNull Object value) throws JSONException {
		JSONObject json = new JSONObject();
		json.put(field, value);
		return patch(serverId, json) != null;
	}
	
	@NonNull
	public String buildFileUrl(@NonNull String recordId,
	                           @NonNull String fileName) {
		return API_ENDPOINT + "/api/files/" +
			AppUser.POCKETBASE_COLLECTION_NAME +
			"/" + recordId + "/" + fileName;
	}
	
	@Nullable
	public Object getField(@NonNull String serverId,
	                       @NonNull String field, @NonNull String deviceId) {
		JSONObject result = query("id='" + serverId + "'", field, deviceId);
		return result != null ? result.opt(field) : null;
	}
	
	@Nullable
	public JSONObject getUserByDeviceId(@NonNull String deviceId) {
		String deviceIdField = AppUser.POCKETBASE_REMOTE_DEVICE_ID_FIELD;
		return query(deviceIdField + "='" + deviceId + "'", null, deviceId);
	}
	
	@Nullable
	public String uploadUserPhoto(@NonNull String serverId,
	                              @NonNull Bitmap bitmap) {
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
