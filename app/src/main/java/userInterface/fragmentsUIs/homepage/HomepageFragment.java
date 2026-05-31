package userInterface.fragmentsUIs.homepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.databinding.FragHomepage1Binding;

import coreUtils.base.BaseFragment;
import userInterface.mainScreen.MainActivity;
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
	
	/**
	 * Called when the view previously created by {@code onCreateView()} is detached
	 * from the fragment. This method cleans up the scroll change listener from the
	 * parent {@link MainActivity} to prevent memory leaks and ensure that scroll-triggered
	 * UI changes (e.g., bottom navigation hide/show) do not occur after this fragment's
	 * view has been destroyed.
	 *
	 * <p>The cleanup is performed only if the hosting activity is an instance of
	 * {@link MainActivity}, as that activity provides the {@code cleanupScrollListener()}
	 * method. After cleanup, the listener reference is removed, allowing proper
	 * garbage collection.
	 *
	 * @see #onCreateView(LayoutInflater, ViewGroup, Bundle)
	 * @see MainActivity#cleanupScrollListener(NestedScrollView)
	 */
	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (getActivity() instanceof MainActivity) {
			((MainActivity) getActivity())
				.cleanupScrollListener(binding.NestedScrollView);
		}
	}
}
