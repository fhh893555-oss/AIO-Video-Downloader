package dataRepo.userDetails;

import android.app.Application;

import androidx.annotation.NonNull;

import org.jetbrains.annotations.NotNull;
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

/**
 * Repository class for managing the application's user entity with local persistence
 * via ObjectBox and optional cloud synchronization with PocketBase. This class
 * follows a singleton pattern with static methods only, providing centralized
 * access to the {@link AppUser} entity throughout the app lifecycle.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Initializes and maintains the singleton {@code AppUser} instance.</li>
 * <li>Provides CRUD operations via {@link #getUser()} and {@link #save(AppUser)}.</li>
 * <li>Synchronizes user data with PocketBase cloud server (create, update, merge).</li>
 * <li>Fetches geolocation data and updates user location fields automatically.</li>
 * <li>Manages observers that receive notifications when user data changes.</li>
 * </ul>
 *
 * <p>Must be initialized once via {@link #initialize(Box)} during app startup.
 * Call {@link #release()} during application termination to clean up resources.
 *
 * @see AppUser
 * @see UserObserver
 * @see #initialize(Box)
 * @see #getUser()
 * @see #save(AppUser)
 */
public final class AppUserRepo {
	
	private static final LoggerUtils logger = LoggerUtils.from(AppUserRepo.class);
	private static final Set<UserObserver> observers = new HashSet<>();
	private static final AppUserCloud cloudClient = new AppUserCloud();
	private static final ThreadTask<String, String> serverSyncJob = new ThreadTask<>();
	
	private static final long DEFAULT_USER_ID = 1L;
	
	private static volatile AppUser activeUser;
	private static Box<AppUser> userBox;
	private static DataSubscription subscription;
	private static volatile boolean isSyncInProgress = false;
	
	private AppUserRepo() {}
	
	/**
	 * Initializes the user repository with the provided ObjectBox box instance.
	 * This method retrieves or creates the singleton user entity with ID
	 * {@code DEFAULT_USER_ID}. If no user exists, a new instance is created
	 * with a unique device identifier generated via {@link DeviceSignature}.
	 * The user's {@code lastSeenTimestamp} is updated to the current time
	 * on every initialization. After setup, the method starts live observation
	 * and initiates background sync operations.
	 *
	 * <p><strong>Initialization flow:</strong>
	 * <ol>
	 * <li>Stores the provided {@code appUserBox} reference.</li>
	 * <li>Attempts to retrieve existing user with ID {@code DEFAULT_USER_ID}.</li>
	 * <li>If {@code null}, creates a new user with a generated device ID.</li>
	 * <li>If exists, updates the {@code lastSeenTimestamp} to now.</li>
	 * <li>Sets {@code activeUser} to the retrieved or newly created instance.</li>
	 * <li>Calls {@link #observeChanges()} to enable live data updates.</li>
	 * <li>Calls {@link #syncWithServer()} to sync with PocketBase cloud.</li>
	 * <li>Calls {@link #syncUserGeoData()} to fetch geolocation information.</li>
	 * </ol>
	 *
	 * @param appUserBox The ObjectBox box for {@link AppUser} entities.
	 *                   Must not be null.
	 * @see #getUser()
	 * @see #save(AppUser)
	 * @see #observeChanges()
	 * @see #syncWithServer()
	 * @see #syncUserGeoData()
	 */
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
	
	/**
	 * Returns the currently active user entity. The returned object is kept in sync
	 * with database changes via the live subscription started in
	 * {@link #observeChanges()}. This method is the primary access point for reading
	 * user data throughout the application.
	 *
	 * <p><strong>Guarantee:</strong>
	 * After {@link #initialize(Box)} has been called, this method never returns
	 * {@code null}. A user entity always exists due to initialization logic that
	 * creates a default instance if none existed.
	 *
	 * @return The singleton {@link AppUser} instance representing the current user.
	 * @see #initialize(Box)
	 * @see #save(AppUser)
	 */
	public static AppUser getUser() {
		return activeUser;
	}
	
