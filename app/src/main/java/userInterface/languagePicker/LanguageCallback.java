package userInterface.languagePicker;

/**
 * Listener interface used to capture and handle language selection events
 * within the user interface.
 * <p>
 * Implement this interface in Activities, Fragments, or ViewModels that
 * need to update the application's locale configuration or refresh UI text
 * strings when a user chooses a new language from a list or spinner.
 * </p>
 */
public interface LanguageCallback {
	
	/**
	 * Invoked immediately after a user selects a specific language option
	 * from the interface components.
	 *
	 * @param languageItem The data model representing the chosen language,
	 *                     containing details like the language name and ISO code.
	 */
	void onLanguageSelected(LanguageItem languageItem);
}