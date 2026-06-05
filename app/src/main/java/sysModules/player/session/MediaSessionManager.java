package sysModules.player.session;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.session.MediaSession;

import coreUtils.library.process.LoggerUtils;

public final class MediaSessionManager {
    private static final LoggerUtils logger = LoggerUtils.from(MediaSessionManager.class);

    private final Context context;
    @Nullable private MediaSession mediaSession;

    public MediaSessionManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public void connect(@NonNull androidx.media3.common.Player player) {
        release();
        mediaSession = new MediaSession.Builder(context, player)
                .setId("TubeAIOPlayback")
                .build();
        logger.d("MediaSession connected");
    }

    @Nullable
    public MediaSession getMediaSession() {
        return mediaSession;
    }

    @Nullable
    public androidx.media.session.MediaSessionCompat.Token getSessionToken() {
        return mediaSession != null ? mediaSession.getSessionCompatToken() : null;
    }

    public void release() {
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
            logger.d("MediaSession released");
        }
    }
}
