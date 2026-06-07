package sysModules.player.resolver;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;

import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Stream;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.SubtitlesStream;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.mediaitem.MediaItemTag;

@SuppressWarnings("all") public final class VideoPlaybackResolver {
    private static final LoggerUtils logger = LoggerUtils.from(VideoPlaybackResolver.class);

    private static final List<Integer> SUPPORTED_ITAG_IDS = List.of(
            17, 36, 18, 34, 35, 59, 78, 22, 37, 38, 43, 44, 45, 46, 171,
            172, 139, 140, 141, 249, 250, 251, 160, 133, 134, 135, 212,
            136, 298, 137, 299, 266, 278, 242, 243, 244, 245, 246, 247,
            248, 271, 272, 302, 303, 308, 313, 315
    );

    private static final List<MediaFormat> VIDEO_FORMAT_QUALITY_RANKING =
            List.of(MediaFormat.v3GPP, MediaFormat.WEBM, MediaFormat.MPEG_4);

    private static final List<MediaFormat> AUDIO_FORMAT_QUALITY_RANKING =
            List.of(MediaFormat.MP3, MediaFormat.WEBMA, MediaFormat.M4A);

    private static final List<MediaFormat> AUDIO_FORMAT_EFFICIENCY_RANKING =
            List.of(MediaFormat.MP3, MediaFormat.M4A, MediaFormat.WEBMA);

    private static final Set<String> HIGH_RESOLUTION_LIST =
            Set.of("1440p", "2160p");

    private final DataSource.Factory dataSourceFactory;
    private final QualityResolver qualityResolver;
    private final Config config;

    @Nullable private String playbackQuality;
    @Nullable private String audioTrack;
    @Nullable private SourceType streamSourceType;

    public VideoPlaybackResolver(@NonNull DataSource.Factory dataSourceFactory,
                                  @NonNull QualityResolver qualityResolver,
                                  @NonNull Config config) {
        this.dataSourceFactory = dataSourceFactory;
        this.qualityResolver = qualityResolver;
        this.config = config;
    }

    public interface Config {
        @Nullable MediaFormat getDefaultVideoFormat();
        @Nullable MediaFormat getDefaultAudioFormat();
        boolean showHigherResolutions();
        @Nullable String getPreferredAudioLanguage();
    }

    public interface QualityResolver {
        int getDefaultResolutionIndex(@NonNull List<VideoStream> sortedVideos);
        int getOverrideResolutionIndex(@NonNull List<VideoStream> sortedVideos,
                                       @NonNull String playbackQuality);
    }

    public enum SourceType {
        LIVE_STREAM,
        VIDEO_WITH_SEPARATED_AUDIO,
        VIDEO_WITH_AUDIO_OR_AUDIO_ONLY
    }

    public void setPlaybackQuality(@Nullable String quality) {
        this.playbackQuality = quality;
    }

    @Nullable
    public String getPlaybackQuality() {
        return playbackQuality;
    }

    public void setAudioTrack(@Nullable String audioTrack) {
        this.audioTrack = audioTrack;
    }

    @Nullable
    public String getAudioTrack() {
        return audioTrack;
    }

    @Nullable
    public SourceType getStreamSourceType() {
        return streamSourceType;
    }

    // ─── Resolve ─────────────────────────────────────────────────────────

