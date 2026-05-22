package userInterface.appUpdater;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.io.File;
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
		
		Request request = new Request.Builder()
			.url(updateInfo.getApkFileUrl())
			.build();
		
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(@NotNull Call call, @NotNull IOException e) {
				logger.error("Network request failed: " + e.getMessage());
				listener.onError(e.getMessage());
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
				if (body == null) {
					String errorMsg = "Response body is null.";
					logger.error(errorMsg);
					if (listener != null) listener.onError(errorMsg);
					return;
				}
				
				try {
					processDownloadStream(body, outputFile, listener);
				} catch (Exception e) {
					logger.error("Error writing file or hashing: " + e.getMessage());
					listener.onError(e.getMessage());
				} finally {
					body.close();
				}
			}
		});
	}
	
	private void processDownloadStream(ResponseBody body, File outputFile, ProgressListener listener)
		throws IOException, NoSuchAlgorithmException {
		
		long totalBytes = body.contentLength();
		long downloadedBytes = 0;
		
		// We calculate the hash (SHA-256) on the fly as we read the file
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		
		try (InputStream inputStream = body.byteStream();
		     FileOutputStream outputStream = new FileOutputStream(outputFile)) {
			
			byte[] buffer = new byte[8192]; // 8KB buffer
			int bytesRead;
			
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				digest.update(buffer, 0, bytesRead);
				downloadedBytes += bytesRead;
				
				if (listener != null && totalBytes > 0) {
					short percentage = (short) ((downloadedBytes * 100L) / totalBytes);
					listener.onProgressUpdate(percentage, downloadedBytes);
				}
			}
			outputStream.flush();
		}
		
		if (listener != null) {
			String hashHex = bytesToHex(digest.digest());
			listener.onDownloadComplete(outputFile, hashHex);
		}
	}
	
	/**
	 * Converts a byte array to a hexadecimal string.
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
	
	public interface ProgressListener {
		void onProgressUpdate(short percentage, long downloadedByte);
		void onDownloadComplete(File apkFile, String downloadedApkHash);
		void onError(String errorMessage);
	}
}