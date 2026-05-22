package userInterface.appUpdater;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.File;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import userInterface.appUpdater.AppUpdaterUtils.UpdateInfo;

public class AppUpdaterViewModel extends ViewModel {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final ThreadTask<UpdateInfo, Void> updateCheckTask = new ThreadTask<>();
	private final MutableLiveData<UpdateInfo> updateAvailableLiveData = new MutableLiveData<>();
	private final MutableLiveData<DownloadStatus> downloadStatusLiveData = new MutableLiveData<>();
	
	/**
	 * Returns the LiveData that notifies when a new application update is available.
	 *
	 * @return Observable update information.
	 */
	public LiveData<UpdateInfo> getUpdateAvailableLiveData() {
		return updateAvailableLiveData;
	}
	
	/**
	 * Returns the LiveData that notifies about the APK download status.
	 *
	 * @return Observable download status.
	 */
	public LiveData<DownloadStatus> getDownloadStatusLiveData() {
		return downloadStatusLiveData;
	}
	
	/**
	 * Initiates an asynchronous check for application updates from the server.
	 *
	 * @param deviceId The unique identifier of the requesting device.
	 */
	public void checkForUpdates(@NonNull String deviceId) {
		logger.debug("Checking for app updates...");
		updateCheckTask.cancel();
		updateCheckTask.setBackgroundTask(progress -> {
			try {
				AppUpdaterUtils updater = new AppUpdaterUtils();
				UpdateInfo latest = updater.fetchLatestUpdateInfo(deviceId);
				if (AppUpdaterUtils.isUpdateAvailable(BaseApplication.AppContext, latest)) {
					return latest;
				}
			} catch (Exception e) {
				logger.error("Update check failed", e);
			}
			return null;
		});
		
		updateCheckTask.setResultTask(result -> {
			if (result != null) {
				logger.debug("New version available: " + result.getVersionName());
				updateAvailableLiveData.setValue(result);
			} else {
				logger.debug("App is up to date.");
			}
		});
		updateCheckTask.start();
	}
	
	/**
	 * Starts downloading the APK file from the provided UpdateInfo.
	 *
	 * @param updateInfo  The update information containing the download URL.
	 * @param downloadDir The directory where the APK should be saved.
	 */
	public void downloadUpdate(@NonNull UpdateInfo updateInfo, @NonNull File downloadDir) {
		logger.debug("Starting APK download: " + updateInfo.getApkFileUrl());
		downloadStatusLiveData.setValue(DownloadStatus.pending());
		
		ApkDownloader downloader = new ApkDownloader();
		downloader.startDownload(updateInfo, downloadDir, new ApkDownloader.ProgressListener() {
			@Override
			public void onProgressUpdate(short percentage, long downloadedByte) {
				downloadStatusLiveData.postValue(DownloadStatus.downloading(percentage));
			}
			
			@Override
			public void onDownloadComplete(File apkFile, String downloadedApkHash) {
				logger.debug("Download complete. Verifying hash...");
				downloadStatusLiveData.postValue(DownloadStatus.verifying());
				
				// Verify integrity
				String expectedHash = updateInfo.getApkFileHash();
				if (expectedHash != null && !expectedHash.equalsIgnoreCase(downloadedApkHash)) {
					logger.error("Hash mismatch! Expected: " + expectedHash + ", Got: " + downloadedApkHash);
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
		});
	}
	
	@Override
	protected void onCleared() {
		super.onCleared();
		updateCheckTask.cancel();
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
		 * time and cannot be modified afterwards, ensuring thread-safety and consistency.
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
