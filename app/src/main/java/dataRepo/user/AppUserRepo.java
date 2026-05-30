package dataRepo.user;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.DeviceSignature;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataSubscription;

public final class AppUserRepo {
	private static final LoggerUtils logger = LoggerUtils.from(AppUserRepo.class);
	private static final Set<UserObserver> observers = new HashSet<>();
	
	private static final long DEFAULT_USER_ID = 1L;
	private static final AppUserCloud cloudClient = new AppUserCloud();
	private static final ThreadTask<String, String> serverSyncJob = new ThreadTask<>();
	private static volatile AppUser activeUser;
	private static Box<AppUser> userBox;
	private static DataSubscription subscription;
	private static volatile boolean isSyncInProgress = false;
	
	private AppUserRepo() {}
	
	public static void initialize(Box<AppUser> appUserBox) {
		userBox = appUserBox;
		AppUser user = userBox.get(DEFAULT_USER_ID);
		if (user == null) {
			user = new AppUser();
			user.id = DEFAULT_USER_ID;
			
			BaseApplication appContext = BaseApplication.AppContext;
			DeviceSignature deviceSignature = DeviceSignature.getInstance(appContext);
			user.userDeviceId = deviceSignature.generate();
			
			userBox.put(user);
		} else {
			user.lastSeenTimestamp = System.currentTimeMillis();
			userBox.put(user);
		}
		
		activeUser = user;
		observeChanges();
		syncWithServer();
		syncUserGeoData();
	}
	
	public static AppUser getUser() {
		return activeUser;
	}
	
	public static void save(AppUser user) {
		save(user, true);
	}
	
	public static void save(AppUser user, boolean syncToPocketBase) {
		if (userBox == null) {
			String message = "AppUserRepo not initialized";
			throw new IllegalStateException(message);
		}
		
		user.id = DEFAULT_USER_ID;
		activeUser = user;
		userBox.put(user);
		
		if (syncToPocketBase && user.userServerId != null && !user.userServerId.isEmpty()) {
			ThreadTask.executeInBackground(() -> {
				try {
					JSONObject pocketBasePayload = toPocketBasePayload(user);
					cloudClient.updateUser(user.userServerId, pocketBasePayload);
				} catch (Exception error) {
					logger.error("Sync to PocketBase failed", error);
				}
			});
		}
	}
	
	public static void updateUser(UpdateBlock block) {
		if (activeUser == null) return;
		block.apply(activeUser);
		save(activeUser);
	}
	
	private static void syncUserGeoData() {
		GeoDetails.fetch(geoDetails -> {
			if (geoDetails == null) return;
			
			AppUser appUser = AppUserRepo.getUser();
			AppConfigs appConfig = AppConfigsRepo.getConfig();
			if (appUser == null || appConfig == null) return;
			
			appUser.countryCode = geoDetails.countryCode;
			appUser.languageCode = appConfig.selectedLanguageCode;
			appUser.locationCity = geoDetails.locationCity;
			appUser.locationRegion = geoDetails.locationRegion;
			appUser.continent = geoDetails.continent;
			appUser.continentCode = geoDetails.continentCode;
			appUser.zipCode = geoDetails.zipCode;
			appUser.networkIsp = geoDetails.networkIsp;
			appUser.userIpAddress = geoDetails.userIpAddress;
			appUser.preferredCategories = "Trending,Featured,Music,Gaming,News,Playlist";
			appUser.lastSeenTimestamp = System.currentTimeMillis();
			userBox.put(appUser);
		}, AppConfigsRepo.getConfig());
	}
	
	private static void observeChanges() {
		try {
			if (userBox == null) return;
			try (Query<AppUser> query = userBox.query()
				.equal(AppUser_.id, DEFAULT_USER_ID)
				.build()) {
				
				subscription = query.subscribe().observer(data -> {
					if (data.isEmpty()) return;
					AppUser updated = data.get(0);
					activeUser = updated;
					notifyObservers(updated);
				});
			}
			
		} catch (Exception error) {
			logger.error("User observation failed", error);
		}
	}
	
	private static void notifyObservers(AppUser user) {
		for (UserObserver observer : observers) {
			try {
				observer.onUserChanged(user);
			} catch (Exception error) {
				logger.error("Observer error", error);
			}
		}
	}
	
	public static void registerObserver(UserObserver observer) {
		observers.add(observer);
	}
	
	public static void unregisterObserver(UserObserver observer) {
		observers.remove(observer);
	}
	
	public static void release() {
		if (subscription != null) {
			subscription.cancel();
			subscription = null;
		}
		observers.clear();
	}
	
	private static void syncWithServer() {
		if (isSyncInProgress) return;
		isSyncInProgress = true;
		
		serverSyncJob.cancel();
		serverSyncJob.setMaxExecutionTimeMs(20_000);
		serverSyncJob.setBackgroundTask(progressCallback -> {
			try {
				AppUser localUser = getUser();
				if (localUser == null) return "Local user not found";
				
				String userDeviceId = localUser.userDeviceId;
				logger.debug("Checking PocketBase user for device: " + userDeviceId);
				JSONObject serverUser = cloudClient.getUserByDeviceId(userDeviceId);
				
				if (serverUser == null || !serverUser.has("id")) {
					logger.debug("User not found: creating new record in server");
					createServerUser(localUser);
				} else {
					mergeServerUser(serverUser);
					logger.debug("User found: synced with server");
				}
			} catch (Exception error) {
				logger.error("User sync failed", error);
				return error.getMessage();
			} finally {
				isSyncInProgress = false;
			}
			return "success";
		});
		serverSyncJob.setResultTask(result -> isSyncInProgress = false);
		serverSyncJob.setErrorTask(error -> isSyncInProgress = false);
		serverSyncJob.start();
	}
	
