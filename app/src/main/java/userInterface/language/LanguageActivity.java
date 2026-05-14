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
import dataRepo.configs.AppConfigsRepo;
import userInterface.main.MainActivity;

public class LanguageActivity extends BaseActivity<ActivityLanguage1Binding> implements LanguageCallback {
    private final LoggerUtils logger = LoggerUtils.from(LanguageViewModel.class);
    private LanguageAdapter languageAdapter;

    @Override
    protected boolean shouldLockOrientation() {
        return true;
    }

    @Override protected ActivityLanguage1Binding inflateBinding(LayoutInflater inflater) {
        return ActivityLanguage1Binding.inflate(inflater);
    }

    @Override protected void onLoadedLayout() {
        LanguageViewModel languageViewModel = getLanguageViewModel();
        initViews(languageViewModel);
        initializeButtons();
    }

    @NonNull
    private LanguageViewModel getLanguageViewModel() {
        ViewModelProvider viewModelProvider = new ViewModelProvider(this);
        return viewModelProvider.get(LanguageViewModel.class);
    }

    private void initViews(LanguageViewModel languageViewModel) {
        setupRecyclerView();
        bindLanguageData(languageViewModel);
    }

    private void setupRecyclerView() {
        languageAdapter = new LanguageAdapter(this);
        binding.languagesRecyclerView.setAdapter(languageAdapter);
        GridLayoutManager manager = new GridLayoutManager(binding.getRoot().getContext(), 3);
        binding.languagesRecyclerView.setLayoutManager(manager);
        int spacingInPx = getResources().getDimensionPixelSize(R.dimen._5);
        binding.languagesRecyclerView.addItemDecoration(new GridLayoutSpacing(spacingInPx, true));
    }

    private void bindLanguageData(LanguageViewModel viewModel) {
        viewModel.getLanguages().observe(this, languages -> {
            if (languageAdapter != null) {
                languageAdapter.setLanguages(languages);
            }
        });
    }

    private void initializeButtons() {
        binding.skipLanguageButton.setOnClickListener(view -> {
            AppConfigsRepo.getConfig().isLocaleConfigured = true;
            AppConfigsRepo.getConfig().save();
            openHomepageActivity();
        });
    }

    @Override
    public void onLanguageSelected(LanguageItem languageItem) {
        String message = "Language selected: " + languageItem.getLanguageName() +
                " (" + languageItem.getLanguageCode() + ")";
        logger.info(message);
        AppConfigsRepo.getConfig().selectedLanguageCode = languageItem.getLanguageCode();
        AppConfigsRepo.getConfig().isLocaleConfigured = true;
        AppConfigsRepo.getConfig().save();
        LocaleHelper.changeLanguage(languageItem.getLanguageCode(), this);
        openHomepageActivity();
    }

    private void openHomepageActivity() {
        Intent intent = new Intent(LanguageActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        ActivityAnimator.animActivityFade(LanguageActivity.this);
        finish();
    }
}