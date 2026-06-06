package sysModules.player.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.EngineCallbacks;
import sysModules.player.engine.MediaEngine;
import sysModules.player.model.PlayerType;
import sysModules.player.queue.PlayQueue;
import sysModules.player.queue.QueueSyncManager;

/**
 * A singleton bridge class that provides centralized access to core playback
 * components including the {@link PlaybackService}, {@link MediaEngine},
 * and {@link QueueSyncManager}. This class acts as a service locator, allowing
 * various parts of the application to obtain references to these components
 * without passing them through deep call chains or using global state.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Maintains singleton references to playback service and engine.</li>
 * <li>Provides thread-safe getter methods for all components.</li>
 * <li>Manages initialization and cleanup of component references.</li>
 * <li>Enables callback registration for playback events.</li>
 * <li>Supports queue binding for system media controls integration.</li>
 * </ul>
 *
 * <p>All public methods are synchronized to ensure thread safety.
 *
 * @see #getInstance()
 * @see #init(PlaybackService, MediaEngine, QueueSyncManager, PlayerType)
 * @see #clear()
 */
public final class ServiceBridge {
	private static final LoggerUtils logger = LoggerUtils.from(ServiceBridge.class);
	private static final ServiceBridge instance = new ServiceBridge();
	
	@Nullable private PlaybackService service;
	@Nullable private MediaEngine engine;
	@Nullable private QueueSyncManager queueManager;
	@Nullable private PlayerType playerType;
	private boolean initialized;
	
	private ServiceBridge() {}
	
	/**
	 * Returns the singleton instance of the ServiceBridge. This method provides
	 * global access to the bridge for coordinating between the playback service,
	 * media engine, and queue manager throughout the application.
	 *
	 * @return The single {@link ServiceBridge} instance (never null).
	 */
	public static ServiceBridge getInstance() {
		return instance;
	}
	
	/**
	 * Initializes the ServiceBridge with the core playback components. This method
	 * should be called once during application startup after the playback service
	 * is created. The bridge becomes fully functional after initialization.
	 *
	 * <p>This method is thread-safe and stores references to:
	 * <ul>
	 * <li>The {@link PlaybackService} for background playback lifecycle.</li>
	 * <li>The {@link MediaEngine} for playback control operations.</li>
	 * <li>The {@link QueueSyncManager} for queue synchronization.</li>
	 * <li>The {@link PlayerType} indicating which player implementation is used.</li>
	 * </ul>
	 *
	 * @param service      The playback service instance managing background playback.
	 * @param engine       The media engine controlling playback.
	 * @param queueManager The queue manager for sync operations.
	 * @param playerType   The type of player (ExoPlayer or Media3).
	 */
	public synchronized void init(@NonNull PlaybackService service,
	                              @NonNull MediaEngine engine,
	                              @NonNull QueueSyncManager queueManager,
	                              @NonNull PlayerType playerType) {
		this.service = service;
		this.engine = engine;
		this.queueManager = queueManager;
		this.playerType = playerType;
		this.initialized = true;
		logger.debug("ServiceBridge initialized: " + playerType);
	}
	
	/**
	 * Clears all references held by the ServiceBridge and resets initialization state.
	 * This method should be called during application shutdown or when the playback
	 * service is being destroyed to prevent memory leaks.
	 *
	 * <p>This method is thread-safe. After clearing, all getter methods will return
	 * {@code null} and {@link #isInitialized()} will return {@code false}. The bridge
	 * can be re-initialized with another call to
	 * {@link #init(PlaybackService, MediaEngine, QueueSyncManager, PlayerType)}.
	 *
	 * @see #init(PlaybackService, MediaEngine, QueueSyncManager, PlayerType)
	 * @see #isInitialized()
	 */
	public synchronized void clear() {
		service = null;
		engine = null;
		queueManager = null;
		playerType = null;
		initialized = false;
		logger.debug("ServiceBridge cleared");
	}
	
	/**
	 * Checks whether the player engine has been properly initialized and is ready
	 * for playback operations. This method is thread-safe and can be called from
	 * any thread to verify the engine's readiness before invoking playback methods.
	 *
	 * @return {@code true} if the engine is initialized and non-null,
	 * {@code false} otherwise.
	 */
	public synchronized boolean isInitialized() {
		return initialized && engine != null;
	}
	
