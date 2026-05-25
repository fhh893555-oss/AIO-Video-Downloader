package userInterface.languagePicker;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/**
 * A data record representing a language option available for selection in the language picker.
 * <p>
 * This immutable record encapsulates all necessary information about a language choice,
 * including its display name, ISO language code, flag illustration resource, and a
 * background color resource for visual styling. Using a Java record provides built-in
 * equals(), hashCode(), and toString() methods while ensuring thread-safety through
 * immutability.
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * LanguageItem english = new LanguageItem(
 *     "English",
 *     "en",
 *     R.drawable.ic_flag_uk,
 *     R.color.language_bg_english
 * );
 * </pre>
 * </p>
 *
 * <p><b>Field Descriptions:</b>
 * <ul>
 *   <li><b>languageName:</b> Human-readable display name (e.g., "English", "Español")</li>
 *   <li><b>languageCode:</b> ISO language code (e.g., "en", "es", "fr") for locale configuration</li>
 *   <li><b>illustrationResId:</b> Drawable resource ID for flag or country illustration</li>
 *   <li><b>backgroundColorResId:</b> Color resource ID for background styling of the language item</li>
 * </ul>
 * </p>
 *
 * @param languageName         The human-readable display name of the language
 * @param languageCode         The ISO language code for system locale configuration
 * @param illustrationResId    The drawable resource ID for the language's flag or icon
 * @param backgroundColorResId The color resource ID for the language item's background
 */
public record LanguageItem(String languageName,
                           String languageCode,
                           @DrawableRes int illustrationResId,
                           @ColorRes int backgroundColorResId) {
	
	/**
	 * Returns the drawable resource ID for the language's flag or illustration.
	 * <p>
	 * This method provides access to the illustration resource that visually represents
	 * the language, typically a country flag or cultural icon. The returned resource ID
	 * can be used with {@link android.widget.ImageView#setImageResource(int)} to display
	 * the illustration in the language selection UI.
	 * </p>
	 *
	 * @return The drawable resource ID (e.g., R.drawable.ic_flag_uk, R.drawable.ic_flag_spain)
	 */
	@Override
	public int illustrationResId() {
		return illustrationResId;
	}
	
	/**
	 * Returns the color resource ID for the language item's background styling.
	 * <p>
	 * This method provides access to the background color resource associated with the
	 * language option, allowing for customized visual distinction between different
	 * language items in the selection grid. The returned resource ID can be used with
	 * {@link android.widget.TextView#setBackgroundResource(int)} or similar methods.
	 * </p>
	 *
	 * @return The color resource ID (e.g., R.color.language_bg_english, R.color.language_bg_spanish)
	 */
	@Override
	public int backgroundColorResId() {
		return backgroundColorResId;
	}
}