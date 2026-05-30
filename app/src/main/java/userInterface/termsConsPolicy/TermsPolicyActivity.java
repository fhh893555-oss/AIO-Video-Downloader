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
import dataRepo.appConfigs.AppConfigs;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.openingSplash.OpeningActivity;

/**
 * Activity that displays the Terms & Conditions and Privacy Policy for user acceptance.
 * <p>
 * This activity presents the legal terms and policies that users must agree to before
 * using the application. It provides an expandable/collapsible interface for browsing
 * different sections of the terms, including acceptance terms, app usage guidelines,
 * intellectual property rights, prohibited activities, liability limitations, and
 * policy change notifications.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Expandable Sections:</b> Users can expand/collapse detailed policy sections
 *       by tapping on each category header</li>
 *   <li><b>Terms Acceptance:</b> Checkbox for users to indicate their agreement
 *       with all terms and conditions</li>
 *   <li><b>Smart Navigation:</b> Different behavior based on launch source
 *       (from opening screen vs. other screens)</li>
 *   <li><b>Persistent Agreement:</b> User's acceptance is saved via {@link AppConfigsRepo}
 *       and persists across app sessions</li>
 *   <li><b>Orientation Lock:</b> Screen orientation is locked to prevent layout
 *       issues during rotation</li>
 * </ul>
 * </p>
 *
 * <p><b>Launch Sources:</b>
 * The activity behaves differently based on the {@link #LAUNCHED_LOCATION_OF_ACTIVITY} extra:
 * <ul>
 *   <li><b>From Opening Screen:</b> After acceptance, proceeds to main application flow</li>
 *   <li><b>From Other Screens:</b> After acceptance, returns to previous screen</li>
 * </ul>
 * </p>
 *
 * <p><b>Behavior Flow:</b>
 * <ol>
 *   <li>User reads the terms and conditions</li>
 *   <li>User checks the agreement checkbox</li>
 *   <li>User clicks "Agree & Continue" button</li>
 *   <li>If checkbox checked: saves agreement status, navigates to appropriate destination</li>
 *   <li>If checkbox not checked: shows error toast, vibrates, and closes activity</li>
 * </ol>
 * </p>
 *
 * <p><b>UI Components:</b>
 * <ul>
 *   <li>Gradient-styled title "Terms & Conditions"</li>
 *   <li>6 expandable content sections</li>
 *   <li>Terms agreement checkbox</li>
 *   <li>"Agree & Continue" action button</li>
 *   <li>Back button for navigation without acceptance</li>
 * </ul>
 * </p>
 *
 * @see BaseActivity
 * @see AppConfigsRepo
 * @see ActivityTermsCon1Binding
 * @see TextViewsUtils
 */
