package sysModules.sysPlayer.queue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class PendingPlaybackQueue {
	private static volatile SinglePlayQueue pending;
	
	private PendingPlaybackQueue() {}
	
	public static void set(@NonNull SinglePlayQueue queue) {
		pending = queue;
	}
	
	@Nullable
	public static SinglePlayQueue consume() {
		SinglePlayQueue q = pending;
		pending = null;
		return q;
	}
}