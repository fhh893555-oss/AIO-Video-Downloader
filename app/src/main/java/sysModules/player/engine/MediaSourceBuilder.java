package sysModules.player.engine;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.List;

import coreUtils.library.process.LoggerUtils;

@OptIn(markerClass = UnstableApi.class)
public final class MediaSourceBuilder {
    private static final LoggerUtils logger = LoggerUtils.from(MediaSourceBuilder.class);

    private MediaSourceBuilder() {}
	
	@NonNull
    public static MediaSource fromUri(@NonNull Uri uri,
                                       @NonNull DataSource.Factory dataSourceFactory) {
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri));
    }

    @Nullable
    public static MediaSource fromStreamInfo(@NonNull Context context,
                                              @NonNull StreamInfo info,
                                              @NonNull DataSource.Factory dataSourceFactory,
                                              boolean preferAudioOnly) {
        if (preferAudioOnly) {
            return buildAudioSource(info, dataSourceFactory);
        }

        String dashUrl = info.getDashUrl();
        String hlsUrl = info.getHlsUrl();

        if (dashUrl != null && info.getDashMimeType() != null) {
            return new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(dashUrl));
        }

        if (hlsUrl != null && info.getHlsMimeType() != null) {
            return new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(hlsUrl));
        }

        List<VideoStream> videoStreams = info.getVideoStreams();
        List<VideoStream> videoOnlyStreams = info.getVideoOnlyStreams();

        if (!videoStreams.isEmpty()) {
            VideoStream selected = videoStreams.get(0);
            MediaItem mediaItem = buildMediaItem(info, selected.getUrl());
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }

        if (!videoOnlyStreams.isEmpty()) {
            VideoStream selected = videoOnlyStreams.get(0);
            MediaItem mediaItem = buildMediaItem(info, selected.getUrl());
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }

        return buildAudioSource(info, dataSourceFactory);
    }

    @Nullable
    private static MediaSource buildAudioSource(@NonNull StreamInfo info,
                                                 @NonNull DataSource.Factory dataSourceFactory) {
        List<AudioStream> audioStreams = info.getAudioStreams();
        if (audioStreams.isEmpty()) {
            logger.w("No audio streams available");
            return null;
        }
        AudioStream selected = audioStreams.get(0);
        for (AudioStream as : audioStreams) {
            if (as.getAverageBitrate() > selected.getAverageBitrate()) {
                selected = as;
            }
        }
        MediaItem mediaItem = buildMediaItem(info, selected.getUrl());
        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
    }

    private static MediaItem buildMediaItem(@NonNull StreamInfo info, @NonNull String url) {
        return new MediaItem.Builder()
                .setUri(url)
                .setMediaId(info.getUrl())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(info.getName())
                        .setArtist(info.getUploaderName())
                        .setArtworkUri(
                                !info.getThumbnails().isEmpty()
                                        ? android.net.Uri.parse(info.getThumbnails().get(0).getUrl())
                                        : null)
                        .build())
                .build();
    }
}
