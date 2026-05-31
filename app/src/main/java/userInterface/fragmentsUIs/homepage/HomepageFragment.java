package userInterface.fragmentsUIs.homepage;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.databinding.FragHomepage1Binding;

import coreUtils.base.BaseFragment;
import userInterface.mainScreen.MainViewModel;

public class HomepageFragment extends BaseFragment<FragHomepage1Binding> {
	
	@Override protected FragHomepage1Binding inflateBinding(LayoutInflater inflater,
	                                                        ViewGroup container) {
		return FragHomepage1Binding.inflate(inflater, container, false);
	}
	
	@Override protected void onLoadedLayout() {
	
	}
	
	public HomepageViewModel getHomepageViewModel() {
		ViewModelProvider viewModelProvider = new ViewModelProvider(this);
		return viewModelProvider.get(HomepageViewModel.class);
	}
	
	public MainViewModel getMainViewModel() {
		FragmentActivity owner = requireActivity();
		ViewModelProvider viewModelProvider = new ViewModelProvider(owner);
		return viewModelProvider.get(MainViewModel.class);
	}
}
