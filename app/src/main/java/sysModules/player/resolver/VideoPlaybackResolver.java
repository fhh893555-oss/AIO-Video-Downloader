package sysModules.player.resolver;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
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

public final class VideoPlaybackResolver {
    private static final LoggerUtils logger = LoggerUtils.from(VideoPlaybackResolver.class);

    private final DataSource.Factory dataSourceFactory;
    private String playbackQuality;

    public VideoPlaybackResolver(@NonNull DataSource.Factory dataSourceFactory) {
        this.dataSourceFactory = dataSourceFactory;
    }

    public void setPlaybackQuality(@Nullable String quality) {
        this.playbackQuality = quality;
    }

    @Nullable
    public MediaSource resolve(@NonNull Context context, @NonNull StreamInfo info) {
        MediaItem mediaItem = buildMediaItem(info);
        if (mediaItem == null) return null;

        StreamType type = info.getStreamType();
        String contentId = info.getUrl();

        switch (type) {
            case DASH:
            case LIVE_STREAM:
                if (info.getDashMimeType() != null && info.getDashUrl() != null) {
                    return new DashMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(info.getDashUrl()));
                }
                break;
            case HLS_STREAM:
                if (info.getHlsMimeType() != null && info.getHlsUrl() != null) {
                    return new HlsMediaSource.Factory(dataSourceFactory)
                            .createMediaSource(MediaItem.fromUri(info.getHlsUrl()));
                }
                break;
        }

        return new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem);
    }

    @Nullable
    private MediaItem buildMediaItem(@NonNull StreamInfo info) {
        List<VideoStream> videoStreams = info.getVideoStreams();
        List<VideoStream> videoOnlyStreams = info.getVideoOnlyStreams();

        if (videoStreams.isEmpty() && videoOnlyStreams.isEmpty()) {
            logger.w("No video streams available for " + info.getName());
            return null;
        }

        VideoStream selected = selectBestVideoStream(videoStreams, videoOnlyStreams);
        if (selected == null && !videoStreams.isEmpty()) {
            selected = videoStreams.get(0);
        }
        if (selected == null && !videoOnlyStreams.isEmpty()) {
            selected = videoOnlyStreams.get(0);
        }
        if (selected == null) return null;

        MediaItem.Builder builder = new MediaItem.Builder()
                .setUri(selected.getUrl())
                .setMediaId(info.getUrl())
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(info.getName())
                        .setArtist(info.getUploaderName())
                        .build());

        if (selected.getFormat() != null) {
            String mimeType = selected.getFormat().mimeType;
            if (mimeType != null) {
                builder.setMimeType(mimeType);
            }
        }

        return builder.build();
    }

    @Nullable
    private VideoStream selectBestVideoStream(@NonNull List<VideoStream> videos,
                                               @NonNull List<VideoStream> videoOnly) {
        if (playbackQuality != null) {
            for (VideoStream vs : videos) {
                if (playbackQuality.equals(vs.getResolution())) return vs;
            }
            for (VideoStream vs : videoOnly) {
                if (playbackQuality.equals(vs.getResolution())) return vs;
            }
        }
        return null;
    }
}
