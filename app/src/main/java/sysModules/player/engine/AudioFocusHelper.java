package sysModules.player.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import coreUtils.library.process.LoggerUtils;

public final class AudioFocusHelper {
	private static final LoggerUtils logger = LoggerUtils.from(AudioFocusHelper.class);
	
	private static final float DUCK_VOLUME = 0.3f;
	
	private final MediaEngine engine;
	private final AudioManager audioManager;
	private final AudioManager.OnAudioFocusChangeListener focusChangeListener;
	private AudioFocusRequest focusRequest;
	
	private boolean hasFocus;
	private boolean wasPlayingBeforeLoss;
	private boolean isDucked;
	
	public AudioFocusHelper(@NonNull Context context, @NonNull MediaEngine engine) {
		this.engine = engine;
		this.audioManager = ContextCompat.getSystemService(
			context.getApplicationContext(), AudioManager.class);
		this.focusChangeListener = createListener();
	}
	
	private AudioManager.OnAudioFocusChangeListener createListener() {
		return change -> {
			switch (change) {
				case AudioManager.AUDIOFOCUS_GAIN:
					hasFocus = true;
					if (isDucked) {
						engine.setVolume(1.0f);
						isDucked = false;
					}
					if (wasPlayingBeforeLoss && !engine.isPlaying()) {
						engine.play();
					}
					wasPlayingBeforeLoss = false;
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					hasFocus = false;
					wasPlayingBeforeLoss = engine.isPlaying();
					if (wasPlayingBeforeLoss) {
						engine.pause();
					}
					if (isDucked) {
						engine.setVolume(1.0f);
						isDucked = false;
					}
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					if (!isDucked) {
						engine.setVolume(DUCK_VOLUME);
						isDucked = true;
					}
					break;
			}
		};
	}
	
	public boolean requestFocus() {
		if (audioManager == null) return false;
		try {
			focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
				.setOnAudioFocusChangeListener(focusChangeListener)
				.setAcceptsDelayedFocusGain(true)
				.setAudioAttributes(buildAudioAttributes())
				.build();
			
			int result = audioManager.requestAudioFocus(focusRequest);
			hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
			return hasFocus;
		} catch (Exception error) {
			logger.error("Failed to request audio focus", error);
			return false;
		}
	}
	
	private static AudioAttributes buildAudioAttributes() {
		return new AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_MEDIA)
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
			.build();
	}
	
	public void abandonFocus() {
		if (audioManager == null) return;
		try {
			audioManager.abandonAudioFocusRequest(focusRequest);
			hasFocus = false;
			wasPlayingBeforeLoss = false;
			
			if (isDucked) {
				engine.setVolume(1.0f);
				isDucked = false;
			}
		} catch (Exception error) {
			logger.error("Failed to abandon audio focus", error);
		}
	}
	
	public boolean hasFocus() {
		return hasFocus;
	}
	
	public boolean isDucked() {
		return isDucked;
	}
}
