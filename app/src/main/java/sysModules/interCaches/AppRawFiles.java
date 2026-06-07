package sysModules.interCaches;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieCompositionFactory;
import com.airbnb.lottie.LottieResult;
import com.nextgen.R;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

/**
 * A singleton utility class responsible for preloading and caching {@link LottieComposition}
 * objects from raw resources.
 *
 * <p>This class manages the asynchronous, parallel loading of various Lottie animations
 * used throughout the application to ensure smooth UI transitions and prevent
 * frame drops during runtime resource initialization.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li>Parallel loading using an {@link ExecutorService}.</li>
 *   <li>Thread-safe singleton access via {@link #getInstance()}.</li>
 *   <li>Centralized access to common animations such as loading states, success
 *       indicators, and empty states.</li>
 * </ul>
 *
 * @author [Your Name/Organization]
 * @version 1.0
 */
public final class AppRawFiles {
	
	/**
	 * Logger instance for this class, used to track the progress of Lottie resource loading
	 * and log any errors encountered during the parallel initialization process.
	 */
	private static final LoggerUtils logger = LoggerUtils.from(AppRawFiles.class);
	
	/**
	 * The single shared instance of {@link AppRawFiles} used for managing and
	 * accessing cached Lottie animation compositions throughout the application.
	 */
	private static final AppRawFiles instance = new AppRawFiles();
	
	/**
	 * Indicates whether all Lottie animation resources have been successfully
	 * loaded into memory. This flag is used to prevent redundant loading operations
	 * and to ensure thread-safe checks across the application.
	 */
	private volatile boolean isLoadedAllComposition = false;
	
	/**
	 * Cached Lottie composition for the initial layout loading animation.
	 * This animation is typically displayed when the main UI or a specific layout container
	 * is being prepared for the first time.
	 *
	 * @see #getLayoutLoadComposition()
	 */
	private volatile LottieComposition layoutLoadComposition,
		waitingLoadingComposition, noResultEmptyComposition,
		openActiveTasksComposition, downloadReadyComposition,
		successfulDownloadComposition, circleLoadingComposition,
		audioVisualizingV1Composition, audioVisualizingComposition,
		emptyGhostComposition, newVersionUpdateComposition,
		loginRequiredComposition, premiumUserComposition,
		upgradeBoxComposition;
	
	/**
	 * Private constructor to prevent instantiation from outside the class.
	 * This class follows the Singleton pattern and should be accessed via {@link #getInstance()}.
	 */
	private AppRawFiles() {}
	
	/**
	 * Returns the singleton instance of the {@code AppRawFiles} class.
	 * Use this method to access the cached Lottie compositions and loading management.
	 *
	 * @return the global instance of {@link AppRawFiles}.
	 */
	public static AppRawFiles getInstance() {
		return instance;
	}
	
	/**
	 * Asynchronously loads all Lottie animation compositions into memory.
	 * <p>
	 * This method checks if the compositions are already loaded; if not, it initiates a
	 * background task to load the raw animation resources in parallel using an executor service.
	 * Progress is tracked via callbacks, and a log message is generated upon successful completion.
	 * </p>
	 */
	public static void loadAllCompositions() {
		if (instance.isLoadedAllComposition) {
			logger.debug("Compositions already loaded.");
			return;
		}
		
		ThreadTask<Boolean, LottieComposition> loadAllCompositionsTask = new ThreadTask<>();
		loadAllCompositionsTask.setBackgroundTask(progressCallback -> {
			instance.loadSync(progressCallback);
			return true;
		});
		
		loadAllCompositionsTask.setResultTask(result ->
			logger.debug("All compositions loaded into memory."));
		
		loadAllCompositionsTask.start();
	}
	
