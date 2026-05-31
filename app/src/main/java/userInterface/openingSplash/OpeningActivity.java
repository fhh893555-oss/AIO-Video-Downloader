package userInterface.openingSplash;

import static userInterface.appUpdater.AppUpdaterUtils.isUpdateAvailable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.TextView;

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
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.appUpdater.AppUpdaterActivity;
import userInterface.appUpdater.AppUpdaterUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.languagePicker.LanguageActivity;
import userInterface.mainScreen.MainActivity;
import userInterface.termsConsPolicy.TermsPolicyActivity;

/**
 * Opening screen activity that serves as the launch entry point for the application.
 * This activity displays a branded loading screen with a Lottie animation, the app
 * title with gradient text effect, and the current version information. It performs
 * update checks and navigates to the appropriate next screen based on app state.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays a loading animation using Lottie while preparing the app.</li>
 * <li>Applies gradient color span to the "NextGen" word in the title.</li>
 * <li>Loads and displays the current application version.</li>
 * <li>Checks for available updates via {@link #checkUpdatesAndNavigate()}.</li>
 * <li>Navigates to update screen if available, or proceeds to language/terms/main
 *     screen based on configuration state.</li>
 * <li>Locks screen orientation to portrait for consistent layout rendering.</li>
 * </ul>
 *
 * <p><strong>Navigation flow:</strong>
 * After a 1-second delay, the activity checks for app updates. If an update is
 * available, {@link AppUpdaterActivity} is launched. Otherwise, navigation proceeds
 * to {@link TermsPolicyActivity} (if terms not agreed), {@link LanguageActivity}
 * (if locale not configured), or the main activity based on {@link #getDestinationIntent()}.
 *
 * <p><strong>Layout:</strong>
 * Uses {@code activity_opening1.xml} with a Lottie animation view, title TextView,
 * and version TextView. The word "NextGen" in the title receives a gradient effect
 * from {@code color_secondary} to {@code color_primary_variant}.
 *
 * @see BaseActivity
 * @see ActivityOpening1Binding
 * @see AppUpdaterActivity
 * @see TermsPolicyActivity
 * @see LanguageActivity
 */
