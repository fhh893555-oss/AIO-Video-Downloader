package userInterface.appCrashed;

import android.content.Intent;
import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityAppCrashed1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import userInterface.openingSplash.OpeningActivity;

public final class AppCrashedActivity extends BaseActivity<ActivityAppCrashed1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Locks the activity's screen orientation to prevent rotation during display.
	 * <p>
	 * This method returns true to lock the orientation, preventing the system from
	 * recreating the activity when the device is rotated. This ensures smooth animation
	 * playback and avoids disruptions during critical operations like splash screen
	 * display, video playback, or download processes where orientation changes could
	 * cause UI glitches or state loss.
	 * </p>
	 *
	 * @return true to lock orientation, false to allow rotation
	 */
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for the app crashed activity.
	 * <p>
	 * This method uses the generated ActivityAppCrashed1Binding class to inflate the
	 * crash report layout XML, providing type-safe access to all UI components
	 * including error message displays, crash details, and action buttons for
	 * reporting or restarting the application.
	 * </p>
	 *
	 * @param inflater the LayoutInflater instance used to inflate the layout
	 * @return the ActivityAppCrashed1Binding containing references to all UI elements
	 */
	@Override protected ActivityAppCrashed1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityAppCrashed1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
		setupButtonClicks();
	}
	
	
	private void setupButtonClicks() {
		binding.btnContinueAnyway.setOnClickListener(view -> {
			Intent intent = new Intent(AppCrashedActivity.this, OpeningActivity.class);
			ActivityAnimator.animActivityFade(AppCrashedActivity.this);
			startActivity(intent);
			finish();
		});
	}
	
}
