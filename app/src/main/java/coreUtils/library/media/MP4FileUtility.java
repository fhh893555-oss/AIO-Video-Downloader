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

public final class MP4FileUtility {
	
	private static final LoggerUtils logger = LoggerUtils.from(MP4FileUtility.class);
	public static final String TMP_MOOV_OPTIMIZED_PREFIX = "moov_optimize_";
	public static final String TMP_MOOV_OPTIMIZED_SUFFIX = ".mp4";
	
	private MP4FileUtility() {}
	
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
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
	
	public static boolean containsMoovAtomAtStart(byte[] data) {
		int index = 0;
		while (index + 8 <= data.length) {
			int size = ((data[index] & 0xFF) << 24)
				| ((data[index + 1] & 0xFF) << 16)
				| ((data[index + 2] & 0xFF) << 8)
				| (data[index + 3] & 0xFF);
			
			if (size <= 0) break;
			if (size < 8) break;
			if (index + size > data.length) break;
			String type = new String(data, index + 4, 4, StandardCharsets.US_ASCII);
			if ("moov".equals(type)) return index <= 1024;
			index += size;
		}
		
		return false;
	}
}