package userInterface.feedback;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityFeedback1Binding;

import java.util.Objects;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.views.TextViewsUtils;

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
					Toast.makeText(this, "Image attached successfully", Toast.LENGTH_SHORT).show();
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
		
		binding.top2.btnHappy.setOnClickListener(v -> selectReaction("Excellent", binding.top2.imgHappy));
		binding.top2.btnGood.setOnClickListener(v -> selectReaction("Good", binding.top2.imgGood));
		binding.top2.btnAverage.setOnClickListener(v -> selectReaction("Average", binding.top2.imgAverage));
		binding.top2.btnPoor.setOnClickListener(v -> selectReaction("Poor", binding.top2.imgPoor));
		binding.top2.btnAngry.setOnClickListener(v -> selectReaction("Angry", binding.top2.imgAngry));
		
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
		binding.top3.editFeedback.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			
			@Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override public void afterTextChanged(Editable s) {
				int length = s.length();
				String countText = length + "/500";
				binding.top3.tvCharCount.setText(countText);
				if (length > 500) {
					binding.top3.tvCharCount.setTextColor(getColor(R.color.color_error));
				} else {
					binding.top3.tvCharCount.setTextColor(getColor(R.color.color_text_secondary));
				}
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
				Toast.makeText(this, "Feedback sent! Thank you.", Toast.LENGTH_LONG).show();
				finish();
			}
		});
		
		viewModel.getSubmissionError().observe(this, error -> {
			if (error != null) {
				Toast.makeText(this, error, Toast.LENGTH_LONG).show();
			}
		});
	}
	
	private void selectReaction(String reaction, ImageView selectedImage) {
		buttonVibrate();
		viewModel.setSelectedReaction(reaction);
		
		// Reset all
		binding.top2.imgHappy.setSelected(false);
		binding.top2.imgGood.setSelected(false);
		binding.top2.imgAverage.setSelected(false);
		binding.top2.imgPoor.setSelected(false);
		binding.top2.imgAngry.setSelected(false);
		
		// Set selected
		selectedImage.setSelected(true);
	}
	
	private void pasteFromClipboard() {
		ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
		if (clipboard != null && clipboard.hasPrimaryClip()) {
			ClipData clip = clipboard.getPrimaryClip();
			if (clip != null && clip.getItemCount() > 0) {
				CharSequence text = clip.getItemAt(0).getText();
				if (text != null) {
					binding.top3.editSubject.setText(text);
				}
			}
		}
	}
	
	private void submitFeedback() {
		String subject = Objects.requireNonNull(binding.top3.editSubject.getText()).toString().trim();
		String message = Objects.requireNonNull(binding.top3.editFeedback.getText()).toString().trim();
		String email = Objects.requireNonNull(binding.top3.editEmail.getText()).toString().trim();
		
		if (message.isEmpty()) {
			Toast.makeText(this, "Please describe your feedback", Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (message.length() > 500) {
			Toast.makeText(this, "Feedback is too long", Toast.LENGTH_SHORT).show();
			return;
		}
		
		viewModel.sendFeedback(this, subject, email, message);
	}
}
