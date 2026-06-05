package sysModules.player.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import androidx.media3.common.text.Cue;
import androidx.media3.common.Tracks;

import java.util.List;

import sysModules.player.model.PlaybackState;
import sysModules.player.queue.PlayQueueItem;

public interface EngineCallbacks {
    void onStateChanged(@NonNull PlaybackState.Phase phase);
    void onProgressChanged(long position, long duration, int bufferPercent);
    void onMetadataChanged(@NonNull PlayQueueItem item, @Nullable StreamInfo info);
    void onVideoSizeChanged(int width, int height);
    void onError(@NonNull Throwable error, boolean recoverable);
    void onTracksChanged(@NonNull Tracks tracks);
    void onCues(@NonNull List<Cue> cues);
    void onIsPlayingChanged(boolean isPlaying);
    void onPlayWhenReadyChanged(boolean playWhenReady, int reason);
}
