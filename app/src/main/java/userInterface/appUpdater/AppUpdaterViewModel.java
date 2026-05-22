package userInterface.appUpdater;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;

/**
 * ViewModel that orchestrates APK update downloads and exposes download state to UI components.
 * <p>
 * This ViewModel serves as the bridge between the download logic ({@link ApkDownloader}) and
 * the UI layer (typically an Activity or Fragment). It manages background download tasks,
 * survives configuration changes (screen rotations, keyboard visibility changes, etc.),
 * and provides observable download status updates through {@link LiveData}.
 * </p>
 *
 * <p><b>Core Responsibilities:</b>
 * <ul>
 *   <li><b>Download Lifecycle Management:</b> Starts new downloads, automatically cancels
 *       previous downloads before starting a new one, and cleans up resources when the
 *       ViewModel is cleared.</li>
 *   <li><b>Status Exposure:</b> Exposes a {@link LiveData<DownloadStatus>} that UI components
 *       can observe to receive real-time updates about pending, downloading, verifying,
 *       completed, error, and hash mismatch states.</li>
 *   <li><b>Integrity Verification:</b> Performs SHA-256 hash validation on downloaded files,
 *       comparing the computed hash against the expected value from {@link UpdateInfo}.</li>
 *   <li><b>Thread Management:</b> Uses {@link ThreadTask} to execute downloads in the
 *       background, preventing UI blocking while ensuring callbacks return to the main thread.</li>
 * </ul>
 * </p>
 *
 * <p><b>Resource Cleanup:</b>
 * The ViewModel automatically cancels the ongoing download task in {@link #onCleared()},
 * which is called when the associated UI controller is permanently destroyed. This prevents
 * memory leaks and avoids unnecessary background operations after the ViewModel is no longer needed.
 * </p>
 *
 * <p><b>Thread Safety Note:</b>
 * The {@link MutableLiveData#postValue(Object)} method is used for all status updates from
 * background threads, ensuring thread-safe communication with the main thread observers.
 * </p>
 *
 * @see ViewModel
 * @see ApkDownloader
 * @see DownloadStatus
 * @see UpdateInfo
 * @see ThreadTask
 */
