package coreUtils.library.media;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.FileDataSourceImpl;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

import coreUtils.library.process.LoggerUtils;

/**
 * Utility class for performing advanced operations and validations on MP4 files.
 *
 * <p>This class provides methods to verify the integrity of MP4 containers, check for
 * stream-ability (Fast Start), and optimize files by relocating the 'moov' atom
 * (metadata) to the beginning of the file. It utilizes the MP4Parser library
 * for deep structural analysis of tracks and samples.</p>
 *
 * <p>Key features include:
 * <ul>
 *   <li>Basic and advanced MP4 integrity validation.</li>
 *   <li>Detection of 'moov' atom positioning for seekable streaming.</li>
 *   <li>Fast Start optimization (moving 'moov' atom to the front).</li>
 * </ul>
 * </p>
 */
public final class MP4FileUtility {

    /**
     * Logger instance for tracking errors and diagnostic information within the MP4 file utility.
     */
    private static final LoggerUtils logger = LoggerUtils.from(MP4FileUtility.class);

    /**
     * The prefix used for temporary files generated during the moov atom optimization process.
     * This prefix helps identify intermediate files created when restructuring the MP4
     * container to move the metadata (moov atom) to the beginning of the file.
     */
    public static final String TMP_MOOV_OPTIMIZED_PREFIX = "moov_optimize_";

    /**
     * The file extension suffix used for temporary files generated during the
     * moov atom optimization process.
     */
    public static final String TMP_MOOV_OPTIMIZED_SUFFIX = ".mp4";

    private MP4FileUtility() {}

    /**
     * Performs an advanced validation of an MP4 file by parsing its internal structure.
     * <p>
     * Unlike basic header checks, this method uses the MP4Parser library to:
     * 1. Ensure the file has a minimum length (100 bytes).
     * 2. Attempt to build a {@link Movie} object from the file data source.
     * 3. Verify that the movie contains at least one track.
     * 4. Verify that every track in the movie contains at least one sample.
     * </p>
     *
     * @param file The MP4 file to validate.
     * @return {@code true} if the file is a structurally valid MP4 with playable tracks and samples;
     * {@code false} otherwise or if an exception occurs during parsing.
     */
    public static boolean isValidMp4FileAdvanced(File file) {
        if (!file.exists() || file.length() < 100) return false;

        try (FileDataSourceImpl dataSource =
                     new FileDataSourceImpl(file.getAbsolutePath())) {
            Movie movie = MovieCreator.build(dataSource);
            if (movie.getTracks().isEmpty()) return false;
            for (Track track : movie.getTracks()) {
                if (track.getSamples().isEmpty()) return false;
            }
            return true;
        } catch (Exception error) {
            logger.error("Advanced MP4 validation failed", error);
            return false;
        }
    }

    /**
     * Performs a fast validation of an MP4 file by checking its header for the "ftyp" signature.
     * <p>
     * This method reads the first 12 bytes of the file and verifies that the brand identifier
     * starting at offset 4 is equal to "ftyp". This is a lightweight check and does not
     * guarantee that the file structure is fully intact or playable.
     * </p>
     *
     * @param file The file to be validated.
     * @return {@code true} if the file exists, meets the minimum size requirement,
     * and contains the MP4 "ftyp" signature; {@code false} otherwise.
     */
    public static boolean isValidMp4File(File file) {
        if (!file.exists() || file.length() < 12) return false;
        try (FileInputStream fis = new FileInputStream(file)) {

            byte[] buffer = new byte[12];
            int bytesRead = fis.read(buffer);

            if (bytesRead < 12) return false;
            String signature = new String(buffer, 4, 4, StandardCharsets.US_ASCII);
            return "ftyp".equals(signature);
        } catch (Exception error) {
            logger.error("Error validating MP4 file", error);
            return false;
        }
    }

