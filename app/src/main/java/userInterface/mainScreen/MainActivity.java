package userInterface.mainScreen;

import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityMain1Binding;
import com.nextgen.databinding.ActivityMain1Tab1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.base.BaseFragment;
import coreUtils.library.process.FragNavigator;
import coreUtils.library.process.LoggerUtils;
import userInterface.fragmentsUIs.homepage.HomepageFragment;

public final class MainActivity extends BaseActivity<ActivityMain1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private FragNavigator fragNavigator;
	private MainViewModel mainViewModel;
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityMain1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityMain1Binding.inflate(inflater);
	}
	
	@Override protected void onLoadedLayout() {
		initMainViewModel();
		initFragmentNavigator();
		initBottomTabButtons();
		loadHomepage();
	}
	
	private void initMainViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		mainViewModel = viewModelProvider.get(MainViewModel.class);
	}
	
	private void initFragmentNavigator() {
		FragmentManager supportFragmentManager = getSupportFragmentManager();
		fragNavigator = new FragNavigator(supportFragmentManager, R.id.fragmentContainer);
	}
	
	private void loadHomepage() {
		ActivityMain1Tab1Binding bottomTabs = binding.bottomTabs;
		bottomTabs.btnHomeTab.callOnClick();
	}
	
	private void initBottomTabButtons() {
		ActivityMain1Tab1Binding bottomTabs = binding.bottomTabs;
		bottomTabs.btnHomeTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnHomeTab, NAVIGATION_TAB.HOME_TAB));
		
		bottomTabs.btnMusicTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnMusicTab, NAVIGATION_TAB.MUSIC_TAB));
		
		bottomTabs.btnMoviesTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnMoviesTab, NAVIGATION_TAB.MOVIES_TABS));
		
		bottomTabs.btnGamesTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnGamesTab, NAVIGATION_TAB.GAMES_TAB));
		
		bottomTabs.btnBrowserTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnBrowserTab, NAVIGATION_TAB.HOME_TAB));
		
		bottomTabs.btnFilesTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnFilesTab, NAVIGATION_TAB.HOME_TAB));
	}
	
	private void switchTab(@NonNull BaseFragment<?> fragment,
	                       @NonNull View activeTab,
	                       @NonNull NAVIGATION_TAB activeTabEnum) {
		fragNavigator.navigateTo(fragment, false);
		mainViewModel.setCurrentTabs(activeTabEnum);
		updateTabUI(activeTab);
	}
	
	private void updateTabUI(View activeTab) {
		ActivityMain1Tab1Binding bottomTabs = binding.bottomTabs;
		bottomTabs.btnHomeTab.setSelected(false);
		bottomTabs.btnMusicTab.setSelected(false);
		bottomTabs.btnMoviesTab.setSelected(false);
		bottomTabs.btnGamesTab.setSelected(false);
		bottomTabs.btnBrowserTab.setSelected(false);
		bottomTabs.btnFilesTab.setSelected(false);
		activeTab.setSelected(true);
	}
	
	public enum NAVIGATION_TAB {
		HOME_TAB, MUSIC_TAB, MOVIES_TABS, GAMES_TAB, BROWSER_TABS, DOWNLOADS_TAB
	}
}
