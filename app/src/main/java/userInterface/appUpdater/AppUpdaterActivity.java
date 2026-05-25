package userInterface.appUpdater;

import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityUpdater1Binding;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nullable;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.IntentLinkHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.TimeFormats;
import coreUtils.library.storage.FileStorageUtility;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.StylizedDialogBuilder;
import coreUtils.library.views.StylizedToastView;
import coreUtils.library.views.TextViewsUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.openingSplash.OpeningActivity;

/**
 * Activity responsible for displaying update information and managing APK downloads.
 * <p>
 * This activity shows the latest available app version details, displays the "What's New"
 * changelog in HTML format, and provides options to either download the update directly
 * or open the official website for manual download. It integrates with a ViewModel to
 * manage the download process with support for progress tracking, speed calculation,
 * estimated time remaining, and error handling.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Displays latest version information from UpdateInfo intent extra</li>
 *   <li>Renders HTML-formatted changelog using legacy HTML mode</li>
 *   <li>Gradient styling applied to "Available" text in title</li>
 *   <li>Dialog-based download progress UI with live updates</li>
 *   <li>Download speed smoothing using exponential moving average</li>
 *   <li>Resumable downloads with hash verification</li>
 *   <li>Option to open official website if download is not desired</li>
 * </ul>
 * </p>
 *
 * <p><b>UI Layout:</b>
 * Uses {@code ActivityUpdater1Binding} with two main sections:
 * <ul>
 *   <li>top1 - Contains the updater title</li>
 *   <li>top2 - Contains version info, changelog, and action buttons</li>
 * </ul>
 * </p>
 *
 * <p><b>Expected Intent Extra:</b>
 * Requires {@link #KEY_INTENT_RECEIVED_KEY} with an {@link UpdateInfo} object
 * containing version details, APK URL, and changelog JSON.
 * </p>
 *
 * @see BaseActivity
 * @see ActivityUpdater1Binding
 * @see AppUpdaterViewModel
 * @see UpdateInfo
 */
