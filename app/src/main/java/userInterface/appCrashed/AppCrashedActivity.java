package userInterface.appCrashed;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.databinding.ActivityAppCrashed1Binding;

import java.io.Serializable;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.StylizedDialogBuilder;
import userInterface.openingSplash.LauncherActivity;
import userInterface.openingSplash.OpeningActivity;

/**
 * Displays crash information to the user after an unhandled exception occurs,
 * offering options to report the crash to a server or continue using the app.
 * <p>
 * This activity is typically launched from a custom
 * {@link Thread.UncaughtExceptionHandler} when the application encounters a
 * fatal runtime exception. It receives a serialized {@link AppCrashedInfo}
 * object via an intent extra, which contains diagnostic data such as the
 * stack trace, device identifier, Android version, and application version.
 * The UI presents three user choices: report the crash and continue, continue
 * without reporting, or dismiss with no action.
 * </p>
 *
 * @see BaseActivity
 * @see AppCrashedInfo
 * @see AppCrashedViewModel
 * @see ActivityAppCrashed1Binding
 */
public final class AppCrashedActivity extends BaseActivity<ActivityAppCrashed1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String CRASHED_INFO_INTENT_KEY = "CRASHED_INFO_INTENT_KEY";
	private StylizedDialogBuilder progressDialogBuilder;
	
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
	
	/**
	 * Configures click listeners for UI components after the layout has been
	 * successfully inflated and loaded.
	 * <p>
	 * This method is invoked as part of a custom lifecycle callback (presumably
	 * from a base activity or fragment) after the layout resources have been
	 * fully inflated and attached to the view hierarchy. It delegates to
	 * {@link #setupButtonClicks()} to attach click handlers to buttons or other
	 * interactive elements that require user interaction handling.
	 * </p>
	 * <p>
	 * Placing button click setup in this method ensures that all view references
	 * are fully resolved and ready for event binding, preventing null pointer
	 * exceptions that might occur if called prematurely during
	 * {@link Activity#onCreate(Bundle)} before view inflation completes.
	 * </p>
	 *
	 * @see #setupButtonClicks()
	 */
	@Override
	protected void onLoadedLayout() {
		setupButtonClicks();
	}
	
	/**
	 * Retrieves or creates the {@link AppCrashedViewModel} associated with this
	 * activity.
	 * <p>
	 * This method uses the standard Android ViewModel provider pattern to obtain
	 * a ViewModel instance scoped to this activity's lifecycle. The ViewModel
	 * survives configuration changes (e.g., screen rotations) and holds crash
	 * report data that would otherwise be lost during recreation. The returned
	 * instance is typically used to observe LiveData, process crash information,
	 * or trigger server submission operations.
	 * </p>
	 * <p>
	 * The method delegates to {@link ViewModelProvider#get(Class)} which either
	 * returns an existing ViewModel from the store or creates a new one using
	 * the default {@link ViewModelProvider.NewInstanceFactory}.
	 * </p>
	 *
	 * @return the activity-scoped {@link AppCrashedViewModel} instance, never
	 * {@code null}
	 * @see ViewModelProvider
	 * @see AppCrashedViewModel
	 * @see androidx.lifecycle.ViewModel
	 */
	private AppCrashedViewModel getViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		return viewModelProvider.get(AppCrashedViewModel.class);
	}
	
	/**
	 * Initializes click handlers for all interactive buttons in the crash report
	 * screen.
	 * <p>
	 * This method attaches {@link android.view.View.OnClickListener} implementations
	 * to three distinct buttons: "Continue Anyway" (proceeds without reporting),
	 * "Report Crash" (submits crash data before proceeding), and "No, Thanks"
	 * (dismisses without reporting). All handlers share the common behavior of
	 * navigating to {@link LauncherActivity} with a fade transition animation,
	 * followed by finishing the current activity.
	 * </p>
	 *
	 * @see #getCrashedInfoFromIntent()
	 * @see #getViewModel()
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
	 */
	private void setupButtonClicks() {
		binding.btnContinueAnyway.setOnClickListener(view -> {
			Intent intent = new Intent(AppCrashedActivity.this, LauncherActivity.class);
			ActivityAnimator.animActivityFade(AppCrashedActivity.this);
			startActivity(intent);
			finish();
		});
		
		binding.btnReportCrash.setOnClickListener(view -> {
			AppCrashedInfo crashedInfo = getCrashedInfoFromIntent();
			if (crashedInfo != null) {
				getViewModel().sendCrashInfoToServer(crashedInfo);
				binding.btnContinueAnyway.performClick();
			}
		});
	}
	
	/**
	 * Extracts a crash information object from the activity's launching intent.
	 * <p>
	 * This method retrieves a serialized {@link AppCrashedInfo} instance that was
	 * previously attached to the intent as an extra under the key
	 * {@link #CRASHED_INFO_INTENT_KEY}. The extracted object contains detailed
	 * crash context (device ID, stack trace, application version, etc.) and is
	 * typically used to display crash details on a dedicated error screen or to
	 * submit the report to a remote server.
	 * </p>
	 *
	 * @return the extracted {@link AppCrashedInfo} object containing crash details,
	 * or {@code null} if the intent extra is absent, null, or of an
	 * incorrect type
	 * @see Intent#getSerializableExtra(String)
	 * @see AppCrashedInfo
	 * @see #CRASHED_INFO_INTENT_KEY
	 */
	@Nullable
	private AppCrashedInfo getCrashedInfoFromIntent() {
		Intent intent = getIntent();
		Serializable packageExtra = intent
			.getSerializableExtra(CRASHED_INFO_INTENT_KEY);
		
		if (packageExtra != null) {
			if (!(packageExtra instanceof AppCrashedInfo)) {return null;}
			return (AppCrashedInfo) packageExtra;
		} else {
			return null;
		}
	}
}
