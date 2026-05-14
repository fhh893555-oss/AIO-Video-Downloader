package coreUtils.library.views;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.applyDimension;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.DefaultTimeBar;

/**
 * A custom {@link DefaultTimeBar} implementation that applies rounded corners to the progress bar.
 *
 * <p>This class extends the standard Media3 {@code DefaultTimeBar} by clipping the canvas
 * to a rounded rectangle path, ensuring that all drawing operations (including the
 * scrubber and progress tracks) respect the specified corner radius.
 *
 * <p>The default corner radius is set to 10dp.
 */
@UnstableApi
public class RoundedTimeBar extends DefaultTimeBar {

    /**
     * Paint used for drawing the rounded components of the time bar with antialiasing enabled.
     */
    private final Paint roundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /**
     * The path used to clip the canvas to create rounded corners for the time bar.
     */
    private final Path clipPath = new Path();

    /**
     * The bounding rectangle representing the area of the time bar, used to define the
     * clipping path for rounded corners.
     */
    private final RectF rectF = new RectF();

    /**
     * The radius, in pixels, applied to the corners of the time bar.
     */
    private final float cornerRadius;

    /**
     * Constructs a new RoundedTimeBar and initializes the corner radius.
     * The corner radius is set to a default of 10 DP, converted to pixels based on the device's
     * display metrics.
     *
     * @param context The Context the view is running in, through which it can access the current
     *                theme, resources, etc.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public RoundedTimeBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        cornerRadius = applyDimension(COMPLEX_UNIT_DIP, 10, displayMetrics);
    }

    /**
     * Called when the size of this view has changed.
     * Updates the bounding rectangle and the clipping path to ensure the rounded
     * corners are correctly calculated for the new dimensions of the time bar.
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        rectF.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW);
    }

    /**
     * Draws the time bar onto the provided canvas, applying a rounded clipping path
     * to ensure the visual content conforms to the specified corner radius.
     *
     * @param canvas The canvas on which the time bar will be drawn.
     */
    @Override
    public void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipPath(clipPath);
        super.onDraw(canvas);
        canvas.restore();
    }

    /**
     * Returns the {@link Paint} object used for rendering the rounded components of the time bar.
     *
     * @return The paint instance used for drawing.
     */
    public Paint getRoundPaint() {
        return roundPaint;
    }
}