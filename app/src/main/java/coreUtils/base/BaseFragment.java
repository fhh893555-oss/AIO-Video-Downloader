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

/**
 * Abstract base class for Fragments that utilizes View Binding to reduce boilerplate code.
 *
 * <p>This class provides a structured way to initialize layouts, manage lifecycle-safe
 * references to {@link ViewBinding}, and access application-level resources. Subclasses
 * must implement {@link #inflateBinding} and {@link #onLoadedLayout}.</p>
 *
 * <p>The class handles the nulling of the binding reference in {@link #onDetach()}
 * to prevent memory leaks and provides utility methods for safe context and fragment
 * state management.</p>
 *
 * @param <VB> The specific {@link ViewBinding} type associated with the fragment's layout.
 */
public abstract class BaseFragment<VB extends ViewBinding> extends Fragment {
	
	protected BaseApplication application;
	protected Context context;
	protected VB binding;
	
	/**
	 * Inflates the {@link ViewBinding} instance for this fragment's layout.
	 * <p>
	 * Subclasses must implement this method to provide the specific binding class
	 * (e.g., {@code FragmentExampleBinding.inflate(inflater, container, false)}).
	 *
	 * @param inflater  The LayoutInflater object that can be used to inflate any views in the fragment.
	 * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
	 * @return The created {@link ViewBinding} instance.
	 */
	protected abstract VB inflateBinding(LayoutInflater inflater, ViewGroup container);
	
	/**
	 * Called after the layout has been inflated and the view hierarchy has been created.
	 * <p>
	 * Subclasses should implement this method to perform view initialization, such as
	 * setting up click listeners, configuring adapters, or observing ViewModel data.
	 * At this point, the {@link #binding} is guaranteed to be initialized and ready for use.
	 * </p>
	 */
	protected abstract void onLoadedLayout();
	
	/**
	 * Called when the fragment is first attached to its context.
	 * This implementation initializes the local {@code context} reference and attempts
	 * to cast the application context to {@link BaseApplication} for easier access
	 * to global application state.
	 *
	 * @param context The context to which the fragment is being attached.
	 */
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		this.context = context;
		Context applicationContext = context.getApplicationContext();
		if (applicationContext instanceof BaseApplication) {
			application = (BaseApplication) applicationContext;
		}
	}
	
	/**
	 * Called to have the fragment instantiate its user interface view. This implementation
	 * initializes the {@link ViewBinding} by calling {@link #inflateBinding}  and returns
	 * the root view of the binding.
	 *
	 * @param inflater           The LayoutInflater object that can be used to inflate any
	 *                           views in the fragment.
	 * @param container          If non-null, this is the parent view that the fragment's
	 *                           UI should be attached to.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed from a
	 *                           previous saved state.
	 * @return The {@link View} for the fragment's UI, or {@code null}.
	 */
	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater,
	                         @Nullable ViewGroup container,
	                         @Nullable Bundle savedInstanceState) {
		binding = inflateBinding(inflater, container);
		return binding.getRoot();
	}
	
	/**
	 * Called immediately after {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)} has returned,
	 * but before any saved state has been restored in to the view.
	 * This implementation calls {@link #onLoadedLayout()} to allow subclasses to perform
	 * view initialization.
	 *
	 * @param view               The View returned by {@link #onCreateView(LayoutInflater, ViewGroup, Bundle)}.
	 * @param savedInstanceState If non-null, this fragment is being re-constructed
	 *                           from a previous saved state as given here.
	 */
	@Override
	public void onViewCreated(@NonNull View view,
	                          @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		onLoadedLayout();
	}
	
	/**
	 * Called when the fragment is no longer attached to its activity.
	 * This implementation clears the references to the {@code context} and {@code binding}
	 * to prevent potential memory leaks.
	 */
	@Override
	public void onDetach() {
		super.onDetach();
		context = null;
		binding = null;
	}
	
	/**
	 * Retrieves the {@link ViewBinding} instance associated with this fragment's layout.
	 * <p>
	 * This instance is initialized in {@link #onCreateView} and cleared in {@link #onDetach}
	 * to prevent memory leaks.
	 *
	 * @return The {@link ViewBinding} instance, or {@code null} if the view has not been
	 * created or has already been destroyed.
	 */
	public VB getBinding() {
		return binding;
	}
	
	/**
	 * Retrieves the arguments bundle passed to this fragment, if any. This method provides
	 * a null-safe way to access fragment arguments, returning {@code null} if no arguments
	 * were supplied.
	 *
	 * @return The {@link Bundle} of arguments provided to this fragment, or {@code null}
	 * if none exist.
	 */
	@Nullable
	protected Bundle getArgumentsSafe() {
		return getArguments();
	}
	
	/**
	 * Checks whether the fragment is currently attached to an activity and in a valid state.
	 * Use this method before performing UI updates or context-dependent operations,
	 * especially after asynchronous callbacks, to prevent crashes.
	 *
	 * @return {@code true} if the fragment is added, has a valid activity, and is not detached;
	 * {@code false} otherwise.
	 */
	protected boolean isFragmentAlive() {
		return isAdded() && getActivity() != null && !isDetached();
	}
	
	/**
	 * Returns the non-null {@link Context} this fragment is currently associated with.
	 * <p>
	 * This method is a wrapper around {@link #requireContext()} to ensure a valid context is
	 * available, throwing an exception if the fragment is not currently attached.
	 *
	 * @return The {@link Context} to which this fragment is attached.
	 * @throws IllegalStateException If the fragment is not currently attached to a context.
	 */
	protected Context requireSafeContext() {
		return requireContext();
	}
	
	/**
	 * Retrieves the custom {@link BaseApplication} instance associated with this fragment.
	 *
	 * @return the {@link BaseApplication} instance, or {@code null} if the application
	 * context does not inherit from BaseApplication.
	 */
	protected BaseApplication getApp() {
		return application;
	}
}