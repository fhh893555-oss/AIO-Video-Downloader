package dataRepo.appConfigs;

import androidx.annotation.Nullable;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.strings.StringHelper;
import dataRepo.dbManager.PocketBaseClient;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class AppConfigsHelper {
	private static final LoggerUtils logger = LoggerUtils.from(AppConfigsHelper.class);
	
	private static final String YTDLP_ENGINE = "YTDLP";
	private static final String NEWPIPE_ENGINE = "NEWPIPE";
	private static final String POCKETBASE_FIELD_ACTIVE_ENGINE = "activeDownloadEngine";
	private static final String POCKETBASE_FIELD_IS_NEWPIPE_DOWN = "isNewPipeDown";
	
	public static void syncDownloadEngineConfig(String deviceId, AppConfigs appConfigs) {
		ThreadTask.executeInBackground(() -> {
			DownloadEngineConfig engineConfig = getDownloadEngineConfigFromServer(deviceId);
			if (engineConfig == null) return;
			
			String activeEngine = engineConfig.getActiveDownloadEngine();
			Boolean isNewPipeDown = engineConfig.isNewPipeLibraryDown();
			
			if (isNewPipeDown) logger.debug("Newpipe extractor is broken");
			logger.debug("Active Download Engine: " + activeEngine);
			
			appConfigs.useYtdlpAsDefaultDownloader = !NEWPIPE_ENGINE.equals(activeEngine);
			appConfigs.isNewPipeUnavailable = isNewPipeDown;
		});
	}
	
	@Nullable
	private static DownloadEngineConfig getDownloadEngineConfigFromServer(@NotNull String deviceId) {
		String path = "/api/collections/ytdlpChannel/records?fields=downloadEngine";
		int desktopHttpUserAgent = R.string.code_browser_default_desktop_http_user_agent;
		Request request = new Request.Builder()
			.url(PocketBaseClient.API_ENDPOINT + path)
			.addHeader("X-Device-Id", deviceId)
			.addHeader("User-Agent", StringHelper.getText(desktopHttpUserAgent))
			.get()
			.build();
		
		OkHttpClient okHttpClient = HttpClientProvider.getOkHttpClient(3, 3);
		DownloadEngineConfig engineConfig = new DownloadEngineConfig();
		
		try (Response response = okHttpClient.newCall(request).execute()) {
			if (response.isSuccessful()) {
				String body = response.body().string();
				
				JSONObject json = new JSONObject(body);
				JSONArray items = json.getJSONArray("items");
				if (items.length() > 0) {
					JSONObject configSource = items.getJSONObject(0);
					String activeDownloadEngine = configSource.getString(POCKETBASE_FIELD_ACTIVE_ENGINE);
					boolean isNewPipeLibDown = configSource.getBoolean(POCKETBASE_FIELD_IS_NEWPIPE_DOWN);
					
					engineConfig.setActiveDownloadEngine(activeDownloadEngine);
					engineConfig.setNewPipeDown(isNewPipeLibDown);
					
					return engineConfig;
				}
			}
		} catch (Exception error) {
			logger.error("Get active download engine failed.", error);
			return null;
		}
		
		return null;
	}
	
	public static final class DownloadEngineConfig {
		private String activeDownloadEngine = YTDLP_ENGINE;
		private Boolean isNewPipeDown = false;
		
		public Boolean isNewPipeLibraryDown() {
			return isNewPipeDown;
		}
		
		public void setNewPipeDown(Boolean newPipeDown) {
			isNewPipeDown = newPipeDown;
		}
		
		public String getActiveDownloadEngine() {
			return activeDownloadEngine == null ?
				YTDLP_ENGINE : activeDownloadEngine;
		}
		
		public void setActiveDownloadEngine(String activeDownloadEngine) {
			this.activeDownloadEngine = activeDownloadEngine;
		}
		
	}
}
