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
import androidx.lifecycle.LifecycleOwner;
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
 * Activity that allows users to submit feedback about the application. This screen
 * provides a comprehensive feedback form including reaction selection (Excellent,
 * Good, Average, Poor, Angry), subject line, optional email, detailed feedback
 * message (with 500-character limit), and optional screenshot attachment.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Displays five reaction options with visual selection highlighting.</li>
 * <li>Provides input fields for subject, email, and detailed feedback.</li>
 * <li>Shows real-time character counter (0/500) with color change on limit exceed.</li>
 * <li>Supports screenshot attachment with file size validation (max 5MB).</li>
 * <li>Offers clipboard paste shortcut for subject field.</li>
 * <li>Submits feedback to remote server via ViewModel.</li>
 * <li>Displays success/error toasts and disables UI during submission.</li>
 * </ul>
 *
 * <p><strong>Validation rules:</strong>
 * Subject cannot be empty, feedback cannot be empty or exceed 500 characters,
 * and attachments cannot exceed 5MB if provided.
 *
 * @see BaseActivity
 * @see ActivityFeedback1Binding
 * @see FeedbackViewModel
 * @see FeedbackReactions
 */
public class FeedbackActivity extends BaseActivity<ActivityFeedback1Binding> {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private FeedbackViewModel viewModel;
	private ActivityResultLauncher<String> imagePickerLauncher;
	
	/**
	 * Inflates the activity's layout using view binding and returns the generated
	 * binding instance for {@code activity_feedback1.xml}. This method is called
	 * during the base activity's {@code setContentView()} phase to create the
	 * binding object that provides type-safe access to all views in the layout.
	 *
	 * <p>The layout includes the following sections:
	 * <ul>
	 * <li>{@code top1} - Title section with gradient text effect.</li>
	 * <li>{@code top2} - Reaction selection area with five emoji/feedback options.</li>
	 * <li>{@code top3} - Input fields for subject, email, feedback message,
	 *     character counter, and action buttons.</li>
	 * </ul>
	 *
	 * @param inflater The layout inflater service used to create the view hierarchy.
	 *                 Must not be {@code null}.
	 * @return The {@link ActivityFeedback1Binding} instance containing references
	 * to all views defined in the feedback screen layout.
	 * @see BaseActivity#inflateBinding(LayoutInflater)
	 * @see ActivityFeedback1Binding
	 */
	@Override
	protected ActivityFeedback1Binding inflateBinding(LayoutInflater inflater) {
		return ActivityFeedback1Binding.inflate(inflater);
	}
	
	/**
	 * Determines whether the activity's screen orientation should be locked.
	 * This implementation returns {@code true}, forcing the feedback screen
	 * to remain in portrait mode regardless of device rotation.
	 *
	 * <p><strong>Design rationale:</strong>
	 * Locking the orientation ensures the reaction selection grid, input fields,
	 * character counter, and action buttons maintain a consistent layout while
	 * the user composes their feedback message, preventing unexpected UI
	 * reconfigurations that could interrupt typing or selection.
	 *
	 * @return {@code true} to lock the activity to portrait orientation.
	 * @see BaseActivity#shouldLockOrientation()
	 */
	@Override
	protected boolean shouldLockOrientation() {
		return true;
	}
	
