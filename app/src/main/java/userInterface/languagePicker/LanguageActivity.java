package userInterface.languagePicker;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.radiobutton.MaterialRadioButton;
import com.nextgen.R;
import com.nextgen.databinding.ActivityLanguage0Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LocaleHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ActivityAnimator;
import dataRepo.appConfigs.AppConfigsRepo;
import userInterface.mainScreen.MainActivity;

public final class LanguageActivity extends BaseActivity<ActivityLanguage0Binding> {

    private final LoggerUtils logger = LoggerUtils.from(getClass());

    @Override
    protected boolean shouldLockOrientation() {
        return true;
    }

    @Override
    protected ActivityLanguage0Binding inflateBinding(LayoutInflater inflater) {
        return ActivityLanguage0Binding.inflate(inflater);
    }


    @Override
    protected void onLoadedLayout() {
        populateLanguages(getLanguageViewModel());
        setupContinueButton();
        setupSkipButton();
    }

    @NonNull
    private LanguageViewModel getLanguageViewModel() {
        ViewModelProvider provider = new ViewModelProvider(this);
        return provider.get(LanguageViewModel.class);
    }

    private void populateLanguages(LanguageViewModel viewModel) {
        viewModel.getLanguages().observe(this, languages -> {
            RadioGroup group = binding.languagesContainer.languageRadioGroup;
            group.removeAllViews();

            LayoutInflater inflater = LayoutInflater.from(this);
            for (int i = 0; i < languages.size(); i++) {
                LanguageItem item = languages.get(i);
                MaterialRadioButton radioButton = (MaterialRadioButton)
                        inflater.inflate(R.layout.activity_language_0_p3, group, false);
                radioButton.setText(item.getLanguageName());
                radioButton.setTag(item);
                group.addView(radioButton);
            }

            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child instanceof MaterialRadioButton radioButton) {
                    if (child.getTag() != null) {
                        String langTag = ((LanguageItem) child.getTag()).getLanguageCode();
                        String appLanguage = AppConfigsRepo.getConfig().selectedLanguageCode;
                        if (langTag.equals(appLanguage)) {
                            child.performClick();
                        }
                    }
                }
            }
        });
    }

    private void setupContinueButton() {
        binding.buttons.btnContinue.setOnClickListener(view -> {
            RadioGroup group = binding.languagesContainer.languageRadioGroup;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child instanceof MaterialRadioButton radioButton) {
                    if (radioButton.isChecked()) {
                        LanguageItem item = (LanguageItem) radioButton.getTag();
                        applyLanguage(item);
                        return;
                    }
                }
            }
        });
    }

    private void setupSkipButton() {
        binding.buttons.btnSkipForNow.setOnClickListener(view -> {
            AppConfigsRepo.getConfig().isLocaleConfigured = true;
            AppConfigsRepo.getConfig().save();
            openNextActivity();
        });
    }

    private void applyLanguage(LanguageItem item) {
        logger.debug("Language selected: " + item.getLanguageName()
                + " (" + item.getLanguageCode() + ")");
        AppConfigsRepo.getConfig().selectedLanguageCode = item.getLanguageCode();
        AppConfigsRepo.getConfig().isLocaleConfigured = true;
        AppConfigsRepo.getConfig().save();
        LocaleHelper.changeLanguage(item.getLanguageCode(), this);
        openNextActivity();
    }


    private void openNextActivity() {
        Intent intent = new Intent(LanguageActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        ActivityAnimator.animActivityFade(LanguageActivity.this);
        finish();
    }
}
