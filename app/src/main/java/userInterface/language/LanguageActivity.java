package userInterface.language;

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
import dataRepo.configs.AppConfigsRepo;
import userInterface.termsPolicy.TermsPolicyActivity;

/**
 * Core activity component responsible for rendering and managing the language selection screen.
 * <p>
 * This class inherits structural window layout operations from {@link BaseActivity} utilizing
 * Android architecture ViewBinding. By implementing {@link LanguageCallback}, it directly intercepts
 * item interaction events from the underlying list layout to update the global device session locale
 * settings when a target language choice is confirmed.
 * </p>
 *
 * @see BaseActivity
 * @see LanguageCallback
 * @see LanguageViewModel
 * @see LanguageAdapter
 */
public class LanguageActivity extends
	BaseActivity<ActivityLanguage1Binding> implements LanguageCallback {
	
	private final LoggerUtils logger = LoggerUtils.from(LanguageViewModel.class);
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
	 * Inflates the {@link ActivityLanguage1Binding} for this activity.
	 * This method provides the binding instance used to interact with the UI components
	 * in a type-safe manner.
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the binding.
	 * @return A new instance of {@link ActivityLanguage1Binding}.
	 */
	@Override
	protected ActivityLanguage1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityLanguage1Binding.inflate(inflater);
	}
	
	/**
	 * Executes supplementary setup routines immediately after the layout system completes
	 * initialization.This hook coordinates UI styling configurations and adapter links.
	 * <p>
	 * It loads data variants into components, establishes operational interaction buttons,
	 * and applies dynamic runtime graphics to header title elements.
	 * </p>
	 */
	@Override
	protected void onLoadedLayout() {
		initViews(getLanguageViewModel());
		initializeButtons();
		applyGradientToTitle();
	}
	
	/**
	 * Initializes and retrieves the {@link LanguageViewModel} for this activity.
	 * This method uses a {@link ViewModelProvider} to ensure the ViewModel
	 * is scoped to the activity's lifecycle.
	 *
	 * @return A non-null instance of {@link LanguageViewModel}.
	 */
	@NonNull
	private LanguageViewModel getLanguageViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		return viewModelProvider.get(LanguageViewModel.class);
	}
	
	/**
	 * Initializes the view components for the language selection screen.
	 * This method sets up the RecyclerView and binds the language data from the ViewModel.
	 *
	 * @param languageViewModel The ViewModel providing the list of available languages.
	 */
	private void initViews(LanguageViewModel languageViewModel) {
		setupRecyclerView();
		bindLanguageData(languageViewModel);
	}
	
	/**
	 * Configures the RecyclerView for displaying available languages.
	 * Sets up the adapter, a GridLayoutManager with 3 columns, and
	 * applies item decoration for consistent grid spacing.
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
	 * Binds the language data from the ViewModel to the view.
	 * Observes the list of available languages and updates the adapter when changes occur.
	 *
	 * @param viewModel The {@link LanguageViewModel} providing the language data stream.
	 */
	private void bindLanguageData(LanguageViewModel viewModel) {
		viewModel.getLanguages().observe(this, languages -> {
			if (languageAdapter != null) {
				languageAdapter.setLanguages(languages);
			}
		});
	}
	
	/**
	 * Initializes the click listeners for the activity buttons.
	 * Sets up the skip button to mark the locale as configured, save the configuration,
	 * and proceed to the next activity.
	 */
	private void initializeButtons() {
		binding.btnSkip.setOnClickListener(view -> {
			AppConfigsRepo.getConfig().isLocaleConfigured = true;
			AppConfigsRepo.getConfig().save();
			openNextActivity();
		});
	}
	
	/**
	 * Applies a color gradient effect to a specific portion of the title text.
	 * <p>
	 * This method searches for the word "Language" within the title TextView and, if found,
	 * applies a linear gradient using the app's secondary and primary variant colors.
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
	 * Handles the selection of a specific language by the user.
	 * This method logs the selection, updates the application configuration,
	 * persists the chosen language code, applies the locale change globally
	 * using {@link LocaleHelper}, and proceeds to the next activity.
	 *
	 * @param languageItem The {@link LanguageItem} object containing the name and
	 *                     code of the selected language.
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
	 * Navigates to the Terms and Policy screen.
	 * <p>
	 * This method clears the existing task stack and starts the {@link TermsPolicyActivity}
	 * as a new task, applies a fade transition animation, and finishes the current activity.
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