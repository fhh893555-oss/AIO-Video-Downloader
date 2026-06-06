package sysModules.player.engine;

import android.content.Context;
import android.content.Intent;
import android.media.audiofx.AudioEffect;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.text.Cue;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.model.PlaybackState;
import sysModules.player.model.RepeatMode;
import sysModules.player.queue.PlayQueueItem;
import sysModules.player.resolver.AudioPlaybackResolver;
import sysModules.player.resolver.PlayerDataSource;
import sysModules.player.resolver.VideoPlaybackResolver;

public final class MediaEngine implements Player.EventListener, AnalyticsListener {
    private static final LoggerUtils logger = LoggerUtils.from(MediaEngine.class);

    public static final int PROGRESS_LOOP_INTERVAL_MILLIS = 1000;
    private static final float MUTED_VOLUME = 0f;
    private static final float NORMAL_VOLUME = 1f;

    private final Context context;
    private final Handler mainHandler;

    private SimpleExoPlayer exoPlayer;
    private DefaultTrackSelector trackSelector;
    private DefaultRenderersFactory renderFactory;
    private LoadController loadController;
    private PlayerDataSource dataSource;

    private final VideoPlaybackResolver videoResolver;
    private final AudioPlaybackResolver audioResolver;

    private PlaybackState.Phase currentPhase = PlaybackState.Phase.PREFLIGHT;
    private RepeatMode repeatMode = RepeatMode.NONE;
    private boolean muted;
    private boolean audioOnly;

    @Nullable private PlayQueueItem currentItem;
    @Nullable private StreamInfo currentInfo;

    private final List<EngineCallbacks> callbacks = new CopyOnWriteArrayList<>();

    private boolean progressLoopRunning;
    private int audioSessionId;

    public MediaEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.dataSource = new PlayerDataSource(this.context);
        this.loadController = new LoadController();

        this.trackSelector = new DefaultTrackSelector(this.context);
        this.renderFactory = new DefaultRenderersFactory(this.context)
                .setEnableDecoderFallback(true);

