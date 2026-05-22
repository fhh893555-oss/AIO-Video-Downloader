package sysModules.ytdlpWrapper;

import androidx.annotation.Nullable;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
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
		int desktopHttpUserAgent = R.string.code_browser_default_desktop_http_user_agent;
		Request request = new Request.Builder()
			.url(PocketBaseClient.API_ENDPOINT + path)
			.addHeader("X-Device-Id", deviceId)
			.addHeader("User-Agent", StringHelper.getText(desktopHttpUserAgent))
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
	
	/**
	 * Retrieves the active yt-dlp channel identifier for a given device from the PocketBase server.
	 * <p>
	 * This method performs a direct HTTP GET request using HttpURLConnection to fetch the
	 * active channel configuration from the ytdlpChannel collection. It includes a device
	 * identifier header for analytics or authorization purposes and parses the JSON response
	 * to extract the active channel value.
	 * </p>
	 *
	 * <p><b>Request Details:</b>
	 * <ul>
	 *   <li>Endpoint: /api/collections/ytdlpChannel/records?fields=activeChannel</li>
	 *   <li>Method: GET</li>
	 *   <li>Timeouts: 15 seconds for both connect and read</li>
	 *   <li>Headers: X-Device-Id, User-Agent (mobile), Accept (application/json)</li>
	 * </ul>
	 * </p>
	 *
	 * @param deviceId a unique identifier for the requesting device, passed in the X-Device-Id header
	 * @return the active channel string if found, or null if no record exists, the request fails,
	 * or the response cannot be parsed
	 */
	@Nullable
	public static String getActiveYtDlpChannelNonOkHttp(@NotNull String deviceId) {
		String path = "/api/collections/ytdlpChannel/records?fields=activeChannel";
		HttpURLConnection connection = null;
		BufferedReader reader = null;
		
		try {
			URL url = new URL(PocketBaseClient.API_ENDPOINT + path);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(15000);
			connection.setReadTimeout(15000);
			
			connection.setRequestProperty("X-Device-Id", deviceId);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 10; Mobile)");
			connection.setRequestProperty("Accept", "application/json");
			
			int responseCode = connection.getResponseCode();
			
			if (responseCode == HttpURLConnection.HTTP_OK) {
				reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder responseOutput = new StringBuilder();
				String line;
				
				while ((line = reader.readLine()) != null) {
					responseOutput.append(line);
				}
				
				JSONObject json = new JSONObject(responseOutput.toString());
				JSONArray items = json.getJSONArray("items");
				if (items.length() > 0) {
					return items.getJSONObject(0).getString("activeChannel");
				}
			} else {
				logger.error("Server returned non-200 status code: " + responseCode);
			}
			
		} catch (Exception error) {
			logger.error("Get active ytdlp channel failed using HttpURLConnection.", error);
			return null;
		} finally {
			if (reader != null) {
				try {reader.close();} catch (Exception ignored) {}
			}
			if (connection != null) {
				connection.disconnect();
			}
		}
		
		return null;
	}
}
