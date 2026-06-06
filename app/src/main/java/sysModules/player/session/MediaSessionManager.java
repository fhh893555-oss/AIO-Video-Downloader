package sysModules.player.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;

import org.schabi.newpipe.extractor.Image;

import java.util.List;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.MediaEngine;
import sysModules.player.queue.PlayQueueItem;

public final class MediaSessionManager {
    private static final LoggerUtils logger = LoggerUtils.from(MediaSessionManager.class);

    private final Context context;
    private final MediaEngine engine;

    @Nullable private MediaSessionCompat mediaSession;
    @Nullable private MediaSessionConnector sessionConnector;
    @Nullable private PlayQueueNavigator queueNavigator;

    public MediaSessionManager(@NonNull Context context, @NonNull MediaEngine engine) {
        this.context = context.getApplicationContext();
        this.engine = engine;
    }

    // ─── Lifecycle ──────────────────────────────────────────────────────────

    public void connect() {
        release();
        mediaSession = new MediaSessionCompat(context, "TubeAIOPlayback");
        mediaSession.setActive(true);
        sessionConnector = new MediaSessionConnector(mediaSession);
        sessionConnector.setMediaMetadataProvider(this::buildMetadata);
        if (queueNavigator != null) {
            sessionConnector.setQueueNavigator(queueNavigator);
        }
        logger.debug("MediaSession connected");
    }

    public void setPlayer(@Nullable Player player) {
        if (sessionConnector != null) {
            sessionConnector.setPlayer(player);
        }
    }

    public void setQueueNavigator(@Nullable PlayQueueNavigator navigator) {
        this.queueNavigator = navigator;
        if (sessionConnector != null) {
            sessionConnector.setQueueNavigator(navigator);
        }
    }

    public void setCloseCallback(@Nullable Runnable callback) {
        if (sessionConnector == null) return;
        if (callback != null) {
            sessionConnector.setCustomActionProviders(
                    new SessionConnectorActionProvider(
                            "ACTION_CLOSE", "Close",
                            android.R.drawable.ic_menu_close_clear_cancel, callback));
        } else {
            sessionConnector.setCustomActionProviders();
        }
    }

    public void invalidateMetadata() {
        if (sessionConnector != null) {
            sessionConnector.invalidateMediaSessionMetadata();
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
        if (sessionConnector != null) {
            sessionConnector.setPlayer(null);
            sessionConnector = null;
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        queueNavigator = null;
        logger.debug("MediaSession released");
    }

    // ─── Metadata provider (called by connector on track change) ────────────

    @Nullable
    private MediaMetadataCompat buildMetadata(@NonNull Player player) {
        PlayQueueItem currentItem = engine.getCurrentItem();
        if (currentItem == null) return null;

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

        return builder.build();
    }


}
