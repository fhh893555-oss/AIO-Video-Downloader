package userInterface.termsPolicy;

import android.view.LayoutInflater;

import com.nextgen.R;
import com.nextgen.databinding.ActivityTermsCon1Binding;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.TextViewsUtils;

public class TermsPolicyActivity extends BaseActivity<ActivityTermsCon1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected ActivityTermsCon1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityTermsCon1Binding.inflate(inflater);
	}
	
	@Override
	protected void onLoadedLayout() {
		applyGradientToTitle();
	}
	
	private void applyGradientToTitle() {
		String fullText = binding.top1.tvTermsTitle.getText().toString();
		int nextGenStart = fullText.indexOf("Conditions");
		if (nextGenStart != -1) {
			TextViewsUtils.applyGradientSpan(
				binding.top1.tvTermsTitle,
				getColor(R.color.color_secondary),
				getColor(R.color.color_primary_variant),
				nextGenStart,
				nextGenStart + 10
			);
		}
	}
	
}
