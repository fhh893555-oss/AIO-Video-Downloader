package userInterface.openingSplash;

import static userInterface.appUpdater.AppUpdaterUtils.isUpdateAvailable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.airbnb.lottie.LottieAnimationView;
import com.nextgen.R;
import com.nextgen.databinding.ActivityOpening1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.TextViewsUtils;
import dataRepo.configs.AppConfigsRepo;
import userInterface.appCrashed.AppCrashedActivity;
import userInterface.appUpdater.AppUpdaterActivity;
import userInterface.appUpdater.AppUpdaterUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.languagePicker.LanguageActivity;
import userInterface.termsConsPolicy.TermsPolicyActivity;

/**
 * Splash screen activity that serves as the entry point for the application.
 * <p>
 * This activity displays branding animations using Lottie, applies gradient styling
 * to the app title, shows the current version information, and handles initial
 * navigation logic including update checks and onboarding flow routing.
 * </p>
 *
 * <p><b>Navigation Flow:</b>
 * <ol>
 *   <li>Displays splash animation for 1 second</li>
 *   <li>Checks for available app updates from the server</li>
 *   <li>If update exists: redirects to {@link AppUpdaterActivity}</li>
 *   <li>If no update: proceeds to {@link LanguageActivity} or {@link TermsPolicyActivity}
 *       based on user configuration (locale setup and terms agreement status)</li>
 * </ol>
 * </p>
 *
 * <p><b>Configuration Decisions:</b>
 * The activity evaluates two persisted flags from {@link AppConfigsRepo}:
 * <ul>
 *   <li><i>isLocaleConfigured</i> - Determines if language has been selected</li>
 *   <li><i>isTermsConditionsAgreed</i> - Determines if terms have been accepted</li>
 * </ul>
 * Terms agreement takes priority over locale configuration when both are incomplete.
 * </p>
 *
 * @see BaseActivity
 * @see ActivityOpening1Binding
 * @see AppUpdaterActivity
 * @see LanguageActivity
 * @see TermsPolicyActivity
 * @see AppUpdaterUtils
 */
