package userInterface.appUpdater;

import androidx.annotation.NonNull;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;

public class ApkDownloader {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final OkHttpClient client;
	
	public ApkDownloader() {
		this.client = new OkHttpClient();
	}
	
	public void startDownload(@NotNull UpdateInfo updateInfo,
	                          @NotNull File downloadDir,
	                          @NotNull ProgressListener listener) {
		
		if (!downloadDir.exists() && !downloadDir.mkdirs()) {
			String errorMsg = "Failed to create download directory.";
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return;
		}
		
		String fileName = StringHelper.getText(R.string.title_app_name_full) +
			"_" + updateInfo.getVersionCode() + "_" + updateInfo.getVersionName() + ".apk";
		
		File outputFile = new File(downloadDir, fileName);
		
		Request headRequest = new Request.Builder()
			.url(updateInfo.getApkFileUrl())
			.head()
			.build();
		
		client.newCall(headRequest).enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException error) {
				handleHeadRequestFailure(error, outputFile, updateInfo, listener);
			}
			
			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) {
				startOrResumeDownload(response, outputFile, updateInfo, listener);
			}
		});
	}
	
	private void startOrResumeDownload(@NonNull Response response,
	                                   @NotNull File outputFile,
	                                   @NonNull UpdateInfo updateInfo,
	                                   @NonNull ProgressListener listener) {
		String apkFileUrl = updateInfo.getApkFileUrl();
		long totalServerSize = response.body().contentLength();
		response.close();
		
		if (totalServerSize <= 0) {
			if (outputFile.exists()) outputFile.delete();
			executeDownloadRequest(apkFileUrl, outputFile, listener, 0);
			return;
		}
		
		if (outputFile.exists()) {
			long localSize = outputFile.length();
			if (localSize == totalServerSize) {
				handleAlreadyDownloaded(outputFile, listener);
			} else if (localSize < totalServerSize) {
				executeResumedDownload(outputFile, updateInfo, listener, localSize);
			} else {
				reinitializeDownload(outputFile, updateInfo, listener);
			}
		} else {
			executeDownloadRequest(apkFileUrl, outputFile, listener, 0);
		}
	}
	
	private void reinitializeDownload(@NonNull File outputFile,
	                                  @NonNull UpdateInfo updateInfo,
	                                  @NonNull ProgressListener listener) {
		logger.warning("Local file is larger than server file. Restarting download.");
		outputFile.delete();
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, 0);
	}
	
	private void executeResumedDownload(@NonNull File outputFile,
	                                    @NonNull UpdateInfo updateInfo,
	                                    @NonNull ProgressListener listener,
	                                    long localSize) {
		logger.debug("Partial download found. Resuming from byte: " + localSize);
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, localSize);
	}
	
	private void handleAlreadyDownloaded(@NonNull File outputFile,
	                                     @NonNull ProgressListener listener) {
		logger.debug("File already fully downloaded. Triggering completion.");
		handleFullyDownloaded(outputFile, listener);
	}
	
	private void handleHeadRequestFailure(@NonNull IOException error,
	                                      @NotNull File outputFile,
	                                      @NonNull UpdateInfo updateInfo,
	                                      @NonNull ProgressListener listener) {
		logger.error("HEAD request failed, falling back to full download: ", error);
		if (outputFile.exists()) outputFile.delete();
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, 0);
	}
	
	private void executeDownloadRequest(@NotNull String url,
	                                    @NonNull File outputFile,
	                                    @NotNull ProgressListener listener,
	                                    long downloadedBytes) {
		boolean isResume = downloadedBytes > 0;
		Request.Builder requestBuilder = new Request.Builder().url(url);
		
		if (isResume) {
			requestBuilder.addHeader("Range", "bytes=" + downloadedBytes + "-");
		}
		
		client.newCall(requestBuilder.build()).enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException error) {
				logger.error("Network request failed: " + error.getMessage());
				listener.onError(error.getMessage());
			}
			
			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) {
				if (!response.isSuccessful()) {
					String errorMsg = "Server returned HTTP error: " + response.code();
					logger.error(errorMsg);
					listener.onError(errorMsg);
					return;
				}
				
				ResponseBody body = response.body();
				
				boolean serverSupportsResume = isResume && response.code() == 206;
				long startingBytes = serverSupportsResume ? downloadedBytes : 0;
				
				if (isResume && !serverSupportsResume) {
					logger.warning("Server does not support resuming. Restarting download.");
					if (outputFile.exists()) {
						outputFile.delete();
					}
				}
				
				try {
					processDownloadStream(body, outputFile, listener,
						startingBytes, serverSupportsResume);
				} catch (Exception error) {
					logger.error("Error writing file or hashing: " + error.getMessage());
					listener.onError(error.getMessage());
				} finally {
					body.close();
				}
			}
		});
	}
	
	private void processDownloadStream(ResponseBody body, File outputFile, ProgressListener listener,
	                                   long alreadyDownloadedBytes, boolean appendToFile)
		throws IOException, NoSuchAlgorithmException {
		
		long remainingBytes = body.contentLength();
		long totalBytes = remainingBytes + alreadyDownloadedBytes;
		long currentDownloadedBytes = alreadyDownloadedBytes;
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		
		if (appendToFile && outputFile.exists()) {
			try (InputStream existingFileInputStream = new FileInputStream(outputFile)) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = existingFileInputStream.read(buffer)) != -1) {
					digest.update(buffer, 0, bytesRead);
				}
			}
		}
		
		try (InputStream inputStream = body.byteStream();
		     FileOutputStream outputStream = new FileOutputStream(outputFile, appendToFile)) {
			
			byte[] buffer = new byte[8192];
			int bytesRead;
			
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				digest.update(buffer, 0, bytesRead);
				currentDownloadedBytes += bytesRead;
				
				if (listener != null && totalBytes > 0) {
					short percentage = (short) ((currentDownloadedBytes * 100L) / totalBytes);
					listener.onProgressUpdate(percentage, currentDownloadedBytes);
				}
			}
			outputStream.flush();
		}
		
		if (listener != null) {
			String hashHex = bytesToHex(digest.digest());
			listener.onDownloadComplete(outputFile, hashHex);
		}
	}
	
	private void handleFullyDownloaded(File outputFile, ProgressListener listener) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			try (InputStream inputStream = new FileInputStream(outputFile)) {
				byte[] buffer = new byte[8192];
				int bytesRead;
				while ((bytesRead = inputStream.read(buffer)) != -1) {
					digest.update(buffer, 0, bytesRead);
				}
			}
			if (listener != null) {
				listener.onProgressUpdate((short) 100, outputFile.length());
				listener.onDownloadComplete(outputFile, bytesToHex(digest.digest()));
			}
		} catch (Exception e) {
			logger.error("Failed to hash existing file: " + e.getMessage());
			if (listener != null) listener.onError("Failed to verify existing file.");
		}
	}
	
	private String bytesToHex(byte[] bytes) {
		StringBuilder hexString = new StringBuilder(2 * bytes.length);
		for (byte b : bytes) {
			String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}
	
	public interface ProgressListener {
		void onProgressUpdate(short percentage, long downloadedByte);
		void onDownloadComplete(File apkFile, String downloadedApkHash);
		void onError(String errorMessage);
	}
}