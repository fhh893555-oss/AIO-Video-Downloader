package userInterface.languagePicker;

/**
 * Callback interface for handling language selection events within the
 * language picker UI. Implementing classes (typically an Activity or Fragment)
 * receive notifications when a user taps on a language option in the
 * RecyclerView or other selection components.
 *
 * <p><strong>Usage pattern:</strong>
 * <pre>
 * public class LanguageActivity extends BaseActivity
 *     implements LanguageCallback {
 *
 *     {@literal @}Override
 *     public void onLanguageSelected(LanguageItem languageItem) {
 *         // Save selection, apply locale, navigate
 *     }
 * }
 * </pre>
 *
 * <p>The callback is passed to {@link LanguageAdapter} at construction time
 * and invoked from the ViewHolder's click listener when a language item is
 * selected. The implementing component is responsible for persisting the
 * selection, updating the application's locale, and any subsequent navigation.
 *
 * @see LanguageAdapter
 * @see LanguageItem
 * @see #onLanguageSelected(LanguageItem)
 */
public interface LanguageCallback {
	
	/**
	 * Invoked immediately after a user selects a specific language option
	 * from the interface components.
	 * <p>
	 * This callback method is triggered when the user clicks on a language item
	 * in the RecyclerView or any other language selection UI component. The
	 * implementing component should handle saving the selected language preference,
	 * updating the application locale, and refreshing the UI or navigating to
	 * the next screen as appropriate.
	 * </p>
	 *
	 * @param languageItem The data model representing the chosen language,
	 *                     containing details like the language name, ISO code,
	 *                     and flag illustration resource ID
	 */
	void onLanguageSelected(LanguageItem languageItem);
}