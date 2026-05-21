package userInterface.appUpdater;

import android.view.LayoutInflater;

import androidx.viewbinding.ViewBinding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public class AppUpdaterActivity extends BaseActivity {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override protected boolean shouldLockOrientation() {
		return false;
	}
	
	@Override protected ViewBinding inflateBinding(LayoutInflater inflater) {
		return null;
	}
	
	@Override protected void onLoadedLayout() {
	
	}
	
}
