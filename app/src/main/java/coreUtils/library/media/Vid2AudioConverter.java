package coreUtils.library.media;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.nio.ByteBuffer;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class Vid2AudioConverter {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private ThreadTask<String, Integer> conversionTask;
	
	public void extractAudio(@Nullable LifecycleOwner owner,
	                         @NonNull String inputFile,
	                         @NonNull String outputFile,
	                         @NonNull ConversionListener listener) {
		
		logger.debug("Starting audio extraction. Input: " + inputFile + ", Output: " + outputFile);
		conversionTask = new ThreadTask.Builder<String, Integer>()
			.withLifecycle(owner)
			.withBackgroundTask(callback -> performExtraction(inputFile, outputFile, callback))
			.withProgressTask(listener::onProgress)
			.withResultTask(listener::onSuccess)
			.withErrorTask(error -> {
				logger.error("Audio extraction failed: " + error.getMessage(), error);
				listener.onFailure(error.getMessage());
			})
			.build();
		
		conversionTask.start();
	}
	
	private String performExtraction(String inputFile, String outputFile,
	                                 ThreadTask.ProgressCallback<Integer> callback) {
		MediaExtractor extractor = new MediaExtractor();
		MediaMuxer muxer = null;
		
		try {
			File sourceFile = new File(inputFile);
			if (!sourceFile.exists()) {
				throw new RuntimeException("Source file does not exist: " + inputFile);
			}
			
			extractor.setDataSource(inputFile);
			
			int audioTrackIndex = -1;
			MediaFormat format = null;
			
			int trackCount = extractor.getTrackCount();
			for (int i = 0; i < trackCount; i++) {
				MediaFormat trackFormat = extractor.getTrackFormat(i);
				String mime = trackFormat.getString(MediaFormat.KEY_MIME);
				if (mime != null && mime.startsWith("audio/")) {
					audioTrackIndex = i;
					format = trackFormat;
					extractor.selectTrack(i);
					logger.debug("Selected audio track #" + i + " with MIME: " + mime);
					break;
				}
			}
			
			if (audioTrackIndex == -1) {
				throw new RuntimeException("No audio track found in: " + inputFile);
			}
			
			String mime = format.getString(MediaFormat.KEY_MIME);
			int muxerFormat;
			
			if ("audio/aac".equals(mime) || "audio/mp4a-latm".equals(mime) || "audio/mpeg".equals(mime)) {
				muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
			} else if ("audio/opus".equals(mime) || "audio/vorbis".equals(mime)) {
				muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM;
			} else {
				throw new RuntimeException("Unsupported audio MIME type for extraction: " + mime);
			}
			
			muxer = new MediaMuxer(outputFile, muxerFormat);
			int newTrackIndex = muxer.addTrack(format);
			muxer.start();
			
			ByteBuffer buffer = ByteBuffer.allocate(4 * 1024 * 1024);
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			
			long totalSize = sourceFile.length();
			long processedSize = 0;
			int lastReportedProgress = -1;
			
			logger.debug("Muxing started. Output format: " + muxerFormat);
			
			while (true) {
				if (conversionTask != null && conversionTask.isCancelled()) {
					logger.warning("Extraction cancelled by user or lifecycle.");
					throw new RuntimeException("Audio extraction cancelled");
				}
				
				buffer.clear();
				int sampleSize = extractor.readSampleData(buffer, 0);
				if (sampleSize < 0) {
					logger.debug("Reached end of stream.");
					break;
				}
				
				bufferInfo.offset = 0;
				bufferInfo.size = sampleSize;
				bufferInfo.presentationTimeUs = extractor.getSampleTime();
				
				int sampleFlags = extractor.getSampleFlags();
				bufferInfo.flags = 0;
				if ((sampleFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
					bufferInfo.flags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
				}
				if ((sampleFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
					bufferInfo.flags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
				}
				
				muxer.writeSampleData(newTrackIndex, buffer, bufferInfo);
				extractor.advance();
				
				processedSize += sampleSize;
				int progress = (int) ((processedSize * 100) / totalSize);
				
				if (progress != lastReportedProgress) {
					callback.onProgress(progress);
					lastReportedProgress = progress;
				}
			}
			
			muxer.stop();
			logger.debug("Extraction successful. File saved: " + outputFile);
			return outputFile;
			
		} catch (Exception error) {
			File partialFile = new File(outputFile);
			if (partialFile.exists()) {
				partialFile.delete();
			}
			throw new RuntimeException(error);
		} finally {
			extractor.release();
			if (muxer != null) {
				muxer.release();
			}
		}
	}
	
	public void cancel() {
		if (conversionTask != null) {
			conversionTask.cancel();
		}
	}
	
	public interface ConversionListener {
		void onProgress(int progress);
		void onSuccess(String outputFile);
		void onFailure(String errorMessage);
	}
}
