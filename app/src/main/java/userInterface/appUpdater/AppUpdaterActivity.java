package userInterface.appUpdater;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityUpdater1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public class AppUpdaterActivity extends BaseActivity<ActivityUpdater1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String KEY_INTENT_RECEIVED_KEY = "KEY_INTENT_RECEIVED_KEY";
	
	@Override protected boolean shouldLockOrientation() {
		return false;
	}
	
	@Override protected ActivityUpdater1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityUpdater1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
	
}