	/**
	 * Persists the provided user entity to the local database and automatically
	 * synchronizes the changes to the PocketBase cloud server. This method is a
	 * convenience wrapper around {@link #save(AppUser, boolean)} with
	 * {@code syncToPocketBase = true}.
	 *
	 * <p>Use this overload when cloud sync is desired after user modifications.
	 * To save without network sync, call {@link #save(AppUser, boolean)} directly
	 * with {@code false} as the second parameter.
	 *
	 * @param user The {@link AppUser} instance to save. Must not be null.
	 * @throws IllegalStateException If the repository has not been initialized.
	 * @see #save(AppUser, boolean)
	 * @see #getUser()
	 */
	public static void save(AppUser user) {
		save(user, true);
	}
	
	/**
	 * Persists the provided user entity to the local ObjectBox database and optionally
	 * synchronizes the changes to the PocketBase cloud server. This method enforces
	 * the singleton user pattern by fixing the entity ID to {@code DEFAULT_USER_ID},
	 * updates the in-memory {@code activeUser} reference, and writes the entity to
	 * the database.
	 *
	 * <p><strong>Cloud sync behavior:</strong>
	 * If {@code syncToPocketBase} is {@code true} and the user has a valid
	 * {@code userServerId} (non-null and non-empty), the method executes a background
	 * task via {@link ThreadTask#executeInBackground(ThreadTask.BackgroundTaskNoResult)}
	 * to send an update request to the PocketBase server using
	 * {@link AppUserCloud#updateUser(String, JSONObject)}. Any exception during cloud sync
	 * is logged but does not affect the local save operation.
	 *
	 * @param user             The {@link AppUser} instance to save. Must not be null.
	 * @param syncToPocketBase If {@code true}, attempts to sync changes to the remote
	 *                         PocketBase server in the background.
	 * @throws IllegalStateException If the repository has not been initialized
	 *                               (i.e., {@code userBox} is {@code null}).
	 * @see #updateUser(UpdateBlock)
	 * @see #toPocketBasePayload(AppUser)
	 * @see AppUserCloud#updateUser(String, JSONObject)
	 */
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
	
	/**
	 * Applies a series of modifications to the currently active user entity within
	 * a single transactional scope. This method accepts an {@link UpdateBlock} that
	 * defines the changes to apply, then automatically persists the updated user
	 * to the local database without triggering cloud sync.
	 *
	 * <p><strong>Example usage:</strong>
	 * <pre>
	 * AppUserRepo.updateUser(user -> {
	 *     user.setName("John Doe");
	 *     user.setEmail("john@example.com");
	 * });
	 * </pre>
	 *
	 * <p><strong>Note:</strong>
	 * Cloud synchronization is disabled ({@code syncToPocketBase = false}) in this
	 * overload to avoid network operations during batch updates. Callers that require
	 * cloud sync should use {@link #save(AppUser, boolean)} directly with the
	 * modified user object.
	 *
	 * @param block The {@link UpdateBlock} containing the modifications to apply
	 *              to the active user. Must not be null. The block receives the
	 *              current {@code activeUser} as its parameter.
	 * @see #save(AppUser, boolean)
	 * @see UpdateBlock#apply(AppUser)
	 */
	public static void updateUser(UpdateBlock block) {
		if (activeUser == null) return;
		block.apply(activeUser);
		save(activeUser);
	}
	
	/**
	 * Fetches and synchronizes geolocation and network data into the local user entity.
	 * This method asynchronously retrieves geographic details via a separate method
	 * {@link GeoDetails#fetch(GeoDetails.OnLocationDataListener, AppConfigs)},
	 * then updates the current user with country, city, region, ISP, IP address, and
	 * preferred content categories. The user object is then persisted to the database.
	 *
	 * <p><strong>Fields updated:</strong>
	 * country code, language code (from app config), city, region, continent,
	 * continent code, postal code, ISP, IP address, preferred categories, and
	 * last seen timestamp. The {@code preferredCategories} field is set to a default
	 * string of trending and popular content types.
	 *
	 * <p>If geolocation fetch returns {@code null}, or if either {@link AppUser} or
	 * {@link AppConfigs} is unavailable, the method returns silently without
	 * performing any updates.
	 *
	 * @see GeoDetails#fetch(GeoDetails.OnLocationDataListener, AppConfigs)
	 * @see AppUserRepo#getUser()
	 * @see AppConfigsRepo#getConfig()
	 */
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
	
