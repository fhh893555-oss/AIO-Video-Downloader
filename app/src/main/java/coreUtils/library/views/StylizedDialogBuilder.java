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

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.nextgen.R;

import java.lang.ref.WeakReference;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;

public class StylizedDialogBuilder {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final WeakReference<BaseActivity<?>> weakActivityRef;
	
	private AlertDialog alertDialog;
	private View dialogRootView;
	private WeakReference<LinearLayout> weakMainContentRef;
	private WeakReference<View> weakCustomChildViewRef;
	
	public StylizedDialogBuilder(@NonNull BaseActivity<?> activity) {
		this.weakActivityRef = new WeakReference<>(activity);
		initializeBaseLayout();
	}
	
	private void initializeBaseLayout() {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
		
		dialogRootView = from(activity).inflate(R.layout.dialog_stylized_window_1, null);
		LinearLayout mainContent = dialogRootView.findViewById(R.id.mainContent);
		this.weakMainContentRef = new WeakReference<>(mainContent);
		
		AlertDialog.Builder nativeBuilder = new AlertDialog.Builder(activity, R.style.style_dialog_window);
		nativeBuilder.setView(dialogRootView);
		this.alertDialog = nativeBuilder.create();
		
		this.alertDialog.setOnDismissListener(dialogInterface -> close());
		View btnClose = dialogRootView.findViewById(R.id.btnCloseDialog);
		if (btnClose != null) {
			btnClose.setOnClickListener(v -> close());
		}
	}
	
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
	
	@NonNull
	public StylizedDialogBuilder setCancelable(boolean cancelable) {
		if (alertDialog != null) {
			alertDialog.setCancelable(cancelable);
			alertDialog.setCanceledOnTouchOutside(cancelable);
		}
		return this;
	}
	
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
	
	@NonNull public StylizedDialogBuilder applyBottomPositioning() {
		enableBottomPosition();
		enableSlideUpAnimation();
		return this;
	}
	
	@NonNull
	public StylizedDialogBuilder setDialogAnimation(int animationResId) {
		if (alertDialog != null && alertDialog.getWindow() != null) {
			alertDialog.getWindow().getAttributes().windowAnimations = animationResId;
		}
		
		return this;
	}
	
	@NonNull
	public StylizedDialogBuilder setDialogDismissListener(OnDismissListener listener) {
		if (alertDialog != null) {
			alertDialog.setOnDismissListener(listener);
		}
		return this;
	}
	
	@NonNull
	public StylizedDialogBuilder setPositiveButtonText(@NonNull String text) {
		if (dialogRootView != null) {
			TextView tvRightBtn = dialogRootView.findViewById(R.id.tvDialogRightBtn);
			if (tvRightBtn != null) tvRightBtn.setText(text);
		}
		return this;
	}
	
	@NonNull
	public StylizedDialogBuilder setPositiveButtonText(@StringRes int resId) {
		BaseActivity<?> activity = weakActivityRef.get();
		if (activity != null) {
			return setPositiveButtonText(activity.getString(resId));
		}
		return this;
	}
	
	@NonNull
	public StylizedDialogBuilder setDialogImage(@DrawableRes int resId) {
		if (dialogRootView != null) {
			ImageView imgDialog = dialogRootView.findViewById(R.id.imgDialog);
			if (imgDialog != null) {
				imgDialog.setImageResource(resId);
				imgDialog.setVisibility(View.VISIBLE);
			}
		}
		return this;
	}
	
	@NonNull
	public StylizedDialogBuilder setDialogImage(@Nullable Drawable drawable) {
		if (dialogRootView != null) {
			ImageView imgDialog = dialogRootView.findViewById(R.id.imgDialog);
			if (imgDialog != null) {
				if (drawable != null) {
					imgDialog.setImageDrawable(drawable);
					imgDialog.setVisibility(View.VISIBLE);
				} else {
					imgDialog.setVisibility(View.GONE);
				}
			}
		}
		return this;
	}
	
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
	
	@NonNull
	public StylizedDialogBuilder setCloseOnPositiveButtonClick() {
		if (dialogRootView != null) {
			View btnContainer = dialogRootView.findViewById(R.id.btnDialogRight);
			if (btnContainer != null) {
				btnContainer.setOnClickListener(v -> {
					close();
				});
			}
		}
		return this;
	}
	
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
	
	@NonNull
	public StylizedDialogBuilder setOnCloseClickListener(@Nullable View.OnClickListener listener) {
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
	
	@NonNull
	public View getCustomContentView() throws IllegalStateException {
		View view = weakCustomChildViewRef != null ? weakCustomChildViewRef.get() : null;
		if (view == null) {
			throw new IllegalStateException("Content view elements are unallocated. " +
				"Invoke setCustomContentView first.");
		}
		return view;
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
			if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
			
			if (alertDialog != null && !alertDialog.isShowing()) {
				alertDialog.show();
				Window alertDialogWindow = alertDialog.getWindow();
				if (alertDialogWindow != null) {
					int resId = R.color.transparent;
					alertDialogWindow.setBackgroundDrawableResource(resId);
				}
			}
		} catch (Exception error) {
			logger.error("Failed to cleanly present custom stylizer dialog metrics: ", error);
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
			if (weakMainContentRef != null) weakMainContentRef.clear();
			if (weakCustomChildViewRef != null) weakCustomChildViewRef.clear();
		} catch (Exception error) {
			logger.error("Exception handled during dialog reference recycling closures: ", error);
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
	
	private void enableSlideUpAnimation() {
		setDialogAnimation(R.style.style_dialog_window_slide_animation);
	}
}