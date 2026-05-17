package sysModules.ytdlpWrapper.libs;

import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YtDlpChannelService {
	private static final LoggerUtils logger = LoggerUtils.from(YtDlpChannelService.class);
	
	@Nullable
	public static String getActiveYtDlpChannel() {
		String path = "/api/collections/ytdlp_channel/records?fields=active_ytdlp_channel";
		Request request = new Request.Builder()
			.url(PocketBaseClient.API_ENDPOINT + path)
			.get()
			.build();
		
		OkHttpClient okHttpClient = HttpClientProvider.getOkHttpClient(10, 10);
		try (Response response = okHttpClient.newCall(request).execute()) {
			if (response.isSuccessful()) {
				String body = response.body().string();
				
				JSONObject json = new JSONObject(body);
				JSONArray items = json.getJSONArray("items");
				if (items.length() > 0) {
					return items.getJSONObject(0)
						.getString("active_ytdlp_channel");
				}
			}
		} catch (Exception error) {
			logger.error("Get active ytdlp channel failed.", error);
			return null;
		}
		
		return null;
	}
}