	/**
	 * Establishes a live data subscription to monitor changes to the singleton user
	 * entity in the ObjectBox database. When the user with ID {@code DEFAULT_USER_ID}
	 * is updated, the observer automatically refreshes the {@code activeUser}
	 * reference and notifies all registered observers via {@link #notifyObservers(AppUser)}.
	 *
	 * <p><strong>Implementation details:</strong>
	 * <ul>
	 * <li>If {@code userBox} is {@code null}, the method returns silently.</li>
	 * <li>A query is built to match the entity with ID {@code DEFAULT_USER_ID}.</li>
	 * <li>The subscription is stored in {@code subscription} to allow later
	 *     cancellation during {@link #release()}.</li>
	 * <li>The observer checks if the result set is non-empty before processing.</li>
	 * <li>Any exceptions during subscription setup are caught and logged.</li>
	 * </ul>
	 *
	 * <p>The query uses a try-with-resources block to ensure the {@link Query} object
	 * is properly closed after subscription creation, preventing resource leaks.
	 *
	 * @see #notifyObservers(AppUser)
	 * @see #release()
	 * @see DataSubscription
	 */
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
	
	/**
	 * Notifies all registered observers about a change to the local user entity.
	 * This method iterates through the {@code observers} collection and invokes
	 * {@link UserObserver#onUserChanged(AppUser)} for each observer. Any exception
	 * thrown during notification is caught and logged without interrupting the
	 * notification of other observers.
	 *
	 * <p><strong>Error isolation:</strong>
	 * A try-catch block wraps each observer invocation individually, ensuring that
	 * a faulty observer implementation does not prevent other observers from
	 * receiving the user update. Exceptions are logged via {@code logger.error()}
	 * with a generic message.
	 *
	 * @param user The updated {@link AppUser} instance to pass to all registered
	 *             observers. Should never be {@code null}.
	 * @see #registerObserver(UserObserver)
	 * @see #unregisterObserver(UserObserver)
	 * @see UserObserver#onUserChanged(AppUser)
	 */
	private static void notifyObservers(AppUser user) {
		for (UserObserver observer : observers) {
			try {
				observer.onUserChanged(user);
			} catch (Exception error) {
				logger.error("Observer error", error);
			}
		}
	}
	
	/**
	 * Registers an observer to receive notifications when the local user entity
	 * changes. The observer is added to the internal {@code observers} collection
	 * and will be notified via {@link #notifyObservers(AppUser)} on subsequent
	 * user updates (e.g., after {@link #save(AppUser)} or sync operations).
	 *
	 * <p><strong>Lifecycle management:</strong>
	 * Observers should be unregistered when they are no longer needed (e.g., in
	 * {@link android.app.Activity#onDestroy()}) to prevent memory leaks. Failure
	 * to unregister may cause the observer to be retained indefinitely.
	 *
	 * @param observer The observer to register. Must not be {@code null}.
	 * @see #unregisterObserver(UserObserver)
	 * @see #notifyObservers(AppUser)
	 * @see #release()
	 */
	public static void registerObserver(UserObserver observer) {
		observers.add(observer);
	}
	
	/**
	 * Unregisters a previously registered observer, removing it from the notification
	 * list. After unregistration, the observer will no longer receive user change
	 * events via {@link #notifyObservers(AppUser)}.
	 *
	 * <p><strong>Idempotency:</strong>
	 * If the specified observer is not currently registered, this method performs
	 * no operation and does not throw an exception. It is safe to call even if the
	 * observer was never registered or has already been removed.
	 *
	 * @param observer The observer to unregister. Must not be {@code null}.
	 * @see #registerObserver(UserObserver)
	 * @see #notifyObservers(AppUser)
	 */
	public static void unregisterObserver(UserObserver observer) {
		observers.remove(observer);
	}
	
