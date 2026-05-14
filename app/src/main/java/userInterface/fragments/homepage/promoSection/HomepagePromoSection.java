package userInterface.fragments.homepage.promoSection;

import android.content.Context;
import android.view.View;

import com.nextgen.databinding.FragHomepage1Binding;
import com.nextgen.databinding.FragHomepage1Ph1Binding;

import coreUtils.library.process.IntentLinkHelper;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.CarouselImageView;
import coreUtils.library.views.ViewsUtility;
import userInterface.fragments.homepage.HomepageFragment;
import userInterface.fragments.homepage.HomepageViewModel;
import userInterface.fragments.homepage.headerSection.HomepageHeroSection;

public class HomepagePromoSection {
    private final LoggerUtils logger = LoggerUtils.from(HomepageHeroSection.class);
    private HomepageFragment homepageFragment;
    private FragHomepage1Binding binding;

    public void initialize(HomepageFragment homepageFragment) {
        this.homepageFragment = homepageFragment;
        this.binding = homepageFragment.getBinding();
        setupPromoCarousel();
    }

    private void setupPromoCarousel() {
        FragHomepage1Ph1Binding promotionSliderContent = binding.homepagePromotionSliderContent;
        CarouselImageView carouselImageView = promotionSliderContent.homepagePromotionSlider;
        View promoSliderContainer = promotionSliderContent.homepagePromotionSlider;
        
        ViewsUtility.hideView(promoSliderContainer);
        carouselImageView.setAutoScrollEnabled(false);
        Context context = homepageFragment.getContext();
        HomepageViewModel homepageViewModel = homepageFragment.getMainViewModel().getHomepageViewModel();
        PromoAdsProvider promoAdsProvider = new PromoAdsProvider();
        promoAdsProvider.getPromoHeaderAdsLiveData().observe(homepageFragment.getViewLifecycleOwner(), promoAdInfos -> {
            if (promoAdInfos == null || promoAdInfos.isEmpty()) {
                ViewsUtility.hideView(promoSliderContainer);
                promoSliderContainer.setOnClickListener(null);
            } else {
                ViewsUtility.showView(promoSliderContainer);
                carouselImageView.loadImages(promoAdsProvider.getPromoHeaderAdImages());
                carouselImageView.setOnImageClickListener((index, url) -> {
                    String clickUrl = promoAdInfos.get(index).clickUrl;
                    logger.debug("Promo image clicked, " + "index: " + index + ", url: " + clickUrl);
                    IntentLinkHelper.openLinkInSystemBrowser(context, clickUrl);
                });
            }
        });
    }

}
