package coreUtils.library.views;

import static android.view.LayoutInflater.from;

import android.app.AlertDialog;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

/**
 * Builder class for creating custom-styled dialogs with a consistent, modern design.
 * <p>
 * This builder provides a fluent API for constructing dialogs with custom layouts,
 * images, buttons, animations, and positioning. It manages weak references to the
 * parent activity and dialog views to prevent memory leaks, and automatically handles
 * cleanup when the dialog is dismissed.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Custom Content:</b> Set any layout or view as the dialog's content area</li>
 *   <li><b>Dialog Image:</b> Add an optional image at the top with customizable width</li>
 *   <li><b>Button Configuration:</b> Configure positive button text and click behavior</li>
 *   <li><b>Positioning:</b> Position dialog at bottom with slide-up animation support</li>
 *   <li><b>Visual Effects:</b> Background blur (API 31+) or dim effect for older versions</li>
 *   <li><b>Lifecycle Safety:</b> Weak references and activity state checks prevent crashes</li>
 * </ul>
 * </p>
 *
 * <p><b>Layout Components (R.layout.dialog_stylized_window_1):</b>
 * <ul>
 *   <li>R.id.imgDialog - Optional image view at the top</li>
 *   <li>R.id.mainContent - Container for custom content views</li>
 *   <li>R.id.tvDialogRightBtn - Positive action button (right side)</li>
 *   <li>R.id.btnCloseDialog - Close button (typically X icon)</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * new StylizedDialogBuilder(activity)
 *     .setDialogImage(R.drawable.ic_success, R.dimen.dialog_image_width)
 *     .setCustomContentView(R.layout.dialog_custom_content)
 *     .setPositiveButtonText(R.string.btn_confirm)
 *     .setOnPositiveClickListener(v -> handleConfirm(), true)
 *     .applyBottomPositioning()
 *     .show();
 * </pre>
 * </p>
 *
 * @see AlertDialog
 * @see WeakReference
 * @see R.layout#dialog_stylized_window_1
 */
public class StylizedDialogBuilder {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	private WeakReference<LinearLayout> weakMainContentRef;
	private WeakReference<View> weakCustomChildViewRef;
	
	/**
	 * Constructs a new StylizedDialogBuilder for creating custom-styled dialogs.
	 * <p>
	 * This constructor initializes the dialog builder with a reference to the parent activity,
	 * inflates the base dialog layout, and sets up default behavior including auto-closing
	 * when the dialog is dismissed or the close button is clicked.
	 * </p>
	 *
	 * @param activity the parent activity used for context and view inflation
	 */
	public StylizedDialogBuilder(@NonNull BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	/**
	 * Initializes the base dialog layout and configures default dialog settings.
	 * <p>
	 * This method inflates the dialog layout, finds the main content container, creates
	 * the AlertDialog instance, and sets up default listeners for dismiss events and
	 * the close button. It safely checks that the activity is still valid before proceeding.
	 * </p>
	 */
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		ViewGroup root = activity.findViewById(android.R.id.content);
		dialogRootView = from(activity).inflate(R.layout.dialog_stylized_window_1, root, false);
		LinearLayout mainContent = dialogRootView.findViewById(R.id.mainContent);
		this.weakMainContentRef = new WeakReference<>(mainContent);
		
		int styledResId = R.style.style_dialog_window;
		AlertDialog.Builder nativeBuilder = new AlertDialog.Builder(activity, styledResId);
		nativeBuilder.setView(dialogRootView);
		this.alertDialog = nativeBuilder.create();
		
		this.alertDialog.setOnDismissListener(dialogInterface -> close());
		View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
		if (btnClose != null) {
			btnClose.setOnClickListener(v -> close());
		}
	}
	
