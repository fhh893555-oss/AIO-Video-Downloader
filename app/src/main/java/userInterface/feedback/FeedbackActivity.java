package userInterface.feedback;

import android.view.LayoutInflater;

import com.nextgen.R;
import com.nextgen.databinding.ActivityFeedback1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.TextViewsUtils;

public class FeedbackActivity extends BaseActivity<ActivityFeedback1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackActivity.class);
	
	@Override protected ActivityFeedback1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback1Binding.inflate(inflater);
	}
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected void onLoadedLayout() {
		applyGradientToTitle();
		setupButtonClicks();
	}
	
	private void applyGradientToTitle() {
		String fullText = binding.top1.tvFeedbackTitle.getText().toString();
		int nextGenStart = fullText.indexOf("Feedback");
		if (nextGenStart != -1) {
			TextViewsUtils.applyGradientSpan(
				binding.top1.tvFeedbackTitle,
				getColor(R.color.color_secondary),
				getColor(R.color.color_primary_variant),
				nextGenStart,
				nextGenStart + 8
			);
		}
	}
	
	private void setupButtonClicks() {
		binding.top1.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
	}
}
