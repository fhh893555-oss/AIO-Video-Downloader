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

/**
 * A utility class for extracting audio tracks from video files on Android.
 * <p>
 * This class uses {@link MediaExtractor} to demux the audio stream and
 * {@link MediaMuxer} to write it into a standalone audio container without
 * re-encoding the data. It supports common formats such as AAC (MPEG-4) and Opus/Vorbis (WebM).
 * </p>
 * <p>
 * Features include:
 * <ul>
 *     <li>Asynchronous progress reporting via {@link ConversionListener}.</li>
 *     <li>Main-thread callback execution for UI updates.</li>
 *     <li>Thread-safe cancellation mechanism.</li>
 * </ul>
 * </p>
 */
public class Vid2AudioConverter {

    /**
     * Logger utility for recording debug information and error messages during
     * the audio extraction process.
     */
    private final LoggerUtils logger = LoggerUtils.from(getClass());

    /**
     * Flag indicating whether the conversion process has been manually canceled by the user.
     * Uses {@link AtomicBoolean} to ensure thread-safe access when checked during the
     * extraction loop or updated via the {@link #cancel()} method.
     */
    private final AtomicBoolean isProcessCancelledByUser = new AtomicBoolean(false);

    /**
     * Indicates whether the {@link MediaMuxer} has been successfully started.
     * This flag is used to ensure that {@link MediaMuxer#stop()} is only called
     * when the muxer is in the correct state, preventing potential crashes
     * during cleanup or error handling.
     */
    private boolean isMuxerStarted = false;

    /**
     * Handler associated with the main looper, used to dispatch progress and result
     * callbacks to the UI thread.
     */
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * Extracts the primary audio track from a video file and saves it to a specified output path.
     * <p>
     * This method identifies the first available audio track in the source file, determines the
     * appropriate container format (MPEG-4 for AAC/MP4A or WebM for Opus/Vorbis), and performs
     * a demuxing process to copy the encoded audio samples without re-encoding.
     * </p>
     *
     * @param inputFile  The absolute path to the source video file.
     * @param outputFile The absolute path where the extracted audio file will be saved.
     * @param listener   A {@link ConversionListener} to receive updates on progress,
     *                   completion, or error events.
     */
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

                if (sampleSize < 0) {
                    break;
                }

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

        } catch (Exception e) {
            String errorMsg = "Audio extraction failed: " + e.getMessage();
            logger.error(errorMsg + ". Error: " + e);
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

    /**
     * Cancels the ongoing audio extraction process.
     * <p>
     * When called, the extraction loop will terminate, and the failure callback
     * will be triggered via the {@link ConversionListener}.
     */
    public void cancel() {
        isProcessCancelledByUser.set(true);
    }

    /**
     * Posts a progress update to the provided listener on the main thread.
     *
     * @param listener The listener to receive the progress update.
     * @param progress The current conversion progress percentage (0-100).
     */
    private void postProgress(ConversionListener listener, int progress) {
        mainHandler.post(() -> listener.onProgress(progress));
    }

    /**
     * Dispatches the success callback to the main thread upon successful completion of the extraction.
     *
     * @param listener   The listener to notify.
     * @param outputFile The file path of the successfully extracted audio.
     */
    private void postSuccess(ConversionListener listener, String outputFile) {
        mainHandler.post(() -> listener.onSuccess(outputFile));
    }

    /**
     * Posts a failure notification to the provided listener on the main UI thread.
     *
     * @param listener The listener to be notified of the failure.
     * @param error    A descriptive error message explaining why the conversion failed.
     */
    private void postFailure(ConversionListener listener, String error) {
        mainHandler.post(() -> listener.onFailure(error));
    }

    /**
     * Callback interface for monitoring the audio extraction process.
     */
    public interface ConversionListener {
        /**
         * Called when a chunk of data is processed.
         *
         * @param progress The estimated percentage of completion (0-100).
         */
        void onProgress(int progress);

        /**
         * Called when the audio track has been successfully saved to disk.
         *
         * @param outputFile The path to the newly created audio file.
         */
        void onSuccess(String outputFile);

        /**
         * Called if the process encounters an error or is canceled.
         *
         * @param errorMessage A descriptive error message.
         */
        void onFailure(String errorMessage);
    }
}