    @Nullable
    public MediaSource resolve(@NonNull StreamInfo info) {
        final MediaSource liveSource = PlaybackResolver.maybeBuildLiveMediaSource(
                dataSourceFactory, info);
        if (liveSource != null) {
            streamSourceType = SourceType.LIVE_STREAM;
            return liveSource;
        }

        final List<MediaSource> sources = new ArrayList<>();

        final List<VideoStream> allVideos = getSortedStreamVideosList(
                info.getVideoStreams(), info.getVideoOnlyStreams(),
                false, config.getDefaultVideoFormat(),
                config.showHigherResolutions(), info.getServiceId());

        int audioIndex = getAudioFormatIndex(info.getAudioStreams(),
                config.getPreferredAudioLanguage(), info.getServiceId());

        final List<AudioStream> allAudio = getFilteredAudioStreams(
                info.getAudioStreams(), info.getServiceId());

        if (allVideos.isEmpty() && allAudio.isEmpty()) {
            logger.warning("No playable streams for " + info.getName());
            return null;
        }

        final int videoIndex;
        if (allVideos.isEmpty()) {
            videoIndex = -1;
        } else if (playbackQuality == null) {
            videoIndex = qualityResolver.getDefaultResolutionIndex(allVideos);
        } else {
            videoIndex = qualityResolver.getOverrideResolutionIndex(allVideos, playbackQuality);
        }

        final VideoStream selectedVideo = (videoIndex >= 0 && videoIndex < allVideos.size())
                ? allVideos.get(videoIndex) : null;

        final AudioStream selectedAudio = (audioIndex >= 0 && audioIndex < allAudio.size())
                ? allAudio.get(audioIndex) : null;

        final MediaItemTag tag = new MediaItemTag(
                info.getServiceId(), videoIndex, audioIndex,
                selectedVideo, selectedAudio);

        if (selectedVideo != null) {
            final String cacheKey = PlaybackResolver.cacheKeyOf(info, selectedVideo);
            final MediaSource videoSource = PlaybackResolver.buildMediaSource(
                    dataSourceFactory, selectedVideo, info, cacheKey, tag);
            if (videoSource != null) {
                sources.add(videoSource);
            }
        }

        final boolean needsSeparateAudio = selectedVideo != null
                && (selectedVideo.isVideoOnly() || audioTrack != null);
        if (selectedAudio != null && (selectedVideo == null || needsSeparateAudio)) {
            final String cacheKey = PlaybackResolver.cacheKeyOf(info, selectedAudio);
            final MediaSource audioSource = PlaybackResolver.buildMediaSource(
                    dataSourceFactory, selectedAudio, info, cacheKey, tag);
            if (audioSource != null) {
                sources.add(audioSource);
            }
        }

        if (selectedVideo != null && selectedAudio != null && needsSeparateAudio) {
            streamSourceType = SourceType.VIDEO_WITH_SEPARATED_AUDIO;
        } else if (selectedVideo != null) {
            streamSourceType = SourceType.VIDEO_WITH_AUDIO_OR_AUDIO_ONLY;
        } else if (selectedAudio != null) {
            streamSourceType = SourceType.VIDEO_WITH_AUDIO_OR_AUDIO_ONLY;
        }

        addSubtitleSources(info, sources);

        if (sources.isEmpty()) return null;
        if (sources.size() == 1) return sources.get(0);
        return new MergingMediaSource(true, sources.toArray(new MediaSource[0]));
    }

    // ─── Video stream sorting & dedup ────────────────────────────────────

    @NonNull
    private List<VideoStream> getSortedStreamVideosList(
            @Nullable List<VideoStream> videoStreams,
            @Nullable List<VideoStream> videoOnlyStreams,
            final boolean preferVideoOnlyStreams,
            @Nullable final MediaFormat defaultFormat,
            final boolean showHigherResolutions,
            final int serviceId) {

        final List<VideoStream> videoFiltered = filterPlayable(videoStreams, serviceId);
        final List<VideoStream> videoOnlyFiltered = filterPlayable(videoOnlyStreams, serviceId);

        final List<List<VideoStream>> ordered = preferVideoOnlyStreams
                ? Arrays.asList(videoFiltered, videoOnlyFiltered)
                : Arrays.asList(videoOnlyFiltered, videoFiltered);

        final List<VideoStream> allInitialStreams = ordered.stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(s -> showHigherResolutions
                        || !HIGH_RESOLUTION_LIST.contains(
                                s.getResolution().replaceAll("p\\d+$", "p")))
                .collect(Collectors.toList());

        final HashMap<String, VideoStream> deduped = new HashMap<>();
        for (final VideoStream vs : allInitialStreams) {
            deduped.put(vs.getResolution(), vs);
        }
        for (final VideoStream vs : allInitialStreams) {
            if (vs.getFormat() == defaultFormat) {
                deduped.put(vs.getResolution(), vs);
            }
        }

        final List<VideoStream> result = new ArrayList<>(deduped.values());
        result.sort((a, b) -> {
            int cmp = compareVideoStreamResolution(b.getResolution(), a.getResolution());
            if (cmp != 0) return cmp;
            return Integer.compare(
                    VIDEO_FORMAT_QUALITY_RANKING.indexOf(a.getFormat()),
                    VIDEO_FORMAT_QUALITY_RANKING.indexOf(b.getFormat()));
        });
        return result;
    }

    @Nullable
    private static List<VideoStream> filterPlayable(
            @Nullable List<VideoStream> streams, final int serviceId) {
        if (streams == null) return null;
        final boolean isYoutube = serviceId == ServiceList.YouTube.getServiceId();
        final List<VideoStream> result = new ArrayList<>();
        for (final VideoStream vs : streams) {
            if (vs.getDeliveryMethod() == DeliveryMethod.TORRENT) continue;
            if (isYoutube && !isSupportedItag(vs.getItagItem())) continue;
            if (resolveUrl(vs) == null) continue;
            result.add(vs);
        }
        return result;
    }

    private static int compareVideoStreamResolution(@NonNull final String r1,
                                                     @NonNull final String r2) {
        try {
            final int res1 = Integer.parseInt(r1.replaceAll("0p\\d+$", "1")
                    .replaceAll("[^\\d.]", ""));
            final int res2 = Integer.parseInt(r2.replaceAll("0p\\d+$", "1")
                    .replaceAll("[^\\d.]", ""));
            return res1 - res2;
        } catch (final NumberFormatException e) {
            return 1;
        }
    }

