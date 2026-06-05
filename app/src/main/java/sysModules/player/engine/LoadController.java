package sysModules.player.engine;

import androidx.annotation.NonNull;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.TrackGroupArray;
import androidx.media3.exoplayer.trackselection.TrackSelectionArray;

public final class LoadController implements LoadControl {
    private final DefaultLoadControl defaultControl;
    private boolean preloadingDisabled;

    public LoadController() {
        this.defaultControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50_000,    // min buffer
                        100_000,   // max buffer
                        2_500,     // buffer for playback start
                        5_000)     // buffer for rebuffer
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
    public long getTargetBufferUs(boolean isPlaying, boolean rebuffering) {
        return defaultControl.getTargetBufferUs(isPlaying, rebuffering);
    }

    @Override
    public long getTargetBufferBytes() {
        return defaultControl.getTargetBufferBytes();
    }

    @Override
    public boolean shouldPreferLongBuffers(boolean isPlaying, boolean rebuffering) {
        return defaultControl.shouldPreferLongBuffers(isPlaying, rebuffering);
    }

    @Override
    public boolean shouldPreferLongRebuffer(boolean isPlaying, boolean rebuffering) {
        return defaultControl.shouldPreferLongRebuffer(isPlaying, rebuffering);
    }

    @Override
    public void onTracksChanged(@NonNull TrackGroupArray trackGroups,
                                 @NonNull TrackSelectionArray trackSelections) {
        defaultControl.onTracksChanged(trackGroups, trackSelections);
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
