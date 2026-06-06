package sysModules.player.session;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;

import org.schabi.newpipe.extractor.Image;

import java.util.List;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.MediaEngine;
import sysModules.player.queue.PlayQueueItem;

public final class MediaSessionManager implements Player.Listener {
    private static final LoggerUtils logger = LoggerUtils.from(MediaSessionManager.class);

    private final Context context;
    private final MediaEngine engine;

    @Nullable private MediaSessionCompat mediaSession;
    @Nullable private Player player;
    @Nullable private PlayQueueNavigator queueNavigator;
    @Nullable private Runnable closeCallback;

    public MediaSessionManager(@NonNull Context context, @NonNull MediaEngine engine) {
        this.context = context.getApplicationContext();
        this.engine = engine;
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public void connect() {
        release();
        mediaSession = new MediaSessionCompat(context, "TubeAIOPlayback");
        mediaSession.setActive(true);
        mediaSession.setCallback(new PlayerCallback());
        updatePlaybackState();
        updateMetadata();
        logger.debug("MediaSession connected");
    }

    public void setPlayer(@Nullable Player player) {
        if (this.player != null) {
            this.player.removeListener(this);
        }
        this.player = player;
        if (player != null) {
            player.addListener(this);
        }
        updatePlaybackState();
        updateMetadata();
    }

    public void setQueueNavigator(@Nullable PlayQueueNavigator navigator) {
        this.queueNavigator = navigator;
    }

    public void setCloseCallback(@Nullable Runnable callback) {
        this.closeCallback = callback;
    }

    public void handleMediaButtonIntent(@NonNull Intent intent) {
        if (mediaSession != null) {
            MediaSessionCompat.Callback callback = mediaSession.getCallback();
            if (callback != null) {
                callback.onMediaButtonEvent(intent);
            }
        }
    }

    @Nullable
    public MediaSessionCompat getMediaSession() {
        return mediaSession;
    }

    @Nullable
    public MediaSessionCompat.Token getSessionToken() {
        return mediaSession != null ? mediaSession.getSessionToken() : null;
    }

    public void release() {
        if (player != null) {
            player.removeListener(this);
            player = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.setCallback(null);
            mediaSession.release();
            mediaSession = null;
        }
        queueNavigator = null;
        logger.debug("MediaSession released");
    }

    // ─── Player.Listener ────────────────────────────────────────────────────

    @Override
    public void onPlaybackStateChanged(int playbackState) {
        updatePlaybackState();
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        updatePlaybackState();
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        updatePlaybackState();
    }

    @Override
    public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
        updateMetadata();
    }

    // ─── PlaybackState ──────────────────────────────────────────────────────

    private void updatePlaybackState() {
        if (mediaSession == null) return;

        int compatState = PlaybackStateCompat.STATE_NONE;
        long position = 0;
        float speed = 1f;

        if (player != null) {
            position = player.getContentPosition();
            PlaybackParameters params = player.getPlaybackParameters();
            speed = params != null ? params.speed : 1f;

            switch (player.getPlaybackState()) {
                case Player.STATE_IDLE:
                    compatState = PlaybackStateCompat.STATE_STOPPED;
                    break;
                case Player.STATE_BUFFERING:
                    compatState = player.getPlayWhenReady()
                            ? PlaybackStateCompat.STATE_BUFFERING
                            : PlaybackStateCompat.STATE_PAUSED;
                    break;
                case Player.STATE_READY:
                    compatState = player.getPlayWhenReady()
                            ? PlaybackStateCompat.STATE_PLAYING
                            : PlaybackStateCompat.STATE_PAUSED;
                    break;
                case Player.STATE_ENDED:
                    compatState = PlaybackStateCompat.STATE_STOPPED;
                    break;
            }
        }

        long actions = PlaybackStateCompat.ACTION_PLAY_PAUSE
                | PlaybackStateCompat.ACTION_SEEK_TO
                | PlaybackStateCompat.ACTION_STOP;

        if (queueNavigator != null) {
            actions |= PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM;
        }

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(compatState, position, speed);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PlaybackStateCompat.CustomAction closeAction =
                    new PlaybackStateCompat.CustomAction.Builder(
                            "ACTION_CLOSE",
                            context.getString(android.R.string.cancel),
                            android.R.drawable.ic_menu_close_clear_cancel)
                            .build();
            stateBuilder.addCustomAction(closeAction);
        }

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    // ─── MediaMetadata ─────────────────────────────────────────────────────

    private void updateMetadata() {
        if (mediaSession == null) return;

        PlayQueueItem currentItem = engine.getCurrentItem();
        if (currentItem == null) {
            mediaSession.setMetadata(null);
            return;
        }

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentItem.getTitle())
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentItem.getUploader())
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                        currentItem.getDuration() * 1000L);

        List<Image> thumbnails = currentItem.getThumbnails();
        if (!thumbnails.isEmpty()) {
            String artUrl = thumbnails.get(0).getUrl();
            if (artUrl != null) {
                builder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUrl);
                builder.putString(MediaMetadataCompat.METADATA_KEY_ART_URI, artUrl);
            }
        }

        mediaSession.setMetadata(builder.build());
    }

    // ─── Callback: dispatches transport controls to engine ──────────────────

    private class PlayerCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            engine.play();
        }

        @Override
        public void onPause() {
            engine.pause();
        }

        @Override
        public void onSkipToNext() {
            engine.playNext();
        }

        @Override
        public void onSkipToPrevious() {
            engine.playPrevious();
        }

        @Override
        public void onSeekTo(long pos) {
            engine.seekTo(pos);
        }

        @Override
        public void onStop() {
            engine.stop();
        }

        @Override
        public void onSkipToQueueItem(long id) {
            if (queueNavigator != null) {
                queueNavigator.onSkipToQueueItem((int) id);
            }
        }

        @Override
        public void onCustomAction(@NonNull String action, @Nullable Bundle extras) {
            if ("ACTION_CLOSE".equals(action)) {
                if (closeCallback != null) {
                    closeCallback.run();
                } else {
                    engine.stop();
                }
            }
        }
    }
}
