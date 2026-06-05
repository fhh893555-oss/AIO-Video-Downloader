package sysModules.player.helper;

import androidx.annotation.NonNull;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class PlayerHelper {
    private PlayerHelper() {}

    public static String getTimeString(long milliSeconds) {
        if (milliSeconds < 0) milliSeconds = 0;
        long seconds = (milliSeconds % 60000) / 1000;
        long minutes = (milliSeconds % 3600000) / 60000;
        long hours = (milliSeconds % 86400000) / 3600000;
        long days = milliSeconds / 86400000;

        if (days > 0) {
            return String.format(Locale.US, "%d:%02d:%02d:%02d", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%d:%02d", minutes, seconds);
        }
    }

    public static String formatSpeed(float speed) {
        if (speed == (long) speed) {
            return String.format(Locale.US, "%dx", (long) speed);
        }
        return String.format(Locale.US, "%.2fx", speed).replaceAll("\\.?0*x$", "x");
    }

    public static int[] getPlaybackSpeeds() {
        return new int[]{25, 50, 75, 100, 125, 150, 175, 200, 300, 400, 500};
    }

    public static float speedForIndex(int index) {
        int[] speeds = getPlaybackSpeeds();
        if (index < 0 || index >= speeds.length) return 1.0f;
        return speeds[index] / 100.0f;
    }

    public static int indexForSpeed(float speed) {
        int[] speeds = getPlaybackSpeeds();
        int closest = 3;
        float minDiff = Math.abs(speed - 1.0f);
        for (int i = 0; i < speeds.length; i++) {
            float diff = Math.abs(speed - speeds[i] / 100.0f);
            if (diff < minDiff) { minDiff = diff; closest = i; }
        }
        return closest;
    }

    public static boolean isLiveStream(long duration) {
        return duration == Long.MAX_VALUE || duration == -1;
    }
}
