package coreUtils.library.views;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;

import com.nextgen.R;

import coreUtils.base.BaseActivity;

public final class ActivityAnimator {

    private ActivityAnimator() {}

    /**
     * Applies a cross-fade transition animation to the specified Activity.
     *
     * @param activity The activity to apply the fade transition to. If null, no animation is applied.
     */
    public static void animActivityFade(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_fade_enter,
                    R.anim.anim_fade_exit
            );
        }
    }

    /**
     * Applies an "In and Out" transition animation to the specified activity.
     * This transition uses a custom entry and exit animation defined in the resources.
     *
     * @param activity The {@link BaseActivity} to apply the transition to. If null, no animation is applied.
     */
    public static void animActivityInAndOut(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_in_out_enter,
                    R.anim.anim_in_out_exit
            );
        }
    }

    /**
     * Applies a slide-down transition animation to the specified activity.
     *
     * @param activity The activity instance to apply the transition to.
     *                 If null, no animation will be performed.
     */
    public static void animActivitySlideDown(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_slide_down_enter,
                    R.anim.anim_slide_down_exit
            );
        }
    }

    /**
     * Applies a slide-left transition animation to the specified activity.
     *
     * @param activity The activity instance where the transition will be applied.
     *                 If null, no transition will be performed.
     */
    public static void animActivitySlideLeft(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_slide_left_enter,
                    R.anim.anim_slide_left_exit
            );
        }
    }

    /**
     * Applies a swipe-right transition animation to the specified activity.
     *
     * @param activity The activity instance to apply the animation to. If null,
     *                 no transition is applied.
     */
    public static void animActivitySwipeRight(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_swipe_right_enter,
                    R.anim.anim_swipe_right_exit
            );
        }
    }

    /**
     * Applies a slide-up transition animation to the given activity.
     *
     * @param activity The activity where the transition will be applied.
     */
    public static void animActivitySlideUp(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_slide_up_enter,
                    R.anim.anim_slide_up_exit
            );
        }
    }

    /**
     * Applies a swipe-left transition animation to the specified activity.
     *
     * @param activity The activity to apply the transition to. If null, no animation is applied.
     */
    public static void animActivitySwipeLeft(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_swipe_left_enter,
                    R.anim.anim_swipe_left_exit
            );
        }
    }

    /**
     * Applies a slide-right transition animation to the specified activity.
     * This is typically used for navigating back, where the new activity slides in
     * from the left and the current activity slides out to the right.
     *
     * @param activity The activity to apply the transition to.
     */
    public static void animActivitySlideRight(@Nullable BaseActivity<?> activity) {
        if (activity != null) {
            activity.overridePendingTransition(
                    R.anim.anim_slide_in_left,
                    R.anim.anim_slide_out_right
            );
        }
    }

    /**
     * Creates and returns activity transition options for a material-style horizontal slide animation.
     *
     * @param activity The current activity context used to create the animation options.
     * @return An {@link ActivityOptionsCompat} object containing the slide-in and slide-out
     * animations, or {@code null} if the provided activity is null.
     */
    @Nullable
    public static ActivityOptionsCompat getMaterialSlideOptions(@Nullable BaseActivity<?> activity) {
        if (activity == null) return null;
        return ActivityOptionsCompat.makeCustomAnimation(
                activity,
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
        );
    }
}