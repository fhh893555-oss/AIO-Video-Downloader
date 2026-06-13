package userInterface.appUpdater;

import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.Serializable;

import coreUtils.library.networks.HttpClientProvider;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.dbManager.PocketBaseClient;

public final class AppUpdaterUtils extends PocketBaseClient {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());

	public static final String COLLECTION_NAME = "appUpdates";
	public static final String FIELD_VERSION_NAME = "currentVersionName";
	public static final String FIELD_VERSION_CODE = "currentVersionCode";

	public static final String FIELD_APK_FILE_32BIT = "currentApkFile32Bit";
	public static final String FIELD_APK_FILE_64BIT = "currentApkFile64Bit";

	public static final String FIELD_WHATS_NEW_JSON = "whatsNewJSON";
    public static final String FIELD_RELEASE_DATE = "releaseDate";
    public static final String FIELD_APK_FILE_SIZE = "apkFileSize";

	public static final String FIELD_APK_FILE_HASH_CODE_32BIT = "apkFileHashFor32Bit";
	public static final String FIELD_APK_FILE_HASH_CODE_64BIT = "apkFileHashFor64Bit";

    @NonNull
	@Override
	protected String getCollectionName() {
		return COLLECTION_NAME;
	}

	@Nullable
	public UpdateInfo fetchLatestUpdateInfo(@NonNull String deviceId) {
		logger.debug("Fetching latest update info for device: " + deviceId);
		setCustomOKHttpClient(HttpClientProvider.getOkHttpClient(2, 2));
		
		boolean is64Bit = is64BitDevice();
		String apkField = is64Bit ? FIELD_APK_FILE_64BIT : FIELD_APK_FILE_32BIT;
        String hashField = is64Bit ? FIELD_APK_FILE_HASH_CODE_64BIT :
                FIELD_APK_FILE_HASH_CODE_32BIT;
		
		String fields = "id," + FIELD_VERSION_CODE + "," + FIELD_VERSION_NAME + "," +
                apkField + "," + hashField + "," + FIELD_WHATS_NEW_JSON + "," +
                FIELD_RELEASE_DATE + "," + FIELD_APK_FILE_SIZE;
		
		logger.debug("Detected architecture: " + (is64Bit ? "64-bit" : "32-bit"));
		logger.debug("Query fields: " + fields);
		JSONObject record = query("id != ''", fields, deviceId);
		
		if (record == null) {
			logger.debug("No update record found or query failed.");
			return null;
		}
		
		logger.debug("Update record retrieved successfully");
		
		try {
			String id = record.getString("id");
			int versionCode = record.getInt(FIELD_VERSION_CODE);
			String versionName = record.getString(FIELD_VERSION_NAME);
			String apkFileName = record.getString(apkField);
			String apkHash = record.getString(hashField);
			String whatsNewJSON = record.optString(FIELD_WHATS_NEW_JSON);
            String releaseDate = record.optString(FIELD_RELEASE_DATE);
            String apkFileSize = record.optString(FIELD_APK_FILE_SIZE);

            logger.debug("Parsed update - ID: " + id + ",\nVersion: " + versionName +
                    " (" + versionCode + "),\nAPK: " + apkFileName + ",\nRelease date: " +
                    releaseDate + ",\nWhats new: " + whatsNewJSON +
                    ",\nApk File size: " + apkFileSize);
			
			String apkFileUrl = API_ENDPOINT + "/api/files/" + COLLECTION_NAME +
				"/" + id + "/" + apkFileName;
			
			logger.debug("Constructed APK URL: " + apkFileUrl);

            return new UpdateInfo(versionCode, versionName, apkFileUrl,
                    apkHash, whatsNewJSON, releaseDate, apkFileSize);

		} catch (Exception error) {
			logger.error("Failed to parse update info", error);
			return null;
		}
	}

	private boolean is64BitDevice() {
		return Build.SUPPORTED_64_BIT_ABIS != null &&
			Build.SUPPORTED_64_BIT_ABIS.length > 0;
	}

    public static boolean isUpdateAvailable(@NotNull Context context,
                                            @Nullable UpdateInfo info) {
		long currentVersionCode = VersionInfo.getVersionCode(context);
		return info != null && info.getVersionCode() > currentVersionCode;
	}

    public static class UpdateInfo implements Serializable {
        private final int versionCode;
        private final String versionName;
        private final String apkFileUrl;
        private final String apkFileHash;
        private final String whatsNewJSON;
        private final String releaseDate;
        private final String apkFileSize;

        public UpdateInfo(int versionCode,
                          String versionName,
                          String apkFileUrl,
                          String apkFileHash,
                          String whatsNewJSON,
                          String releaseDate,
                          String apkFileSize) {
			this.versionCode = versionCode;
			this.versionName = versionName;
			this.apkFileUrl = apkFileUrl;
			this.apkFileHash = apkFileHash;
			this.whatsNewJSON = whatsNewJSON;
            this.releaseDate = releaseDate;
            this.apkFileSize = apkFileSize;
		}

        public int getVersionCode() {
            return versionCode;
        }

        public String getVersionName() {
			return versionName;
		}

        public String getApkFileUrl() {
			return apkFileUrl;
		}

        public String getApkFileHash() {
			return apkFileHash;
		}

        public String getWhatsNewJSON() {
			return whatsNewJSON;
		}

        public String getReleaseDate() {
            return releaseDate;
        }

        public String getApkFileSize() {
            return apkFileSize;
        }
	}
}
