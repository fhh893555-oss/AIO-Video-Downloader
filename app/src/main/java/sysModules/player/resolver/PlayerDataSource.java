package sysModules.player.resolver;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import sysModules.player.datasource.YoutubeHttpDataSource;

/**
 * Provides {@link DataSource.Factory} instances for all content types.
 * Caches manifest creator instances and the {@link SimpleCache} for the player.
 *
 * <p>For YouTube streams, {@link YoutubeHttpDataSource.Factory} is used by default.
 * For other services, a standard {@link DefaultHttpDataSource.Factory} is used.</p>
 */
public final class PlayerDataSource {
    private static final long MAX_CACHE_BYTES = 200L * 1024 * 1024;
    private static final int CONNECT_TIMEOUT_MS = 30_000;
    private static final int READ_TIMEOUT_MS = 60_000;

    private final Context context;
    private final DefaultHttpDataSource.Factory defaultHttpFactory;
    private final YoutubeHttpDataSource.Factory youTubeHttpFactory;
    private final CacheDataSource.Factory cacheFactory;
    private final SimpleCache cache;
    private final Map<Integer, YoutubeHttpDataSource.Factory> youTubeStreamHttpFactories;

    public PlayerDataSource(@NonNull Context context) {
        this.context = context.getApplicationContext();

        defaultHttpFactory = new DefaultHttpDataSource.Factory()
                .setUserAgent(getUserAgent(context))
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true);

        youTubeHttpFactory = new YoutubeHttpDataSource.Factory()
                .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                .setReadTimeoutMs(READ_TIMEOUT_MS)
                .setAllowCrossProtocolRedirects(true);

        File cacheDir = new File(context.getCacheDir(), "exo_player_cache");
        cache = new SimpleCache(cacheDir, new LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES));

        cacheFactory = new CacheDataSource.Factory()
                .setCache(cache)
                .setUpstreamDataSourceFactory(defaultHttpFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR);

        youTubeStreamHttpFactories = new HashMap<>();
    }

    // ─── DataSource.Factory instances ─────────────────────────────────────

    /**
     * Default factory with caching. Used for non-YouTube progressive/DASH/HLS/SS sources.
     */
    @NonNull
    public DataSource.Factory getCacheDataSourceFactory() {
        return cacheFactory;
    }

    /**
     * Raw HTTP factory without caching. Used for manifests and non-URI content.
     */
    @NonNull
    public DataSource.Factory getHttpDataSourceFactory() {
        return defaultHttpFactory;
    }

    /**
     * YouTube-specific HTTP factory with URL spoofing. Does not include caching.
     * Used by {@code PlaybackResolver} for YouTube stream resolution.
     */
    @NonNull
    public YoutubeHttpDataSource.Factory getYouTubeHttpFactory() {
        return youTubeHttpFactory;
    }

    /**
     * Returns a cached {@link YoutubeHttpDataSource.Factory} for a specific YouTube
     * stream service ID. Creates one if it doesn't exist yet.
     *
     * @param serviceId the NewPipe service ID (e.g. YouTube = 0)
     * @return the YouTube HTTP factory for this service
     */
    @NonNull
    public YoutubeHttpDataSource.Factory getYouTubeStreamHttpFactory(final int serviceId) {
        YoutubeHttpDataSource.Factory factory = youTubeStreamHttpFactories.get(serviceId);
        if (factory == null) {
            factory = new YoutubeHttpDataSource.Factory()
                    .setConnectTimeoutMs(CONNECT_TIMEOUT_MS)
                    .setReadTimeoutMs(READ_TIMEOUT_MS)
                    .setAllowCrossProtocolRedirects(true);
            youTubeStreamHttpFactories.put(serviceId, factory);
        }
        return factory;
    }

    // ─── MediaSource factory helpers ──────────────────────────────────────

    @NonNull
    public MediaSource buildProgressiveSource(@NonNull final android.net.Uri uri,
                                               @Nullable final String cacheKey,
                                               @Nullable final Object tag) {
        final com.google.android.exoplayer2.MediaItem.Builder itemBuilder =
                new com.google.android.exoplayer2.MediaItem.Builder().setUri(uri);
        if (cacheKey != null) itemBuilder.setCustomCacheKey(cacheKey);
        if (tag != null) itemBuilder.setTag(tag);
        return new ProgressiveMediaSource.Factory(cacheFactory)
                .createMediaSource(itemBuilder.build());
    }

    @NonNull
    public MediaSource buildDashSource(@NonNull final android.net.Uri uri,
                                        @Nullable final String cacheKey,
                                        @Nullable final Object tag) {
        final com.google.android.exoplayer2.MediaItem.Builder itemBuilder =
                new com.google.android.exoplayer2.MediaItem.Builder().setUri(uri);
        if (cacheKey != null) itemBuilder.setCustomCacheKey(cacheKey);
        if (tag != null) itemBuilder.setTag(tag);
        return new DashMediaSource.Factory(cacheFactory)
                .createMediaSource(itemBuilder.build());
    }

    @NonNull
    public MediaSource buildHlsSource(@NonNull final android.net.Uri uri,
                                       @Nullable final String cacheKey,
                                       @Nullable final Object tag) {
        final com.google.android.exoplayer2.MediaItem.Builder itemBuilder =
                new com.google.android.exoplayer2.MediaItem.Builder().setUri(uri);
        if (cacheKey != null) itemBuilder.setCustomCacheKey(cacheKey);
        if (tag != null) itemBuilder.setTag(tag);
        return new HlsMediaSource.Factory(cacheFactory)
                .createMediaSource(itemBuilder.build());
    }

    @NonNull
    public MediaSource buildSsSource(@NonNull final android.net.Uri uri,
                                      @Nullable final String cacheKey,
                                      @Nullable final Object tag) {
        final com.google.android.exoplayer2.MediaItem.Builder itemBuilder =
                new com.google.android.exoplayer2.MediaItem.Builder().setUri(uri);
        if (cacheKey != null) itemBuilder.setCustomCacheKey(cacheKey);
        if (tag != null) itemBuilder.setTag(tag);
        return new SsMediaSource.Factory(cacheFactory)
                .createMediaSource(itemBuilder.build());
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────

    public void release() {
        youTubeStreamHttpFactories.clear();
        try {
            cache.release();
        } catch (Exception ignored) {}
    }

    @NonNull
    public SimpleCache getCache() {
        return cache;
    }

    private static String getUserAgent(@NonNull final Context context) {
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
