package sysModules.ytdlpWrapper;

import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;

import coreUtils.base.BaseApplication;
import coreUtils.library.networks.NetworkUtils;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;

/**
 * Manages the lifecycle, initialization, and background updates for the yt-dlp and FFmpeg libraries.
 *
 * <p>This class handles the asynchronous initialization of the {@link YoutubeDL} and {@link FFmpeg}
 * instances and attempts to update the yt-dlp binaries from the network if a connection is available.
 * The update channel is determined dynamically via {@link YtDlpChannelService}.</p>
 */
public class YtDlpLibraryManager {
    
    private static final LoggerUtils logger = LoggerUtils.from(YtDlpLibraryManager.class);
    private static final ThreadTask<Boolean, Boolean> updateYtdlpTask = new ThreadTask<>();

    private YtDlpLibraryManager() {}

    /**
     * Initializes the YouTube-DL and FFmpeg libraries and asynchronously updates the
     * YouTube-DL binaries if an internet connection is available.
     *
     * <p>This method cancels any existing update tasks before starting a new initialization
     * sequence. It handles library initialization, update channel selection, and binary
     * updates within a background thread. The process has a maximum execution time of 30 seconds.</p>
     *
     * @param appContext The application context required to initialize the library components.
     */
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

    /**
     * Determines the appropriate {@link YoutubeDL.UpdateChannel} for updating the binaries.
     * <p>
     * This method fetches the current channel configuration from the {@link YtDlpChannelService}.
     * It maps string constants to their corresponding enum values, defaulting to
     * {@code _STABLE} if no channel is defined, and {@code _MASTER} for unknown values.
     * </p>
     *
     * @return the resolved {@link YoutubeDL.UpdateChannel} to be used for the update process.
     */
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
}