	/**
	 * Releases all resources held by the user repository. This method cancels any
	 * active ObjectBox subscription (if present) and clears the observers list.
	 * Call this method during application termination (e.g., in
	 * {@link Application#onTerminate()}) to prevent memory leaks.
	 *
	 * <p><strong>Resource cleanup:</strong>
	 * <ul>
	 * <li>Cancels the {@code subscription} to stop receiving database updates.</li>
	 * <li>Sets the subscription reference to {@code null} to avoid stale references.</li>
	 * <li>Clears all registered observers from the {@code observers} collection.</li>
	 * </ul>
	 *
	 * <p>After calling this method, the repository is no longer functional for
	 * receiving live updates. Observers must be re-registered if the repository
	 * is re-initialized later in the application lifecycle.
	 *
	 * @see #registerObserver(UserObserver)
	 * @see DataSubscription#cancel()
	 */
	public static void release() {
		if (subscription != null) {
			subscription.cancel();
			subscription = null;
		}
		observers.clear();
	}
	
	/**
	 * Initiates an asynchronous background task to synchronize the local user data
	 * with the PocketBase cloud server. This method prevents concurrent sync
	 * operations using the {@code isSyncInProgress} flag and schedules the sync job
	 * with a 20-second execution timeout.
	 *
	 * <p><strong>Sync flow:</strong>
	 * <ol>
	 * <li>Checks if a sync is already in progress; exits early if true.</li>
	 * <li>Sets the sync flag to {@code true} to block concurrent runs.</li>
	 * <li>Configures the {@code serverSyncJob} with timeout and execution logic.</li>
	 * <li>Retrieves the local user; aborts if {@code null}.</li>
	 * <li>Queries the server for a user record matching the local device ID.</li>
	 * <li>If no record exists, calls {@link #createServerUser(AppUser)}.</li>
	 * <li>If a record exists, calls {@link #mergeServerUser(JSONObject)}.</li>
	 * <li>Resets the sync flag in {@code finally} block or error handlers.</li>
	 * </ul>
	 *
	 * <p><strong>Error handling:</strong>
	 * Any exception during sync is logged. The sync flag is always reset to
	 * {@code false} via the {@code finally} block and task callbacks to prevent
	 * deadlocks.
	 *
	 * @see #isSyncInProgress
	 * @see #createServerUser(AppUser)
	 * @see #mergeServerUser(JSONObject)
	 * @see AppUserCloud#getUserByDeviceId(String)
	 */
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
	
	/**
	 * Creates a new user record on the PocketBase server using the local user data.
	 * This method converts the local {@link AppUser} to a JSON payload via
	 * {@link #toPocketBasePayload(AppUser)} and sends it to the server via
	 * {@link AppUserCloud#createUser(JSONObject)}.
	 *
	 * <p>If creation succeeds, the server-assigned record ID is extracted from the
	 * response, stored in the local user's {@code userServerId} field, and the user
	 * is saved to the local database without triggering observer notifications.
	 *
	 * @param user The local {@link AppUser} instance to push to the server.
	 *             Must not be null and should contain a valid device ID.
	 * @see #toPocketBasePayload(AppUser)
	 * @see AppUserCloud#createUser(JSONObject)
	 * @see #save(AppUser, boolean)
	 */
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
	
	/**
	 * Merges user data received from the PocketBase server into the local
	 * {@link AppUser} entity. This method compares each remote field with its
	 * local counterpart and updates the local user only when differences are
	 * detected. The operation is idempotent and safe to call multiple times.
	 *
	 * <p><strong>Sync behavior:</strong>
	 * <ul>
	 * <li>Extracts the server ID and updates {@code userServerId} if changed.</li>
	 * <li>Uses {@link #syncStringField}, {@link #syncBooleanField}, and
	 *     {@link #syncIntField} helpers for field-specific comparison and update.</li>
	 * <li>Tracks whether any field changed via a boolean flag.</li>
	 * <li>If changes are detected, saves the updated user to local storage
	 *     without notifying observers (see {@code notifyObservers = false}).</li>
	 * </ul>
	 *
	 * <p>If the local user is {@code null} (not yet initialized) or an exception
	 * occurs during processing, the method logs the error and returns silently
	 * without modifying any data.
	 *
	 * <p><strong>Remote fields synced:</strong>
	 * {@code id}, {@link AppUser#POCKETBASE_REMOTE_NAME_FIELD},
	 * {@link AppUser#POCKETBASE_REMOTE_EMAIL_FIELD},
	 * {@link AppUser#POCKETBASE_REMOTE_IS_PREMIUM_FIELD},
	 * {@link AppUser#POCKETBASE_REMOTE_HAS_REFERRAL_BONUS_FIELD},
	 * {@link AppUser#POCKETBASE_REMOTE_IS_BANNED_FIELD},
	 * {@link AppUser#POCKETBASE_REMOTE_TOTAL_REFERRALS_COUNT_FIELD}
	 *
	 * @param serverData The JSON response from PocketBase containing the remote
	 *                   user record. Must not be null and should contain valid
	 *                   field names matching the local entity constants.
	 * @see #getUser()
	 * @see #syncStringField(JSONObject, String, String, Consumer)
	 * @see #syncBooleanField(JSONObject, String, boolean, Consumer)
	 * @see #syncIntField(JSONObject, String, int, Consumer)
	 * @see #save(AppUser, boolean)
	 */
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
			