public class TermsPolicyActivity extends BaseActivity<ActivityTermsCon1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	public static final String LAUNCHED_LOCATION_OF_ACTIVITY = "Location";
	public static final short LAUNCHED_FROM_OPENING_SCREEN = 1;
	public static final short LAUNCHED_FROM_SETTINGS_SCREEN = 2;
	
	/**
	 * Determines whether the activity should be locked to a specific orientation.
	 * <p>
	 * Overriding this method and returning {@code true} ensures that the terms and
	 * conditions screen maintains a consistent orientation (typically portrait),
	 * preventing layout inconsistencies during rotation and ensuring that expanded
	 * content panels remain in their correct state when the device is rotated.
	 * </p>
	 *
	 * @return {@code true} to lock the orientation; {@code false} otherwise
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Inflates the view binding for the Terms and Conditions activity.
	 * <p>
	 * This method initializes the {@link ActivityTermsCon1Binding} using the provided
	 * {@link LayoutInflater}, allowing for type-safe access to the layout's views
	 * including the back button, expandable section items, checkbox, and agree button.
	 * </p>
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the binding
	 * @return A new instance of {@link ActivityTermsCon1Binding}
	 */
	@Override
	protected ActivityTermsCon1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityTermsCon1Binding.inflate(inflater);
	}
	
	/**
	 * Initializes the activity's components once the layout has been loaded.
	 * <p>
	 * This method is called after the layout has been inflated and performs the
	 * following setup in sequence:
	 * </p>
	 * <ol>
	 *   <li>Hides all expandable detailed sections (starts with collapsed state)</li>
	 *   <li>Applies gradient styling to the "Conditions" portion of the title text</li>
	 *   <li>Sets up all button click listeners (back, expandable sections, checkbox, agree button)</li>
	 * </ol>
	 */
	@Override
	protected void onLoadedLayout() {
		hideExpandedDetails();
		applyGradientToTitle();
		setupButtonClickEvents();
	}
	
	/**
	 * Applies a color gradient span to the "Conditions" portion of the terms title text.
	 * <p>
	 * This method searches for the word "Conditions" within the title text. If found,
	 * it applies a linear gradient transition between the secondary color and primary
	 * variant color to that specific word using {@link TextViewsUtils#applyGradientSpan}.
	 * This creates a visually appealing gradient effect that highlights the key part
	 * of the "Terms & Conditions" title.
	 * </p>
	 *
	 * <p><b>Gradient Parameters:</b>
	 * <ul>
	 *   <li>Start color: R.color.color_secondary</li>
	 *   <li>End color: R.color.color_primary_variant</li>
	 *   <li>Span length: 10 characters ("Conditions")</li>
	 * </ul>
	 * </p>
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
	 * Sets up all button click listeners for the terms and conditions activity.
	 * <p>
	 * This method serves as a central configuration point for all interactive buttons
	 * on the terms screen. It delegates to individual setup methods for each button
	 * to maintain clean, organized, and maintainable code.
	 * </p>
	 *
	 * <p><b>Buttons Configured:</b>
	 * <ul>
	 *   <li>Back button - finishes the activity</li>
	 *   <li>Expandable section buttons - toggle detailed content visibility</li>
	 *   <li>Terms checkbox - toggles agreement state</li>
	 *   <li>Agree & Continue button - saves agreement and navigates accordingly</li>
	 * </ul>
	 * </p>
	 */
	private void setupButtonClickEvents() {
		setupBackButtonClickEvent();
		setupExpandTermsButtonClickEvent();
		configureTermsCheckbox();
		setupAgreeTermsButton();
	}
	
	/**
	 * Configures the click listener for the back button.
	 * <p>
	 * When clicked, this method simply finishes the current activity, returning the user
	 * to the previous screen without saving any changes to the terms agreement state.
	 * This allows users to exit the terms screen without accepting or rejecting the terms.
	 * </p>
	 */
	private void setupBackButtonClickEvent() {
		binding.btnBack.setOnClickListener(view -> finish());
	}
	
	/**
	 * Configures click listeners for all expandable terms and conditions sections.
	 * <p>
	 * This method sets up toggle behavior for each expandable item in the terms screen.
	 * When a user clicks on any of the section titles (Acceptance, Use of App,
	 * Intellectual Property, Prohibited Activities, Limitation of Liability, Changes),
	 * the corresponding detailed content panel toggles between visible and hidden states.
	 * </p>
	 *
	 * <p><b>Sections Configured:</b>
	 * <ul>
	 *   <li>itemAcceptance → extraAcceptance (Terms acceptance details)</li>
	 *   <li>itemUseOfApp → extraUseOfApp (App usage guidelines)</li>
	 *   <li>itemIntellectual → extraIntellectual (Intellectual property rights)</li>
	 *   <li>itemProhibited → extraProhibited (Prohibited actions list)</li>
	 *   <li>itemLimitation → extraLimitation (Liability limitations)</li>
	 *   <li>itemChanges → extraChanges (Terms modification policy)</li>
	 * </ul>
	 * </p>
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
	 * Configures the click listener for the "Agree & Continue" button.
	 * <p>
	 * This method handles the terms agreement workflow. When clicked, it saves the checkbox
	 * state to the AppConfig repository, then determines the appropriate action based on
	 * whether terms were accepted and where the activity was launched from.
	 * </p>
	 *
	 * <p><b>Behavior Matrix:</b>
	 * <ul>
	 *   <li><b>Terms Accepted + From Opening Screen:</b> Navigates to OpeningActivity (main flow)</li>
	 *   <li><b>Terms Accepted + Not From Opening Screen:</b> Simulates back button press</li>
	 *   <li><b>Terms Not Accepted:</b> Shows error toast, vibrates, and finishes the activity</li>
	 * </ul>
	 * </p>
	 */
	private void setupAgreeTermsButton() {
		binding.top2.btnAgreeContinue.setOnClickListener(view -> {
			AppConfigs config = AppConfigsRepo.getConfig();
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
	 * Configures the checkbox toggling behavior for terms agreement.
	 * <p>
	 * This method sets up a click listener on the checkbox area/button that calls
	 * {@link #toggleTermsAgreementCheckbox()} to invert the checkbox state. This provides
	 * an alternative touch target for users to agree to terms, improving usability by
	 * allowing them to tap either the checkbox or its surrounding container.
	 * </p>
	 */
	private void configureTermsCheckbox() {
		binding.top2.btnAcceptTermsCheck.setOnClickListener(view ->
			toggleTermsAgreementCheckbox());
	}
	
	/**
	 * Hides all expandable detailed sections of the terms and conditions.
	 * <p>
	 * This method sets the visibility of all detail panels to GONE, effectively
	 * collapsing any expanded content. These panels typically contain detailed
	 * information about acceptance terms, app usage rights, intellectual property,
	 * prohibited activities, limitation of liability, and changes to terms.
	 * </p>
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
	 * Toggles the visibility state of a target view between GONE and VISIBLE.
	 * <p>
	 * If the target view is currently GONE (hidden and not occupying layout space),
	 * this method makes it VISIBLE. If it is VISIBLE, it becomes GONE. This is useful
	 * for showing/hiding UI elements such as progress indicators, error messages,
	 * or optional form fields.
	 * </p>
	 *
	 * @param targetView The view whose visibility state will be toggled
	 */
	private void toggleVisibility(View targetView) {
		boolean isNoVisibility = targetView.getVisibility() == View.GONE;
		targetView.setVisibility(isNoVisibility ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Toggles the checked state of the terms agreement checkbox.
	 * <p>
	 * This method inverts the current checked state of the checkbox, allowing users
	 * to quickly toggle their agreement status without directly interacting with
	 * the checkbox itself (e.g., by tapping a label or button associated with it).
	 * </p>
	 */
	private void toggleTermsAgreementCheckbox() {
		binding.top2.cbAgreeTerms.setChecked(!binding.top2.cbAgreeTerms.isChecked());
	}
	
	/**
	 * Determines whether this activity was launched from the opening/splash screen.
	 * <p>
	 * This method reads the {@link #LAUNCHED_LOCATION_OF_ACTIVITY} extra from the
	 * intent that started this activity. It compares the value against the constant
	 * {@link #LAUNCHED_FROM_OPENING_SCREEN} to determine if the TermsPolicyActivity
	 * was opened from the splash screen (true) or from another source (false).
	 * </p>
	 *
	 * <p><b>Usage Context:</b>
	 * Different launch locations may require different behavior. For example, when
	 * launched from the opening screen, the activity should not allow navigating
	 * back without accepting terms, whereas other launch sources might be less strict.
	 * </p>
	 *
	 * @return true if the activity was launched from the opening screen, false otherwise
	 */
	private boolean isLaunchedFromOpeningScreen() {
		Intent intent = getIntent();
		short defaultIntentValue = LAUNCHED_FROM_OPENING_SCREEN;
		short launchLocation = intent.getShortExtra(
			LAUNCHED_LOCATION_OF_ACTIVITY, defaultIntentValue);
		
		return launchLocation == defaultIntentValue;
	}
}
