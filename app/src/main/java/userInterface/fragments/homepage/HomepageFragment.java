package userInterface.fragments.homepage;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.databinding.FragHomepage1Binding;

import coreUtils.base.BaseFragment;
import coreUtils.library.process.LoggerUtils;
import userInterface.fragments.homepage.headerSection.HomepageHeroSection;
import userInterface.fragments.homepage.musicSection.HomepageMusicSection;
import userInterface.fragments.homepage.promoSection.HomepagePromoSection;
import userInterface.fragments.homepage.recentDownloads.HomepageRecentSection;
import userInterface.fragments.homepage.storageSection.HomepageStorageSection;
import userInterface.main.MainViewModel;

public class HomepageFragment extends BaseFragment<FragHomepage1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(HomepageFragment.class);
	private HomepageStorageSection homepageStorageSection;
	
	@Override protected FragHomepage1Binding inflateBinding(
		LayoutInflater inflater, ViewGroup container) {
		return FragHomepage1Binding.inflate(inflater, container, false);
	}
	
	@Override protected void onLoadedLayout() {
		MainViewModel mainViewModel = getMainViewModel();
		
		new HomepageHeroSection().initialize(this);
		new HomepagePromoSection().initialize(this);
		
		new HomepageRecentSection().initialize(this);
		new HomepageMusicSection().initialize(this);
		
		homepageStorageSection = new HomepageStorageSection();
		homepageStorageSection.initialize(this);
	}
	
	@Override public void onResume() {
		super.onResume();
		homepageStorageSection.onFragmentResume();
	}
	
	public MainViewModel getMainViewModel() {
		FragmentActivity owner = requireActivity();
		ViewModelProvider viewModelProvider = new ViewModelProvider(owner);
		return viewModelProvider.get(MainViewModel.class);
	}
	
}
