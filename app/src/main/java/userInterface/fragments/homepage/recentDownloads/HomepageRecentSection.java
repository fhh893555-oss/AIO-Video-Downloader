package userInterface.fragments.homepage.recentDownloads;

import android.view.View;
import android.widget.LinearLayout;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieComposition;
import com.nextgen.R;
import com.nextgen.databinding.FragHomepage1Binding;
import com.nextgen.databinding.FragHomepage1Rd1Binding;

import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.ViewsUtility;
import sysModules.interCaches.AppRawFiles;
import userInterface.fragments.homepage.HomepageFragment;
import userInterface.fragments.homepage.headerSection.HomepageHeroSection;

public final class HomepageRecentSection {
	private final LoggerUtils logger = LoggerUtils.from(HomepageHeroSection.class);
	private FragHomepage1Binding binding;
	
	public void initialize(HomepageFragment homepageFragment) {
		this.binding = homepageFragment.getBinding();
		setupEmptyDownloadsState();
	}
	
	private void setupEmptyDownloadsState() {
		FragHomepage1Rd1Binding downloadsContent = binding.homepageRecentDownloadsContent;
		LottieAnimationView animationView = downloadsContent.homepageRecentDownloadsEmptyImage;
		LinearLayout emptyContainer = downloadsContent.homepageRecentDownloadsEmptyContainer;
		View recycleDownloadItemsList = downloadsContent.homepageRecentDownloadsRecycleView;
		updateEmptyState(emptyContainer, recycleDownloadItemsList, animationView);
	}
	
	private void updateEmptyState(LinearLayout downloadsEmptyContainer,
	                              View recycleDownloadItemsList,
	                              LottieAnimationView animationView) {
		if (isRecentDownloadsEmpty()) {
			ViewsUtility.showView(downloadsEmptyContainer);
			ViewsUtility.hideView(recycleDownloadItemsList);
			
			loadLottieAnimation(animationView);
		} else {
			ViewsUtility.hideView(downloadsEmptyContainer);
			ViewsUtility.showView(recycleDownloadItemsList);
			animationView.clearAnimation();
		}
	}
	
	private static void loadLottieAnimation(LottieAnimationView animationView) {
		AppRawFiles appRawFiles = AppRawFiles.getInstance();
		LottieComposition composition = appRawFiles.getNoResultEmptyComposition();
		if (composition == null) animationView.setAnimation(R.raw.animation_no_result);
		else animationView.setComposition(composition);
	}
	
	private boolean isRecentDownloadsEmpty() {
		return true; //todo: implement the logic here.
	}
}
