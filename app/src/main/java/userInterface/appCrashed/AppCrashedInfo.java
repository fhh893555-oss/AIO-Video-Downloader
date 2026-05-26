package userInterface.appCrashed;

import java.io.Serializable;

/**
 * A serializable data container that captures application crash context information
 * for logging, analytics, or remote reporting purposes.
 * <p>
 * This immutable-style POJO stores device-specific and environment details at the
 * moment of an application crash. All fields are optional and default to {@code null}
 * until populated via setters. The class implements {@link Serializable} to support
 * persistence to disk, transmission via {@link android.content.Intent} extras, or
 * attachment to bug reporting intents.
 * </p>
 * <ul>
 * <li>All fields are package-private and accessed via standard getter/setter pairs</li>
 * <li>No validation is performed on setter inputs; callers must sanitize where
 *     required</li>
 * <li>Thread-safety is not guaranteed; intended for single-threaded crash handling
 *     scenarios</li>
 * </ul>
 *
 * @see Serializable
 * @see Thread#getDefaultUncaughtExceptionHandler()
 */
public final class AppCrashedInfo implements Serializable {
	
	private String deviceId;
	private String androidVersion;
	private String userCountry;
	private String applicationVersion;
	private String stackStraceInfo;
	private String detailedInfo;
	
	/**
	 * Returns the unique hardware identifier of the device where the crash occurred.
	 *
	 * @return the device ID string, or {@code null} if not set
	 */
	public String getDeviceId() {
		return deviceId;
	}
	
	/**
	 * Sets the unique hardware identifier of the device where the crash occurred.
	 *
	 * @param deviceId the device ID string (e.g., ANDROID_ID or advertising ID)
	 */
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	
	/**
	 * Returns the Android OS version running on the device at the time of crash.
	 *
	 * @return the Android version string (e.g., "14" or "Android 14"), or
	 * {@code null} if not set
	 */
	public String getAndroidVersion() {
		return androidVersion;
	}
	
	/**
	 * Sets the Android OS version running on the device at the time of crash.
	 *
	 * @param androidVersion the Android version string (typically from
	 *                       {@link android.os.Build.VERSION#RELEASE})
	 */
	public void setAndroidVersion(String androidVersion) {
		this.androidVersion = androidVersion;
	}
	
	/**
	 * Returns the geographical country code of the user when the crash occurred.
	 *
	 * @return the country code string (e.g., "US", "IN", "GB"), or {@code null}
	 * if not set
	 */
	public String getUserCountry() {
		return userCountry;
	}
	
	/**
	 * Sets the geographical country code of the user when the crash occurred.
	 *
	 * @param userCountry the ISO 3166-1 alpha-2 country code (typically from
	 *                    {@link java.util.Locale#getCountry()})
	 */
	public void setUserCountry(String userCountry) {
		this.userCountry = userCountry;
	}
	
	/**
	 * Returns the version name of the crashing application.
	 *
	 * @return the application version string (e.g., "3.2.1"), or {@code null}
	 * if not set
	 */
	public String getApplicationVersion() {
		return applicationVersion;
	}
	
	/**
	 * Sets the version name of the crashing application.
	 *
	 * @param applicationVersion the app version string (typically from
	 *                           {@link android.content.pm.PackageInfo#versionName})
	 */
	public void setApplicationVersion(String applicationVersion) {
		this.applicationVersion = applicationVersion;
	}
	
	/**
	 * Returns the raw stack trace string from the crash exception.
	 * <p>
	 * The returned string is typically formatted using
	 * {@link android.util.Log#getStackTraceString(Throwable)}.
	 * </p>
	 *
	 * @return the stack trace string, or {@code null} if not set
	 */
	public String getStackStraceInfo() {
		return stackStraceInfo;
	}
	
	/**
	 * Sets the raw stack trace string from the crash exception.
	 * <p>
	 * <b>Note:</b> The method name contains the typo "Strace" instead of "Trace".
	 * This is preserved for backward compatibility with existing serialized data.
	 * </p>
	 *
	 * @param stackStraceInfo the formatted stack trace string
	 */
	public void setStackStraceInfo(String stackStraceInfo) {
		this.stackStraceInfo = stackStraceInfo;
	}
	
	/**
	 * Returns any additional developer-provided context information for the crash.
	 *
	 * @return a detailed info string (e.g., custom metadata, user actions), or
	 * {@code null} if not set
	 */
	public String getDetailedInfo() {
		return detailedInfo;
	}
	
	/**
	 * Sets any additional developer-provided context information for the crash.
	 *
	 * @param detailedInfo an arbitrary string containing supplemental crash
	 *                     details (e.g., current screen name, network state)
	 */
	public void setDetailedInfo(String detailedInfo) {
		this.detailedInfo = detailedInfo;
	}
}