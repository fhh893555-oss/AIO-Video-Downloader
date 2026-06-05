package sysModules.player.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.AudioFocusHelper;
import sysModules.player.engine.MediaEngine;
import sysModules.player.model.PlayerType;
import sysModules.player.notification.NotificationConstants;
import sysModules.player.notification.PlaybackNotification;
import sysModules.player.queue.PlayQueue;
import sysModules.player.queue.QueueSyncManager;
import sysModules.player.session.MediaSessionManager;

public class PlaybackService extends Service {
    private static final LoggerUtils logger = LoggerUtils.from(PlaybackService.class);

    private MediaEngine engine;
    private QueueSyncManager queueManager;
    private AudioFocusHelper audioFocus;
    private MediaSessionManager sessionManager;
    private PlaybackNotification notification;
    private PlayerType playerType;
    private boolean serviceStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        engine = new MediaEngine(this);
        queueManager = new QueueSyncManager(engine);
        audioFocus = new AudioFocusHelper(this, engine);
        sessionManager = new MediaSessionManager(this);
        notification = new PlaybackNotification(this);
        playerType = PlayerType.MAIN;

        engine.addCallback(notification);

        ServiceBridge.getInstance().init(this, engine, queueManager, playerType);
        logger.d("PlaybackService created");
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                handleAction(action, intent);
            }

            if (intent.hasExtra(NotificationConstants.EXTRA_PLAYER_TYPE)) {
                playerType = (PlayerType) intent.getSerializableExtra(
                        NotificationConstants.EXTRA_PLAYER_TYPE);
            }
        }
        return START_STICKY;
    }

    private void handleAction(@NonNull String action, @NonNull Intent intent) {
        switch (action) {
            case NotificationConstants.ACTION_PLAY:
                engine.play();
                ensureForeground();
                break;
            case NotificationConstants.ACTION_PAUSE:
                engine.pause();
                break;
            case NotificationConstants.ACTION_PLAY_PAUSE:
                engine.playPause();
                ensureForeground();
                break;
            case NotificationConstants.ACTION_NEXT:
                engine.playNext();
                break;
            case NotificationConstants.ACTION_PREVIOUS:
                engine.playPrevious();
                break;
            case NotificationConstants.ACTION_STOP:
                stopPlayback();
                break;
            case NotificationConstants.ACTION_CLOSE:
                closePlayer();
                break;
        }
    }

    public void loadAndPlay(@NonNull PlayQueue queue, long startPosition) {
        sessionManager.connect(engine.getExoPlayer());
        notification.setSessionToken(sessionManager.getSessionToken());

        ensureForeground();
        notification.start();

        audioFocus.requestFocus();
        queueManager.bind(queue);
    }

    private void ensureForeground() {
        if (!serviceStarted) {
            startForeground(NotificationConstants.NOTIFICATION_ID,
                    buildPlaceholderNotification());
            serviceStarted = true;
            logger.d("Service moved to foreground");
        }
    }

    @SuppressWarnings("deprecation")
    private android.app.Notification buildPlaceholderNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, NotificationConstants.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(getString(com.nextgen.R.string.player_loading))
                .setOngoing(true);
        return builder.build();
    }

    private void stopPlayback() {
        audioFocus.abandonFocus();
        engine.stop();
        if (serviceStarted) {
            stopForeground(true);
            serviceStarted = false;
        }
        notification.stop();
        sessionManager.release();
        logger.d("Playback stopped");
    }

    private void closePlayer() {
        stopPlayback();
        queueManager.unbind();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        logger.d("PlaybackService destroyed");
        ServiceBridge.getInstance().clear();
        engine.removeCallback(notification);
        notification.stop();
        sessionManager.release();
        audioFocus.abandonFocus();
        queueManager.unbind();
        engine.release();
        if (serviceStarted) {
            stopForeground(true);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public MediaEngine getEngine() {
        return engine;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(@NonNull PlayerType playerType) {
        this.playerType = playerType;
    }
}
