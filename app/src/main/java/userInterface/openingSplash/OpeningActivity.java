package userInterface.openingSplash;

import static userInterface.appUpdater.AppUpdaterUtils.isUpdateAvailable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.viewbinding.ViewBinding;

import com.nextgen.databinding.ActivityOpening0Binding;

import coreUtils.base.BaseActivity;
import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.views.ActivityAnimator;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsHelper;
import dataRepo.appConfigs.AppConfigsRepo;
import dataRepo.userDetails.AppUserRepo;
import userInterface.appUpdater.AppUpdaterActivity;
import userInterface.appUpdater.AppUpdaterUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.languagePicker.LanguageActivity;
import userInterface.mainScreen.MainActivity;
import userInterface.termsConsPolicy.TermsPolicyActivity;


public final class OpeningActivity extends BaseActivity<ActivityOpening0Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
    ThreadTask<Boolean, Boolean> versionSyncTask = new ThreadTask<>();
    ThreadTask<Boolean, Boolean> configSyncTask = new ThreadTask<>();


    /**
     * Determines whether the activity's screen orientation should be locked.
     * This implementation returns {@code true}, forcing the opening screen
     * to remain in portrait mode regardless of device rotation.
     *
     * <p><strong>Design rationale:</strong>
     * Locking the orientation ensures the version information, loading
     * indicator, and any introductory content maintain a consistent layout
     * without unexpected reconfigurations during the brief display period
     * before navigation.
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
     * binding instance for {@code activity_opening_0.xml}. This method is called
     * during the base activity's {@code setContentView()} phase to create the
     * binding object that provides type-safe access to all views in the layout,
     * including the version TextView and any loading animations.
     *
     * @param inflater The layout inflater service used to create the view hierarchy.
     *                 Must not be {@code null}.
     * @return The {@link ActivityOpening0Binding} instance containing references
     * to all views defined in the opening screen layout.
     * @see BaseActivity#inflateBinding(LayoutInflater)
     */
    @Override
    protected ActivityOpening0Binding inflateBinding(LayoutInflater inflater) {
        return ActivityOpening0Binding.inflate(inflater);
    }

    /**
     * Performs post-layout initialization after the content view has been inflated.
     * This method is invoked by the base activity at the end of {@code onCreate()}
     * and is responsible for loading the app version information, syncing the
     * download engine configuration, and checking for updates before navigating.
     *
     * <p><strong>Initialization order:</strong>
     * <ol>
     * <li>Loads and displays the current app version via {@link #loadVersionInfo()}.</li>
     * <li>Syncs download engine configuration via {@link #syncDownloadEngineConfig()}.</li>
     * <li>Checks for updates and navigates via {@link #checkUpdatesAndNavigate()}.</li>
     * </ol>
     *
     * @see BaseActivity#onLoadedLayout()
     * @see #loadVersionInfo()
     * @see #syncDownloadEngineConfig()
     * @see #checkUpdatesAndNavigate()
     */
    @Override
    protected void onLoadedLayout() {
        loadVersionInfo();
        syncDownloadEngineConfig();
        checkUpdatesAndNavigate();
    }

    /**
     * Synchronizes the local download engine configuration with the remote server.
     * This method retrieves the current user's device ID from {@link AppUserRepo}
     * and the local application configuration from {@link AppConfigsRepo}, then
     * delegates to {@link AppConfigsHelper#syncDownloadEngineConfig(String, AppConfigs)}
     * to perform the actual sync operation asynchronously on a background thread.
     *
     * <p>Configuration data synced typically includes download concurrency limits,
     * speed restrictions, Wi-Fi-only preferences, and auto-resume settings.
     *
     * @see AppUserRepo#getUser()
     * @see AppConfigsRepo#getConfig()
     * @see AppConfigsHelper#syncDownloadEngineConfig(String, AppConfigs)
     */
    private void syncDownloadEngineConfig() {
        if (!configSyncTask.isCancelled()) {
            configSyncTask.cancel();
        }

        configSyncTask.setMaxExecutionTimeMs(2000);
        configSyncTask.setBackgroundTask(callback -> {
            try {
                String userDeviceId = AppUserRepo.getUser().userDeviceId;
                AppConfigs appConfigs = AppConfigsRepo.getConfig();
                AppConfigsHelper.syncDownloadEngineConfig(userDeviceId, appConfigs);
                return true;
            } catch (Exception error) {
                logger.error("Error syncing download engine: ", error);
                return false;
            }
        });
        configSyncTask.start();
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
     * Checks for application updates after a 500ms delay, then navigates either
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
     * <li>If update available, calls {@link #launchAppUpdater(UpdateInfo)} and finishes.</li>
     * <li>If no update, calls {@link #proceedToNextScreen()}.</li>
     * </ol>
     *
     * @see #getLatestUpdateInfo()
     * @see AppUpdaterUtils#isUpdateAvailable(Context, UpdateInfo)
     * @see #launchAppUpdater(UpdateInfo)
     * @see #proceedToNextScreen()
     */
    private void checkUpdatesAndNavigate() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if(!versionSyncTask.isCancelled()) {
                versionSyncTask.cancel();
            }

            versionSyncTask.setMaxExecutionTimeMs(2000);
            versionSyncTask.setBackgroundTask(callback -> {
                UpdateInfo latestUpdateInfo = getLatestUpdateInfo();
                if (isUpdateAvailable(getApplicationContext(), latestUpdateInfo)) {
                    logger.debug("Update available, launching app updater");
                    launchAppUpdater(latestUpdateInfo);
                    finish();
                } else {
                    logger.debug("No update available, proceeding to next screen");
                    proceedToNextScreen();
                }
                return true;
            });
        }, 500);
    }

    /**
     * Launches the application updater screen with the latest update information.
     * This method constructs an intent targeting {@link AppUpdaterActivity},
     * attaches the {@link UpdateInfo} object as an extra using the key
     * {@link AppUpdaterActivity#KEY_INTENT_RECEIVED_KEY}, triggers a short
     * haptic vibration via {@link #vibrate()}, starts the activity with a
     * fade animation, and finishes the current opening activity.
     *
     * @param latestUpdateInfo The {@link UpdateInfo} object containing version
     *                         details, changelog, and download URL. Must not be
     *                         {@code null} when an update is available.
     * @see AppUpdaterActivity
     * @see UpdateInfo
     * @see #vibrate()
     * @see ActivityAnimator#animActivityFade(BaseActivity)
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
     * starts the target activity with a fade transition animation, and finishes
     * the current opening screen.
     *
     * <p><strong>Intent flags:</strong>
     * <ul>
     * <li>{@link Intent#FLAG_ACTIVITY_NEW_TASK} - Starts the activity as a new task.</li>
     * <li>{@link Intent#FLAG_ACTIVITY_CLEAR_TASK} - Clears any existing activities
     *     from the task stack before launching.</li>
     * </ul>
     *
     * <p>The method logs whether the locale is already configured for debugging
     * purposes, then applies a fade animation and closes the opening screen.
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

        startActivity(destinationIntent);
        ActivityAnimator.animActivityFade(OpeningActivity.this);
        finish();
    }

    /**
     * Determines the appropriate destination activity based on the current
     * application configuration state. The decision logic evaluates:
     * <ol>
     * <li>If terms and conditions have not been accepted, opens
     *     {@link TermsPolicyActivity}.</li>
     * <li>If terms are accepted but locale is not configured, opens
     *     {@link LanguageActivity}.</li>
     * <li>If both terms and locale are configured, opens {@link MainActivity}.</li>
     * </ol>
     *
     * <p>The method also adds an extra with key
     * {@link TermsPolicyActivity#KEY_ACTIVITY_LAUNCHED_LOCATION} and value
     * {@link TermsPolicyActivity#LAUNCHED_FROM_OPENING_SCREEN} to indicate the
     * origin of the launch for tracking or behavior customization.
     *
     * @return A non-null {@link Intent} targeting the determined destination
     * {@link BaseActivity} subclass.
     * @see AppConfigsRepo#getConfig()
     * @see TermsPolicyActivity
     * @see LanguageActivity
     * @see MainActivity
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