public class AppUpdaterViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final ThreadTask<UpdateInfo, Void> downloadTask = new ThreadTask<>();
	private final MutableLiveData<DownloadStatus> downloadStatusLiveData = new MutableLiveData<>();
	
	/**
	 * Returns the LiveData object for observing download status updates.
	 * <p>
	 * UI components can observe this LiveData to receive real-time updates about the
	 * download progress, including pending, downloading, verifying, completed, error,
	 * and hash mismatch states. The observer will receive the latest status immediately
	 * upon subscription and then each time the status changes.
	 * </p>
	 *
	 * @return LiveData containing the current DownloadStatus of the APK update
	 */
	public LiveData<DownloadStatus> getDownloadStatusLiveData() {
		return downloadStatusLiveData;
	}
	
	/**
	 * Initiates or restarts the APK update download process.
	 * <p>
	 * This method cancels any ongoing download task before starting a new one. It wraps
	 * the download operation in a background task, sets the initial status to PENDING,
	 * and delegates the actual download to ApkDownloader. The download supports automatic
	 * resumption of partial downloads and includes SHA-256 hash verification to ensure
	 * file integrity. Progress and completion updates are propagated through the
	 * downloadStatusLiveData via the ProgressListener.
	 * </p>
	 *
	 * <p><b>Behavior:</b>
	 * <ul>
	 *   <li>Cancels any previously running download task</li>
	 *   <li>Creates a new background task for the download operation</li>
	 *   <li>Sets initial LiveData status to PENDING</li>
	 *   <li>Delegates to ApkDownloader for actual network operations</li>
	 *   <li>Uses buildProgressListener() to handle callbacks</li>
	 *   <li>Starts the background task asynchronously</li>
	 * </ul>
	 * </p>
	 *
	 * @param updateInfo  contains the APK download URL and expected hash for verification
	 * @param downloadDir the directory where the APK file will be saved
	 */
	public void downloadUpdate(@NonNull UpdateInfo updateInfo, @NonNull File downloadDir) {
		downloadTask.cancel();
		downloadTask.setBackgroundTask(callback -> {
			logger.debug("Starting APK download: " + updateInfo.getApkFileUrl());
			downloadStatusLiveData.setValue(DownloadStatus.pending());
			
			ApkDownloader downloader = new ApkDownloader();
			downloader.startDownload(updateInfo, downloadDir, buildProgressListener(updateInfo));
			return null;
		});
		
		downloadTask.start();
	}
	
	/**
	 * Creates and returns a ProgressListener for monitoring APK download progress and completion.
	 * <p>
	 * This factory method constructs a ProgressListener that bridges the ApkDownloader's
	 * asynchronous callbacks to the ViewModel's LiveData, allowing UI components to observe
	 * download status updates. The listener handles progress tracking, hash verification,
	 * and error reporting, automatically updating the downloadStatusLiveData accordingly.
	 * </p>
	 *
	 * <p><b>Callback Handling:</b>
	 * <ul>
	 *   <li><b>onProgressUpdate:</b> Posts DOWNLOADING status with current percentage</li>
	 *   <li><b>onDownloadComplete:</b> Posts VERIFYING status, validates SHA-256 hash,
	 *       then posts either COMPLETED or HASH_MISMATCH based on verification result</li>
	 *   <li><b>onError:</b> Posts ERROR status with the error message</li>
	 * </ul>
	 * </p>
	 *
	 * @param updateInfo contains the expected SHA-256 hash for file integrity verification
	 * @return a ProgressListener implementation that updates downloadStatusLiveData
	 */
	private ApkDownloader.ProgressListener buildProgressListener(@NonNull UpdateInfo updateInfo) {
		return new ApkDownloader.ProgressListener() {
			@Override
			public void onProgressUpdate(short percentage, long downloadedByte) {
				downloadStatusLiveData.postValue(DownloadStatus.downloading(percentage));
			}
			
			@Override
			public void onDownloadComplete(File apkFile, String downloadedApkHash) {
				logger.debug("Download complete. Verifying hash...");
				downloadStatusLiveData.postValue(DownloadStatus.verifying());
				
				String expectedHash = updateInfo.getApkFileHash();
				if (expectedHash != null && !expectedHash.equalsIgnoreCase(downloadedApkHash)) {
					logger.error("Hash mismatch! Expected: " +
						expectedHash + ", Got: " + downloadedApkHash);
					
					downloadStatusLiveData.postValue(DownloadStatus.hashMismatch());
				} else {
					logger.debug("Hash verification successful.");
					downloadStatusLiveData.postValue(DownloadStatus.completed(apkFile));
				}
			}
			
			@Override
			public void onError(String errorMessage) {
				logger.error("Download failed: " + errorMessage);
				downloadStatusLiveData.postValue(DownloadStatus.error(errorMessage));
			}
		};
	}
	
	/**
	 * Cleans up resources when the ViewModel is cleared.
	 * <p>
	 * This lifecycle method is called when the ViewModel is no longer used and will be destroyed.
	 * It cancels any ongoing background update check tasks to prevent memory leaks and avoid
	 * unnecessary network operations after the ViewModel is no longer needed.
	 * </p>
	 */
	@Override
	protected void onCleared() {
		super.onCleared();
		downloadTask.cancel();
	}
	
	/**
	 * Represents the current state and progress of an APK download operation.
	 * <p>
	 * This immutable data class encapsulates all relevant information about a download's
	 * status, including its current state (PENDING, DOWNLOADING, COMPLETED, etc.),
	 * progress percentage, the downloaded file (if completed), and any error messages
	 * (if an error occurred). Factory methods are provided for creating instances
	 * representing each possible state.
	 */
	public static class DownloadStatus {
		public enum State {PENDING, DOWNLOADING, COMPLETED, ERROR, VERIFYING, HASH_MISMATCH}
		
		private final State state;
		private final short progress;
		private final File file;
		private final String error;
		
		/**
		 * Private constructor for creating DownloadStatus instances.
		 * <p>
		 * This constructor is only accessible within the class and is used by the public
		 * factory methods to create immutable status objects. All fields are set at creation
		 * time and cannot be modified afterward, ensuring thread-safety and consistency.
		 * </p>
		 *
		 * @param state    the current download state (PENDING, DOWNLOADING, etc.)
		 * @param progress the completion percentage (0-100)
		 * @param file     the downloaded APK file reference (null until completed)
		 * @param error    the error message if an error occurred (null otherwise)
		 */
		private DownloadStatus(State state, short progress, File file, String error) {
			this.state = state;
			this.progress = progress;
			this.file = file;
			this.error = error;
		}
		
		/**
		 * Creates a status indicating the download is waiting to start.
		 * <p>
		 * This is the initial state when a download is requested but has not yet begun.
		 * Progress is 0% and no file or error information is available.
		 * </p>
		 *
		 * @return a DownloadStatus instance with PENDING state
		 */
		public static DownloadStatus pending() {
			return new DownloadStatus(State.PENDING, (short) 0, null, null);
		}
		
		/**
		 * Creates a status indicating the download is actively in progress.
		 * <p>
		 * This state is updated periodically as data is received from the server.
		 * The progress parameter should reflect the percentage of bytes downloaded
		 * relative to the total file size reported by the server.
		 * </p>
		 *
		 * @param progress the current download completion percentage (0-100)
		 * @return a DownloadStatus instance with DOWNLOADING state and specified progress
		 */
		public static DownloadStatus downloading(short progress) {
			return new DownloadStatus(State.DOWNLOADING,
				progress, null, null);
		}
		
		/**
		 * Creates a status indicating the downloaded file is undergoing integrity verification.
		 * <p>
		 * This state occurs immediately after the download completes, before the file is
		 * reported as successfully downloaded. During this phase, the SHA-256 hash of the
		 * file is computed and compared against the expected hash from the server.
		 * </p>
		 *
		 * @return a DownloadStatus instance with VERIFYING state and 100% progress
		 */
		public static DownloadStatus verifying() {
			return new DownloadStatus(State.VERIFYING, (short) 100, null, null);
		}
		
		/**
		 * Creates a status indicating the download has completed successfully.
		 * <p>
		 * This state is reached when the file has been fully downloaded and has passed
		 * integrity verification. The provided File reference points to the verified APK
		 * file, which is ready for installation. Progress is reported as 100%.
		 * </p>
		 *
		 * @param file the downloaded APK file that passed integrity verification
		 * @return a DownloadStatus instance with COMPLETED state and the verified file reference
		 */
		public static DownloadStatus completed(File file) {
			return new DownloadStatus(State.COMPLETED, (short) 100,
				file, null);
		}
		
		/**
		 * Creates a status instance representing a download failure with a custom error message.
		 * <p>
		 * This factory method is called when an unrecoverable error occurs during the download
		 * process, such as network failures, I/O errors, or server-side issues. The resulting
		 * status contains zero progress and no file reference, only the error description.
		 * </p>
		 *
		 * @param error a human-readable description explaining the cause of the download failure
		 * @return a DownloadStatus instance configured with ERROR state and the provided error message
		 */
		public static DownloadStatus error(String error) {
			return new DownloadStatus(State.ERROR, (short) 0, null,
				error);
		}
		
		/**
		 * Creates a status instance indicating the downloaded file failed integrity verification.
		 * <p>
		 * This status is used when the SHA-256 hash computed from the downloaded APK file does
		 * not match the expected hash value provided by the server. This typically indicates
		 * file corruption during transfer, incomplete download, or potential tampering. The
		 * download should be retried or the user should be notified to download manually.
		 * </p>
		 *
		 * @return a DownloadStatus instance configured with HASH_MISMATCH state and a default error message
		 */
		public static DownloadStatus hashMismatch() {
			return new DownloadStatus(State.HASH_MISMATCH, (short) 0, null,
				"Hash verification failed.");
		}
		
		/**
		 * Retrieves the current state of the download operation.
		 * <p>
		 * The returned state indicates what phase the download is in: pending, actively
		 * downloading, completed, verifying integrity, error, or hash mismatch. This can
		 * be used to update UI elements like status messages or progress indicators.
		 * </p>
		 *
		 * @return the State enum value representing the current download status
		 */
		public State getState() {
			return state;
		}
		
		/**
		 * Returns the download completion percentage.
		 * <p>
		 * This value ranges from 0 to 100 and represents how much of the APK file has been
		 * successfully downloaded. For states like ERROR, HASH_MISMATCH, and PENDING, the
		 * progress will be 0. For COMPLETED and VERIFYING states, progress will be 100.
		 * </p>
		 *
		 * @return a short value between 0 and 100 indicating the download progress percentage
		 */
		public short getProgress() {
			return progress;
		}
		
		/**
		 * Returns the downloaded APK file reference when the download is complete.
		 * <p>
		 * This method returns the File object only when the download has successfully
		 * completed and passed integrity verification (COMPLETED state). For all other
		 * states, it returns null. The returned file can be used to initiate installation.
		 * </p>
		 *
		 * @return the File object pointing to the downloaded and verified APK, or null if not available
		 */
		public File getFile() {
			return file;
		}
		
		/**
		 * Returns the error message associated with a failed download.
		 * <p>
		 * This method provides a human-readable explanation of what went wrong during
		 * the download process. It only contains meaningful data when the state is ERROR
		 * or HASH_MISMATCH. For successful downloads or operations still in progress,
		 * it returns null.
		 * </p>
		 *
		 * @return a descriptive error message string, or null if no error has occurred
		 */
		public String getError() {
			return error;
		}
	}
}
