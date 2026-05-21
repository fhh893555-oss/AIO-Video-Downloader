package userInterface.termsConsPolicy;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;

import com.nextgen.R;
import com.nextgen.databinding.ActivityTermsCon1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.ActivityAnimator;
import coreUtils.library.views.StylizedToastView;
import coreUtils.library.views.TextViewsUtils;
import dataRepo.configs.AppConfig;
import dataRepo.configs.AppConfigsRepo;
import userInterface.openingSplash.OpeningActivity;

/**
 * Activity responsible for displaying and managing the Terms and Conditions and Privacy Policy.
 * <p>
 * This screen allows users to read detailed policy sections through expandable views,
 * acknowledge their agreement via a checkbox, and persist this state to the application
 * configuration. It handles different navigation flows depending on whether it was
 * launched from the initial onboarding (Opening screen) or from the app settings.
 * </p>
 *
 * @see BaseActivity
 * @see ActivityTermsCon1Binding
 * @see AppConfig
 */
public class TermsPolicyActivity extends BaseActivity<ActivityTermsCon1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	public static final String LAUNCHED_LOCATION_OF_ACTIVITY = "Location";
	public static final short LAUNCHED_FROM_OPENING_SCREEN = 1;
	public static final short LAUNCHED_FROM_SETTINGS_SCREEN = 2;
	
	/**
	 * Determines whether the screen orientation should be locked for this activity.
	 *
	 * @return {@code true} to prevent the screen from rotating and lock it in its
	 * current or default orientation, {@code false} otherwise.
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the {@link ActivityTermsCon1Binding} for this activity.
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the binding.
	 * @return The initialized binding instance for the Terms and Policy layout.
	 */
	@Override protected ActivityTermsCon1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityTermsCon1Binding.inflate(inflater);
	}
	
	/**
	 * Called after the layout has been successfully loaded and inflated.
	 * This method initializes the UI state by hiding expandable details,
	 * applying visual styling to the title, and setting up all necessary
	 * button click listeners for the activity.
	 */
	@Override
	protected void onLoadedLayout() {
		hideExpandedDetails();
		applyGradientToTitle();
		setupButtonClickEvents();
	}
	
	/**
	 * Applies a color gradient effect to a specific portion of the terms title text.
	 * It searches for the keyword "Conditions" within the title and applies a
	 * horizontal linear gradient using the secondary and primary variant colors.
	 */
	private void applyGradientToTitle() {
		String fullText = binding.top1.tvTermsTitle.getText().toString();
		int nextGenStart = fullText.indexOf("Conditions");
		if (nextGenStart != -1) {
			TextViewsUtils.applyGradientSpan(
				binding.top1.tvTermsTitle,
				getColor(R.color.color_secondary),
				getColor(R.color.color_primary_variant),
				nextGenStart,
				nextGenStart + 10
			);
		}
	}
	
	/**
	 * Initializes and configures all click event listeners for the activity's user interface
	 * components. This includes setting up handlers for the back navigation, expandable policy
	 * sections, terms agreement checkbox, and the final agreement submission button.
	 */
	private void setupButtonClickEvents() {
		setupBackButtonClickEvent();
		setupExpandTermsButtonClickEvent();
		configureTermsCheckbox();
		setupAgreeTermsButton();
	}
	
	/**
	 * Configures the click listener for the back button to terminate the current activity
	 * and return the user to the previous screen.
	 */
	private void setupBackButtonClickEvent() {
		binding.btnBack.setOnClickListener(view -> finish());
	}
	
	/**
	 * Configures click listeners for the individual terms and conditions sections.
	 * When a section header is clicked, it toggles the visibility of its
	 * corresponding detailed content area.
	 */
	private void setupExpandTermsButtonClickEvent() {
		binding.top2.itemAcceptance.setOnClickListener(view ->
			toggleVisibility(binding.top2.extraAcceptance));
		
		binding.top2.itemUseOfApp.setOnClickListener(view ->
			toggleVisibility(binding.top2.extraUseOfApp));
		
		binding.top2.itemIntellectual.setOnClickListener(view ->
			toggleVisibility(binding.top2.extraIntellectual));
		
		binding.top2.itemProhibited.setOnClickListener(view ->
			toggleVisibility(binding.top2.extraProhibited));
		
		binding.top2.itemLimitation.setOnClickListener(view ->
			toggleVisibility(binding.top2.extraLimitation));
		
		binding.top2.itemChanges.setOnClickListener(view ->
			toggleVisibility(binding.top2.extraChanges));
	}
	
	/**
	 * Configures the click listener for the "Agree and Continue" button.
	 * <p>
	 * This method handles the logic for saving the user's acceptance status:
	 * <ul>
	 *     <li>Updates the {@link AppConfig} with the current state of the agreement checkbox.</li>
	 *     <li>If agreed and launched from the opening screen, navigates to the main/opening activity.</li>
	 *     <li>If agreed and launched from settings, returns the user to the previous screen.</li>
	 *     <li>If not agreed, displays an error message, provides haptic feedback, and closes the activity.</li>
	 * </ul>
	 */
	private void setupAgreeTermsButton() {
		binding.top2.btnAgreeContinue.setOnClickListener(view -> {
			AppConfig config = AppConfigsRepo.getConfig();
			config.isTermsConditionsAgreed = binding.top2.cbAgreeTerms.isChecked();
			config.save();
			
			if (config.isTermsConditionsAgreed && isLaunchedFromOpeningScreen()) {
				//todo: open main activity.
				Intent intent = new Intent(this, OpeningActivity.class);
				startActivity(intent);
				ActivityAnimator.animActivityFade(this);
				finish();
			} else if (config.isTermsConditionsAgreed && !isLaunchedFromOpeningScreen()) {
				binding.btnBack.performClick();
			} else {
				String toastMessage = StringHelper.getText(R.string.hint_you_must_accept_the_terms);
				StylizedToastView.showError(this, toastMessage);
				vibrate(50);
				finish();
			}
		});
	}
	
	/**
	 * Configures the click listener for the terms and conditions checkbox container.
	 * When the associated view is clicked, it toggles the checked state of the
	 * terms agreement checkbox.
	 */
	private void configureTermsCheckbox() {
		binding.top2.btnAcceptTermsCheck.setOnClickListener(view ->
			toggleTermsAgreementCheckbox());
	}
	
	/**
	 * Hides all expanded detail sections of the terms and conditions.
	 * This method sets the visibility of each supplemental information view to {@link View#GONE},
	 * effectively collapsing all terms sections to their initial state.
	 */
	private void hideExpandedDetails() {
		binding.top2.extraAcceptance.setVisibility(View.GONE);
		binding.top2.extraUseOfApp.setVisibility(View.GONE);
		binding.top2.extraIntellectual.setVisibility(View.GONE);
		binding.top2.extraProhibited.setVisibility(View.GONE);
		binding.top2.extraLimitation.setVisibility(View.GONE);
		binding.top2.extraChanges.setVisibility(View.GONE);
	}
	
	/**
	 * Toggles the visibility of the specified view between {@link View#VISIBLE} and {@link View#GONE}.
	 * If the view is currently hidden (GONE), it will be made visible; otherwise, it will be hidden.
	 *
	 * @param targetView The view whose visibility state should be toggled.
	 */
	private void toggleVisibility(View targetView) {
		boolean isNoVisibility = targetView.getVisibility() == View.GONE;
		targetView.setVisibility(isNoVisibility ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Toggles the state of the terms and conditions agreement checkbox.
	 * If the checkbox is currently checked, it will be unchecked, and vice versa.
	 */
	private void toggleTermsAgreementCheckbox() {
		binding.top2.cbAgreeTerms.setChecked(!binding.top2.cbAgreeTerms.isChecked());
	}
	
	/**
	 * Determines whether this activity was launched from the opening/onboarding screen.
	 * It checks the intent extras for a specific launch location identifier.
	 *
	 * @return {@code true} if the activity was started from the opening screen or if no
	 * location was specified; {@code false} otherwise (e.g., from settings).
	 */
	private boolean isLaunchedFromOpeningScreen() {
		Intent intent = getIntent();
		short defaultIntentValue = LAUNCHED_FROM_OPENING_SCREEN;
		short launchLocation = intent.getShortExtra(LAUNCHED_LOCATION_OF_ACTIVITY, defaultIntentValue);
		return launchLocation == defaultIntentValue;
	}
}
