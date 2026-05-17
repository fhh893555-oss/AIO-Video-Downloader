package coreUtils.library.views;

import static android.content.res.ColorStateList.valueOf;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;

import com.nextgen.R;

import coreUtils.base.BaseActivity;
import coreUtils.library.networks.URLUtility;

/**
 * A custom implementation of the {@link Toast} class that provides enhanced styling options,
 * including support for icons, custom colors, and typefaces.
 *
 * <p>This class utilizes a custom layout and provides a fluent API for configuring toast properties
 * such as icon tints, text styles, and duration. It also includes static utility methods for
 * displaying common feedback types like Success, Error, and Warning toasts.</p>
 *
 * <p>Validation logic is included to prevent the display of null, empty, or URL-formatted messages.</p>
 *
 * @see Toast
 */
@SuppressWarnings("ALL")
public class StylizedToastView extends Toast {
	
	private final View rootView;
	private final TextView textView;
	private final ImageView imageView;
	
	/**
	 * Initializes a new instance of the StylizedToastView with a custom layout.
	 *
	 * @param context The context to use. Usually your {@link android.app.Application}
	 *                or {@link android.app.Activity} object.
	 * @param view    The custom view to be displayed within the toast. This view
	 *                should contain a TextView with ID {@code R.id.txLabel} and
	 *                an ImageView with ID {@code R.id.imgIcon}.
	 */
	public StylizedToastView(@NonNull Context context, @NonNull View view) {
		super(context);
		this.rootView = view;
		this.textView = view.findViewById(R.id.txLabel);
		this.imageView = view.findViewById(R.id.imgIcon);
		setView(view);
	}
	
	/**
	 * Sets the icon for the toast using a drawable resource ID.
	 * This method will make the icon view visible if it was previously hidden.
	 *
	 * @param iconResId The resource ID of the drawable to display.
	 * @return The current {@link StylizedToastView} instance for method chaining.
	 */
	public StylizedToastView setIcon(@DrawableRes int iconResId) {
		if (imageView != null) {
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageResource(iconResId);
		}
		return this;
	}
	
	/**
	 * Sets the icon for the toast using a {@link Drawable} object.
	 * This method will make the icon's image view visible if it was previously hidden.
	 *
	 * @param drawable The drawable to be used as the icon, or {@code null} to clear the icon.
	 * @return This {@code StylizedToastView} instance for method chaining.
	 */
	public StylizedToastView setIcon(@Nullable Drawable drawable) {
		if (imageView != null) {
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageDrawable(drawable);
		}
		return this;
	}
	
	/**
	 * Hides the icon in the toast view by setting its visibility to {@link View#GONE}.
	 *
	 * @return The current instance of {@link StylizedToastView} for method chaining.
	 */
	public StylizedToastView hideIcon() {
		if (imageView != null) {
			imageView.setVisibility(View.GONE);
		}
		return this;
	}
	
