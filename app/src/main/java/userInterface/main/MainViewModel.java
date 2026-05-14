package userInterface.main;

import androidx.lifecycle.ViewModel;

import coreUtils.library.process.LoggerUtils;
import userInterface.fragments.homepage.HomepageViewModel;
import userInterface.main.MainActivity.NAVIGATION_TAB;

public class MainViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(MainViewModel.class);
	private final HomepageViewModel homepageViewModel = new HomepageViewModel();
	
	private NAVIGATION_TAB currentTabs;
	
	public NAVIGATION_TAB getCurrentTabs() {
		return currentTabs;
	}
	
	public void setCurrentTabs(NAVIGATION_TAB currentTabs) {
		this.currentTabs = currentTabs;
	}
	
	public HomepageViewModel getHomepageViewModel() {
		return homepageViewModel;
	}
}