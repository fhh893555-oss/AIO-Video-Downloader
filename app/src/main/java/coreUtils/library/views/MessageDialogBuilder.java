package coreUtils.library.views;

import android.app.AlertDialog;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

/**
 * A fluent builder for creating fully customizable message dialogs with modern styling.
 * <p>
 * This builder provides a comprehensive API for creating styled dialogs with support for
 * titles, messages, dual buttons (left/right) with icons, custom animations, background
 * blur effects, and automatic lifecycle management. It uses a WeakReference to the
 * associated activity to prevent memory leaks and automatically cleans up resources
 * when the dialog is dismissed.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Fluent API:</b> Method chaining for concise and readable dialog configuration</li>
 *   <li><b>Dual Buttons:</b> Left (negative/cancel) and right (positive/accept) buttons with icon support</li>
 *   <li><b>Animations:</b> Slide-up and fade-in animations supported</li>
 *   <li><b>Background Blur:</b> Modern frosted-glass effect (dim fallback for older Android versions)</li>
 *   <li><b>Automatic Cleanup:</b> Resources are released when dialog is dismissed</li>
 *   <li><b>Lifecycle Safe:</b> Prevents showing dialogs on destroyed or finishing activities</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * new MessageDialogBuilder(activity)
 *     .setTitle(R.string.title_confirmation)
 *     .setMessage(R.string.message_delete_confirm)
 *     .setLeftButtonText(R.string.label_cancel)
 *     .setLeftButtonIcons(R.drawable.ic_cancel, 0)
 *     .setRightButtonText(R.string.label_delete)
 *     .setRightButtonIcons(R.drawable.ic_delete, 0)
 *     .setOnRightClickListener(view -> deleteItem(), true)
 *     .enableBackgroundBlur(60)
 *     .applyBottomPositioning()
 *     .show();
 * </pre>
 * </p>
 *
 * <p><b>Default Configuration:</b>
 * <ul>
 *   <li>Animation: Slide-up entrance</li>
 *   <li>Cancelable: True (dismissible by back press or outside touch)</li>
 *   <li>Both buttons dismiss the dialog by default unless overridden</li>
 *   <li>Dialog style: R.style.style_dialog_window</li>
 * </ul>
 * </p>
 *
 * @see AlertDialog
 * @see WeakReference
 * @see BaseActivity
 */
