package dataRepo.dbManager;

import coreUtils.base.BaseApplication;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.MyObjectBox;
import dataRepo.user.AppUser;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import sysModules.newPipeLib.cache.YtStreamInfo;
import userInterface.appCrashed.AppCrashedInfo;

/**
 * Provides static utility methods for global ObjectBox database initialization and
 * box access across the application.
 * <p>
 * This final helper class centralizes all interactions with the ObjectBox database
 * framework, including one-time {@link BoxStore} initialization and typed
 * {@link Box} retrieval for application entities such as {@link AppConfigs},
 * {@link AppUser}, and {@link YtStreamInfo}. All methods are static, requiring no
 * instance allocation. The class maintains a single shared {@link BoxStore}
 * reference that must be initialized via {@link #initialize(BaseApplication)}
 * before any box accessor methods are called.
 * </p>
 * <ul>
 * <li>The class is declared {@code final} to prevent subclassing</li>
 * <li>All methods are static and thread-safe when {@code objectBoxStore} is fully
 *     initialized</li>
 * <li>Entities are managed via generated {@code MyObjectBox} builder class</li>
 * <li>Callers should ensure initialization occurs once during application startup</li>
 * </ul>
 *
 * @see BoxStore
 * @see Box
 * @see MyObjectBox
 * @see #initialize(BaseApplication)
 */
public final class ObjectBoxHelper {
	
	private static BoxStore objectBoxStore;
	
	/**
	 * Initializes the global ObjectBox {@link BoxStore} instance for the application.
	 * <p>
	 * This method must be called once during application startup (typically in
	 * {@link BaseApplication#onCreate()}) to construct the underlying database
	 * store. It uses the generated {@code MyObjectBox} builder class to create
	 * a {@link BoxStore} configured with the provided Android application context.
	 * Subsequent calls to {@link #getObjectBoxStore()} will return the initialized
	 * instance, and box accessor methods (e.g., {@link #getAppConfigBox()}) will
	 * function correctly.
	 * </p>
	 * <ul>
	 * <li>This method is not thread-safe and should be called on the main thread</li>
	 * <li>Only one {@link BoxStore} instance should exist per application
	 *     process</li>
	 * <li>Calling this method multiple times may create redundant instances or
	 *     throw exceptions depending on the implementation</li>
	 * </ul>
	 *
	 * @param baseApplication the application context required for ObjectBox
	 *                        initialization; must not be {@code null}
	 * @see MyObjectBox
	 * @see BoxStore
	 * @see #getObjectBoxStore()
	 */
	public static void initialize(BaseApplication baseApplication) {
		objectBoxStore = MyObjectBox.builder()
			.androidContext(baseApplication)
			.build();
	}
	
	/**
	 * Returns the globally initialized ObjectBox {@link BoxStore} instance.
	 * <p>
	 * This getter provides access to the single {@link BoxStore} instance created
	 * during {@link #initialize(BaseApplication)}. The store is required for
	 * obtaining typed {@link Box} objects via {@link BoxStore#boxFor(Class)} and
	 * for managing database transactions, queries, and subscriptions. Callers
	 * should ensure that {@link #initialize(BaseApplication)} has been called
	 * before invoking this method; otherwise, a {@code null} reference may be
	 * returned or a runtime exception may occur.
	 * </p>
	 *
	 * @return the singleton {@link BoxStore} instance, or {@code null} if not yet
	 * initialized
	 * @see BoxStore
	 * @see #initialize(BaseApplication)
	 */
	public static BoxStore getObjectBoxStore() {
		return objectBoxStore;
	}
	
	/**
	 * Returns the ObjectBox {@link Box} instance for the {@link AppConfigs} entity.
	 * <p>
	 * This static factory method retrieves the typed box from the shared
	 * {@code objectBoxStore} instance, enabling database operations such as
	 * {@link Box#put(Object)}, {@link Box#get(long)}, {@link Box#query()}, and
	 * {@link Box#remove(long)} for {@link AppConfigs} objects. The box is
	 * thread-safe and can be cached locally for repeated use.
	 * </p>
	 *
	 * @return a non-null {@link Box} handling {@link AppConfigs} entity persistence
	 * @see Box
	 * @see AppConfigs
	 */
	public static Box<AppConfigs> getAppConfigBox() {
		return objectBoxStore.boxFor(AppConfigs.class);
	}
	
	/**
	 * Returns the ObjectBox {@link Box} instance for the {@link AppUser} entity.
	 * <p>
	 * This method provides typed access to the underlying object box for
	 * {@link AppUser} objects. Callers can perform standard CRUD operations,
	 * execute queries with {@link Box#query()}, or manage relationships defined
	 * in the entity model. The returned box is backed by the same
	 * {@link io.objectbox.BoxStore} instance used across the application.
	 * </p>
	 *
	 * @return a non-null {@link Box} handling {@link AppUser} entity persistence
	 * @see Box
	 * @see AppUser
	 */
	public static Box<AppUser> getAppUserBox() {
		return objectBoxStore.boxFor(AppUser.class);
	}
	
	/**
	 * Returns the ObjectBox {@link Box} instance for the {@link YtStreamInfo} entity.
	 * <p>
	 * This static accessor retrieves the box responsible for persisting YouTube
	 * stream information objects. Common use cases include storing stream
	 * metadata, caching video details, or managing offline content queues. The
	 * box operates on the main ObjectBox thread and supports reactive queries
	 * via {@link Box#query()} and {@link io.objectbox.reactive.DataSubscription}.
	 * </p>
	 *
	 * @return a non-null {@link Box} handling {@link YtStreamInfo} entity persistence
	 * @see Box
	 * @see YtStreamInfo
	 */
	public static Box<YtStreamInfo> getYtStreamInfoBox() {
		return objectBoxStore.boxFor(YtStreamInfo.class);
	}
	
	/**
	 * Returns the ObjectBox {@link Box} instance for the {@link AppCrashedInfo} entity.
	 * <p>
	 * This static accessor retrieves the typed box responsible for persisting
	 * application crash report objects. The returned box enables standard CRUD
	 * operations such as {@link Box#put(Object)} to store crash data locally,
	 * {@link Box#get(long)} to retrieve individual records by ID, and
	 * {@link Box#query()} to perform complex searches across stored crash
	 * information.
	 * </p>
	 * <p>
	 * Local storage of crash reports using this box can serve as a persistent
	 * queue for crash data that has not yet been successfully transmitted to a
	 * remote server, enabling retry mechanisms and offline crash capture.
	 * </p>
	 *
	 * @return a non-null {@link Box} handling {@link AppCrashedInfo} entity
	 *         persistence operations
	 * @see Box
	 * @see AppCrashedInfo
	 * @see #getObjectBoxStore()
	 */
	public static Box<AppCrashedInfo> getAppCrashedInfoBox() {
		return objectBoxStore.boxFor(AppCrashedInfo.class);
	}
}
