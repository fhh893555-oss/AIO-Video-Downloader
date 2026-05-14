package coreUtils.library.views;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.nextgen.R;

/**
 * A custom {@link View} that displays progress in a vertical orientation.
 * <p>
 * This component renders a vertical bar that fills from the bottom upwards based on
 * the current progress and maximum values. It uses rounded corners for both the
 * background and the progress indicator, with colors defined by the application's
 * theme resources (surfacePrimary and surfaceElevated).
 * </p>
 *
 * @see #setProgress(int)
 * @see #setMax(int)
 */
public class VerticalProgressBar extends View {

    /**
     * The current progress value, ranging from 0 to {@link #max}.
     * Determines the height of the progress indicator drawn on the canvas.
     */
    private int progress = 0;

    /**
     * The maximum value for the progress bar. Defaults to 100.
     */
    private int max = 100;

    /**
     * The paint used to draw the background track of the progress bar.
     */
    private final Paint backgroundPaint = new Paint();

    /**
     * Paint object used to draw the filled portion of the progress bar.
     */
    private final Paint progressPaint = new Paint();

    /**
     * Simple constructor to use when creating a VerticalProgressBar from code.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     */
    public VerticalProgressBar(@NonNull Context context) {
        this(context, null);
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public VerticalProgressBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute. This constructor allows the VerticalProgressBar to use its
     * own base style when being inflated and initializes the paint objects used
     * for drawing the background and progress indicators.
     *
     * @param context The Context the view is running in, through which it can
     *                access the current theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public VerticalProgressBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        backgroundPaint.setColor(ContextCompat.getColor(context, R.color.color_primary));
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setAntiAlias(true);

        progressPaint.setColor(ContextCompat.getColor(context, R.color.color_secondary));
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setAntiAlias(true);
    }

    /**
     * Sets the current progress to the specified value.
     * <p>
     * The progress value is constrained between 0 and the maximum value defined for this bar.
     * Calling this method will trigger a redraw of the view.
     * </p>
     *
     * @param value the new progress value
     */
    public void setProgress(final int value) {
        progress = Math.max(0, Math.min(value, max));
        invalidate();
    }

    /**
     * Sets the maximum value of the progress bar.
     * Calling this method will trigger a redraw of the view.
     *
     * @param value The maximum progress value.
     */
    public void setMax(final int value) {
        max = value;
        invalidate();
    }

    /**
     * Measures the view to determine its width and height.
     * This implementation uses a default desired width of 10dp and a desired height of 200dp,
     * resolving these values against the constraints provided by the parent layout.
     *
     * @param widthMeasureSpec  Horizontal space requirements as imposed by the parent.
     * @param heightMeasureSpec Vertical space requirements as imposed by the parent.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = dpToPx(10);
        int desiredHeight = dpToPx(200);

        int width = resolveSize(desiredWidth, widthMeasureSpec);
        int height = resolveSize(desiredHeight, heightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    /**
     * Draws the vertical progress bar on the provided canvas.
     * This method renders the background track and the progress indicator as rounded rectangles.
     * The progress is filled from the bottom of the view upwards based on the current
     * progress and max values.
     *
     * @param canvas The canvas on which the background and progress will be drawn.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        float radius = dpToPx(10);
        canvas.drawRoundRect(0f, 0f, getWidth(), getHeight(),
                radius, radius, backgroundPaint);

        float progressHeight = 0f;
        if (max > 0) {
            progressHeight = (getHeight() * progress) / (float) max;
        }

        canvas.drawRoundRect(0f, getHeight() - progressHeight, getWidth(),
                getHeight(), radius, radius, progressPaint);
    }

    /**
     * Converts density-independent pixels (dp) to device pixels (px).
     *
     * @param dp The value in density-independent pixels to convert.
     * @return The converted value in pixels as an integer.
     */
    private int dpToPx(int dp) {
        Resources displayResources = Resources.getSystem();
        return (int) (dp * displayResources.getDisplayMetrics().density);
    }
}