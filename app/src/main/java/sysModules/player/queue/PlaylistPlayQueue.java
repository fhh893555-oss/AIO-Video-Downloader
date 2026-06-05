package sysModules.player.queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.util.List;

import coreUtils.library.process.LoggerUtils;

public final class PlaylistPlayQueue extends AbstractInfoPlayQueue<PlaylistInfo> {
    private static final LoggerUtils logger = LoggerUtils.from(PlaylistPlayQueue.class);

    public PlaylistPlayQueue(@NonNull PlaylistInfo info) {
        super(info);
    }

    public PlaylistPlayQueue(@NonNull PlaylistInfo info, int index) {
        super(info, index);
    }

    public PlaylistPlayQueue(int serviceId, String url,
                              @Nullable Page nextPage,
                              @NonNull List<StreamInfoItem> streams,
                              int index) {
        super(serviceId, url, nextPage, streams, index);
    }

    @Override
    protected String getTag() {
        return "PlaylistPlayQueue@" + Integer.toHexString(hashCode());
    }

    @Override
    public void fetch() {
        if (isInitial) {
            runOnBackground(() -> {
                try {
                    PlaylistInfo info = PlaylistInfo.getInfo(baseUrl);
                    onHeadFetched(info);
                } catch (Exception e) {
                    onFetchError(e);
                }
            });
        } else if (nextPage != null) {
            runOnBackground(() -> {
                try {
                    var result = PlaylistInfo.getMoreItems(serviceId, baseUrl, nextPage);
                    onMoreFetched(result);
                } catch (Exception e) {
                    onFetchError(e);
                }
            });
        }
    }
}
