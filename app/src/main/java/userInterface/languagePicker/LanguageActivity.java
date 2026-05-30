package userInterface.languagePicker;

import android.content.Intent;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.nextgen.R;
import com.nextgen.databinding.ActivityLanguage1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.GridLayoutSpacing;
import coreUtils.library.views.TextViewsUtils;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.termsConsPolicy.TermsPolicyActivity;

/**
 * Core activity component responsible for rendering and managing the language selection screen.
 * <p>
 * This class inherits structural window layout operations from {@link BaseActivity} utilizing
 * Android architecture ViewBinding. By implementing {@link LanguageCallback}, it directly intercepts
 * item interaction events from the underlying list layout to update the global device session locale
 * settings when a target language choice is confirmed.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Displays available languages in a 3-column grid layout</li>
 *   <li>Provides a skip button for users who want to proceed with default language settings</li>
 *   <li>Automatically saves selected language preference to app configuration</li>
 *   <li>Applies locale changes to the entire application context</li>
 *   <li>Navigates to Terms & Conditions screen after language selection</li>
 *   <li>Applies gradient styling to the title text for visual appeal</li>
 * </ul>
 * </p>
 *
 * <p><b>Layout Structure:</b>
 * <ul>
 *   <li>Top: Title text with gradient effect</li>
 *   <li>Middle: RecyclerView displaying language options in grid format</li>
 *   <li>Bottom: Skip button for bypassing language selection</li>
 * </ul>
 * </p>
 *
 * <p><b>Behavioral Notes:</b>
 * When a language is selected, the activity automatically saves the configuration,
 * applies the new locale using {@link LocaleHelper}, and proceeds to the next
 * onboarding step (TermsPolicyActivity). The skip button marks the locale as
 * configured without changing it and proceeds to the next screen.
 * </p>
 *
 * @see BaseActivity
 * @see LanguageCallback
 * @see LanguageViewModel
 * @see LanguageAdapter
 */
