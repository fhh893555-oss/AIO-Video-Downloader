package userInterface.appUpdater;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.Serializable;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.manager.PocketBaseClient;

/**
 * Utility class for checking and retrieving application updates from a PocketBase backend.
 * <p>
 * This class extends {@link PocketBaseClient} to interact with the PocketBase collection
 * that stores application version information. It provides methods to fetch the latest
 * update metadata from the server and determine whether a newer version is available
 * for the currently running application instance.
 * </p>
 *
 * <p><b>Typical Usage Example:</b>
 * <pre>
 * AppUpdaterUtils updater = new AppUpdaterUtils();
 * UpdateInfo latestUpdate = updater.fetchLatestUpdateInfo("device-12345");
 *
 * if (updater.isUpdateAvailable(BuildConfig.VERSION_CODE, latestUpdate)) {
 *     // Show update dialog and start download
 *     String downloadUrl = latestUpdate.getApkFileUrl();
 * }
 * </pre>
 * </p>
 *
 * <p><b>PocketBase Collection Schema Requirements:</b>
 * The "appUpdates" collection must contain records with the following fields:
 * <ul>
 *   <li>currentVersionName (String) - Human-readable version (e.g., "2.1.0")</li>
 *   <li>currentVersionCode (Number) - Integer version code for comparison</li>
 *   <li>currentApkFile (File) - The APK file attachment</li>
 *   <li>whatsNewJSON (String, optional) - JSON changelog data</li>
 * </ul>
 * </p>
 */
