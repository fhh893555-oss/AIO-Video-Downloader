package userInterface.userFeedback;

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
import androidx.annotation.NonNull;
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
import coreUtils.library.strings.StringHelper;
import coreUtils.library.views.StylizedToastView;
import coreUtils.library.views.TextViewsUtils;
import coreUtils.library.views.listeners.EditTextListener;

/**
 * FeedbackActivity provides a user interface for submitting application feedback.
 * <p>
 * This activity allows users to:
 * <ul>
 *     <li>Select a reaction representing their experience (Excellent to Angry).</li>
 *     <li>Enter a subject and a detailed feedback message (up to 500 characters).</li>
 *     <li>Attach an image file from the device (with a size limit of 5MB).</li>
 *     <li>Optionally provide an email address for follow-up.</li>
 *     <li>Quickly paste text from the clipboard into the subject field.</li>
 * </ul>
 * <p>
 * The class follows the MVVM architecture, utilizing {@link FeedbackViewModel} to handle
 * data submission and state management. It also manages UI states for submission
 * progress, success, and error handling.
 */
public class FeedbackActivity extends BaseActivity<ActivityFeedback1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(FeedbackActivity.class);
	private FeedbackViewModel viewModel;
	private ActivityResultLauncher<String> imagePickerLauncher;
	
	/**
	 * Inflates the view binding for the Feedback activity.
	 * <p>
	 * This method initializes the {@link ActivityFeedback1Binding} using the provided
	 * {@link LayoutInflater}, allowing for type-safe access to the layout's views.
	 * </p>
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the binding.
	 * @return A new instance of {@link ActivityFeedback1Binding}.
	 */
	@Override protected ActivityFeedback1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback1Binding.inflate(inflater);
	}
	
	/**
	 * Determines whether the activity should be locked to a specific orientation.
	 * <p>
	 * Overriding this method and returning {@code true} ensures that the feedback screen
	 * maintains a consistent orientation (typically portrait), preventing layout
	 * inconsistencies during rotation.
	 * </p>
	 *
	 * @return {@code true} to lock the orientation; {@code false} otherwise.
	 */
	@Override protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Initializes the activity's components and UI state once the layout has been loaded.
	 * <p>
	 * This method sets up the ViewModel, configures the image picker for attachments,
	 * applies visual styling to the title, initializes button click listeners and text watchers,
	 * starts observing ViewModel live data, and sets the default reaction state.
	 * </p>
	 */
	@Override protected void onLoadedLayout() {
		initViewModel();
		initImagePicker();
		applyGradientToTitle();
		setupButtonClicks();
		monitorFeedbackLength();
		initViewModelObservers();
		selectExcellentReactionByDefault();
	}
	
	/**
	 * Returns the {@link FeedbackViewModel} instance associated with this activity.
	 *
	 * @return The current {@link FeedbackViewModel} used for managing feedback data and state.
	 */
	public FeedbackViewModel getViewModel() {
		return viewModel;
	}
	
	/**
	 * Initializes the {@link FeedbackViewModel} for this activity.
	 * <p>
	 * This method uses the {@link ViewModelProvider} to create or retrieve an existing
	 * instance of {@code FeedbackViewModel}, ensuring the ViewModel persists
	 * through configuration changes.
	 */
	private void initViewModel() {
		viewModel = new ViewModelProvider(this)
			.get(FeedbackViewModel.class);
	}
	
	/**
	 * Initializes the image picker functionality for feedback attachments.
	 * <p>
	 * This method performs the following setup:
	 * <ul>
	 *     <li>Resets the attachment UI container to show the initial upload state.</li>
	 *     <li>Registers an {@link ActivityResultLauncher} for the {@link ActivityResultContracts.GetContent}
	 *     contract to handle file selection and validation.</li>
	 *     <li>Observes the ViewModel's selected screenshot LiveData to update the UI when
	 *     a file is successfully attached or removed.</li>
	 * </ul>
	 */
	private void initImagePicker() {
		resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		imagePickerLauncher = registerForActivityResult(
			new ActivityResultContracts.GetContent(),
			this::validateAttachmentFile
		);
		
		getViewModel().getSelectedScreenshot().observe(this,
			this::observeSelectedAttachmentFile);
	}
	
	/**
	 * Updates the UI to display information about the selected attachment file.
	 * <p>
	 * This method is called when a file is successfully selected and validated.
	 * It retrieves the file name and size, updates the corresponding text views,
	 * and toggles the visibility of the attachment container to show the
	 * selected file details instead of the upload button.
	 * </p>
	 *
	 * @param documentFile The {@link DocumentFile} representing the selected attachment.
	 *                     If null, the method returns without performing any action.
	 */
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
	
	/**
	 * Validates the selected attachment file and updates the ViewModel if it meets the requirements.
	 * <p>
	 * This method checks if the provided {@link Uri} is not null, converts it to a {@link DocumentFile},
	 * and verifies that the file size is less than 5MB. If the file is valid, it is set as the
	 * selected screenshot in the {@link FeedbackViewModel}. If the file exceeds the size limit,
	 * the device vibrates and an error toast is displayed.
	 * </p>
	 *
	 * @param fileUri The {@link Uri} of the file selected from the image picker.
	 */
	private void validateAttachmentFile(Uri fileUri) {
		if (fileUri != null) {
			DocumentFile file = DocumentFile.fromSingleUri(this, fileUri);
			if (file.length() < 5 * 1024 * 1024) {
				getViewModel().setSelectedScreenshot(file);
			} else {
				vibrate(50);
				StylizedToastView.showWarning(FeedbackActivity.this,
					StringHelper.getText(R.string.hint_file_size_exceed_5mb));
			}
		}
	}
	
	/**
	 * Updates the visibility of the attachment-related UI containers.
	 * <p>
	 * This method toggles between the state where an attachment is selected and
	 * the state where the user is prompted to upload a new picture.
	 * </p>
	 *
	 * @param attachmentVisibility The visibility state (e.g., {@link View#VISIBLE}, {@link View#GONE})
	 *                             for the container displaying the selected attachment.
	 * @param uploadPicVisibility  The visibility state (e.g., {@link View#VISIBLE}, {@link View#GONE})
	 *                             for the container prompting to upload a picture.
	 */
	private void resetUploadAttachmentContainer(int attachmentVisibility,
	                                            int uploadPicVisibility) {
		binding.top3.contAttachmentSelected.setVisibility(attachmentVisibility);
		binding.top3.contUploadPic.setVisibility(uploadPicVisibility);
	}
	
	/**
	 * Applies a color gradient span to a specific portion of the feedback title text.
	 * <p>
	 * This method searches for the keyword "Feedback" within the title's text. If found,
	 * it applies a linear gradient transition between the secondary and primary variant
	 * colors to that specific word using {@link TextViewsUtils#applyGradientSpan}.
	 * </p>
	 */
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
	
	/**
	 * Initializes and configures the click listeners for all interactive UI elements
	 * in the activity.
	 * <p>
	 * This method sets up the behavior for:
	 * <ul>
	 *     <li>The back button to close the activity.</li>
	 *     <li>The reaction selection buttons (Excellent, Good, Average, Poor, Angry) to
	 *     update the UI and ViewModel.</li>
	 *     <li>The attachment upload button to launch the image picker.</li>
	 *     <li>The cancel upload button to remove selected screenshots.</li>
	 *     <li>The paste button to populate the subject field from the clipboard.</li>
	 *     <li>The send button to trigger feedback validation and submission.</li>
	 * </ul>
	 */
	private void setupButtonClicks() {
		setupBackButton();
		setupReactionListeners();
		setupUploadButton();
		setupCancelUploadListener();
		setupPasteButton();
		setupFeedbackButton();
	}
	
	/**
	 * Configures the click listener for the back button.
	 * When clicked, it triggers a haptic vibration feedback and finishes the activity
	 * to return the user to the previous screen.
	 */
	private void setupBackButton() {
		binding.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
	}
	
	/**
	 * Configures click listeners for the feedback reaction buttons (Excellent, Good, Average, Poor, Angry).
	 * <p>
	 * This method maps each reaction layout to its corresponding selection logic, ensuring that
	 * clicking a reaction updates the ViewModel and reflects the selection visually in the UI.
	 * It also initializes the necessary references to images and text views for each reaction state.
	 * </p>
	 */
	private void setupReactionListeners() {
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
		
		btnHappy.setOnClickListener(view -> applyReactionSelection(excellent, imgHappy, txtHappy));
		btnGood.setOnClickListener(view -> applyReactionSelection(good, imgGood, txtGood));
		btnAverage.setOnClickListener(view -> applyReactionSelection(average, imgAverage, txtAverage));
		btnPoor.setOnClickListener(view -> applyReactionSelection(poor, imgPoor, txtPoor));
		btnAngry.setOnClickListener(view -> applyReactionSelection(angry, imgAngry, txtAngry));
	}
	
	/**
	 * Configures the click listener for the upload button to trigger the system image picker.
	 * When clicked, it provides haptic feedback and launches an activity result contract
	 * to allow the user to select an image file from their device storage.
	 */
	private void setupUploadButton() {
		binding.top3.btnUploadPic.setOnClickListener(view -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
	}
	
	/**
	 * Configures the click listener for the cancel upload button.
	 * When triggered, it clears the currently selected attachment from the ViewModel
	 * and resets the UI to show the initial upload prompt instead of the file details.
	 */
	private void setupCancelUploadListener() {
		binding.top3.btnCancelUpload.setOnClickListener(view -> {
			getViewModel().setSelectedScreenshot(null);
			resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		});
	}
	
	/**
	 * Retrieves text from the system clipboard and populates the feedback subject field.
	 * <p>
	 * This method uses {@link ClipboardHelper} to access the clipboard. If valid text
	 * is found, it updates the {@code editSubject} view with the clipboard content.
	 */
	private void setupPasteButton() {
		binding.top3.btnPasteIcon.setOnClickListener(view -> {
			buttonVibrate();
			fillSubjectFromClipboard();
		});
	}
	
	/**
	 * Configures the click listeners for all interactive buttons in the activity.
	 * <p>
	 * This includes setting up the back button, reaction selection buttons (Excellent,
	 * Good, Average, Poor, Angry), attachment upload and cancellation controls,
	 * the clipboard paste utility for the subject field, and the final feedback
	 * submission trigger.
	 */
	private void setupFeedbackButton() {
		binding.top3.btnSendFeedback.setOnClickListener(view -> {
			buttonVibrate();
			validateAndSendFeedback();
		});
	}
	
	/**
	 * Configures text change listeners for the feedback input fields.
	 * <p>
	 * Specifically, it monitors the feedback description field to update a real-time
	 * character counter. It also dynamically changes the counter's text color to
	 * an error state if the input exceeds the maximum limit of 500 characters.
	 */
	private void monitorFeedbackLength() {
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
	
	/**
	 * Configures LiveData observers for the {@link FeedbackViewModel}.
	 * <p>
	 * This method monitors the submission state to update the UI components accordingly:
	 * <ul>
	 *     <li><b>Submission State:</b> Toggles the enablement and opacity of the send button.</li>
	 *     <li><b>Success:</b> Triggers a success toast and resets the feedback form upon
	 *     successful transmission.</li>
	 *     <li><b>Error:</b> Displays a toast message containing the error details if the
	 *     submission fails.</li>
	 * </ul>
	 */
	private void initViewModelObservers() {
		handleFeedbackSubmission();
		observeSubmissionResult();
		observeSubmissionErrors();
	}
	
	/**
	 * Configures an observer for the feedback submission error LiveData.
	 * <p>
	 * This method monitors the {@link FeedbackViewModel} for any error messages
	 * generated during the submission process. When an error occurs, it displays
	 * a stylized error toast to the user and triggers a haptic vibration
	 * to provide immediate feedback.
	 */
	private void observeSubmissionErrors() {
		getViewModel().getSubmissionError().observe(this, errorMessage -> {
			if (errorMessage != null && !errorMessage.isEmpty()) {
				StylizedToastView.showError(FeedbackActivity.this, errorMessage);
				vibrate();
			}
		});
	}
	
	/**
	 * Observes the LiveData streams from the {@link FeedbackViewModel} to handle the UI state
	 * during and after the feedback submission process.
	 * <p>
	 * This method sets up observers for:
	 * <ul>
	 *     <li><b>Submission Loading State:</b> Disables the send button and shows a loading
	 *     indicator (text change and alpha) while the feedback is being transmitted.</li>
	 *     <li><b>Submission Success:</b> Displays a success toast, triggers haptic feedback,
	 *     and resets the form fields upon successful delivery.</li>
	 *     <li><b>Submission Error:</b> Displays an error toast and triggers haptic feedback
	 *     if the submission fails.</li>
	 * </ul>
	 */
	private void observeSubmissionResult() {
		getViewModel().getSubmissionSuccess().observe(this, isSuccess -> {
			if (isSuccess != null && isSuccess) {
				StylizedToastView.showSuccess(FeedbackActivity.this,
					StringHelper.getText(R.string.hint_feedback_sent_thank_you));
				resetFeedbackSubmission();
				vibrate();
			}
		});
	}
	
	/**
	 * Validates the user input and initiates the feedback submission process.
	 * <p>
	 * This method performs several validation checks:
	 * <ul>
	 *     <li>Ensures the subject field is not empty.</li>
	 *     <li>Ensures the feedback message field is not empty.</li>
	 *     <li>Ensures the feedback message does not exceed the maximum character limit (500).</li>
	 * </ul>
	 * If any validation fails, a toast message is displayed and the device vibrates.
	 * If validation passes, it calls the {@link FeedbackViewModel#sendFeedback} method
	 * to transmit the data to the server.
	 */
	private void handleFeedbackSubmission() {
		getViewModel().getIsSubmitting().observe(this, isSubmitting -> {
			binding.top3.btnSendFeedback.setEnabled(!isSubmitting);
			binding.top3.btnSendFeedback.setAlpha(isSubmitting ? 0.8f : 1.0f);
			binding.top3.txtSendFeedback.setText(isSubmitting ?
				R.string.hint_sending_feedback : R.string.btn_send_feedback);
			binding.top3.txtSendFeedback.setTextColor(
				isSubmitting ? getColor(R.color.color_primary) : getColor(R.color.color_surface)
			);
		});
	}
	
	/**
	 * Resets the feedback submission form to its initial state.
	 * This clears all input fields (subject, email, and feedback message),
	 * removes any selected attachments, and resets the attachment UI visibility.
	 */
	private void resetFeedbackSubmission() {
		binding.top3.editSubject.setText("");
		binding.top3.editEmail.setText("");
		binding.top3.editFeedback.setText("");
		resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		getViewModel().setSelectedScreenshot(null);
	}
	
	/**
	 * Updates the UI to reflect the user's selected feedback reaction.
	 * <p>
	 * This method updates the ViewModel with the chosen reaction, resets the visual state
	 * of all reaction icons and text labels to their default appearance, and then highlights
	 * the specific reaction that was selected by applying a bold typeface and primary color.
	 * </p>
	 *
	 * @param reaction         The string representation of the selected reaction.
	 * @param selectedImage    The {@link ImageView} corresponding to the selected reaction.
	 * @param selectedTextView The {@link TextView} label corresponding to the selected reaction.
	 */
	private void applyReactionSelection(@NonNull String reaction,
	                                    @NonNull ImageView selectedImage,
	                                    @NonNull TextView selectedTextView) {
		getViewModel().setSelectedReaction(reaction);
		
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
	
	/**
	 * Sets the "Excellent" feedback reaction as the default selected state
	 * upon initialization. This updates the ViewModel and highlights the
	 * corresponding UI components (happy icon and text).
	 */
	private void selectExcellentReactionByDefault() {
		applyReactionSelection(FeedbackReactions.Excellent.name(),
			binding.top2.imgHappy, binding.top2.txtHappy);
	}
	
	/**
	 * Fills the feedback subject field with text retrieved from the system clipboard.
	 * <p>
	 * This method retrieves any available text from the clipboard using {@link ClipboardHelper}.
	 * If the clipboard contains non-empty text, it populates the {@code editSubject} field.
	 */
	private void fillSubjectFromClipboard() {
		Context context = getBaseContext();
		CharSequence text = ClipboardHelper.getTextFromClipboard(context);
		if (text.length() > 0) {
			binding.top3.editSubject.setText(text);
		}
	}
	
	/**
	 * Validates the user input and initiates the feedback submission process.
	 * <p>
	 * This method performs several validation checks:
	 * <ul>
	 *     <li>Ensures the subject field is not empty.</li>
	 *     <li>Ensures the feedback message field is not empty.</li>
	 *     <li>Ensures the feedback message does not exceed the maximum character limit (500).</li>
	 * </ul>
	 * If any validation fails, a toast message is displayed and the device vibrates.
	 * If validation passes, it calls the {@link FeedbackViewModel#sendFeedback} method
	 * to transmit the data to the server.
	 */
	private void validateAndSendFeedback() {
		String subject = getSubjectText().toString().trim();
		String message = getFeedbackText().toString().trim();
		String email = getEmailText().toString().trim();
		
		if (subject.isEmpty()) {
			StylizedToastView.showWarning(this,
				StringHelper.getText(R.string.hint_please_give_subject));
			vibrate();
			return;
		}
		
		if (message.isEmpty()) {
			StylizedToastView.showWarning(this,
				StringHelper.getText(R.string.hint_please_describe_your_feedback));
			vibrate();
			return;
		}
		
		if (message.length() > 500) {
			StylizedToastView.showWarning(this,
				StringHelper.getText(R.string.hint_feedback_is_too_long));
			vibrate();
			return;
		}
		
		getViewModel().sendFeedback(this, getViewModel().getSelectedReaction().getValue(),
			subject, email, message, getViewModel().getSelectedScreenshot().getValue());
	}
	
	/**
	 * Constructs the formatted email body text based on the user's feedback input,
	 * selected reaction, and device information.
	 *
	 * @return A {@link String} containing the full email content to be sent.
	 */
	@NonNull private Editable getEmailText() {
		return Objects.requireNonNull(binding.top3.editEmail.getText());
	}
	
	/**
	 * Retrieves the current text entered in the feedback description field.
	 *
	 * @return A string containing the user's feedback message from the edit text view.
	 */
	@NonNull private Editable getFeedbackText() {
		return Objects.requireNonNull(binding.top3.editFeedback.getText());
	}
	
	/**
	 * Retrieves the current text from the subject input field.
	 *
	 * @return The subject text entered by the user, or an empty string if null.
	 */
	@NonNull private Editable getSubjectText() {
		return Objects.requireNonNull(binding.top3.editSubject.getText());
	}
}
