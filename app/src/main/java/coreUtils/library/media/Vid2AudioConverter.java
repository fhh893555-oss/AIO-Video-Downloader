package coreUtils.library.media;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import coreUtils.library.process.LoggerUtils;

public class Vid2AudioConverter {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final AtomicBoolean isProcessCancelledByUser = new AtomicBoolean(false);
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private boolean isMuxerStarted = false;
	
	public void extractAudio(String inputFile,
	                         String outputFile, ConversionListener listener) {
		MediaExtractor extractor = null;
		MediaMuxer muxer = null;
		
		try {
			extractor = new MediaExtractor();
			extractor.setDataSource(inputFile);
			
			int audioTrackIndex = -1;
			MediaFormat format = null;
			
			for (int i = 0; i < extractor.getTrackCount(); i++) {
				format = extractor.getTrackFormat(i);
				String mime = format.getString(MediaFormat.KEY_MIME);
				if (mime != null && mime.startsWith("audio/")) {
					audioTrackIndex = i;
					extractor.selectTrack(i);
					break;
				}
			}
			
			if (audioTrackIndex == -1) {
				String errorMsg = "No audio track found in video file: " + inputFile;
				logger.debug(errorMsg);
				postFailure(listener, errorMsg);
				return;
			}
			
			String mime = format.getString(MediaFormat.KEY_MIME);
			int muxerFormat;
			
			if ("audio/aac".equals(mime) || "audio/mp4a-latm".equals(mime)) {
				muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4;
				
			} else if ("audio/opus".equals(mime) || "audio/vorbis".equals(mime)) {
				muxerFormat = MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM;
				
			} else {
				String errorMsg = "Unsupported audio MIME type extraction: " + mime;
				logger.error(errorMsg);
				postFailure(listener, errorMsg);
				return;
			}
			
			muxer = new MediaMuxer(outputFile, muxerFormat);
			int newTrackIndex = muxer.addTrack(format);
			muxer.start();
			isMuxerStarted = true;
			
			ByteBuffer buffer = ByteBuffer.allocate(4096);
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			
			float fileSize = new File(inputFile).length();
			long extractedSize = 0;
			
			while (!isProcessCancelledByUser.get()) {
				buffer.clear();
				int sampleSize = extractor.readSampleData(buffer, 0);
				if (sampleSize < 0) break;
				
				bufferInfo.offset = 0;
				bufferInfo.size = sampleSize;
				bufferInfo.presentationTimeUs = extractor.getSampleTime();
				bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
				
				muxer.writeSampleData(newTrackIndex, buffer, bufferInfo);
				extractor.advance();
				
				extractedSize += sampleSize;
				int progress = (int) ((extractedSize / fileSize) * 100);
				postProgress(listener, progress);
			}
			
			if (isProcessCancelledByUser.get()) {
				postFailure(listener, "Audio extraction cancelled");
				return;
			}
			
			muxer.stop();
			isMuxerStarted = false;
			postSuccess(listener, outputFile);
			
		} catch (Exception error) {
			String errorMsg = "Audio extraction failed: " + error.getMessage();
			logger.error(errorMsg + ". Error: " + error);
			postFailure(listener, errorMsg);
		} finally {
			if (isMuxerStarted && muxer != null) {
				try {
					muxer.stop();
				} catch (Exception error) {
					logger.error("Error stopping MediaMuxer", error);
				}
			}
			if (muxer != null) muxer.release();
			if (extractor != null) extractor.release();
			isMuxerStarted = false;
		}
	}
	
	public void cancel() {
		isProcessCancelledByUser.set(true);
	}
	
	private void postProgress(ConversionListener listener, int progress) {
		mainHandler.post(() -> listener.onProgress(progress));
	}
	
	private void postSuccess(ConversionListener listener, String outputFile) {
		mainHandler.post(() -> listener.onSuccess(outputFile));
	}
	
	private void postFailure(ConversionListener listener, String error) {
		mainHandler.post(() -> listener.onFailure(error));
	}
	
	public interface ConversionListener {
		void onProgress(int progress);
		void onSuccess(String outputFile);
		void onFailure(String errorMessage);
	}
}