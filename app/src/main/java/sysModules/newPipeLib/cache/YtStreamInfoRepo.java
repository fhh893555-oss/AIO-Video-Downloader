package sysModules.newPipeLib.cache;

import java.util.List;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.process.ThreadTask;
import io.objectbox.Box;
import io.objectbox.query.LazyList;
import io.objectbox.query.Query;

public final class YtStreamInfoRepo {
    private static final LoggerUtils logger = LoggerUtils.from(YtStreamInfoRepo.class);
    private static Box<YtStreamInfo> ytStreamInfoBox;

    private YtStreamInfoRepo() {}

    public static void initialize(Box<YtStreamInfo> ytStreamInfoBox) {
        YtStreamInfoRepo.ytStreamInfoBox = ytStreamInfoBox;
        YtStreamInfoRepo.pruneOldCache();
    }

    public static YtStreamInfo findStreamInfo(String url) {
        if (ytStreamInfoBox == null || url == null) return null;

        String streamId = YtStreamInfo.extractVideoId(url);
        if (streamId == null) return null;

        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.streamId.equal(streamId)).build()) {
            YtStreamInfo cached = query.findFirst();
            if (cached != null) {
                if (cached.isExpired()) {
                    logger.debug("Cache expired. Refreshing background...");
                    refreshInBackground(cached);
                }
                return cached;
            }
        }
        return null;
    }

    private static void refreshInBackground(YtStreamInfo expiredInfo) {
        new ThreadTask.Builder<YtStreamInfo, Void>()
                .withBackgroundTask(callback -> {
                    expiredInfo.updateStreamDetails();
                    return expiredInfo;
                })
                .withResultTask(updatedInfo -> {
                    if (updatedInfo != null) {
                        save(updatedInfo);
                        String message = "Background refresh complete for: " +
                                updatedInfo.name;
                        logger.debug(message);
                    }
                })
                .withErrorTask(error -> {
                    String message1 = "Failed to refresh cache in background";
                    logger.error(message1, error);
                })
                .build()
                .start();
    }

    public static void pruneOldCache() {
        long twoWeekAgo = System.currentTimeMillis() - (14 * 24 * 60 * 60 * 1000L);
        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.cacheExpiryTime.less(twoWeekAgo))
                .build()) {
            query.remove();
        }
    }

    public static void save(YtStreamInfo info) {
        if (info == null || ytStreamInfoBox == null) return;

        if (info.streamId != null) {
            YtStreamInfo existing = getByStreamId(info.streamId);
            if (existing != null) {
                info.objectBoxId = existing.objectBoxId;
            }
        }
        ytStreamInfoBox.put(info);
    }

    public static void saveAll(List<YtStreamInfo> list) {
        if (ytStreamInfoBox == null || list == null || list.isEmpty()) return;

        for (YtStreamInfo info : list) {
            if (info.streamId != null) {
                YtStreamInfo existing = getByStreamId(info.streamId);
                if (existing != null) {
                    info.objectBoxId = existing.objectBoxId;
                }
            }
        }
        ytStreamInfoBox.put(list);
    }

    public static void delete(YtStreamInfo info) {
        if (info == null || ytStreamInfoBox == null)
            return;
        ytStreamInfoBox.remove(info);
    }

    public static void deleteById(long id) {
        if (ytStreamInfoBox == null)
            return;
        ytStreamInfoBox.remove(id);
    }

    public static void clearAll() {
        if (ytStreamInfoBox == null)
            return;
        ytStreamInfoBox.removeAll();
    }

    public static YtStreamInfo getByUrl(String url) {
        if (ytStreamInfoBox == null || url == null)
            return null;
        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.url.equal(url))
                .build()) {
            return query.findFirst();
        }
    }

    public static YtStreamInfo getByStreamId(String streamId) {
        if (ytStreamInfoBox == null || streamId == null) return null;
        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.streamId.equal(streamId))
                .build()) {
            return query.findFirst();
        }
    }

    public static List<YtStreamInfo> getByCategory(String category, int limit) {
        if (ytStreamInfoBox == null || category == null) return null;
        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.category.equal(category))
                .orderDesc(YtStreamInfo_.cacheCreatedAt)
                .build()) {
            return query.find(0, limit);
        }
    }

    /**
     * Retrieves the most recently watched streams.
     *
     * @param limit the number of recent items to retrieve.
     * @return a list of {@link YtStreamInfo} recently watched by the user.
     */
    public static List<YtStreamInfo> getRecentlyWatched(int limit) {
        if (ytStreamInfoBox == null) return null;
        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.lastWatchedAt.greater(0))
                .orderDesc(YtStreamInfo_.lastWatchedAt)
                .build()) {
            return query.find(0, limit);
        }
    }

    /**
     * Marks a stream as watched and updates its metadata for interest tracking.
     *
     * @param url the URL of the stream being watched.
     */
    public static void recordWatchEvent(String url) {
        ThreadTask.executeInBackground(() -> {
            YtStreamInfo info = getByUrl(url);
            if (info == null) {
                // If not in cache, we try to create a basic entry first
                return;
            }
            info.lastWatchedAt = System.currentTimeMillis();
            info.watchCount++;
            save(info);
            logger.debug("Watch event recorded for: " + info.name);
        });
    }

    public static LazyList<YtStreamInfo> getExpiredCache() {
        if (ytStreamInfoBox == null) return null;
        long now = System.currentTimeMillis();
        try (Query<YtStreamInfo> query = ytStreamInfoBox
                .query(YtStreamInfo_.cacheExpiryTime.less(now))
                .build()) {
            return query.findLazyCached();
        }
    }
}