	/**
	 * Sets a custom content view for the dialog using a layout resource ID.
	 * <p>
	 * This method inflates the specified layout resource and adds it as the content
	 * of the dialog, replacing any existing custom content. The inflated view is
	 * stored in a weak reference for later access via {@link #getCustomContentView()}.
	 * </p>
	 *
	 * @param childLayoutResId the layout resource ID to inflate as dialog content
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setCustomContentView(@LayoutRes int childLayoutResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		LinearLayout container = weakMainContentRef != null ? weakMainContentRef.get() : null;
		
		if (activity != null && container != null) {
			container.removeAllViews();
			View childView = from(activity).inflate(childLayoutResId, container, true);
			this.weakCustomChildViewRef = new WeakReference<>(childView);
		}
		return this;
	}
	
	/**
	 * Sets a custom content view for the dialog using a pre-inflated View.
	 * <p>
	 * This method takes an existing View and adds it as the content of the dialog,
	 * replacing any existing custom content. The provided view is stored in a weak
	 * reference for later access. This overload is useful when the content view
	 * requires complex configuration before being added to the dialog.
	 * </p>
	 *
	 * @param childView the pre-inflated View to use as dialog content
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setCustomContentView(@NonNull View childView) {
		LinearLayout container = weakMainContentRef != null ? weakMainContentRef.get() : null;
		if (container != null) {
			container.removeAllViews();
			container.addView(childView);
			this.weakCustomChildViewRef = new WeakReference<>(childView);
		}
		return this;
	}
	
	/**
	 * Sets whether the dialog can be canceled by user interactions.
	 * <p>
	 * This method controls two behaviors: whether the back button dismisses the dialog
	 * and whether touching outside the dialog area dismisses it. When cancelable is true,
	 * both actions will dismiss the dialog; when false, the user must use the provided
	 * buttons to close it.
	 * </p>
	 *
	 * @param cancelable true to allow cancellation via back button or outside touch,
	 *                   false to prevent both
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
	/**
	 * Enables a blur effect behind the dialog window for visual separation.
	 * <p>
	 * This method applies a blur effect to the content behind the dialog, making the
	 * background appear frosted or blurred. On Android S (API 31) and above, it uses
	 * the native blur API with configurable radius. On older versions, it falls back
	 * to a dim effect (60% dark overlay) as blur is not supported.
	 * </p>
	 *
	 * <p><b>Behavior by Android Version:</b>
	 * <ul>
	 *   <li>Android 12+ (API 31+): Native blur with specified radius</li>
	 *   <li>Android 11 and below: Dim background with 60% opacity fallback</li>
	 * </ul>
	 * </p>
	 *
	 * @param blurRadius the radius of the blur effect in pixels (API 31+ only)
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder enableBackgroundBlur(int blurRadius) {
		if (alertDialog == null) return this;
		
		Window window = alertDialog.getWindow();
		if (window != null) {
			window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				window.getAttributes().setBlurBehindRadius(blurRadius);
			} else {
				window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
				window.getAttributes().dimAmount = 0.6f;
			}
		}
		return this;
	}
	
	/**
	 * Applies a complete bottom-positioned dialog configuration.
	 * <p>
	 * This convenience method combines two common configurations for bottom-sheet style
	 * dialogs: positioning the dialog at the bottom of the screen and enabling a
	 * slide-up animation. This creates a consistent "sheet" appearance typical of
	 * modern mobile UI patterns.
	 * </p>
	 *
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
	}
	
	/**
	 * Sets a custom animation for the dialog window when showing and dismissing.
	 * <p>
	 * This method applies the specified animation resource to the dialog window,
	 * controlling how the dialog enters and exits the screen. Common animations
	 * include slide-up, fade-in, or scale effects.
	 * </p>
	 *
	 * @param animationResId the resource ID of the animation style (e.g., R.style.style_dialog_slide_up)
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog.getWindow().getAttributes().windowAnimations = animationResId;
		}
		
		return this;
	}
	
	/**
	 * Enables a slide-up animation for the dialog.
	 * <p>
	 * This convenience method applies a predefined slide-up animation style to the dialog,
	 * causing it to slide upward from the bottom of the screen when shown and slide down
	 * when dismissed. This is commonly used for bottom-sheet style dialogs.
	 * </p>
	 *
	 * @return this builder instance for method chaining
	 */
	@NotNull
	public StylizedDialogBuilder enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Enables a fade-in animation for the dialog.
	 * <p>
	 * This convenience method applies a fade animation style to the dialog,
	 * causing it to fade into view when shown and fade out when dismissed.
	 * Note: Currently uses the same animation as slide-up; override with
	 * a custom fade animation resource if different behavior is desired.
	 * </p>
	 *
	 * @return this builder instance for method chaining
	 */
	@NotNull
	public StylizedDialogBuilder enableFadeInAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Sets a listener to be called when the dialog is dismissed.
	 * <p>
	 * This method registers an OnDismissListener that triggers when the dialog
	 * is closed, whether by user action (clicking close/outside) or programmatically.
	 * Useful for cleanup operations or triggering actions after dialog closure.
	 * </p>
	 *
	 * @param listener the listener to invoke when the dialog is dismissed
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setDialogDismissListener(OnDismissListener listener) {
		if (alertDialog != null) {
			alertDialog.setOnDismissListener(listener);
		}
		return this;
	}
	
