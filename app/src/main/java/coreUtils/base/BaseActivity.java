package coreUtils.base;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

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
import sysModules.crashedHandler.GlobalCrashedHandler;

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
	
	@Override
	protected void attachBaseContext(Context newBase) {
		Context localizedContext = LocaleHelper.applySavedLanguage(newBase);
		super.attachBaseContext(getDelegate().attachBaseContext2(localizedContext));
	}
	
	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		getDelegate().applyDayNight();
		logger.debug("Activity configuration has changed. Night mode is: " +
			((newConfig.uiMode & Configuration.UI_MODE_NIGHT_MASK)
				== Configuration.UI_MODE_NIGHT_YES));
	}
	
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		applyOrientationLock();
		super.onCreate(savedInstanceState);
		registerGlobalCrashHandler();
		validateAppDirectories();
		setContentView();
		onLoadedLayout();
	}
	
	@SuppressLint("SourceLockedOrientationActivity")
	private void applyOrientationLock() {
		if (shouldLockOrientation()) {
			int screenOrientationPortrait = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
			setRequestedOrientation(screenOrientationPortrait);
		}
	}
	
	private void validateAppDirectories() {
		try {
			AppDirsValidator.performValidation();
		} catch (Exception error) {
			logger.error("Failed to validate app dirs", error);
		}
	}
	
	private void setContentView() {
		binding = inflateBinding(getLayoutInflater());
		setContentView(binding.getRoot());
	}
	
	public void buttonVibrate() {
		vibrate(30);
	}
	
	protected void vibrate() {
		vibrate(20);
	}
	
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
	
	private void registerGlobalCrashHandler() {
		setDefaultUncaughtExceptionHandler(new GlobalCrashedHandler());
	}
	
	protected boolean isNotificationPermissionAllowed() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String postNotifications = Manifest.permission.POST_NOTIFICATIONS;
			return ContextCompat.checkSelfPermission(this, postNotifications)
				== PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}
	
	protected void requestForNotificationPermission() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			String postNotifications = Manifest.permission.POST_NOTIFICATIONS;
			ActivityCompat.requestPermissions(this,
				new String[]{postNotifications}, REQUEST_CODE_POST_NOTIFICATIONS);
		}
	}
	
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
	
	protected boolean isAllFilesAccessAllowed() {
		Context applicationContext = getApplicationContext();
		return FileStorageUtility.hasFullFileSystemAccess(applicationContext);
	}
	
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
	
	protected boolean isInstallPackagesAllowed() {
		return getPackageManager().canRequestPackageInstalls();
	}
	
	protected void requestForInstallPackages() {
		Uri packageUri = Uri.parse("package:" + getPackageName());
		Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageUri);
		startActivity(intent);
	}
}
