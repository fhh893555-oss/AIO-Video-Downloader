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
 * </p>
 *
 * <p><b>Architecture:</b>
 * The class follows the MVVM architecture, utilizing {@link FeedbackViewModel} to handle
 * data submission and state management. It also manages UI states for submission
 * progress, success, and error handling. LiveData observers keep the UI synchronized
 * with the ViewModel's state across configuration changes.
 * </p>
 *
 * <p><b>Validation Rules:</b>
 * <ul>
 *   <li>Subject must not be empty</li>
 *   <li>Feedback message must not be empty</li>
 *   <li>Feedback message must not exceed 500 characters</li>
 *   <li>Screenshot attachments must be less than 5MB</li>
 * </ul>
 * </p>
 *
 * <p><b>UI Features:</b>
 * <ul>
 *   <li>Five reaction buttons with visual highlighting for selected state</li>
 *   <li>Real-time character counter for feedback message (0/500 to 500/500)</li>
 *   <li>Clipboard paste button for quick subject input</li>
 *   <li>Image picker with file name and size display</li>
 *   <li>Loading state during submission with disabled send button</li>
 *   <li>Success/error toast notifications</li>
 * </ul>
 * </p>
 *
 * <p><b>Submission Flow:</b>
 * <ol>
 *   <li>User selects reaction (defaults to Excellent)</li>
 *   <li>User enters subject and feedback message</li>
 *   <li>Optionally attaches screenshot (max 5MB)</li>
 *   <li>Clicks send button → validation occurs</li>
 *   <li>If valid, ViewModel submits to server via FeedbackPocketbase</li>
 *   <li>UI shows loading state during network request</li>
 *   <li>On success: toast shown, form reset, return to previous screen</li>
 *   <li>On error: error toast shown, user can retry</li>
 * </ol>
 * </p>
 *
 * @see BaseActivity
 * @see FeedbackViewModel
 * @see FeedbackPocketbase
 * @see FeedbackReactions
 */