public class OpeningActivity extends BaseActivity<ActivityOpening1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Locks the activity orientation to prevent configuration changes during splash display.
	 * <p>
	 * Returns true to lock the screen orientation, preventing the system from recreating
	 * the activity when the device is rotated. This ensures smooth animation playback
	 * and avoids disruptions during the initial loading phase.
	 * </p>
	 *
	 * @return true to lock orientation, false to allow rotation
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for the opening splash screen activity.
	 * <p>
	 * Uses the generated ActivityOpening1Binding class to inflate the layout XML,
	 * providing type-safe access to all UI components including the Lottie animation
	 * view, title text, and version display TextView.
	 * </p>
	 *
	 * @param inflater LayoutInflater instance used to inflate the layout
	 * @return ActivityOpening1Binding containing references to all UI elements
	 */
	@Override protected ActivityOpening1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityOpening1Binding.inflate(inflater);
	}
	
	/**
	 * Configures all UI components after the layout has been successfully inflated.
	 * <p>
	 * This lifecycle method performs the following initialization steps:
	 * Enables merge path support for Lottie animation (compatible with KitKat+),
	 * applies gradient color styling to the "NextGen" portion of the title text,
	 * loads and displays the current app version information, and initiates the
	 * update check and navigation flow.
	 * </p>
	 */
	@Override
	protected void onLoadedLayout() {
		LottieAnimationView animationView = binding.lavLoading;
		animationView.enableMergePathsForKitKatAndAbove(true);
		
		applyGradientToTitle();
		loadVersionInfo();
		//checkUpdatesAndNavigate();
		Intent intent = new Intent(this, AppCrashedActivity.class);
		startActivity(intent);
	}
	
	/**
	 * Applies a gradient color effect to the "NextGen" substring in the title TextView.
	 * <p>
	 * Searches for the word "NextGen" within the title text. If found, uses TextViewsUtils
	 * to apply a gradient span transitioning from secondary color to primary variant color
	 * across the 7 characters of the word. This creates a visually appealing branding effect.
	 * </p>
	 */
	private void applyGradientToTitle() {
		String fullText = binding.tvTitle.getText().toString();
		int nextGenStart = fullText.indexOf("NextGen");
		if (nextGenStart != -1) {
			TextViewsUtils.applyGradientSpan(
				binding.tvTitle,
				getColor(R.color.color_secondary),
				getColor(R.color.color_primary_variant),
				nextGenStart,
				nextGenStart + 7
			);
		}
	}
	
	/**
	 * Retrieves and displays the current application version information.
	 * <p>
	 * Fetches both the version name (e.g., "2.1.0") and version code (e.g., 42) from
	 * VersionInfo utility. Formats them as "versionName (versionCode)" and displays the
	 * result in the version TextView. Logs the formatted version string for debugging
	 * purposes.
	 * </p>
	 */
	private void loadVersionInfo() {
		BaseApplication app = BaseApplication.AppContext;
		String versionCode = String.valueOf(VersionInfo.getVersionCode(app));
		String versionName = String.valueOf(VersionInfo.getVersionName(app));
		String versionInfo = versionName + " (" + versionCode + ")";
		logger.debug("Version result: " + versionInfo);
		binding.tvVersion.setText(versionInfo);
	}
	
	/**
	 * Delays execution for 1 second, then checks for available app updates in the background.
	 * <p>
	 * This method serves as the entry point for post-splash navigation logic. It waits 1 second
	 * to ensure animations complete, then fetches update information from the server. If an
	 * update is available, it redirects to the app updater screen; otherwise, it proceeds to
	 * the normal application flow (language selection or terms agreement).
	 * </p>
	 */
	private void checkUpdatesAndNavigate() {
		new Handler(Looper.getMainLooper()).postDelayed(() ->
			ThreadTask.executeInBackground(() -> {
				UpdateInfo latestUpdateInfo = getLatestUpdateInfo();
				Context context = getApplicationContext();
				ThreadTask.executeOnMainThread(() -> {
					if (isUpdateAvailable(context, latestUpdateInfo)) {
						logger.debug("Update available, launching app updater");
						launchAppUpdater(latestUpdateInfo);
					} else {
						logger.debug("No update available, proceeding to next screen");
						proceedToNextScreen();
					}
				});
			}), 1000);
	}
	
	/**
	 * Launches the app updater activity with the latest update information.
	 * <p>
	 * Executes on the main thread to start AppUpdaterActivity, passing the UpdateInfo object
	 * as an intent extra. Triggers haptic feedback vibration and applies a fade animation
	 * transition. The updater activity will handle the download and installation process.
	 * </p>
	 *
	 * @param latestUpdateInfo The update details containing version info and APK download URL
	 */
	private void launchAppUpdater(UpdateInfo latestUpdateInfo) {
		Intent intent = new Intent(OpeningActivity.this, AppUpdaterActivity.class);
		intent.putExtra(AppUpdaterActivity.KEY_INTENT_RECEIVED_KEY, latestUpdateInfo);
		vibrate();
		startActivity(intent);
		ActivityAnimator.animActivityFade(OpeningActivity.this);
	}
	
	/**
	 * Navigates to the next appropriate screen when no update is required.
	 * <p>
	 * Determines the destination activity based on current configuration state (locale setup
	 * and terms agreement), adds flags to clear the activity stack so users cannot return
	 * to the splash screen, logs the navigation decision, and starts the activity with a
	 * fade animation before finishing the current splash activity.
	 * </p>
	 */
	private void proceedToNextScreen() {
		Intent destinationIntent = getDestinationIntent();
		destinationIntent.addFlags(
			Intent.FLAG_ACTIVITY_NEW_TASK |
				Intent.FLAG_ACTIVITY_CLEAR_TASK);
		
		if (AppConfigsRepo.getConfig().isLocaleConfigured)
			logger.debug("Locale is already configured, opening main activity");
		else logger.debug("Locale is not configured, opening language activity.");
		
		ActivityAnimator.animActivityFade(OpeningActivity.this);
		startActivity(destinationIntent);
		finish();
	}
	
	/**
	 * Determines the next activity to launch based on user configuration state.
	 * <p>
	 * Evaluates two configuration flags: locale setup status and terms agreement status.
	 * Priority is given to terms agreement - if terms are not agreed, always navigates to
	 * TermsPolicyActivity. Otherwise, navigates to LanguageActivity if locale is not configured,
	 * or to TermsPolicyActivity if locale is configured. Adds an extra flag when launching
	 * the terms activity to indicate this was launched from the opening screen.
	 * </p>
	 *
	 * @return Intent configured to launch the appropriate onboarding activity
	 */
	@NonNull
	private Intent getDestinationIntent() {
		Intent destinationIntent;
		boolean isLocaleConfigured = AppConfigsRepo.getConfig().isLocaleConfigured;
		
		Class<? extends BaseActivity<? extends ViewBinding>> nextActivityToOpen =
			isLocaleConfigured ? TermsPolicyActivity.class : LanguageActivity.class;
		
		boolean isTermsActivitySelected = false;
		if (!AppConfigsRepo.getConfig().isTermsConditionsAgreed) {
			nextActivityToOpen = TermsPolicyActivity.class;
			isTermsActivitySelected = true;
		}
		
		destinationIntent = new Intent(this, nextActivityToOpen);
		if (isTermsActivitySelected) {
			destinationIntent.getShortExtra(
				TermsPolicyActivity.LAUNCHED_LOCATION_OF_ACTIVITY,
				TermsPolicyActivity.LAUNCHED_FROM_OPENING_SCREEN);
		}
		
		return destinationIntent;
	}
	
	/**
	 * Fetches the latest application update information from the PocketBase server.
	 * <p>
	 * Creates an instance of AppUpdaterUtils, generates a unique device signature using
	 * the device's hardware identifiers as a request parameter, and queries the server
	 * for the most recent update record. Returns null if no update record exists, the
	 * network request fails, or the response cannot be parsed properly.
	 * </p>
	 *
	 * @return UpdateInfo object containing version code, version name, APK download URL,
	 * and changelog JSON, or null if no update is available or an error occurs
	 */
	private UpdateInfo getLatestUpdateInfo() {
		AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils();
		Context context = getApplicationContext();
		String deviceId = DeviceSignature.getInstance(context).generate();
		return appUpdaterUtils.fetchLatestUpdateInfo(deviceId);
	}
}