	/**
	 * Sets the positive button text using a direct string value.
	 * <p>
	 * This method updates the text of the positive button (right button) in the dialog.
	 * The button is identified by R.id.tvDialogRightBtn. Use this overload when the
	 * button text is dynamically generated or not from string resources.
	 * </p>
	 *
	 * @param text the text to display on the positive button
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setPositiveButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvRightBtn = dialogRootView.findViewById(R.id.tvDialogRightBtn);
			if (tvRightBtn != null) tvRightBtn.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the positive button text using a string resource ID.
	 * <p>
	 * This method loads the string from the specified resource ID using the associated
	 * activity's resources. If the activity reference is no longer valid, the method
	 * returns without making changes.
	 * </p>
	 *
	 * @param resId the resource ID of the string to use for the positive button text
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setPositiveButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setPositiveButtonText(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets an image at the top of the dialog using a drawable resource with custom width.
	 * <p>
	 * This method loads an image from a drawable resource and applies the specified width
	 * dimension. The image is set to visible and its width is adjusted while height remains
	 * wrapped to maintain aspect ratio. The activity reference is used to resolve dimension
	 * pixels from the resource ID.
	 * </p>
	 *
	 * @param resId         the drawable resource ID for the image
	 * @param imageWidthRes the dimension resource ID for the image width (e.g., R.dimen.dialog_image_width)
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setDialogImage(@DrawableRes int resId,
	                                            @DimenRes int imageWidthRes) {
		if (dialogRootView != null) {
			ImageView imgDialog = dialogRootView.findViewById(R.id.imgDialog);
			if (imgDialog != null) {
				imgDialog.setImageResource(resId);
				imgDialog.setVisibility(View.VISIBLE);
				
				BaseActivity<?> activity = weakActivityRef.get();
				if (activity != null) {
					int width = activity.getResources().getDimensionPixelSize(imageWidthRes);
					ViewGroup.LayoutParams params = imgDialog.getLayoutParams();
					params.width = width;
					imgDialog.setLayoutParams(params);
				}
			}
		}
		return this;
	}
	
	/**
	 * Sets an image at the top of the dialog using a Drawable object with custom width.
	 * <p>
	 * This method accepts a Drawable object and applies the specified width dimension.
	 * If the drawable is null, the image view is hidden. The width is set from the
	 * dimension resource, while height wraps content to preserve aspect ratio.
	 * </p>
	 *
	 * @param drawable      the Drawable to display, or null to hide the image view
	 * @param imageWidthRes the dimension resource ID for the image width
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setDialogImage(@Nullable Drawable drawable,
	                                            @DimenRes int imageWidthRes) {
		if (dialogRootView != null) {
			ImageView imgDialog = dialogRootView.findViewById(R.id.imgDialog);
			if (imgDialog != null) {
				if (drawable != null) {
					imgDialog.setImageDrawable(drawable);
					imgDialog.setVisibility(View.VISIBLE);
					
					BaseActivity<?> activity = weakActivityRef.get();
					if (activity != null) {
						int width = activity.getResources().getDimensionPixelSize(imageWidthRes);
						ViewGroup.LayoutParams params = imgDialog.getLayoutParams();
						params.width = width;
						imgDialog.setLayoutParams(params);
					}
				} else {
					imgDialog.setVisibility(View.GONE);
				}
			}
		}
		return this;
	}
	
	/**
	 * Sets a click listener for the positive action button (right button) in the dialog.
	 * <p>
	 * This method configures the behavior of the positive button (typically used for
	 * confirm, accept, or proceed actions). The listener can optionally trigger the
	 * dialog to close after execution. The button is identified by R.id.btnDialogRight.
	 * </p>
	 *
	 * @param listener                  the click listener to invoke when the positive button is clicked
	 * @param shouldCloseAfterExecution if true, the dialog will automatically close after
	 *                                  the listener executes; if false, the dialog stays open
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setOnPositiveClickListener(@NonNull View.OnClickListener listener,
	                                                        boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> {
					listener.onClick(v);
					if (shouldCloseAfterExecution) {
						close();
					}
				});
			}
		}
		return this;
	}
	
	/**
	 * Configures the positive button to simply close the dialog without additional actions.
	 * <p>
	 * This convenience method sets the right button's click behavior to only dismiss
	 * the dialog, useful for cancel, dismiss, or "OK" actions where no custom logic
	 * is required before closing.
	 * </p>
	 *
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setCloseOnPositiveButtonClick() {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> close());
			}
		}
		return this;
	}
	
	/**
	 * Controls the visibility of the dialog's close button (X icon).
	 * <p>
	 * This method allows showing or hiding the close button typically located in the
	 * top corner of the dialog. Hiding the close button is useful when you want to
	 * force users to make a selection via positive/negative buttons instead.
	 * </p>
	 *
	 * @param visible true to show the close button, false to hide it
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setCloseButtonVisible(boolean visible) {
		if (dialogRootView != null) {
			View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
			if (btnClose != null) {
				btnClose.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		}
		return this;
	}
	
	/**
	 * Sets the click listener for the dialog's close button.
	 * <p>
	 * This method configures the behavior of the close button within the dialog.
	 * If a custom listener is provided, it will be executed before the dialog is closed.
	 * If no listener is provided (null), the dialog will simply close without additional actions.
	 * The method automatically handles finding the close button by ID (R.id.btnCloseDialog).
	 * </p>
	 *
	 * @param listener the click listener to invoke when close button is clicked,
	 *                 or null to use default close behavior only
	 * @return this builder instance for method chaining
	 */
	@NonNull
	public StylizedDialogBuilder setOnCloseClickListener(@Nullable
	                                                     View.OnClickListener listener) {
		if (dialogRootView != null) {
			View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
			if (btnClose != null) {
				if (listener == null) {
					btnClose.setOnClickListener(v -> close());
				} else {
					btnClose.setOnClickListener(v -> {
						listener.onClick(v);
						close();
					});
				}
			}
		}
		return this;
	}
	
