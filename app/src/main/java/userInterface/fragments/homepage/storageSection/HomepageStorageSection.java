package userInterface.fragments.homepage.storageSection;

import static coreUtils.library.storage.FileStorageUtility.hasFullFileSystemAccess;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.nextgen.databinding.FragHomepage1Binding;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.storage.FileStorageUtility;
import userInterface.fragments.homepage.HomepageFragment;
import userInterface.fragments.homepage.headerSection.HomepageHeroSection;

public class HomepageStorageSection {
    private final LoggerUtils logger = LoggerUtils.from(HomepageHeroSection.class);
    private HomepageFragment homepageFragment;
    private FragHomepage1Binding binding;

    public void initialize(HomepageFragment homepageFragment) {
        this.homepageFragment = homepageFragment;
        this.binding = homepageFragment.getBinding();
        setupStorageAccessBanner();
    }

    public void onFragmentResume() {
        setupStorageAccessBanner();
    }

    private void setupStorageAccessBanner() {
        try {
            View container = binding.homepageStoragePermissionCard;
            Context context = homepageFragment.getContext();
            boolean isStoragePermissionGranted = hasFullFileSystemAccess(context);
            if (isStoragePermissionGranted) container.setVisibility(View.GONE);
            else container.setVisibility(View.VISIBLE);

            TextView btnAllowStorage = binding.homepageStoragePermissionContent.homepageStoragePermissionAllowButton;
            btnAllowStorage.setOnClickListener(view -> {
                FileStorageUtility.openAllFilesAccessSettings(context);
            });
        } catch (Exception error) {
            logger.error("Failed to init storage permission button", error);
        }
    }
}
