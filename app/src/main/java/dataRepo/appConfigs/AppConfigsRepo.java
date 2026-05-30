package dataRepo.appConfigs;

import android.app.Activity;

import java.util.HashSet;
import java.util.Set;

import coreUtils.library.process.LoggerUtils;
import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataSubscription;

/**
 * Repository class for managing the application's persistent configuration data.
 * This class follows a singleton pattern with static methods only, providing
 * centralized access to the {@link AppConfigs} entity stored in ObjectBox.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Initializes and maintains the singleton {@code AppConfigs} instance.</li>
 * <li>Provides CRUD operations via {@link #getConfig()} and {@link #save(AppConfigs)}.</li>
 * <li>Establishes a live database subscription to sync in-memory config changes.</li>
 * <li>Manages observers that receive notifications when configuration changes.</li>
 * </ul>
 *
 * <p>Must be initialized exactly once via {@link #initialize(Box)} during app
 * startup. Components can react to config changes by implementing
 * {@link AppConfigsObserver} and registering via {@link #registerObserver(AppConfigsObserver)}.
 * Call {@link #release()} during application termination to cancel the database
 * subscription and clear all observers, preventing memory leaks.
 *
 * @see AppConfigs
 * @see AppConfigsObserver
 * @see #initialize(Box)
 * @see #getConfig()
 * @see #save(AppConfigs)
 * @see #release()
 */
public class AppConfigsRepo {
	
	private static final LoggerUtils logger = LoggerUtils.from(AppConfigsRepo.class);
	
	private static final long APP_CONFIG_ID = 1L;
	private static final Set<AppConfigsObserver> observers = new HashSet<>();
	private static DataSubscription configSubscription;
	private static volatile AppConfigs activeConfig;
	private static Box<AppConfigs> appConfigObjectBox;
	
	private AppConfigsRepo() {}
	
	/**
	 * Initializes the configuration repository with the provided ObjectBox box instance.
	 * This method retrieves the singleton configuration entity using the fixed
	 * {@code APP_CONFIG_ID} (typically {@code 1}). If no configuration exists, a new
	 * instance is created with default values and persisted to the database. The
	 * retrieved or newly created config is stored in {@code activeConfig} and a
	 * live subscription is started via {@link #observeChanges()}.
	 *
	 * <p><strong>Initialization flow:</strong>
	 * <ol>
	 * <li>Stores the provided {@code appConfigObjectBox} reference.</li>
	 * <li>Attempts to retrieve existing config with ID {@code APP_CONFIG_ID}.</li>
	 * <li>If {@code null}, creates a new {@link AppConfigs} instance and inserts it.</li>
	 * <li>Sets {@code activeConfig} to the retrieved or newly created instance.</li>
	 * <li>Subscribes to database changes to keep {@code activeConfig} synchronized.</li>
	 * </ol>
	 *
	 * <p><strong>Thread safety:</strong>
	 * This method should be called once during application startup, typically from
	 * a repository initialization method or application class, before any config
	 * read or write operations are performed.
	 *
	 * @param appConfigObjectBox The ObjectBox box for {@link AppConfigs} entities.
	 *                           Must not be {@code null}.
	 * @throws IllegalStateException If called again after initialization (indirectly)
	 *                               through other methods that require initialization.
	 * @see #getConfig()
	 * @see #save(AppConfigs)
	 * @see #observeChanges()
	 */
	public static void initialize(Box<AppConfigs> appConfigObjectBox) {
		AppConfigsRepo.appConfigObjectBox = appConfigObjectBox;
		AppConfigs appConfig = AppConfigsRepo.appConfigObjectBox.get(APP_CONFIG_ID);
		if (appConfig == null) {
			appConfig = new AppConfigs();
			appConfig.entityId = APP_CONFIG_ID;
			AppConfigsRepo.appConfigObjectBox.put(appConfig);
		}
		
		activeConfig = appConfig;
		observeChanges();
	}
	
