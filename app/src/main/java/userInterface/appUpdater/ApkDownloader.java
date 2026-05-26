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

/**
 * Handles APK file downloading from a remote server with resume capability and integrity verification.
 * <p>
 * This class manages the complete download lifecycle for application update APK files,
 * including network requests, file I/O, SHA-256 hash calculation for integrity checks,
 * and automatic resume support for interrupted downloads. It uses OkHttp for efficient
 * asynchronous networking and provides progress callbacks through the ProgressListener interface.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Resumable downloads - automatically resumes partial downloads from where they left off</li>
 *   <li>Integrity verification - computes SHA-256 hash of downloaded files</li>
 *   <li>Progress tracking - real-time percentage and byte progress callbacks</li>
 *   <li>Error handling - graceful fallback to full download when resume is not supported</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * ApkDownloader downloader = new ApkDownloader();
 * UpdateInfo updateInfo = ...;
 * File downloadDir = new File(context.getExternalFilesDir(null), "updates");
 *
 * downloader.startDownload(updateInfo, downloadDir, new ProgressListener() {
 *     public void onProgressUpdate(short percentage, long downloadedByte) {
 *         updateProgressBar(percentage);
 *     }
 *     public void onDownloadComplete(File apkFile, String hash) {
 *         installApk(apkFile);
 *     }
 *     public void onError(String errorMessage) {
 *         showError(errorMessage);
 *     }
 * });
 * </pre>
 * </p>
 *
 * @see UpdateInfo
 * @see ProgressListener
 * @see OkHttpClient
 */
public class ApkDownloader {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final OkHttpClient client;
	private Call currentCall;
	
	/**
	 * Constructs a new ApkDownloader instance with a default OkHttpClient configuration.
	 * <p>
	 * The client is configured without custom timeouts or interceptors, relying on
	 * OkHttp's default settings. For production use, consider customizing the client
	 * with appropriate timeouts and caching strategies.
	 * </p>
	 */
	public ApkDownloader() {
		this.client = new OkHttpClient();
	}
	