public class FeedbackActivity extends BaseActivity<ActivityFeedback1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private FeedbackViewModel viewModel;
	private ActivityResultLauncher<String> imagePickerLauncher;
	
	/**
	 * Inflates the view binding for the Feedback activity.
	 * <p>
	 * This method initializes the {@link ActivityFeedback1Binding} using the provided
	 * {@link LayoutInflater}, allowing for type-safe access to the layout's views.
	 * The binding provides direct references to all UI components including the
	 * reaction buttons, text inputs, and feedback submission controls.
	 * </p>
	 *
	 * @param inflater The {@link LayoutInflater} used to inflate the binding
	 * @return A new instance of {@link ActivityFeedback1Binding}
	 */
	@Override
	protected ActivityFeedback1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback1Binding.inflate(inflater);
	}
	
	/**
	 * Determines whether the activity should be locked to a specific orientation.
	 * <p>
	 * Overriding this method and returning {@code true} ensures that the feedback screen
	 * maintains a consistent orientation (typically portrait), preventing layout
	 * inconsistencies during rotation and ensuring that the feedback form layout
	 * remains stable while the user is typing or selecting attachments.
	 * </p>
	 *
	 * @return {@code true} to lock the orientation; {@code false} otherwise
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Initializes the activity's components and UI state once the layout has been loaded.
	 * <p>
	 * This method is called after the layout has been inflated and serves as the main
	 * initialization entry point. It performs the following setup in sequence:
	 * </p>
	 * <ol>
	 *   <li>Creates or retrieves the ViewModel instance</li>
	 *   <li>Initializes the image picker for screenshot attachments</li>
	 *   <li>Applies gradient styling to the "Feedback" title text</li>
	 *   <li>Sets up all button click listeners (back, reactions, upload, paste, send)</li>
	 *   <li>Monitors feedback text input with a character counter</li>
	 *   <li>Configures LiveData observers for submission state, success, and errors</li>
	 *   <li>Sets "Excellent" as the default selected reaction</li>
	 * </ol>
	 */
	@Override
	protected void onLoadedLayout() {
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
	 * <p>
	 * This method provides access to the ViewModel which manages the state and business logic
	 * for the feedback submission process, including reaction selection, screenshot attachments,
	 * submission progress tracking, and error handling. The ViewModel is created and retained
	 * by the ViewModelProvider and survives configuration changes.
	 * </p>
	 *
	 * @return The current {@link FeedbackViewModel} instance
	 */
	public FeedbackViewModel getViewModel() {
		return viewModel;
	}
	
	/**
	 * Initializes the {@link FeedbackViewModel} for this activity.
	 * <p>
	 * This method uses the {@link ViewModelProvider} to create or retrieve an existing
	 * instance of {@code FeedbackViewModel}, ensuring the ViewModel persists through
	 * configuration changes such as screen rotations. The ViewModel is scoped to this
	 * activity's lifecycle and will be cleared when the activity is finished.
	 * </p>
	 *
	 * <p><b>Initialization Details:</b>
	 * <ul>
	 *   <li>Uses the default ViewModelProvider with this activity as the scope</li>
	 *   <li>Returns an existing ViewModel if one already exists for this activity</li>
	 *   <li>Creates a new instance only on first request</li>
	 * </ul>
	 * </p>
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
	 *     <li>Registers an {@link ActivityResultLauncher} for the
	 *     {@link ActivityResultContracts.GetContent}
	 *     contract to handle file selection and validation.</li>
	 *     <li>Observes the ViewModel's selected screenshot LiveData to update the UI when
	 *     a file is successfully attached or removed.</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Lifecycle Integration:</b>
	 * The ActivityResultLauncher is automatically managed by the Activity Result API
	 * and is registered only once during activity creation to prevent redundant
	 * registrations and ensure proper lifecycle handling.
	 * </p>
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
	 * Validates the selected attachment file and updates the ViewModel if it meets requirements.
	 * <p>
	 * This method processes a file URI from the image picker, converts it to a DocumentFile,
	 * and checks if the file size is within the 5MB limit. Valid files are stored in the
	 * ViewModel, triggering UI updates to display the file name and size. Files exceeding
	 * the limit trigger haptic feedback and display a warning toast without updating the ViewModel.
	 * </p>
	 *
	 * @param fileUri The URI of the file selected from the image picker, or null if selection
	 *                was canceled
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
	 * This method toggles between two UI states: one where a selected attachment is
	 * displayed with file details and a cancel button, and another where the user
	 * is prompted to upload a new picture. This provides visual feedback as the
	 * user adds or removes attachments from their feedback submission.
	 * </p>
	 *
	 * @param attachmentVisibility The visibility state (e.g., {@link View#VISIBLE}, {@link View#GONE})
	 *                             for the container displaying the selected attachment details
	 * @param uploadPicVisibility  The visibility state (e.g., {@link View#VISIBLE}, {@link View#GONE})
	 *                             for the container prompting the user to upload a picture
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
	 * This creates a visually appealing gradient effect on the "Feedback" text.
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
	 * Initializes and configures the click listeners for all interactive UI elements in the activity.
	 * <p>
	 * This method serves as the central setup point for all button click handlers in the
	 * feedback screen. It delegates to specific setup methods for each UI component,
	 * ensuring clean separation of concerns and maintainable code organization.
	 * </p>
	 *
	 * <p><b>UI Components Configured:</b>
	 * <ul>
	 *     <li>The back button to close the activity</li>
	 *     <li>The reaction selection buttons (Excellent, Good, Average, Poor, Angry) to
	 *     update the UI and ViewModel</li>
	 *     <li>The attachment upload button to launch the image picker</li>
	 *     <li>The cancel upload button to remove selected screenshots</li>
	 *     <li>The paste button to populate the subject field from the clipboard</li>
	 *     <li>The send button to trigger feedback validation and submission</li>
	 * </ul>
	 * </p>
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
	 * <p>
	 * When clicked, it triggers a haptic vibration feedback to acknowledge the user's
	 * input and finishes the current activity, returning the user to the previous screen
	 * without submitting any feedback data.
	 * </p>
	 */
	private void setupBackButton() {
		binding.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
	}
	
	/**
	 * Configures click listeners for the feedback reaction buttons (Excellent, Good,
	 * Average, Poor, Angry).
	 * <p>
	 * This method maps each reaction layout to its corresponding selection logic, ensuring
	 * that clicking a reaction updates the ViewModel and reflects the selection visually in
	 * the UI. It also initializes the necessary references to images and text views for each
	 * reaction state.
	 * </p>
	 *
	 * <p><b>Reaction Options Configured:</b>
	 * <ul>
	 *   <li>Excellent - Happy face icon and text</li>
	 *   <li>Good - Thumbs up/positive icon and text</li>
	 *   <li>Average - Neutral face icon and text</li>
	 *   <li>Poor - Thumbs down/negative icon and text</li>
	 *   <li>Angry - Angry face icon and text</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Behavior:</b>
	 * When a reaction button is clicked, it triggers
	 * {@link #applyReactionSelection(String, ImageView, TextView)}
	 * which updates the ViewModel with the selected reaction, resets all reaction UI elements
	 * to their default unselected state, and highlights only the clicked reaction with a bold
	 * typeface, primary color, and selected visual state.
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
	 * <p>
	 * When clicked, it provides haptic feedback and launches an activity result contract
	 * to allow the user to select an image file from their device storage. The selected
	 * image will be attached to the feedback submission as a screenshot or visual reference.
	 * </p>
	 */
	private void setupUploadButton() {
		binding.top3.btnUploadPic.setOnClickListener(view -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
	}
	
	/**
	 * Configures the click listener for the cancel upload button.
	 * <p>
	 * When triggered, it clears the currently selected attachment from the ViewModel
	 * and resets the UI to show the initial upload prompt instead of the file details.
	 * This allows the user to discard a previously selected screenshot and optionally
	 * choose a different one.
	 * </p>
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
	 * This provides a convenient way for users to paste error logs, crash reports,
	 * or other relevant text without manually typing or long-pressing the input field.
	 * </p>
	 */
	private void setupPasteButton() {
		binding.top3.btnPasteIcon.setOnClickListener(view -> {
			buttonVibrate();
			fillSubjectFromClipboard();
		});
	}
	
	/**
	 * Configures the click listener for the feedback submission button.
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
	 * This method sets up all necessary observers to monitor the ViewModel's state
	 * and update the UI accordingly. It observes three key states:
	 * </p>
	 * <ul>
	 *     <li><b>Submission State:</b> Toggles the enablement, opacity, and text of the send button</li>
	 *     <li><b>Success Result:</b> Triggers a success toast and resets the feedback form upon
	 *     successful transmission</li>
	 *     <li><b>Error:</b> Displays a toast message containing the error details if the
	 *     submission fails</li>
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
	 * a stylized error toast to the user and triggers haptic vibration to provide
	 * immediate tactile feedback. The toast is only shown if the error message
	 * is non-null and not empty.
	 * </p>
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
	 * Observes the submission state and updates the UI accordingly during feedback transmission.
	 * <p>
	 * This method sets up a LiveData observer on the ViewModel's submission state. When feedback
	 * is being submitted, the send button is disabled, its opacity is reduced, the button text
	 * changes to a "sending" indicator, and the text color changes to primary. Once submission
	 * completes (success or failure), the button returns to its normal enabled state with the
	 * original text and color. This provides visual feedback to the user during network operations.
	 * </p>
	 *
	 * <p><b>UI State Transitions:</b>
	 * <ul>
	 *   <li><b>Submitting (isSubmitting = true):</b> Button disabled, alpha 0.8f, text "Sending...",
	 *       text color primary</li>
	 *   <li><b>Not Submitting (isSubmitting = false):</b> Button enabled, alpha 1.0f, text
	 *   "Send Feedback",
	 *       text color surface</li>
	 * </ul>
	 * </p>
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
	 * Resets the feedback submission form to its initial clean state.
	 * <p>
	 * This method clears all user input fields (subject, email, and feedback message),
	 * removes any selected screenshot attachments, and resets the attachment container
	 * UI visibility. It is typically called after a successful feedback submission or
	 * when the user chooses to start over with a new feedback entry.
	 * </p>
	 *
	 * <p><b>Actions Performed:</b>
	 * <ul>
	 *   <li>Clears the subject input field</li>
	 *   <li>Clears the email input field</li>
	 *   <li>Clears the feedback message field</li>
	 *   <li>Resets attachment container visibility (hides preview, shows add button)</li>
	 *   <li>Removes any selected screenshot from the ViewModel</li>
	 * </ul>
	 * </p>
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
	 * This method manages the visual state of all feedback reaction options (Happy, Good,
	 * Average, Poor, Angry) based on which one the user selected. It updates the ViewModel
	 * with the chosen reaction, resets all reaction icons and text labels to their default
	 * appearance (regular typeface, secondary text color, unselected state), and then
	 * highlights only the selected reaction by applying a bold typeface, primary color,
	 * and selected state to its corresponding UI components.
	 * </p>
	 *
	 * <p><b>Visual Effects Applied:</b>
	 * <ul>
	 *   <li><b>Selected Reaction:</b> Bold typeface, primary text color, selected image state</li>
	 *   <li><b>Unselected Reactions:</b> Regular typeface, secondary text color, unselected image state</li>
	 * </ul>
	 * </p>
	 *
	 * @param reaction         the string representation of the selected reaction
	 *                         (e.g., "Excellent", "Good", "Average", "Poor", "Angry")
	 * @param selectedImage    the {@link ImageView} corresponding to the selected reaction
	 * @param selectedTextView the {@link TextView} label corresponding to the selected reaction
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
	 * Sets the "Excellent" feedback reaction as the default selected state upon initialization.
	 * <p>
	 * This method is called during UI setup to establish a default feedback reaction
	 * before the user makes a selection. It updates the ViewModel with the Excellent
	 * reaction and applies visual highlighting to the corresponding UI components
	 * (happy icon and text) to indicate the default selection state.
	 * </p>
	 */
	private void selectExcellentReactionByDefault() {
		applyReactionSelection(FeedbackReactions.Excellent.name(),
			binding.top2.imgHappy, binding.top2.txtHappy);
	}
	
	/**
	 * Fills the feedback subject field with text retrieved from the system clipboard.
	 * <p>
	 * This method retrieves any available text from the device's clipboard using
	 * {@link ClipboardHelper}. If the clipboard contains non-empty text, it automatically
	 * populates the subject input field with that text as a convenience for the user.
	 * This can be useful when users want to paste error logs, crash reports, or other
	 * relevant information without manually typing or long-pressing the input field.
	 * </p>
	 *
	 * <p><b>Behavior:</b>
	 * <ul>
	 *   <li>Retrieves text from clipboard using ClipboardHelper</li>
	 *   <li>If clipboard text is present and non-empty, sets it as the subject text</li>
	 *   <li>If clipboard is empty or contains no text, no action is taken</li>
	 * </ul>
	 * </p>
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
	 * This method performs several validation checks on the user's input before
	 * submitting feedback to the server. It validates the subject field, feedback
	 * message field, and enforces a maximum character limit. If validation passes,
	 * it delegates to the ViewModel to send the feedback data.
	 * </p>
	 *
	 * <p><b>Validation Rules:</b>
	 * <ul>
	 *   <li>Subject field must not be empty</li>
	 *   <li>Feedback message must not be empty</li>
	 *   <li>Feedback message must not exceed 500 characters</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Validation Failure Handling:</b>
	 * If any validation fails, a warning toast is displayed and the device vibrates
	 * to alert the user. No network request is initiated in these cases.
	 * </p>
	 *
	 * <p><b>Data Submitted:</b>
	 * Upon successful validation, the following data is sent to the ViewModel:
	 * <ul>
	 *   <li>Selected reaction (e.g., emoji rating)</li>
	 *   <li>Subject line</li>
	 *   <li>Email address</li>
	 *   <li>Feedback message</li>
	 *   <li>Selected screenshot (if any)</li>
	 * </ul>
	 * </p>
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
	 * Retrieves the current text entered in the email address input field.
	 * <p>
	 * This method returns the user's email address as an Editable object,
	 * allowing the calling code to modify the text if needed. The email is used
	 * as the sender's reply-to address when submitting feedback.
	 * </p>
	 *
	 * @return an Editable containing the user's email address text
	 */
	@NonNull private Editable getEmailText() {
		return Objects.requireNonNull(binding.top3.editEmail.getText());
	}
	
	/**
	 * Retrieves the current text entered in the feedback description field.
	 * <p>
	 * This method returns the user's detailed feedback message as an Editable object.
	 * The feedback text typically contains the user's suggestions, bug reports,
	 * or general comments about the application.
	 * </p>
	 *
	 * @return an Editable containing the user's feedback message
	 */
	@NonNull private Editable getFeedbackText() {
		return Objects.requireNonNull(binding.top3.editFeedback.getText());
	}
	
	/**
	 * Retrieves the current text from the subject input field.
	 * <p>
	 * This method returns the user's chosen subject line for the feedback email
	 * as an Editable object. The subject provides a brief summary of the feedback
	 * content and helps with categorization on the receiving end.
	 * </p>
	 *
	 * @return an Editable containing the subject text entered by the user
	 */
	@NonNull private Editable getSubjectText() {
		return Objects.requireNonNull(binding.top3.editSubject.getText());
	}
}
