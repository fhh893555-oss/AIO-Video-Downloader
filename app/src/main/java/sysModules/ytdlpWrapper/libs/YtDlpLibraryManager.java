package sysModules.ytdlpWrapper.libs;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;

import coreUtils.base.BaseApplication;
import coreUtils.library.networks.NetworkUtils;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

public class YtDlpLibraryManager {
    private static final LoggerUtils logger = LoggerUtils.from(YtDlpLibraryManager.class);
    private static final ThreadTask<Boolean, Boolean> updateYtdlpTask = new ThreadTask<>();

    private YtDlpLibraryManager() {}

    public static void initialize(BaseApplication appContext) {
        updateYtdlpTask.cancel();
        updateYtdlpTask.setBackgroundTask(progressCallback -> {
            try {
                YoutubeDL.getInstance().init(appContext);
                FFmpeg.getInstance().init(appContext);
                if (NetworkUtils.isInternetConnected()) {
                    logger.debug("Updating YouTube-DL binaries...");
                    YoutubeDL.UpdateChannel ytDlpUpdateChannel = getYtDlpUpdateChannel();
                    logger.debug("Update channel: " + ytDlpUpdateChannel);
                    YoutubeDL.getInstance().updateYoutubeDL(appContext, ytDlpUpdateChannel);
                }
                return true;
            } catch (Exception error) {
                logger.error("Initialization failed", error);
                return false;
            }
        });
        updateYtdlpTask.setResultTask(result -> {
            if (result) logger.debug("Initialization successful");
            updateYtdlpTask.cancel();
        });
        updateYtdlpTask.setErrorTask(error -> {
            logger.error("Initialization failed", error);
            updateYtdlpTask.cancel();
        });
        updateYtdlpTask.setMaxExecutionTimeMs(30_000);
        updateYtdlpTask.start();
    }

    private static YoutubeDL.UpdateChannel getYtDlpUpdateChannel() {
        String cloudChannel = YtDlpChannelService.getActiveYtDlpChannel();
        logger.debug("Cloud channel: " + cloudChannel);
        if (cloudChannel != null) {
            return switch (cloudChannel) {
                case "_STABLE" -> YoutubeDL.UpdateChannel._STABLE;
                case "_NIGHTLY" -> YoutubeDL.UpdateChannel._NIGHTLY;
                default -> YoutubeDL.UpdateChannel._MASTER;
            };
        } else {
            return YoutubeDL.UpdateChannel._STABLE;
        }
    }

    public static void shutdown() {

    }
}
