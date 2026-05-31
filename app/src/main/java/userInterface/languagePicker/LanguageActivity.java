package userInterface.languagePicker;

import android.content.Intent;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.nextgen.R;
import com.nextgen.databinding.ActivityLanguage1Binding;

import java.util.List;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.GridLayoutSpacing;
import coreUtils.library.views.TextViewsUtils;
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.mainScreen.MainActivity;

/**
 * Activity that allows users to select their preferred language for the application.
 * This screen displays a grid of available languages, provides a skip option to
 * bypass selection, and applies visual enhancements such as gradient text effects.
 * The activity implements {@link LanguageCallback} to receive language selection
 * events and extends {@link BaseActivity} with view binding for
 * {@link ActivityLanguage1Binding}.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays a grid of language options using RecyclerView with 3 columns.</li>
 * <li>Handles user language selection via {@link #onLanguageSelected(LanguageItem)}.</li>
 * <li>Provides a skip button to proceed with default locale configuration.</li>
 * <li>Saves selected language to {@link AppConfigsRepo} and applies locale changes.</li>
 * <li>Applies gradient color span to the word "Language" in the title for branding.</li>
 * <li>Locks screen orientation to portrait for consistent layout rendering.</li>
 * </ul>
 *
 * <p><strong>Navigation flow:</strong>
 * Upon language selection or skip, the activity saves the configuration,
 * applies the locale change (if a language was selected), and navigates to
 * {@link MainActivity} with intent flags that clear the activity back stack.
 * A fade animation is applied during the transition.
 *
 * <p><strong>Layout:</strong>
 * Uses {@code activity_language1.xml} with a RecyclerView for language grid,
 * a title text view, and a skip button. The word "Language" in the title
 * receives a gradient effect from {@code color_secondary} to
 * {@code color_primary_variant}.
 *
 * @see BaseActivity
 * @see ActivityLanguage1Binding
 * @see LanguageCallback
 * @see LanguageViewModel
 * @see LanguageAdapter
 */
