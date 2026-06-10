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
 * A customizable, stylized toast notification view that extends the standard Android Toast.
 * <p>
 * This class provides an enhanced toast implementation with support for custom layouts,
 * icons, icon tinting, text color customization, typeface selection, and configurable
 * positioning. It maintains compatibility with the standard Toast API while offering
 * modern styling options for consistent application branding.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Custom Layout:</b> Uses a predefined layout with TextView and ImageView components</li>
 *   <li><b>Icon Support:</b> Display icons with optional color tinting via {@link #setIcon(int)} and
 *   {@link #setIconTint(int)}</li>
 *   <li><b>Text Styling:</b> Customizable text size, color, and typeface</li>
 *   <li><b>Position Control:</b> Configurable gravity and offsets via {@link #setToastGravity(int, int, int)}</li>
 *   <li><b>Duration Options:</b> Supports both LENGTH_SHORT and LENGTH_LONG durations</li>
 *   <li><b>Factory Methods:</b> Convenient {@link #create(BaseActivity, CharSequence)} method with message
 *   validation</li>
 * </ul>
 * </p>
 *
 * <p><b>Layout Requirements:</b>
 * The custom layout must contain:
 * <ul>
 *   <li>TextView with ID {@code R.id.txLabel} - For displaying the toast message</li>
 *   <li>ImageView with ID {@code R.id.imgIcon} - For displaying an optional icon</li>
 * </ul>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * // Basic usage
 * StylizedToastView.show(activity, "Operation completed");
 *
 * // Success toast with icon
 * StylizedToastView.showSuccess(activity, "File downloaded successfully");
 *
 * // Custom configuration
 * StylizedToastView toast = StylizedToastView.create(activity, "Custom message");
 * toast.setIcon(R.drawable.ic_custom)
 *      .setIconTint(Color.BLUE)
 *      .setTextColor(Color.WHITE)
 *      .setToastGravity(Gravity.CENTER, 0, 0)
 *      .setToastDuration(Toast.LENGTH_LONG)
 *      .show();
 * </pre>
 * </p>
 *
 * <p><b>Thread Safety:</b>
 * This class is not thread-safe. All methods should be called from the UI thread.
 * </p>
 *
 * @see Toast
 * @see StylizedToastView#create(BaseActivity, CharSequence)
 * @see StylizedToastView#show(BaseActivity, CharSequence)
 * @see StylizedToastView#showSuccess(BaseActivity, CharSequence)
 * @see StylizedToastView#showError(BaseActivity, CharSequence)
 * @see StylizedToastView#showWarning(BaseActivity, CharSequence)
 */
@SuppressWarnings("ALL")
public class StylizedToastView extends Toast {

    private final View rootView;
    private final TextView textView;
    private final ImageView imageView;

    /**
     * Initializes a new instance of the StylizedToastView with a custom layout.
     * <p>
     * This constructor creates a stylized toast using a custom view layout. The provided
     * view must contain specific views with predefined IDs for the toast to function
     * correctly. The toast inherits from Android's standard Toast class, allowing it to
     * be used as a drop-in replacement with enhanced styling capabilities.
     * </p>
     *
     * <p><b>Required View IDs:</b>
     * <ul>
     *   <li>{@code R.id.txLabel} - TextView for displaying the toast message</li>
     *   <li>{@code R.id.imgIcon} - ImageView for displaying an optional icon</li>
     * </ul>
     * </p>
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
     * <p>
     * This method loads a drawable from the application resources using the provided
     * resource ID and displays it as an icon to the left of the toast message. The icon
     * view is automatically made visible if it was previously hidden. This is useful for
     * displaying standard icons from the resource system.
     * </p>
     *
     * @param iconResId The resource ID of the drawable to display (e.g., R.drawable.ic_success)
     * @return The current {@link StylizedToastView} instance for method chaining
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
     * <p>
     * This method accepts a pre-loaded Drawable object and displays it as an icon to the
     * left of the toast message. The icon view is automatically made visible if it was
     * previously hidden. Passing null will clear the icon but keep the view visible
     * (consider using {@link #hideIcon()} to completely remove the icon space).
     * </p>
     *
     * @param drawable The drawable to be used as the icon, or {@code null} to clear the current icon
     * @return This {@code StylizedToastView} instance for method chaining
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
     * <p>
     * This method removes the icon from the toast layout, allowing the text message
     * to take up the full width of the toast. The icon space is collapsed rather than
     * remaining as empty space.
     * </p>
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
     * <p>
     * This method applies a color filter to the icon drawable, allowing the same
     * icon asset to be reused with different colors for different notification types
     * (e.g., green for success, red for error, accent for warning). The tint is applied
     * using the SRC_IN PorterDuff mode, which combines the icon's alpha channel with
     * the specified color.
     * </p>
     *
     * @param color The color to apply to the icon as a tint (e.g., Color.GREEN,
     *              ContextCompat.getColor(context, R.color.color_success)).
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
     * <p>
     * This method updates the toast's message text with the provided character sequence.
     * The message can be a plain string, a formatted string, or a Spannable for rich text.
     * Empty or null messages will result in a blank toast, which should be avoided.
     * </p>
     *
     * @param message The character sequence to display in the toast.
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
     * <p>
     * This convenience method resolves the string from the given resource ID using
     * the view's context and sets it as the toast message. It overrides the standard
     * Toast.setText method to integrate with the stylized toast implementation.
     * </p>
     *
     * @param resId The resource ID of the string resource to use as the message.
     */
    @Override
    public void setText(int resId) {
        setMessage(rootView.getContext().getText(resId));
    }

    /**
     * Sets the text to be displayed in the toast using a character sequence.
     * <p>
     * This method overrides the standard Toast.setText method to delegate to
     * {@link #setMessage(CharSequence)}. It provides compatibility with the
     * standard Toast API while maintaining the stylized appearance.
     * </p>
     *
     * @param charSequence The character sequence to display in the toast.
     */
    @Override
    public void setText(CharSequence charSequence) {
        setMessage(charSequence);
    }

    /**
     * Sets the text color of the toast message.
     * <p>
     * This method applies a custom color to the toast message text, overriding
     * any default styling. The color is specified as an ARGB integer, which can
     * be obtained from {@link android.content.res.Resources#getColor(int)} or
     * {@link android.content.Context#getColor(int)}.
     * </p>
     *
     * @param color The color integer to apply to the text (e.g., Color.WHITE,
     *              ContextCompat.getColor(context, R.color.text_primary)).
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
     * <p>
     * This method configures the font size for the toast message text. The size is
     * specified in scaled pixel (sp) units, which automatically scale based on the
     * user's font size preferences in system settings. This ensures accessibility
     * for users who require larger text.
     * </p>
     *
     * @param sizeSp The size in "scaled pixel" (sp) units (e.g., 14sp, 16sp, 18sp).
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
     * <p>
     * This method applies a custom typeface (font) to the toast message text. It can
     * be used to set different font families (e.g., Roboto, Open Sans) or styles
     * (e.g., bold, italic) to match the application's branding or visual design.
     * </p>
     *
     * @param typeface The typeface to be used for the toast message. Can be obtained
     *                 from {@link Typeface#createFromAsset(android.content.AssetManager, String)},
     *                 {@link Typeface#defaultFromStyle(int)}, or system typefaces.
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
     * <p>
     * This method configures the position of the toast on the screen using Android's
     * gravity system. By default, toasts appear at the bottom center. This method
     * allows customization to center, top, or any other position with optional
     * horizontal and vertical offsets.
     * </p>
     *
     * <p><b>Common Gravity Values:</b>
     * <ul>
     *   <li>{@link android.view.Gravity#CENTER} - Middle of the screen</li>
     *   <li>{@link android.view.Gravity#TOP} - Top of the screen</li>
     *   <li>{@link android.view.Gravity#BOTTOM} - Bottom of the screen (default)</li>
     *   <li>{@link android.view.Gravity#CENTER_HORIZONTAL} - Horizontally centered</li>
     * </ul>
     * </p>
     *
     * @param gravity The gravity constant (e.g., {@link android.view.Gravity#CENTER},
     *                {@link android.view.Gravity#TOP}, {@link android.view.Gravity#BOTTOM}).
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
     * <p>
     * This method configures the display duration for the toast. The duration can be
     * either {@link Toast#LENGTH_SHORT} (approximately 2 seconds) or
     * {@link Toast#LENGTH_LONG} (approximately 3.5 seconds). The default duration
     * is LENGTH_LONG if not specified.
     * </p>
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
     * <p>
     * This method triggers the underlying {@link #show()} logic to present the
     * configured view to the user. The toast will appear at the bottom of the screen
     * and automatically dismiss after the configured duration. This is an alias for
     * the standard {@code show()} method for semantic clarity.
     * </p>
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
     * </p>
     *
     * <p><b>Validation Rules:</b>
     * <ul>
     *   <li>Null or empty messages are rejected</li>
     *   <li>URLs are rejected to prevent accidental clicks or phishing concerns</li>
     * </ul>
     * </p>
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
        View view = inflater.inflate(R.layout.layout_stylized_toast_1, null, false);

        Context applicationContext = activity.getApplicationContext();
        StylizedToastView toast = new StylizedToastView(applicationContext, view);
        toast.setMessage(message);
        toast.setDuration(LENGTH_LONG);

        return toast;
    }

    /**
     * Creates and displays a stylized toast message with default styling.
     * <p>
     * This is the base method for showing a standard toast notification. The toast
     * appears with default colors and no icon. The toast automatically dismisses
     * after a short duration and supports proper lifecycle awareness.
     * </p>
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
     * <p>
     * This method shows a success notification with a check circle icon tinted with
     * the success color (typically green). It is suitable for confirming successful
     * operations such as file downloads, settings saves, or data updates.
     * </p>
     *
     * @param activity The activity context used to create and theme the toast.
     * @param message  The text message to be displayed within the toast.
     */
    public static void showSuccess(@NonNull BaseActivity<?> activity,
                                   @NonNull CharSequence message) {
        StylizedToastView toast = create(activity, message);
        if (toast == null) return;

        toast.setIcon(R.drawable.ic_check_circle)
                .setIconTint(activity.getColor(R.color.style_color_success))
                .setTextColor(activity.getColor(R.color.style_color_success_text))
                .show();
    }

    /**
     * Displays a stylized error toast message with a predefined error icon and color.
     * <p>
     * This method shows an error notification with a cancel circle icon tinted with
     * the error color (typically red). It is suitable for indicating failed operations,
     * network errors, permission denials, or validation failures.
     * </p>
     *
     * @param activity The activity context used to create and display the toast.
     * @param message  The error message to be displayed.
     */
    public static void showError(@NonNull BaseActivity<?> activity,
                                 @NonNull CharSequence message) {
        StylizedToastView toast = create(activity, message);
        if (toast == null) return;

        toast.setIcon(R.drawable.ic_cancel_circle)
                .setIconTint(activity.getColor(R.color.style_color_error))
                .setTextColor(activity.getColor(R.color.style_color_error_text))
                .show();
    }

    /**
     * Displays a stylized warning toast with a warning icon and accent-colored tint.
     * <p>
     * This method shows a warning notification with an information icon tinted with
     * the accent color. It is suitable for non-critical alerts, confirmation requests,
     * or informational messages that require user attention but are not errors.
     * </p>
     *
     * @param activity The base activity context used to create and color the toast.
     * @param message  The text message to be displayed in the toast.
     */
    public static void showWarning(@NonNull BaseActivity<?> activity,
                                   @NonNull CharSequence message) {
        StylizedToastView toast = create(activity, message);
        if (toast == null) return;
        toast.setIcon(R.drawable.ic_information)
                .setIconTint(activity.getColor(R.color.style_color_warning))
                .setTextColor(R.color.style_color_warning_text)
                .show();
    }

    /**
     * Displays a custom stylized toast with full control over icon, icon tint, and text color.
     * <p>
     * This method provides maximum customization for toast notifications. Callers can
     * specify a custom icon drawable, icon tint color, and text color to match specific
     * branding requirements or visual contexts not covered by the standard methods.
     * </p>
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