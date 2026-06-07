package sysModules.player.mediasource;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.CompositeMediaSource;
import com.google.android.exoplayer2.source.MediaPeriod;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.upstream.Allocator;

import sysModules.player.mediaitem.PlaceholderTag;
import sysModules.player.queue.PlayQueueItem;

/**
 * A placeholder {@link MediaSource} used in the playlist timeline for queue items
 * that have not yet been resolved. Always returns true for
 * {@link #shouldBeReplacedWith}, signaling that it should be replaced as soon as
 * the real source is available.
 */
public final class PlaceholderMediaSource extends CompositeMediaSource<Void>
        implements ManagedMediaSource {

    public static final PlaceholderMediaSource COPY = new PlaceholderMediaSource();
    private static final MediaItem MEDIA_ITEM =
            PlaceholderTag.EMPTY.withExtras(COPY).asMediaItem();

    private PlaceholderMediaSource() { }

    @NonNull
    @Override
    public MediaItem getMediaItem() {
        return MEDIA_ITEM;
    }

    @Override
    protected void onChildSourceInfoRefreshed(final Void id,
                                              final MediaSource mediaSource,
                                              final Timeline timeline) {
        /* No-op: do not update timeline or propagate errors */
    }

    @Override
    public MediaPeriod createPeriod(final MediaPeriodId id,
                                    final Allocator allocator,
                                    final long startPositionUs) {
        return null;
    }

    @Override
    public void releasePeriod(final MediaPeriod mediaPeriod) { }

    @Override
    public boolean shouldBeReplacedWith(@NonNull final PlayQueueItem newIdentity,
                                        final boolean isInterruptable) {
        return true;
    }

    @Override
    public boolean isStreamEqual(@NonNull final PlayQueueItem stream) {
        return false;
    }
}
