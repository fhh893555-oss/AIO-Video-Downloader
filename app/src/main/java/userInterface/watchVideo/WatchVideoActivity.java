package userInterface.watchVideo;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityWatchVideo1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public class WatchVideoActivity extends BaseActivity<ActivityWatchVideo1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override protected boolean shouldLockOrientation() {
		return false;
	}
	
	@Override protected ActivityWatchVideo1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityWatchVideo1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
}
