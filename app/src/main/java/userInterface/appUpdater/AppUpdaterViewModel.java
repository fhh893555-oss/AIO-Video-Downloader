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

public class AppUpdaterViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final ThreadTask<UpdateInfo, Void> downloadTask = new ThreadTask<>();
	
	private final ApkDownloader downloader = new ApkDownloader();
	private final MutableLiveData<DownloadStatus>
		downloadStatusLiveData = new MutableLiveData<>();
	
	public LiveData<DownloadStatus> getDownloadStatusLiveData() {
		return downloadStatusLiveData;
	}
	
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
	
	public void stopDownloadingAPK() {
		downloader.stopDownload();
		downloadTask.cancel();
	}
	
	private ProgressListener buildProgressListener(@NonNull UpdateInfo updateInfo) {
		return new ProgressListener() {
			
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
			
			@Override
			public void onError(String errorMessage) {
				ThreadTask.executeOnMainThread(() -> {
					logger.error("Download failed: " + errorMessage);
					downloadStatusLiveData.postValue(DownloadStatus.error(errorMessage));
				});
			}
		};
	}
	
	@Override
	protected void onCleared() {
		super.onCleared();
		downloader.stopDownload();
		downloadTask.cancel();
	}
	
	public static class DownloadStatus {
		
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
		
		public static DownloadStatus pending() {
			return new DownloadStatus(State.PENDING, (short) 0, 0L, 0L, null, null);
		}
		
		public static DownloadStatus downloading(short progress,
		                                         long downloadedByte,
		                                         long totalFileSize) {
			return new DownloadStatus(State.DOWNLOADING, progress, downloadedByte,
				totalFileSize, null, null);
		}
		
		public static DownloadStatus verifying() {
			return new DownloadStatus(State.VERIFYING, (short) 100, 100L,
				100L, null, null);
		}
		
		public static DownloadStatus completed(File file) {
			return new DownloadStatus(State.COMPLETED, (short) 100,
				file.length(), file.length(), file, null);
		}
		
		public static DownloadStatus error(String error) {
			return new DownloadStatus(State.ERROR, (short) 0, 0L, 0L,
				null, error);
		}
		
		public static DownloadStatus hashMismatch() {
			return new DownloadStatus(State.HASH_MISMATCH, (short) 0, 0L, 0L,
				null, "Hash verification failed.");
		}
		
		public State getState() {return state;}
		
		public short getProgress() {return progress;}
		
		public long getDownloadedByte() {return downloadedByte;}
		
		public long getTotalFileSize() {return totalFileSize;}
		
		public File getFile() {return file;}
		
		public String getError() {return error;}
	}
}
