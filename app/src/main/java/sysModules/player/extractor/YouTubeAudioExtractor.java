package sysModules.player.extractor;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.services.youtube.ItagItem;
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper;
import org.schabi.newpipe.extractor.services.youtube.YoutubeStreamHelper;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import coreUtils.library.process.LoggerUtils;

public final class YouTubeAudioExtractor {
	private static final LoggerUtils logger = LoggerUtils.from(YouTubeAudioExtractor.class);
	
	private static final String VIDEO_DETAILS = "videoDetails";
	private static final String STREAMING_DATA = "streamingData";
	private static final String PLAYABILITY_STATUS = "playabilityStatus";
	private static final String ADAPTIVE_FORMATS = "adaptiveFormats";
	private static final String THUMBNAIL = "thumbnail";
	private static final String THUMBNAILS = "thumbnails";
	private static final String TITLE = "title";
	
	private YouTubeAudioExtractor() {
	}
	
	public static StreamInfo extractAudioInfo(final String videoUrl)
		throws IOException, ExtractionException {
		final StreamingService youtube = NewPipe.getService(ServiceList.YouTube.getServiceId());
		
		final String videoId = youtube.getStreamExtractor(videoUrl).getId();
		
		try {
			return fastExtract(youtube, videoUrl, videoId);
		} catch (final ExtractionException | IOException e) {
			logger.debug("Fast extraction failed for " + videoId + ": " + e.getMessage()
				+ ", falling back to full extraction");
			return StreamInfo.getInfo(youtube, videoUrl);
		}
	}
	
	private static StreamInfo fastExtract(final StreamingService youtube,
	                                      final String videoUrl,
	                                      final String videoId)
		throws IOException, ExtractionException {
		final String cpn = YoutubeParsingHelper.generateContentPlaybackNonce();
		final JsonObject playerResponse = YoutubeStreamHelper.getAndroidReelPlayerResponse(
			NewPipe.getPreferredContentCountry(),
			NewPipe.getPreferredLocalization(),
			videoId,
			cpn);
		
		final JsonObject playabilityStatus = playerResponse.getObject(PLAYABILITY_STATUS);
		if (playabilityStatus != null) {
			final String status = playabilityStatus.getString("status");
			if (status != null && !"OK".equalsIgnoreCase(status)) {
				throw new ContentNotAvailableException(
					"Video not playable: status=" + status
						+ " reason=" + playabilityStatus.getString("reason"));
			}
		}
		
		final JsonObject videoDetails = playerResponse.getObject(VIDEO_DETAILS);
		if (videoDetails == null) {
			throw new ExtractionException("No videoDetails in player response");
		}
		
		final String title = videoDetails.getString(TITLE);
		final String author = videoDetails.getString("author");
		final long lengthSeconds = videoDetails.getLong("lengthSeconds");
		final String extractedVideoId = videoDetails.getString("videoId");
		
		final List<Image> thumbnails = extractThumbnails(videoDetails);
		
		final JsonObject streamingData = playerResponse.getObject(STREAMING_DATA);
		if (streamingData == null) {
			throw new ExtractionException("No streamingData in player response");
		}
		
		final List<AudioStream> audioStreams = extractAudioStreams(streamingData);
		if (audioStreams.isEmpty()) {
			throw new ExtractionException("No audio streams found in player response");
		}
		
		final StreamInfo info = new StreamInfo(
			ServiceList.YouTube.getServiceId(),
			videoUrl,
			videoUrl,
			StreamType.AUDIO_STREAM,
			extractedVideoId != null ? extractedVideoId : videoId,
			title != null ? title : "",
			0);
		info.setAudioStreams(audioStreams);
		info.setDuration(lengthSeconds);
		info.setThumbnails(thumbnails);
		info.setUploaderName(author != null ? author : "");
		
		return info;
	}
	
	private static List<Image> extractThumbnails(final JsonObject videoDetails) {
		final List<Image> images = new ArrayList<>();
		final JsonObject thumbnail = videoDetails.getObject(THUMBNAIL);
		if (thumbnail == null) return images;
		
		final JsonArray thumbnailsArray = thumbnail.getArray(THUMBNAILS);
		if (thumbnailsArray == null) return images;
		
		for (int i = 0; i < thumbnailsArray.size(); i++) {
			final JsonObject t = thumbnailsArray.getObject(i);
			if (t == null) continue;
			final String url = t.getString("url");
			if (url == null) continue;
			images.add(new Image(
				url,
				t.getInt("height"),
				t.getInt("width"),
				Image.ResolutionLevel.UNKNOWN));
		}
		return images;
	}
	
	private static List<AudioStream> extractAudioStreams(final JsonObject streamingData) {
		final List<AudioStream> audioStreams = new ArrayList<>();
		final JsonArray adaptiveFormats = streamingData.getArray(ADAPTIVE_FORMATS);
		if (adaptiveFormats == null) return audioStreams;
		
		for (int i = 0; i < adaptiveFormats.size(); i++) {
			final JsonObject format = adaptiveFormats.getObject(i);
			if (format == null) continue;
			
			final String mimeType = format.getString("mimeType");
			if (mimeType == null || !mimeType.startsWith("audio/")) continue;
			
			final String streamUrl = format.getString("url");
			if (streamUrl == null) continue;
			
			final int itagId = format.getInt("itag");
			if (itagId == 0) continue;
			
			try {
				final AudioStream audioStream = buildAudioStream(itagId, streamUrl, format);
				audioStreams.add(audioStream);
			} catch (final Exception e) {
				logger.debug("Skipping audio format itag=" + itagId + ": " + e.getMessage());
			}
		}
		return audioStreams;
	}
	
	private static AudioStream buildAudioStream(final int itagId,
	                                            final String url,
	                                            final JsonObject format) {
		AudioStream.Builder builder = new AudioStream.Builder()
			.setId(String.valueOf(itagId))
			.setContent(url, true)
			.setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP);
		
		try {
			final ItagItem itagItem = ItagItem.getItag(itagId);
			final MediaFormat mediaFormat = itagItem.getMediaFormat();
			if (mediaFormat != null) {
				builder.setMediaFormat(mediaFormat);
			}
			builder.setItagItem(itagItem);
			
			final int avgBitrate = format.getInt("averageBitrate");
			if (avgBitrate > 0) {
				builder.setAverageBitrate(avgBitrate);
			} else {
				builder.setAverageBitrate(itagItem.getAverageBitrate());
			}
		} catch (final Exception e) {
			logger.debug("Could not get ItagItem for itag " + itagId + ": " + e.getMessage());
		}
		
		return builder.build();
	}
}
