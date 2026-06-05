package sysModules.player.queue;

import sysModules.player.model.QueueEvent;

@FunctionalInterface
public interface QueueListener {
    void onQueueChanged(QueueEvent event);
}
