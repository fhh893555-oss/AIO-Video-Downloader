package userInterface.appUpdater;

import androidx.annotation.NonNull;

import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;

public final class AppUpdaterUtils extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	public static final String COLLECTION_NAME = "appUpdates";
	public static final String FIELD_ = "appUpdates";
	
	
	@NonNull @Override protected String getCollectionName() {
		return "";
	}
}
