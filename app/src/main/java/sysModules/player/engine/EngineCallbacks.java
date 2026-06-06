package sysModules.player.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.List;

import sysModules.player.model.PlaybackState;
import sysModules.player.queue.PlayQueueItem;

public interface EngineCallbacks {
    void onStateChanged(@NonNull PlaybackState.Phase phase);
    void onProgressChanged(long position, long duration, int bufferPercent);
    void onMetadataChanged(@NonNull PlayQueueItem item, @Nullable StreamInfo info);
    void onVideoSizeChanged(int width, int height);
    void onError(@NonNull Throwable error, boolean recoverable);
    void onTracksChanged(@NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections);
    void onCues(@NonNull List<Cue> cues);
    void onIsPlayingChanged(boolean isPlaying);
    void onPlayWhenReadyChanged(boolean playWhenReady, int reason);
}
