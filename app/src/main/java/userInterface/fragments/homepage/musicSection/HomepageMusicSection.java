package userInterface.fragments.homepage.musicSection;

import android.content.Context;
import android.view.View;

import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.nextgen.R;
import com.nextgen.databinding.FragHomepage1Binding;
import com.nextgen.databinding.FragHomepage1Mc1Binding;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.GridLayoutSpacing;
import sysModules.vidRecEngine.api.RecommendationEngine;
import sysModules.vidRecEngine.models.enums.ContentSource;
import sysModules.vidRecEngine.models.enums.VideoCategory;
import userInterface.fragments.homepage.HomepageFragment;
import userInterface.fragments.homepage.HomepageViewModel;
import userInterface.fragments.homepage.headerSection.HomepageHeroSection;
import userInterface.main.MainViewModel;

public class HomepageMusicSection {
	private final LoggerUtils logger = LoggerUtils.from(HomepageHeroSection.class);
	private HomepageFragment homepageFragment;
	private FragHomepage1Binding binding;
	
	private YtStreamInfoAdapter adapter;
	
	public void initialize(HomepageFragment homepageFragment) {
		this.homepageFragment = homepageFragment;
		this.binding = homepageFragment.getBinding();
		
		configureMusicVideoGrid();
		loadFeaturedMusicVideos();
		
		HomepageMusicProvider homepageMusicProvider = new HomepageMusicProvider();
		homepageMusicProvider.loadMusicRecommendations(homepageFragment, adapter);
	}
	
	private void configureMusicVideoGrid() {
		FragHomepage1Mc1Binding featuredMusicContent = binding.homepageFeaturedMusicContent;
		RecyclerView featuredMusicVideoList = featuredMusicContent.homepageFeaturedMusicItemsContainer;
		GridLayoutManager manager = new GridLayoutManager(featuredMusicContent.getRoot().getContext(), 2);
		featuredMusicVideoList.setLayoutManager(manager);
		int spacingInPx = homepageFragment.getResources().getDimensionPixelSize(R.dimen._5);
		featuredMusicVideoList.addItemDecoration(new GridLayoutSpacing(spacingInPx, true));
	}
	
	private void loadFeaturedMusicVideos() {
		MainViewModel mainViewModel = homepageFragment.getMainViewModel();
		HomepageViewModel homepageViewModel = mainViewModel.getHomepageViewModel();
		adapter = new YtStreamInfoAdapter();
		View featuredMusicVideosContainer = binding.homepageFeaturedMusicCard;
		FragHomepage1Mc1Binding musicContentBinding = binding.homepageFeaturedMusicContent;
		musicContentBinding.homepageFeaturedMusicItemsContainer.setAdapter(adapter);
		
		LifecycleOwner lifecycleOwner = homepageFragment.getViewLifecycleOwner();
		adapter.setOnItemClickListener(item -> {
			logger.debug("Music video clicked: " + item.name + " Url: " + item.url);
			Context context = homepageFragment.getContext();
			RecommendationEngine recommendationEngine = BaseApplication.recommendationEngine;
			recommendationEngine.submitRecommendationClick(item.streamId, VideoCategory.MUSIC);
			recommendationEngine.submitWatchEvent(item.streamId, VideoCategory.MUSIC,
				item.duration, ContentSource.RECOMMENDED_TAB);
		});
	}
	
}
