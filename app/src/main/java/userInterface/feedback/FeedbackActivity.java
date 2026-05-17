package userInterface.feedback;

import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;
import com.nextgen.databinding.ActivityFeedback1Binding;

import java.util.Objects;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.storage.FileStorageUtility;
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
		initViewModel();
		initImagePicker();
		applyGradientToTitle();
		setupButtonClicks();
		setupTextWatchers();
		observeViewModel();
		selectExcellentReactionByDefault();
	}
	
	public FeedbackViewModel getViewModel() {
		return viewModel;
	}
	
	private void initViewModel() {
		viewModel = new ViewModelProvider(this)
			.get(FeedbackViewModel.class);
	}
	
	private void initImagePicker() {
		resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		imagePickerLauncher = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			this::validateAttachmentFile
		);
		
		getViewModel().getSelectedScreenshot().observe(this,
			this::observeSelectedAttachmentFile);
	}
	
	private void observeSelectedAttachmentFile(DocumentFile documentFile) {
		if (documentFile == null) return;
		
		String fileName = documentFile.getName();
		if (fileName != null && fileName.length() > 0) {
			resetUploadAttachmentContainer(View.VISIBLE, View.GONE);
			
			binding.top3.txtFileAttch.setText(fileName);
			String fileSize = FileStorageUtility.humanReadableSizeOf(documentFile.length());
			CharSequence text = getText(R.string.label_file_size) + " " + fileSize;
			binding.top3.txtFileSize.setText(text);
		}
	}
	
	private void validateAttachmentFile(Uri fileUri) {
		if (fileUri != null) {
			DocumentFile file = DocumentFile.fromSingleUri(this, fileUri);
			if (file.length() < 5 * 1024 * 1024) {
				getViewModel().setSelectedScreenshot(file);
			} else {
				vibrate(50);
				StylizedToastView.showToast(FeedbackActivity.this,
					R.string.hint_file_size_exceed_5mb);
			}
		}
	}
	
	private void resetUploadAttachmentContainer(int attachmentVisibility,
	                                            int uploadPicVisibility) {
		binding.top3.contAttachmentSelected.setVisibility(attachmentVisibility);
		binding.top3.contUploadPic.setVisibility(uploadPicVisibility);
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
		
		ImageView imgHappy = binding.top2.imgHappy;
		TextView txtHappy = binding.top2.txtHappy;
		ImageView imgGood = binding.top2.imgGood;
		TextView txtGood = binding.top2.txtGood;
		ImageView imgAverage = binding.top2.imgAverage;
		TextView txtAverage = binding.top2.txtAverage;
		ImageView imgPoor = binding.top2.imgPoor;
		TextView txtPoor = binding.top2.txtPoor;
		ImageView imgAngry = binding.top2.imgAngry;
		TextView txtAngry = binding.top2.txtAngry;
		
		LinearLayout btnHappy = binding.top2.btnHappy;
		LinearLayout btnGood = binding.top2.btnGood;
		LinearLayout btnAverage = binding.top2.btnAverage;
		LinearLayout btnPoor = binding.top2.btnPoor;
		LinearLayout btnAngry = binding.top2.btnAngry;
		
		btnHappy.setOnClickListener(v -> selectReaction(excellent, imgHappy, txtHappy));
		btnGood.setOnClickListener(v -> selectReaction(good, imgGood, txtGood));
		btnAverage.setOnClickListener(v -> selectReaction(average, imgAverage, txtAverage));
		btnPoor.setOnClickListener(v -> selectReaction(poor, imgPoor, txtPoor));
		btnAngry.setOnClickListener(v -> selectReaction(angry, imgAngry, txtAngry));
		
		binding.top3.btnUploadPic.setOnClickListener(v -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
		
		binding.top3.btnCancelUpload.setOnClickListener(v -> {
			getViewModel().setSelectedScreenshot(null);
			resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
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
			
			binding.top3.txtSendFeedback.setText(isSubmitting ?
				R.string.hint_sending_feedback : R.string.btn_send_feedback);
		});
		
		viewModel.getSubmissionSuccess().observe(this, success -> {
			if (success != null && success) {
				vibrate(50);
				int sentThankYou = R.string.hint_feedback_sent_thank_you;
				StylizedToastView.showToast(FeedbackActivity.this, sentThankYou);
				resetFeedbackSubmission();
			}
		});
		
		viewModel.getSubmissionError().observe(this, error -> {
			if (error != null) {
				vibrate(50);
				StylizedToastView.showToast(FeedbackActivity.this, error);
			}
		});
	}
	
	private void resetFeedbackSubmission() {
		binding.top3.editSubject.setText("");
		binding.top3.editEmail.setText("");
		binding.top3.editFeedback.setText("");
		resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		getViewModel().setSelectedScreenshot(null);
	}
	
	private void selectReaction(String reaction,
	                            ImageView selectedImage, TextView selectedTextView) {
		viewModel.setSelectedReaction(reaction);
		
		Typeface regular = ResourcesCompat.getFont(this, R.font.font_family_regular);
		Typeface bold = ResourcesCompat.getFont(this, R.font.font_family_bold);
		
		binding.top2.imgHappy.setSelected(false);
		binding.top2.imgGood.setSelected(false);
		binding.top2.imgAverage.setSelected(false);
		binding.top2.imgPoor.setSelected(false);
		binding.top2.imgAngry.setSelected(false);
		
		binding.top2.txtHappy.setTypeface(regular);
		binding.top2.txtGood.setTypeface(regular);
		binding.top2.txtAverage.setTypeface(regular);
		binding.top2.txtPoor.setTypeface(regular);
		binding.top2.txtAngry.setTypeface(regular);
		
		binding.top2.txtHappy.setTextColor(getColor(R.color.color_text_secondary));
		binding.top2.txtGood.setTextColor(getColor(R.color.color_text_secondary));
		binding.top2.txtAverage.setTextColor(getColor(R.color.color_text_secondary));
		binding.top2.txtPoor.setTextColor(getColor(R.color.color_text_secondary));
		binding.top2.txtAngry.setTextColor(getColor(R.color.color_text_secondary));
		
		selectedImage.setSelected(true);
		selectedTextView.setTextColor(getColor(R.color.color_primary));
		selectedTextView.setTypeface(bold);
	}
	
	private void selectExcellentReactionByDefault() {
		selectReaction(FeedbackReactions.Excellent.name(),
			binding.top2.imgHappy, binding.top2.txtHappy);
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
		
		if (subject.isEmpty()) {
			StylizedToastView.showToast(this, R.string.hint_please_give_subject);
			vibrate();
			return;
		}
		
		if (message.isEmpty()) {
			StylizedToastView.showToast(this, R.string.hint_please_describe_your_feedback);
			vibrate();
			return;
		}
		
		if (message.length() > 500) {
			StylizedToastView.showToast(this, R.string.hint_feedback_is_too_long);
			vibrate();
			return;
		}
		
		viewModel.sendFeedback(this, subject, email, message);
	}
}
