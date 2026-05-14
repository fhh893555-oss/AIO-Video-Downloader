package userInterface.fragments.homepage.headerSection;

import coreUtils.library.process.LoggerUtils;
import userInterface.fragments.homepage.HomepageFragment;

public class HomepageHeroSection {
    private final LoggerUtils logger = LoggerUtils.from(HomepageHeroSection.class);
    private HomepageFragment homepageFragment;

    public void initialize(HomepageFragment homepageFragment) {
        this.homepageFragment = homepageFragment;
    }
}