    /**
     * Relocates the 'moov' atom to the beginning of an MP4 file.
     * <p>
     * This process (often called "Fast Start" or "web optimization") allows the video to
     * start playing before the entire file has been downloaded. It uses the MP4Parser
     * library to rebuild the file structure, ensuring that the metadata precedes the
     * media data (mdat).
     * </p>
     * <p>
     * The method performs several safety checks, including:
     * <ul>
     *     <li>Verifying input file existence and readability.</li>
     *     <li>Ensuring sufficient disk space is available for the operation (requires approx. 2x input size).</li>
     *     <li>Validating the MP4 format of both input and output files.</li>
     *     <li>Checking the file size ratio to prevent data loss during processing.</li>
     * </ul>
     *
     * @param inputFile  The source MP4 file to be optimized.
     * @param outputFile The destination file where the optimized MP4 will be saved.
     * @return {@code true} if the operation was successful and the output file is a valid MP4;
     * {@code false} otherwise.
     */
    public static boolean moveMoovAtomToStart(File inputFile, File outputFile) {
        if (!inputFile.exists()) return false;
        if (inputFile.length() <= 0L) return false;
        if (!inputFile.canRead()) return false;

        File outputDir = outputFile.getParentFile();
        if (outputDir != null) {
            if (!outputDir.exists() && !outputDir.mkdirs()) return false;
            if (!outputDir.canWrite()) return false;
        }

        long requiredSpace = inputFile.length() * 2;
        File spaceDir = outputDir;
        if (spaceDir == null) spaceDir = inputFile.getParentFile();

        long availableSpace = 0L;
        if (spaceDir != null) availableSpace = spaceDir.getFreeSpace();
        if (availableSpace < requiredSpace) return false;
        if (!isValidMp4File(inputFile)) return false;

        File tempFile;
        try {
            String prefix = TMP_MOOV_OPTIMIZED_PREFIX + inputFile.getName();
            tempFile = File.createTempFile(prefix, TMP_MOOV_OPTIMIZED_SUFFIX, outputDir);
        } catch (Exception error) {
            logger.error("Temp file creation failed", error);
            return false;
        }

        try (FileDataSourceImpl dataSource =
                     new FileDataSourceImpl(inputFile.getAbsolutePath())) {
            Movie movie = MovieCreator.build(dataSource);

            if (movie.getTracks().isEmpty()) {
                tempFile.delete();
                return false;
            }

            DefaultMp4Builder builder = new DefaultMp4Builder();
            Container container = builder.build(movie);

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                container.writeContainer(fos.getChannel());
                fos.getChannel().force(true);
            }

            if (!isValidMp4File(tempFile)) {
                tempFile.delete();
                return false;
            }

            long inputSize = inputFile.length();
            long tempSize = tempFile.length();

            double sizeRatio = (double) tempSize / (double) inputSize;
            if (sizeRatio < 0.5 || sizeRatio > 1.5) {
                tempFile.delete();
                return false;
            }

            if (outputFile.exists() && !outputFile.delete()) {
                tempFile.delete();
                return false;
            }

            boolean renamed = tempFile.renameTo(outputFile);
            if (!renamed) {
                tempFile.delete();
                return false;
            }

            if (!isValidMp4FileAdvanced(outputFile)) {
                outputFile.delete();
                return false;
            }

            return true;
        } catch (Exception error) {
            logger.error("Error moving moov atom to start", error);

            try {
                if (tempFile.exists()) tempFile.delete();
            } catch (Exception ignored) {}

            try {
                if (outputFile.exists()) outputFile.delete();
            } catch (Exception ignored) {}

            return false;
        } finally {
            try {
                if (tempFile.exists()) tempFile.delete();
            } catch (Exception ignored) {}
        }
    }

    /**
     * Determines if an MP4 file is "seekable" for progressive download or fast-start streaming.
     * <p>
     * A file is considered seekable if it meets the following criteria:
     * <ul>
     *     <li>The file exists and meets a minimum size threshold.</li>
     *     <li>The 'moov' atom (metadata) is located at the beginning of the file (Fast Start/Web Optimized).</li>
     *     <li>The file structure is valid and contains at least one track.</li>
     *     <li>All identified tracks contain valid sample data.</li>
     * </ul>
     *
     * @param file The MP4 file to validate.
     * @return {@code true} if the file is a valid MP4 with the 'moov' atom at the start and contains
     * readable tracks/samples; {@code false} otherwise.
     */
    public static boolean isMp4Seekable(File file) {
        if (!file.exists() || file.length() < 100) return false;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead = fis.read(buffer);

            if (bytesRead <= 0) {
                logger.debug("Failed to read file: " + file.getAbsolutePath());
                return false;
            }

            byte[] content = new byte[bytesRead];
            System.arraycopy(buffer, 0, content, 0, bytesRead);

            if (!containsMoovAtomAtStart(content)) {
                logger.debug("'moov' atom not found near start: " + file.getName());
                return false;
            }
        } catch (Exception error) {
            logger.error("Error validating MP4 file", error);
            return false;
        }

        try (FileDataSourceImpl dataSource =
                     new FileDataSourceImpl(file.getAbsolutePath())) {
            Movie movie = MovieCreator.build(dataSource);
            if (movie.getTracks().isEmpty()) {
                return false;
            }

            for (Track track : movie.getTracks()) {
                if (track.getSamples().isEmpty()) {
                    return false;
                }
            }

            return true;
        } catch (Exception error) {
            logger.error("MP4Parser validation failed", error);
            return false;
        }
    }

    /**
     * Checks if the MP4 'moov' atom (movie metadata) is located near the beginning of the byte array.
     * This is typically used to determine if an MP4 file is "fast-start" optimized or streamable,
     * allowing playback to begin before the entire file is downloaded.
     *
     * @param data The byte array containing the header data of an MP4 file.
     * @return {@code true} if the 'moov' atom is found within the first 1024 bytes; {@code false} otherwise.
     */
    public static boolean containsMoovAtomAtStart(byte[] data) {
        int i = 0;
        while (i + 8 <= data.length) {
            int size = ((data[i] & 0xFF) << 24)
                    | ((data[i + 1] & 0xFF) << 16)
                    | ((data[i + 2] & 0xFF) << 8)
                    | (data[i + 3] & 0xFF);

            if (size <= 0) break;
            if (size < 8) break;
            if (i + size > data.length) break;
            String type = new String(data, i + 4, 4, StandardCharsets.US_ASCII);
            if ("moov".equals(type)) return i <= 1024;
            i += size;
        }

        return false;
    }
}