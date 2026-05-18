package userInterface.language;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

/**
 * An immutable data container (Java Record) representing an individual language configuration option
 * inside the user interface.
 * <p>
 * This model encapsulates all properties necessary to render a localized language choice item on screen,
 * including string identifiers and android-specific graphical resource references.
 * </p>
 *
 * @param languageName         The human-readable title of the language (e.g., "English", "Español").
 * @param languageCode         The unique localization ISO or regional code configuration string
 *                             (e.g., "en", "es").
 * @param illustrationResId    The Android drawable resource identifier representing the language's
 *                             graphical flag or icon.
 * @param backgroundColorResId The Android color resource identifier utilized to paint background
 *                             elements or card themes.
 */
public record LanguageItem(String languageName,
                           String languageCode,
                           @DrawableRes int illustrationResId,
                           @ColorRes int backgroundColorResId) {
	
	/**
	 * Returns the Android drawable resource identifier for this language's flag or icon graphic.
	 *
	 * @return An integer referencing a valid drawable resource package identifier.
	 */
	@Override public int illustrationResId() {
		return illustrationResId;
	}
	
	/**
	 * Returns the Android color resource identifier used to theme the item's background layer.
	 *
	 * @return An integer referencing a valid color resource package identifier.
	 */
	@Override public int backgroundColorResId() {
		return backgroundColorResId;
	}
}