	/**
	 * Performs post-layout initialization after the content view has been inflated.
	 * This method is invoked by the base activity at the end of {@code onCreate()}
	 * and is responsible for setting up the ViewModel, image picker, UI enhancements,
	 * button listeners, input monitoring, observers, and default reaction selection.
	 *
	 * <p><strong>Initialization order:</strong>
	 * <ol>
	 * <li>Initializes the ViewModel via {@link #initViewModel()}.</li>
	 * <li>Sets up the image picker for screenshot attachments via {@link #initImagePicker()}.</li>
	 * <li>Applies gradient span to the word "Feedback" via {@link #applyGradientToTitle()}.</li>
	 * <li>Configures all button click listeners via {@link #setupButtonClicks()}.</li>
	 * <li>Monitors feedback message length with character counter via {@link #monitorFeedbackLength()}.</li>
	 * <li>Initializes ViewModel LiveData observers via {@link #initViewModelObservers()}.</li>
	 * <li>Selects "Excellent" reaction as the default option via
	 * {@link #selectExcellentReactionByDefault()}.</li>
	 * </ol>
	 *
	 * @see BaseActivity#onLoadedLayout()
	 * @see #initViewModel()
	 * @see #initImagePicker()
	 * @see #applyGradientToTitle()
	 * @see #setupButtonClicks()
	 * @see #monitorFeedbackLength()
	 * @see #initViewModelObservers()
	 * @see #selectExcellentReactionByDefault()
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
	 * Returns the ViewModel instance associated with this feedback activity.
	 * This getter provides access to the ViewModel for observers and other
	 * methods that need to interact with the ViewModel's LiveData or call
	 * its methods.
	 *
	 * <p>The ViewModel is created in {@link #initViewModel()} and is scoped
	 * to the activity's lifecycle, surviving configuration changes.
	 *
	 * @return The {@link FeedbackViewModel} instance. Never {@code null} after
	 * {@link #initViewModel()} has been called.
	 * @see #initViewModel()
	 * @see FeedbackViewModel
	 */
	public FeedbackViewModel getViewModel() {
		return viewModel;
	}
	
	/**
	 * Initializes the ViewModel for the feedback screen. This method creates a
	 * {@link FeedbackViewModel} instance using the {@link ViewModelProvider} scoped
	 * to this activity. The ViewModel is responsible for managing UI-related data
	 * such as selected reaction, screenshot attachment, submission state, and
	 * LiveData for observing results and errors.
	 *
	 * <p>The ViewModel survives configuration changes (though orientation is locked)
	 * and retains data across the activity lifecycle, preventing data loss during
	 * recreations.
	 *
	 * @see ViewModelProvider
	 * @see FeedbackViewModel
	 * @see #getViewModel()
	 */
	private void initViewModel() {
		viewModel = new ViewModelProvider(this)
			.get(FeedbackViewModel.class);
	}
	