public class AppUpdaterActivity extends BaseActivity<ActivityUpdater1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String KEY_INTENT_RECEIVED_KEY = "KEY_INTENT_RECEIVED_KEY";
	
	private StylizedDialogBuilder downloadDialog;
	private AppUpdaterViewModel viewModel;
	
	private long lastProgressBytes = 0;
	private long lastProgressTime = 0;
	private double smoothedSpeed = 0;
	private boolean isDownloadActive = false;
	
	private TextView tvPercentage, tvProgressSize, tvSpeedValue;
	private TextView tvTimeValue, tvTotalSize;
	private ProgressBar pbDownload;
	
	/**
	 * Locks the activity orientation to prevent configuration changes during update operations.
	 * <p>
	 * Returns true to lock the screen orientation, preventing the system from recreating
	 * the activity when the device is rotated. This ensures that the APK download process
	 * continues uninterrupted and the update UI remains stable during downloads.
	 * </p>
	 *
	 * @return true to lock orientation, false to allow rotation
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for the app updater activity.
	 * <p>
	 * Uses the generated ActivityUpdater1Binding class to inflate the layout XML,
	 * providing type-safe access to all UI components including version displays,
	 * download progress indicators, and control buttons for the update process.
	 * </p>
	 *
	 * @param inflater LayoutInflater instance used to inflate the layout
	 * @return ActivityUpdater1Binding containing references to all UI elements
	 */
	@Override
	protected ActivityUpdater1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityUpdater1Binding.inflate(inflater);
	}
	
	/**
	 * Called after the layout has been inflated and is ready for initialization.
	 * <p>
	 * This lifecycle method performs the following setup tasks in order:
	 * <ol>
	 *   <li>Applies gradient styling to the title text</li>
	 *   <li>Initializes the ViewModel for download management</li>
	 *   <li>Displays the latest available update version information</li>
	 *   <li>Renders the "What's New" changelog using HTML formatting</li>
	 *   <li>Configures click event handlers for all interactive buttons</li>
	 *   <li>Sets up LiveData observer for download status updates</li>
	 * </ol>
	 * </p>
	 */
	@Override
	protected void onLoadedLayout() {
		applyGradientToTitle();
		initializeViewModel();
		showLatestUpdateVersion();
		showWhatsNewChangelog();
		setupButtonClickEvents();
		observeDownloadStatus();
	}
	
	/**
	 * Applies a color gradient to the "Available" substring in the title TextView.
	 * <p>
	 * This method searches for the word "Available" within the updater title text.
	 * If found, it uses TextViewsUtils to apply a gradient span transitioning from
	 * secondary color to primary variant color across the 9 characters of the word,
	 * creating a visually appealing emphasis effect for the update availability message.
	 * </p>
	 *
	 * <p><b>UI Component:</b>
	 * {@code binding.top1.tvUpdaterTitle} - TextView containing the updater title
	 * </p>
	 *
	 * <p><b>Gradient Colors:</b>
	 * <ul>
	 *   <li>Start: {@code R.color.color_secondary}</li>
	 *   <li>End: {@code R.color.color_primary_variant}</li>
	 * </ul>
	 * </p>
	 */
	private void applyGradientToTitle() {
		String fullText = binding.top1.tvUpdaterTitle.getText().toString();
		int nextGenStart = fullText.indexOf("Available");
		if (nextGenStart != -1) {
			TextViewsUtils.applyGradientSpan(
				binding.top1.tvUpdaterTitle,
				getColor(R.color.color_secondary),
				getColor(R.color.color_primary_variant),
				nextGenStart,
				nextGenStart + 9
			);
		}
	}
	
	/**
	 * Initializes the AppUpdaterViewModel instance for this activity.
	 * <p>
	 * This method creates a ViewModelProvider scoped to this activity and retrieves
	 * the AppUpdaterViewModel. The ViewModel survives configuration changes and
	 * maintains download state across screen rotations. If the ViewModel was already
	 * initialized, this method will return the existing instance.
	 * </p>
	 */
	private void initializeViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		viewModel = viewModelProvider.get(AppUpdaterViewModel.class);
	}
	
	/**
	 * Returns the AppUpdaterViewModel instance, initializing it if necessary.
	 * <p>
	 * This lazy initialization method ensures the ViewModel is created on first access
	 * rather than in onCreate(), which can improve startup performance. Subsequent calls
	 * return the cached instance.
	 * </p>
	 *
	 * @return the AppUpdaterViewModel instance associated with this activity
	 */
	private AppUpdaterViewModel getViewModel() {
		if (viewModel == null) initializeViewModel();
		return viewModel;
	}
	
	/**
	 * Displays the "What's New" changelog in the UI using HTML formatting.
	 * <p>
	 * This method retrieves the UpdateInfo from the intent, extracts the JSON changelog
	 * string, and renders it as HTML in the tvChangelog TextView. The changelog typically
	 * contains formatted text with bullet points, bold text, and line breaks describing
	 * new features, bug fixes, and improvements in the update.
	 * </p>
	 *
	 * <p><b>HTML Rendering:</b>
	 * Uses {@link Html#fromHtml(String, int)} with {@link Html#FROM_HTML_MODE_LEGACY}
	 * for compatibility with older Android versions.
	 * </p>
	 *
	 * <p><b>UI Component:</b>
	 * {@code binding.top2.tvChangelog} - TextView where the formatted changelog is displayed
	 * </p>
	 */
	private void showWhatsNewChangelog() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		String changeLogHtmlString = updateInfo.getWhatsNewJSON();
		TextView tvChangelog = binding.top2.tvChangelog;
		tvChangelog.setText(Html.fromHtml(changeLogHtmlString, Html.FROM_HTML_MODE_LEGACY));
	}
	
	/**
	 * Configures click event handlers for all interactive buttons in the activity.
	 * <p>
	 * This method sets up OnClickListeners for the following UI elements:
	 * <ul>
	 *   <li><b>btnBack:</b> Closes the current activity and returns to previous screen</li>
	 *   <li><b>btnInstallUpdate:</b> Initiates the download process by showing the download dialog</li>
	 *   <li><b>btnDownloadFromSite:</b> Opens the official website in an external browser</li>
	 * </ul>
	 * </p>
	 */
	private void setupButtonClickEvents() {
		binding.btnBack.setOnClickListener(view -> finish());
		binding.top2.btnInstallUpdate.setOnClickListener(view -> showDownloaderDialog());
		binding.top2.btnDownloadFromSite.setOnClickListener(view -> openOfficialWebsite());
	}
	
	/**
	 * Opens the official application website in the device's system browser.
	 * <p>
	 * This method retrieves the official website URL from string resources and attempts
	 * to launch it using the default system browser via an implicit intent. Any exceptions
	 * during the process are caught and logged without crashing the application.
	 * </p>
	 *
	 * <p><b>URL Resource:</b>
	 * {@code R.string.http_tubeaio_official_page_url} - The official website address
	 * </p>
	 *
	 * <p><b>Error Handling:</b>
	 * If opening the browser fails (e.g., no browser app available, malformed URL),
	 * the error is logged but not shown to the user to maintain a smooth UX.
	 * </p>
	 */
	private void openOfficialWebsite() {
		try {
			String webAddress = StringHelper.getText(R.string.http_tubeaio_official_page_url);
			IntentLinkHelper.openLinkInSystemBrowser(this, webAddress);
		} catch (Exception error) {
			String somethingWentWrong = StringHelper.getText(R.string.label_something_went_wrong);
			StylizedToastView.showError(this, somethingWentWrong);
			logger.error("Error opening system browser for official site: ", error);
		}
	}
	
	/**
	 * Observes the download status LiveData and updates the UI accordingly.
	 * <p>
	 * This method sets up a LiveData observer that reacts to changes in the download status.
	 * When a new status is received, it first checks if the download is still active,
	 * then handles any errors, dismisses the error UI if no error exists, and finally
	 * updates the progress display. The observer is automatically cleaned up when the
	 * associated LifecycleOwner (this activity) is destroyed.
	 * </p>
	 *
	 * <p><b>Status Processing Flow:</b>
	 * <ol>
	 *   <li>Skip update if download is not active</li>
	 *   <li>Check and handle any download errors</li>
	 *   <li>Clear error UI if no errors present</li>
	 *   <li>Update progress bars and text displays</li>
	 * </ol>
	 * </p>
	 */
	private void observeDownloadStatus() {
		getViewModel().getDownloadStatusLiveData().observe(this, downloadStatus -> {
			if (!isDownloadActive) return;
			if (handleDownloadError(downloadStatus)) return;
			dismissDownloadError();
			updateDownloadProgress(downloadStatus);
		});
	}
	
	/**
	 * Displays the download progress dialog and initiates the APK download.
	 * <p>
	 * This method creates and shows a styled dialog for download progress tracking.
	 * It sets the download active flag, starts the actual download process via the ViewModel,
	 * and initializes all UI component references from the dialog's custom content view.
	 * If a download is already active, the method returns without creating a new dialog.
	 * </p>
	 *
	 * <p><b>UI Components Initialized:</b>
	 * <ul>
	 *   <li>tvPercentage - percentage text (e.g., "45%")</li>
	 *   <li>tvProgressSize - downloaded/total size display</li>
	 *   <li>tvSpeedValue - download speed display</li>
	 *   <li>tvTimeValue - estimated time remaining</li>
	 *   <li>tvTotalSize - total file size display</li>
	 *   <li>pbDownload - progress bar</li>
	 * </ul>
	 * </p>
	 */
	private void showDownloaderDialog() {
		if (isDownloadActive) return;
		downloadDialog = getStylizedDialogBuilder();
		downloadDialog.show();
		isDownloadActive = true;
		
		startDownloadingLatestApk();
		
		View contentView = downloadDialog.getCustomContentView();
		tvPercentage = contentView.findViewById(R.id.tvPercentage);
		tvProgressSize = contentView.findViewById(R.id.tvProgressSize);
		tvSpeedValue = contentView.findViewById(R.id.tvSpeedValue);
		tvTimeValue = contentView.findViewById(R.id.tvTimeValue);
		tvTotalSize = contentView.findViewById(R.id.tvTotalSize);
		pbDownload = contentView.findViewById(R.id.pbDownload);
	}
	
	/**
	 * Updates the UI with current download progress, speed, and estimated time remaining.
	 * <p>
	 * This method extracts progress data from the DownloadStatus and updates all relevant
	 * UI components including percentage text, progress bar, downloaded/total size display,
	 * download speed, and estimated time remaining. It implements speed smoothing using an
	 * exponential moving average (EMA) to provide stable speed readings that aren't overly
	 * sensitive to momentary fluctuations.
	 * </p>
	 *
	 * <p><b>UI Components Updated:</b>
	 * <ul>
	 *   <li>tvPercentage - download completion percentage (e.g., "45%")</li>
	 *   <li>pbDownload - progress bar (0-100)</li>
	 *   <li>tvTotalSize - total file size in human-readable format</li>
	 *   <li>tvProgressSize - downloaded size vs total size (e.g., "5.2 MB / 10 MB")</li>
	 *   <li>tvSpeedValue - current download speed (e.g., "1.5 MB/s")</li>
	 *   <li>tvTimeValue - estimated time remaining (e.g., "00:02:30")</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Speed Calculation:</b>
	 * Speed is calculated based on bytes downloaded between consecutive updates.
	 * An exponential moving average (80% previous, 20% current) is applied to smooth
	 * out fluctuations and provide a more stable reading.
	 * </p>
	 *
	 * <p><b>Edge Cases:</b>
	 * <ul>
	 *   <li>Before first progress update: Shows "Connecting" for speed</li>
	 *   <li>When total size is 0: Shows "--" for total size</li>
	 *   <li>When download is finishing: Shows "Finishing" for time</li>
	 *   <li>When speed cannot be calculated: Shows "--:--" for time</li>
	 * </ul>
	 * </p>
	 *
	 * @param downloadStatus the current download status containing progress, bytes downloaded,
	 *                       and total file size information
	 */
	private void updateDownloadProgress(AppUpdaterViewModel.DownloadStatus downloadStatus) {
		int percentage = downloadStatus.getProgress();
		long downloadedByte = downloadStatus.getDownloadedByte();
		long totalFileSizeInByte = downloadStatus.getTotalFileSize();
		long currentTime = System.currentTimeMillis();
		
		String totalInFormat = FileStorageUtility
			.humanReadableSizeOf(totalFileSizeInByte);
		
		String downloadedInFormat = FileStorageUtility
			.humanReadableSizeOf(downloadedByte);
		
		tvPercentage.setText(MessageFormat.format("{0}%", percentage));
		pbDownload.setProgress(percentage);
		
		if (percentage > 0) tvTotalSize.setText(totalInFormat);
		else tvTotalSize.setText("--");
		
		tvProgressSize.setText(MessageFormat.format("{0} / {1}",
			downloadedInFormat, totalInFormat));
		
		if (lastProgressTime > 0 && currentTime > lastProgressTime) {
			long timeDelta = currentTime - lastProgressTime;
			long bytesDelta = downloadedByte - lastProgressBytes;
			
			if (bytesDelta >= 0) {
				double currentSpeed = (bytesDelta * 1000.0) / timeDelta;
				
				if (smoothedSpeed == 0) smoothedSpeed = currentSpeed;
				else smoothedSpeed = (smoothedSpeed * 0.8) + (currentSpeed * 0.2);
				
				tvSpeedValue.setText(MessageFormat.format("{0}/s",
					FileStorageUtility.humanReadableSizeOf((long) smoothedSpeed)));
				
				if (smoothedSpeed > 0 && totalFileSizeInByte > downloadedByte) {
					long bytesRemaining = totalFileSizeInByte - downloadedByte;
					long secondsRemaining = (long) (bytesRemaining / smoothedSpeed);
					
					tvTimeValue.setText(TimeFormats.toHumanReadableTime(secondsRemaining));
				} else if (totalFileSizeInByte == downloadedByte) {
					tvTimeValue.setText(R.string.label_finishing);
				} else {
					tvTimeValue.setText("--:--");
				}
			}
		} else {
			tvSpeedValue.setText(R.string.label_connecting);
			tvTimeValue.setText("--:--");
		}
		
		lastProgressBytes = downloadedByte;
		lastProgressTime = currentTime;
	}
	
	/**
	 * Handles download error states by resetting UI components and displaying error messages.
	 * <p>
	 * This method checks if the provided DownloadStatus contains an error. If an error is present,
	 * it deactivates the download flag, closes and nullifies any active download dialog, clears
	 * all download-related UI references, and displays error messages in the error container.
	 * </p>
	 *
	 * <p><b>Actions Performed on Error:</b>
	 * <ul>
	 *   <li>Sets isDownloadActive to false</li>
	 *   <li>Closes and releases the download dialog if present</li>
	 *   <li>Nullifies all progress tracking UI references</li>
	 *   <li>Displays generic error messages in the error container</li>
	 *   <li>Makes the error container visible</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>UI References Cleared:</b>
	 * tvPercentage, pbDownload, tvTotalSize, tvProgressSize, tvSpeedValue, tvTimeValue
	 * </p>
	 *
	 * @param downloadStatus the current download status containing potential error information
	 * @return true if an error was handled (error present and non-empty), false otherwise
	 */
	private boolean handleDownloadError(AppUpdaterViewModel.DownloadStatus downloadStatus) {
		if (downloadStatus.getError() == null) return false;
		if (!downloadStatus.getError().isEmpty()) {
			isDownloadActive = false;
			if (downloadDialog != null) {
				downloadDialog.close();
				downloadDialog = null;
			}
			tvPercentage = null;
			pbDownload = null;
			tvTotalSize = null;
			tvProgressSize = null;
			tvSpeedValue = null;
			tvTimeValue = null;
			
			binding.top2.tvDownloadError.setText(R.string.title_download_has_failed);
			binding.top2.tvDownloadErrorDetailed.setText(R.string.title_download_has_failed_reason);
			binding.top2.containerDownloadError.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}
	
	/**
	 * Clears and hides the download error UI components.
	 * <p>
	 * This method resets the error message TextViews to empty strings and hides the
	 * error container view. It is typically called when a new download attempt begins
	 * or when the user acknowledges the error.
	 * </p>
	 *
	 * <p><b>UI Components Affected:</b>
	 * <ul>
	 *   <li>tvDownloadError - primary error message</li>
	 *   <li>tvDownloadErrorDetailed - detailed error description</li>
	 *   <li>containerDownloadError - parent container for error messages</li>
	 * </ul>
	 * </p>
	 */
	private void dismissDownloadError() {
		binding.top2.tvDownloadError.setText(R.string.label__);
		binding.top2.tvDownloadErrorDetailed.setText(R.string.label__);
		binding.top2.containerDownloadError.setVisibility(View.GONE);
	}
	
	/**
	 * Creates and configures a styled dialog for download progress and cancellation.
	 * <p>
	 * This builder method constructs a StylizedDialog that appears during the download
	 * process. The dialog is non-cancelable, features a slide-up animation, background
	 * blur effect, and provides a positive button to cancel the download. Both the
	 * close icon and positive button trigger cancellation of the ongoing download.
	 * </p>
	 *
	 * @return a fully configured StylizedDialogBuilder instance ready to be shown
	 */
	@NonNull
	private StylizedDialogBuilder getStylizedDialogBuilder() {
		StylizedDialogBuilder downloadDialog = new StylizedDialogBuilder(this);
		downloadDialog.setCustomContentView(R.layout.activity_updater_1_dialog_1);
		downloadDialog.setCancelable(false);
		downloadDialog.enableSlideUpAnimation();
		downloadDialog.setOnCloseClickListener(view -> stopDownloadingLatestApk());
		downloadDialog.setPositiveButtonText(R.string.label_cancel_installing);
		downloadDialog.setPositiveButtonIcons(R.drawable.ic_cancel_circle, 0);
		downloadDialog.setOnPositiveClickListener(view -> stopDownloadingLatestApk(), true);
		downloadDialog.setDialogImage(R.drawable.img_updater_act_dialog_top, R.dimen._350);
		downloadDialog.enableBackgroundBlur(60);
		return downloadDialog;
	}
	
	/**
	 * Initiates the download of the latest APK update.
	 * <p>
	 * This method retrieves the UpdateInfo from the intent, validates the application
	 * storage directory using AppDirsValidator, creates a subdirectory for app programs,
	 * and delegates the actual download to the ViewModel. The download supports resume
	 * functionality and includes hash verification for integrity checking.
	 * </p>
	 *
	 * <p><b>Directory Structure:</b>
	 * The APK is saved to: [ApplicationDirectory]/[subDirectoryName]/
	 * where subDirectoryName is the localized string for "tubeaio_programs".
	 * </p>
	 *
	 * <p><b>Preconditions:</b>
	 * <ul>
	 *   <li>UpdateInfo must be present in intent extras</li>
	 *   <li>Application directory must be validated and accessible</li>
	 *   <li>App programs folder must be non-null</li>
	 * </ul>
	 * </p>
	 */
	private void startDownloadingLatestApk() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		
		AppDirsValidator.performValidation();
		File applicationDirectory = AppDirsValidator.getApplicationDirectory();
		if (applicationDirectory == null) return;
		
		String subDirName = StringHelper.getText(R.string.title_tubeaio_programs);
		File appProgramsFolder = new File(applicationDirectory, subDirName);
		File programsFolder = Objects.requireNonNull(appProgramsFolder);
		getViewModel().downloadUpdatedAPK(updateInfo, programsFolder, this);
	}
	
	/**
	 * Stops the ongoing latest APK download operation.
	 * <p>
	 * This method sets the download active flag to false and delegates the cancellation
	 * to the ViewModel. The ViewModel will cancel the background task and any active
	 * network request. Partial downloaded files may remain on disk to allow resuming
	 * if the download is restarted later.
	 * </p>
	 */
	private void stopDownloadingLatestApk() {
		isDownloadActive = false;
		getViewModel().stopDownloadingAPK();
	}
	
	/**
	 * Displays the latest available update version information in the UI.
	 * <p>
	 * This method retrieves the UpdateInfo object from the activity intent, extracts
	 * the version name and version code, formats them as "versionName (versionCode)",
	 * and displays the result in the version info TextView located in the top section
	 * of the layout. If no update information is available, the method returns silently.
	 * </p>
	 *
	 * <p><b>UI Component:</b>
	 * The version info is displayed in {@code binding.top2.tvAppVersionInfo}.
	 * </p>
	 */
	private void showLatestUpdateVersion() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		
		String versionName = updateInfo.getVersionName();
		int versionCode = updateInfo.getVersionCode();
		String versionInfo = versionName + " (" + versionCode + ")";
		binding.top2.tvAppVersionInfo.setText(versionInfo);
	}
	
	/**
	 * Extracts the UpdateInfo object from the activity's intent extras.
	 * <p>
	 * This method retrieves the serialized UpdateInfo that was passed to this activity
	 * via the intent's extra bundle using {@link #KEY_INTENT_RECEIVED_KEY} as the key.
	 * It performs type safety checks to ensure the extracted object is properly
	 * typed as an UpdateInfo instance before returning it.
	 * </p>
	 *
	 * <p><b>Usage Context:</b>
	 * Typically called when this activity is launched from {@link OpeningActivity}
	 * after an update is detected. The UpdateInfo contains version details and the
	 * APK download URL for the available update.
	 * </p>
	 *
	 * @return the UpdateInfo object extracted from the intent, or {@code null} if
	 * the intent contains no serializable extra under the expected key,
	 * or if the extra is not an instance of UpdateInfo
	 */
	private @Nullable UpdateInfo getUpdateInfoPackageFromIntent() {
		Intent intent = getIntent();
		Serializable packageExtra =
			intent.getSerializableExtra(KEY_INTENT_RECEIVED_KEY);
		
		if (packageExtra != null) {
			if (!(packageExtra instanceof UpdateInfo)) {return null;}
			return (UpdateInfo) packageExtra;
		} else {
			return null;
		}
	}
	
	/**
	 * Retrieves the JSON changelog string from the provided UpdateInfo object.
	 * <p>
	 * This convenience method simply delegates to {@link UpdateInfo#getWhatsNewJSON()}
	 * to obtain the structured "What's New" content. The returned JSON typically contains
	 * information about new features, bug fixes, and improvements in the update.
	 * </p>
	 *
	 * @param updateInfo the UpdateInfo object containing version and changelog details
	 * @return a JSON formatted string describing the update's changes, or null/empty if not provided
	 */
	private String getWhatsNewJSON(@NotNull UpdateInfo updateInfo) {
		return updateInfo.getWhatsNewJSON();
	}
}
