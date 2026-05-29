package sysModules.crashedHandler;

import android.os.Build;

import androidx.annotation.NonNull;

import com.nextgen.BuildConfig;

import java.io.PrintWriter;
import java.io.StringWriter;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceInfoUtils;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.VersionInfo;
import dataRepo.configs.AppConfig;
import dataRepo.configs.AppConfigsRepo;
import dataRepo.dbManager.ObjectBoxHelper;
import dataRepo.user.AppUserRepo;
import io.objectbox.Box;
import userInterface.appCrashed.AppCrashedInfo;

public class AppCrashedHandler implements Thread.UncaughtExceptionHandler {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override public void uncaughtException(@NonNull Thread thread,
	                                        @NonNull Throwable throwable) {
		try {
			logger.debug("Uncaught exception in thread: " + thread.getName());
			
			String stackTrace;
			try (StringWriter sw = new StringWriter();
			     PrintWriter pw = new PrintWriter(sw)) {
				throwable.printStackTrace(pw);
				stackTrace = sw.toString();
			}
			
			logger.debug("Stack trace successfully captured");
			if (!BuildConfig.IS_DEBUG_MODE_ON) return;
			if (stackTrace.isEmpty()) return;
			
			Box<AppCrashedInfo> crashedInfoBox = ObjectBoxHelper.getAppCrashedInfoBox();
			crashedInfoBox.put(getConfiguredAppCrashInfo(stackTrace));
			
			AppConfig appConfig = AppConfigsRepo.getConfig();
			appConfig.hasAppCrashedRecently = true;
			appConfig.save();
			
		} catch (Exception error) {
			logger.error("Secondary error during crash handling: ", error);
		}
	}
	
	private AppCrashedInfo getConfiguredAppCrashInfo(String stackTrace) {
		AppCrashedInfo crashedInfo = new AppCrashedInfo();
		BaseApplication appContext = BaseApplication.AppContext;
		
		crashedInfo.setDeviceId(AppUserRepo.getUser().userDeviceId);
		crashedInfo.setUserCountry(AppUserRepo.getUser().countryCode);
		crashedInfo.setApplicationVersion(VersionInfo.getVersionName(appContext));
		crashedInfo.setAndroidVersion(getAndroidVersion());
		crashedInfo.setStackStraceInfo(stackTrace);
		crashedInfo.setDetailedInfo(DeviceInfoUtils.getDeviceInformation());
		
		return crashedInfo;
	}
	
	@NonNull private static String getAndroidVersion() {
		return "[" + Build.VERSION.SDK_INT + "]" +
			" [" + Build.VERSION.RELEASE + "]";
	}
}
