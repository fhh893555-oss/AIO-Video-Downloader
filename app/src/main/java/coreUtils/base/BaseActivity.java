package coreUtils.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
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
	
	protected void vibrate(long duration) {
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
}