        this.videoResolver = new VideoPlaybackResolver(dataSource);
        this.audioResolver = new AudioPlaybackResolver(dataSource);
    }

    // ─── Callbacks ───────────────────────────────────────────────────────────

    public void addCallback(@NonNull EngineCallbacks cb) {
        if (!callbacks.contains(cb)) callbacks.add(cb);
    }

    public void removeCallback(@NonNull EngineCallbacks cb) {
        callbacks.remove(cb);
    }

    // ─── Playback lifecycle ─────────────────────────────────────────────────

    public void load(@NonNull StreamInfo info, long startPosition) {
        this.currentInfo = info;

        MediaSource mediaSource =
                MediaSourceBuilder.fromStreamInfo(context, info, dataSource, audioOnly);

        if (mediaSource == null) {
            notifyError(new RuntimeException("No playable streams found for " + info.getName()), false);
            return;
        }

        ensurePlayerReady();

        if (currentItem != null) {
            exoPlayer.setMediaSource(mediaSource, startPosition > 0 ? startPosition : C.TIME_UNSET);
        } else {
            exoPlayer.setMediaSource(mediaSource);
            if (startPosition > 0) exoPlayer.seekTo(startPosition);
        }

        exoPlayer.prepare();
        setState(PlaybackState.Phase.BUFFERING);
        notifyMetadataChanged();
    }

    public void load(@NonNull MediaItem mediaItem, long startPosition) {
        this.currentInfo = null;
        ensurePlayerReady();

        if (currentItem != null && startPosition > 0) {
            exoPlayer.setMediaItem(mediaItem, startPosition);
        } else {
            exoPlayer.setMediaItem(mediaItem);
            if (startPosition > 0) exoPlayer.seekTo(startPosition);
        }

        exoPlayer.prepare();
        setState(PlaybackState.Phase.BUFFERING);
        notifyMetadataChanged();
    }

    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
        setState(PlaybackState.Phase.PREFLIGHT);
    }

    public void release() {
        stopProgressLoop();
        if (exoPlayer != null) {
            notifyAudioSessionUpdate(false);
            exoPlayer.removeListener(this);
            exoPlayer.removeAnalyticsListener(this);
            exoPlayer.release();
            exoPlayer = null;
        }
        trackSelector = null;
        renderFactory = null;
        loadController = null;
        if (dataSource != null) {
            dataSource.release();
            dataSource = null;
        }
        currentItem = null;
        currentInfo = null;
    }

    // ─── Playback controls ─────────────────────────────────────────────────

    public void play() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }
    }

    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
            if (currentPhase == PlaybackState.Phase.PLAYING) {
                setState(PlaybackState.Phase.PAUSED);
            }
        }
    }

    public void playPause() {
        if (isPlaying()) pause(); else play();
    }

    public void seekTo(long positionMs) {
        if (exoPlayer != null) {
            if (currentPhase == PlaybackState.Phase.PAUSED) {
                setState(PlaybackState.Phase.PAUSED_SEEK);
            }
            exoPlayer.seekTo(positionMs);
        }
    }

    public void playPrevious() {
        setState(PlaybackState.Phase.BUFFERING);
    }

    public void playNext() {
        setState(PlaybackState.Phase.BUFFERING);
    }

    public void fastForward() {
        if (exoPlayer != null) {
            seekTo(exoPlayer.getCurrentPosition() + 10_000);
        }
    }

    public void fastRewind() {
        if (exoPlayer != null) {
            seekTo(Math.max(0, exoPlayer.getCurrentPosition() - 10_000));
        }
    }

    // ─── Parameters ─────────────────────────────────────────────────────────

    public void setPlaybackSpeed(float speed) {
        if (exoPlayer != null) {
            PlaybackParameters params = exoPlayer.getPlaybackParameters();
            exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, params.pitch));
            notifyProgress();
        }
    }

    public void setPlaybackPitch(float pitch) {
        if (exoPlayer != null) {
            PlaybackParameters params = exoPlayer.getPlaybackParameters();
            exoPlayer.setPlaybackParameters(new PlaybackParameters(params.speed, pitch));
            notifyProgress();
        }
    }

    public void setPlaybackParameters(float speed, float pitch) {
        if (exoPlayer != null) {
            exoPlayer.setPlaybackParameters(new PlaybackParameters(speed, pitch));
            notifyProgress();
        }
    }

    public float getPlaybackSpeed() {
        if (exoPlayer != null) {
            return exoPlayer.getPlaybackParameters().speed;
        }
        return 1.0f;
    }

    public float getPlaybackPitch() {
        if (exoPlayer != null) {
            return exoPlayer.getPlaybackParameters().pitch;
        }
        return 1.0f;
    }

    public boolean isMuted() { return muted; }

    public void setMuted(boolean mute) {
        this.muted = mute;
        setVolume(mute ? MUTED_VOLUME : NORMAL_VOLUME);
        notifyProgress();
    }

    public void toggleMute() {
        setMuted(!muted);
    }

    public void setVolume(float volume) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(volume);
        }
    }

    // ─── Queue integration ──────────────────────────────────────────────────

    public void cycleRepeatMode() {
        repeatMode = repeatMode.cycleNext();
        if (exoPlayer != null) {
            switch (repeatMode) {
                case NONE: exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); break;
                case ONE:  exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE); break;
                case ALL:  exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL); break;
            }
        }
        notifyProgress();
    }

    public void setRepeatMode(@NonNull RepeatMode mode) {
        this.repeatMode = mode;
        if (exoPlayer != null) {
            switch (mode) {
                case NONE: exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); break;
                case ONE:  exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE); break;
                case ALL:  exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL); break;
            }
        }
    }

    public RepeatMode getRepeatMode() { return repeatMode; }

    // ─── Stream selection ───────────────────────────────────────────────────

    public void setVideoQuality(int streamIndex) {
        if (trackSelector != null) {
            MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
            if (info != null) {
                for (int renderer = 0; renderer < info.getRendererCount(); renderer++) {
                    if (info.getRendererType(renderer) == C.TRACK_TYPE_VIDEO) {
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setSelectionOverride(renderer, info.getTrackGroups(renderer),
                                                new DefaultTrackSelector.SelectionOverride(
                                                        streamIndex, 0)));
                        break;
                    }
                }
            }
        }
    }

    public void setAudioTrack(int streamIndex) {
        if (trackSelector != null) {
            MappingTrackSelector.MappedTrackInfo info = trackSelector.getCurrentMappedTrackInfo();
            if (info != null) {
                for (int renderer = 0; renderer < info.getRendererCount(); renderer++) {
                    if (info.getRendererType(renderer) == C.TRACK_TYPE_AUDIO) {
                        trackSelector.setParameters(
                                trackSelector.buildUponParameters()
                                        .setSelectionOverride(renderer, info.getTrackGroups(renderer),
                                                new DefaultTrackSelector.SelectionOverride(
                                                        streamIndex, 0)));
                        break;
                    }
                }
            }
        }
    }

    public void setCaptionLanguage(@Nullable String language) {
        if (trackSelector != null) {
            DefaultTrackSelector.Parameters.Builder params =
                    trackSelector.buildUponParameters();
            if (language != null) {
                params.setPreferredTextLanguage(language);
                params.setRendererDisabled(C.TRACK_TYPE_TEXT, false);
            } else {
                params.setRendererDisabled(C.TRACK_TYPE_TEXT, true);
            }
            trackSelector.setParameters(params.build());
        }
    }

    public void disableCaptions() {
        if (trackSelector != null) {
            trackSelector.setParameters(
                    trackSelector.buildUponParameters()
                            .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
                            .build());
        }
    }

    // ─── Video ──────────────────────────────────────────────────────────────

    public void setResizeMode(@AspectRatioFrameLayout.ResizeMode int mode) {
        notifyProgress();
    }

    public boolean isPlaying() {
        return exoPlayer != null
                && exoPlayer.getPlaybackState() == Player.STATE_READY
                && exoPlayer.getPlayWhenReady();
    }

    public boolean isPlayingOrBuffering() {
        return currentPhase == PlaybackState.Phase.PLAYING
                || currentPhase == PlaybackState.Phase.BUFFERING;
    }

    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return exoPlayer != null ? exoPlayer.getDuration() : 0;
    }

    public int getBufferedPercentage() {
        return exoPlayer != null ? exoPlayer.getBufferedPercentage() : 0;
    }

    // ─── Internal ───────────────────────────────────────────────────────────

    private void ensurePlayerReady() {
        if (exoPlayer == null) {
            exoPlayer = new SimpleExoPlayer.Builder(context, renderFactory)
                    .setTrackSelector(trackSelector)
                    .setLoadControl(loadController)
                    .build();
            exoPlayer.addListener(this);
            exoPlayer.addAnalyticsListener(this);
            exoPlayer.setPlayWhenReady(true);
            exoPlayer.setHandleAudioBecomingNoisy(true);
        }
    }

    public SimpleExoPlayer getExoPlayer() {
        ensurePlayerReady();
        return exoPlayer;
    }

    private void setState(PlaybackState.Phase newPhase) {
        if (currentPhase == newPhase) return;
        if (!currentPhase.canTransitionTo(newPhase)) {
            logger.w("Invalid state transition: " + currentPhase + " → " + newPhase);
        }
        currentPhase = newPhase;
        for (EngineCallbacks cb : callbacks) {
            cb.onStateChanged(currentPhase);
        }
        if (newPhase == PlaybackState.Phase.PLAYING
                || newPhase == PlaybackState.Phase.BUFFERING) {
            startProgressLoop();
        } else {
            stopProgressLoop();
        }
    }

    public PlaybackState.Phase getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentItem(@Nullable PlayQueueItem item) {
        this.currentItem = item;
    }

    @Nullable
    public PlayQueueItem getCurrentItem() {
        return currentItem;
    }

    @Nullable
    public StreamInfo getCurrentInfo() {
        return currentInfo;
    }

    private void notifyMetadataChanged() {
        if (currentItem == null) return;
        for (EngineCallbacks cb : callbacks) {
            cb.onMetadataChanged(currentItem, currentInfo);
        }
    }

    private void notifyProgress() {
        long pos = getCurrentPosition();
        long dur = getDuration();
        int buf = getBufferedPercentage();
        for (EngineCallbacks cb : callbacks) {
            cb.onProgressChanged(pos, dur, buf);
        }
    }

    private void notifyError(Throwable error, boolean recoverable) {
        for (EngineCallbacks cb : callbacks) {
            cb.onError(error, recoverable);
        }
    }

    public void onPlayerError(@NonNull Throwable error) {
        notifyError(error, true);
    }

    private void notifyAudioSessionUpdate(boolean active) {
        if (audioSessionId == 0) return;
        Intent intent = new Intent(active
                ? AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION
                : AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audioSessionId);
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.getPackageName());
        context.sendBroadcast(intent);
    }

    // ─── Progress loop ─────────────────────────────────────────────────────

    private void startProgressLoop() {
        if (progressLoopRunning) return;
        progressLoopRunning = true;
        mainHandler.post(progressRunnable);
    }

    private void stopProgressLoop() {
        progressLoopRunning = false;
        mainHandler.removeCallbacks(progressRunnable);
    }

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (!progressLoopRunning || exoPlayer == null) return;
            notifyProgress();
            mainHandler.postDelayed(this, PROGRESS_LOOP_INTERVAL_MILLIS);
        }
    };

    // ─── Player.EventListener ──────────────────────────────────────────────

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case Player.STATE_IDLE:
                setState(PlaybackState.Phase.PREFLIGHT);
                break;
            case Player.STATE_BUFFERING:
                setState(PlaybackState.Phase.BUFFERING);
                break;
            case Player.STATE_READY:
                if (playWhenReady) {
                    setState(PlaybackState.Phase.PLAYING);
                } else {
                    setState(PlaybackState.Phase.PAUSED);
                }
                break;
            case Player.STATE_ENDED:
                setState(PlaybackState.Phase.COMPLETED);
                break;
        }
    }

    @Override
    public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
        for (EngineCallbacks cb : callbacks) {
            cb.onPlayWhenReadyChanged(playWhenReady, reason);
        }
    }

    @Override
    public void onIsPlayingChanged(boolean isPlaying) {
        for (EngineCallbacks cb : callbacks) {
            cb.onIsPlayingChanged(isPlaying);
        }
    }

    @Override
    public void onPlayerError(@NonNull ExoPlaybackException error) {
        logger.e("Player error: " + error.getMessage());
        notifyError(error, true);
        setState(PlaybackState.Phase.BLOCKED);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
        for (EngineCallbacks cb : callbacks) {
            cb.onVideoSizeChanged(width, height);
        }
    }

    @Override
    public void onTracksChanged(@NonNull TrackGroupArray trackGroups, @NonNull TrackSelectionArray trackSelections) {
        for (EngineCallbacks cb : callbacks) {
            cb.onTracksChanged(trackGroups, trackSelections);
        }
    }

    @Override
    public void onCues(@NonNull List<Cue> cues) {
        for (EngineCallbacks cb : callbacks) {
            cb.onCues(cues);
        }
    }

    @Override
    public void onPlaybackParametersChanged(@NonNull PlaybackParameters params) {
        notifyProgress();
    }

    // ─── AnalyticsListener (audio session for equalizers) ──────────────────

    @Override
    public void onAudioSessionIdChanged(@NonNull EventTime eventTime, int audioSessionId) {
        this.audioSessionId = audioSessionId;
        notifyAudioSessionUpdate(true);
    }

    public Context getContext() {
        return context;
    }

    public PlayerDataSource getDataSource() {
        return dataSource;
    }
}