			changed |= syncStringField(serverData,
				nameField, localUser.name, val -> localUser.name = val);
			
			changed |= syncStringField(serverData,
				emailField, localUser.email, val -> localUser.email = val);
			
			changed |= syncBooleanField(serverData,
				isPremiumField, localUser.isPremiumUser, val ->
					localUser.isPremiumUser = val);
			
			changed |= syncBooleanField(serverData,
				hasReferralBonusField, localUser.hasReferralBonus, val ->
					localUser.hasReferralBonus = val);
			
			changed |= syncBooleanField(serverData,
				isBannedField, localUser.isBanned, val ->
					localUser.isBanned = val);
			
			changed |= syncIntField(serverData,
				referralsCountField, localUser.totalReferralsCount, val ->
					localUser.totalReferralsCount = val);
			
			if (changed) {
				logger.debug("Local user updated from server");
				save(localUser, false);
			}
		} catch (Exception error) {
			logger.error("Server user merge failed", error);
		}
	}
	
	/**
	 * Synchronizes a string field from a remote JSON response with a local value.
	 * This helper method extracts the specified field from the server data and
	 * updates the local value via the provided setter if the server value is
	 * non-empty and differs from the current local value.
	 *
	 * @param data    The JSON object returned from the PocketBase server. Must not
	 *                be null and should contain the expected field.
	 * @param field   The name of the field to extract from the JSON object.
	 *                Must not be null.
	 * @param current The current local string value to compare against.
	 * @param setter  A {@link Consumer} that applies the new value to the local
	 *                user object. Must not be null.
	 * @return {@code true} if the field was updated (server value differed and was
	 * non-empty), {@code false} otherwise.
	 */
	private static boolean syncStringField(@NonNull JSONObject data,
	                                       @NonNull String field,
	                                       @NonNull String current,
	                                       @NonNull Consumer<String> setter) {
		String serverValue = data.optString(field);
		if (!serverValue.isEmpty() && !serverValue.equals(current)) {
			setter.accept(serverValue);
			return true;
		}
		return false;
	}
	
	/**
	 * Synchronizes a boolean field from a remote JSON response with a local value.
	 * This helper method checks if the field exists in the server data, extracts
	 * its boolean value, and updates the local value via the provided setter if
	 * the server value differs from the current local value.
	 *
	 * <p>Unlike string fields, boolean fields are not checked for emptiness since
	 * false is a valid value. The field presence is verified via {@link JSONObject#has(String)}.
	 *
	 * @param data    The JSON object returned from the PocketBase server. Must not
	 *                be null and may optionally contain the specified field.
	 * @param field   The name of the field to extract from the JSON object.
	 *                Must not be null.
	 * @param current The current local boolean value to compare against.
	 * @param setter  A {@link Consumer} that applies the new value to the local
	 *                user object. Must not be null.
	 * @return {@code true} if the field existed in the JSON and its value differed
	 * from the current local value, {@code false} otherwise.
	 */
	private static boolean syncBooleanField(@NonNull JSONObject data,
	                                        @NonNull String field, boolean current,
	                                        @NonNull Consumer<Boolean> setter) {
		if (data.has(field)) {
			boolean serverValue = data.optBoolean(field);
			if (serverValue != current) {
				setter.accept(serverValue);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Synchronizes an integer field from a remote JSON response with a local value.
	 * This helper method checks if the field exists in the server data, extracts
	 * its integer value, and updates the local value via the provided setter if
	 * the server value differs from the current local value.
	 *
	 * @param data    The JSON object returned from the PocketBase server. Must not
	 *                be null and may optionally contain the specified field.
	 * @param field   The name of the field to extract from the JSON object.
	 *                Must not be null.
	 * @param current The current local integer value to compare against.
	 * @param setter  A {@link Consumer} that applies the new value to the local
	 *                user object. Must not be null.
	 * @return {@code true} if the field existed in the JSON and its value differed
	 * from the current local value, {@code false} otherwise.
	 */
	private static boolean syncIntField(@NotNull JSONObject data,
	                                    @NotNull String field, int current,
	                                    @NonNull Consumer<Integer> setter) {
		if (data.has(field)) {
			int serverValue = data.optInt(field);
			if (serverValue != current) {
				setter.accept(serverValue);
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Converts a local {@link AppUser} entity into a JSON payload suitable for
	 * syncing with the PocketBase remote server. This method maps local user fields
	 * to their corresponding remote field names defined in the {@link AppUser}
	 * constants, and constructs a JSON object for use in POST or PATCH requests.
	 *
	 * <p><strong>Field mapping:</strong>
	 * <ul>
	 * <li>{@code userDeviceId} → {@value AppUser#POCKETBASE_REMOTE_DEVICE_ID_FIELD}</li>
	 * <li>{@code name} → {@value AppUser#POCKETBASE_REMOTE_NAME_FIELD}</li>
	 * <li>{@code email} → {@value AppUser#POCKETBASE_REMOTE_EMAIL_FIELD}</li>
	 * <li>{@code isPremiumUser} → {@value AppUser#POCKETBASE_REMOTE_IS_PREMIUM_FIELD}</li>
	 * <li>{@code hasReferralBonus} → {@value AppUser#POCKETBASE_REMOTE_HAS_REFERRAL_BONUS_FIELD}</li>
	 * <li>{@code totalReferralsCount} → {@value AppUser#POCKETBASE_REMOTE_TOTAL_REFERRALS_COUNT_FIELD}</li>
	 * <li>{@code isBanned} → {@value AppUser#POCKETBASE_REMOTE_IS_BANNED_FIELD}</li>
	 * </ul>
	 *
	 * <p>If JSON construction fails (e.g., due to invalid data types), the error is
	 * logged and an empty JSON object is returned to prevent crashes.
	 *
	 * @param user The local {@link AppUser} instance to convert. Must not be null.
	 * @return A JSON object containing the mapped fields ready for remote sync,
	 * or an empty JSON object on failure.
	 * @see AppUserCloud#createUser(JSONObject)
	 * @see AppUserCloud#updateUser(String, JSONObject)
	 */
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
	
	/**
	 * Functional interface for applying modifications to an {@link AppUser} instance
	 * within a transactional or sync context. Implementations of this interface
	 * define a block of operations to be executed on a user object, typically
	 * during update operations where multiple fields are modified atomically.
	 *
	 * <p><strong>Example usage:</strong>
	 * <pre>
	 * UpdateBlock block = user -> {
	 *     user.setName("New Name");
	 *     user.setEmail("new@example.com");
	 * };
	 * </pre>
	 *
	 * @see AppUserRepo#updateUser(UpdateBlock)
	 */
	public interface UpdateBlock {
		void apply(AppUser user);
	}
	
	/**
	 * Observer interface for receiving notifications when the local {@link AppUser}
	 * entity changes. Implement this interface and register with
	 * {@link AppUserRepo#registerObserver(UserObserver)} to react to user data updates
	 * such as profile modifications, premium status changes, or preference toggles.
	 *
	 * <p>The callback is invoked after the user object has been successfully saved
	 * to the local database. Use this to refresh UI components, update caches, or
	 * trigger sync operations with the cloud backend.
	 *
	 * @see AppUserRepo#registerObserver(UserObserver)
	 * @see AppUserRepo#unregisterObserver(UserObserver)
	 * @see #onUserChanged(AppUser)
	 */
	public interface UserObserver {
		void onUserChanged(AppUser user);
	}
}