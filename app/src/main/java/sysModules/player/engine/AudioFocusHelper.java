package sysModules.player.engine;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import coreUtils.library.process.LoggerUtils;

/**
 * Helper class for managing audio focus requests and responding to focus changes
 * during media playback. This class encapsulates the logic for requesting and
 * abandoning audio focus, handling focus gain/loss events, and coordinating
 * with a {@link MediaEngine} to pause, play, or adjust volume accordingly.
 *
 * <p><strong>Focus change handling:</strong>
 * <ul>
 * <li>GAIN → Restores full volume, resumes playback if paused due to loss.</li>
 * <li>LOSS / LOSS_TRANSIENT → Pauses playback, tracks state for potential resumption.</li>
 * <li>LOSS_TRANSIENT_CAN_DUCK → Lowers volume to 30% (ducking).</li>
 * </ul>
 *
 * <p>This class uses the modern {@link AudioFocusRequest} API (introduced in
 * Android O / API 26) for better control over delayed focus gains and audio
 * attributes. It is intended to be used as a companion to a media playback
 * engine in video player modules.
 *
 * @see AudioManager
 * @see AudioFocusRequest
 * @see MediaEngine
 */
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
	
	/**
	 * Constructs an AudioFocusHelper instance associated with the given media engine.
	 * The helper automatically creates an audio focus change listener and retains
	 * a reference to the system's {@link AudioManager} service.
	 *
	 * @param context The application or activity context used to obtain the
	 *                {@link AudioManager} system service. The application context
	 *                is used to avoid memory leaks.
	 * @param engine  The {@link MediaEngine} instance that will be controlled
	 *                (play, pause, set volume) in response to focus changes.
	 */
	public AudioFocusHelper(@NonNull Context context, @NonNull MediaEngine engine) {
		this.engine = engine;
		this.audioManager = ContextCompat.getSystemService(
			context.getApplicationContext(), AudioManager.class);
		this.focusChangeListener = createListener();
	}
	
	/**
	 * Creates and returns the audio focus change listener that reacts to focus
	 * events from the system. The listener handles three types of focus changes:
	 *
	 * <p><strong>AUDIOFOCUS_GAIN</strong> – Restores volume if ducked, resumes
	 * playback if it was paused due to focus loss, and resets loss tracking.
	 *
	 * <p><strong>AUDIOFOCUS_LOSS / LOSS_TRANSIENT</strong> – Pauses playback if
	 * currently playing, records that playback was active, and resets ducking.
	 *
	 * <p><strong>AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK</strong> – Reduces volume to
	 * {@link #DUCK_VOLUME} (30%) if not already ducked.
	 *
	 * @return The configured {@link AudioManager.OnAudioFocusChangeListener} instance.
	 */
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
	
	/**
	 * Requests permanent audio focus for media playback. This method builds an
	 * {@link AudioFocusRequest} with gain type {@link AudioManager#AUDIOFOCUS_GAIN}
	 * and configures it to accept delayed focus gains. The request includes audio
	 * attributes indicating media usage with music content type.
	 *
	 * <p><strong>Request behavior:</strong>
	 * <ul>
	 * <li>Focus gain type: AUDIOFOCUS_GAIN (expected to hold focus indefinitely).</li>
	 * <li>Delayed focus gain is accepted via {@code setAcceptsDelayedFocusGain(true)}.</li>
	 * <li>Audio attributes are set to {@code USAGE_MEDIA} and {@code CONTENT_TYPE_MUSIC}.</li>
	 * <li>The focus change listener is attached to handle focus events.</li>
	 * </ul>
	 *
	 * <p>If the request is granted, the {@code hasFocus} flag is set to {@code true}.
	 * If the {@link AudioManager} is unavailable or an exception occurs, the method
	 * returns {@code false} and logs the error.
	 *
	 * @return {@code true} if audio focus was successfully granted,
	 * {@code false} otherwise.
	 * @see AudioFocusRequest.Builder
	 * @see AudioManager#requestAudioFocus(AudioFocusRequest)
	 * @see #abandonFocus()
	 */
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
	
	/**
	 * Builds and returns the audio attributes for media playback focus requests.
	 * The attributes specify that the audio stream is used for media playback
	 * and contains music content. This classification helps the system make
	 * appropriate focus management decisions when multiple apps request focus.
	 *
	 * @return An {@link AudioAttributes} instance with usage set to
	 * {@link AudioAttributes#USAGE_MEDIA} and content type set to
	 * {@link AudioAttributes#CONTENT_TYPE_MUSIC}.
	 * @see AudioAttributes.Builder
	 */
	private static AudioAttributes buildAudioAttributes() {
		return new AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_MEDIA)
			.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
			.build();
	}
	
	/**
	 * Releases the previously requested audio focus. This method should be called
	 * when playback stops or the app no longer needs exclusive audio focus (e.g.,
	 * when the user navigates away from the player or the engine is destroyed).
	 *
	 * <p><strong>Cleanup performed:</strong>
	 * <ul>
	 * <li>Abandons the audio focus request via
	 *     {@link AudioManager#abandonAudioFocusRequest(AudioFocusRequest)}.</li>
	 * <li>Resets the {@code hasFocus} flag to {@code false}.</li>
	 * <li>Clears the {@code wasPlayingBeforeLoss} state flag.</li>
	 * <li>If ducking was active, restores volume to 100% and resets the ducked flag.</li>
	 * </ul>
	 *
	 * <p>If the {@link AudioManager} instance is unavailable or an exception occurs
	 * during focus abandonment, the error is logged but not rethrown.
	 *
	 * @see AudioManager#abandonAudioFocusRequest(AudioFocusRequest)
	 * @see #requestFocus()
	 */
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
	
	/**
	 * Returns whether the application currently holds audio focus. Focus is typically
	 * requested via {@link #requestFocus()} before starting media playback and
	 * abandoned via {@link #abandonFocus()} when playback stops.
	 *
	 * @return {@code true} if audio focus is currently held, {@code false} otherwise.
	 * @see #requestFocus()
	 * @see #abandonFocus()
	 */
	public boolean hasFocus() {
		return hasFocus;
	}
	
	/**
	 * Returns whether the engine is currently in a ducked state (reduced volume).
	 * Ducking occurs when another app requests transient focus that can be shared,
	 * such as voice navigation or notifications, requiring the media volume to
	 * be temporarily lowered (typically to 30% of normal volume).
	 *
	 * <p>When ducking is active, the engine's volume is reduced via
	 * {@link MediaEngine#setVolume(float)} with {@link #DUCK_VOLUME} (0.3f).
	 * Ducking is automatically cleared when focus is regained.
	 *
	 * @return {@code true} if the engine volume is currently ducked, {@code false}
	 * if playing at full volume or no ducking is active.
	 * @see #DUCK_VOLUME
	 * @see AudioManager#AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
	 */
	public boolean isDucked() {
		return isDucked;
	}
}