public final class MessageDialogBuilder {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	
	/**
	 * Constructs a new MessageDialogBuilder for creating styled message dialogs.
	 * <p>
	 * This builder creates a fully customizable dialog with support for title, message,
	 * left/right buttons with icons, animations, background blur, and automatic lifecycle
	 * management. The dialog is tied to the provided activity via a WeakReference to
	 * prevent memory leaks.
	 * </p>
	 *
	 * @param activity the BaseActivity context used for inflating the dialog and accessing resources
	 */
	public MessageDialogBuilder(@NonNull BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	/**
	 * Initializes the dialog's base layout and configures default behavior.
	 * <p>
	 * This method inflates the dialog layout, creates the AlertDialog instance,
	 * sets up default click listeners that close the dialog, configures slide-up
	 * animation, and makes the dialog cancelable by default. If the activity is
	 * finishing or destroyed, initialization is skipped.
	 * </p>
	 *
	 * <p><b>Default Configuration:</b>
	 * <ul>
	 *   <li>Dialog style: R.style.style_dialog_window</li>
	 *   <li>Left button: Closes the dialog when clicked</li>
	 *   <li>Right button: Closes the dialog when clicked</li>
	 *   <li>Animation: Slide-up entrance</li>
	 *   <li>Cancelable: True (dismissible by back press or outside touch)</li>
	 *   <li>OnDismissListener: Automatically calls {@link #close()} for cleanup</li>
	 * </ul>
	 * </p>
	 */
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		ViewGroup root = activity.findViewById(android.R.id.content);
		dialogRootView = activity.getLayoutInflater()
			.inflate(R.layout.dialog_normal_message_1, root, false);
		
		int styledResId = R.style.style_dialog_window;
		AlertDialog.Builder nativeBuilder = new AlertDialog.Builder(activity, styledResId);
		nativeBuilder.setView(dialogRootView);
		this.alertDialog = nativeBuilder.create();
		
		this.alertDialog.setOnDismissListener(dialogInterface -> close());
		
		if (dialogRootView != null) {
			View btnLeft = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnLeft != null) {
				btnLeft.setOnClickListener(v -> close());
			}
			
			View btnRight = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnRight != null) {
				btnRight.setOnClickListener(v -> close());
			}
		}
		
		enableSlideUpAnimation();
		setCancelable(true);
	}
	
	/**
	 * Sets the title text to be displayed at the top of the dialog.
	 * <p>
	 * This method updates the title TextView in the dialog with the provided string.
	 * The title typically summarizes the dialog's purpose or indicates what action
	 * is being requested from the user.
	 * </p>
	 *
	 * @param text the string to display as the dialog title
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setTitle(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvTitle = dialogRootView.findViewById(R.id.tvDialogTitle);
			if (tvTitle != null) tvTitle.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the title text to be displayed at the top of the dialog using a string resource ID.
	 * <p>
	 * This convenience method resolves the string from the given resource ID
	 * using the associated activity and delegates to {@link #setTitle(String)}.
	 * If the activity is no longer available, the method returns without making changes.
	 * </p>
	 *
	 * @param resId the resource ID of the string to display as the dialog title
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setTitle(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setTitle(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets the message text to be displayed in the dialog body.
	 * <p>
	 * This method updates the main message TextView in the dialog with the provided
	 * string. The message typically explains the purpose of the dialog or provides
	 * information that requires user acknowledgment.
	 * </p>
	 *
	 * @param text the string to display as the dialog message
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setMessage(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvMessage = dialogRootView.findViewById(R.id.tvDialogMessage);
			if (tvMessage != null) tvMessage.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the message text to be displayed in the dialog body using a string resource ID.
	 * <p>
	 * This convenience method resolves the string from the given resource ID
	 * using the associated activity and delegates to {@link #setMessage(String)}.
	 * If the activity is no longer available, the method returns without making changes.
	 * </p>
	 *
	 * @param resId the resource ID of the string to display as the dialog message
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setMessage(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setMessage(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets the text for the left button of the dialog.
	 * <p>
	 * This method updates the text displayed on the left button, which typically
	 * represents a negative or cancel action (e.g., "Cancel", "No", "Decline").
	 * </p>
	 *
	 * @param text the string to display on the left button
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setLeftButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvLeftBtn = dialogRootView.findViewById(R.id.tvDialogLeftBtn);
			if (tvLeftBtn != null) tvLeftBtn.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the text for the left button of the dialog using a string resource ID.
	 * <p>
	 * This convenience method resolves the string from the given resource ID
	 * using the associated activity and delegates to {@link #setLeftButtonText(String)}.
	 * If the activity is no longer available, the method returns without making changes.
	 * </p>
	 *
	 * @param resId the resource ID of the string to display on the left button
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setLeftButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setLeftButtonText(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets compound drawable icons to the left and right of the left button text.
	 * <p>
	 * This method adds drawable icons to the left and/or right side of the left button's
	 * text using Android's compound drawable system. Pass null for an icon that should
	 * not be displayed. The left button typically represents the negative/cancel action.
	 * </p>
	 *
	 * @param leftIcon  drawable to place to the left of the text (maybe null)
	 * @param rightIcon drawable to place to the right of the text (maybe null)
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setLeftButtonIcons(@Nullable Drawable leftIcon,
	                                               @Nullable Drawable rightIcon) {
		if (dialogRootView != null) {
			TextView tvLeftBtn = dialogRootView.findViewById(R.id.tvDialogLeftBtn);
			if (tvLeftBtn != null) {
				tvLeftBtn.setCompoundDrawablesWithIntrinsicBounds(
					leftIcon, null, rightIcon, null);
			}
		}
		return this;
	}
	
	/**
	 * Sets compound drawable icons to the left and right of the left button text using resource IDs.
	 * <p>
	 * This convenience method resolves drawable resources from the provided IDs
	 * using the associated activity and delegates to {@link #setLeftButtonIcons(Drawable, Drawable)}.
	 * Pass 0 for an icon that should not be displayed. If the activity is no longer
	 * available, the method returns without making changes.
	 * </p>
	 *
	 * @param leftResId  resource ID of the drawable to place to the left of the text (use 0 for none)
	 * @param rightResId resource ID of the drawable to place to the right of the text (use 0 for none)
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setLeftButtonIcons(@DrawableRes int leftResId,
	                                               @DrawableRes int rightResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			Drawable leftDrawable = leftResId != 0
				? ContextCompat.getDrawable(activity, leftResId) : null;
			Drawable rightDrawable = rightResId != 0
				? ContextCompat.getDrawable(activity, rightResId) : null;
			return setLeftButtonIcons(leftDrawable, rightDrawable);
		}
		return this;
	}
	
	/**
	 * Sets a click listener for the left button of the dialog.
	 * <p>
	 * This method configures the left button (typically the negative/cancel action)
	 * with a custom click listener. Optionally, the dialog can be automatically
	 * closed after the listener executes, based on the shouldCloseAfterExecution flag.
	 * </p>
	 *
	 * @param listener                  the click listener to invoke when the left button is pressed
	 * @param shouldCloseAfterExecution if true, the dialog will close automatically after the
	 *                                  listener executes
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setOnLeftClickListener(@NonNull View.OnClickListener listener,
	                                                   boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> {
					listener.onClick(v);
					if (shouldCloseAfterExecution) close();
				});
			}
		}
		return this;
	}
	
	/**
	 * Sets the text for the right button using a string resource.
	 * <p>
	 * This method retrieves the string from the provided resource ID and sets it
	 * as the text for the right button (typically the positive/accept action).
	 * </p>
	 *
	 * @param text the string to display on the right button
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setRightButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvRightBtn = dialogRootView.findViewById(R.id.tvDialogRightBtn);
			if (tvRightBtn != null) tvRightBtn.setText(text);
		}
		return this;
	}
	
	/**
	 * Sets the text for the right button using a string resource ID.
	 * <p>
	 * This convenience method resolves the string from the given resource ID
	 * using the associated activity and delegates to {@link #setRightButtonText(String)}.
	 * If the activity is no longer available, the method returns without making changes.
	 * </p>
	 *
	 * @param resId the resource ID of the string to display on the right button
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setRightButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setRightButtonText(activity.getString(resId));
		}
		return this;
	}
	
	/**
	 * Sets compound drawable icons to the left and right of the right button text.
	 * <p>
	 * This method adds drawable icons to the left and/or right side of the right button's
	 * text using Android's compound drawable system. Pass null for an icon that should
	 * not be displayed.
	 * </p>
	 *
	 * @param leftIcon  drawable to place to the left of the text (maybe null)
	 * @param rightIcon drawable to place to the right of the text (maybe null)
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setRightButtonIcons(@Nullable Drawable leftIcon,
	                                                @Nullable Drawable rightIcon) {
		if (dialogRootView != null) {
			TextView tvRightBtn = dialogRootView.findViewById(R.id.tvDialogRightBtn);
			if (tvRightBtn != null) {
				tvRightBtn.setCompoundDrawablesWithIntrinsicBounds(
					leftIcon, null, rightIcon, null);
			}
		}
		return this;
	}
	
	/**
	 * Sets compound drawable icons to the left and right of the right button text using resource IDs.
	 * <p>
	 * This convenience method resolves drawable resources from the provided IDs
	 * using the associated activity and delegates to {@link #setRightButtonIcons(Drawable, Drawable)}.
	 * Pass 0 for an icon that should not be displayed. If the activity is no longer
	 * available, the method returns without making changes.
	 * </p>
	 *
	 * @param leftResId  resource ID of the drawable to place to the left of the text (use 0 for none)
	 * @param rightResId resource ID of the drawable to place to the right of the text (use 0 for none)
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setRightButtonIcons(@DrawableRes int leftResId,
	                                                @DrawableRes int rightResId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			Drawable leftDrawable = leftResId != 0
				? ContextCompat.getDrawable(activity, leftResId) : null;
			Drawable rightDrawable = rightResId != 0
				? ContextCompat.getDrawable(activity, rightResId) : null;
			return setRightButtonIcons(leftDrawable, rightDrawable);
		}
		return this;
	}
	
	/**
	 * Sets a click listener for the right button of the dialog.
	 * <p>
	 * This method configures the right button (typically the positive/accept action)
	 * with a custom click listener. Optionally, the dialog can be automatically
	 * closed after the listener executes, based on the shouldCloseAfterExecution flag.
	 * </p>
	 *
	 * @param listener                  the click listener to invoke when the right button is pressed
	 * @param shouldCloseAfterExecution if true, the dialog will close automatically after the
	 *                                  listener executes
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setOnRightClickListener(@NonNull View.OnClickListener listener,
	                                                    boolean shouldCloseAfterExecution) {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> {
					listener.onClick(v);
					if (shouldCloseAfterExecution) close();
				});
			}
		}
		return this;
	}
	
	/**
	 * Sets whether the dialog can be canceled by back press or touching outside.
	 * <p>
	 * This method configures both the cancelable property (affects back button behavior)
	 * and the canceledOnTouchOutside property (affects tapping outside the dialog).
	 * When set to false, the user must explicitly interact with the dialog's buttons
	 * to dismiss it, ensuring critical actions are acknowledged.
	 * </p>
	 *
	 * @param cancelable true if the dialog can be dismissed by back press or outside touch,
	 *                   false if the dialog is non-cancelable
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
	/**
	 * Enables background blur effect behind the dialog for visual depth.
	 * <p>
	 * This method applies a blur effect to the content behind the dialog, creating
	 * a modern frosted-glass appearance. On Android 12+ (API 31+), it uses the
	 * native blur-behind API with the specified radius. On older Android versions,
	 * it falls back to a dim background effect (60% dim) as blur is not supported.
	 * </p>
	 *
	 * <p><b>Behavior by Android Version:</b>
	 * <ul>
	 *   <li>Android 12+ (API 31+): Native blur with configurable radius</li>
	 *   <li>Android 11 and below: Dim background (0.6 dimAmount) as fallback</li>
	 * </ul>
	 * </p>
	 *
	 * @param blurRadius the blur radius in pixels (only used on Android 12+)
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder enableBackgroundBlur(int blurRadius) {
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
	 * Sets a custom window animation for the dialog.
	 * <p>
	 * This method applies the specified animation resource ID to the dialog's window,
	 * controlling how the dialog enters and exits the screen. The animation is applied
	 * immediately if the dialog window is available.
	 * </p>
	 *
	 * @param animationResId the resource ID of the animation to apply (e.g.,
	 *                       R.style.style_dialog_slide_up)
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog.getWindow().getAttributes().windowAnimations = animationResId;
		}
		return this;
	}
	
	/**
	 * Configures the dialog to use a slide-up entrance animation.
	 * <p>
	 * This convenience method applies a predefined slide-up animation resource
	 * to the dialog, causing it to slide upward from the bottom of the screen
	 * when shown and slide downward when dismissed.
	 * </p>
	 *
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NotNull
	public MessageDialogBuilder enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Configures the dialog to use a fade-in entrance animation.
	 * <p>
	 * This convenience method applies a predefined fade animation resource
	 * to the dialog, causing it to fade into view when shown and fade out
	 * when dismissed.
	 * </p>
	 *
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NotNull
	public MessageDialogBuilder enableFadeInAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	/**
	 * Sets a listener to be called when the dialog is dismissed.
	 * <p>
	 * This method registers an OnDismissListener that is triggered when the dialog
	 * is closed. The listener's {@code onDismiss} method is called after the dialog
	 * is dismissed, and the dialog resources are automatically cleaned up via
	 * {@link #close()} before the listener callback is invoked.
	 * </p>
	 *
	 * @param listener the OnDismissListener to call when the dialog is dismissed,
	 *                 may be null to remove any existing listener
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder setDialogDismissListener(OnDismissListener listener) {
		if (alertDialog != null) {
			alertDialog.setOnDismissListener(dialogInterface -> {
				close();
				if (listener != null) listener.onDismiss(dialogInterface);
			});
		}
		return this;
	}
	
	/**
	 * Applies bottom positioning and slide-up animation to the dialog.
	 * <p>
	 * This method configures the dialog to appear at the bottom of the screen with
	 * a slide-up entrance animation. It is part of the builder pattern, allowing
	 * method chaining for concise dialog configuration.
	 * </p>
	 *
	 * @return this MessageDialogBuilder instance for method chaining
	 */
	@NonNull
	public MessageDialogBuilder applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
	}
	
	/**
	 * Returns the underlying AlertDialog instance if it has been created.
	 * <p>
	 * This method provides access to the native AlertDialog for advanced
	 * customizations that are not exposed through the builder interface.
	 * The returned value may be null if the dialog has not been built yet.
	 * </p>
	 *
	 * @return the AlertDialog instance, or null if not yet created
	 */
	@Nullable
	public AlertDialog getAlertDialog() {
		return alertDialog;
	}
	
	/**
	 * Returns the activity reference associated with this dialog builder.
	 * <p>
	 * This method returns the activity stored in a WeakReference, which may
	 * be null if the activity has been garbage collected or was never set.
	 * Using a WeakReference prevents memory leaks by not holding a strong
	 * reference to the activity after it has been destroyed.
	 * </p>
	 *
	 * @return the BaseActivity instance, or null if the reference is no longer valid
	 */
	@Nullable
	public BaseActivity<?> getActivity() {
		return weakActivityRef != null ? weakActivityRef.get() : null;
	}
	
	/**
	 * Displays the dialog if the associated activity is still valid.
	 * <p>
	 * This method attempts to show the dialog only when the activity is still
	 * alive (not finishing or destroyed). It prevents crashes that could occur
	 * when trying to show a dialog after the activity has been terminated.
	 * The dialog's window background is also set to transparent during showing.
	 * </p>
	 *
	 * <p><b>Preconditions Checked:</b>
	 * <ul>
	 *   <li>Activity is not null</li>
	 *   <li>Activity is not finishing</li>
	 *   <li>Activity is not destroyed</li>
	 *   <li>AlertDialog exists and is not already showing</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Error Handling:</b>
	 * Any exceptions during the show operation are caught and logged without crashing.
	 * </p>
	 */
	public void show() {
		try {
			BaseActivity<?> activity = weakActivityRef.get();
			if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
			
			if (alertDialog != null && !alertDialog.isShowing()) {
				alertDialog.show();
				Window window = alertDialog.getWindow();
				if (window != null) {
					window.setBackgroundDrawableResource(R.color.transparent);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to present message dialog: ", error);
		}
	}
	
	/**
	 * Closes the dialog and releases all associated resources to prevent memory leaks.
	 * <p>
	 * This method safely dismisses the dialog if it is currently showing, removes any
	 * dismiss listeners, and clears all click listeners from the dialog's root view
	 * and its entire view hierarchy. All references are then nullified to allow garbage
	 * collection.
	 * </p>
	 *
	 * <p><b>Cleanup Steps:</b>
	 * <ol>
	 *   <li>Removes any registered OnDismissListener</li>
	 *   <li>Dismisses the dialog if it is currently showing</li>
	 *   <li>Nullifies the alertDialog reference</li>
	 *   <li>Recursively clears click listeners from the root view hierarchy</li>
	 *   <li>Nullifies the dialogRootView reference</li>
	 * </ol>
	 * </p>
	 *
	 * <p><b>Error Handling:</b>
	 * Any exceptions during cleanup are caught and logged without crashing,
	 * ensuring the dialog close operation never throws unhandled exceptions.
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
		} catch (Exception error) {
			logger.error("Exception during message dialog cleanup: ", error);
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
	 * <p><b>Traversal Behavior:</b>
	 * <ul>
	 *   <li>Sets OnClickListener to null on the current view</li>
	 *   <li>If the view is a ViewGroup, recursively processes all child views</li>
	 *   <li>Skips silently if the input view is null</li>
	 * </ul>
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
	 * allowing the dialog to slide up from the bottom edge rather than centering vertically.
	 * </p>
	 *
	 * <p><b>Usage Context:</b>
	 * Typically called before showing a dialog that should slide up from the bottom,
	 * such as a bottom sheet styled dialog or action selection menu.
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
