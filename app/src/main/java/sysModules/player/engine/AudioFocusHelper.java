package sysModules.player.engine;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import coreUtils.library.process.LoggerUtils;

public final class AudioFocusHelper {
    private static final LoggerUtils logger = LoggerUtils.from(AudioFocusHelper.class);

    private final Context context;
    private final MediaEngine engine;
    private AudioManager audioManager;
    private AudioFocusRequest focusRequest;
    private boolean hasFocus;

    private final AudioManager.OnAudioFocusChangeListener focusChangeListener =
            change -> {
                switch (change) {
                    case AudioManager.AUDIOFOCUS_GAIN:
                        hasFocus = true;
                        engine.setVolume(1.0f);
                        if (!engine.isPlaying()) engine.play();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS:
                        hasFocus = false;
                        engine.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                        hasFocus = false;
                        engine.pause();
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        engine.setVolume(0.3f);
                        break;
                }
            };

    public AudioFocusHelper(@NonNull Context context, @NonNull MediaEngine engine) {
        this.context = context.getApplicationContext();
        this.engine = engine;
        this.audioManager = ContextCompat.getSystemService(this.context, AudioManager.class);
    }

    public boolean requestFocus() {
        if (audioManager == null) return false;
        try {
            int result;
	        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
		        .setOnAudioFocusChangeListener(focusChangeListener)
		        .setAcceptsDelayedFocusGain(true)
		        .setAudioAttributes(
			        new android.media.AudioAttributes.Builder()
				        .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
				        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
				        .build())
		        .build();
	        result = audioManager.requestAudioFocus(focusRequest);
	        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
            return hasFocus;
        } catch (Exception e) {
            logger.e("Failed to request audio focus", e);
            return false;
        }
    }

    public void abandonFocus() {
        if (audioManager == null) return;
        try {
            if (focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest);
            } else {
                audioManager.abandonAudioFocus(focusChangeListener);
            }
            hasFocus = false;
        } catch (Exception e) {
            logger.error("Failed to abandon audio focus", e);
        }
    }

    public boolean hasFocus() {
        return hasFocus;
    }
}
