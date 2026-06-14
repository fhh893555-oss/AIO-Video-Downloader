package userInterface.appUpdater;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import org.jetbrains.annotations.NotNull;

import java.io.File;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import userInterface.appUpdater.ApkDownloader.ProgressListener;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;

/**
 * ViewModel for managing APK download operations in the app updater screen.
 * This class handles the download lifecycle, exposes download status via
 * LiveData, and ensures proper cleanup when the associated activity is destroyed.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Manages the {@link ApkDownloader} instance for network operations.</li>
 * <li>Exposes a LiveData {@link DownloadStatus} stream for UI observation.</li>
 * <li>Provides methods to start and stop APK downloads.</li>
 * <li>Bridges download callbacks to the LiveData via a ProgressListener.</li>
 * <li>Performs SHA-256 hash verification after download completion.</li>
 * <li>Automatically cancels ongoing downloads when the ViewModel is cleared.</li>
 * </ul>
 *
 * <p>The ViewModel survives configuration changes (screen rotations), ensuring
 * that downloads continue uninterrupted and progress is not lost.
 * </p>
 *
 * @see ViewModel
 * @see DownloadStatus
 * @see ApkDownloader
 * @see ProgressListener
 */
public class AppUpdaterViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final ThreadTask<UpdateInfo, Void> downloadTask = new ThreadTask<>();
	
	private final ApkDownloader downloader = new ApkDownloader();
	private final MutableLiveData<DownloadStatus>
		downloadStatusLiveData = new MutableLiveData<>();
	
	/**
	 * Returns the LiveData that holds the current download status. UI components
	 * can observe this LiveData to receive real-time updates about download
	 * progress, completion, errors, and verification results.
	 *
	 * @return The LiveData containing the current {@link DownloadStatus}.
	 */
	public LiveData<DownloadStatus> getDownloadStatusLiveData() {
		return downloadStatusLiveData;
	}
	
	/**
	 * Initiates the APK download process asynchronously. This method cancels any
	 * existing download task, binds the task lifecycle to the provided LifecycleOwner,
	 * and starts a background download operation. Progress updates are posted to
	 * the LiveData via the {@link ProgressListener} bridge.
	 *
	 * <p><strong>Execution flow:</strong>
	 * <ol>
	 * <li>Cancels any previously running download task.</li>
	 * <li>Binds the task to the lifecycle owner (automatically cancels on destroy).</li>
	 * <li>Posts a PENDING status to the LiveData on the main thread.</li>
	 * <li>Starts the download using
	 * {@link ApkDownloader#startDownload(UpdateInfo, File, ProgressListener)}.</li>
	 * </ol>
	 *
	 * @param updateInfo    The update information containing the APK URL and hash.
	 * @param downloadDir   The directory where the APK file will be saved.
	 * @param lifecycleOwner The lifecycle owner (typically the activity) for task binding.
	 */
	public void downloadUpdatedAPK(@NonNull UpdateInfo updateInfo,
	                               @NonNull File downloadDir,
	                               @NotNull LifecycleOwner lifecycleOwner) {
		downloadTask.cancel();
		downloadTask.observeLifecycle(lifecycleOwner);
		downloadTask.setBackgroundTask(callback -> {
			logger.debug("Starting APK download: " + updateInfo.getApkFileUrl());
			ThreadTask.executeOnMainThread(() ->
				downloadStatusLiveData.setValue(DownloadStatus.pending()));
			ProgressListener progressListener = buildProgressListener(updateInfo);
			downloader.startDownload(updateInfo, downloadDir, progressListener);
			return null;
		});
		
		downloadTask.start();
	}
	
	/**
	 * Stops the active APK download operation. This method cancels both the
	 * background download task and the underlying network request via
	 * {@link ApkDownloader#stopDownload()}. It should be called when the user
	 * cancels the download or when the ViewModel is being cleared.
	 *
	 * @see ApkDownloader#stopDownload()
	 * @see #onCleared()
	 */
	public void stopDownloadingAPK() {
		downloader.stopDownload();
		downloadTask.cancel();
	}
	
	/**
	 * Creates and returns a ProgressListener that bridges download events to the
	 * ViewModel's LiveData. The listener captures progress updates, completion
	 * events, and errors, then posts the appropriate DownloadStatus to the
	 * LiveData for observation by the UI layer.
	 *
	 * <p>The listener ensures all callbacks are executed on the main thread via
	 * {@link ThreadTask#executeOnMainThread(ThreadTask.UITask)}  to allow safe LiveData
	 * updates from background download threads.
	 *
	 * @param updateInfo The UpdateInfo object containing the expected APK hash
	 *                   for integrity verification after download completes.
	 * @return A ProgressListener implementation that updates downloadStatusLiveData.
	 */
	private ProgressListener buildProgressListener(@NonNull UpdateInfo updateInfo) {
		return new ProgressListener() {
			
			/**
			 * Called periodically during the download to report progress. This method
			 * updates the LiveData with a DOWNLOADING status containing the current
			 * progress percentage and byte counts.
			 *
			 * @param percentage     The download progress as a percentage (0-100).
			 * @param downloadedByte The number of bytes downloaded so far.
			 * @param totalFileSize  The total file size in bytes.
			 */
			@Override
			public void onProgressUpdate(short percentage,
			                             long downloadedByte,
			                             long totalFileSize) {
				ThreadTask.executeOnMainThread(() -> {
					logger.debug("Download progress info: percentage:" + percentage +
						" download byte:" + downloadedByte);
					
					downloadStatusLiveData.postValue(DownloadStatus
						.downloading(percentage, downloadedByte, totalFileSize));
				});
			}
			
			/**
			 * Called when the download completes successfully. This method updates the
			 * LiveData to a VERIFYING state, then computes the SHA-256 hash of the
			 * downloaded file and compares it against the expected hash from the server.
			 * If hashes match, COMPLETED status is posted; otherwise, HASH_MISMATCH.
			 *
			 * @param apkFile           The downloaded APK file.
			 * @param downloadedApkHash The SHA-256 hash of the downloaded file.
			 */
			@Override
			public void onDownloadComplete(File apkFile, String downloadedApkHash) {
				ThreadTask.executeOnMainThread(() -> {
					logger.debug("Download complete. Verifying hash...");
					downloadStatusLiveData.postValue(DownloadStatus.verifying());
					
					String expectedHash = updateInfo.getApkFileHash();
					if (expectedHash != null &&
						!expectedHash.equalsIgnoreCase(downloadedApkHash)) {
						logger.error("Hash mismatch! Expected: " +
							expectedHash + ", Got: " + downloadedApkHash);
						downloadStatusLiveData.postValue(DownloadStatus.hashMismatch());
						
					} else {
						logger.debug("Hash verification successful.");
						downloadStatusLiveData.postValue(DownloadStatus.completed(apkFile));
					}
				});
			}
			
			/**
			 * Called when an error occurs during the download process. This method posts
			 * an ERROR status with the provided error message to the LiveData, which the
			 * UI can observe and display to the user.
			 *
			 * @param errorMessage A description of the error that occurred (e.g., network
			 *                     failure, file write error, server rejection).
			 */
			@Override
			public void onError(String errorMessage) {
				ThreadTask.executeOnMainThread(() -> {
					logger.error("Download failed: " + errorMessage);
					downloadStatusLiveData.postValue(DownloadStatus.error(errorMessage));
				});
			}
		};
	}
	
	/**
	 * Called when the ViewModel is cleared (e.g., when the associated activity
	 * is finished or destroyed). This method performs cleanup of resources to
	 * prevent memory leaks and cancel any ongoing operations.
	 *
	 * <p><strong>Cleanup performed:</strong>
	 * <ul>
	 * <li>Calls {@link ApkDownloader#stopDownload()} to cancel any active
	 *     network request and release associated resources.</li>
	 * <li>Cancels the {@code downloadTask} background task to prevent further
	 *     processing after the ViewModel is no longer in use.</li>
	 * <li>Delegates to the superclass to complete standard cleanup.</li>
	 * </ul>
	 *
	 * <p>This ensures that downloads do not continue in the background after
	 * the user has navigated away from the updater screen.
	 *
	 * @see androidx.lifecycle.ViewModel#onCleared()
	 * @see ApkDownloader#stopDownload()
	 */
	@Override
	protected void onCleared() {
		super.onCleared();
		downloader.stopDownload();
		downloadTask.cancel();
	}
	
	/**
	 * Immutable data class representing the current status of an APK download
	 * operation. This class contains all relevant information about the download
	 * progress, state, file reference, and any errors that may have occurred.
	 *
	 * <p><strong>Fields:</strong>
	 * <ul>
	 * <li>{@code state} – Current download state (PENDING, DOWNLOADING, VERIFYING,
	 *     COMPLETED, ERROR, HASH_MISMATCH).</li>
	 * <li>{@code progress} – Download progress as a percentage (0-100).</li>
	 * <li>{@code downloadedByte} – Number of bytes downloaded so far.</li>
	 * <li>{@code totalFileSize} – Total file size in bytes.</li>
	 * <li>{@code file} – Reference to the downloaded file (only when COMPLETED).</li>
	 * <li>{@code error} – Error message (only when ERROR or HASH_MISMATCH).</li>
	 * </ul>
	 *
	 * <p>Instances are created via static factory methods:
	 * {@link #pending()}, {@link #downloading(short, long, long)},
	 * {@link #verifying()}, {@link #completed(File)}, {@link #error(String)},
	 * and {@link #hashMismatch()}.
	 *
	 * <p>This class is used by {@link AppUpdaterViewModel} to emit LiveData
	 * updates to the UI layer, allowing the activity to observe and react
	 * to download progress and completion events.
	 *
	 * @see State
	 * @see #getState()
	 * @see #getProgress()
	 * @see #getDownloadedByte()
	 * @see #getTotalFileSize()
	 * @see #getFile()
	 * @see #getError()
	 */
	public static class DownloadStatus {
		
		/**
		 * Defines the possible states of an APK download operation. These states
		 * are used by {@link DownloadStatus} to communicate the current phase of
		 * the download lifecycle to the UI layer.
		 *
		 * <p><strong>State descriptions:</strong>
		 * <ul>
		 * <li>{@link #PENDING} – Download requested but not yet started.</li>
		 * <li>{@link #DOWNLOADING} – Actively downloading with progress updates.</li>
		 * <li>{@link #VERIFYING} – Download complete, performing hash verification.</li>
		 * <li>{@link #COMPLETED} – Download and verification successful.</li>
		 * <li>{@link #ERROR} – Download failed due to network or I/O error.</li>
		 * <li>{@link #HASH_MISMATCH} – File downloaded but SHA-256 hash does not match.</li>
		 * </ul>
		 *
		 * @see DownloadStatus
		 * @see DownloadStatus#getState()
		 */
		public enum State {
			COMPLETED, DOWNLOADING, ERROR,
			HASH_MISMATCH, PENDING, VERIFYING
		}
		
		private final State state;
		private final short progress;
		private final long downloadedByte;
		private final long totalFileSize;
		private final File file;
		private final String error;
		
		/**
		 * Private constructor for creating a DownloadStatus instance. This constructor
		 * is only called by the static factory methods to ensure consistent state
		 * creation. All fields are immutable after construction.
		 *
		 * @param state          The current download state (PENDING, DOWNLOADING,
		 *                       VERIFYING, COMPLETED, ERROR, HASH_MISMATCH).
		 * @param progress       Download progress as a percentage (0-100).
		 * @param downloadedByte Number of bytes downloaded so far.
		 * @param totalFileSize  Total file size in bytes.
		 * @param file           The downloaded file reference (only for COMPLETED state).
		 * @param error          Error message (only for ERROR or HASH_MISMATCH states).
		 */
		private DownloadStatus(State state, short progress,
		                       long downloadedByte, long totalFileSize,
		                       File file, String error) {
			this.state = state;
			this.progress = progress;
			this.downloadedByte = downloadedByte;
			this.totalFileSize = totalFileSize;
			this.file = file;
			this.error = error;
		}
		
		/**
		 * Creates a pending download status indicating that the download has been
		 * requested but has not yet started. Progress is set to 0%, and no file
		 * or error is associated. This status is used as the initial state before
		 * network requests begin.
		 *
		 * @return A DownloadStatus instance with state PENDING.
		 */
		public static DownloadStatus pending() {
			return new DownloadStatus(State.PENDING, (short) 0, 0L, 0L, null, null);
		}
		
		/**
		 * Creates a downloading status with the current progress metrics. This status
		 * is used during active download to report progress to the UI. The file
		 * reference remains null until the download is complete.
		 *
		 * @param progress        The download progress as a percentage (0-100).
		 * @param downloadedByte  The number of bytes downloaded so far.
		 * @param totalFileSize   The total file size in bytes.
		 * @return A DownloadStatus instance with state DOWNLOADING.
		 */
		public static DownloadStatus downloading(short progress,
		                                         long downloadedByte,
		                                         long totalFileSize) {
			return new DownloadStatus(State.DOWNLOADING, progress, downloadedByte,
				totalFileSize, null, null);
		}
		
		/**
		 * Creates a verifying status indicating that the downloaded file is being
		 * validated (e.g., SHA-256 hash verification in progress). Progress is set
		 * to 100% to indicate that the download phase is complete, and the status
		 * is used to show a "Verifying..." message in the UI.
		 *
		 * @return A DownloadStatus instance with state VERIFYING.
		 */
		public static DownloadStatus verifying() {
			return new DownloadStatus(State.VERIFYING, (short) 100, 100L,
				100L, null, null);
		}
		
		/**
		 * Creates a completed download status with the successfully downloaded file.
		 * The progress is set to 100%, and both downloaded and total bytes are set
		 * to the file's length. This status is used to signal that the APK has been
		 * fully downloaded and is ready for installation.
		 *
		 * @param file The successfully downloaded APK file.
		 * @return A DownloadStatus instance with state COMPLETED.
		 */
		public static DownloadStatus completed(File file) {
			return new DownloadStatus(State.COMPLETED, (short) 100,
				file.length(), file.length(), file, null);
		}
		
		/**
		 * Creates an error download status with the specified error message.
		 * The progress is set to 0%, and file reference is null. This status is used
		 * to signal that the download failed due to network issues, permission errors,
		 * or other recoverable problems.
		 *
		 * @param error The error message describing the failure.
		 * @return A DownloadStatus instance with state ERROR.
		 */
		public static DownloadStatus error(String error) {
			return new DownloadStatus(State.ERROR, (short) 0, 0L, 0L,
				null, error);
		}
		
		/**
		 * Creates a hash mismatch download status indicating that the downloaded file's
		 * SHA-256 hash does not match the expected value. This status is used when
		 * integrity verification fails, suggesting a corrupted download or tampering.
		 *
		 * <p>The error message is set to "Hash verification failed."
		 *
		 * @return A DownloadStatus instance with state HASH_MISMATCH.
		 */
		public static DownloadStatus hashMismatch() {
			return new DownloadStatus(State.HASH_MISMATCH, (short) 0, 0L, 0L,
				null, "Hash verification failed.");
		}
		
		/**
		 * Returns the current download state (e.g., IN_PROGRESS, COMPLETED, FAILED).
		 *
		 * @return The {@link State} of the download operation.
		 */
		public State getState() {return state;}
		
		/**
		 * Returns the download progress as a percentage (0-100).
		 *
		 * @return The progress percentage.
		 */
		public short getProgress() {return progress;}
		
		/**
		 * Returns the number of bytes that have been downloaded so far.
		 *
		 * @return The downloaded byte count.
		 */
		public long getDownloadedByte() {return downloadedByte;}
		
		/**
		 * Returns the total file size in bytes of the download.
		 *
		 * @return The total file size in bytes, or 0 if unknown.
		 */
		public long getTotalFileSize() {return totalFileSize;}
		
		/**
		 * Returns the downloaded file reference upon successful completion.
		 *
		 * @return The {@link File} object representing the downloaded APK, or null.
		 */
		public File getFile() {return file;}
		
		/**
		 * Returns the error message if the download failed.
		 *
		 * @return The error message string, or null if no error occurred.
		 */
		public String getError() {return error;}
	}
}
