package userInterface.termsPolicy;

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
import userInterface.opening.OpeningActivity;

public class TermsPolicyActivity extends BaseActivity<ActivityTermsCon1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	public static final String LAUNCHED_LOCATION_OF_ACTIVITY = "Location";
	public static final short LAUNCHED_FROM_OPENING_SCREEN = 1;
	public static final short LAUNCHED_FROM_SETTINGS_SCREEN = 2;
	
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityTermsCon1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityTermsCon1Binding.inflate(inflater);
	}
	
	@Override
	protected void onLoadedLayout() {
		hideExpandedDetails();
		applyGradientToTitle();
		setupButtonClickEvents();
	}
	
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
	
	private void setupButtonClickEvents() {
		setupBackButtonClickEvent();
		setupExpandTermsButtonClickEvent();
		configureTermsCheckbox();
		setupAgreeTermsButton();
	}
	
	private void setupBackButtonClickEvent() {
		binding.btnBack.setOnClickListener(view -> finish());
	}
	
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
	
	private void configureTermsCheckbox() {
		binding.top2.btnAcceptTermsCheck.setOnClickListener(view ->
			toggleTermsAgreementCheckbox());
	}
	
	private void hideExpandedDetails() {
		binding.top2.extraAcceptance.setVisibility(View.GONE);
		binding.top2.extraUseOfApp.setVisibility(View.GONE);
		binding.top2.extraIntellectual.setVisibility(View.GONE);
		binding.top2.extraProhibited.setVisibility(View.GONE);
		binding.top2.extraLimitation.setVisibility(View.GONE);
		binding.top2.extraChanges.setVisibility(View.GONE);
	}
	
	private void toggleVisibility(View targetView) {
		boolean isNoVisibility = targetView.getVisibility() == View.GONE;
		targetView.setVisibility(isNoVisibility ? View.VISIBLE : View.GONE);
	}
	
	private void toggleTermsAgreementCheckbox() {
		binding.top2.cbAgreeTerms.setChecked(!binding.top2.cbAgreeTerms.isChecked());
	}
	
	private boolean isLaunchedFromOpeningScreen() {
		Intent intent = getIntent();
		short defaultIntentValue = LAUNCHED_FROM_OPENING_SCREEN;
		short launchLocation = intent.getShortExtra(LAUNCHED_LOCATION_OF_ACTIVITY, defaultIntentValue);
		return launchLocation == defaultIntentValue;
	}
}
