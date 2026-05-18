package coreUtils.base;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.provider.Settings;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewbinding.ViewBinding;

import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.storage.FileStorageUtility;

/**
 * Base abstract activity class that provides a boilerplate template for ViewBinding,
 * localization support, orientation management, and haptic feedback utilities.
 *
 * <p>Key features include:
 * <ul>
 *     <li>Automatic ViewBinding inflation and content view setup.</li>
 *     <li>Standardized lifecycle hook {@link #onLoadedLayout()} for post-inflation logic.</li>
 *     <li>Integration with {@link LocaleHelper} for persistent language settings.</li>
 *     <li>Optional screen orientation locking via {@link #shouldLockOrientation()}.</li>
 *     <li>Centralized vibration and haptic feedback helper methods.</li>
 *     <li>Automatic validation of application directories on creation.</li>
 * </ul>
 *
 * @param <VB> The specific {@link ViewBinding} type associated with the activity's layout.
 */
public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	protected static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;
	protected static final int REQUEST_CODE_STORAGE_PERMISSION = 102;
	protected static final int REQUEST_CODE_MANAGE_STORAGE = 103;
	protected static final int REQUEST_CODE_INSTALL_PACKAGES = 104;
	
	protected VB binding;
	
	protected abstract VB inflateBinding(LayoutInflater inflater);
	protected abstract void onLoadedLayout();
	protected abstract boolean shouldLockOrientation();
	
	/**
	 * Attaches the base context to the activity, applying the user's saved language preference
	 * and ensuring proper integration with the AppCompat delegate.
	 *
	 * @param newBase The original base context.
	 */
	@Override
	protected void attachBaseContext(Context newBase) {
		Context localizedContext = LocaleHelper.applySavedLanguage(newBase);
		super.attachBaseContext(getDelegate().attachBaseContext2(localizedContext));
	}
	
	/**
	 * Called by the system when the device configuration changes while the activity is running.
	 * This implementation ensures that Day/Night mode themes are correctly reapplied via the
	 * AppCompat delegate and logs the updated night mode status.
	 *
	 * @param newConfig The new device configuration.
	 */
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getDelegate().applyDayNight();
		logger.debug("Activity configuration has changed. Night mode is: " +
			((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK)
				== Configuration.UI_MODE_NIGHT_YES));
	}
	
	/**
	 * Initializes the activity, manages orientation locking, validates application directories,
	 * and sets up the view binding content.
	 *
	 * <p>This implementation follows a specific execution order:
	 * <ol>
	 *     <li>Applies orientation lock if required.</li>
	 *     <li>Calls the superclass {@code onCreate}.</li>
	 *     <li>Validates internal application directories.</li>
	 *     <li>Inflates and sets the content view via ViewBinding.</li>
	 *     <li>Triggers {@link #onLoadedLayout()} for subclass-specific initialization.</li>
	 * </ol>
	 *
	 * @param savedInstanceState If the activity is being re-initialized after previously being
	 *                           shut down then this Bundle contains the data it most recently
	 *                           supplied in {@link #onSaveInstanceState}. Otherwise, it is null.
	 */
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		applyOrientationLock();
		super.onCreate(savedInstanceState);
		validateAppDirectories();
		setContentView();
		onLoadedLayout();
	}
	
	/**
	 * Applies a fixed portrait orientation lock to the activity if
	 * {@link #shouldLockOrientation()} returns {@code true}.
	 * <p>
	 * This method is called during {@link #onCreate(Bundle)} to ensure the UI
	 * conforms to the specified orientation constraints before the view is displayed.
	 */
	@SuppressLint("SourceLockedOrientationActivity")
	private void applyOrientationLock() {
		if (shouldLockOrientation()) {
			int screenOrientationPortrait = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			setRequestedOrientation(screenOrientationPortrait);
		}
	}
	
	/**
	 * Validates the required application directories using {@link AppDirsValidator}.
	 * This check ensures that necessary file system structures are present and
	 * accessible before the activity continues its setup. Any errors encountered
	 * during validation are caught and logged to prevent application crashes.
	 */
	private void validateAppDirectories() {
		try {
			AppDirsValidator.performValidation();
		} catch (Exception error) {
			logger.error("Failed to validate app dirs", error);
		}
	}
	
	/**
	 * Initializes the View Binding and sets the activity's content view.
	 * <p>
	 * This method calls {@link #inflateBinding(LayoutInflater)} to instantiate the
	 * binding class and then sets the activity's layout to the root view of that binding.
	 * </p>
	 */
	private void setContentView() {
		binding = inflateBinding(getLayoutInflater());
		setContentView(binding.getRoot());
	}
	
	/**
	 * Triggers a short haptic feedback vibration intended for button clicks.
	 * This method provides a consistent 30ms vibration effect across the application.
	 */
	public void buttonVibrate() {
		vibrate(30);
	}
	
	/**
	 * Triggers a short vibration with a default duration of 20 milliseconds.
	 * Commonly used for standard haptic feedback.
	 */
	protected void vibrate() {
		vibrate(20);
	}
	
	/**
	 * Vibrates the device for a specified duration.
	 * <p>
	 * This method handles compatibility across different Android versions, utilizing
	 * {@link VibratorManager} for API 31 and above, and falling back to
	 * {@link Vibrator} for older versions.
	 * </p>
	 *
	 * @param duration The duration of the vibration in milliseconds.
	 */
	@SuppressWarnings("deprecation") protected void vibrate(long duration) {
		try {
			Vibrator vibrator;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				Object systemService = getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
				VibratorManager vibratorManager = (VibratorManager) systemService;
				vibrator = (vibratorManager != null) ? vibratorManager.getDefaultVibrator() :
					(Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			} else {
				vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			}
			
			if (vibrator != null && vibrator.hasVibrator()) {
				int defaultAmplitude = VibrationEffect.DEFAULT_AMPLITUDE;
				VibrationEffect oneShot = VibrationEffect.createOneShot(duration, defaultAmplitude);
				vibrator.vibrate(oneShot);
			}
		} catch (Exception error) {
			logger.error("Failed to vibrate", error);
		}
	}
	
	/**
	 * Checks if the application has been granted the permission to post notifications.
	 * <p>
	 * On Android 13 (API level 33) and above, this method checks for the
	 * {@link Manifest.permission#POST_NOTIFICATIONS} permission. For older versions,
	 * it returns {@code true} as the permission is granted by default at install time.
	 * </p>
	 *
	 * @return {@code true} if notification permissions are granted or not required;
	 * {@code false} otherwise.
	 */
	protected boolean isNotificationPermissionAllowed() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String postNotifications = Manifest.permission.POST_NOTIFICATIONS;
			return ContextCompat.checkSelfPermission(this, postNotifications)
				== PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}
	
	/**
	 * Requests the user for notification permissions on supported Android versions.
	 * <p>
	 * For devices running Android 13 (API level 33) or higher, this method triggers
	 * the system permission dialog for {@link Manifest.permission#POST_NOTIFICATIONS}.
	 * On older versions, this method performs no action as the permission is
	 * granted automatically at install time.
	 * </p>
	 */
	protected void requestForNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String postNotifications = Manifest.permission.POST_NOTIFICATIONS;
			ActivityCompat.requestPermissions(this,
				new String[]{postNotifications}, REQUEST_CODE_POST_NOTIFICATIONS);
		}
	}
	
	/**
	 * Checks if the application has been granted the legacy storage permissions.
	 * <p>
	 * On Android 13 (API level 33) and above, these permissions are replaced by
	 * more granular media permissions. For older versions, it checks for
	 * {@link Manifest.permission#READ_EXTERNAL_STORAGE} and
	 * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE}.
	 * </p>
	 *
	 * @return {@code true} if storage permissions are granted or not required (API 33+);
	 * {@code false} otherwise.
	 */
	protected boolean isStoragePermissionAllowed() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			int readPermission = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE);
			int writePermission = ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE);
			return readPermission == PackageManager.PERMISSION_GRANTED &&
				writePermission == PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}
	
	/**
	 * Requests the user for legacy storage permissions (Read/Write) on supported Android versions.
	 * <p>
	 * For devices running below Android 13 (API level 33), this method triggers the system
	 * permission dialog for {@link Manifest.permission#READ_EXTERNAL_STORAGE} and
	 * {@link Manifest.permission#WRITE_EXTERNAL_STORAGE}. On Android 13 and above,
	 * this method performs no action as these legacy permissions are deprecated and
	 */
	protected void requestForStoragePermission() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			String[] permissions = {
				Manifest.permission.READ_EXTERNAL_STORAGE,
				Manifest.permission.WRITE_EXTERNAL_STORAGE
			};
			ActivityCompat.requestPermissions(this,
				permissions, REQUEST_CODE_STORAGE_PERMISSION);
		}
	}
	
	/**
	 * Checks if the application has been granted "All Files Access" (MANAGE_EXTERNAL_STORAGE).
	 * <p>
	 * This permission is required for full storage access on Android 11 (API level 30) and above.
	 * Since the application's {@code minSdk} is 30, this check is always relevant.
	 * </p>
	 *
	 * @return {@code true} if "All Files Access" is granted; {@code false} otherwise.
	 */
	protected boolean isAllFilesAccessAllowed() {
		Context applicationContext = getApplicationContext();
		return FileStorageUtility.hasFullFileSystemAccess(applicationContext);
	}
	
	/**
	 * Directs the user to the system settings page to grant the "All Files Access"
	 * ({@code MANAGE_EXTERNAL_STORAGE}) permission.
	 *
	 * <p>This method attempts to open the specific settings page for this application.
	 * If that fails, it falls back to the general management page for all apps.
	 * This permission is required for broad access to shared storage on Android 11
	 * (API level 30) and above.</p>
	 */
	protected void requestForAllFilesAccess() {
		try {
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			intent.addCategory("android.intent.category.DEFAULT");
			intent.setData(Uri.parse(String.format("package:%s", getPackageName())));
			startActivity(intent);
		} catch (Exception error) {
			logger.error("Error requesting all file access: ", error);
			Intent intent = new Intent();
			intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
			startActivity(intent);
		}
	}
	
	/**
	 * Checks if the application is allowed to install packages.
	 * <p>
	 * This check is relevant for Android 8.0 (API level 26) and above.
	 * Since the application's {@code minSdk} is 30, this is always supported.
	 * </p>
	 *
	 * @return {@code true} if package installation is allowed; {@code false} otherwise.
	 */
	protected boolean isInstallPackagesAllowed() {
		return getPackageManager().canRequestPackageInstalls();
	}
	
	/**
	 * Directs the user to the system settings page to allow installation of apps from unknown sources.
	 * This is required for installing APKs on Android 8.0 (API level 26) and above.
	 */
	protected void requestForInstallPackages() {
		Uri packageUri = Uri.parse("package:" + getPackageName());
		Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
		startActivity(intent);
	}
}
