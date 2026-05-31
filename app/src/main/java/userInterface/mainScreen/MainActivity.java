package userInterface.mainScreen;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityMain1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public final class MainActivity extends BaseActivity<ActivityMain1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityMain1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityMain1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
}