public final class AppUpdaterUtils extends PocketBaseClient {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * The PocketBase collection name that stores application update records.
	 * <p>
	 * This collection should contain one or more records where each record represents
	 * an available application version. Typically, the record with the highest
	 * {@link #FIELD_VERSION_CODE} value is considered the latest available update.
	 * The collection structure must match the field constants defined in this class.
	 * </p>
	 */
	public static final String COLLECTION_NAME = "appUpdates";
	
	/**
	 * Field name for the user-facing version string in the PocketBase collection.
	 * <p>
	 * Values in this field follow semantic versioning format (e.g., "1.0.0", "2.3.1-beta")
	 * and are displayed to users in update dialogs. This field is required for each record.
	 * </p>
	 */
	public static final String FIELD_VERSION_NAME = "currentVersionName";
	
	/**
	 * Field name for the numeric version code in the PocketBase collection.
	 * <p>
	 * This integer field is used for version comparison logic. Higher values indicate
	 * newer releases. Must match or correspond to Android's {@code versionCode} in
	 * the app's build.gradle file. Required field for all records.
	 * </p>
	 */
	public static final String FIELD_VERSION_CODE = "currentVersionCode";
	
	/**
	 * Field name for the APK file attachment in the PocketBase collection.
	 * <p>
	 * This field stores the actual application package file as a PocketBase file attachment.
	 * The field type should be "File" in the PocketBase collection schema. The file name
	 * is used to construct the downloadable URL via the PocketBase file API.
	 * </p>
	 */
	public static final String FIELD_APK_FILE = "currentApkFile";
	
	/**
	 * Field name for the JSON changelog data in the PocketBase collection.
	 * <p>
	 * This optional field contains structured information about what's new in the update.
	 * Can be null or empty for records that don't provide changelog details. When present,
	 * the JSON should follow the format expected by {@link UpdateInfo}.
	 * </p>
	 */
	public static final String FIELD_WHATS_NEW_JSON = "whatsNewJSON";
	
	/**
	 * Returns the name of the PocketBase collection used for update records.
	 * <p>
	 * This method overrides the abstract method from {@link PocketBaseClient} to
	 * provide the specific collection name used by this updater utility. The collection
	 * name must exactly match the one created in the PocketBase admin interface.
	 * </p>
	 *
	 * @return the collection name string, which is {@link #COLLECTION_NAME}
	 */
	@NonNull
	@Override
	protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	/**
	 * Retrieves the latest application update information from the server.
	 * <p>
	 * This method queries the PocketBase collection for available update records and
	 * constructs an {@link UpdateInfo} object from the most recent entry. It builds
	 * the complete download URL by combining the API endpoint with the collection name,
	 * record ID, and the stored APK file name.
	 * </p>
	 *
	 * <p><b>Query Behavior:</b>
	 * The method uses a filter condition {@code "id != ''"} to fetch the first available
	 * record. For production use, consider modifying the query to order by version code
	 * descending and limit to 1 record to ensure the latest update is always returned.
	 * </p>
	 *
	 * @param deviceId a unique identifier for the requesting device. This parameter is
	 *                 passed to the parent query method for potential filtering or logging.
	 *                 Should not be null.
	 * @return an {@link UpdateInfo} object containing the latest update details if found
	 * and successfully parsed; returns {@code null} if no record exists, the query fails,
	 * or JSON parsing encounters an error
	 * @throws IllegalArgumentException if deviceId is null (due to @NonNull annotation)
	 */
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
	
	/**
	 * Determines whether a newer application version is available for installation.
	 * <p>
	 * This method performs a simple version code comparison between the currently
	 * running application and the fetched update information. An update is considered
	 * available if the server's version code is strictly greater than the current one.
	 * </p>
	 *
	 * <p><b>Comparison Logic:</b>
	 * The method returns {@code false} in the following scenarios:
	 * <ul>
	 *   <li>The UpdateInfo parameter is null (no update data available)</li>
	 *   <li>The server version code is less than or equal to the current version code</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * int currentVersion = BuildConfig.VERSION_CODE;
	 * UpdateInfo latest = updater.fetchLatestUpdateInfo(deviceId);
	 *
	 * if (AppUpdaterUtils.isUpdateAvailable(context, latest)) {
	 *     // Prompt user to download version " + latest.getVersionName()
	 *     startUpdateDownload(latest.getApkFileUrl());
	 * }
	 * </pre>
	 * </p>
	 *
	 * @param context the application context used to retrieve the current installed app's version code
	 * @param info    the {@link UpdateInfo} object fetched from the server,
	 *                or {@code null} if no update data is available
	 * @return {@code true} if the server has a newer version available (info.versionCode > currentVersionCode);
	 * {@code false} otherwise or if info is null
	 */
	public static boolean isUpdateAvailable(@NotNull Context context, @Nullable UpdateInfo info) {
		long currentVersionCode = VersionInfo.getVersionCode(context);
		return info != null && info.getVersionCode() > currentVersionCode;
	}
	
	/**
	 * Represents version update information for an Android application.
	 * <p>
	 * This immutable data container holds all necessary details about a specific application update,
	 * including version identifiers, download location, and changelog information. Instances of this
	 * class are typically created by an update checker service when a new version is available.
	 * </p>
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * UpdateInfo update = new UpdateInfo(
	 *     42,
	 *     "2.1.0",
	 *     "<a href="https://example.com/app-v2.1.0.apk">...</a>",
	 *     "{\"features\":[\"Dark mode\",\"Improved performance\"]}"
	 * );
	 *
	 * int currentVersion = update.getVersionCode();
	 * String downloadUrl = update.getApkFileUrl();
	 * </pre>
	 * </p>
	 */
	public static class UpdateInfo implements Serializable {
		private int versionCode;
		private String versionName;
		private String apkFileUrl;
		private String whatsNewJSON;
		
		/**
		 * Constructs a new UpdateInfo instance with complete update details.
		 * <p>
		 * All parameters are required for a valid update definition. The version code is used for
		 * numerical comparison (higher means newer), while version name is for display purposes.
		 * The APK URL must point to a valid downloadable Android package file, and the whatsNewJSON
		 * should follow the expected changelog schema.
		 * </p>
		 *
		 * @param versionCode  the numeric version identifier (e.g., 42). Higher values indicate newer releases.
		 * @param versionName  the human-readable version string (e.g., "2.1.0" or "2.1.0-beta").
		 * @param apkFileUrl   the complete HTTPS URL where the APK file can be downloaded.
		 * @param whatsNewJSON a JSON formatted string containing changelog or feature description.
		 *                     May follow format: {"features":[...], "bugfixes":[...]}
		 */
		public UpdateInfo(int versionCode, String versionName,
		                  String apkFileUrl, String whatsNewJSON) {
			this.versionCode = versionCode;
			this.versionName = versionName;
			this.apkFileUrl = apkFileUrl;
			this.whatsNewJSON = whatsNewJSON;
		}
		
		/**
		 * Retrieves the numeric version code of this update.
		 * <p>
		 * Version code is an integer value that increases monotonically with each release.
		 * It is used by the system to determine whether an update is newer than the currently
		 * installed version. Unlike version name, this value is not shown to end users.
		 * </p>
		 *
		 * @return the integer version code (e.g., 42, 100, 1001)
		 */
		public int getVersionCode() {
			return versionCode;
		}
		
		/**
		 * Assigns a numeric version identifier to this update instance.
		 * <p>
		 * The version code must be greater than the currently installed version's code
		 * for the update to be considered valid. Typical values start at 1 and increase
		 * by 1 or more with each release. For beta/alpha builds, consider using a separate
		 * code range or prefix (e.g., 10000+ for betas).
		 * </p>
		 *
		 * @param versionCode the numeric version code to assign.
		 *                    Must be a positive integer greater than previous version's code.
		 */
		public void setVersionCode(int versionCode) {
			this.versionCode = versionCode;
		}
		
		/**
		 * Retrieves the user-friendly version name string.
		 * <p>
		 * This value is displayed to users in update dialogs, about screens, and store listings.
		 * It follows semantic versioning typically (e.g., "1.0.0", "2.3.1-beta"), but can use
		 * any string format that is meaningful to end users. This field is for presentation only
		 * and should not be used for version comparison logic.
		 * </p>
		 *
		 * @return the string representing the version name, never null if properly initialized
		 */
		public String getVersionName() {
			return versionName;
		}
		
		/**
		 * Updates the display version name for this update information.
		 * <p>
		 * The version name should be human-readable and follow a consistent format across
		 * updates (e.g., "Major.Minor.Patch"). Including build metadata or pre-release
		 * identifiers (like "-alpha", "-rc1") is acceptable. Changes to this field after
		 * object creation should be reflected in any UI components showing the update info.
		 * </p>
		 *
		 * @param versionName the human-readable version string to set.
		 *                    Recommended format: "X.Y.Z" or "X.Y.Z-status"
		 */
		public void setVersionName(String versionName) {
			this.versionName = versionName;
		}
		
		/**
		 * Provides the direct download URL for the application APK file.
		 * <p>
		 * The returned URL should point to a secure HTTPS endpoint serving the Android
		 * Package (APK) file for this specific version. The URL must be accessible without
		 * additional authentication in most production scenarios, or should include necessary
		 * tokens as query parameters if authentication is required.
		 * </p>
		 *
		 * @return the complete URL string (e.g., "<a href="https://cdn.example.com/app-v2.1.0.apk">...</a>")
		 */
		public String getApkFileUrl() {
			return apkFileUrl;
		}
		
		/**
		 * Defines the download location for this update's APK file.
		 * <p>
		 * The URL must be valid and reachable when the update process initiates. Consider
		 * using CDN URLs with version-specific paths to enable caching. HTTPS is strongly
		 * recommended to ensure download integrity. The file referenced should match the
		 * version information provided in this UpdateInfo instance.
		 * </p>
		 *
		 * @param apkFileUrl the direct HTTPS/HTTP URL string where the APK file is hosted.
		 *                   Must be non-null and point to a valid .apk file.
		 */
		public void setApkFileUrl(String apkFileUrl) {
			this.apkFileUrl = apkFileUrl;
		}
		
		/**
		 * Retrieves the JSON formatted changelog or "What's New" content.
		 * <p>
		 * The JSON string typically contains structured information about new features,
		 * bug fixes, and improvements in this update. Common schemas include arrays of
		 * feature descriptions or categorized changes. Parse this using a JSON library
		 * like Gson or org.json to display formatted release notes to users.
		 * </p>
		 *
		 * @return a JSON formatted string representing the update's changes.
		 * May return null or empty string if no changelog is provided.
		 * Example: {"features":["Added dark mode"],"fixes":["Crash on startup"]}
		 */
		public String getWhatsNewJSON() {
			return whatsNewJSON;
		}
		
		/**
		 * Stores the JSON structure containing detailed update information.
		 * <p>
		 * This JSON can include various fields such as "features", "bugfixes", "improvements",
		 * "security", and "notes". The exact schema should be documented separately and agreed
		 * upon between the update server and client application. For backward compatibility,
		 * consider using optional fields and providing reasonable defaults when parsing.
		 * </p>
		 *
		 * <p><b>Recommended JSON Structure:</b>
		 * <pre>
		 * {
		 *   "title": "Version 2.1.0",
		 *   "features": ["Dark mode support", "New gestures"],
		 *   "fixes": ["Fixed login crash", "Improved battery usage"],
		 *   "security": ["Updated SSL certificates"]
		 * }
		 * </pre>
		 * </p>
		 *
		 * @param whatsNewJSON the JSON formatted string describing what's new in this update.
		 *                     Should be valid JSON but may be null if no details available.
		 */
		public void setWhatsNewJSON(String whatsNewJSON) {
			this.whatsNewJSON = whatsNewJSON;
		}
	}
}