public class LanguageActivity extends
	BaseActivity<ActivityLanguage1Binding> implements LanguageCallback {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private LanguageAdapter languageAdapter;
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the language selection
	 * screen to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures a consistent layout for the language grid
	 * (3 columns) and prevents unexpected UI reconfigurations during language
	 * selection. Users are expected to complete this screen quickly, making
	 * orientation locking an acceptable trade-off for layout stability.
	 *
	 * @return {@code true} to lock the activity to portrait orientation.
	 * @see BaseActivity#shouldLockOrientation()
	 * @see #onLoadedLayout()
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_language1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout.
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityLanguage1Binding} instance containing references
	 * to all views defined in the language selection layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 * @see #onLoadedLayout()
	 */
	@Override
	protected ActivityLanguage1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityLanguage1Binding.inflate(inflater);
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and is responsible for setting up the RecyclerView, initializing click
	 * listeners, and applying visual enhancements to the title text.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Initializes the RecyclerView and binds language data via
	 *     {@link #initViews(LanguageViewModel)}.</li>
	 * <li>Sets up button click listeners via {@link #initializeButtons()}.</li>
	 * <li>Applies gradient span to the word "Language" in the title via
	 *     {@link #applyGradientToTitle()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #initViews(LanguageViewModel)
	 * @see #initializeButtons()
	 * @see #applyGradientToTitle()
	 */
	@Override
	protected void onLoadedLayout() {
		initViews(getLanguageViewModel());
		initializeButtons();
		applyGradientToTitle();
	}
	
	/**
	 * Provides the ViewModel instance for the language selection screen.
	 * This method uses {@link ViewModelProvider} to obtain or create the
	 * {@link LanguageViewModel}, which manages the list of available languages
	 * and handles language selection logic.
	 *
	 * <p>The ViewModel is scoped to the activity's lifecycle, surviving
	 * configuration changes such as screen rotations (though orientation is
	 * locked in this activity). This ensures language data is preserved and
	 * not re-fetched unnecessarily.
	 *
	 * @return A non-null {@link LanguageViewModel} instance associated with
	 * this activity.
	 * @see ViewModelProvider
	 * @see LanguageViewModel
	 * @see #initViews(LanguageViewModel)
	 */
	@NonNull
	private LanguageViewModel getLanguageViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		return viewModelProvider.get(LanguageViewModel.class);
	}
	
	/**
	 * Initializes the UI components of the language selection screen. This method
	 * sets up the RecyclerView for displaying language options and binds the
	 * ViewModel's language data to the adapter for observation.
	 *
	 * @param languageViewModel The ViewModel containing the list of available
	 *                          languages to display. Must not be {@code null}.
	 * @see #setupRecyclerView()
	 * @see #bindLanguageData(LanguageViewModel)
	 */
	private void initViews(LanguageViewModel languageViewModel) {
		setupRecyclerView();
		bindLanguageData(languageViewModel);
	}
	
	/**
	 * Configures the RecyclerView for displaying the list of selectable languages.
	 * This method initializes the {@link LanguageAdapter}, attaches it to the
	 * RecyclerView, sets a 3-column grid layout, and adds spacing decoration
	 * between grid items.
	 *
	 * <p><strong>Layout configuration:</strong>
	 * <ul>
	 * <li>Adapter: {@link LanguageAdapter} with the current activity context.</li>
	 * <li>Layout manager: {@link GridLayoutManager} with 3 columns.</li>
	 * <li>Spacing: {@code R.dimen._2} (typically 2dp) added between grid items
	 *     via {@link GridLayoutSpacing} decoration.</li>
	 * </ul>
	 *
	 * <p>The spacing decoration is applied with the {@code includeEdge} parameter
	 * set to {@code true}, ensuring consistent margins around the outer edges
	 * of the grid as well as between items.
	 *
	 * @see LanguageAdapter
	 * @see GridLayoutManager
	 * @see GridLayoutSpacing
	 */
	private void setupRecyclerView() {
		languageAdapter = new LanguageAdapter(this);
		binding.rvLangs.setAdapter(languageAdapter);
		GridLayoutManager manager = new GridLayoutManager(binding.getRoot().getContext(), 3);
		binding.rvLangs.setLayoutManager(manager);
		int spacingInPx = getResources().getDimensionPixelSize(R.dimen._2);
		binding.rvLangs.addItemDecoration(new GridLayoutSpacing(spacingInPx, true));
	}
	
	/**
	 * Observes the LiveData of available languages from the ViewModel and updates
	 * the RecyclerView adapter when the data changes. This method establishes a
	 * lifecycle-aware observation that automatically handles configuration changes
	 * and activity lifecycle events.
	 *
	 * <p><strong>Observation flow:</strong>
	 * When {@link LanguageViewModel#getLanguages()} emits a new list of languages,
	 * the observer checks if the adapter is non-null and calls
	 * {@link LanguageAdapter#setLanguages(List)} to refresh the displayed items.
	 *
	 * <p>The observer is bound to the activity's lifecycle via {@code this} as the
	 * LifecycleOwner, preventing memory leaks and ensuring updates are only
	 * delivered when the activity is in a resumed state.
	 *
	 * @param viewModel The ViewModel providing the LiveData of language items.
	 *                  Must not be {@code null}. The LiveData is expected to be
	 *                  already initialized and ready for observation.
	 * @see LanguageViewModel#getLanguages()
	 * @see LanguageAdapter#setLanguages(List)
	 * @see LiveData#observe(LifecycleOwner, androidx.lifecycle.Observer)
	 */
	private void bindLanguageData(LanguageViewModel viewModel) {
		viewModel.getLanguages().observe(this, languages -> {
			if (languageAdapter != null) {
				languageAdapter.setLanguages(languages);
			}
		});
	}
	
	/**
	 * Configures click listeners for all interactive buttons in the language selection
	 * screen. This method currently sets up the skip button, which allows the user
	 * to bypass language selection and proceed with the default locale.
	 *
	 * <p><strong>Skip button behavior:</strong>
	 * When the skip button is clicked, the method marks the locale as configured
	 * by setting {@link AppConfigs#isLocaleConfigured} to {@code true}, persists
	 * the change via {@link AppConfigs#save()}, and navigates to the next activity
	 * using {@link #openNextActivity()}. No language change is applied, so the
	 * app continues with the system default or previously selected language.
	 *
	 * <p>The lambda expression uses view binding to reference the skip button
	 * from the layout. Additional buttons (e.g., confirm, back) should be
	 * initialized here as they are added to the layout.
	 *
	 * @see #openNextActivity()
	 * @see AppConfigsRepo#getConfig()
	 * @see AppConfigs#save()
	 */
	private void initializeButtons() {
		binding.btnSkip.setOnClickListener(view -> {
			AppConfigsRepo.getConfig().isLocaleConfigured = true;
			AppConfigsRepo.getConfig().save();
			openNextActivity();
		});
	}
	
	/**
	 * Applies a gradient color span to the word "Language" within the title text
	 * view. This method searches for the substring "Language" in the full title
	 * text and, if found, applies a gradient effect using
	 * {@link TextViewsUtils#applyGradientSpan(android.widget.TextView, int, int, int, int)}.
	 *
	 * <p><strong>Visual effect:</strong>
	 * The gradient transitions from {@code color_secondary} to
	 * {@code color_primary_variant}, spanning the 8 characters of the word
	 * "Language". This creates a highlighted, branded appearance for the key
	 * word in the title, drawing user attention to the language selection purpose
	 * of the screen.
	 *
	 * <p><strong>Error handling:</strong>
	 * If the word "Language" is not found in the title string (e.g., due to
	 * localization or layout changes), the method silently does nothing without
	 * throwing an exception. The start index check ensures the span is only
	 * applied when the substring exists.
	 *
	 * @see TextViewsUtils#applyGradientSpan(android.widget.TextView, int, int, int, int)
	 * @see #getColor(int)
	 */
	private void applyGradientToTitle() {
		String fullText = binding.tvTitle.getText().toString();
		int nextGenStart = fullText.indexOf("Language");
		if (nextGenStart != -1) {
			TextViewsUtils.applyGradientSpan(
				binding.tvTitle,
				getColor(R.color.color_secondary),
				getColor(R.color.color_primary_variant),
				nextGenStart,
				nextGenStart + 8
			);
		}
	}
	
	/**
	 * Handles the user's language selection from the language picker interface.
	 * This callback method logs the selected language, updates the application
	 * configuration with the new language code, marks the locale as configured,
	 * persists the changes to the database, applies the language change to the
	 * current activity context, and navigates to the next screen.
	 *
	 * <p>The activity transition uses a fade animation via
	 * {@link ActivityAnimator#animActivityFade(BaseActivity)} . The intent
	 * flags {@link Intent#FLAG_ACTIVITY_NEW_TASK} and
	 * {@link Intent#FLAG_ACTIVITY_CLEAR_TASK} clear the activity back stack,
	 * preventing the user from returning to the language selection screen via
	 * the back button after language configuration is complete.
	 *
	 * @param languageItem The selected language item containing the language name
	 *                     and code. Must not be {@code null}.
	 * @see #openNextActivity()
	 * @see LocaleHelper#changeLanguage(String, BaseActivity)
	 * @see AppConfigsRepo#getConfig()
	 */
	@Override
	public void onLanguageSelected(LanguageItem languageItem) {
		String message = "Language selected: " + languageItem.languageName() +
			" (" + languageItem.languageCode() + ")";
		logger.info(message);
		AppConfigsRepo.getConfig().selectedLanguageCode = languageItem.languageCode();
		AppConfigsRepo.getConfig().isLocaleConfigured = true;
		AppConfigsRepo.getConfig().save();
		LocaleHelper.changeLanguage(languageItem.languageCode(), this);
		openNextActivity();
	}
	
	/**
	 * Navigates to the main activity and finishes the current language selection screen.
	 * This method constructs an intent targeting {@link MainActivity}, adds flags to
	 * clear the activity back stack, starts the activity with a fade transition animation,
	 * and closes the current activity.
	 *
	 * <p><strong>Intent flags:</strong>
	 * <ul>
	 * <li>{@link Intent#FLAG_ACTIVITY_NEW_TASK} - Starts the activity as a new task.</li>
	 * <li>{@link Intent#FLAG_ACTIVITY_CLEAR_TASK} - Clears any existing activities
	 *     from the task stack before launching.</li>
	 * </ul>
	 * These flags ensure the user cannot press the back button to return to the
	 * language selection screen after completing the initial setup flow.
	 *
	 * <p>The fade animation is applied via
	 * {@link ActivityAnimator#animActivityFade(BaseActivity)}, which provides
	 * a smooth visual transition between screens.
	 *
	 * @see #onLanguageSelected(LanguageItem)
	 * @see ActivityAnimator#animActivityFade(BaseActivity)
	 */
	private void openNextActivity() {
		Intent intent = new Intent(LanguageActivity.this, MainActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		ActivityAnimator.animActivityFade(LanguageActivity.this);
		finish();
	}
}