	/**
	 * Sets the color tint for the toast's icon.
	 *
	 * @param color The color to apply to the icon as a tint.
	 * @return The current {@link StylizedToastView} instance for method chaining.
	 */
	public StylizedToastView setIconTint(@ColorInt int color) {
		if (imageView != null) {
			imageView.setImageTintList(valueOf(color));
			imageView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		return this;
	}
	
	/**
	 * Sets the text message to be displayed in the toast.
	 *
	 * @param message The character sequence to display.
	 * @return The current instance of {@link StylizedToastView} for method chaining.
	 */
	public StylizedToastView setMessage(CharSequence message) {
		if (textView != null) {
			textView.setText(message);
		}
		return this;
	}

	/**
	 * Sets the text message to be displayed in the toast using a string resource ID.
	 *
	 * @param resId The resource ID of the string resource to use as the message.
	 */
	@Override
	public void setText(int resId) {
		setMessage(rootView.getContext().getText(resId));
	}

	/**
	 * Sets the text to be displayed in the toast using a string resource.
	 *
	 * @param resId The resource id of the string resource to use.
	 */
	@Override
	public void setText(CharSequence charSequence) {
		setMessage(charSequence);
	}
	
	/**
	 * Sets the text color of the toast message.
	 *
	 * @param color The color integer to apply to the text.
	 * @return The current {@link StylizedToastView} instance for method chaining.
	 */
	public StylizedToastView setTextColor(@ColorInt int color) {
		if (textView != null) {
			textView.setTextColor(color);
		}
		return this;
	}
	
	/**
	 * Sets the text size of the toast message.
	 *
	 * @param sizeSp The size in "scaled pixel" (sp) units.
	 * @return This StylizedToastView object for method chaining.
	 */
	public StylizedToastView setTextSize(float sizeSp) {
		if (textView != null) {
			textView.setTextSize(sizeSp);
		}
		return this;
	}
	
	/**
	 * Sets the typeface and style in which the text should be displayed.
	 *
	 * @param typeface The typeface to be used for the toast message.
	 * @return The current instance of {@link StylizedToastView} for method chaining.
	 */
	public StylizedToastView setTypeface(Typeface typeface) {
		if (textView != null) {
			textView.setTypeface(typeface);
		}
		return this;
	}
	
	/**
	 * Sets the location at which the notification should appear on the screen.
	 *
	 * @param gravity The gravity constant (e.g., {@link android.view.Gravity#CENTER}).
	 * @param xOffset The horizontal offset from the gravity position in pixels.
	 * @param yOffset The vertical offset from the gravity position in pixels.
	 * @return The current {@link StylizedToastView} instance for method chaining.
	 */
	public StylizedToastView setToastGravity(int gravity,
	                                         int xOffset, int yOffset) {
		setGravity(gravity, xOffset, yOffset);
		return this;
	}
	
	/**
	 * Sets how long the toast notification should be displayed on the screen.
	 *
	 * @param duration The duration for the toast. Use {@link Toast#LENGTH_SHORT}
	 *                 or {@link Toast#LENGTH_LONG}.
	 * @return This {@code StylizedToastView} instance for method chaining.
	 */
	public StylizedToastView setToastDuration(int duration) {
		setDuration(duration);
		return this;
	}
	
	/**
	 * Displays the stylized toast notification on the screen.
	 * This method triggers the underlying {@link #show()} logic to present the
	 * configured view to the user.
	 */
	public void showToast() {
		show();
	}
	
	/**
	 * Creates a new instance of {@link StylizedToastView} with a specified message.
	 * <p>
	 * The method performs validation on the message and will return {@code null} if:
	 * <ul>
	 *     <li>The message is null or contains only whitespace.</li>
	 *     <li>The message is identified as a valid URL (to prevent displaying links in toasts).</li>
	 * </ul>
	 * It applies a default application theme to the toast view and sets the default
	 * duration to {@link Toast#LENGTH_LONG}.
	 *
	 * @param activity The activity context used to inflate the layout and access resources.
	 * @param message  The text message to be displayed in the toast.
	 * @return A configured {@link StylizedToastView} instance, or {@code null} if the message is invalid.
	 */
	public static StylizedToastView create(@NonNull BaseActivity<?> activity,
	                                       @Nullable CharSequence message) {
		
		if (message == null) return null;
		String msg = message.toString().trim();
		
		if (msg.isEmpty()) return null;
		if (URLUtility.isValidURL(msg)) return null;
		
		ContextThemeWrapper themedContext =
			new ContextThemeWrapper(activity, R.style.style_application);
		
		LayoutInflater inflater = LayoutInflater.from(themedContext);
		View view = inflater.inflate(R.layout.layout_stylized_toast, null, false);
		
		Context applicationContext = activity.getApplicationContext();
		StylizedToastView toast = new StylizedToastView(applicationContext, view);
		toast.setMessage(message);
		toast.setDuration(LENGTH_LONG);
		
		return toast;
	}
	
	/**
	 * Creates and displays a stylized toast message.
	 *
	 * @param activity The activity context used to inflate the toast layout.
	 * @param message  The text message to be displayed in the toast.
	 */
	public static void show(@NonNull BaseActivity<?> activity,
	                        @NonNull CharSequence message) {
		StylizedToastView toast = create(activity, message);
		if (toast != null) {
			toast.show();
		}
	}
	
	/**
	 * Displays a stylized success toast message with a predefined check circle icon.
	 *
	 * @param activity The activity context used to create and theme the toast.
	 * @param message  The text message to be displayed within the toast.
	 */
	public static void showSuccess(@NonNull BaseActivity<?> activity,
	                               @NonNull CharSequence message) {
		StylizedToastView toast = create(activity, message);
		if (toast == null) return;
		
		toast.setIcon(R.drawable.ic_check_circle)
			.setIconTint(activity.getColor(R.color.color_success))
			.show();
	}
	
	/**
	 * Displays a stylized error toast message with a predefined error icon and color.
	 *
	 * @param activity The activity context used to create and display the toast.
	 * @param message  The error message to be displayed.
	 */
	public static void showError(@NonNull BaseActivity<?> activity,
	                             @NonNull CharSequence message) {
		StylizedToastView toast = create(activity, message);
		if (toast == null) return;
		
		toast.setIcon(R.drawable.ic_cancel_circle)
			.setIconTint(activity.getColor(R.color.color_error))
			.show();
	}
	
	/**
	 * Displays a stylized warning toast with a warning icon and accent-colored tint.
	 *
	 * @param activity The base activity context used to create and color the toast.
	 * @param message  The text message to be displayed in the toast.
	 */
	public static void showWarning(@NonNull BaseActivity<?> activity,
	                               @NonNull CharSequence message) {
		StylizedToastView toast = create(activity, message);
		if (toast == null) return;
		toast.setIcon(R.drawable.ic_information)
			.setIconTint(activity.getColor(R.color.color_accent))
			.show();
	}
	
	/**
	 * Displays a custom stylized toast with a specific message, icon, icon tint, and text color.
	 *
	 * @param activity  The activity context used to create and display the toast.
	 * @param message   The text message to be displayed.
	 * @param icon      The resource ID of the drawable to be used as the toast icon.
	 * @param iconTint  The color integer to be applied as a tint to the icon.
	 * @param textColor The color integer to be applied to the message text.
	 */
	public static void showCustom(@NonNull BaseActivity<?> activity,
	                              @NonNull CharSequence message,
	                              @DrawableRes int icon,
	                              @ColorInt int iconTint,
	                              @ColorInt int textColor) {
		StylizedToastView toast = create(activity, message);
		if (toast == null) return;
		
		toast.setIcon(icon)
			.setIconTint(iconTint)
			.setTextColor(textColor)
			.show();
	}
}