public final class OpeningActivity extends BaseActivity<ActivityOpening1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the opening screen
	 * to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures the Lottie loading animation, title text,
	 * and version information maintain a consistent layout without unexpected
	 * reconfigurations during the brief display period before navigation.
	 *
	 * @return {@code true} to lock the activity to portrait orientation.
	 * @see BaseActivity#shouldLockOrientation()
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_opening1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout,
	 * including the Lottie animation view, title TextView, and version TextView.
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityOpening1Binding} instance containing references
	 * to all views defined in the opening screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 */
	@Override protected ActivityOpening1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityOpening1Binding.inflate(inflater);
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and is responsible for configuring the Lottie loading animation, applying
	 * visual enhancements to the title text, loading version information, and
	 * initiating the update check and navigation flow.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Enables merge paths for the Lottie animation (KitKat and above compatibility).</li>
	 * <li>Applies gradient span to the word "NextGen" in the title via
	 *     {@link #applyGradientToTitle()}.</li>
	 * <li>Loads and displays the current app version via {@link #loadVersionInfo()}.</li>
	 * <li>Checks for updates and navigates to the appropriate next screen via
	 *     {@link #checkUpdatesAndNavigate()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #applyGradientToTitle()
	 * @see #loadVersionInfo()
	 * @see #checkUpdatesAndNavigate()
	 */
	@Override
	protected void onLoadedLayout() {
		LottieAnimationView animationView = binding.lavLoading;
		animationView.enableMergePathsForKitKatAndAbove(true);
		
		applyGradientToTitle();
		loadVersionInfo();
		checkUpdatesAndNavigate();
	}
	
	/**
	 * Applies a gradient color span to the word "NextGen" within the title text view.
	 * This method searches for the substring "NextGen" in the full title text and,
	 * if found, applies a gradient effect using
	 * {@link TextViewsUtils#applyGradientSpan(TextView, int, int, int, int)}.
	 *
	 * <p><strong>Visual effect:</strong>
	 * The gradient transitions from {@code color_secondary} to
	 * {@code color_primary_variant}, spanning the 7 characters of the word
	 * "NextGen". This creates a highlighted, branded appearance for the key
	 * word in the opening screen title, drawing user attention to the app's
	 * name or tagline.
	 *
	 * <p><strong>Error handling:</strong>
	 * If the word "NextGen" is not found in the title string (e.g., due to
	 * localization or layout changes), the method silently does nothing without
	 * throwing an exception. The start index check ensures the span is only
	 * applied when the substring exists.
	 *
	 * @see TextViewsUtils#applyGradientSpan(TextView, int, int, int, int)
	 * @see #getColor(int)
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
	 * Loads and displays the current application version information on the screen.
	 * This method retrieves both the version name (e.g., "1.2.3") and version code
	 * (e.g., "4") from {@link VersionInfo}, formats them as a combined string,
	 * logs the result for debugging, and sets the text on the version TextView.
	 *
	 * <p><strong>Format example:</strong> "1.2.3 (4)"
	 *
	 * @see VersionInfo#getVersionCode(android.content.Context)
	 * @see VersionInfo#getVersionName(android.content.Context)
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
	 * Checks for application updates after a 1-second delay, then navigates either
	 * to the app updater screen or the next screen based on update availability.
	 * The update check runs in a background thread to avoid blocking the UI,
	 * while navigation logic executes on the main thread.
	 *
	 * <p><strong>Execution flow:</strong>
	 * <ol>
	 * <li>Delays execution by 500ms to allow the opening screen to be visible.</li>
	 * <li>Executes {@link #getLatestUpdateInfo()} in background thread.</li>
	 * <li>On main thread, evaluates whether an update is available via
	 *     {@link AppUpdaterUtils#isUpdateAvailable(Context, UpdateInfo)}.</li>
	 * <li>If update available, calls {@link #launchAppUpdater(UpdateInfo)}.</li>
	 * <li>If no update, calls {@link #proceedToNextScreen()}.</li>
	 * </ol>
	 *
	 * @see #getLatestUpdateInfo()
	 * @see AppUpdaterUtils#isUpdateAvailable(Context, UpdateInfo)
	 * @see #launchAppUpdater(UpdateInfo)
	 * @see #proceedToNextScreen()
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
						finish();
					} else {
						logger.debug("No update available, proceeding to next screen");
						proceedToNextScreen();
					}
				});
			}), 500);
	}
	
	/**
	 * Launches the application updater screen with the latest update information.
	 * This method constructs an intent targeting {@link AppUpdaterActivity},
	 * attaches the {@link UpdateInfo} object as an extra using the key
	 * {@link AppUpdaterActivity#KEY_INTENT_RECEIVED_KEY}, triggers a short
	 * haptic vibration via {@link #vibrate()}, starts the activity with a
	 * fade animation, and finishes the current opening activity.
	 *
	 * <p>The fade animation is applied via
	 * {@link ActivityAnimator#animActivityFade(BaseActivity)}.
	 *
	 * @param latestUpdateInfo The {@link UpdateInfo} object containing version
	 *                         details, changelog, and download URL. Must not be
	 *                         {@code null} when an update is available.
	 * @see AppUpdaterActivity
	 * @see UpdateInfo
	 * @see #vibrate()
	 */
	private void launchAppUpdater(UpdateInfo latestUpdateInfo) {
		Intent intent = new Intent(OpeningActivity.this, AppUpdaterActivity.class);
		intent.putExtra(AppUpdaterActivity.KEY_INTENT_RECEIVED_KEY, latestUpdateInfo);
		vibrate();
		startActivity(intent);
		ActivityAnimator.animActivityFade(OpeningActivity.this);
	}
	
	/**
	 * Navigates to the appropriate next screen based on the application's
	 * configuration state. This method constructs an intent using
	 * {@link #getDestinationIntent()}, adds flags to clear the activity stack,
	 * applies a fade transition animation, starts the target activity, and
	 * finishes the current opening screen.
	 *
	 * <p><strong>Intent flags:</strong>
	 * <ul>
	 * <li>{@link Intent#FLAG_ACTIVITY_NEW_TASK} - Starts the activity as a new task.</li>
	 * <li>{@link Intent#FLAG_ACTIVITY_CLEAR_TASK} - Clears any existing activities
	 *     from the task stack before launching.</li>
	 * </ul>
	 *
	 * <p>The method logs whether the locale is already configured for debugging
	 * purposes, then applies a fade animation via
	 * {@link ActivityAnimator#animActivityFade(BaseActivity)}.
	 *
	 * @see #getDestinationIntent()
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
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
	 * Determines the appropriate destination activity based on the current
	 * application configuration state. The decision logic evaluates:
	 * <ol>
	 * <li>If terms and conditions have not been agreed, opens
	 *     {@link TermsPolicyActivity} with launch location extras.</li>
	 * <li>Otherwise, if locale is configured, opens the main activity (default).</li>
	 * <li>Otherwise, opens {@link LanguageActivity} for language selection.</li>
	 * </ol>
	 *
	 * <p>When the terms activity is selected, the method adds an extra with key
	 * {@link TermsPolicyActivity#KEY_ACTIVITY_LAUNCHED_LOCATION} and value
	 * {@link TermsPolicyActivity#LAUNCHED_FROM_OPENING_SCREEN} to indicate the
	 * origin of the launch.
	 *
	 * @return A non-null {@link Intent} targeting the determined destination
	 * {@link BaseActivity} subclass.
	 * @see AppConfigsRepo#getConfig()
	 * @see TermsPolicyActivity
	 * @see LanguageActivity
	 */
	@NonNull
	private Intent getDestinationIntent() {
		Intent destinationIntent;
		AppConfigs appConfig = AppConfigsRepo.getConfig();
		boolean isTermsAccepted = appConfig.isTermsConditionsAgreed;
		boolean isLocaleConfigured = appConfig.isLocaleConfigured;
		
		Class<? extends BaseActivity<? extends ViewBinding>> nextActivityToOpen =
			isTermsAccepted ? LanguageActivity.class : TermsPolicyActivity.class;
		
		if (isLocaleConfigured) nextActivityToOpen = MainActivity.class;
		
		destinationIntent = new Intent(this, nextActivityToOpen);
		destinationIntent.getShortExtra(
			TermsPolicyActivity.KEY_ACTIVITY_LAUNCHED_LOCATION,
			TermsPolicyActivity.LAUNCHED_FROM_OPENING_SCREEN);
		
		return destinationIntent;
	}
	
	/**
	 * Fetches the latest application update information from the remote server.
	 * This method creates an {@link AppUpdaterUtils} instance, generates a unique
	 * device identifier via {@link DeviceSignature}, and retrieves update details
	 * such as version code, changelog, and download URL.
	 *
	 * <p>The fetched {@link UpdateInfo} can be used to check for available updates
	 * and prompt the user to install a newer version of the application.
	 *
	 * @return An {@link UpdateInfo} object containing the latest version details,
	 * or {@code null} if the fetch operation fails or no update is available.
	 * @see AppUpdaterUtils#fetchLatestUpdateInfo(String)
	 * @see DeviceSignature#generate()
	 */
	private UpdateInfo getLatestUpdateInfo() {
		AppUpdaterUtils appUpdaterUtils = new AppUpdaterUtils();
		Context context = getApplicationContext();
		String deviceId = DeviceSignature.getInstance(context).generate();
		return appUpdaterUtils.fetchLatestUpdateInfo(deviceId);
	}
}
