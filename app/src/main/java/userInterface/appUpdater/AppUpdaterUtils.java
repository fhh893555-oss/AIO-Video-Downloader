package userInterface.appUpdater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;

public final class AppUpdaterUtils extends PocketBaseClient {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String COLLECTION_NAME = "appUpdates";
	public static final String FIELD_VERSION_NAME = "currentVersionName";
	public static final String FIELD_VERSION_CODE = "currentVersionCode";
	public static final String FIELD_APK_FILE = "currentApkFile";
	public static final String FIELD_WHATS_NEW_JSON = "whatsNewJSON";
	
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	@Nullable
	public UpdateInfo fetchLatestUpdateInfo(@NonNull String deviceId) {
		String fields = "id," + FIELD_VERSION_CODE + "," + FIELD_VERSION_NAME + "," +
			FIELD_APK_FILE + "," + FIELD_WHATS_NEW_JSON;
		
		JSONObject record = query("id != ''", fields, deviceId);
		if (record == null) {
			logger.debug("No update record found or query failed.");
			return null;
		}
		
		try {
			String id = record.getString("id");
			int versionCode = record.getInt(FIELD_VERSION_CODE);
			String versionName = record.getString(FIELD_VERSION_NAME);
			String apkFileName = record.getString(FIELD_APK_FILE);
			String whatsNewJSON = record.optString(FIELD_WHATS_NEW_JSON);
			
			String apkFileUrl = API_ENDPOINT + "/api/files/" + COLLECTION_NAME +
				"/" + id + "/" + apkFileName;
			
			return new UpdateInfo(versionCode, versionName, apkFileUrl, whatsNewJSON);
		} catch (Exception error) {
			logger.error("Failed to parse update info", error);
			return null;
		}
	}
	
	public boolean isUpdateAvailable(int currentVersionCode, @Nullable UpdateInfo info) {
		return info != null && info.getVersionCode() > currentVersionCode;
	}
	
	public static class UpdateInfo {
		private int versionCode;
		private String versionName;
		private String apkFileUrl;
		private String whatsNewJSON;
		
		public UpdateInfo(int versionCode, String versionName,
		                  String apkFileUrl, String whatsNewJSON) {
			this.versionCode = versionCode;
			this.versionName = versionName;
			this.apkFileUrl = apkFileUrl;
			this.whatsNewJSON = whatsNewJSON;
		}
		
		public int getVersionCode() {
			return versionCode;
		}
		
		public void setVersionCode(int versionCode) {
			this.versionCode = versionCode;
		}
		
		public String getVersionName() {
			return versionName;
		}
		
		public void setVersionName(String versionName) {
			this.versionName = versionName;
		}
		
		public String getApkFileUrl() {
			return apkFileUrl;
		}
		
		public void setApkFileUrl(String apkFileUrl) {
			this.apkFileUrl = apkFileUrl;
		}
		
		public String getWhatsNewJSON() {
			return whatsNewJSON;
		}
		
		public void setWhatsNewJSON(String whatsNewJSON) {
			this.whatsNewJSON = whatsNewJSON;
		}
	}
}
