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

/**
 * Helper class for synchronizing application configuration settings with the
 * remote PocketBase server. This class manages download engine configuration
 * retrieval and sync operations that determine which extraction library
 * (yt-dlp or NewPipe) should be active for video/audio downloads.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Fetches download engine configuration from remote server.</li>
 * <li>Updates local {@link AppConfigs} with active engine and NewPipe status.</li>
 * <li>Runs sync operations asynchronously on background threads.</li>
 * <li>Handles network errors gracefully without crashing the app.</li>
 * </ul>
 *
 * <p><strong>Engine constants:</strong>
 * <ul>
 * <li>{@link #YTDLP_ENGINE} - "yt-dlp" (default download engine).</li>
 * <li>{@link #NEWPIPE_ENGINE} - "newpipe" (alternative extraction library).</li>
 * </ul>
 *
 * @see DownloadEngineConfig
 * @see AppConfigs
 * @see #syncDownloadEngineConfig(String, AppConfigs)
 */
public final class AppConfigsHelper {
	private static final LoggerUtils logger = LoggerUtils.from(AppConfigsHelper.class);
	
	private static final String YTDLP_ENGINE = "YTDLP";
	private static final String NEWPIPE_ENGINE = "NEWPIPE";
	private static final String POCKETBASE_FIELD_DOWNLOAD_ENGINE = "downloadEngine";
	private static final String POCKETBASE_FIELD_ACTIVE_ENGINE = "activeDownloadEngine";
	private static final String POCKETBASE_FIELD_IS_NEWPIPE_DOWN = "isNewPipeDown";
	
	/**
	 * Synchronizes the local download engine configuration with remote server settings.
	 * This method runs on a background thread and fetches the latest engine configuration
	 * from the server, then updates the local {@link AppConfigs} instance accordingly.
	 *
	 * <p><strong>Sync behavior:</strong>
	 * <ul>
	 * <li>If the server returns {@code null} configuration, the method returns early.</li>
	 * <li>When the active engine is NOT {@link #NEWPIPE_ENGINE}, {@code useYtdlpAsDefaultDownloader}
	 *     is set to {@code true} (use yt-dlp).</li>
	 * <li>When the active engine IS {@link #NEWPIPE_ENGINE}, yt-dlp is not used as default.</li>
	 * <li>The {@code isNewPipeUnavailable} flag is set directly from the server response.</li>
	 * </ul>
	 *
	 * @param deviceId   The unique device identifier for the API request.
	 * @param appConfigs The local configuration object to update. Must not be null.
	 * @see #getDownloadEngineConfigFromServer(String)
	 * @see AppConfigs#useYtdlpAsDefaultDownloader
	 * @see AppConfigs#isNewPipeUnavailable
	 */
	public static void syncDownloadEngineConfig(String deviceId, AppConfigs appConfigs) {
		ThreadTask.executeInBackground(() -> {
			try {
				DownloadEngineConfig engineConfig = getDownloadEngineConfigFromServer(deviceId);
				if (engineConfig == null) return;
				
				String activeEngine = engineConfig.getActiveDownloadEngine();
				Boolean isNewPipeDown = engineConfig.isNewPipeLibraryDown();
				
				if (isNewPipeDown) logger.debug("Newpipe extractor is broken");
				logger.debug("Active Download Engine: " + activeEngine);
				
				appConfigs.useYtdlpAsDefaultDownloader = !NEWPIPE_ENGINE.equals(activeEngine);
				appConfigs.isNewPipeUnavailable = isNewPipeDown;
				appConfigs.save();
			} catch (Exception error) {
				logger.error("Error syncing download engine: ", error);
			}
		});
	}
	
