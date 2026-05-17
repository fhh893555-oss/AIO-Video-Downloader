package userInterface.opening;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import com.airbnb.lottie.LottieAnimationView;
import com.nextgen.R;
import com.nextgen.databinding.ActivityOpening1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.base.BaseApplication;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.TextViewsUtils;
import dataRepo.configs.AppConfigsRepo;
import userInterface.language.LanguageActivity;

public class OpeningActivity extends BaseActivity<ActivityOpening1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityOpening1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityOpening1Binding.inflate(inflater);
	}
	
	@Override
	protected void onLoadedLayout() {
		LottieAnimationView animationView = binding.lavLoading;
		animationView.enableMergePathsForKitKatAndAbove(true);
		
		applyGradientToTitle();
		loadVersionInfo();
		startNextActivity();
	}
	
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
	
	private void loadVersionInfo() {
		BaseApplication app = BaseApplication.AppContext;
		String versionCode = String.valueOf(VersionInfo.getVersionCode(app));
		String versionName = String.valueOf(VersionInfo.getVersionName(app));
		String versionInfo = versionName + " (" + versionCode + ")";
		logger.debug("Version result: " + versionInfo);
		binding.tvVersion.setText(versionInfo);
	}
	
	private void startNextActivity() {
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			Intent intent = new Intent(OpeningActivity.this, LanguageActivity.class);
			if (AppConfigsRepo.getConfig().isLocaleConfigured) {
				logger.debug("Locale is already configured.");
				intent = new Intent(OpeningActivity.this, LanguageActivity.class);
				//todo: change the target class to main activity
			}
			
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			startActivity(intent);
			finish();
			ActivityAnimator.animActivityFade(OpeningActivity.this);
		}, 1000);
	}
}
