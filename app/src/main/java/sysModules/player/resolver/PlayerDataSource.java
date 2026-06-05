package sysModules.player.resolver;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

public final class PlayerDataSource implements DataSource.Factory {
    private final DefaultHttpDataSource.Factory httpFactory;
    private final SimpleCache cache;

    public PlayerDataSource(@NonNull Context context) {
        httpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(getUserAgent(context))
                .setConnectTimeoutMs(30_000)
                .setReadTimeoutMs(60_000)
                .setAllowCrossProtocolRedirects(true);

        File cacheDir = new File(context.getCacheDir(), "media3_cache");
        cache = new SimpleCache(cacheDir, new NoOpCacheEvictor());
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