	/**
	 * Returns the underlying media engine instance responsible for playback control,
	 * track loading, and state management. The engine provides methods for play,
	 * pause, seek, volume control, and queue management.
	 *
	 * <p>This method is thread-safe. The returned engine is non-null only after
	 * successful initialization via .
	 *
	 * @return The {@link MediaEngine} instance, or {@code null} if not initialized.
	 * @see MediaEngine
	 */
	@Nullable
	public synchronized MediaEngine getEngine() {
		return engine;
	}
	
	/**
	 * Returns the playback service instance associated with this player engine.
	 * The service manages the background playback lifecycle, notification handling,
	 * and audio focus integration.
	 *
	 * <p>This method is thread-safe. The returned service is non-null only after
	 * successful initialization.
	 *
	 * @return The {@link PlaybackService} instance, or {@code null} if not initialized.
	 * @see PlaybackService
	 */
	@Nullable
	public synchronized PlaybackService getService() {
		return service;
	}
	
	/**
	 * Returns the current player type (e.g., ExoPlayer or Media3 Player).
	 * This method is thread-safe and can be called from any thread.
	 *
	 * @return The current {@link PlayerType}, or {@code null} if no player type
	 * has been set or the engine is not initialized.
	 * @see PlayerType
	 */
	@Nullable
	public synchronized PlayerType getPlayerType() {
		return playerType;
	}
	
	/**
	 * Registers a callback to receive playback events from the media engine.
	 * The callback will be notified of state changes, progress updates, metadata
	 * changes, video size changes, errors, track changes, and cue updates.
	 *
	 * <p>This method is thread-safe. If the engine is not initialized, the callback
	 * is not added and the method returns silently. Multiple callbacks can be added
	 * and will be notified in the order they were registered.
	 *
	 * @param callback The {@link EngineCallbacks} instance to register. Must not be null.
	 * @see #removeCallback(EngineCallbacks)
	 * @see MediaEngine#addCallback(EngineCallbacks)
	 */
	public synchronized void addCallback(@NonNull EngineCallbacks callback) {
		if (engine != null) {
			engine.addCallback(callback);
		}
	}
	
	/**
	 * Unregisters a previously registered callback, preventing it from receiving
	 * further playback events. This method should be called when a component
	 * (e.g., Activity or Fragment) is destroyed to avoid memory leaks.
	 *
	 * <p>This method is thread-safe. If the engine is not initialized or the
	 * callback was not previously registered, the call is silently ignored.
	 *
	 * @param callback The {@link EngineCallbacks} instance to unregister. Must not be null.
	 * @see #addCallback(EngineCallbacks)
	 * @see MediaEngine#removeCallback(EngineCallbacks)
	 */
	public synchronized void removeCallback(@NonNull EngineCallbacks callback) {
		if (engine != null) {
			engine.removeCallback(callback);
		}
	}
	
	/**
	 * Binds a play queue to the queue manager for synchronization with system media
	 * controls. After binding, the queue manager can publish the queue to the media
	 * session, enabling features such as "skip to next/previous" and displaying the
	 * queue in Android Auto and other system UI components.
	 *
	 * <p>This method is thread-safe and can be called from any thread. If the
	 * queue manager is not initialized, the call is silently ignored.
	 *
	 * @param queue The {@link PlayQueue} instance to bind. Must not be null.
	 * @see QueueSyncManager#bind(PlayQueue)
	 */
	public synchronized void bindPlayQueue(@NonNull PlayQueue queue) {
		if (queueManager != null) {
			queueManager.bind(queue);
		}
	}
	
	/**
	 * Unbinds the currently bound play queue from the queue manager. After unbinding,
	 * the media session will no longer have access to the queue, and system controls
	 * will revert to basic playback actions (play/pause/stop) without queue navigation.
	 *
	 * <p>This method is thread-safe and can be called from any thread. If the
	 * queue manager is not initialized or no queue is currently bound, the call
	 * is silently ignored.
	 *
	 * @see QueueSyncManager#unbind()
	 */
	public synchronized void unbindPlayQueue() {
		if (queueManager != null) {
			queueManager.unbind();
		}
	}
	
	/**
	 * Returns the currently bound play queue, if any. This method can be used to
	 * check whether a queue is currently synchronized with the media session or
	 * to access the queue for other operations.
	 *
	 * <p>This method is thread-safe. If the queue manager is not initialized or
	 * no queue is currently bound, {@code null} is returned.
	 *
	 * @return The currently bound {@link PlayQueue}, or {@code null} if none is bound.
	 * @see QueueSyncManager#getQueue()
	 */
	@Nullable
	public synchronized PlayQueue getPlayQueue() {
		return queueManager != null ? queueManager.getQueue() : null;
	}
}