	/**
	 * Returns the currently active application configuration instance. The returned
	 * object is kept in sync with database changes via the live subscription started
	 * in {@link #observeChanges()}. This method is the primary access point for
	 * reading configuration values throughout the application.
	 *
	 * <p><strong>Guarantee:</strong>
	 * After {@link #initialize(Box)} has been called, this method never returns
	 * {@code null}. The configuration object always exists in the database due to
	 * initialization logic that creates a default instance if none existed.
	 *
	 * @return The singleton {@link AppConfigs} instance representing the current
	 * application configuration state.
	 * @throws IllegalStateException If the repository has not been initialized
	 *                               before calling this method.
	 * @see #initialize(Box)
	 * @see #save(AppConfigs)
	 */
	public static AppConfigs getConfig() {
		return activeConfig;
	}
	
	/**
	 * Persists the provided configuration object to the database and updates the
	 * active in-memory instance. This method validates that the repository has been
	 * initialized, sets the entity ID to the fixed {@code APP_CONFIG_ID} (ensuring
	 * the singleton is updated rather than creating a new record), updates the
	 * {@code activeConfig} reference, and writes the changes to ObjectBox.
	 *
	 * <p><strong>Observers:</strong>
	 * After the database write completes, the live subscription in
	 * {@link #observeChanges()} detects the change and automatically notifies all
	 * registered observers. Callers do not need to manually invoke notification.
	 *
	 * @param config The configuration instance to save. Must not be {@code null}.
	 *               The provided instance becomes the new {@code activeConfig}.
	 * @throws IllegalStateException If the repository has not been initialized
	 *                               (i.e., {@code appConfigObjectBox} is {@code null}).
	 * @see #getConfig()
	 * @see #initialize(Box)
	 * @see #notifyObservers(AppConfigs)
	 */
	public static void save(AppConfigs config) {
		if (appConfigObjectBox == null) {
			throw new IllegalStateException("AppConfigsRepo's not initialized");
		}
		
		config.entityId = APP_CONFIG_ID;
		activeConfig = config;
		appConfigObjectBox.put(config);
	}
	
	/**
	 * Establishes a live data subscription to monitor changes to the singleton
	 * configuration entity in the ObjectBox database. When the entity with ID
	 * {@code APP_CONFIG_ID} is updated, the observer automatically refreshes the
	 * {@code activeConfig} reference and notifies all registered observers via
	 * {@link #notifyObservers(AppConfigs)}.
	 *
	 * <p><strong>Implementation details:</strong>
	 * <ul>
	 * <li>If {@code appConfigObjectBox} is {@code null}, the method returns silently.</li>
	 * <li>A query is built to match the entity with ID {@code APP_CONFIG_ID}.</li>
	 * <li>The subscription is stored in {@code configSubscription} to allow later
	 *     cancellation during {@link #release()}.</li>
	 * <li>The observer checks if the result set is non-empty before processing.</li>
	 * <li>Any exceptions during subscription setup are caught and logged without
	 *     crashing the application.</li>
	 * </ul>
	 *
	 * <p><strong>Resource management:</strong>
	 * The query uses a try-with-resources block to ensure the {@link Query} object
	 * is properly closed after subscription creation, preventing resource leaks.
	 *
	 * @see io.objectbox.reactive.DataSubscription
	 * @see #notifyObservers(AppConfigs)
	 * @see #release()
	 */
	private static void observeChanges() {
		try {
			if (appConfigObjectBox == null) return;
			try (Query<AppConfigs> query = appConfigObjectBox.query()
				.equal(AppConfigs_.entityId, APP_CONFIG_ID)
				.build()) {
				configSubscription = query.subscribe()
					.observer(data -> {
						if (data.isEmpty()) return;
						AppConfigs updatedConfig = data.get(0);
						activeConfig = updatedConfig;
						notifyObservers(updatedConfig);
					});
			}
		} catch (Exception error) {
			logger.error("Error while observing app configs.", error);
		}
	}
	