	private static void createServerUser(AppUser user) {
		try {
			logger.debug("Creating PocketBase user");
			JSONObject response = cloudClient.createUser(toPocketBasePayload(user));
			if (response != null) {
				String serverId = response.optString("id");
				if (!serverId.isEmpty()) {
					logger.debug("Server user created: " + serverId);
					user.userServerId = serverId;
					save(user, false);
				}
			}
		} catch (Exception error) {
			logger.error("Creating server user failed", error);
		}
	}
	
	private static void mergeServerUser(JSONObject serverData) {
		try {
			AppUser localUser = getUser();
			if (localUser == null) return;
			
			logger.debug("Merging server user data");
			boolean changed = false;
			
			String serverId = serverData.optString("id");
			if (!serverId.isEmpty() && !serverId.equals(localUser.userServerId)) {
				localUser.userServerId = serverId;
				changed = true;
			}
			
			String nameField = AppUser.POCKETBASE_REMOTE_NAME_FIELD;
			String emailField = AppUser.POCKETBASE_REMOTE_EMAIL_FIELD;
			String isPremiumField = AppUser.POCKETBASE_REMOTE_IS_PREMIUM_FIELD;
			String hasReferralBonusField = AppUser.POCKETBASE_REMOTE_HAS_REFERRAL_BONUS_FIELD;
			String isBannedField = AppUser.POCKETBASE_REMOTE_IS_BANNED_FIELD;
			String referralsCountField = AppUser.POCKETBASE_REMOTE_TOTAL_REFERRALS_COUNT_FIELD;
			
			changed |= syncStringField(serverData, nameField, localUser.name, val -> localUser.name = val);
			changed |= syncStringField(serverData, emailField, localUser.email, val -> localUser.email = val);
			changed |= syncBooleanField(serverData, isPremiumField, localUser.isPremiumUser, val -> localUser.isPremiumUser = val);
			changed |= syncBooleanField(serverData, hasReferralBonusField, localUser.hasReferralBonus, val -> localUser.hasReferralBonus = val);
			changed |= syncBooleanField(serverData, isBannedField, localUser.isBanned, val -> localUser.isBanned = val);
			changed |= syncIntField(serverData, referralsCountField, localUser.totalReferralsCount, val -> localUser.totalReferralsCount = val);
			
			if (changed) {
				logger.debug("Local user updated from server");
				save(localUser, false);
			}
		} catch (Exception error) {
			logger.error("Server user merge failed", error);
		}
	}
	
	private static boolean syncStringField(JSONObject data, String field,
	                                       String current, Consumer<String> setter) {
		String serverValue = data.optString(field);
		if (!serverValue.isEmpty() && !serverValue.equals(current)) {
			setter.accept(serverValue);
			return true;
		}
		return false;
	}
	
	private static boolean syncBooleanField(JSONObject data,
	                                        String field, boolean current,
	                                        Consumer<Boolean> setter) {
		if (data.has(field)) {
			boolean serverValue = data.optBoolean(field);
			if (serverValue != current) {
				setter.accept(serverValue);
				return true;
			}
		}
		return false;
	}
	
	private static boolean syncIntField(JSONObject data, String field,
	                                    int current, Consumer<Integer> setter) {
		if (data.has(field)) {
			int serverValue = data.optInt(field);
			if (serverValue != current) {
				setter.accept(serverValue);
				return true;
			}
		}
		return false;
	}
	
	private static JSONObject toPocketBasePayload(AppUser user) {
		try {
			JSONObject json = new JSONObject();
			String deviceIdField = AppUser.POCKETBASE_REMOTE_DEVICE_ID_FIELD;
			String remoteNameField = AppUser.POCKETBASE_REMOTE_NAME_FIELD;
			String remoteEmailField = AppUser.POCKETBASE_REMOTE_EMAIL_FIELD;
			String isPremiumField = AppUser.POCKETBASE_REMOTE_IS_PREMIUM_FIELD;
			String hasReferralBonusField = AppUser.POCKETBASE_REMOTE_HAS_REFERRAL_BONUS_FIELD;
			String referralsCountField = AppUser.POCKETBASE_REMOTE_TOTAL_REFERRALS_COUNT_FIELD;
			String isBannedField = AppUser.POCKETBASE_REMOTE_IS_BANNED_FIELD;
			
			json.put(deviceIdField, user.userDeviceId);
			json.put(remoteNameField, user.name);
			json.put(remoteEmailField, user.email);
			json.put(isPremiumField, user.isPremiumUser);
			json.put(hasReferralBonusField, user.hasReferralBonus);
			json.put(referralsCountField, user.totalReferralsCount);
			json.put(isBannedField, user.isBanned);
			return json;
		} catch (Exception error) {
			logger.error("Failed to create PocketBase payload", error);
			return new JSONObject();
		}
	}
	
	public interface UpdateBlock {
		void apply(AppUser user);
	}
	
	public interface UserObserver {
		void onUserChanged(AppUser user);
	}
}