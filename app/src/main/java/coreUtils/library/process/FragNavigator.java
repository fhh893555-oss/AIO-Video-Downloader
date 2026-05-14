package coreUtils.library.process;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import coreUtils.base.BaseFragment;

public class FragNavigator {

    private final FragmentManager fragmentManager;
    private final @IdRes int containerId;

    public FragNavigator(FragmentManager fragmentManager, @IdRes int containerId) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
    }

    public void navigateTo(BaseFragment<?> fragment, boolean addToBackStack) {
        String tag = fragment.getClass().getSimpleName();
        Fragment currentFragment = fragmentManager.findFragmentById(containerId);
        if (currentFragment != null) {
            Class<? extends Fragment> currentFragmentClass = currentFragment.getClass();
            if (currentFragmentClass.equals(fragment.getClass())) return;
        }

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
        transaction.replace(containerId, fragment, tag);
        if (addToBackStack) {
            transaction.addToBackStack(tag);
        }

        transaction.commit();
    }
}