	/**
	 * Synchronously loads all defined Lottie animation resources into memory using parallel execution.
	 *
	 * <p>This method utilizes a fixed thread pool based on the system's available processors to
	 * decode multiple Lottie compositions simultaneously. It uses a {@link CountDownLatch}
	 * to ensure that the method blocks until all resources have either completed loading or failed.</p>
	 *
	 * @param progressCallback An optional callback to receive updates as each individual
	 *                         {@link LottieComposition} finishes loading.
	 */
	private void loadSync(ThreadTask.ProgressCallback<LottieComposition> progressCallback) {
		if (isLoadedAllComposition) return;
		long startTime = System.currentTimeMillis();
		
		ResourceMapping[] mappings = {
			new ResourceMapping(R.raw.animation_layout_load, comp -> layoutLoadComposition = comp),
			new ResourceMapping(R.raw.animation_no_result, comp -> noResultEmptyComposition = comp),
			new ResourceMapping(R.raw.animation_waiting_loading, comp -> waitingLoadingComposition = comp),
			new ResourceMapping(R.raw.animation_active_tasks, comp -> openActiveTasksComposition = comp),
			new ResourceMapping(R.raw.animation_videos_found, comp -> downloadReadyComposition = comp),
			new ResourceMapping(R.raw.animation_successful, comp -> successfulDownloadComposition = comp),
			new ResourceMapping(R.raw.animation_circle_loading, comp -> circleLoadingComposition = comp),
			new ResourceMapping(R.raw.animation_audio_visualizing_v1, comp -> audioVisualizingV1Composition
				= comp),
			new ResourceMapping(R.raw.animation_audio_visualizing, comp -> audioVisualizingComposition =
				comp),
			new ResourceMapping(R.raw.animation_empty_ghost, comp -> emptyGhostComposition = comp),
			new ResourceMapping(R.raw.animation_new_app_version, comp -> newVersionUpdateComposition = comp),
			new ResourceMapping(R.raw.animation_login_required, comp -> loginRequiredComposition = comp),
			new ResourceMapping(R.raw.animation_premium_user, comp -> premiumUserComposition = comp),
			new ResourceMapping(R.raw.animation_upgrade_box, comp -> upgradeBoxComposition = comp)
		};
		
		int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors());
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
			CountDownLatch latch = new CountDownLatch(mappings.length);
			for (ResourceMapping mapping : mappings) {
				executor.execute(() -> {
					try {
						BaseApplication appContext = BaseApplication.AppContext;
						LottieResult<LottieComposition> result = LottieCompositionFactory
							.fromRawResSync(appContext, mapping.resId);
						
						if (result.getValue() != null) {
							mapping.setter.set(result.getValue());
							if (progressCallback != null) {
								progressCallback.onProgress(result.getValue());
							}
						} else if (result.getException() != null) {
							logger.error("Failed to load Lottie resource: " +
								mapping.resId, result.getException());
						}
					} catch (Exception error) {
						logger.error("Unexpected error loading Lottie resource: " + mapping.resId, error);
					} finally {
						latch.countDown();
					}
				});
			}
			
			try {
				latch.await();
			} catch (InterruptedException error) {
				logger.error("Lottie parallel loading interrupted", error);
				Thread.currentThread().interrupt();
			} finally {
				executor.shutdown();
			}
			
