package userInterface.languagePicker;

/**
 * Listener interface used to capture and handle language selection events
 * within the user interface.
 * <p>
 * Implement this interface in Activities, Fragments, or ViewModels that
 * need to update the application's locale configuration or refresh UI text
 * strings when a user chooses a new language from a list or spinner.
 * </p>
 *
 * <p><b>Typical Usage:</b>
 * <pre>
 * public class LanguageActivity extends BaseActivity implements LanguageCallback {
 *     private LanguageAdapter adapter;
 *
 *     private void setupRecyclerView() {
 *         adapter = new LanguageAdapter(this); // 'this' as LanguageCallback
 *         binding.rvLangs.setAdapter(adapter);
 *     }
 *
 *     {@literal @}Override
 *     public void onLanguageSelected(LanguageItem languageItem) {
 *         // Save selected language
 *         AppConfigsRepo.getConfig().selectedLanguageCode = languageItem.languageCode();
 *         AppConfigsRepo.getConfig().save();
 *
 *         // Apply locale change
 *         LocaleHelper.changeLanguage(languageItem.languageCode(), this);
 *
 *         // Navigate to next screen
 *         openNextActivity();
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see LanguageItem
 * @see LanguageAdapter
 * @see LanguageActivity
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