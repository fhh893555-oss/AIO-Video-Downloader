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
import com.nextgen.databinding.ActivityUpdater0Binding;

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
import coreUtils.library.process.VersionInfo;
import coreUtils.library.storage.FileStorageUtility;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.MessageDialogBuilder;
import coreUtils.library.views.StylizedDialogBuilder;
import coreUtils.library.views.StylizedToastView;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.appUpdater.AppUpdaterViewModel.DownloadStatus;
import userInterface.mainScreen.MainActivity;

public class AppUpdaterActivity extends BaseActivity<ActivityUpdater0Binding> {
	
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

	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}

	@Override
    protected ActivityUpdater0Binding inflateBinding(LayoutInflater inflater) {
        return ActivityUpdater0Binding.inflate(inflater);
	}

	@Override
	protected void onLoadedLayout() {
		initializeViewModel();
		showLatestUpdateVersion();
		showWhatsNewChangelog();
		setupButtonClickEvents();
		observeDownloadStatus();
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
        TextView tvChangelog = binding.updateDetails.tvVersionChangelog;
		tvChangelog.setText(Html.fromHtml(changeLogHtmlString, Html.FROM_HTML_MODE_LEGACY));
	}


    private void setupButtonClickEvents() {
        binding.actionButtons.btnUpdateNow.setOnClickListener(view -> validateAndStartDownload());
        binding.actionButtons.btnRemindLater.setOnClickListener(view -> scheduleUpdateReminder());
        binding.actionButtons.btnVisitOfficial.setOnClickListener(view -> openOfficialWebsite());
	}

    private void scheduleUpdateReminder() {
        AppConfigs appConfigs = AppConfigsRepo.getConfig();
        appConfigs.isUserNeedToRemindForAppUpdate = true;
        appConfigs.save();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        ActivityAnimator.animActivityFade(this);

        finish();
    }

    private void openOfficialWebsite() {
		try {
			String webAddress = StringHelper.getText(R.string.http_tubeaio_official_page_url);
			IntentLinkHelper.openLinkInSystemBrowser(this, webAddress);
		} catch (Exception error) {
			String somethingWentWrong =
                    StringHelper.getText(R.string.label_something_went_wrong);
			StylizedToastView.showError(this, somethingWentWrong);
			logger.error("Error opening system browser for official site: ", error);
		}
	}


    private void observeDownloadStatus() {
		getViewModel().getDownloadStatusLiveData().observe(this, downloadStatus -> {
			if (!isDownloadActive) return;
			if (handleDownloadError(downloadStatus)) return;
			dismissDownloadError();
			updateDownloadProgress(downloadStatus);
			handleDownloadComplete(downloadStatus);
		});
	}

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

	private void updateDownloadProgress(DownloadStatus downloadStatus) {
		int percentage = downloadStatus.getProgress();
		long downloadedByte = downloadStatus.getDownloadedByte();
		long totalFileSizeInByte = downloadStatus.getTotalFileSize();
		long currentTime = System.currentTimeMillis();
		
		String totalInFormat = FileStorageUtility
			.toMegabytesString(totalFileSizeInByte);
		
		String downloadedInFormat = FileStorageUtility
			.toMegabytesString(downloadedByte);
		
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
					FileStorageUtility.toMegabytesString(smoothedSpeed)));
				
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

	private void handleDownloadComplete(DownloadStatus downloadStatus) {
		try {
			if (downloadStatus.getState() == DownloadStatus.State.COMPLETED) {
				File apkFile = downloadStatus.getFile();
				if (apkFile.exists() && apkFile.isFile() && apkFile.length() > 0) {
					isDownloadActive = false;
					if (downloadDialog != null) {
						downloadDialog.close();
						downloadDialog = null;
					}
					FileStorageUtility.openApkFile(this, apkFile);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to install apk file: ", error);
		}
	}

	private boolean handleDownloadError(DownloadStatus downloadStatus) {
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

            binding.updateDetails.tvDownloadError.setText(R.string.label_download_has_failed);
            binding.updateDetails.tvDownloadErrorDetailed.setText(R.string.desc_download_has_failed_reason);
            binding.updateDetails.containerDownloadError.setVisibility(View.VISIBLE);
			return true;
		}
		return false;
	}

	private void dismissDownloadError() {
		binding.updateDetails.tvDownloadError.setText(R.string.label__);
		binding.updateDetails.tvDownloadErrorDetailed.setText(R.string.label__);
		binding.updateDetails.containerDownloadError.setVisibility(View.GONE);
	}

	@NonNull
	private StylizedDialogBuilder getStylizedDialogBuilder() {
		StylizedDialogBuilder downloadDialog = new StylizedDialogBuilder(this);
		downloadDialog.setCustomContentView(R.layout.activity_updater_1_dialog_1);
		downloadDialog.setCancelable(false);
		downloadDialog.enableFadeInAnimation();
		downloadDialog.setOnCloseClickListener(view -> stopDownloadingLatestApk());
		downloadDialog.setPositiveButtonText(R.string.label_cancel_installing);
		downloadDialog.setPositiveButtonIcons(R.drawable.ic_cancel_circle, 0);
		downloadDialog.setOnPositiveClickListener(view -> stopDownloadingLatestApk(), true);
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
		File programsFolder = Objects.requireNonNull(appProgramsFolder);
		getViewModel().downloadUpdatedAPK(updateInfo, programsFolder, this);
	}

	private void stopDownloadingLatestApk() {
		isDownloadActive = false;
		getViewModel().stopDownloadingAPK();
	}

	private void showLatestUpdateVersion() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		
		String latestVersionName = updateInfo.getVersionName();
		int latestVersionCode = updateInfo.getVersionCode();
		String latestVersionInfo = latestVersionName + " (" + latestVersionCode + ")";
        String currentVersionInfo = VersionInfo.getVersionName(getApplicationContext());

        binding.updateDetails.tvVersionName.setText(latestVersionInfo);
        binding.updateDetails.txtCurrentVersion.setText(currentVersionInfo);
		binding.updateDetails.txtLatestVersion.setText(latestVersionInfo);
	}

	private void validateAndStartDownload() {
		if (!FileStorageUtility.hasFullFileSystemAccess(this)) promptForStorageAccess();
		else showDownloaderDialog();
	}

    private void promptForStorageAccess() {
		new MessageDialogBuilder(this)
			.enableBackgroundBlur(60)
			.setCancelable(true)
			.enableFadeInAnimation()
			.setTitle(R.string.label_storage_permission_needed)
			.setMessage(R.string.text_storage_permission_required)
			.setLeftButtonText(R.string.label_cancel)
			.setRightButtonText(R.string.label_allow_now)
			.setLeftButtonIcons(R.drawable.ic_cancel_circle, 0)
			.setRightButtonIcons(R.drawable.ic_okay_check, 0)
			.setOnRightClickListener(view -> requestForAllFilesAccess(), true)
			.show();
	}


    @Nullable
    private UpdateInfo getUpdateInfoPackageFromIntent() {
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

	private String getWhatsNewJSON(@NotNull UpdateInfo updateInfo) {
		return updateInfo.getWhatsNewJSON();
	}
}
