package coreUtils.library.views;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nextgen.R;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public final class CircularLoadingDialog {
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	
	public CircularLoadingDialog(BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		ViewGroup root = activity.findViewById(android.R.id.content);
		dialogRootView = activity.getLayoutInflater()
			.inflate(R.layout.dialog_static_loading_1, root, false);
		
		int styledResId = R.style.style_dialog_window;
		AlertDialog.Builder nativeBuilder = new AlertDialog.Builder(activity, styledResId);
		nativeBuilder.setView(dialogRootView);
		this.alertDialog = nativeBuilder.create();
		
		this.alertDialog.setOnDismissListener(dialogInterface -> close());
		
		if (dialogRootView != null) {
			View btnLeft = dialogRootView.findViewById(R.id.btnDialogLeft);
			if (btnLeft != null) {
				btnLeft.setOnClickListener(view -> close());
			}
			
			View btnRight = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnRight != null) {
				btnRight.setOnClickListener(view -> close());
			}
		}
		setCancelable(true);
	}
	
	@NonNull
	public CircularLoadingDialog setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
	@NonNull
	public CircularLoadingDialog enableBackgroundBlur(int blurRadius) {
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
	
	@NonNull
	public CircularLoadingDialog setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog
				.getWindow()
				.getAttributes()
				.windowAnimations = animationResId;
		}
		return this;
	}
	
	@NotNull
	public CircularLoadingDialog enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	@NotNull
	public CircularLoadingDialog enableFadeInAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
		return this;
	}
	
	@NonNull
	public CircularLoadingDialog setDialogDismissListener(DialogInterface.OnDismissListener listener) {
		if (alertDialog != null) {
			alertDialog.setOnDismissListener(dialogInterface -> {
				close();
				if (listener != null) {
					listener.onDismiss(dialogInterface);
				}
			});
		}
		return this;
	}
	
	@NonNull
	public CircularLoadingDialog applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
	}
	
	@Nullable
	public AlertDialog getAlertDialog() {
		return alertDialog;
	}
	
	@Nullable
	public BaseActivity<?> getActivity() {
		return weakActivityRef != null ? weakActivityRef.get() : null;
	}
	
	public void show() {
		try {
			BaseActivity<?> activity = weakActivityRef.get();
			if (activity == null || activity.isFinishing() ||
				activity.isDestroyed()) return;
			
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
	
	private void clearAllLayoutListeners(@Nullable View view) {
		if (view == null) return;
		view.setOnClickListener(null);
		if (view instanceof ViewGroup group) {
			for (int index = 0; index < group.getChildCount(); index++) {
				clearAllLayoutListeners(group.getChildAt(index));
			}
		}
	}
	
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
