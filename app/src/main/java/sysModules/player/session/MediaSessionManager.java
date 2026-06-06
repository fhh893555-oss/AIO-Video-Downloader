package sysModules.player.session;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.support.v4.media.session.MediaSessionCompat;

import coreUtils.library.process.LoggerUtils;

public final class MediaSessionManager {
    private static final LoggerUtils logger = LoggerUtils.from(MediaSessionManager.class);

    private final android.content.Context context;
    @Nullable private MediaSessionCompat mediaSession;

    public MediaSessionManager(@NonNull android.content.Context context) {
        this.context = context.getApplicationContext();
    }

    public void connect() {
        release();
        mediaSession = new MediaSessionCompat(context, "TubeAIOPlayback");
        mediaSession.setActive(true);
        logger.debug("MediaSession connected");
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
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
            logger.debug("MediaSession released");
        }
    }
}