	/**
	 * Notifies all registered observers about a change in the application configuration.
	 * This method iterates through the {@code observers} collection and invokes
	 * {@link AppConfigsObserver#onConfigChanged(AppConfigs)} for each observer.
	 * Any exception thrown during notification is caught and logged without
	 * interrupting the notification of other observers.
	 *
	 * <p><strong>Error isolation:</strong>
	 * A try-catch block wraps each observer invocation individually. This ensures
	 * that a faulty observer implementation does not prevent other observers from
	 * receiving the configuration update. Exceptions are logged via
	 * {@code logger.error()} with a descriptive message.
	 *
	 * @param config The updated {@link AppConfigs} object to be passed to all
	 *               registered observers. Should never be {@code null}.
	 * @see #registerObserver(AppConfigsObserver)
	 * @see #unregisterObserver(AppConfigsObserver)
	 * @see AppConfigsObserver#onConfigChanged(AppConfigs)
	 */
	private static void notifyObservers(AppConfigs config) {
		for (AppConfigsObserver observer : observers) {
			try {
				observer.onConfigChanged(config);
			} catch (Exception error) {
				logger.error("Error while notifying observers.", error);
			}
		}
	}
	
	/**
	 * Registers an observer to receive notifications when the application configuration
	 * changes. The observer is added to the internal {@code observers} collection and
	 * will be notified via {@link #notifyObservers(AppConfigs)} on subsequent config
	 * updates.
	 *
	 * <p><strong>Lifecycle management:</strong>
	 * Observers should be unregistered when they are no longer needed (e.g., in
	 * {@link Activity#onDestroy()}) to prevent memory leaks. Failure to
	 * unregister may cause the observer to be retained indefinitely.
	 *
	 * @param observer The observer to register. Must not be {@code null}.
	 * @see #unregisterObserver(AppConfigsObserver)
	 * @see #notifyObservers(AppConfigs)
	 * @see #release()
	 */
	public static void registerObserver(AppConfigsObserver observer) {
		observers.add(observer);
	}
	
	/**
	 * Unregisters a previously registered observer, removing it from the notification
	 * list. After unregistration, the observer will no longer receive configuration
	 * change events via {@link #notifyObservers(AppConfigs)}.
	 *
	 * <p><strong>Idempotency:</strong>
	 * If the specified observer is not currently registered, this method performs
	 * no operation and does not throw an exception. It is safe to call even if the
	 * observer was never registered or has already been removed.
	 *
	 * @param observer The observer to unregister. Must not be {@code null}.
	 * @see #registerObserver(AppConfigsObserver)
	 * @see #notifyObservers(AppConfigs)
	 */
	public static void unregisterObserver(AppConfigsObserver observer) {
		observers.remove(observer);
	}
	
	/**
	 * Releases all resources held by the configuration repository. This method cancels
	 * any active ObjectBox subscription (if present) and clears the observers list.
	 * Call this method during application termination (e.g., in
	 * {@link android.app.Application#onTerminate()}) to prevent memory leaks.
	 *
	 * <p><strong>Resource cleanup:</strong>
	 * <ul>
	 * <li>Cancels the {@code configSubscription} to stop receiving database updates.</li>
	 * <li>Sets the subscription reference to {@code null} to avoid stale references.</li>
	 * <li>Clears all registered observers from the {@code observers} collection.</li>
	 * </ul>
	 *
	 * <p>After calling this method, the repository is no longer functional for
	 * receiving live updates. Observers must be re-registered if the repository
	 * is re-initialized later in the application lifecycle.
	 *
	 * @see #registerObserver(AppConfigsObserver)
	 * @see io.objectbox.reactive.DataSubscription#cancel()
	 */
	public static void release() {
		if (configSubscription != null) {
			configSubscription.cancel();
			configSubscription = null;
		}
		observers.clear();
	}
}