	/**
	 * Initializes the image picker functionality for attaching screenshots to feedback.
	 * This method performs the following setup:
	 * <ol>
	 * <li>Resets the attachment container UI to its default state (upload button visible,
	 *     preview hidden).</li>
	 * <li>Registers an {@link ActivityResultLauncher} using the
	 * {@link ActivityResultContracts.GetContent}
	 *     contract to allow users to select an image from their device storage.</li>
	 * <li>Observes the ViewModel's selected screenshot LiveData to update the UI
	 *     when a file is selected.</li>
	 * </ol>
	 *
	 * <p>When the user selects an image, the callback {@link #validateAttachmentFile(Uri)}
	 * is invoked to validate the file size. Upon successful validation, the ViewModel
	 * stores the selected file, and {@link #observeSelectedAttachmentFile(DocumentFile)}
	 * updates the UI to show the file name and size.
	 *
	 * @see #validateAttachmentFile(Uri)
	 * @see #observeSelectedAttachmentFile(DocumentFile)
	 * @see #resetUploadAttachmentContainer(int, int)
	 * @see ActivityResultContracts.GetContent
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
	 * Observes and displays the selected screenshot attachment in the UI.
	 * When the ViewModel's selected screenshot LiveData emits a non-null
	 * {@link DocumentFile}, this method:
	 * <ul>
	 * <li>Shows the attachment preview container and hides the upload button.</li>
	 * <li>Displays the selected file's name in the attachment TextView.</li>
	 * <li>Displays the file size in a human-readable format (e.g., "123 KB").</li>
	 * </ul>
	 *
	 * <p>If the {@code documentFile} parameter is {@code null}, the method returns
	 * early without updating the UI, maintaining the current attachment state.
	 *
	 * @param documentFile The selected screenshot file, or {@code null} if no
	 *                     file is currently selected. Must contain a valid name
	 *                     for proper display.
	 * @see DocumentFile#getName()
	 * @see DocumentFile#length()
	 * @see FileStorageUtility#humanReadableSizeOf(long)
	 * @see #resetUploadAttachmentContainer(int, int)
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
	 * Validates an image file selected by the user for attachment to feedback.
	 * This method checks the file size and accepts it only if it is less than
	 * 5 megabytes (5 * 1024 * 1024 bytes).
	 *
	 * <p><strong>Validation behavior:</strong>
	 * <ul>
	 * <li>If the file size is less than 5MB, the screenshot is stored in the
	 *     ViewModel via {@link FeedbackViewModel#setSelectedScreenshot(DocumentFile)}.</li>
	 * <li>If the file size exceeds 5MB, a warning toast is displayed, and a
	 *     50ms haptic vibration is triggered. The file is rejected.</li>
	 * <li>If the file URI is {@code null}, the method does nothing.</li>
	 * </ul>
	 *
	 * <p>The 5MB limit ensures that feedback submissions remain reasonably sized
	 * for network transmission and server storage constraints.
	 *
	 * @param fileUri The URI of the image file selected by the user. May be
	 *                {@code null}, in which case validation is skipped.
	 * @see DocumentFile#fromSingleUri(android.content.Context, android.net.Uri)
	 * @see DocumentFile#length()
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 * @see #vibrate(long)
	 * @see StylizedToastView#showWarning(BaseActivity, CharSequence)
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
	 * Resets the visibility state of the attachment upload UI components.
	 * This method controls two containers: the attachment preview container
	 * (shown when a file is attached) and the upload picture button container
	 * (shown when no file is attached).
	 *
	 * <p><strong>Typical usage patterns:</strong>
	 * <ul>
	 * <li>When a file is selected: hide upload button, show attachment preview.</li>
	 * <li>When upload is canceled: show upload button, hide attachment preview.</li>
	 * <li>After successful submission: reset to default (show upload button).</li>
	 * </ul>
	 *
	 * @param attachmentVisibility The visibility state for the attachment preview
	 *                             container (e.g., {@link View#VISIBLE}, {@link View#GONE}).
	 * @param uploadPicVisibility  The visibility state for the upload picture button
	 *                             container (e.g., {@link View#VISIBLE}, {@link View#GONE}).
	 * @see View#setVisibility(int)
	 * @see #resetFeedbackSubmission()
	 * @see #setupCancelUploadListener()
	 */
	private void resetUploadAttachmentContainer(int attachmentVisibility,
	                                            int uploadPicVisibility) {
		binding.top3.contAttachmentSelected.setVisibility(attachmentVisibility);
		binding.top3.contUploadPic.setVisibility(uploadPicVisibility);
	}
	
	/**
	 * Applies a gradient color span to the word "Feedback" within the feedback screen
	 * title text view. This method searches for the substring "Feedback" in the full
	 * title text and, if found, applies a gradient effect using
	 * {@link TextViewsUtils#applyGradientSpan(android.widget.TextView, int, int, int, int)}.
	 *
	 * <p><strong>Visual effect:</strong>
	 * The gradient transitions from {@code color_secondary} to
	 * {@code color_primary_variant}, spanning the 8 characters of the word
	 * "Feedback". This creates a highlighted, branded appearance for the key word
	 * in the feedback screen title, drawing user attention to the purpose of the screen.
	 *
	 * <p><strong>Error handling:</strong>
	 * If the word "Feedback" is not found in the title string (e.g., due to
	 * localization changes), the method silently does nothing without throwing
	 * an exception.
	 *
	 * @see TextViewsUtils#applyGradientSpan(android.widget.TextView, int, int, int, int)
	 * @see #getColor(int)
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
	 * Initializes all button click listeners for the feedback screen. This method
	 * aggregates the setup calls for each interactive UI component, ensuring all
	 * user input elements respond appropriately to clicks.
	 *
	 * <p><strong>Buttons configured:</strong>
	 * <ul>
	 * <li>Back button via {@link #setupBackButton()}</li>
	 * <li>Reaction selection options via {@link #setupReactionListeners()}</li>
	 * <li>Upload picture button via {@link #setupUploadButton()}</li>
	 * <li>Cancel upload button via {@link #setupCancelUploadListener()}</li>
	 * <li>Paste from clipboard button via {@link #setupPasteButton()}</li>
	 * <li>Send feedback button via {@link #setupFeedbackButton()}</li>
	 * </ul>
	 *
	 * @see #setupBackButton()
	 * @see #setupReactionListeners()
	 * @see #setupUploadButton()
	 * @see #setupCancelUploadListener()
	 * @see #setupPasteButton()
	 * @see #setupFeedbackButton()
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
	 * Configures the back button click listener. When the user clicks the back button,
	 * this method triggers a short haptic vibration and finishes the current activity,
	 * returning the user to the previous screen.
	 *
	 * <p>The haptic feedback provides tactile confirmation of the button press,
	 * enhancing the user experience with a subtle vibration.
	 *
	 * @see #buttonVibrate()
	 * @see #finish()
	 */
	private void setupBackButton() {
		binding.btnBack.setOnClickListener(view -> {
			buttonVibrate();
			finish();
		});
	}
	
