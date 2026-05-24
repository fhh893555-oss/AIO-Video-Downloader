package userInterface.appUpdater;

import android.content.Intent;
import android.text.Html;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityUpdater1Binding;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

import javax.annotation.Nullable;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.AppDirsValidator;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.StylizedDialogBuilder;
import coreUtils.library.views.TextViewsUtils;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;
import userInterface.openingSplash.OpeningActivity;

public class AppUpdaterActivity extends BaseActivity<ActivityUpdater1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String KEY_INTENT_RECEIVED_KEY = "KEY_INTENT_RECEIVED_KEY";
	private AppUpdaterViewModel viewModel;
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityUpdater1Binding inflateBinding(LayoutInflater inflater) {
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
	
	private void startDownloadLatestApk() {
		UpdateInfo updateInfo = getUpdateInfoPackageFromIntent();
		if (updateInfo == null) return;
		AppDirsValidator.performValidation();
		File applicationDirectory = AppDirsValidator.getApplicationDirectory();
		if (applicationDirectory == null) return;
		
		String subDirName = StringHelper.getText(R.string.title_tubeaio_programs);
		File appProgramsFolder = new File(applicationDirectory, subDirName);
		getViewModel().downloadUpdatedAPK(updateInfo, Objects.requireNonNull(appProgramsFolder), this);
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
		binding.top2.btnInstallUpdate.setOnClickListener(view -> showDemiDialog());
		binding.top2.btnDownloadFromSite.setOnClickListener(view -> openOfficialWebsite());
	}
	
	private void openOfficialWebsite() {
	
	}
	
	private void showDemiDialog() {
		new StylizedDialogBuilder(this)
			.setCancelable(false)
			.setDialogAnimation(R.style.style_dialog_window_fade_animation)
			.setPositiveButtonText(R.string.label_cancel)
			.setCloseOnPositiveButtonClick()
			.show();
	}
	
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
