package dataRepo.manager;

import coreUtils.base.BaseApplication;
import dataRepo.configs.AppConfig;
import dataRepo.configs.MyObjectBox;
import dataRepo.user.AppUser;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import sysModules.newPipeLib.cache.YtStreamInfo;

public final class ObjectBoxHelper {
    private static BoxStore objectBoxStore;

    public static void initialize(BaseApplication baseApplication) {
        objectBoxStore = MyObjectBox.builder()
                .androidContext(baseApplication)
                .build();
    }

    public static BoxStore getObjectBoxStore() {
        return objectBoxStore;
    }

    public static Box<AppConfig> getAppConfigBox() {
        return objectBoxStore.boxFor(AppConfig.class);
    }

    public static Box<AppUser> getAppUserBox() {
        return objectBoxStore.boxFor(AppUser.class);
    }

    public static Box<YtStreamInfo> getYtStreamInfoBox() {
        return objectBoxStore.boxFor(YtStreamInfo.class);
    }
}
