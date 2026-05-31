package userInterface.mainScreen;

import androidx.lifecycle.ViewModel;

import coreUtils.library.process.LoggerUtils;

public class MainViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(MainViewModel.class);
	private NavigationTabs currentTabs;
	
	public NavigationTabs getCurrentTabs() {
		return currentTabs;
	}
	
	public void setCurrentTabs(NavigationTabs currentTabs) {
		this.currentTabs = currentTabs;
	}
}
