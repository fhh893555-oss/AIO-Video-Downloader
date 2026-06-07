package sysModules.player.resolver;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;

public final class PlayerDataSource implements DataSource.Factory {
    private static final long MAX_CACHE_BYTES = 200L * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private final DefaultHttpDataSource.Factory httpFactory;
    private final SimpleCache cache;

    public PlayerDataSource(@NonNull Context context) {
        httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(getUserAgent(context))
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true);

        File cacheDir = new File(context.getCacheDir(), "exo_player_cache");
        cache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES));
    }

    @Override
    public DataSource createDataSource() {
        return new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(httpFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                .createDataSource();
    }

    public DataSource.Factory getHttpFactory() {
        return httpFactory;
    }

    public void release() {
        try {
            cache.release();
        } catch (Exception ignored) {}
    }

    private static String getUserAgent(Context context) {
        try {
            String pkg = context.getPackageName();
            String version = context.getPackageManager()
                    .getPackageInfo(pkg, 0).versionName;
            return pkg + "/" + (version != null ? version : "1.0");
        } catch (Exception e) {
            return "TubeAIO-Player/1.0";
        }
    }
}