	/**
	 * Fetches the download engine configuration from the remote PocketBase server.
	 * This method queries the "ytdlpChannel" collection for a record containing
	 * the active download engine and NewPipe availability status.
	 *
	 * <p><strong>Request details:</strong>
	 * GET request to {@code /api/collections/ytdlpChannel/records?fields=downloadEngine}
	 * with desktop User-Agent header. The response expects an "items" array containing
	 * the configuration object with fields defined by {@link #POCKETBASE_FIELD_ACTIVE_ENGINE}
	 * and {@link #POCKETBASE_FIELD_IS_NEWPIPE_DOWN}.
	 *
	 * @param deviceId The unique device identifier sent in the X-Device-Id header.
	 * @return A {@link DownloadEngineConfig} object populated with remote values,
	 * or {@code null} if the request fails, response is empty, or an error occurs.
	 * @see DownloadEngineConfig
	 * @see PocketBaseClient#API_ENDPOINT
	 */
	@Nullable
	private static DownloadEngineConfig getDownloadEngineConfigFromServer(@NotNull String deviceId) {
		String path = "/api/collections/" + POCKETBASE_FIELD_DOWNLOAD_ENGINE + "/records?fields=" +
			POCKETBASE_FIELD_ACTIVE_ENGINE + "," + POCKETBASE_FIELD_IS_NEWPIPE_DOWN;
		logger.debug("Pocketbase request url: " + PocketBaseClient.API_ENDPOINT + path);
		
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
					String activeDownloadEngine =
						configSource.getString(POCKETBASE_FIELD_ACTIVE_ENGINE);
					
					boolean isNewPipeLibDown =
						configSource.getBoolean(POCKETBASE_FIELD_IS_NEWPIPE_DOWN);
					
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
	
	/**
	 * Configuration class that manages the active download engine selection and
	 * availability status for the application. This nested static class stores
	 * which download engine is currently active (e.g., yt-dlp or NewPipe) and
	 * whether the NewPipe library is currently unavailable or down.
	 *
	 * <p><strong>Fields:</strong>
	 * <ul>
	 * <li>{@code activeDownloadEngine} - Current engine (default: {@link #YTDLP_ENGINE}).</li>
	 * <li>{@code isNewPipeDown} - True if NewPipe is unavailable.</li>
	 * </ul>
	 *
	 * <p>Default engine is yt-dlp. The getter for {@code activeDownloadEngine}
	 * returns {@link #YTDLP_ENGINE} as a fallback if the stored value is null.
	 *
	 * @see #YTDLP_ENGINE
	 * @see #NEWPIPE_ENGINE
	 */
	public static final class DownloadEngineConfig {
		private String activeDownloadEngine = YTDLP_ENGINE;
		private Boolean isNewPipeDown = false;
		
		/**
		 * Returns whether the NewPipe extraction library is currently unavailable or down.
		 * This flag is used to determine if the app should fall back to the alternative
		 * download engine (yt-dlp) when NewPipe fails to respond or is temporarily disabled.
		 *
		 * @return {@code true} if the NewPipe library is down, {@code false} otherwise.
		 * @see #setNewPipeDown(Boolean)
		 */
		public Boolean isNewPipeLibraryDown() {
			return isNewPipeDown;
		}
		
		/**
		 * Sets the availability status of the NewPipe extraction library. This method
		 * should be called when the library fails to respond or when it becomes
		 * available again after a failure.
		 *
		 * @param newPipeDown {@code true} to mark the library as down/unavailable,
		 *                    {@code false} to mark it as available.
		 * @see #isNewPipeLibraryDown()
		 */
		public void setNewPipeDown(Boolean newPipeDown) {
			isNewPipeDown = newPipeDown;
		}
		
		/**
		 * Returns the currently active download engine identifier. The default value
		 * is {@link #YTDLP_ENGINE}. If the stored engine value is {@code null},
		 * this method returns the default engine as a fallback to ensure system stability.
		 *
		 * @return The active download engine identifier (e.g., "yt-dlp" or "newpipe").
		 * Never returns {@code null}.
		 * @see #setActiveDownloadEngine(String)
		 * @see #YTDLP_ENGINE
		 * @see #NEWPIPE_ENGINE
		 */
		public String getActiveDownloadEngine() {
			return activeDownloadEngine == null ?
				YTDLP_ENGINE : activeDownloadEngine;
		}
		
		/**
		 * Sets the active download engine to be used for video/audio extraction.
		 * The provided identifier should match one of the predefined engine constants
		 * such as {@link #YTDLP_ENGINE} or {@link #NEWPIPE_ENGINE}.
		 *
		 * @param activeDownloadEngine The engine identifier to activate (e.g., "yt-dlp").
		 * @see #getActiveDownloadEngine()
		 * @see #YTDLP_ENGINE
		 * @see #NEWPIPE_ENGINE
		 */
		public void setActiveDownloadEngine(String activeDownloadEngine) {
			this.activeDownloadEngine = activeDownloadEngine;
		}
	}
}
