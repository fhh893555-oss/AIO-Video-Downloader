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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Objects;

import javax.annotation.Nullable;

import coreUtils.base.BaseActivity;
import coreUtils.library.networks.URLUtility;
import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.TimeFormats;
import coreUtils.library.storage.FileStorageUtility;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.StylizedDialogBuilder;
import coreUtils.library.views.StylizedToastView;
import coreUtils.library.views.TextViewsUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.openingSplash.OpeningActivity;

public class AppUpdaterActivity extends BaseActivity<ActivityUpdater1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String KEY_INTENT_RECEIVED_KEY = "KEY_INTENT_RECEIVED_KEY";
	private AppUpdaterViewModel viewModel;
	private long lastProgressBytes = 0;
	private long lastProgressTime = 0;
	private double smoothedSpeed = 0;
	
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
	
	@Override protected void onLoadedLayout() {
		applyGradientToTitle();
		initializeViewModel();
		showLatestUpdateVersion();
		showWhatsNewChangelog();
		setupButtonClickEvents();
		
		// startDownloadLatestApk();
	}
	
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
	
	private void initializeViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		viewModel = viewModelProvider.get(AppUpdaterViewModel.class);
	}
	
	private AppUpdaterViewModel getViewModel() {
		if (viewModel == null) initializeViewModel();
		return viewModel;
	}
	
	private void showWhatsNewChangelog() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		String changeLogHtmlString = updateInfo.getWhatsNewJSON();
		TextView tvChangelog = binding.top2.tvChangelog;
		tvChangelog.setText(Html.fromHtml(changeLogHtmlString, Html.FROM_HTML_MODE_LEGACY));
	}
	
	private void setupButtonClickEvents() {
		binding.btnBack.setOnClickListener(view -> finish());
		binding.top2.btnInstallUpdate.setOnClickListener(view -> showDownloaderDialog());
		binding.top2.btnDownloadFromSite.setOnClickListener(view -> openOfficialWebsite());
	}
	
	private void openOfficialWebsite() {
	
	}
	
	private void showDownloaderDialog() {
		StylizedDialogBuilder downloadDialog = getStylizedDialogBuilder();
		downloadDialog.show();
		
		startDownloadingLatestApk();
		
		View contentView = downloadDialog.getCustomContentView();
		TextView tvPercentage = contentView.findViewById(R.id.tvPercentage);
		TextView tvProgressSize = contentView.findViewById(R.id.tvProgressSize);
		TextView tvSpeedValue = contentView.findViewById(R.id.tvSpeedValue);
		TextView tvTimeValue = contentView.findViewById(R.id.tvTimeValue);
		TextView tvTotalSize = contentView.findViewById(R.id.tvTotalSize);
		ProgressBar pbDownload = contentView.findViewById(R.id.pbDownload);
		
		getViewModel().getDownloadStatusLiveData().observe(this, downloadStatus -> {
			int percentage = downloadStatus.getProgress();
			long downloadedByte = downloadStatus.getDownloadedByte();
			long currentTime = System.currentTimeMillis();
			
			tvPercentage.setText(MessageFormat.format("{0}%", percentage));
			pbDownload.setProgress(percentage);
			
			long totalByte = 0;
			String totalInFormat = "--";
			
			if (percentage > 0) {
				totalByte = (downloadedByte * 100L) / percentage;
				totalInFormat = FileStorageUtility.humanReadableSizeOf(totalByte);
				tvTotalSize.setText(totalInFormat);
			} else {
				tvTotalSize.setText("--");
			}
			
			String downloadedInFormat = FileStorageUtility.humanReadableSizeOf(downloadedByte);
			tvProgressSize.setText(MessageFormat.format("{0} / {1}", downloadedInFormat, totalInFormat));
			
			if (lastProgressTime > 0 && currentTime > lastProgressTime) {
				long timeDelta = currentTime - lastProgressTime;
				long bytesDelta = downloadedByte - lastProgressBytes;
				
				if (bytesDelta >= 0) {
					double currentSpeed = (bytesDelta * 1000.0) / timeDelta;
					
					if (smoothedSpeed == 0) {
						smoothedSpeed = currentSpeed;
					} else {
						smoothedSpeed = (smoothedSpeed * 0.8) + (currentSpeed * 0.2);
					}
					
					tvSpeedValue.setText(MessageFormat.format("{0}/s",
						FileStorageUtility.humanReadableSizeOf((long) smoothedSpeed)));
					
					if (smoothedSpeed > 0 && totalByte > downloadedByte) {
						long bytesRemaining = totalByte - downloadedByte;
						long secondsRemaining = (long) (bytesRemaining / smoothedSpeed);
						
						tvTimeValue.setText(TimeFormats.toHumanReadableTime(secondsRemaining));
					} else if (totalByte == downloadedByte) {
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
		});
	}
	
	@NonNull private StylizedDialogBuilder getStylizedDialogBuilder() {
		StylizedDialogBuilder downloadDialog = new StylizedDialogBuilder(this);
		downloadDialog.setCustomContentView(R.layout.activity_updater_1_dialog_1);
		downloadDialog.setCancelable(false);
		downloadDialog.enableSlideUpAnimation();
		downloadDialog.setOnCloseClickListener(view -> stopDownloading());
		downloadDialog.setPositiveButtonText(R.string.label_cancel_installing);
		downloadDialog.setPositiveButtonIcons(R.drawable.ic_cancel_circle, 0);
		downloadDialog.setOnPositiveClickListener(view -> stopDownloading(), true);
		downloadDialog.setDialogImage(R.drawable.img_updater_act_dialog_top, R.dimen._350);
		downloadDialog.enableBackgroundBlur(60);
		return downloadDialog;
	}
	
	private void startDownloadingLatestApk() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		AppDirsValidator.performValidation();
		File applicationDirectory = AppDirsValidator.getApplicationDirectory();
		if (applicationDirectory == null) return;
		
		String subDirName = StringHelper.getText(R.string.title_tubeaio_programs);
		File appProgramsFolder = new File(applicationDirectory, subDirName);
		getViewModel().downloadUpdatedAPK(updateInfo, Objects.requireNonNull(appProgramsFolder), this);
	}
	
	private void stopDownloading() {
		vibrate();
		StylizedToastView.showError(this, getString(R.string.hint_updating_is_canceled));
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
	 * @return a JSON formatted string describing the update's changes, or null/empty if
	 * not provided
	 */
	private String getWhatsNewJSON(@NotNull UpdateInfo updateInfo) {
		return updateInfo.getWhatsNewJSON();
	}
}