	/**
	 * Configures click listeners for all five feedback reaction options (Excellent,
	 * Good, Average, Poor, Angry). Each reaction is represented by a LinearLayout
	 * containing an ImageView (emoji/icon) and a TextView (label). When a reaction
	 * is clicked, the method delegates to
	 * {@link #applyReactionSelection(String, ImageView, TextView)} to update the UI
	 * and ViewModel state.
	 *
	 * <p><strong>Reaction components mapping:</strong>
	 * <ul>
	 * <li>Excellent → {@code btnHappy}, {@code imgHappy}, {@code txtHappy}</li>
	 * <li>Good → {@code btnGood}, {@code imgGood}, {@code txtGood}</li>
	 * <li>Average → {@code btnAverage}, {@code imgAverage}, {@code txtAverage}</li>
	 * <li>Poor → {@code btnPoor}, {@code imgPoor}, {@code txtPoor}</li>
	 * <li>Angry → {@code btnAngry}, {@code imgAngry}, {@code txtAngry}</li>
	 * </ul>
	 *
	 * <p>Each reaction name is obtained from the {@link FeedbackReactions} enum
	 * using the {@link Enum#name()} method. The default reaction (Excellent) is
	 * pre-selected via {@link #selectExcellentReactionByDefault()} during
	 * ViewModel initialization.
	 *
	 * @see #applyReactionSelection(String, ImageView, TextView)
	 * @see FeedbackReactions
	 * @see #selectExcellentReactionByDefault()
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
	 * Configures the upload picture button click listener. When the user clicks the
	 * upload button, this method triggers a short haptic vibration and launches the
	 * system image picker to allow the user to select a screenshot or image to
	 * attach with their feedback. The launcher uses the {@link ActivityResultLauncher}
	 * with MIME type "image/*" to accept all image formats.
	 *
	 * <p>After an image is selected, it is processed and stored in the ViewModel
	 * via the selected screenshot LiveData. The UI then updates to show a preview
	 * of the attached image and the cancel upload button.
	 *
	 * @see #buttonVibrate()
	 * @see #imagePickerLauncher
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 */
	private void setupUploadButton() {
		binding.top3.btnUploadPic.setOnClickListener(view -> {
			buttonVibrate();
			imagePickerLauncher.launch("image/*");
		});
	}
	
	/**
	 * Configures the cancel upload button click listener. When the user clicks the
	 * cancel button while a screenshot is attached, this method clears the selected
	 * screenshot from the ViewModel and resets the attachment container UI to its
	 * default state. (Hiding the preview and showing the add attachment button.)
	 *
	 * <p>The reset operation uses {@link #resetUploadAttachmentContainer(int, int)}
	 * with {@link View#GONE} for hiding the preview container and {@link View#VISIBLE}
	 * for showing the add attachment button.
	 *
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 * @see #resetUploadAttachmentContainer(int, int)
	 */
	private void setupCancelUploadListener() {
		binding.top3.btnCancelUpload.setOnClickListener(view -> {
			getViewModel().setSelectedScreenshot(null);
			resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		});
	}
	
	/**
	 * Configures the paste button click listener. When the user clicks the paste icon,
	 * this method triggers a short haptic vibration via {@link #buttonVibrate()} and
	 * populates the subject input field with text retrieved from the system clipboard
	 * via {@link #fillSubjectFromClipboard()}.
	 *
	 * <p>This feature provides a convenient way for users to quickly paste error
	 * messages, log output, or other relevant text into the feedback subject field
	 * without manual typing.
	 *
	 * @see #buttonVibrate()
	 * @see #fillSubjectFromClipboard()
	 */
	private void setupPasteButton() {
		binding.top3.btnPasteIcon.setOnClickListener(view -> {
			buttonVibrate();
			fillSubjectFromClipboard();
		});
	}
	
