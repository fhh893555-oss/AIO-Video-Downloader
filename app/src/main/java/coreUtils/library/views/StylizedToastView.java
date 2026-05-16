package coreUtils.library.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;

import com.nextgen.R;

import coreUtils.base.BaseActivity;
import coreUtils.library.networks.URLUtility;
import coreUtils.library.strings.StringHelper;

@SuppressWarnings("ALL")
public class StylizedToastView extends Toast {
	
	public StylizedToastView(Context context) {
		super(context);
	}
	
	public void setIcon(int iconResId) {
		if (getView() != null) {
			ImageView imageView = getView().findViewById(R.id.imgIcon);
			if (imageView != null) {
				imageView.setImageResource(iconResId);
			}
		}
	}
	
	public static void showToast(final BaseActivity activity, final String msg) {
		if (activity == null) return;
		showTextToast(activity, msg);
	}
	
	public static void showToast(final BaseActivity activity, final int msgId) {
		if (activity == null) return;
		showResourceToast(activity, msgId);
	}
	
	private static void showResourceToast(BaseActivity activity, int msgId) {
		CharSequence message = StringHelper.getText(msgId);
		if (URLUtility.isValidURL(message != null ? message.toString() : null)) {
			return;
		}
		
		StylizedToastView toast = makeText(activity, message, LENGTH_LONG);
		if (toast != null) toast.show();
		
	}
	
	private static void showTextToast(BaseActivity activity, String msg) {
		if (URLUtility.isValidURL(msg)) return;
		StylizedToastView toast = makeText(activity, msg, LENGTH_LONG);
		if (toast != null) toast.show();
		
	}
	
	private static StylizedToastView makeText(
		BaseActivity activity, CharSequence toastMessage, int duration) {
		if (activity == null) return null;
		return configureToastView(activity, toastMessage, duration);
	}
	
	private static StylizedToastView configureToastView(
		BaseActivity activity, CharSequence toastMessage, int duration) {
		if (toastMessage == null || toastMessage.toString().trim().isEmpty()) return null;
		
		int styleApplication = R.style.style_application;
		ContextThemeWrapper themedCtx = new ContextThemeWrapper(activity, styleApplication);
		LayoutInflater inflater = LayoutInflater.from(themedCtx);
		
		View toastView = inflater.inflate(R.layout.layout_stylized_toast, null);
		if (toastView == null) return null;
		
		TextView textView = toastView.findViewById(R.id.txLabel);
		if (textView == null) return null;
		
		textView.setText(toastMessage);
		
		StylizedToastView toast = new StylizedToastView(activity.getApplicationContext());
		toast.setView(toastView);
		toast.setDuration(duration);
		return toast;
	}
}