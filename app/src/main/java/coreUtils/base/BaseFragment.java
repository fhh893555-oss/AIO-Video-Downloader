package coreUtils.base;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {

    protected VB binding;
    protected BaseApplication application;
    protected Context context;

    protected abstract VB inflateBinding(LayoutInflater inflater, ViewGroup container);
    protected abstract void onLoadedLayout();

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
        Context applicationContext = context.getApplicationContext();
        if (applicationContext instanceof BaseApplication) {
            application = (BaseApplication) applicationContext;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = inflateBinding(inflater, container);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onLoadedLayout();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        context = null;
        binding = null;
    }

    public VB getBinding() {
        return binding;
    }

    @Nullable
    protected Bundle getArgumentsSafe() {
        return getArguments();
    }

    protected boolean isFragmentAlive() {
        return isAdded() && getActivity() != null && !isDetached();
    }

    protected Context requireSafeContext() {
        return requireContext();
    }

    protected BaseApplication getApp() {
        return application;
    }


}