public class LanguageActivity extends
	BaseActivity<ActivityLanguage1Binding> implements LanguageCallback {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private LanguageAdapter languageAdapter;
	
	/**
	 * Determines whether the screen layout configuration should freeze its physical
	 * position state. Overriding this to return {@code true} signals the core engine
	 * layer to lock down screen configurations, blocking user device movements from
	 * forcing runtime rotation changes.
	 *
	 * @return {@code true} to explicitly lock the orientation environment; {@code false} to
	 * allow rotational changes.
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for the Language selection activity.
	 * <p>
	 * This method initializes the {@link ActivityLanguage1Binding} using the provided
	 * {@link LayoutInflater}, allowing for type-safe access to the layout's views
	 * including the title text, language RecyclerView, and skip button.
	 * </p>
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the binding
	 * @return A new instance of {@link ActivityLanguage1Binding}
	 */
	@Override
	protected ActivityLanguage1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityLanguage1Binding.inflate(inflater);
	}
	
	/**
	 * Initializes the activity's components once the layout has been loaded.
	 * <p>
	 * This method is called after the layout has been inflated and performs the
	 * following setup in sequence:
	 * </p>
	 * <ol>
	 *   <li>Initializes the RecyclerView and binds language data from the ViewModel</li>
	 *   <li>Configures the skip button click listener</li>
	 *   <li>Applies gradient styling to the "Language" portion of the title text</li>
	 * </ol>
	 */
	@Override
	protected void onLoadedLayout() {
		initViews(getLanguageViewModel());
		initializeButtons();
		applyGradientToTitle();
	}
	
	/**
	 * Retrieves or creates the LanguageViewModel instance for this activity.
	 * <p>
	 * This method uses {@link ViewModelProvider} to obtain the ViewModel scoped to
	 * this activity's lifecycle. The ViewModel survives configuration changes such
	 * as screen rotations, preserving language data and selection state.
	 * </p>
	 *
	 * <p><b>ViewModel Scope:</b>
	 * The ViewModel is tied to the activity's lifecycle and will be cleared only
	 * when the activity finishes, not during configuration changes.
	 * </p>
	 *
	 * @return The {@link LanguageViewModel} instance associated with this activity
	 */
	@NonNull
	private LanguageViewModel getLanguageViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		return viewModelProvider.get(LanguageViewModel.class);
	}
	
	/**
	 * Initializes the UI components for the language selection screen.
	 * <p>
	 * This method sets up the RecyclerView that displays available language options
	 * and binds the language data from the ViewModel to the adapter.
	 * </p>
	 *
	 * @param languageViewModel The ViewModel that provides language data for the adapter
	 */
	private void initViews(LanguageViewModel languageViewModel) {
		setupRecyclerView();
		bindLanguageData(languageViewModel);
	}
	
	/**
	 * Configures the RecyclerView for displaying language options in a grid layout.
	 * <p>
	 * This method creates and sets up the LanguageAdapter, configures a GridLayoutManager
	 * with 3 columns, and adds spacing decoration between grid items. The RecyclerView
	 * will display available language options that users can select from.
	 * </p>
	 *
	 * <p><b>Configuration Details:</b>
	 * <ul>
	 *   <li>Adapter: LanguageAdapter handles language item rendering and click events</li>
	 *   <li>LayoutManager: GridLayoutManager with 3 columns</li>
	 *   <li>Item Decoration: GridLayoutSpacing with 2dp spacing between items</li>
	 * </ul>
	 * </p>
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
	 * Observes language data from the ViewModel and updates the RecyclerView adapter.
	 * <p>
	 * This method sets up a LiveData observer on the language list provided by the
	 * ViewModel. Whenever the language data changes, the observer updates the adapter
	 * with the new list, causing the RecyclerView to refresh and display the updated
	 * language options.
	 * </p>
	 *
	 * @param viewModel The ViewModel containing the LiveData of language items
	 */
	private void bindLanguageData(LanguageViewModel viewModel) {
		viewModel.getLanguages().observe(this, languages -> {
			if (languageAdapter != null) {
				languageAdapter.setLanguages(languages);
			}
		});
	}
	
	/**
	 * Configures click listeners for all interactive buttons in the language selection screen.
	 * <p>
	 * This method currently sets up the skip button, which allows users to bypass
	 * language selection and proceed with the default or previously configured locale.
	 * When clicked, it marks the locale as configured in the app settings and navigates
	 * to the next activity in the onboarding flow.
	 * </p>
	 *
	 * <p><b>Skip Button Behavior:</b>
	 * <ul>
	 *   <li>Sets isLocaleConfigured flag to true in AppConfigsRepo</li>
	 *   <li>Saves the updated configuration</li>
	 *   <li>Opens the next activity (TermsPolicyActivity)</li>
	 * </ul>
	 * </p>
	 */
	private void initializeButtons() {
		binding.btnSkip.setOnClickListener(view -> {
			AppConfigsRepo.getConfig().isLocaleConfigured = true;
			AppConfigsRepo.getConfig().save();
			openNextActivity();
		});
	}
	
	/**
	 * Applies a color gradient span to the "Language" portion of the title text.
	 * <p>
	 * This method searches for the word "Language" within the title text. If found,
	 * it applies a linear gradient transition between the secondary color and primary
	 * variant color to that specific word using {@link TextViewsUtils#applyGradientSpan}.
	 * This creates a visually appealing gradient effect that highlights the key part
	 * of the language selection title.
	 * </p>
	 *
	 * <p><b>Gradient Parameters:</b>
	 * <ul>
	 *   <li>Start color: R.color.color_secondary</li>
	 *   <li>End color: R.color.color_primary_variant</li>
	 *   <li>Span length: 8 characters ("Language")</li>
	 * </ul>
	 * </p>
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
	 * Called when the user selects a language from the language picker.
	 * <p>
	 * This method handles the language selection callback, logging the selection,
	 * saving the selected language code and locale configuration status to the
	 * app configuration repository, applying the language change to the application
	 * context, and proceeding to the next activity in the onboarding flow.
	 * </p>
	 *
	 * <p><b>Actions Performed:</b>
	 * <ul>
	 *   <li>Logs the selected language name and code for debugging</li>
	 *   <li>Stores the language code in AppConfigsRepo</li>
	 *   <li>Sets isLocaleConfigured flag to true</li>
	 *   <li>Saves the updated configuration</li>
	 *   <li>Changes the app's locale using LocaleHelper</li>
	 *   <li>Opens the next activity (TermsPolicyActivity)</li>
	 * </ul>
	 * </p>
	 *
	 * @param languageItem The selected language item containing language name and code
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
	 * Opens the next activity in the onboarding flow after language selection.
	 * <p>
	 * This method creates an intent to start the TermsPolicyActivity, adds flags
	 * to clear the activity stack (preventing the user from returning to the language
	 * selection screen), applies a fade animation transition, and finishes the
	 * current activity.
	 * </p>
	 *
	 * <p><b>Navigation Flags:</b>
	 * <ul>
	 *   <li>{@link Intent#FLAG_ACTIVITY_NEW_TASK} - Starts the activity in a new task</li>
	 *   <li>{@link Intent#FLAG_ACTIVITY_CLEAR_TASK} - Clears any existing activities from the
	 *   task</li>
	 * </ul>
	 * </p>
	 */
	private void openNextActivity() {
		Intent intent = new Intent(LanguageActivity.this, TermsPolicyActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		startActivity(intent);
		ActivityAnimator.animActivityFade(LanguageActivity.this);
		finish();
	}
}