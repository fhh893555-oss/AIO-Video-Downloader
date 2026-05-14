package coreUtils.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewbinding.ViewBinding;

import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;

public abstract class BaseActivity<VB extends ViewBinding> extends AppCompatActivity {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final @LayoutRes int NO_LAYOUT_PROVIDED = -1;
	
	protected VB binding;
	
	protected abstract VB inflateBinding(LayoutInflater inflater);
	protected abstract void onLoadedLayout();
	protected abstract void onActivityPause();
	protected abstract void onActivityResume();
	protected abstract void onActivityDestroyed();
	protected abstract boolean shouldLockOrientation();
	
	@Override
	protected void attachBaseContext(Context newBase) {
		Context localizedContext = LocaleHelper.applySavedLanguage(newBase);
		Configuration configuration = localizedContext.getResources().getConfiguration();
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
	
	@Override
	protected void onPause() {
		super.onPause();
		onActivityPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		onActivityResume();
	}
	
	@Override
	protected void onDestroy() {
		onActivityDestroyed();
		super.onDestroy();
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
}