    // ─── Audio stream filtering & selection ──────────────────────────────

    @NonNull
    private List<AudioStream> getFilteredAudioStreams(
            @Nullable List<AudioStream> streams, final int serviceId) {
        if (streams == null) return Collections.emptyList();
        final boolean isYoutube = serviceId == ServiceList.YouTube.getServiceId();
        final List<AudioStream> filtered = new ArrayList<>();
        for (final AudioStream as : streams) {
            if (as.getDeliveryMethod() == DeliveryMethod.TORRENT) continue;
            if (as.getDeliveryMethod() == DeliveryMethod.HLS
                    && as.getFormat() == MediaFormat.OPUS) continue;
            if (isYoutube && !isSupportedItag(as.getItagItem())) continue;
            if (resolveUrl(as) == null) continue;
            filtered.add(as);
        }

        final HashMap<String, AudioStream> deduped = new HashMap<>();
        for (final AudioStream as : filtered) {
            final String trackId = Objects.toString(as.getAudioTrackId(), "");
            final AudioStream existing = deduped.get(trackId);
            if (existing == null || as.getAverageBitrate() > existing.getAverageBitrate()) {
                deduped.put(trackId, as);
            }
        }

        if (deduped.size() > 1) {
            deduped.remove("");
        }

        return new ArrayList<>(deduped.values());
    }

    private int getAudioFormatIndex(@Nullable List<AudioStream> audioStreams,
                                    @Nullable String preferredLanguage,
                                    final int serviceId) {
        final List<AudioStream> filtered = getFilteredAudioStreams(audioStreams, serviceId);
        if (filtered.isEmpty()) return -1;

        if (preferredLanguage != null) {
            for (int i = 0; i < filtered.size(); i++) {
                final AudioStream s = filtered.get(i);
                if (s.getAudioLocale() != null
                        && s.getAudioLocale().getISO3Language().equals(preferredLanguage)) {
                    return i;
                }
            }
        }

        final MediaFormat defaultFormat = config.getDefaultAudioFormat();
        final Comparator<AudioStream> comparator = getAudioFormatComparator(defaultFormat, false);
        int bestIndex = 0;
        AudioStream best = filtered.get(0);
        for (int i = 1; i < filtered.size(); i++) {
            final AudioStream current = filtered.get(i);
            if (comparator.compare(best, current) < 0) {
                best = current;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private static Comparator<AudioStream> getAudioFormatComparator(
            @Nullable final MediaFormat defaultFormat, final boolean limitDataUsage) {
        final List<MediaFormat> formatRanking = limitDataUsage
                ? AUDIO_FORMAT_EFFICIENCY_RANKING
                : AUDIO_FORMAT_QUALITY_RANKING;

        final Comparator<AudioStream> bitrateComparator =
                Comparator.comparingInt(AudioStream::getAverageBitrate);

        return Comparator.comparing(AudioStream::getFormat, (o1, o2) -> {
            if (defaultFormat != null) {
                return Boolean.compare(o1 == defaultFormat, o2 == defaultFormat);
            }
            return 0;
        }).thenComparing(bitrateComparator)
                .thenComparingInt(s -> formatRanking.indexOf(s.getFormat()));
    }

    // ─── Utils ───────────────────────────────────────────────────────────

    private static boolean isSupportedItag(@Nullable final ItagItem itag) {
        return itag == null || SUPPORTED_ITAG_IDS.contains(itag.id);
    }

    @Nullable
    private static String resolveUrl(@NonNull final Stream stream) {
        if (stream.isUrl()) {
            return stream.getContent();
        }
        return stream.getUrl();
    }

    // ─── Subtitle sources ────────────────────────────────────────────────

    private void addSubtitleSources(@NonNull StreamInfo info,
                                     @NonNull List<MediaSource> sources) {
        final List<SubtitlesStream> subtitles = info.getSubtitles();
        if (subtitles == null || subtitles.isEmpty()) return;

        for (final SubtitlesStream subtitle : subtitles) {
            if (!subtitle.isUrl()
                    || subtitle.getDeliveryMethod() == DeliveryMethod.TORRENT) continue;
            final MediaFormat format = subtitle.getFormat();
            if (format == null) continue;

            @C.RoleFlags final int textRoleFlag = subtitle.isAutoGenerated()
                    ? C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND
                    : C.ROLE_FLAG_CAPTION;

            final MediaItem.SubtitleConfiguration config =
                    new MediaItem.SubtitleConfiguration.Builder(
                            Uri.parse(subtitle.getContent()))
                            .setMimeType(format.getMimeType())
                            .setRoleFlags(textRoleFlag)
                            .setLanguage(subtitle.getLanguageTag())
                            .build();

            final MediaSource textSource = new SingleSampleMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(config, C.TIME_UNSET);
            sources.add(textSource);
        }
    }
}
