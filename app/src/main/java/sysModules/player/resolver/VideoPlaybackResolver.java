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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.mediaitem.MediaItemTag;

@SuppressWarnings("deprecation") public final class VideoPlaybackResolver {
	private static final LoggerUtils logger = LoggerUtils.from(VideoPlaybackResolver.class);
	
	private static final List<Integer> SUPPORTED_ITAG_IDS = List.of(
		17, 36, 18, 34, 35, 59, 78, 22, 37, 38, 43, 44, 45, 46, 171,
		172, 139, 140, 141, 249, 250, 251, 160, 133, 134, 135, 212,
		136, 298, 137, 299, 266, 278, 242, 243, 244, 245, 246, 247,
		248, 271, 272, 302, 303, 308, 313, 315
	);
	
	private final DataSource.Factory dataSourceFactory;
	private final QualityResolver qualityResolver;
	
	@Nullable private String playbackQuality;
	@Nullable private String audioTrack;
	@Nullable private SourceType streamSourceType;
	
	public VideoPlaybackResolver(@NonNull DataSource.Factory dataSourceFactory,
	                             @NonNull QualityResolver qualityResolver) {
		this.dataSourceFactory = dataSourceFactory;
		this.qualityResolver = qualityResolver;
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
	
	@Nullable
	public MediaSource resolve(@NonNull StreamInfo info) {
		final MediaSource liveSource = PlaybackResolver.maybeBuildLiveMediaSource(
			dataSourceFactory, info);
		if (liveSource != null) {
			streamSourceType = SourceType.LIVE_STREAM;
			return liveSource;
		}
		
		final List<MediaSource> sources = new ArrayList<>();
		
		final List<VideoStream> videoStreams = getPlayableVideoStreams(
			info.getVideoStreams(), info.getServiceId());
		final List<VideoStream> videoOnlyStreams = getPlayableVideoStreams(
			info.getVideoOnlyStreams(), info.getServiceId());
		final List<AudioStream> audioStreams = getFilteredAudioStreams(
			info.getAudioStreams(), info.getServiceId());
		
		final List<VideoStream> allVideos = new ArrayList<>();
		if (videoStreams != null) allVideos.addAll(videoStreams);
		if (videoOnlyStreams != null) allVideos.addAll(videoOnlyStreams);
		sortVideoStreams(allVideos);
		
		if (allVideos.isEmpty() && audioStreams.isEmpty()) {
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
		
		final int audioIndex = 0;
		final AudioStream selectedAudio = !audioStreams.isEmpty()
			? audioStreams.get(0) : null;
		
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
				streamSourceType = SourceType.VIDEO_WITH_SEPARATED_AUDIO;
			}
		}
		
		if (selectedVideo != null && selectedAudio != null && !needsSeparateAudio) {
			streamSourceType = SourceType.VIDEO_WITH_AUDIO_OR_AUDIO_ONLY;
		}
		
		addSubtitleSources(info, sources);
		
		if (sources.isEmpty()) return null;
		if (sources.size() == 1) return sources.get(0);
		return new MergingMediaSource(true, sources.toArray(new MediaSource[0]));
	}
	
	@Nullable
	private static List<VideoStream> getPlayableVideoStreams(
		@Nullable List<VideoStream> streams, final int serviceId) {
		if (streams == null) return null;
		final boolean isYoutube = serviceId == ServiceList.YouTube.getServiceId();
		final List<VideoStream> result = new ArrayList<>();
		for (final VideoStream vs : streams) {
			if (vs.getDeliveryMethod() == DeliveryMethod.TORRENT) continue;
			if (isYoutube && !isSupportedItag(vs.getItagItem())) continue;
			final String url = resolveUrl(vs);
			if (url == null) continue;
			result.add(vs);
		}
		return result;
	}
	
	@NonNull
	private static List<AudioStream> getFilteredAudioStreams(
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
	
	public static void sortVideoStreams(@NonNull List<VideoStream> streams) {
		streams.sort((a, b) -> Integer.compare(
			parseResolution(b.getResolution()),
			parseResolution(a.getResolution())));
	}
	
	static int parseResolution(@NonNull String resolution) {
		String raw = resolution.toLowerCase().trim();
		String number = raw.replaceAll("[^0-9]", "");
		if (number.isEmpty()) {
			if (raw.contains("4k")) return 2160;
			if (raw.contains("1440")) return 1440;
			if (raw.contains("1080")) return 1080;
			if (raw.contains("720")) return 720;
			if (raw.contains("480")) return 480;
			if (raw.contains("360")) return 360;
			if (raw.contains("240")) return 240;
			if (raw.contains("144")) return 144;
			return 0;
		}
		try {
			int value = Integer.parseInt(number);
			if (raw.contains("p60") || raw.contains("p50")
				|| raw.contains("60fps") || raw.contains("50fps")) {
				value += 1;
			}
			return value;
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
