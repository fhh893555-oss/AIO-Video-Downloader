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

/**
 * Utility class for fetching application update information from a PocketBase
 * backend. This client extends {@link PocketBaseClient} and provides methods
 * to retrieve the latest available APK version, download URL, and metadata
 * such as changelog, release date, and file size.
 *
 * <p><strong>Core features:</strong>
 * <ul>
 * <li>Automatic architecture detection (32-bit vs 64-bit) for APK selection.</li>
 * <li>Constructs full APK download URLs from PocketBase file references.</li>
 * <li>Provides static helper {@link #isUpdateAvailable(Context, UpdateInfo)}.</li>
 * </ul>
 *
 * @see PocketBaseClient
 * @see UpdateInfo
 * @see #fetchLatestUpdateInfo(String)
 */
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
	
	/**
	 * Returns the name of the PocketBase collection used for storing update
	 * information. This collection contains records with version codes, APK
	 * file references, hashes, changelogs, and release metadata.
	 *
	 * @return The collection name constant {@link #COLLECTION_NAME}.
	 */
	@Override protected @NonNull String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	/**
	 * Fetches the latest application update information from the PocketBase server.
	 * This method performs a query against the updates collection, selecting
	 * fields appropriate for the device's architecture (32-bit or 64-bit).
	 *
	 * <p><strong>Request details:</strong>
	 * <ul>
	 * <li>Uses a 2-second connection and read timeout via custom OkHttpClient.</li>
	 * <li>Automatically selects 32-bit or 64-bit APK fields based on device architecture.</li>
	 * <li>Queries for the first available record (ordered by version code desc).</li>
	 * <li>Constructs the full APK download URL using the PocketBase file API.</li>
	 * </ul>
	 *
	 * <p>If no update record is found or parsing fails, the method returns {@code null}.
	 *
	 * @param deviceId The unique device identifier sent in the X-Device-Id header.
	 * @return An {@link UpdateInfo} object containing the latest update metadata,
	 * or {@code null} if no update is available or an error occurs.
	 * @see #is64BitDevice()
	 * @see #COLLECTION_NAME
	 * @see #FIELD_VERSION_CODE
	 * @see #FIELD_VERSION_NAME
	 */
	@Nullable public UpdateInfo fetchLatestUpdateInfo(@NonNull String deviceId) {
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
	
	/**
	 * Determines whether the current device is running a 64-bit architecture.
	 * This method checks the {@link Build#SUPPORTED_64_BIT_ABIS} array, which
	 * contains the list of 64-bit ABIs supported by the device. If the array
	 * is non-null and contains at least one entry, the device is considered 64-bit.
	 *
	 * <p>This information can be used to select appropriate APK variants or
	 * native libraries when multiple architecture versions are available.
	 *
	 * @return {@code true} if the device supports 64-bit ABIs, {@code false} otherwise.
	 * @see Build#SUPPORTED_64_BIT_ABIS
	 */
	private boolean is64BitDevice() {
		return Build.SUPPORTED_64_BIT_ABIS != null &&
			Build.SUPPORTED_64_BIT_ABIS.length > 0;
	}
	
	/**
	 * Checks whether an application update is available by comparing the version
	 * code of the provided update information with the currently installed version.
	 *
	 * <p>An update is considered available if:
	 * <ul>
	 * <li>The UpdateInfo object is not null.</li>
	 * <li>The update's version code is greater than the currently installed version code.</li>
	 * </ul>
	 *
	 * <p>Version names are not compared as they are not guaranteed to follow
	 * semantic versioning; version codes are the definitive comparison metric.
	 *
	 * @param context The application context used to retrieve the current version code.
	 * @param info    The UpdateInfo object containing the available update metadata,
	 *                or {@code null} if no update information was fetched.
	 * @return {@code true} if an update is available, {@code false} otherwise.
	 * @see VersionInfo#getVersionCode(android.content.Context)
	 */
	public static boolean isUpdateAvailable(@NotNull Context context,
	                                        @Nullable UpdateInfo info) {
		long currentVersionCode = VersionInfo.getVersionCode(context);
		return info != null && info.getVersionCode() > currentVersionCode;
	}
	
	/**
	 * Data class representing application update metadata retrieved from a remote
	 * server. This class encapsulates all information needed to inform the user
	 * about an available update, download the APK file, and verify its integrity
	 * using SHA-256 hashing.
	 *
	 * <p><strong>Fields:</strong>
	 * <ul>
	 * <li>{@code versionCode} – Integer version code (increments with each release).</li>
	 * <li>{@code versionName} – Human-readable version name (e.g., "2.1.0").</li>
	 * <li>{@code apkFileUrl} – Download URL for the APK file.</li>
	 * <li>{@code apkFileHash} – SHA-256 hash for integrity verification.</li>
	 * <li>{@code whatsNewJSON} – JSON changelog of new features and bug fixes.</li>
	 * <li>{@code releaseDate} – Release date in human-readable format.</li>
	 * <li>{@code apkFileSize} – Formatted file size (e.g., "12.5 MB").</li>
	 * </ul>
	 *
	 * <p>This class implements {@link Serializable} to allow the UpdateInfo object
	 * to be passed between activities and services via Intent extras, enabling
	 * seamless transition from update check to download and installation screens.
	 *
	 * @see Serializable
	 * @see ApkDownloader
	 * @see AppUpdaterViewModel
	 */
	public static class UpdateInfo implements Serializable {
		
		private final int versionCode;
		private final String versionName;
		private final String apkFileUrl;
		private final String apkFileHash;
		private final String whatsNewJSON;
		private final String releaseDate;
		private final String apkFileSize;
		
		/**
		 * Constructs a new UpdateInfo object containing all metadata for an
		 * application update. This data is typically fetched from a remote server
		 * and used to inform the user about available updates, manage downloads,
		 * and verify file integrity.
		 *
		 * @param versionCode  The integer version code (increments with each release).
		 * @param versionName  The human-readable version name (e.g., "2.1.0").
		 * @param apkFileUrl   The URL from which the APK file can be downloaded.
		 * @param apkFileHash  The SHA-256 hash of the APK for integrity verification.
		 * @param whatsNewJSON JSON string containing the changelog of new features/bug fixes.
		 * @param releaseDate  The release date of this update (human-readable format).
		 * @param apkFileSize  The file size as a human-readable string (e.g., "12.5 MB").
		 */
		public UpdateInfo(int versionCode, @NonNull String versionName,
		                  @NonNull String apkFileUrl, @NonNull String apkFileHash,
		                  @NonNull String whatsNewJSON, @NonNull String releaseDate,
		                  @NonNull String apkFileSize) {
			this.versionCode = versionCode;
			this.versionName = versionName;
			this.apkFileUrl = apkFileUrl;
			this.apkFileHash = apkFileHash;
			this.whatsNewJSON = whatsNewJSON;
			this.releaseDate = releaseDate;
			this.apkFileSize = apkFileSize;
		}
		
		/**
		 * Returns the version code of the update. Version codes are integers that
		 * increase with each release and are used to determine whether an update
		 * is newer than the currently installed version.
		 *
		 * @return The version code (e.g., 15, 42, 103).
		 * @see #getVersionName()
		 */
		public int getVersionCode() {
			return versionCode;
		}
		
		/**
		 * Returns the version name of the update. This is a human-readable string
		 * representing the release version (e.g., "1.2.3", "2.0.0-beta").
		 *
		 * @return The version name string.
		 * @see #getVersionCode()
		 */
		public String getVersionName() {
			return versionName;
		}
		
		/**
		 * Returns the URL from which the APK file can be downloaded. This URL is
		 * used by the {@link ApkDownloader} to fetch the update package.
		 *
		 * @return The download URL as a string.
		 */
		public String getApkFileUrl() {
			return apkFileUrl;
		}
		
		/**
		 * Returns the SHA-256 hash of the APK file for integrity verification.
		 * After download, the file's hash is computed and compared against this
		 * value to ensure the download is valid and untampered.
		 *
		 * @return The SHA-256 hash string (hexadecimal format).
		 */
		public String getApkFileHash() {
			return apkFileHash;
		}
		
		/**
		 * Returns a JSON string containing the "What's New" changelog for this update.
		 * The JSON typically includes bullet points or structured data about new
		 * features, bug fixes, and improvements.
		 *
		 * @return The changelog JSON string.
		 */
		public String getWhatsNewJSON() {
			return whatsNewJSON;
		}
		
		/**
		 * Returns the release date of this update as a human-readable string.
		 * The format may vary but typically follows "MMM dd, yyyy" (e.g., "Jun 15, 2024").
		 *
		 * @return The release date string.
		 */
		public String getReleaseDate() {
			return releaseDate;
		}
		
		/**
		 * Returns the file size of the APK as a human-readable string
		 * (e.g., "12.5 MB", "45.2 MB"). This value is displayed in the UI
		 * to inform users of the download size before they proceed.
		 *
		 * @return The formatted file size string.
		 */
		public String getApkFileSize() {
			return apkFileSize;
		}
	}
}
