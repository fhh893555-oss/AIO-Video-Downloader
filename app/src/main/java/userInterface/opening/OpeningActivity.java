package userInterface.opening;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityOpening1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.base.BaseApplication;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import coreUtils.library.views.ActivityAnimator;
import dataRepo.configs.AppConfigsRepo;
import userInterface.main.MainActivity;
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
        binding.loadingAnimationView.enableMergePathsForKitKatAndAbove(true);
        loadVersionInfo();
    }

    private void loadVersionInfo() {
        BaseApplication app = BaseApplication.AppContext;
        String versionCode = String.valueOf(VersionInfo.getVersionCode(app));
        String versionName = String.valueOf(VersionInfo.getVersionName(app));
        String versionInfo = versionName + " (" + versionCode + ")";
        logger.debug("Version result: " + versionInfo);
        binding.versionInfoText.setText(versionInfo);
        startMainActivity();
    }

    private void startMainActivity() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(OpeningActivity.this, LanguageActivity.class);
            if (AppConfigsRepo.getConfig().isLocaleConfigured) {
                logger.debug("Locale is already configured.");
                intent = new Intent(OpeningActivity.this, MainActivity.class);
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            ActivityAnimator.animActivityFade(OpeningActivity.this);
        }, 1000);
    }
}
