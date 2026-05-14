package coreUtils.library.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

/**
 * A custom {@link ViewPager} that allows enabling or disabling swiping between pages.
 * <p>
 * This class overrides {@link #onTouchEvent(MotionEvent)} and {@link #onInterceptTouchEvent(MotionEvent)}
 * to prevent touch events from being processed when paging is disabled.
 * </p>
 */
public class NonSwappableViewPager extends ViewPager {

    /**
     * Flag to determine if swiping (paging) is enabled.
     */
    private boolean isPagingEnabled = true;

    /**
     * Simple constructor to use when creating a NonSwappableViewPager from code.
     *
     * @param context The Context the view is running in.
     */
    public NonSwappableViewPager(@NonNull Context context) {
        super(context);
    }

    /**
     * Constructor that is called when inflating a view from XML.
     *
     * @param context The Context the view is running in.
     * @param attrs   The attributes of the XML tag that is inflating the view.
     */
    public NonSwappableViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Overrides onTouchEvent to conditionally process touch events based on paging state.
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return isPagingEnabled && super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * Overrides onInterceptTouchEvent to conditionally intercept touch events based on paging state.
     *
     * @param event The motion event.
     * @return True if the event should be intercepted, false otherwise.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return isPagingEnabled && super.onInterceptTouchEvent(event);
    }

    /**
     * Enables or disables swiping (paging) for this ViewPager.
     *
     * @param enabled True to enable swiping, false to disable it.
     */
    public void setPagingEnabled(boolean enabled) {
        this.isPagingEnabled = enabled;
    }

    /**
     * Checks if swiping (paging) is currently enabled.
     *
     * @return True if paging is enabled, false otherwise.
     */
    public boolean isPagingEnabled() {
        return isPagingEnabled;
    }
}
