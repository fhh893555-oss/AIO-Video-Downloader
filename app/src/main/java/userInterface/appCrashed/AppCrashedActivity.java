package userInterface.appCrashed;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityAppCrashed1Binding;

import java.io.Serializable;
import java.text.MessageFormat;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.StylizedDialogBuilder;
import sysModules.crashedHandler.AppCrashedInfo;
import sysModules.crashedHandler.GlobalCrashedHandler;
import userInterface.openingSplash.LauncherActivity;


/**
 * Activity displayed when the application crashes unexpectedly. This screen
 * presents crash information to the user, allowing them to send a crash report
 * to the server, view technical details (stacktrace and device info), and
 * continue using the app after acknowledging the crash.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays a unique crash ID for user reference and support tracking.</li>
 * <li>Shows the crash stacktrace in an expandable/collapsible section.</li>
 * <li>Provides a "Send Report" button to upload crash data to the server.</li>
 * <li>Provides a "Continue Anyway" button to restart the app normally.</li>
 * <li>Toggles technical details visibility based on user preference.</li>
 * </ul>
 *
 * <p><strong>Input data:</strong>
 * The activity expects an {@link AppCrashedInfo} object as an intent extra
 * with key {@link #CRASHED_INFO_INTENT_KEY}. This object contains the
 * stacktrace and other crash metadata captured by the global crash handler.
 *
 * <p>The activity locks screen orientation to portrait and uses view binding
 * for type-safe view access. Crash reports are sent asynchronously via
 * {@link AppCrashedViewModel}.
 *
 * @see BaseActivity
 * @see AppCrashedInfo
 * @see GlobalCrashedHandler
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
    @Override
    protected boolean shouldLockOrientation() {
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
    @Override
    protected ActivityAppCrashed1Binding inflateBinding(LayoutInflater inflater) {
        return ActivityAppCrashed1Binding.inflate(inflater);
    }

    /**
     * Performs post-layout initialization after the content view has been inflated.
     * This method is invoked by the base activity at the end of {@code onCreate()}
     * and is responsible for populating the crash stacktrace information and
     * configuring all button click listeners.
     *
     * <p><strong>Initialization order:</strong>
     * <ol>
     * <li>Sets up crash stacktrace display via {@link #setUpCrashStraceInfo()}.</li>
     * <li>Configures all button click listeners via {@link #setupButtonClicks()}.</li>
     * </ol>
     *
     * @see BaseActivity#onLoadedLayout()
     * @see #setUpCrashStraceInfo()
     * @see #setupButtonClicks()
     */
    @Override
    protected void onLoadedLayout() {
        setupButtonClicks();
        setUpCrashStraceInfo();
        setupCrashId();
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
     * Populates the stacktrace text view with crash information from the intent.
     * This method retrieves the {@link AppCrashedInfo} object passed from the
     * crash handler and displays the stack trace in the UI for user review.
     *
     * <p>If no crash information is available (e.g., the intent is missing the
     * expected extra), the method does nothing and the stacktrace view remains
     * empty. This typically occurs when the activity is launched outside of
     * the expected crash reporting flow.
     *
     * @see #getCrashedInfoFromIntent()
     * @see AppCrashedInfo#getStackStraceInfo()
     */
    private void setUpCrashStraceInfo() {
        AppCrashedInfo appCrashedInfo = getCrashedInfoFromIntent();
        if (appCrashedInfo != null) {
            binding.txtStacktrace.setText(appCrashedInfo.getStackStraceInfo());
        }
    }

    /**
     * Generates and displays a unique crash identifier for this crash report.
     * This method creates a random 9-character alphanumeric string prefixed with
     * "id" using {@link StringHelper#generateRandomString(int, String)}. The
     * formatted crash ID is displayed in a text view with the pattern
     * "Crash ID#xxxxx".
     *
     * <p>The crash ID helps users reference this specific crash when contacting
     * support, and assists developers in correlating user reports with backend
     * crash logs.
     *
     * @see StringHelper#generateRandomString(int, String)
     * @see MessageFormat#format(String, Object...)
     */
    private void setupCrashId() {
        String randomString = StringHelper.generateRandomString(9, "id");
        binding.tvCrashId.setText(MessageFormat.format("{0}#{1}",
                getString(R.string.label_crash_id), randomString));
    }

    /**
     * Initializes all button click listeners for the crash report screen.
     * This method aggregates the setup calls for each interactive UI component,
     * ensuring all user input elements respond appropriately to clicks.
     *
     * <p><strong>Buttons configured:</strong>
     * <ul>
     * <li>Continue button via {@link #setupContinueButton()}</li>
     * <li>Send Report button via {@link #setupSendReportButton()}</li>
     * <li>Stacktrace toggle button via {@link #setupStacktraceToggle()}</li>
     * <li>Crash log visibility button via {@link #setupCrashLogButton()}</li>
     * </ul>
     *
     * @see #setupContinueButton()
     * @see #setupSendReportButton()
     * @see #setupStacktraceToggle()
     * @see #setupCrashLogButton()
     */
    private void setupButtonClicks() {
        setupContinueButton();
        setupSendReportButton();
        setupStacktraceToggle();
        setupCrashLogButton();
    }

    /**
     * Configures the click listener for the "Check Crash Log" button. When clicked,
     * this method calls {@link #updateTechnicalDetailsState()} to toggle the
     * visibility of the technical details section based on the checkbox state.
     *
     * <p>This button provides an alternative way for users to show or hide
     * the detailed crash information without interacting with the checkbox directly.
     *
     * @see #updateTechnicalDetailsState()
     */
    private void setupCrashLogButton() {
        binding.btnCheckCrashLog.setOnClickListener(view ->
                updateTechnicalDetailsState());
    }

    /**
     * Configures the click listener for the "Technical Details" button. When clicked,
     * this method toggles the visibility of the stacktrace text view between
     * {@link View#VISIBLE} and {@link View#GONE}. This allows users to expand
     * or collapse the detailed crash stacktrace for better readability.
     *
     * <p>If the stacktrace is currently visible, it becomes hidden; if hidden,
     * it becomes visible. This provides a simple expand/collapse interaction.
     */
    private void setupStacktraceToggle() {
        binding.btnTechnicalDetails.setOnClickListener(view ->
                binding.txtStacktrace.setVisibility(
                        binding.txtStacktrace.getVisibility() ==
                                View.VISIBLE ? View.GONE : View.VISIBLE));
    }

    /**
     * Configures the click listener for the "Send Report" button. When clicked,
     * this method retrieves the crash information from the intent, sends it to
     * the server via the ViewModel, and then programmatically clicks the
     * "Continue Anyway" button to proceed with app navigation.
     *
     * <p>The crash report is sent asynchronously; the user is not blocked while
     * the network request completes. Even if sending fails, the user can still
     * continue using the app.
     *
     * @see #getCrashedInfoFromIntent()
     * @see AppCrashedViewModel#sendCrashInfoToServer(AppCrashedInfo)
     */
    private void setupSendReportButton() {
        binding.btnSendReport.setOnClickListener(view -> {
            AppCrashedInfo crashedInfo = getCrashedInfoFromIntent();
            if (crashedInfo != null) {
                getViewModel().sendCrashInfoToServer(crashedInfo);
                binding.btnContinueAnyway.performClick();
            }
        });
    }

    /**
     * Configures the click listener for the "Continue Anyway" button. When clicked,
     * this method navigates back to the {@link LauncherActivity} to restart the
     * app normally, bypassing the crash loop. A fade animation is applied during
     * the transition.
     *
     * @see LauncherActivity
     * @see ActivityAnimator#animActivityFade(BaseActivity) (Activity)
     */
    private void setupContinueButton() {
        binding.btnContinueAnyway.setOnClickListener(view -> {
            Intent intent = new Intent(AppCrashedActivity.this, LauncherActivity.class);
            ActivityAnimator.animActivityFade(AppCrashedActivity.this);
            startActivity(intent);
            finish();
        });
    }

    /**
     * Toggles the visibility of the technical details section based on the
     * "Include crash log" checkbox state. When the checkbox is checked, the
     * "View Technical Details" button becomes visible; otherwise, it is hidden.
     * Additionally, the stacktrace text view is shown or hidden based on
     * the visibility of the "View Technical Details" button.
     *
     * <p>This method is typically called when the checkbox state changes,
     * allowing the user to expand or collapse crash details for viewing.
     */
    private void updateTechnicalDetailsState() {
        binding.btnTechnicalDetails.setVisibility(
                binding.btnCheckCrashLog.isChecked() ? View.VISIBLE : View.GONE);

        binding.txtStacktrace.setVisibility(
                binding.btnTechnicalDetails.getVisibility() ==
                        View.VISIBLE ? View.VISIBLE : View.GONE);
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
            if (!(packageExtra instanceof AppCrashedInfo)) {
                return null;
            }
            return (AppCrashedInfo) packageExtra;
        } else {
            return null;
        }
    }
}