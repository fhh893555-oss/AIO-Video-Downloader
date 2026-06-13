package userInterface.appUpdater;

import androidx.annotation.NonNull;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import coreUtils.library.networks.URLUtility;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.storage.FileStorageUtility;
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
	private Call currentCall;
	
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
		
		if (outputFile.exists() && !outputFile.canWrite()) {
			String errorMsg = "Cannot write to existing APK file. " +
				"The file may be protected or storage permission is denied.";
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return;
		}
		
		Request headRequest = new Request.Builder()
			.url(updateInfo.getApkFileUrl())
			.head()
			.build();
		
		currentCall = client.newCall(headRequest);
		currentCall.enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException error) {
				if (call.isCanceled()) {
					logger.debug("HEAD request canceled.");
					return;
				}
				
				handleHeadRequestFailure(error, outputFile, updateInfo, listener);
			}
			
			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) {
				try {
					startOrResumeDownload(response, outputFile, updateInfo, listener);
					
				} catch (MalformedURLException error) {
					logger.error("Url malfunction is found: ", error);
				} catch (Exception error) {
					logger.error("Something went wrong: ", error);
				}
			}
		});
	}
	
	private void startOrResumeDownload(@NonNull Response response,
	                                   @NotNull File outputFile,
	                                   @NonNull UpdateInfo updateInfo,
	                                   @NonNull ProgressListener listener) throws MalformedURLException {
		String apkFileUrl = updateInfo.getApkFileUrl();
		long totalServerSize = URLUtility.getFileSizeFromUrl(new URL(apkFileUrl));
		logger.debug("Received file size form server: " +
			FileStorageUtility.humanReadableSizeOf(totalServerSize));
		response.close();
		
		if (totalServerSize <= 0) {
			logger.debug("File size is received from server: 0Byte");
			if (outputFile.exists()) outputFile.delete();
			executeDownloadRequest(apkFileUrl, outputFile, listener, 0);
			return;
		}
		
		if (outputFile.exists()) {
			long localSize = outputFile.length();
			logger.debug("Local file is existed, file size:" +
				FileStorageUtility.humanReadableSizeOf(localSize));
			
			if (localSize == totalServerSize) {
				logger.debug("Local file and server file size matched, " +
					"meaning no need to download");
				handleAlreadyDownloaded(outputFile, listener);
				
			} else if (localSize < totalServerSize) {
				logger.debug("Local file has not fully downloaded, " +
					"need to resume download");
				executeResumedDownload(outputFile, updateInfo,
					listener, localSize);
				
			} else {
				logger.debug("Starting over the download");
				reinitializeDownload(outputFile, updateInfo, listener);
			}
		} else {
			logger.debug("Local file is not found, redownload the file");
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
		
		currentCall = client.newCall(requestBuilder.build());
		currentCall.enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException error) {
				if (call.isCanceled()) {
					logger.debug("Download request canceled.");
					return;
				}
				
				logger.error("Network request failed: " + error.getMessage());
				listener.onError(error.getMessage());
			}
			
			@Override
			public void onResponse(@NotNull Call call, @NotNull Response response) {
				if (isRequestFailed(response, listener)) return;
				
				ResponseBody body = response.body();
				boolean serverSupportsResume = isResume && response.code() == 206;
				long startingBytes = serverSupportsResume ? downloadedBytes : 0;
				
				if (shouldAbortDownload(serverSupportsResume, isResume,
					outputFile, listener)) return;
				
				handleDownloadResponse(body, startingBytes,
					serverSupportsResume, outputFile, listener);
			}
		});
	}
	
	private boolean isRequestFailed(@NonNull Response response,
	                                @NonNull ProgressListener listener) {
		if (!response.isSuccessful()) {
			String errorMsg = "Server returned HTTP error: " + response.code();
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return true;
		}
		return false;
	}
	
	private boolean shouldAbortDownload(boolean serverSupportsResume,
	                                    boolean isResume,
	                                    @NonNull File outputFile,
	                                    @NonNull ProgressListener listener) {
		if (isResume && !serverSupportsResume) {
			logger.warning("Server does not support resuming. Restarting download.");
			if (outputFile.exists()) {
				outputFile.delete();
			}
		}
		
		if (outputFile.exists() && !outputFile.canWrite()) {
			String errorMsg = "Download failed: target file is not writable. " +
				"Try clearing app storage or granting storage permission.";
			
			logger.error(errorMsg);
			listener.onError(errorMsg);
			return true;
		}
		return false;
	}
	
	private void handleDownloadResponse(@NotNull ResponseBody body,
	                                    long startingBytes,
	                                    boolean serverSupportsResume,
	                                    @NonNull File outputFile,
	                                    @NonNull ProgressListener listener) {
		try (body) {
			processDownloadStream(body, outputFile, listener,
				startingBytes, serverSupportsResume);
			
		} catch (FileNotFoundException error) {
			String errorMsg = "Cannot write APK file — " +
				"storage permission may be denied or the file is protected.";
			logger.error(errorMsg);
			listener.onError(errorMsg);
			
		} catch (SecurityException error) {
			String errorMsg = "Security restriction: cannot write to download " +
				"location. Grant storage permission and try again.";
			logger.error(errorMsg);
			
			listener.onError(errorMsg);
		} catch (Exception error) {
			if (currentCall != null && currentCall.isCanceled()) {
				logger.debug("Download stream interrupted by cancellation.");
			} else {
				logger.error("Error writing file or hashing: " + error.getMessage());
				listener.onError(error.getMessage());
			}
		}
	}
	
	private void processDownloadStream(@NotNull ResponseBody body,
	                                   @NotNull File outputFile,
	                                   @NotNull ProgressListener listener,
	                                   long alreadyDownloadedBytes,
	                                   boolean appendToFile)
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
				if (currentCall != null && currentCall.isCanceled()) {
					logger.debug("Download cancelled during stream processing.");
					return;
				}
				outputStream.write(buffer, 0, bytesRead);
				digest.update(buffer, 0, bytesRead);
				currentDownloadedBytes += bytesRead;
				
				if (totalBytes > 0) {
					short percentage = (short) ((currentDownloadedBytes * 100L) / totalBytes);
					listener.onProgressUpdate(percentage, currentDownloadedBytes, totalBytes);
				}
			}
			outputStream.flush();
		}
		
		String hashHex = bytesToHex(digest.digest());
		listener.onDownloadComplete(outputFile, hashHex);
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
				listener.onProgressUpdate((short) 100, outputFile.length(), outputFile.length());
				listener.onDownloadComplete(outputFile, bytesToHex(digest.digest()));
			}
		} catch (Exception error) {
			logger.error("Failed to hash existing file: " + error.getMessage());
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
	
	public void stopDownload() {
		if (currentCall != null) {
			logger.debug("Stopping/Canceling current download call.");
			currentCall.cancel();
			currentCall = null;
		}
	}
	
	public interface ProgressListener {
		
		void onProgressUpdate(short percentage, long downloadedByte, long totalFileSize);
		void onDownloadComplete(File apkFile, String downloadedApkHash);
		void onError(String errorMessage);
	}
}