	/**
	 * Configures the send feedback button click listener. When the user clicks the
	 * send button, this method triggers a short haptic vibration and initiates the
	 * feedback validation and submission process via {@link #validateAndSendFeedback()}.
	 *
	 * <p>The button is automatically disabled during active submission (handled by
	 * {@link #handleFeedbackSubmission()}) to prevent duplicate submissions while
	 * a network request is in progress.
	 *
	 * @see #buttonVibrate()
	 * @see #validateAndSendFeedback()
	 * @see #handleFeedbackSubmission()
	 */
	private void setupFeedbackButton() {
		binding.top3.btnSendFeedback.setOnClickListener(view -> {
			buttonVibrate();
			validateAndSendFeedback();
		});
	}
	
	/**
	 * Monitors the length of the feedback message input field and updates a
	 * character count display in real time. This method attaches a text change
	 * listener to the feedback EditText that updates a TextView showing the
	 * current character count (e.g., "42/500").
	 *
	 * <p><strong>Visual behavior:</strong>
	 * <ul>
	 * <li>The counter displays as "{currentLength}/500".</li>
	 * <li>When the input length exceeds 500 characters, the counter text color
	 *     changes to error color ({@code color_error}) to warn the user.</li>
	 * <li>When within the limit (≤ 500 characters), the counter uses secondary
	 *     text color ({@code color_text_secondary}).</li>
	 * </ul>
	 *
	 * <p>This validation prevents submission of overly long feedback, as the
	 * validation logic in {@link #validateAndSendFeedback()} rejects messages
	 * longer than 500 characters.
	 *
	 * @see #validateAndSendFeedback()
	 * @see android.text.TextWatcher
	 * @see EditTextListener
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
	 * Initializes all LiveData observers for the feedback ViewModel. This method
	 * aggregates the setup of UI state observers, including submission state,
	 * submission success result, and submission error handling.
	 *
	 * <p><strong>Observers initialized:</strong>
	 * <ul>
	 * <li>{@link #handleFeedbackSubmission()} - Updates button UI during submission.</li>
	 * <li>{@link #observeSubmissionResult()} - Handles successful submission.</li>
	 * <li>{@link #observeSubmissionErrors()} - Displays error messages.</li>
	 * </ul>
	 *
	 * @see #handleFeedbackSubmission()
	 * @see #observeSubmissionResult()
	 * @see #observeSubmissionErrors()
	 */
	private void initViewModelObservers() {
		handleFeedbackSubmission();
		observeSubmissionResult();
		observeSubmissionErrors();
	}
	
