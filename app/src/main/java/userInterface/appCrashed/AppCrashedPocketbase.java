package userInterface.appCrashed;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import coreUtils.library.process.LoggerUtils;
import dataRepo.manager.PocketBaseClient;

public class AppCrashedPocketbase extends PocketBaseClient {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private static final String COLLECTION_NAME = "appCrashes";
	private static final String FILED_DEVICE_ID = "deviceId";
	private static final String FIELD_ANDROID_VERSION = "androidVersion";
	private static final String FIELD_APP_VERSION = "appVersion";
	private static final String FIELD_USER_COUNTRY = "userCountry";
	private static final String FIELD_STACKTRACE = "stackStrace";
	private static final String FIELD_DETAILED_INFO = "detailedInfo";
	
	@NonNull @Override protected String getCollectionName() {
		return COLLECTION_NAME;
	}
	
	public JSONObject sendCrashInfoToServer(AppCrashedInfo crashInfo) throws JSONException {
		JSONObject payload = new JSONObject();
		payload.put(FILED_DEVICE_ID, crashInfo.getDetailedInfo());
		payload.put(FIELD_ANDROID_VERSION, crashInfo.getAndroidVersion());
		payload.put(FIELD_APP_VERSION, crashInfo.getApplicationVersion());
		payload.put(FIELD_USER_COUNTRY, crashInfo.getUserCountry());
		payload.put(FIELD_STACKTRACE, crashInfo.getStackStraceInfo());
		payload.put(FIELD_DETAILED_INFO, crashInfo.getDetailedInfo());
		return post(payload);
	}
}
