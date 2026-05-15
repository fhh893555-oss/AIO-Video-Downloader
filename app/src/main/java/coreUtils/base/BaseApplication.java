package coreUtils.base;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.nextgen.BuildConfig;

import coreUtils.library.process.CrashLogWriter;
import coreUtils.library.process.LoggerUtils;
import dataRepo.configs.AppConfigsRepo;
import dataRepo.manager.ObjectBoxHelper;
import dataRepo.user.AppUserRepo;
import sysModules.interCaches.AppRawFiles;
import sysModules.newPipeLib.cache.YtStreamInfoRepo;
import sysModules.newPipeLib.libs.NewPipeLibraryManager;
import sysModules.ytdlpWrapper.libs.YtDlpLibraryManager;

public class BaseApplication extends Application {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static BaseApplication AppContext;
	
	public static BaseApplication getInstance() {
		return AppContext;
	}
	
	public static boolean isDebugBuild() {
		return BuildConfig.IS_DEBUG_MODE_ON;
	}
	
	@Override
	public void onCreate() {
		configureThemeBySystem();
		super.onCreate();
		AppContext = this;
		initRepositories();
		initLottieComposition();
		initYtDlpService();
		initNewPipeExtractor();
	}
	
	private static void configureThemeBySystem() {
		int modeNightFollowSystem = AppCompatDelegate.MODE_NIGHT_NO;
		AppCompatDelegate.setDefaultNightMode(modeNightFollowSystem);
	}
	
	@Override
	public void onTerminate() {
		YtDlpLibraryManager.shutdown();
		CrashLogWriter.shutdown();
		super.onTerminate();
	}
	
	private void initLottieComposition() {
		AppRawFiles.loadAllCompositions();
	}
	
	private void initRepositories() {
		long startTime = System.currentTimeMillis();
		ObjectBoxHelper.initialize(this);
		AppConfigsRepo.initialize(ObjectBoxHelper.getAppConfigBox());
		AppUserRepo.initialize(ObjectBoxHelper.getAppUserBox());
		YtStreamInfoRepo.initialize(ObjectBoxHelper.getYtStreamInfoBox());
		long endTime = System.currentTimeMillis();
		logger.debug("initRepositories took: " + (endTime - startTime) + " ms");
	}
	
	private void initYtDlpService() {
		YtDlpLibraryManager.initialize(this);
	}
	
	private void initNewPipeExtractor() {
		NewPipeLibraryManager.initializeNewPipeLibrary();
	}
}
