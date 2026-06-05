package sysModules.player.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import androidx.media3.common.text.Cue;
import androidx.media3.common.Tracks;

import java.util.List;

import sysModules.player.model.PlaybackState;
import sysModules.player.queue.PlayQueueItem;

/**
 * Callback interface for receiving notifications from a media playback engine.
 * Implementers of this interface (typically activities or fragments) can observe
 * and react to various playback events including state changes, progress updates,
 * metadata loading, video dimension changes, errors, track changes, and cue updates.
 *
 * <p><strong>Callback types:</strong>
 * <ul>
 * <li>State changes – Playback phase transitions (buffering, ready, ended, etc.).</li>
 * <li>Progress updates – Current position, duration, and buffer percentage.</li>
 * <li>Metadata – When queue item or stream info changes.</li>
 * <li>Video size – Adapts UI when video dimensions are known.</li>
 * <li>Error handling – Differentiates between recoverable and fatal errors.</li>
 * <li>Tracks and cues – For subtitle/closed caption display.</li>
 * <li>Playback flags – Playing state and play-when-ready reason.</li>
 * </ul>
 *
 * @see MediaEngine
 * @see PlaybackState.Phase
 * @see PlayQueueItem
 * @see StreamInfo
 */
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
