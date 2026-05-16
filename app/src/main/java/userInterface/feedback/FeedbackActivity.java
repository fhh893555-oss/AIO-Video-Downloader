package userInterface.feedback;

import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityFeedback1Binding;

import java.util.Objects;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.ClipboardHelper;
import coreUtils.library.views.StylizedToastView;
import coreUtils.library.views.TextViewsUtils;
import coreUtils.library.views.listeners.EditTextListener;

public class FeedbackActivity extends BaseActivity<ActivityFeedback1Binding> {
	private final LoggerUtils logger = LoggerUtils.from(FeedbackActivity.class);
	private FeedbackViewModel viewModel;
	private ActivityResultLauncher<String> imagePickerLauncher;
	
	@Override protected ActivityFeedback1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback1Binding.inflate(inflater);
	}
	
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override protected void onLoadedLayout() {
		viewModel = new ViewModelProvider(this).get(FeedbackViewModel.class);
		
		initImagePicker();
		applyGradientToTitle();
		setupButtonClicks();
		setupTextWatchers();
		observeViewModel();
		
		// Set default selection
		selectReaction(FeedbackReactions.Excellent.name(), binding.top2.imgHappy);
	}
	
	private void initImagePicker() {
		imagePickerLauncher = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			uri -> {
				if (uri != null) {
					viewModel.setSelectedScreenshot(DocumentFile.fromSingleUri(this, uri));
					StylizedToastView.showToast(FeedbackActivity.this,
						R.string.hint_image_attached_successfully);
				}
			}
		);
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
		binding.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
		
		String excellent = FeedbackReactions.Excellent.name();
		String good = FeedbackReactions.Good.name();
		String average = FeedbackReactions.Average.name();
		String poor = FeedbackReactions.Poor.name();
		String angry = FeedbackReactions.Angry.name();
		
		binding.top2.btnHappy.setOnClickListener(v -> selectReaction(excellent, binding.top2.imgHappy));
		binding.top2.btnGood.setOnClickListener(v -> selectReaction(good, binding.top2.imgGood));
		binding.top2.btnAverage.setOnClickListener(v -> selectReaction(average, binding.top2.imgAverage));
		binding.top2.btnPoor.setOnClickListener(v -> selectReaction(poor, binding.top2.imgPoor));
		binding.top2.btnAngry.setOnClickListener(v -> selectReaction(angry, binding.top2.imgAngry));
		
		binding.top3.btnUploadPic.setOnClickListener(v -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
		
		binding.top3.btnPasteIcon.setOnClickListener(v -> {
			buttonVibrate();
			pasteFromClipboard();
		});
		
		binding.top3.btnSendFeedback.setOnClickListener(v -> {
			buttonVibrate();
			submitFeedback();
		});
	}
	
	private void setupTextWatchers() {
		binding.top3.editFeedback.addTextChangedListener(new EditTextListener() {
			@Override public void afterTextChanged(Editable editable) {
				int length = editable.length();
				String countText = length + "/500";
				TextView tvCharCount = binding.top3.tvCharCount;
				tvCharCount.setText(countText);
				if (length > 500) tvCharCount.setTextColor(getColor(R.color.color_error));
				else tvCharCount.setTextColor(getColor(R.color.color_text_secondary));
			}
		});
	}
	
	private void observeViewModel() {
		viewModel.getIsSubmitting().observe(this, isSubmitting -> {
			binding.top3.btnSendFeedback.setEnabled(!isSubmitting);
			binding.top3.btnSendFeedback.setAlpha(isSubmitting ? 0.5f : 1.0f);
		});
		
		viewModel.getSubmissionSuccess().observe(this, success -> {
			if (success != null && success) {
				vibrate(50);
				int sentThankYou = R.string.hint_feedback_sent_thank_you;
				StylizedToastView.showToast(FeedbackActivity.this, sentThankYou);
				finish();
			}
		});
		
		viewModel.getSubmissionError().observe(this, error -> {
			if (error != null) {
				vibrate(50);
				StylizedToastView.showToast(FeedbackActivity.this, error);
			}
		});
	}
	
	private void selectReaction(String reaction, ImageView selectedImage) {
		buttonVibrate();
		viewModel.setSelectedReaction(reaction);
		
		binding.top2.imgHappy.setSelected(false);
		binding.top2.imgGood.setSelected(false);
		binding.top2.imgAverage.setSelected(false);
		binding.top2.imgPoor.setSelected(false);
		binding.top2.imgAngry.setSelected(false);
		
		selectedImage.setSelected(true);
	}
	
	private void pasteFromClipboard() {
		Context context = getBaseContext();
		CharSequence text = ClipboardHelper.getTextFromClipboard(context);
		if (text.length() > 0) {
			binding.top3.editSubject.setText(text);
		}
	}
	
	private void submitFeedback() {
		String subject = Objects.requireNonNull(binding.top3.editSubject.getText()).toString().trim();
		String message = Objects.requireNonNull(binding.top3.editFeedback.getText()).toString().trim();
		String email = Objects.requireNonNull(binding.top3.editEmail.getText()).toString().trim();
		
		if (message.isEmpty()) {
			StylizedToastView.showToast(this, R.string.hint_please_describe_your_feedback);
			return;
		}
		
		if (message.length() > 500) {
			StylizedToastView.showToast(this, R.string.hint_feedback_is_too_long);
			return;
		}
		
		viewModel.sendFeedback(this, subject, email, message);
	}
}
