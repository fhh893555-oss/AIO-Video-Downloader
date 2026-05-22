package userInterface.appUpdater;

import androidx.annotation.NonNull;

import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;

public final class AppUpdaterUtils extends PocketBaseClient {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String COLLECTION_NAME = "appUpdates";
	public static final String FIELD_VERSION_NAME = "currentVersionName";
	public static final String FIELD_VERSION_CODE = "currentVersionCode";
	public static final String FIELD_APK_FILE = "currentApkFile";
	public static final String FIELD_WHATS_NEW_JSON = "whatsNewJSON";
	
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	
}
