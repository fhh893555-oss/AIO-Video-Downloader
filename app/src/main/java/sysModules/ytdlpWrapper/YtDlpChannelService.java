package sysModules.ytdlpWrapper;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Service class responsible for retrieving the active yt-dlp release channel configuration
 * from the remote PocketBase backend.
 * <p>
 * This service determines which update track (e.g., stable, nightly) should be used
 * when performing yt-dlp binary updates or version checks.
 */
public class YtDlpChannelService {
	
	private static final LoggerUtils logger = LoggerUtils.from(YtDlpChannelService.class);
	
	/**
	 * Retrieves the currently active yt-dlp release channel from the backend database.
	 * <p>
	 * This method performs a synchronous network request to the PocketBase API to fetch
	 * the configured update channel (e.g., "stable", "nightly").
	 *
	 * @return The name of the active channel if found; {@code null} if the request fails,
	 * the response is empty, or an error occurs during parsing.
	 */
	@Nullable
	public static String getActiveYtDlpChannel(@NotNull String deviceId) {
		String path = "/api/collections/ytdlpChannel/records?fields=activeChannel";
		Request request = new Request.Builder()
			.url(PocketBaseClient.API_ENDPOINT + path)
			.addHeader("X-Device-Id", deviceId)
			.get()
			.build();
		
		OkHttpClient okHttpClient = HttpClientProvider.getOkHttpClient(3, 3);
		try (Response response = okHttpClient.newCall(request).execute()) {
			if (response.isSuccessful()) {
				String body = response.body().string();
				
				JSONObject json = new JSONObject(body);
				JSONArray items = json.getJSONArray("items");
				if (items.length() > 0) {
					return items.getJSONObject(0)
						.getString("activeChannel");
				}
			}
		} catch (Exception error) {
			logger.error("Get active ytdlp channel failed.", error);
			return null;
		}
		
		return null;
	}
}
