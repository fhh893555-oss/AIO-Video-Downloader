package sysModules.player.queue;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.schabi.newpipe.extractor.stream.StreamInfo;

import coreUtils.library.process.LoggerUtils;
import sysModules.player.engine.MediaEngine;
import sysModules.player.model.QueueEvent;

public class QueueSyncManager implements QueueListener {
    private static final LoggerUtils logger = LoggerUtils.from(QueueSyncManager.class);

    private final MediaEngine engine;
    private final Handler mainHandler;
    @Nullable private PlayQueue queue;
    private boolean loading;

    public QueueSyncManager(@NonNull MediaEngine engine) {
        this.engine = engine;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void bind(@NonNull PlayQueue queue) {
        unbind();
        this.queue = queue;
        queue.addListener(this);
        queue.init();
        loadCurrent(0);
    }

    public void unbind() {
        if (queue != null) {
            queue.removeListener(this);
            queue.dispose();
            queue = null;
        }
        loading = false;
    }

    @Nullable
    public PlayQueue getQueue() {
        return queue;
    }

    @Override
    public void onQueueChanged(@NonNull QueueEvent event) {
        switch (event.getType()) {
            case SELECT:
                loadCurrent(0);
                break;
            case ERROR:
                QueueEvent.ErrorEvent ee = (QueueEvent.ErrorEvent) event;
                logger.e("Queue error at index " + ee.getErrorIndex());
                if (queue != null) {
                    PlayQueueItem item = queue.getItem();
                    if (item != null && item.getError() != null) {
                        engine.onPlayerError(new Exception("Stream load failed", item.getError()));
                    }
                }
                break;
            case RECOVERY:
                QueueEvent.RecoveryEvent re = (QueueEvent.RecoveryEvent) event;
                engine.seekTo(re.getPosition());
                break;
        }
    }

    public void loadCurrent(long seekToPosition) {
        if (queue == null || loading) return;
        PlayQueueItem item = queue.getItem();
        if (item == null) return;

        loading = true;
        item.fetchStreamInfo(new PlayQueueItem.StreamCallback() {
            @Override
            public void onSuccess(StreamInfo info) {
                mainHandler.post(() -> {
                    if (queue == null || queue.isDisposed()) return;
                    loading = false;
                    long seek = seekToPosition > 0 ? seekToPosition : item.getRecoveryPosition();
                    if (seek != PlayQueueItem.RECOVERY_UNSET && seek > 0) {
                        engine.load(info, seek);
                    } else {
                        engine.load(info, 0);
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                mainHandler.post(() -> {
                    loading = false;
                    logger.e("Failed to load stream: " + item.getUrl(), error);
                    item.setError(error);
                    if (queue != null) {
                        int oldIndex = queue.getIndex();
                        int newIndex = queue.error();
                        engine.onPlayerError(new Exception("Failed to load stream " + item.getTitle(), error));
                    }
                });
            }
        });
    }
}
