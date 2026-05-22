package userInterface.openingSplash;

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
import userInterface.appUpdater.AppUpdaterActivity;
import userInterface.appUpdater.AppUpdaterUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.languagePicker.LanguageActivity;
import userInterface.termsConsPolicy.TermsPolicyActivity;

/**
 * OpeningActivity serves as the splash screen for the application.
 *
 * <p>This activity is responsible for:
 * <ul>
 *     <li>Displaying the initial branding animations using Lottie.</li>
 *     <li>Applying visual styling, such as gradients, to the application title.</li>
 *     <li>Retrieving and displaying the current application version information.</li>
 *     <li>Handling the initial routing logic to direct the user to the {@link LanguageActivity}
 *     or the main interface based on existing configuration.</li>
 * </ul>
 *
 * <p>The activity is locked to a specific orientation and automatically transitions
 * to the next screen after a predefined delay.</p>
 */
public class OpeningActivity extends BaseActivity<ActivityOpening1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Indicates whether the activity should lock its orientation.
	 * For the opening screen, this is set to true to prevent orientation changes
	 * during initial loading.
	 *
	 * @return {@code true} to lock the orientation; {@code false} otherwise.
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for this activity, providing access to the UI components.
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the layout.
	 * @return An instance of {@link ActivityOpening1Binding} associated with this activity.
	 */
	@Override protected ActivityOpening1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityOpening1Binding.inflate(inflater);
	}
	
	/**
	 * Called when the layout has been successfully inflated and is ready for
	 * initialization. This method configures the Lottie animation view, applies visual
	 * styling to the title, populates the version information, and initiates the transition
	 * logic to the next screen.
	 */
	@Override
	protected void onLoadedLayout() {
		LottieAnimationView animationView = binding.lavLoading;
		animationView.enableMergePathsForKitKatAndAbove(true);
		
		applyGradientToTitle();
		loadVersionInfo();
		startNextActivity();
	}
	
	/**
	 * Applies a color gradient span to the "NextGen" portion of the title text.
	 * This method searches for the "NextGen" substring within the title {@code TextView}
	 * and applies a transition between the secondary and primary variant colors.
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
	 * Retrieves the application's version name and version code, formats them into a
	 * readable string (e.g., "1.0.0 (1)"), and updates the version text view in the UI.
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
	 * Initiates the transition to the next activity after checking for app updates.
	 * <p>
	 * Executes in background thread to fetch update info from server. If a newer version
	 * is available, navigates to AppUpdaterActivity with the update details. Otherwise,
	 * delays for 1 second then proceeds to the appropriate onboarding or main activity
	 * based on user configuration. Clears the activity stack to prevent back navigation.
	 * </p>
	 */
	private void startNextActivity() {
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			ThreadTask.executeInBackground(() -> {
				UpdateInfo latestUpdateInfo = getLatestUpdateInfo();
				Context context = getApplicationContext();
				if (AppUpdaterUtils.isUpdateAvailable(context, latestUpdateInfo)) {
					launchAppUpdater(latestUpdateInfo);
				} else {
					proceedToNextScreen();
				}
			});
		}, 1000);
	}
	
	private void launchAppUpdater(UpdateInfo latestUpdateInfo) {
		ThreadTask.executeOnMainThread(() -> {
			Intent intent = new Intent(OpeningActivity.this, AppUpdaterActivity.class);
			intent.putExtra(AppUpdaterActivity.KEY_INTENT_RECEIVED_KEY, latestUpdateInfo);
			vibrate();
			startActivity(intent);
			ActivityAnimator.animActivityFade(OpeningActivity.this);
		});
	}
	
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
	 * Checks two configuration flags: locale setup and terms agreement. If terms are not agreed,
	 * always navigates to TermsPolicyActivity. Otherwise, navigates to LanguageActivity if locale
	 * is not configured, or TermsPolicyActivity if locale is configured but terms may need review.
	 * </p>
	 *
	 * @return Intent configured to launch the appropriate onboarding or terms activity
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
	 * Creates an instance of AppUpdaterUtils, generates a unique device signature as an identifier,
	 * and queries the server for the most recent update record. Returns null if no update is
	 * available or if the request fails.
	 * </p>
	 *
	 * @return UpdateInfo object containing version details and download URL, or null if none found
	 */
	private UpdateInfo getLatestUpdateInfo() {
		AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils();
		Context context = getApplicationContext();
		String deviceId = DeviceSignature.getInstance(context).generate();
		return appUpdaterUtils.fetchLatestUpdateInfo(deviceId);
	}
}
