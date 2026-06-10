package userInterface;

import android.view.LayoutInflater;

import com.nextgen.databinding.ActivityAppCrashed2Binding;

import coreUtils.base.BaseActivity;

public class DemoActivity extends BaseActivity<ActivityAppCrashed2Binding> {

    @Override
    protected boolean shouldLockOrientation() {
        return true;
    }

    @Override
    protected ActivityAppCrashed2Binding inflateBinding(LayoutInflater inflater) {
        return ActivityAppCrashed2Binding.inflate(inflater);
    }

    @Override
    protected void onLoadedLayout() {

    }
}