			isLoadedAllComposition = true;
			long endTime = System.currentTimeMillis();
			logger.debug("Parallel Lottie loading finished in " + (endTime - startTime) + " ms");
		}
	}
	
	/**
	 * A helper class that pairs a raw resource ID with a specific callback to store
	 * the loaded {@link LottieComposition}.
	 * <p>
	 * This is used to map animation resources to their corresponding fields in
	 * {@link AppRawFiles} during the bulk loading process.
	 */
	private static class ResourceMapping {
		final int resId;
		final CompositionSetter setter;
		
		ResourceMapping(int resId, CompositionSetter setter) {
			this.resId = resId;
			this.setter = setter;
		}
	}
	
	/**
	 * Functional interface used to map a loaded {@link LottieComposition} to its
	 * corresponding member variable within {@link AppRawFiles}.
	 * <p>
	 * This is used during the bulk loading process to decouple the loading logic
	 * from the assignment of the resulting composition.
	 */
	private interface CompositionSetter {
		void set(LottieComposition composition);
	}
	
	/**
	 * Gets the preloaded Lottie composition for the layout loading animation.
	 * This animation is typically used as a placeholder while the main layout or UI components
	 * are being prepared.
	 *
	 * @return the {@link LottieComposition} for the animation_layout_load resource,
	 * or {@code null} if it hasn't been loaded yet.
	 */
	public LottieComposition getLayoutLoadComposition() {return layoutLoadComposition;}
	
	/**
	 * Gets the cached Lottie composition for the waiting/loading animation.
	 *
	 * @return The {@link LottieComposition} for {@code R.raw.animation_waiting_loading},
	 * or {@code null} if it has not been loaded yet.
	 */
	public LottieComposition getWaitingLoadingComposition() {return waitingLoadingComposition;}
	
	/**
	 * Gets the preloaded Lottie composition for the "no result" or empty state animation.
	 * This animation is typically displayed when a search or list returns no data.
	 *
	 * @return the {@link LottieComposition} for the animation_no_result resource,
	 * or {@code null} if it hasn't been loaded yet.
	 */
	public LottieComposition getNoResultEmptyComposition() {return noResultEmptyComposition;}
	
	/**
	 * Gets the preloaded Lottie composition for the active tasks' animation.
	 * This animation is typically used to represent ongoing or pending background tasks.
	 *
	 * @return The {@link LottieComposition} for the active tasks animation,
	 * or {@code null} if it hasn't been loaded yet.
	 */
	public LottieComposition getOpenActiveTasksComposition() {return openActiveTasksComposition;}
	
	/**
	 * Retrieves the cached Lottie composition for the "download ready" or "videos found" animation.
	 * This animation is typically used when media has been successfully scanned and is available for
	 * download.
	 *
	 * @return The preloaded {@link LottieComposition} corresponding to {@code R.raw.animation_videos_found}.
	 */
	public LottieComposition getDownloadReadyComposition() {return downloadReadyComposition;}
	
	/**
	 * Returns the cached Lottie composition for the successful download animation.
	 * This animation is typically displayed when a download task completes successfully.
	 *
	 * @return The {@link LottieComposition} for the success state, or {@code null} if not yet loaded.
	 */
	public LottieComposition getSuccessfulDownloadComposition() {return successfulDownloadComposition;}
	
	/**
	 * Gets the preloaded Lottie composition for the circle loading animation.
	 * This animation is typically used to indicate a background process or indeterminate loading state.
	 *
	 * @return The {@link LottieComposition} for the circle loading animation,
	 * or {@code null} if it hasn't been loaded yet.
	 */
	public LottieComposition getCircleLoadingComposition() {return circleLoadingComposition;}
	
	/**
	 * Gets the cached Lottie composition for the version 1 audio visualization animation.
	 * This composition is loaded from {@code R.raw.animation_audio_visualizing_v1}.
	 *
	 * @return The {@link LottieComposition} for audio visualizing V1, or {@code null} if not yet loaded.
	 */
	public LottieComposition getAudioVisualizingV1Composition() {return audioVisualizingV1Composition;}
	
	/**
	 * Returns the cached Lottie composition for the audio visualizing animation.
	 *
	 * @return the {@link LottieComposition} for audio visualization, or {@code null} if not yet loaded.
	 */
	public LottieComposition getAudioVisualizingComposition() {return audioVisualizingComposition;}
	
	/**
	 * Retrieves the cached Lottie composition for the empty ghost animation.
	 * This animation is typically used to represent empty states or "no results" scenarios.
	 *
	 * @return the {@link LottieComposition} for the empty ghost resource, or {@code null}
	 * if it has not been loaded yet.
	 */
	public LottieComposition getEmptyGhostComposition() {return emptyGhostComposition;}
	
	/**
	 * Retrieves the cached Lottie composition for the new version update animation.
	 * This animation is associated with the {@code R.raw.animation_new_app_version} resource.
	 *
	 * @return The {@link LottieComposition} used to indicate that a new version of the app is available,
	 * or {@code null} if the resource has not yet been loaded.
	 */
	public LottieComposition getNewVersionUpdateComposition() {return newVersionUpdateComposition;}
	
	/**
	 * Gets the cached Lottie composition for the "login required" animation.
	 * This animation is typically used to prompt users to sign in to access specific features.
	 *
	 * @return The {@link LottieComposition} for the login required state,
	 * or {@code null} if it hasn't been loaded yet.
	 */
	public LottieComposition getLoginRequiredComposition() {return loginRequiredComposition;}
	
	/**
	 * Gets the cached Lottie composition for the premium user animation.
	 * This animation is typically displayed to indicate premium status or features.
	 *
	 * @return The {@link LottieComposition} for the premium user resource,
	 * or {@code null} if it hasn't been loaded yet.
	 */
	public LottieComposition getPremiumUserComposition() {return premiumUserComposition;}
	
	/**
	 * Retrieves the cached Lottie composition for the upgrade box animation.
	 *
	 * @return the {@link LottieComposition} for the upgrade box, or {@code null} if not yet loaded.
	 */
	public LottieComposition getUpgradeBoxComposition() {return upgradeBoxComposition;}
	
	/**
	 * Checks whether all Lottie compositions have been successfully loaded into memory.
	 *
	 * @return {@code true} if all animation resources are loaded and ready for use;
	 * {@code false} otherwise.
	 */
	public boolean isLoadedAllComposition() {
		return isLoadedAllComposition;
	}
}