	/**
	 * Observes submission error messages from the ViewModel and displays them to
	 * the user. When the LiveData emits a non-null, non-empty error message,
	 * this method shows an error toast with the message and triggers haptic
	 * feedback to alert the user.
	 *
	 * <p>Common error scenarios include:
	 * <ul>
	 * <li>Network connectivity issues (no internet).</li>
	 * <li>Server-side validation failures.</li>
	 * <li>Timeout or server unavailability.</li>
	 * <li>Authentication or permission errors.</li>
	 * </ul>
	 *
	 * @see FeedbackViewModel#getSubmissionError()
	 * @see StylizedToastView#showError(BaseActivity, CharSequence)
	 * @see #vibrate()
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
	 * Observes the feedback submission result LiveData from the ViewModel. When a
	 * successful submission occurs, this method displays a success toast message,
	 * resets all input fields and attachments, and triggers haptic feedback.
	 *
	 * <p>The observer is lifecycle-aware, automatically cleaning up when the
	 * activity is destroyed. Success is indicated by {@code isSuccess} being
	 * {@code true}. The observed LiveData is typically updated by the ViewModel
	 * after a network request completes.
	 *
	 * @see FeedbackViewModel#getSubmissionSuccess()
	 * @see #resetFeedbackSubmission()
	 * @see StylizedToastView#showSuccess(BaseActivity, CharSequence)
	 * @see #vibrate()
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
	 * Observes the submission state LiveData to update the UI during feedback submission.
	 * While feedback is being sent, this method disables the send button, reduces its
	 * opacity, changes the button text to "Sending...", and changes the text color.
	 * When submission completes, the button returns to its default enabled state.
	 *
	 * <p><strong>UI changes during submission:</strong>
	 * <ul>
	 * <li>Send button is disabled (prevents duplicate submissions).</li>
	 * <li>Button alpha reduced to 0.8f (visual indicator of disabled state).</li>
	 * <li>Button text changes to "Sending feedback".</li>
	 * <li>Button text color changes to primary color.</li>
	 * </ul>
	 *
	 * @see FeedbackViewModel#getIsSubmitting()
	 * @see android.widget.Button#setEnabled(boolean)
	 * @see android.widget.Button#setAlpha(float)
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
	 * Resets all feedback input fields and clears the selected screenshot after a
	 * successful submission. This method clears the subject, email, and feedback
	 * text fields, resets the attachment container visibility (hiding the preview
	 * and showing the add button), and clears the selected screenshot from the
	 * ViewModel.
	 *
	 * <p>The attachment container reset uses {@link #resetUploadAttachmentContainer(int, int)}
	 * with parameters {@link View#GONE} for hiding preview and {@link View#VISIBLE}
	 * for showing the add attachment button.
	 *
	 * @see #resetUploadAttachmentContainer(int, int)
	 * @see FeedbackViewModel#setSelectedScreenshot(DocumentFile)
	 * @see android.widget.EditText#setText(CharSequence)
	 */
	private void resetFeedbackSubmission() {
		binding.top3.editSubject.setText("");
		binding.top3.editEmail.setText("");
		binding.top3.editFeedback.setText("");
		resetUploadAttachmentContainer(View.GONE, View.VISIBLE);
		getViewModel().setSelectedScreenshot(null);
	}
	
	/**
	 * Applies the visual selection state for a feedback reaction option. This method
	 * updates the ViewModel with the selected reaction, resets all reaction UI
	 * components to their default (unselected) state, then highlights the specific
	 * reaction's image and text to indicate current selection.
	 *
	 * <p><strong>Visual changes applied to all reactions (reset):</strong>
	 * <ul>
	 * <li>All images have {@code selected} state set to {@code false}</li>
	 * <li>All text views have regular font typeface</li>
	 * <li>All text views have secondary text color</li>
	 * </ul>
	 *
	 * <p><strong>Visual changes applied to the selected reaction:</strong>
	 * <ul>
	 * <li>Selected image's {@code selected} state set to {@code true}</li>
	 * <li>Selected text view gets primary color</li>
	 * <li>Selected text view gets bold font typeface</li>
	 * </ul>
	 *
	 * <p>Supported reactions: Happy (Excellent), Good, Average, Poor, Angry.
	 *
	 * @param reaction         The reaction string value to store in the ViewModel
	 *                         (e.g., "Excellent", "Good", "Average", "Poor", "Angry").
	 * @param selectedImage    The ImageView corresponding to the selected reaction.
	 * @param selectedTextView The TextView corresponding to the selected reaction.
	 * @see FeedbackViewModel#setSelectedReaction(String)
	 * @see #selectExcellentReactionByDefault()
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
	 * Sets the "Excellent" (Happy) reaction as the default selected option when the
	 * feedback screen is first loaded. This method delegates to
	 * {@link #applyReactionSelection(String, ImageView, TextView)} with the
	 * Happy reaction's image and text views.
	 *
	 * <p>The default selection ensures that the user is never left with no reaction
	 * selected, and it guides users toward a positive default response while still
	 * allowing them to change their selection freely.
	 *
	 * @see FeedbackReactions#Excellent
	 * @see #applyReactionSelection(String, ImageView, TextView)
	 */
	private void selectExcellentReactionByDefault() {
		applyReactionSelection(FeedbackReactions.Excellent.name(),
			binding.top2.imgHappy, binding.top2.txtHappy);
	}
	
