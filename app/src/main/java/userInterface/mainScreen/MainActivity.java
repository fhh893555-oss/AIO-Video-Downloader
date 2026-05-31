package userInterface.mainScreen;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityMain1Binding;
import com.nextgen.databinding.ActivityMain1Tab1Binding;

import org.jetbrains.annotations.NotNull;

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
		FragmentManager frgManager = getSupportFragmentManager();
		fragNavigator = new FragNavigator(frgManager, R.id.fragmentContainer);
	}
	
	private void loadHomepage() {
		ActivityMain1Tab1Binding bottomTabs = binding.bottomTabs;
		bottomTabs.btnHomeTab.callOnClick();
	}
	
	private void initBottomTabButtons() {
		ActivityMain1Tab1Binding bottomTabs = binding.bottomTabs;
		bottomTabs.btnHomeTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnHomeTab, bottomTabs.imgHomeTab,
				bottomTabs.tvHomeTab, NavigationTabs.HOME_TAB));
		
		bottomTabs.btnMusicTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnMusicTab, bottomTabs.imgMusicTab,
				bottomTabs.tvMusicsTab, NavigationTabs.MUSIC_TAB));
		
		bottomTabs.btnMoviesTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnMoviesTab, bottomTabs.imgMoviesTab,
				bottomTabs.tvMoviesTab, NavigationTabs.MOVIES_TABS));
		
		bottomTabs.btnGamesTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnGamesTab, bottomTabs.imgGamesTab,
				bottomTabs.tvGamesTab, NavigationTabs.GAMES_TAB));
		
		bottomTabs.btnBrowserTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnBrowserTab, bottomTabs.imgBrowserTab,
				bottomTabs.tvBrowserTab, NavigationTabs.HOME_TAB));
		
		bottomTabs.btnFilesTab.setOnClickListener(view ->
			switchTab(new HomepageFragment(),
				bottomTabs.btnFilesTab, bottomTabs.imgFilesTab,
				bottomTabs.tvFilesTab, NavigationTabs.HOME_TAB));
	}
	
	private void switchTab(@NonNull BaseFragment<?> fragment,
	                       @NonNull View btnTab,
	                       @NotNull ImageView imgTab,
	                       @NotNull TextView tvTab,
	                       @NonNull NavigationTabs activeTabEnum) {
		fragNavigator.navigateTo(fragment, false);
		mainViewModel.setCurrentTabs(activeTabEnum);
		updateTabUI(btnTab, imgTab, tvTab);
	}
	
	private void updateTabUI(@NotNull View activeBtnTab,
	                         @NotNull ImageView activeImgTab,
	                         @NotNull TextView activeTvTab) {
		ActivityMain1Tab1Binding bottomTabs = binding.bottomTabs;
		bottomTabs.btnHomeTab.setSelected(false);
		bottomTabs.btnMusicTab.setSelected(false);
		bottomTabs.btnMoviesTab.setSelected(false);
		bottomTabs.btnGamesTab.setSelected(false);
		bottomTabs.btnBrowserTab.setSelected(false);
		bottomTabs.btnFilesTab.setSelected(false);
		
		Typeface regularFont = ResourcesCompat
			.getFont(this, R.font.font_family_regular);
		
		Typeface semiBoldFont = ResourcesCompat
			.getFont(this, R.font.font_family_semibold);
		
		bottomTabs.tvHomeTab.setTypeface(regularFont);
		bottomTabs.tvMusicsTab.setTypeface(regularFont);
		bottomTabs.tvMoviesTab.setTypeface(regularFont);
		bottomTabs.tvGamesTab.setTypeface(regularFont);
		bottomTabs.tvBrowserTab.setTypeface(regularFont);
		bottomTabs.tvFilesTab.setTypeface(regularFont);
		
		activeTvTab.setTypeface(semiBoldFont);
		activeBtnTab.setSelected(true);
	}
	
}
