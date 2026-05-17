package coreUtils.library.views;

import android.view.View;

import coreUtils.library.process.LoggerUtils;

public class ViewsUtility {
	private static final LoggerUtils logger = LoggerUtils.from(TextViewsUtils.class);
	
	private ViewsUtility(){}
	
	public static void hideView(View targetView) {
		hideView(targetView, View.GONE, false, 500);
	}
	
	public static void hideView(View targetView, boolean shouldAnimate,
	                            long animTimeout) {
		hideView(targetView, View.GONE, shouldAnimate, animTimeout);
	}
	
	public static void hideView(View targetView, int visibility,
	                            boolean shouldAnimate, long animTimeout) {
		if (targetView == null) return;
		if (targetView.getVisibility() != View.VISIBLE) return;
		
		targetView.animate().cancel();
		if (shouldAnimate) {
			targetView.animate()
				.alpha(0f)
				.setDuration(animTimeout)
				.withEndAction(() -> {
					targetView.setVisibility(visibility);
					targetView.setAlpha(1f);
				})
				.start();
		} else {
			targetView.setVisibility(visibility);
		}
	}
	
	public static void showView(View targetView) {
		showView(targetView, false, 500);
	}
	
	public static void showView(View targetView, boolean shouldAnimate,
	                            long animTimeout) {
		if (targetView == null) return;
		if (targetView.getVisibility() == View.VISIBLE) return;
		targetView.animate().cancel();
		
		if (shouldAnimate) {
			targetView.setAlpha(0f);
			targetView.setVisibility(View.VISIBLE);
			
			targetView.animate()
				.alpha(1f)
				.setDuration(animTimeout)
				.start();
		} else {
			targetView.setVisibility(View.VISIBLE);
		}
	}
}
