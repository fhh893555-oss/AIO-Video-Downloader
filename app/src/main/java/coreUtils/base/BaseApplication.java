package coreUtils.base;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.nextgen.BuildConfig;

import coreUtils.library.process.CrashLogWriter;
import coreUtils.library.process.LoggerUtils;
import dataRepo.configs.AppConfigsRepo;
import dataRepo.dbManager.ObjectBoxHelper;
import dataRepo.user.AppUserRepo;
import sysModules.crashedHandler.GlobalCrashedHandler;
import sysModules.interCaches.AppRawFiles;
import sysModules.newPipeLib.cache.YtStreamInfoRepo;
import sysModules.newPipeLib.libs.NewPipeLibraryManager;
import sysModules.ytdlpWrapper.YtDlpLibraryManager;

/**
 * Base Application class that serves as the entry point and global context provider for the app.
 * <p>
 * This class extends Android's {@link Application} and is instantiated before any other
 * component
 * when the application process is created. It manages core initialization tasks including
 * repository setup, library initialization (yt-dlp, NewPipe, Lottie), theme configuration,
 * and provides a static reference to the application context for global access.
 * </p>
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Provides global application context via {@link #getInstance()}</li>
 *   <li>Initializes ObjectBox database repositories</li>
 *   <li>Sets up yt-dlp native library for content downloading</li>
 *   <li>Initializes NewPipe extractor for YouTube metadata parsing</li>
 *   <li>Preloads Lottie animation compositions</li>
 *   <li>Configures theme mode (light/dark) based on system preferences</li>
 *   <li>Cleans up crash log writer and repositories on termination</li>
 * </ul>
 * </p>
 *
 * <p><b>Initialization Order (onCreate):</b>
 * <ol>
 *   <li>Theme configuration</li>
 *   <li>Application context reference</li>
 *   <li>ObjectBox repositories</li>
 *   <li>Lottie compositions preloading</li>
 *   <li>yt-dlp service</li>
 *   <li>NewPipe extractor</li>
 * </ol>
 * </p>
 *
 * @see Application
 * @see ObjectBoxHelper
 * @see YtDlpLibraryManager
 * @see NewPipeLibraryManager
 */
public class BaseApplication extends Application {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static BaseApplication AppContext;
	
	/**
	 * Returns the singleton instance of the BaseApplication.
	 * <p>
	 * This method provides access to the application context from anywhere in the app,
	 * useful for accessing resources, system services, or application-level settings
	 * when an activity context is not available.
	 * </p>
	 *
	 * @return the global BaseApplication instance
	 */
	public static BaseApplication getInstance() {
		return AppContext;
	}
	
	/**
	 * Checks whether the current build is a debug variant.
	 * <p>
	 * This method returns the debug mode flag from build configuration, allowing
	 * features such as verbose logging, development tools, or test endpoints to be
	 * enabled only in debug builds and excluded from release versions.
	 * </p>
	 *
	 * @return true if the app is running a debug build, false for release builds
	 */
	public static boolean isDebugBuild() {
		return BuildConfig.IS_DEBUG_MODE_ON;
	}
	
	/**
	 * Called when the application is first created.
	 * <p>
	 * This lifecycle method performs critical app initialization in the following order:
	 * configures the theme based on system settings, sets the application context,
	 * initializes ObjectBox repositories, preloads Lottie animation compositions,
	 * and sets up the yt-dlp and NewPipe extractor libraries.
	 * </p>
	 */
	@Override
	public void onCreate() {
		configureThemeBySystem();
		super.onCreate();
		AppContext = this;
		registerGlobalCrashHandler();
		initRepositories();
		initLottieComposition();
		initYtDlpService();
		initNewPipeExtractor();
	}
	
	/**
	 * Configures the app's theme mode based on system preferences.
	 * <p>
	 * This method sets the default night mode to MODE_NIGHT_NO, which disables dark mode
	 * and forces light theme regardless of system settings. This ensures consistent
	 * UI appearance across all application screens.
	 * </p>
	 */
	private static void configureThemeBySystem() {
		int modeNightFollowSystem = AppCompatDelegate.MODE_NIGHT_NO;
		AppCompatDelegate.setDefaultNightMode(modeNightFollowSystem);
	}
	
	/**
	 * Called when the application is about to terminate.
	 * <p>
	 * This lifecycle method performs cleanup operations including shutting down the
	 * crash log writer to flush any pending logs and releasing repository resources
	 * to prevent memory leaks before the app process ends.
	 * </p>
	 */
	@Override
	public void onTerminate() {
		CrashLogWriter.shutdown();
		AppUserRepo.release();
		super.onTerminate();
	}
	
	private void registerGlobalCrashHandler() {
		setDefaultUncaughtExceptionHandler(new GlobalCrashedHandler());
	}
	
	/**
	 * Preloads all Lottie animation compositions from raw resources.
	 * <p>
	 * This method loads animation compositions into memory ahead of time to
	 * ensure smooth playback when they are first displayed, preventing UI
	 * jank or delays during animation rendering.
	 * </p>
	 */
	private void initLottieComposition() {
		AppRawFiles.loadAllCompositions();
	}
	
	/**
	 * Initializes all ObjectBox database repositories and measures initialization time.
	 * <p>
	 * This method sets up the core data access layer by initializing ObjectBox helper
	 * and all repository instances including AppConfigsRepo, AppUserRepo, and YtStreamInfoRepo.
	 * The initialization time is logged to help monitor performance and identify slow startups.
	 * </p>
	 *
	 * <p><b>Repositories Initialized:</b>
	 * <ul>
	 *   <li>AppConfigsRepo - application configuration settings</li>
	 *   <li>AppUserRepo - user data and preferences</li>
	 *   <li>YtStreamInfoRepo - YouTube stream information cache</li>
	 * </ul>
	 * </p>
	 */
	private void initRepositories() {
		long startTime = System.currentTimeMillis();
		ObjectBoxHelper.initialize(this);
		AppConfigsRepo.initialize(ObjectBoxHelper.getAppConfigBox());
		AppUserRepo.initialize(ObjectBoxHelper.getAppUserBox());
		YtStreamInfoRepo.initialize(ObjectBoxHelper.getYtStreamInfoBox());
		long endTime = System.currentTimeMillis();
		logger.debug("initRepositories took: " + (endTime - startTime) + " ms");
	}
	
	/**
	 * Initializes the yt-dlp native library for downloading YouTube content.
	 * <p>
	 * This method sets up the yt-dlp library manager, which handles loading of
	 * native binaries and provides functionality for extracting video information
	 * and downloading streams from various platforms.
	 * </p>
	 */
	private void initYtDlpService() {
		YtDlpLibraryManager.initialize(this);
	}
	
	/**
	 * Initializes the NewPipe extractor library for parsing YouTube metadata.
	 * <p>
	 * This method sets up the NewPipe library, which provides a lightweight
	 * extractor for fetching video details, stream URLs, subtitles, and other
	 * metadata from YouTube without requiring the official API.
	 * </p>
	 */
	private void initNewPipeExtractor() {
		NewPipeLibraryManager.initializeNewPipeLibrary();
	}
}
