package sysModules.player.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.Tracks;
import com.google.android.exoplayer2.text.CueGroup;

import com.nextgen.R;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.EngineCallbacks;
import sysModules.player.model.PlaybackState;
import sysModules.player.queue.PlayQueueItem;

public final class PlaybackNotification implements EngineCallbacks {
    private static final LoggerUtils logger = LoggerUtils.from(PlaybackNotification.class);

    private final Context context;
    private final NotificationManager notificationManager;
    @Nullable private MediaSessionCompat.Token sessionToken;
    private boolean started;
    private boolean isPlaying;

    @Nullable private String currentTitle;
    @Nullable private String currentUploader;

    public PlaybackNotification(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.notificationManager = (NotificationManager)
                this.context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    public void setSessionToken(@Nullable MediaSessionCompat.Token token) {
        this.sessionToken = token;
    }

    public void start() {
        started = true;
        showNotification(false);
    }

    public void stop() {
        started = false;
        notificationManager.cancel(NotificationConstants.NOTIFICATION_ID);
    }

    public void post() {
        if (started) {
            showNotification(isPlaying);
        }
    }

    private void showNotification(boolean playing) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, NotificationConstants.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(currentTitle != null ? currentTitle :
                        context.getString(R.string.player_no_title))
                .setContentText(currentUploader)
                .setOngoing(true)
                .setShowWhen(false);

        if (sessionToken != null) {
            builder.setStyle(new MediaStyle()
                    .setMediaSession(sessionToken)
                    .setShowActionsInCompactView(0, 1, 2));
        }

        builder.addAction(buildPreviousAction());
        if (playing) {
            builder.addAction(buildPauseAction());
        } else {
            builder.addAction(buildPlayAction());
        }
        builder.addAction(buildNextAction());
        builder.addAction(buildCloseAction());

        notificationManager.notify(NotificationConstants.NOTIFICATION_ID, builder.build());
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NotificationConstants.CHANNEL_ID,
                    context.getString(NotificationConstants.CHANNEL_NAME_RES),
                    NotificationConstants.CHANNEL_IMPORTANCE);
            channel.setDescription(
                    context.getString(NotificationConstants.CHANNEL_DESC_RES));
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private NotificationCompat.Action buildPlayAction() {
        return new NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                context.getString(R.string.player_notification_play),
                NotificationActions.playPause(context));
    }

    private NotificationCompat.Action buildPauseAction() {
        return new NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                context.getString(R.string.player_notification_pause),
                NotificationActions.playPause(context));
    }

    private NotificationCompat.Action buildNextAction() {
        return new NotificationCompat.Action(
                android.R.drawable.ic_media_next,
                context.getString(R.string.player_notification_next),
                NotificationActions.next(context));
    }

    private NotificationCompat.Action buildPreviousAction() {
        return new NotificationCompat.Action(
                android.R.drawable.ic_media_previous,
                context.getString(R.string.player_notification_previous),
                NotificationActions.previous(context));
    }

    private NotificationCompat.Action buildCloseAction() {
        return new NotificationCompat.Action(
                android.R.drawable.ic_menu_close_clear_cancel,
                context.getString(R.string.player_notification_close),
                NotificationActions.close(context));
    }

    @Override
    public void onStateChanged(@NonNull PlaybackState.Phase phase) {
        isPlaying = phase == PlaybackState.Phase.PLAYING
                || phase == PlaybackState.Phase.BUFFERING;
        if (started) {
            showNotification(isPlaying);
        }
    }

    @Override
    public void onProgressChanged(long position, long duration, int bufferPercent) {
    }

    @Override
    public void onMetadataChanged(@NonNull PlayQueueItem item, @Nullable StreamInfo info) {
        currentTitle = item.getTitle();
        currentUploader = item.getUploader();
        if (started) {
            showNotification(isPlaying);
        }
    }

    @Override
    public void onVideoSizeChanged(int width, int height) {
    }

    @Override
    public void onError(@NonNull Throwable error, boolean recoverable) {
        logger.error("Playback error: " + error.getMessage());
    }

    @Override
    public void onTracksChanged(@NonNull Tracks tracks) {
    }

    @Override
    public void onCues(@NonNull CueGroup cueGroup) {
    }

    @Override
    public void onIsPlayingChanged(boolean playing) {
        isPlaying = playing;
        if (started) {
            showNotification(isPlaying);
        }
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
    }
}
