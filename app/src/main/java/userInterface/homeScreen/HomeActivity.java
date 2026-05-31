package userInterface.homeScreen;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityHome1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public final class HomeActivity extends BaseActivity<ActivityHome1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityHome1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityHome1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
}