	/**
	 * Returns the custom content view previously set for this dialog.
	 * <p>
	 * This method retrieves the custom view from the weak reference and validates that
	 * it has been properly allocated. If no content view was set prior to calling this
	 * method, an IllegalStateException is thrown to prevent null access.
	 * </p>
	 *
	 * @return the custom content view that was set via setCustomContentView()
	 * @throws IllegalStateException if no custom content view has been allocated
	 */
	@NonNull
	public View getCustomContentView() throws IllegalStateException {
		View view = weakCustomChildViewRef != null ?
			weakCustomChildViewRef.get() : null;
		
		if (view == null) {
			throw new IllegalStateException("Content view elements are unallocated. " +
				"Invoke setCustomContentView first.");
		}
		return view;
	}
	
	/**
	 * Returns the underlying AlertDialog instance.
	 * <p>
	 * This method provides access to the native AlertDialog used internally,
	 * allowing for advanced customization beyond what the builder provides.
	 * May return null if the dialog has not been created yet.
	 * </p>
	 *
	 * @return the AlertDialog instance, or null if not yet created
	 */
	@Nullable
	public AlertDialog getAlertDialog() {
		return alertDialog;
	}
	
	/**
	 * Returns the parent activity associated with this dialog.
	 * <p>
	 * This method retrieves the BaseActivity instance from the weak reference.
	 * Returns null if the activity reference has been cleared or the activity
	 * is no longer available (e.g., destroyed).
	 * </p>
	 *
	 * @return the BaseActivity instance, or null if not available
	 */
	@Nullable
	public BaseActivity<?> getActivity() {
		return weakActivityRef != null ? weakActivityRef.get() : null;
	}
	