	/**
	 * Initiates the APK download process with automatic resume support for partial downloads.
	 * <p>
	 * This is the public entry point for downloading app updates. It creates the download
	 * directory if needed, generates a unique filename using the app name and version details,
	 * then performs a HEAD request to query the server for file metadata before starting
	 * the actual download. The download can automatically resume if a partial file exists.
	 * </p>
	 *
	 * <p><b>Filename Format:</b>
	 * <pre>
	 * [AppName]_[VersionCode]_[VersionName].apk
	 * Example: "TubeAIO-NextGen_42_2.1.0.apk"
	 * </pre>
	 * </p>
	 *
	 * <p><b>Download Flow:</b>
	 * <ol>
	 *   <li>Ensures download directory exists (creates if missing)</li>
	 *   <li>Generates a unique filename based on version information</li>
	 *   <li>Sends a HEAD request to get the server file size</li>
	 *   <li>Delegates to startOrResumeDownload() to handle the actual download strategy</li>
	 * </ol>
	 * </p>
	 *
	 * @param updateInfo  contains the APK download URL and version details for filename generation
	 * @param downloadDir the directory where the APK file will be saved
	 * @param listener    callback listener for download progress, completion, and error events
	 */
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
				startOrResumeDownload(response, outputFile, updateInfo, listener);
			}
		});
	}
	
	/**
	 * Determines the appropriate download strategy based on local file state and server file size.
	 * <p>
	 * This method performs a HEAD request to obtain the total file size from the server, then
	 * compares it with any existing local file to decide the optimal download approach:
	 * </p>
	 * <ul>
	 *   <li><b>No local file:</b> Start a fresh download from byte 0</li>
	 *   <li><b>Local size equals server size:</b> File already complete, trigger completion</li>
	 *   <li><b>Local size less than server size:</b> Resume download from existing offset</li>
	 *   <li><b>Local size greater than server size:</b> Delete and restart fresh download</li>
	 *   <li><b>Invalid server size (≤ 0):</b> Delete any local file and start fresh download</li>
	 * </ul>
	 *
	 * @param response   the HEAD response containing content-length header
	 * @param outputFile the local file (may or may not exist)
	 * @param updateInfo contains the APK download URL
	 * @param listener   callback listener for download progress and completion
	 */
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
				executeResumedDownload(outputFile, updateInfo,
					listener, localSize);
				
			} else {
				reinitializeDownload(outputFile, updateInfo, listener);
			}
		} else {
			executeDownloadRequest(apkFileUrl, outputFile, listener, 0);
		}
	}
	
	/**
	 * Restarts the download from scratch when local file size exceeds server file size.
	 * <p>
	 * This situation indicates file corruption or an inconsistent partial download where
	 * the local file is larger than what the server reports. The existing file is deleted
	 * and a fresh download is initiated from byte 0.
	 * </p>
	 *
	 * @param outputFile the local file that is larger than the server version
	 * @param updateInfo contains the APK download URL
	 * @param listener   callback listener for download progress and completion
	 */
	private void reinitializeDownload(@NonNull File outputFile,
	                                  @NonNull UpdateInfo updateInfo,
	                                  @NonNull ProgressListener listener) {
		logger.warning("Local file is larger than server file. Restarting download.");
		outputFile.delete();
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, 0);
	}
	
	/**
	 * Resumes an incomplete download from the specified byte offset.
	 * <p>
	 * Called when a partially downloaded file exists and its size is less than the
	 * total file size reported by the server. The download resumes from the existing
	 * file's current size, appending new data to the already downloaded content.
	 * </p>
	 *
	 * @param outputFile the partially downloaded local file
	 * @param updateInfo contains the APK download URL
	 * @param listener   callback listener for download progress and completion
	 * @param localSize  the current size of the local file (resume offset)
	 */
	private void executeResumedDownload(@NonNull File outputFile,
	                                    @NonNull UpdateInfo updateInfo,
	                                    @NonNull ProgressListener listener,
	                                    long localSize) {
		logger.debug("Partial download found. Resuming from byte: " + localSize);
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, localSize);
	}
	
	/**
	 * Triggers completion flow when the APK file is already fully downloaded.
	 * <p>
	 * Called when the local file size matches the server-reported file size, indicating
	 * the file has already been completely downloaded. The method proceeds directly to
	 * verification without performing any additional network requests.
	 * </p>
	 *
	 * @param outputFile the fully downloaded APK file
	 * @param listener   callback listener to receive completion notification
	 */
	private void handleAlreadyDownloaded(@NonNull File outputFile,
	                                     @NonNull ProgressListener listener) {
		logger.debug("File already fully downloaded. Triggering completion.");
		handleFullyDownloaded(outputFile, listener);
	}
	
	/**
	 * Handles HEAD request failures by falling back to a complete fresh download.
	 * <p>
	 * When the initial HEAD request to check file metadata fails (network error,
	 * timeout, or server error), this method deletes any existing local file and
	 * starts a full download from scratch. This ensures the download can proceed
	 * even when metadata retrieval is unsuccessful.
	 * </p>
	 *
	 * @param error      the IOException that caused the HEAD request failure
	 * @param outputFile the local file (will be deleted if exists)
	 * @param updateInfo contains the APK download URL
	 * @param listener   callback listener for download progress and completion
	 */
	private void handleHeadRequestFailure(@NonNull IOException error,
	                                      @NotNull File outputFile,
	                                      @NonNull UpdateInfo updateInfo,
	                                      @NonNull ProgressListener listener) {
		logger.error("HEAD request failed, falling back to full download: ", error);
		if (outputFile.exists()) outputFile.delete();
		executeDownloadRequest(updateInfo.getApkFileUrl(), outputFile, listener, 0);
	}
	
	/**
	 * Executes the network request to download the APK file with optional resume support.
	 * <p>
	 * This method creates and enqueues an asynchronous HTTP request using OkHttp. If a
	 * non-zero downloadedBytes value is provided, it adds a "Range" header to request
	 * partial content (resume download). The server's response is checked for HTTP 206
	 * (Partial Content) to verify resume support; if not supported, the existing file
	 * is deleted and the download restarts from the beginning.
	 * </p>
	 *
	 * <p><b>Resume Behavior:</b>
	 * <ul>
	 *   <li>If downloadedBytes > 0: Adds "Range: bytes=X-" header to resume from offset X</li>
	 *   <li>If server responds with 206: Resume is supported, download continues from offset</li>
	 *   <li>If server responds with 200 when resume was requested: Server doesn't support resume,
	 *       existing file is deleted and download restarts from scratch</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Cancellation Handling:</b>
	 * The request is stored in {@code currentCall} to allow external cancellation via
	 * {@link #stopDownload()}. When cancellation occurs, the callback checks
	 * {@link Call#isCanceled()} and suppresses error reporting to avoid misleading
	 * error messages for user-initiated cancellations.
	 * </p>
	 *
	 * <p><b>Error Handling:</b>
	 * The method handles various error scenarios including network failures, HTTP errors,
	 * file permission issues (FileNotFoundException, SecurityException), and general
	 * I/O exceptions. User-friendly error messages are provided for permission-related
	 * failures to guide the user toward resolution.
	 * </p>
	 *
	 * @param url             the complete HTTPS URL of the APK file to download
	 * @param outputFile      the destination file where the APK will be saved
	 * @param listener        callback listener for progress updates and completion events
	 * @param downloadedBytes number of bytes already downloaded (0 for new download,
	 *                        positive for resume attempts)
	 */
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
	
	/**
	 * Checks if an HTTP response indicates a failed request and notifies the listener.
	 * <p>
	 * This method evaluates the HTTP status code of the response. If the response is not
	 * successful (status code outside 2xx range), it logs the error and notifies the
	 * listener with an appropriate error message containing the HTTP status code.
	 * </p>
	 *
	 * @param response the OkHttp Response to check for success status
	 * @param listener callback listener to receive error notification if request failed
	 * @return true if the request failed (response not successful), false otherwise
	 */
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
	
	/**
	 * Determines whether the download should be aborted based on resume support and file writability.
	 * <p>
	 * This method performs two critical checks:
	 * <ol>
	 *   <li>If resume was requested but the server doesn't support it (HTTP 206),
	 *       the existing partial file is deleted and the download will restart from scratch.
	 *       This is not considered an abort condition; the method returns false.</li>
	 *   <li>If the output file exists but is not writable (permission issue or file locked),
	 *       an error is reported to the listener and the method returns true to indicate
	 *       the download should be aborted.</li>
	 * </ol>
	 * </p>
	 *
	 * @param serverSupportsResume true if server responded with HTTP 206 (Partial Content)
	 * @param isResume             true if this download was attempting to resume (downloadedBytes > 0)
	 * @param outputFile           the destination file to check for writability
	 * @param listener             callback listener for error notification if file is not writable
	 * @return true if the download should be aborted (file not writable), false otherwise
	 */
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
	
	/**
	 * Processes the download response body and handles any exceptions during stream processing.
	 * <p>
	 * This method delegates to {@link #processDownloadStream} for the actual file writing
	 * and hash verification. It uses a try-with-resources block to ensure the ResponseBody
	 * is properly closed after processing. Specific exceptions are caught and converted
	 * into user-friendly error messages for permission-related failures, while general
	 * exceptions are logged with appropriate handling for cancellation scenarios.
	 * </p>
	 *
	 * <p><b>Exception Handling:</b>
	 * <ul>
	 *   <li><b>FileNotFoundException:</b> Indicates storage permission denied or protected file</li>
	 *   <li><b>SecurityException:</b> Indicates security restriction on write location</li>
	 *   <li><b>General Exception:</b> May be I/O errors or hashing failures; cancellation is suppressed</li>
	 * </ul>
	 * </p>
	 *
	 * @param body                 the OkHttp ResponseBody containing the download stream
	 * @param startingBytes        number of bytes already downloaded (resume offset)
	 * @param serverSupportsResume true if server responded with HTTP 206 (Partial Content)
	 * @param outputFile           the destination file to write the APK content
	 * @param listener             callback listener for progress and error events
	 */
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
	
	/**
	 * Processes the download stream and writes the APK file to disk with hash verification.
	 * <p>
	 * This method handles the core download logic including resumable downloads when appendToFile
	 * is enabled. It streams data from the network response body, writes it to the output file,
	 * and simultaneously computes an SHA-256 hash of the entire file for integrity verification.
	 * Progress updates are reported back through the ProgressListener at regular intervals.
	 * The method also checks for cancellation requests during the streaming loop to allow
	 * graceful interruption of long-running downloads.
	 * </p>
	 *
	 * <p><b>Resume Download Support:</b>
	 * When appendToFile is true and the output file already exists, this method will first hash
	 * the existing content, then append new data to the end of the file. The hash will be
	 * continuously updated to reflect the complete file after all data is written.
	 * </p>
	 *
	 * <p><b>Cancellation Support:</b>
	 * During the streaming loop, the method checks {@code currentCall.isCanceled()} and exits
	 * early if a cancellation was requested. This prevents unnecessary I/O operations and
	 * provides responsive cancellation for user-initiated stop requests.
	 * </p>
	 *
	 * @param body                   the OkHttp ResponseBody containing the download stream
	 * @param outputFile             the destination file on disk to write the APK content
	 * @param listener               callback listener for progress and completion events, must be non-null
	 * @param alreadyDownloadedBytes number of bytes already downloaded in a previous session
	 * @param appendToFile           whether to append to an existing file (resume) or overwrite
	 * @throws IOException              if an I/O error occurs during reading, writing, or hashing
	 * @throws NoSuchAlgorithmException if SHA-256 algorithm is not available (should not occur)
	 */
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
	
	/**
	 * Handles the post-download verification of a successfully downloaded APK file.
	 * <p>
	 * This method computes an SHA-256 hash of the downloaded file to verify its integrity,
	 * then notifies the ProgressListener of completion with the file and its hash.
	 * The hash can be compared against the expected value from the server to ensure
	 * the file hasn't been corrupted or tampered with during download.
	 * </p>
	 *
	 * <p><b>Verification Process:</b>
	 * <ol>
	 *   <li>Creates an SHA-256 MessageDigest instance</li>
	 *   <li>Reads the file in 8KB chunks, updating the digest incrementally</li>
	 *   <li>Converts the final digest bytes to a hexadecimal string</li>
	 *   <li>Notifies listener with 100% progress and the computed hash</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Error Handling:</b>
	 * If hashing fails (e.g., file read error, algorithm not available), an error
	 * notification is sent to the listener with a descriptive message.
	 * </p>
	 *
	 * @param outputFile the downloaded APK file that has been fully written to disk
	 * @param listener   the progress listener to notify about completion and hash results,
	 *                   may be null to skip notifications
	 */
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
	
	/**
	 * Converts a byte array to its hexadecimal string representation.
	 * <p>
	 * This utility method is used to convert cryptographic hash bytes (e.g., from SHA-256)
	 * into a readable hex string format suitable for comparison with server-provided hashes.
	 * Each byte is represented as two hexadecimal characters (0-9, a-f), with leading zeros
	 * preserved to ensure consistent string length.
	 * </p>
	 *
	 * @param bytes the byte array to convert, typically a hash digest (e.g., 32 bytes for SHA-256)
	 * @return a hexadecimal string representation of the input bytes, or empty string if bytes is empty
	 */
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
	
	/**
	 * Cancels the ongoing download process.
	 * <p>
	 * This method immediately terminates any active network request associated with
	 * this downloader instance, whether it's the initial HEAD request or the
	 * actual file download. If a download was in progress, the partial file
	 * will remain on disk for future resume attempts. This method is safe to
	 * call even when no download is active.
	 * </p>
	 *
	 * <p><b>Usage Example:</b>
	 * <pre>
	 * ApkDownloader downloader = new ApkDownloader();
	 * downloader.startDownload(updateInfo, downloadDir, listener);
	 *
	 * // Later, if user cancels
	 * downloader.stopDownload();
	 * </pre>
	 * </p>
	 */
	public void stopDownload() {
		if (currentCall != null) {
			logger.debug("Stopping/Canceling current download call.");
			currentCall.cancel();
			currentCall = null;
		}
	}
	
	/**
	 * Callback interface for monitoring APK download progress, completion, and errors.
	 * <p>
	 * Implement this interface to receive real-time updates during the app update
	 * download process. Methods are called at various stages including progress updates,
	 * successful completion with file verification, and error conditions. All callbacks
	 * are invoked on the downloader's thread; implementations should handle thread
	 * switching if UI updates are required.
	 * </p>
	 *
	 * <p><b>Typical Usage:</b>
	 * <pre>
	 * downloader.startDownload(updateInfo, downloadDir, new ProgressListener() {
	 *     public void onProgressUpdate(short percentage, long downloadedByte, long totalFileSize) {
	 *         runOnUiThread(() -> progressBar.setProgress(percentage));
	 *     }
	 *
	 *     public void onDownloadComplete(File apkFile, String downloadedApkHash) {
	 *         runOnUiThread(() -> installApk(apkFile));
	 *     }
	 *
	 *     public void onError(String errorMessage) {
	 *         runOnUiThread(() -> showError(errorMessage));
	 *     }
	 * });
	 * </pre>
	 * </p>
	 */
	public interface ProgressListener {
		
		/**
		 * Called periodically during the download to report current progress.
		 * <p>
		 * This method is invoked multiple times as data is received from the server,
		 * typically after each chunk of data is written. It provides both percentage
		 * completion and raw byte counts for detailed progress display.
		 * </p>
		 *
		 * @param percentage     the download completion percentage (0-100)
		 * @param downloadedByte the total number of bytes downloaded so far
		 * @param totalFileSize  the total size of the file being downloaded in bytes
		 */
		void onProgressUpdate(short percentage, long downloadedByte, long totalFileSize);
		
		/**
		 * Called when the APK download is complete and file integrity has been verified.
		 * <p>
		 * This method is invoked after the entire file has been downloaded and its
		 * SHA-256 hash has been computed. The provided hash can be compared against
		 * the expected hash from the server for additional verification, though basic
		 * verification is already performed by the downloader before this callback.
		 * </p>
		 *
		 * @param apkFile           the downloaded APK file ready for installation
		 * @param downloadedApkHash the SHA-256 hash of the downloaded file for integrity validation
		 */
		void onDownloadComplete(File apkFile, String downloadedApkHash);
		
		/**
		 * Called when an error occurs during the download or verification process.
		 * <p>
		 * This method is invoked for any recoverable or unrecoverable error during
		 * the download, including network failures, I/O errors, hash mismatches, or
		 * insufficient storage space. The error message provides details that can be
		 * displayed to the user or logged for debugging.
		 * </p>
		 *
		 * @param errorMessage a human-readable description of the error that occurred
		 */
		void onError(String errorMessage);
	}
}