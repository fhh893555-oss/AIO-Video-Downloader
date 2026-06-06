package sysModules.player.session;

import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;

import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;

import org.schabi.newpipe.extractor.Image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.MediaEngine;
import sysModules.player.model.QueueEvent;
import sysModules.player.queue.PlayQueue;
import sysModules.player.queue.PlayQueueItem;
import sysModules.player.queue.QueueListener;

public final class PlayQueueNavigator implements QueueListener {
    private static final LoggerUtils logger = LoggerUtils.from(PlayQueueNavigator.class);
    private static final int MAX_QUEUE_SIZE = 10;

    private final MediaSessionCompat mediaSession;
    private final PlayQueue playQueue;
    private final MediaEngine engine;

    public PlayQueueNavigator(@NonNull MediaSessionCompat mediaSession,
                              @NonNull PlayQueue playQueue,
                              @NonNull MediaEngine engine) {
        this.mediaSession = mediaSession;
        this.playQueue = playQueue;
        this.engine = engine;
        playQueue.addListener(this);
        publishFloatingQueueWindow();
    }

    public void onSkipToQueueItem(int index) {
        playQueue.setIndex(index);
    }

    public void onSkipToNext() {
        engine.playNext();
    }

    public void onSkipToPrevious() {
        engine.playPrevious();
    }

    public void dispose() {
        playQueue.removeListener(this);
    }

    // ─── QueueListener ──────────────────────────────────────────────────────

    @Override
    public void onQueueChanged(@NonNull QueueEvent event) {
        publishFloatingQueueWindow();
    }

    // ─── Internal ───────────────────────────────────────────────────────────

    private void publishFloatingQueueWindow() {
        List<PlayQueueItem> streams = playQueue.getStreams();
        if (streams.isEmpty()) {
            mediaSession.setQueue(Collections.emptyList());
            return;
        }

        int currentIndex = playQueue.getIndex();
        int windowCount = streams.size();
        int queueSize = Math.min(MAX_QUEUE_SIZE, windowCount);

        int half = (queueSize - 1) / 2;
        int startIndex = Math.max(0,
                Math.min(currentIndex - half, windowCount - queueSize));

        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        for (int i = startIndex; i < startIndex + queueSize; i++) {
            PlayQueueItem item = streams.get(i);
            if (item != null) {
                queue.add(new MediaSessionCompat.QueueItem(
                        buildDescription(item, i), i));
            }
        }
        mediaSession.setQueue(queue);
    }

    private MediaDescriptionCompat buildDescription(@NonNull PlayQueueItem item,
                                                     int index) {
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(String.valueOf(index))
                .setTitle(item.getTitle())
                .setSubtitle(item.getUploader());

        Bundle extras = new Bundle();
        extras.putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.getTitle());
        extras.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.getUploader());
        extras.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                item.getDuration() * 1000L);
        extras.putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, index + 1L);
        extras.putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS,
                playQueue.size());
        builder.setExtras(extras);

        List<Image> thumbnails = item.getThumbnails();
        if (!thumbnails.isEmpty()) {
            try {
                String thumbUrl = thumbnails.get(0).getUrl();
                if (thumbUrl != null) {
                    builder.setIconUri(Uri.parse(thumbUrl));
                }
            } catch (Exception ignored) {
            }
        }

        return builder.build();
    }
}
