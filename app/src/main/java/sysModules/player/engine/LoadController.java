package sysModules.player.engine;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

public final class LoadController implements LoadControl {
    private final DefaultLoadControl defaultControl;
    private boolean preloadingDisabled;

    public LoadController() {
        this.defaultControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50_000,
                        100_000,
                        2_500,
                        5_000)
                .setTargetBufferBytes(-1)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
    }

    public void disablePreloadingOfCurrentTrack() {
        preloadingDisabled = true;
    }

    public void enablePreloading() {
        preloadingDisabled = false;
    }

    @Override
    public boolean shouldStartPlayback(@NonNull TrackGroupArray trackGroups,
                                        @NonNull TrackSelectionArray trackSelections,
                                        long playingTimeUs,
                                        boolean rebuffering) {
        return defaultControl.shouldStartPlayback(
                trackGroups, trackSelections, playingTimeUs, rebuffering);
    }

    @Override
    public boolean shouldContinueLoading(long playbackPositionUs,
                                          long bufferedDurationUs,
                                          float playbackSpeed) {
        if (preloadingDisabled && bufferedDurationUs > 30_000_000) {
            return false;
        }
        return defaultControl.shouldContinueLoading(
                playbackPositionUs, bufferedDurationUs, playbackSpeed);
    }

    @Override
    public long getTargetBufferBytes() {
        return defaultControl.getTargetBufferBytes();
    }

    @Override
    public void onStopped() {
        defaultControl.onStopped();
    }

    @Override
    public void onReleased() {
        defaultControl.onReleased();
    }
}
