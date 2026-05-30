package userInterface.languagePicker;

import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LocaleHelper;

/**
 * Data record representing a single language option available for selection
 * in the language picker screen. This immutable record encapsulates all
 * necessary information to display a language item, including the display name,
 * ISO language code, and resource identifiers for visual presentation.
 *
 * <p><strong>Field descriptions:</strong>
 * <ul>
 * <li>{@code languageName} - Display name of the language (e.g., "English",
 *     "हिन्दी", "Español").</li>
 * <li>{@code languageCode} - ISO language code (e.g., "EN", "HI", "ES") used
 *     for locale configuration via {@link LocaleHelper}.</li>
 * <li>{@code illustrationResId} - Drawable resource ID for the flag or
 *     illustration representing the language/country.</li>
 * <li>{@code backgroundColorResId} - Color resource ID for the background
 *     of the language item grid cell (provides visual variety).</li>
 * </ul>
 *
 * <p><strong>Usage in UI:</strong>
 * The adapter binds these fields to a TextView for the language name and an
 * ImageView for the illustration. The background color is typically applied
 * to the item's root layout or card container.
 *
 * <p>This class is a Java record (immutable data carrier), automatically
 * providing constructor, accessors, {@code equals()}, {@code hashCode()},
 * and {@code toString()} methods.
 *
 * @param languageName         The display name of the language
 * @param languageCode         The ISO code for locale configuration
 * @param illustrationResId    Resource ID of the flag/illustration drawable
 * @param backgroundColorResId Resource ID of the background color
 * @see LocaleHelper#changeLanguage(String, BaseActivity)
 * @see LanguageAdapter
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
	 * @return The drawable resource ID (e.g., R.drawable.ic_flag_uk,
	 * R.drawable.ic_flag_spain)
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
	 * {@link TextView#setBackgroundResource(int)} or similar methods.
	 * </p>
	 *
	 * @return The color resource ID (e.g.,
	 * R.color.language_bg_english, R.color.language_bg_spanish)
	 */
	@Override
	public int backgroundColorResId() {
		return backgroundColorResId;
	}
}