	/**
	 * Fills the feedback subject input field with text retrieved from the system
	 * clipboard. This method obtains the current clipboard content via
	 * {@link ClipboardHelper#getTextFromClipboard(Context)} and, if the text is
	 * non-empty, sets it as the subject field's text.
	 *
	 * <p>This convenience feature allows users to quickly paste a pre-copied subject
	 * line (e.g., from an error message or log) without manually typing it.
	 *
	 * <p>If the clipboard is empty or contains no text, the method does nothing
	 * and the subject field remains unchanged.
	 *
	 * @see ClipboardHelper#getTextFromClipboard(Context)
	 * @see android.widget.EditText#setText(CharSequence)
	 */
	private void fillSubjectFromClipboard() {
		Context context = getBaseContext();
		CharSequence text = ClipboardHelper.getTextFromClipboard(context);
		if (text.length() > 0) {
			binding.top3.editSubject.setText(text);
		}
	}
	
	/**
	 * Validates the user's feedback input and initiates the submission process if all
	 * validation checks pass. This method extracts the subject, message, and email
	 * from the input fields, performs validation on each field, and displays warning
	 * toasts with haptic feedback for any validation failures.
	 *
	 * <p><strong>Validation rules:</strong>
	 * <ul>
	 * <li>Subject must not be empty. If empty → warning toast + vibration.</li>
	 * <li>Feedback message must not be empty. If empty → warning toast + vibration.</li>
	 * <li>Feedback message must not exceed 500 characters. If exceeded → warning
	 *     toast + vibration.</li>
	 * </ul>
	 *
	 * <p>If all validation checks pass, the method delegates to
	 * {@link FeedbackViewModel#sendFeedback(LifecycleOwner, String, String, String, String, DocumentFile)}
	 * to submit the feedback along with the selected reaction type and screenshot
	 * (if any). The email field is optional and may be empty.
	 *
	 * @see #getSubjectText()
	 * @see #getFeedbackText()
	 * @see #getEmailText()
	 * @see #vibrate()
	 * @see StylizedToastView#showWarning(BaseActivity, CharSequence)
	 * @see FeedbackViewModel#sendFeedback(LifecycleOwner, String, String, String, String, DocumentFile)
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
	 * Retrieves the text content from the email input field as a non-null Editable.
	 * This method uses {@link Objects#requireNonNull(Object)} to ensure the returned
	 * value is never {@code null}, even if the underlying EditText returns null
	 * (which should not happen under normal circumstances).
	 *
	 * <p>The returned {@link Editable} allows direct manipulation of the text content
	 * if needed, though typically the text is read for validation or submission.
	 *
	 * @return The non-null {@link Editable} containing the user's entered email address.
	 * @throws NullPointerException If the EditText's text is unexpectedly null.
	 * @see android.widget.EditText#getText()
	 */
	@NonNull private Editable getEmailText() {
		return Objects.requireNonNull(binding.top3.editEmail.getText());
	}
	
	/**
	 * Retrieves the text content from the feedback input field as a non-null Editable.
	 * This method uses {@link Objects#requireNonNull(Object)} to guarantee a non-null
	 * return value, providing safe access to the user's feedback message text.
	 *
	 * <p>The returned Editable can be converted to a String via {@link Editable#toString()}
	 * for validation, storage, or submission to the backend.
	 *
	 * @return The non-null {@link Editable} containing the user's entered feedback text.
	 * @throws NullPointerException If the EditText's text is unexpectedly null.
	 * @see android.widget.EditText#getText()
	 */
	@NonNull private Editable getFeedbackText() {
		return Objects.requireNonNull(binding.top3.editFeedback.getText());
	}
	
	/**
	 * Retrieves the text content from the subject input field as a non-null Editable.
	 * This method uses {@link Objects#requireNonNull(Object)} to ensure the returned
	 * value is never {@code null}, providing safe access to the user's subject line text.
	 *
	 * <p>The subject field is typically used to categorize the feedback (e.g., "Bug Report",
	 * "Feature Request", "General Inquiry") before submission.
	 *
	 * @return The non-null {@link Editable} containing the user's entered subject text.
	 * @throws NullPointerException If the EditText's text is unexpectedly null.
	 * @see android.widget.EditText#getText()
	 */
	@NonNull private Editable getSubjectText() {
		return Objects.requireNonNull(binding.top3.editSubject.getText());
	}
}