package sysModules.player.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.EngineCallbacks;
import sysModules.player.engine.MediaEngine;
import sysModules.player.model.PlayerType;
import sysModules.player.queue.PlayQueue;
import sysModules.player.queue.QueueSyncManager;

public final class ServiceBridge {
    private static final LoggerUtils logger = LoggerUtils.from(ServiceBridge.class);
    private static final ServiceBridge instance = new ServiceBridge();

    @Nullable private PlaybackService service;
    @Nullable private MediaEngine engine;
    @Nullable private QueueSyncManager queueManager;
    @Nullable private PlayerType playerType;
    private boolean initialized;

    private ServiceBridge() {}

    public static ServiceBridge getInstance() {
        return instance;
    }

    public synchronized void init(@NonNull PlaybackService service,
                                   @NonNull MediaEngine engine,
                                   @NonNull QueueSyncManager queueManager,
                                   @NonNull PlayerType playerType) {
        this.service = service;
        this.engine = engine;
        this.queueManager = queueManager;
        this.playerType = playerType;
        this.initialized = true;
        logger.d("ServiceBridge initialized: " + playerType);
    }

    public synchronized void clear() {
        service = null;
        engine = null;
        queueManager = null;
        playerType = null;
        initialized = false;
        logger.d("ServiceBridge cleared");
    }

    public synchronized boolean isInitialized() {
        return initialized && engine != null;
    }

    @Nullable
    public synchronized MediaEngine getEngine() {
        return engine;
    }

    @Nullable
    public synchronized PlaybackService getService() {
        return service;
    }

    @Nullable
    public synchronized PlayerType getPlayerType() {
        return playerType;
    }

    public synchronized void addCallback(@NonNull EngineCallbacks callback) {
        if (engine != null) {
            engine.addCallback(callback);
        }
    }

    public synchronized void removeCallback(@NonNull EngineCallbacks callback) {
        if (engine != null) {
            engine.removeCallback(callback);
        }
    }

    public synchronized void bindPlayQueue(@NonNull PlayQueue queue) {
        if (queueManager != null) {
            queueManager.bind(queue);
        }
    }

    public synchronized void unbindPlayQueue() {
        if (queueManager != null) {
            queueManager.unbind();
        }
    }

    @Nullable
    public synchronized PlayQueue getPlayQueue() {
        return queueManager != null ? queueManager.getQueue() : null;
    }
}
