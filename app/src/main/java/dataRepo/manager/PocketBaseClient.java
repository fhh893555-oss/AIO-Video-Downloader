package dataRepo.manager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public abstract class PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	protected OkHttpClient httpClient = HttpClientProvider.getOkHttpClient(10, 10);
	protected final MediaType jsonMedia = MediaType.parse("application/json");
	
	public static final String API_ENDPOINT = "https://cloud.tubeaio.com";
	
	@NonNull
	protected String recordsUrl() {
		return API_ENDPOINT + "/api/collections/" + getCollectionName() + "/records";
	}
	
	@NonNull
	protected abstract String getCollectionName();
	
	@Nullable
	protected JSONObject query(@NonNull String filter,
	                           @Nullable String fields, @NonNull String deviceId) {
		try {
			String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8);
			StringBuilder url = new StringBuilder(recordsUrl())
				.append("?filter=")
				.append(encodedFilter);
			
			if (fields != null && !fields.isEmpty()) {
				url.append("&fields=").append(fields);
			}
			
			Request request = new Request.Builder()
				.url(url.toString())
				.addHeader("X-Device-Id", deviceId)
				.get()
				.build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Query failed code=" + response.code());
					return null;
				}
				
				String body = response.body().string();
				JSONArray items = new JSONObject(body).optJSONArray("items");
				if (items == null || items.length() == 0) return null;
				return items.getJSONObject(0);
			}
			
		} catch (Exception error) {
			logger.error("Query failed.", error);
			return null;
		}
	}
	
	@Nullable
	protected JSONObject patch(@NonNull String recordId,
	                           @NonNull JSONObject data) {
		try {
			RequestBody requestBody = RequestBody.create(data.toString(), jsonMedia);
			Request request = new Request.Builder()
				.url(recordsUrl() + "/" + recordId)
				.patch(requestBody).build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Patch failed code=" + response.code());
					return null;
				}
				
				return new JSONObject(response.body().string());
			}
		} catch (Exception error) {
			logger.error("Patch failed.", error);
			return null;
		}
	}
	
	@Nullable
	protected JSONObject post(@NonNull JSONObject data) {
		try {
			RequestBody requestBody = RequestBody.create(data.toString(), jsonMedia);
			Request request = new Request.Builder()
				.url(recordsUrl()).post(requestBody).build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Post failed code=" + response.code());
					return null;
				}
				
				return new JSONObject(response.body().string());
			}
		} catch (Exception error) {
			logger.error("Post failed.", error);
			return null;
		}
	}
	
	@Nullable
	protected JSONObject post(@NonNull RequestBody requestBody) {
		try {
			Request request = new Request.Builder()
				.url(recordsUrl()).post(requestBody).build();
			
			try (Response response = httpClient.newCall(request).execute()) {
				if (!response.isSuccessful()) {
					logger.debug("Post failed code=" + response.code());
					return null;
				}
				
				return new JSONObject(response.body().string());
			}
		} catch (Exception error) {
			logger.error("Post failed.", error);
			return null;
		}
	}
	
	public void setCustomOKHttpClient(@NonNull OkHttpClient httpClient) {
		this.httpClient = httpClient;
	}
	
}