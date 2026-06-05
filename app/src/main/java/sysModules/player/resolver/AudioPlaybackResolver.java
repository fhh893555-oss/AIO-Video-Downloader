package sysModules.player.resolver;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;

import java.util.List;

import coreUtils.library.process.LoggerUtils;

public final class AudioPlaybackResolver {
    private static final LoggerUtils logger = LoggerUtils.from(AudioPlaybackResolver.class);

    private final DataSource.Factory dataSourceFactory;

    public AudioPlaybackResolver(@NonNull DataSource.Factory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    @Nullable
    public MediaSource resolve(@NonNull Context context, @NonNull StreamInfo info) {
        List<AudioStream> audioStreams = info.getAudioStreams();
        if (audioStreams.isEmpty()) {
            logger.w("No audio streams available for " + info.getName());
            return null;
        }

        AudioStream selected = selectBestAudioStream(audioStreams);
        if (selected == null) selected = audioStreams.get(0);

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(selected.getUrl())
                .setMediaId(info.getUrl())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(info.getName())
                        .setArtist(info.getUploaderName())
                        .build())
                .build();

        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
    }

    @Nullable
    private AudioStream selectBestAudioStream(@NonNull List<AudioStream> streams) {
        AudioStream best = null;
        for (AudioStream as : streams) {
            if (best == null || as.getAverageBitrate() > best.getAverageBitrate()) {
                best = as;
            }
        }
        return best;
    }
}