	/**
	 * Displays the custom styled dialog if the parent activity is still alive.
	 * <p>
	 * This method safely shows the alert dialog after verifying that the referenced
	 * activity still exists, is not finishing, and has not been destroyed. It also
	 * applies a transparent background to the dialog window for custom styling.
	 * </p>
	 *
	 * <p><b>Safety Checks:</b>
	 * <ul>
	 *   <li>Verifies activity reference is still valid</li>
	 *   <li>Checks activity is not finishing or destroyed</li>
	 *   <li>Ensures dialog is not already showing</li>
	 * </ul>
	 * </p>
	 */
	public void show() {
		try {
			BaseActivity<?> activity = weakActivityRef.get();
			if (activity == null || activity.isFinishing() ||
				activity.isDestroyed()) return;
			
			if (alertDialog != null && !alertDialog.isShowing()) {
				alertDialog.show();
				Window alertDialogWindow = alertDialog.getWindow();
				if (alertDialogWindow != null) {
					int resId = R.color.transparent;
					alertDialogWindow.setBackgroundDrawableResource(resId);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to cleanly present custom stylizer" +
				" dialog metrics: ", error);
		}
	}
	
	/**
	 * Dismisses the dialog and releases all associated resources.
	 * <p>
	 * This method performs comprehensive cleanup by dismissing the dialog, removing
	 * dismiss listeners, clearing click listeners from the view hierarchy, and nullifying
	 * all weak references. This prevents memory leaks when the dialog is no longer needed.
	 * </p>
	 *
	 * <p><b>Cleanup Actions:</b>
	 * <ul>
	 *   <li>Dismisses the alert dialog if showing</li>
	 *   <li>Removes dismiss listeners</li>
	 *   <li>Recursively clears click listeners from the view hierarchy</li>
	 *   <li>Clears all weak references to activity and views</li>
	 *   <li>Nullifies dialog reference for GC</li>
	 * </ul>
	 * </p>
	 */
	public void close() {
		try {
			if (alertDialog != null) {
				alertDialog.setOnDismissListener(null);
				if (alertDialog.isShowing()) {
					alertDialog.dismiss();
				}
				alertDialog = null;
			}
			if (dialogRootView != null) {
				clearAllLayoutListeners(dialogRootView);
				dialogRootView = null;
			}
			if (weakMainContentRef != null) weakMainContentRef.clear();
			if (weakCustomChildViewRef != null) weakCustomChildViewRef.clear();
		} catch (Exception error) {
			logger.error("Exception handled during dialog reference " +
				"recycling closures: ", error);
		}
	}
	
	/**
	 * Recursively clears all click listeners from a view and its child views.
	 * <p>
	 * This method traverses the view hierarchy starting from the given view,
	 * removing click listeners from each view to prevent memory leaks when
	 * a dialog or fragment is being destroyed. It handles both simple views
	 * and ViewGroup containers with children.
	 * </p>
	 *
	 * @param view the root view to start clearing listeners from, or null to skip
	 */
	private void clearAllLayoutListeners(@Nullable View view) {
		if (view == null) return;
		view.setOnClickListener(null);
		if (view instanceof ViewGroup group) {
			for (int index = 0; index < group.getChildCount(); index++) {
				clearAllLayoutListeners(group.getChildAt(index));
			}
		}
	}
	
	/**
	 * Positions the alert dialog at the bottom of the screen with custom styling.
	 * <p>
	 * This method configures the dialog to appear anchored to the bottom of the screen,
	 * with transparent background and full width. The window gravity is set to BOTTOM,
	 * allowing
	 * the dialog to slide up from the bottom edge rather than centering vertically.
	 * </p>
	 *
	 * <p><b>Configuration Applied:</b>
	 * <ul>
	 *   <li>Window gravity set to Gravity.BOTTOM</li>
	 *   <li>Zero vertical offset (y = 0)</li>
	 *   <li>Transparent background drawable</li>
	 *   <li>Layout width: MATCH_PARENT (full screen width)</li>
	 *   <li>Layout height: WRAP_CONTENT (height based on content)</li>
	 * </ul>
	 * </p>
	 */
	private void enableBottomPosition() {
		if (alertDialog.getWindow() != null) {
			alertDialog.getWindow().setGravity(Gravity.BOTTOM);
			WindowManager.LayoutParams params = alertDialog.getWindow().getAttributes();
			params.y = 0;
			alertDialog.getWindow().setAttributes(params);
			alertDialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
			alertDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		}
	}
}