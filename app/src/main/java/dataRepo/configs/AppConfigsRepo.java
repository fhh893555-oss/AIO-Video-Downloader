package dataRepo.configs;

import java.util.HashSet;
import java.util.Set;

import coreUtils.library.process.LoggerUtils;
import io.objectbox.Box;
import io.objectbox.query.Query;
import io.objectbox.reactive.DataSubscription;

public class AppConfigsRepo {
    private static final LoggerUtils logger = LoggerUtils.from(AppConfigsRepo.class);

    private static final long APP_CONFIG_ID = 1L;
    private static final Set<AppConfigsObserver> observers = new HashSet<>();
    private static DataSubscription configSubscription;
    private static volatile AppConfig activeConfig;
    private static Box<AppConfig> appConfigObjectBox;

    private AppConfigsRepo() {}

    public static void initialize(Box<AppConfig> appConfigObjectBox) {
        AppConfigsRepo.appConfigObjectBox = appConfigObjectBox;
        AppConfig appConfig = AppConfigsRepo.appConfigObjectBox.get(APP_CONFIG_ID);
        if (appConfig == null) {
            appConfig = new AppConfig();
            appConfig.entityId = APP_CONFIG_ID;
            AppConfigsRepo.appConfigObjectBox.put(appConfig);
        }

        activeConfig = appConfig;
        observeChanges();
    }

    public static AppConfig getConfig() {
        return activeConfig;
    }

    public static void save(AppConfig config) {
        if (appConfigObjectBox == null) {
            throw new IllegalStateException("AppConfigsRepo's not initialized");
        }

        config.entityId = APP_CONFIG_ID;
        activeConfig = config;
        appConfigObjectBox.put(config);
    }

    private static void observeChanges() {
        try {
            if (appConfigObjectBox == null) return;
            try (Query<AppConfig> query = appConfigObjectBox.query()
                    .equal(AppConfig_.entityId, APP_CONFIG_ID)
                    .build()) {
                configSubscription = query.subscribe()
                        .observer(data -> {
                            if (data.isEmpty()) return;
                            AppConfig updatedConfig = data.get(0);
                            activeConfig = updatedConfig;
                            notifyObservers(updatedConfig);
                        });
            }
        } catch (Exception error) {
            logger.error("Error while observing app configs.", error);
        }
    }

    private static void notifyObservers(AppConfig config) {
        for (AppConfigsObserver observer : observers) {
            try {
                observer.onConfigChanged(config);
            } catch (Exception error) {
                logger.error("Error while notifying observers.", error);
            }
        }
    }

    public static void registerObserver(AppConfigsObserver observer) {
        observers.add(observer);
    }

    public static void unregisterObserver(AppConfigsObserver observer) {
        observers.remove(observer);
    }

    public static void release() {
        if (configSubscription != null) {
            configSubscription.cancel();
            configSubscription = null;
        }
        observers.clear();
    }
}