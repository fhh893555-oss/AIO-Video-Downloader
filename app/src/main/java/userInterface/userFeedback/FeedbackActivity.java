package userInterface.userFeedback;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.imageview.ShapeableImageView;
import com.nextgen.R;
import com.nextgen.databinding.ActivityFeedback0Binding;

import java.util.Objects;

import coreUtils.base.BaseActivity;
import coreUtils.library.process.LoggerUtils;
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.StylizedToastView;

public class FeedbackActivity extends BaseActivity<ActivityFeedback0Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private FeedbackViewModel viewModel;
	private ActivityResultLauncher<String> imagePickerLauncher;
	
	@Override
	protected ActivityFeedback0Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback0Binding.inflate(inflater);
	}
	
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	@Override
	protected void onLoadedLayout() {
		initViewModel();
		initImagePicker();
		setupButtonClicks();
		initViewModelObservers();
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
		clearAttachmentPreview();
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
			Glide.with(this)
				.asBitmap()
				.load(documentFile.getUri())
				.into(new CustomTarget<Bitmap>() {
					private final ShapeableImageView attachmentPreview =
						binding.userMessage.ivAttachmentPreview;
					
					@Override
					public void onResourceReady(@NonNull Bitmap bitmap,
					                            Transition<? super Bitmap> transition) {
						Drawable drawable = new BitmapDrawable(getResources(), bitmap);
						attachmentPreview.setBackground(drawable);
					}
					
					@Override
					public void onLoadCleared(@Nullable Drawable placeholder) {
						Drawable defaultPreviewBG = ContextCompat
							.getDrawable(getApplicationContext(),
								R.drawable.ic_rd_primary_light_color);
						attachmentPreview.setBackground(defaultPreviewBG);
					}
				});
			
			binding.actionButtons.btnClearAttachment.setVisibility(View.VISIBLE);
			binding.userMessage.tvAddImage.setVisibility(View.INVISIBLE);
		}
	}
	
	private void validateAttachmentFile(Uri fileUri) {
		if (fileUri != null) {
			DocumentFile file = DocumentFile.fromSingleUri(this, fileUri);
			if (file.length() < 5 * 1024 * 1024) {
				getViewModel().setSelectedScreenshot(file);
			} else {
				vibrate(50);
				StylizedToastView.show(FeedbackActivity.this,
					StringHelper.getText(R.string.hint_file_size_exceed_5mb));
			}
		}
	}
	
	private void clearAttachmentPreview() {
		Drawable defaultPreviewBG = ContextCompat
			.getDrawable(this, R.drawable.ic_rd_primary_light_color);
		binding.userMessage.ivAttachmentPreview.setBackground(defaultPreviewBG);
		
		binding.userMessage.tvAddImage.setVisibility(View.VISIBLE);
		binding.actionButtons.btnClearAttachment.setVisibility(View.GONE);
	}
	
	private void setupButtonClicks() {
		setupBackButton();
		setupReactionListeners();
		setupUploadButton();
		setupCancelUploadListener();
		setupFeedbackButton();
	}
	
	private void setupBackButton() {
		binding.topBar.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
	}
	
	private void setupReactionListeners() {
		String excellent = FeedbackReactions.Excellent.name();
		String good = FeedbackReactions.Good.name();
		String average = FeedbackReactions.Average.name();
		String poor = FeedbackReactions.Poor.name();
		String angry = FeedbackReactions.Angry.name();
		
		ImageView imgHappy = binding.reactions.imgHappy;
		TextView txtHappy = binding.reactions.txtHappy;
		ImageView imgGood = binding.reactions.imgGood;
		TextView txtGood = binding.reactions.txtGood;
		ImageView imgAverage = binding.reactions.imgAverage;
		TextView txtAverage = binding.reactions.txtAverage;
		ImageView imgPoor = binding.reactions.imgPoor;
		TextView txtPoor = binding.reactions.txtPoor;
		ImageView imgAngry = binding.reactions.imgAngry;
		TextView txtAngry = binding.reactions.txtAngry;
		
		LinearLayout btnHappy = binding.reactions.btnHappy;
		LinearLayout btnGood = binding.reactions.btnGood;
		LinearLayout btnAverage = binding.reactions.btnAverage;
		LinearLayout btnPoor = binding.reactions.btnPoor;
		LinearLayout btnAngry = binding.reactions.btnAngry;
		
		btnHappy.setOnClickListener(view -> applyReactionSelection(excellent, imgHappy, txtHappy));
		btnGood.setOnClickListener(view -> applyReactionSelection(good, imgGood, txtGood));
		btnAverage.setOnClickListener(view -> applyReactionSelection(average, imgAverage,
			txtAverage));
		btnPoor.setOnClickListener(view -> applyReactionSelection(poor, imgPoor, txtPoor));
		btnAngry.setOnClickListener(view -> applyReactionSelection(angry, imgAngry, txtAngry));
	}
	
	private void setupUploadButton() {
		binding.userMessage.btnUploadPic.setOnClickListener(view -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
	}
	
	private void setupCancelUploadListener() {
		binding.actionButtons.btnClearAttachment.setOnClickListener(view -> {
			getViewModel().setSelectedScreenshot(null);
			clearAttachmentPreview();
		});
	}
	
	private void setupFeedbackButton() {
		binding.actionButtons.btnSendReport.setOnClickListener(view -> {
			buttonVibrate();
			validateAndSendFeedback();
		});
	}
	
	private void initViewModelObservers() {
		observeSubmissionResult();
		observeSubmissionErrors();
	}
	
	private void observeSubmissionErrors() {
		getViewModel().getSubmissionError().observe(this, errorMessage -> {
			if (errorMessage != null && !errorMessage.isEmpty()) {
				StylizedToastView.show(FeedbackActivity.this, errorMessage);
				vibrate();
			}
		});
	}
	
	private void observeSubmissionResult() {
		getViewModel().getSubmissionSuccess().observe(this, isSuccess -> {
			if (isSuccess != null && isSuccess) {
				StylizedToastView.show(FeedbackActivity.this,
					StringHelper.getText(R.string.hint_feedback_sent_thank_you));
				resetFeedbackSubmission();
				vibrate();
			}
		});
	}
	
	private void resetFeedbackSubmission() {
		binding.userMessage.editEmail.setText("");
		binding.userMessage.editFeedback.setText("");
		clearAttachmentPreview();
		getViewModel().setSelectedScreenshot(null);
	}
	
	private void applyReactionSelection(@NonNull String reaction,
	                                    @NonNull ImageView selectedImage,
	                                    @NonNull TextView selectedTextView) {
		getViewModel().setSelectedReaction(reaction);
		
		Typeface regular = ResourcesCompat.getFont(this, R.font.font_family_regular);
		Typeface bold = ResourcesCompat.getFont(this, R.font.font_family_bold);
		
		binding.reactions.imgHappy.setSelected(false);
		binding.reactions.imgGood.setSelected(false);
		binding.reactions.imgAverage.setSelected(false);
		binding.reactions.imgPoor.setSelected(false);
		binding.reactions.imgAngry.setSelected(false);
		
		binding.reactions.txtHappy.setTypeface(regular);
		binding.reactions.txtGood.setTypeface(regular);
		binding.reactions.txtAverage.setTypeface(regular);
		binding.reactions.txtPoor.setTypeface(regular);
		binding.reactions.txtAngry.setTypeface(regular);
		
		int unselectedColor = getColor(R.color.style_color_text_secondary);
		binding.reactions.txtHappy.setTextColor(unselectedColor);
		binding.reactions.txtGood.setTextColor(unselectedColor);
		binding.reactions.txtAverage.setTextColor(unselectedColor);
		binding.reactions.txtPoor.setTextColor(unselectedColor);
		binding.reactions.txtAngry.setTextColor(unselectedColor);
		
		selectedImage.setSelected(true);
		selectedTextView.setTextColor(getColor(R.color.style_color_text_primary));
		selectedTextView.setTypeface(bold);
	}
	
	private void selectExcellentReactionByDefault() {
		applyReactionSelection(FeedbackReactions.Excellent.name(),
			binding.reactions.imgHappy, binding.reactions.txtHappy);
	}
	
	private void validateAndSendFeedback() {
		String email = getEmailText().toString().trim();
		String message = getFeedbackText().toString().trim();
		
		if (message.isEmpty()) {
			String toastMessage =
				StringHelper.getText(R.string.hint_please_describe_feedback);
			StylizedToastView.show(this, toastMessage);
			vibrate();
			return;
		}
		
		if (message.length() > 500) {
			String toastMessage =
				StringHelper.getText(R.string.hint_feedback_is_too_long);
			StylizedToastView.show(this, toastMessage);
			vibrate();
			return;
		}
		
		String reaction = getViewModel().getSelectedReaction().getValue();
		DocumentFile attachment = getViewModel().getSelectedScreenshot().getValue();
		getViewModel().sendFeedback(this, reaction, email, message, attachment);
	}
	
	@NonNull private Editable getEmailText() {
		return Objects.requireNonNull(binding.userMessage.editEmail.getText());
	}
	
	@NonNull private Editable getFeedbackText() {
		return Objects.requireNonNull(binding.userMessage.editFeedback.getText());
	}
	
}