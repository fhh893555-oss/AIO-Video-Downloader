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
import org.schabi.newpipe.extractor.stream.AudioTrackType;
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
import java.util.Locale;
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

    private static final List<AudioTrackType> AUDIO_TRACK_TYPE_RANKING =
            List.of(AudioTrackType.ORIGINAL, AudioTrackType.DUBBED,
                    AudioTrackType.DESCRIPTIVE, AudioTrackType.SECONDARY);

    private static final List<AudioTrackType> AUDIO_TRACK_TYPE_RANKING_DESCRIPTIVE =
            List.of(AudioTrackType.DESCRIPTIVE, AudioTrackType.DUBBED,
                    AudioTrackType.ORIGINAL, AudioTrackType.SECONDARY);

    private static final Set<String> HIGH_RESOLUTION_LIST =
            Set.of("1440p", "2160p");

    private static final String ENGLISH_LANGUAGE = "eng";

    private final DataSource.Factory dataSourceFactory;
    private final Config config;

    @Nullable private String playbackQuality;
    @Nullable private String audioTrack;
    @Nullable private SourceType streamSourceType;

    public VideoPlaybackResolver(@NonNull DataSource.Factory dataSourceFactory,
                                  @NonNull Config config) {
        this.dataSourceFactory = dataSourceFactory;
        this.config = config;
    }

    public interface Config {
        @Nullable MediaFormat getDefaultVideoFormat();
        @Nullable MediaFormat getDefaultAudioFormat();
        boolean showHigherResolutions();
        @Nullable String getPreferredAudioLanguage();
        boolean limitDataUsage();
        boolean preferVideoOnly();
        boolean preferOriginalAudio();
        boolean preferDescriptiveAudio();
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
                config.preferVideoOnly(), config.getDefaultVideoFormat(),
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
            videoIndex = getDefaultResolutionIndex(allVideos, config.getDefaultVideoFormat());
        } else {
            videoIndex = getOverrideResolutionIndex(allVideos, playbackQuality,
                    config.getDefaultVideoFormat());
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

    // ─── 5-phase resolution index selection ──────────────────────────────

    private static int parseResolutionValue(@NonNull final String resolution) {
        // Extract base height before 'p' — "1080p60" → 1080, "720p" → 720
        try {
            final int pIndex = resolution.indexOf('p');
            if (pIndex > 0) {
                return Integer.parseInt(resolution.substring(0, pIndex)
                        .replaceAll("[^\\d]", ""));
            }
            return Integer.parseInt(resolution.replaceAll("[^\\d]", ""));
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    @NonNull
    private static String stripRefreshRate(@NonNull final String resolution) {
        return resolution.replaceAll("p\\d+$", "p");
    }

    private static int getDefaultResolutionIndex(
            @NonNull final List<VideoStream> sortedVideos,
            @Nullable final MediaFormat defaultFormat) {
        if (sortedVideos.isEmpty()) return 0;
        return 0;
    }

    private static int getOverrideResolutionIndex(
            @NonNull final List<VideoStream> sortedVideos,
            @NonNull final String playbackQuality,
            @Nullable final MediaFormat defaultFormat) {
        if (sortedVideos.isEmpty()) return 0;

        final String qualityResolution = parseResolutionString(playbackQuality);
        final MediaFormat qualityFormat = parseFormatFromQuality(playbackQuality);

        // Phase 1: exact resolution + format match
        for (int i = 0; i < sortedVideos.size(); i++) {
            final VideoStream vs = sortedVideos.get(i);
            if (vs.getResolution().equals(playbackQuality)) {
                return i;
            }
        }

        // Phase 2: format match + resolution ignoring refresh rate
        if (qualityFormat != null) {
            final String strippedQuality = stripRefreshRate(qualityResolution);
            for (int i = 0; i < sortedVideos.size(); i++) {
                final VideoStream vs = sortedVideos.get(i);
                if (vs.getFormat() == qualityFormat
                        && stripRefreshRate(vs.getResolution()).equals(strippedQuality)) {
                    return i;
                }
            }
        }

        // Phase 3: resolution match only
        final int qualityResValue = parseResolutionValue(qualityResolution);
        for (int i = 0; i < sortedVideos.size(); i++) {
            final VideoStream vs = sortedVideos.get(i);
            if (parseResolutionValue(vs.getResolution()) == qualityResValue) {
                return i;
            }
        }

        // Phase 4: resolution ignoring refresh rate
        final String strippedQualityRes = stripRefreshRate(qualityResolution);
        for (int i = 0; i < sortedVideos.size(); i++) {
            final VideoStream vs = sortedVideos.get(i);
            if (stripRefreshRate(vs.getResolution()).equals(strippedQualityRes)) {
                return i;
            }
        }

        // Phase 5: closest lower resolution ignoring refresh rate
        int closestIndex = 0;
        int closestDiff = Integer.MAX_VALUE;
        for (int i = 0; i < sortedVideos.size(); i++) {
            final VideoStream vs = sortedVideos.get(i);
            final int vsRes = parseResolutionValue(vs.getResolution());
            if (vsRes <= qualityResValue) {
                final int diff = qualityResValue - vsRes;
                if (diff < closestDiff) {
                    closestDiff = diff;
                    closestIndex = i;
                }
            }
        }
        return closestIndex;
    }

    @NonNull
    private static String parseResolutionString(@NonNull final String quality) {
        return quality.replaceAll("\\s*\\(.*\\)$", "").trim();
    }

    @Nullable
    private static MediaFormat parseFormatFromQuality(@NonNull final String quality) {
        final int idx = quality.indexOf('(');
        if (idx < 0) return null;
        final String formatPart = quality.substring(idx + 1, quality.indexOf(')', idx)).trim();
        for (final MediaFormat fmt : MediaFormat.values()) {
            if (fmt.getName() != null && fmt.getName().equalsIgnoreCase(formatPart)) {
                return fmt;
            }
        }
        return null;
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

        final Comparator<AudioStream> comparator = getAudioTrackComparator(
                preferredLanguage, config.getDefaultAudioFormat(),
                config.preferOriginalAudio(), config.preferDescriptiveAudio(),
                config.limitDataUsage());
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

    private static Comparator<AudioStream> getAudioTrackComparator(
            @Nullable final String preferredLanguage,
            @Nullable final MediaFormat defaultFormat,
            final boolean preferOriginalAudio,
            final boolean preferDescriptiveAudio,
            final boolean limitDataUsage) {

        final List<MediaFormat> formatRanking = limitDataUsage
                ? AUDIO_FORMAT_EFFICIENCY_RANKING
                : AUDIO_FORMAT_QUALITY_RANKING;

        final List<AudioTrackType> trackTypeRanking = preferDescriptiveAudio
                ? AUDIO_TRACK_TYPE_RANKING_DESCRIPTIVE
                : AUDIO_TRACK_TYPE_RANKING;

        return (a, b) -> {
            // 1) preferOriginalAudio: ORIGINAL always wins
            if (preferOriginalAudio) {
                final boolean aIsOriginal = a.getAudioTrackType() == AudioTrackType.ORIGINAL;
                final boolean bIsOriginal = b.getAudioTrackType() == AudioTrackType.ORIGINAL;
                if (aIsOriginal != bIsOriginal) {
                    return aIsOriginal ? 1 : -1;
                }
            }

            // 2) Language match
            final int aLangScore = languageScore(a, preferredLanguage);
            final int bLangScore = languageScore(b, preferredLanguage);
            if (aLangScore != bLangScore) {
                return Integer.compare(aLangScore, bLangScore);
            }

            // 3) Track type ranking
            final int aTypeIdx = trackTypeRanking.indexOf(a.getAudioTrackType());
            final int bTypeIdx = trackTypeRanking.indexOf(b.getAudioTrackType());
            if (aTypeIdx != bTypeIdx) {
                return Integer.compare(aTypeIdx, bTypeIdx);
            }

            // 4) Default format preference
            if (defaultFormat != null) {
                final boolean aIsDefault = a.getFormat() == defaultFormat;
                final boolean bIsDefault = b.getFormat() == defaultFormat;
                if (aIsDefault != bIsDefault) {
                    return aIsDefault ? 1 : -1;
                }
            }

            // 5) Bitrate
            final int bitrateCompare = Integer.compare(
                    a.getAverageBitrate(), b.getAverageBitrate());
            if (bitrateCompare != 0) {
                return limitDataUsage ? -bitrateCompare : bitrateCompare;
            }

            // 6) Format quality ranking
            return Integer.compare(
                    formatRanking.indexOf(a.getFormat()),
                    formatRanking.indexOf(b.getFormat()));
        };
    }

    private static int languageScore(@NonNull final AudioStream stream,
                                      @Nullable final String preferredLanguage) {
        final Locale locale = stream.getAudioLocale();
        if (locale == null) return 0;

        if (preferredLanguage != null) {
            if (preferredLanguage.equals(locale.getISO3Language())) return 5;
            if (preferredLanguage.length() == 2
                    && preferredLanguage.equals(locale.getLanguage())) return 5;
        }

        if (ENGLISH_LANGUAGE.equals(locale.getISO3Language())) return 3;

        return 1;
    }
    

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
