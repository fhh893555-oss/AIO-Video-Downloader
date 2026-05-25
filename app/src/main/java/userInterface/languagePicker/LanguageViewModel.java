package userInterface.languagePicker;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.nextgen.R;

import java.util.ArrayList;
import java.util.List;

import coreUtils.library.process.LoggerUtils;

/**
 * ViewModel implementation responsible for preparing and managing data structures
 * related to the application's language selection interface.
 * <p>
 * This class isolates the language dataset instantiation logic from the View lifecycle,
 * leveraging {@link LiveData} streams to publish the available localized options asynchronously
 * to active UI controllers (Activities or Fragments).
 * </p>
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Provides a LiveData list of available language options for UI observation</li>
 *   <li>Loads and manages the language dataset containing 15 supported languages</li>
 *   <li>Survives configuration changes (screen rotations) preserving language data</li>
 *   <li>Separates data management from UI components for better testability</li>
 * </ul>
 * </p>
 *
 * <p><b>Supported Languages:</b>
 * The ViewModel loads 15 languages including English and 14 Indian regional languages:
 * Hindi, Tamil, Telugu, Punjabi, Marathi, Gujarati, Malayalam, Bengali, Odia, Kannada,
 * Assamese, Bhojpuri, Haryanvi, and Rajasthani.
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * public class LanguageActivity extends BaseActivity {
 *     private LanguageViewModel viewModel;
 *
 *     private void initViewModel() {
 *         viewModel = new ViewModelProvider(this).get(LanguageViewModel.class);
 *         viewModel.getLanguages().observe(this, languages -> {
 *             languageAdapter.setLanguages(languages);
 *         });
 *     }
 * }
 * </pre>
 * </p>
 *
 * @see ViewModel
 * @see LiveData
 * @see LanguageItem
 */
public class LanguageViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final MutableLiveData<List<LanguageItem>> languages = new MutableLiveData<>();
	
	/**
	 * Constructs a new LanguageViewModel and initializes the language list.
	 * <p>
	 * This constructor creates a new ViewModel instance and immediately loads
	 * the available language options by calling {@link #loadLanguages()}.
	 * The language data is stored in a LiveData object for observation by UI components.
	 * </p>
	 */
	public LanguageViewModel() {
		loadLanguages();
	}
	
	/**
	 * Returns the LiveData containing the list of available language options.
	 * <p>
	 * UI components can observe this LiveData to receive updates when the language
	 * list changes. The language list is loaded once during ViewModel initialization
	 * and remains constant throughout the ViewModel's lifecycle.
	 * </p>
	 *
	 * @return A LiveData object holding an immutable list of LanguageItem objects
	 */
	public LiveData<List<LanguageItem>> getLanguages() {
		return languages;
	}
	
	/**
	 * Loads and populates the list of available language options for selection.
	 * <p>
	 * This method creates a list of LanguageItem objects representing all supported
	 * languages in the application. Each language item includes:
	 * </p>
	 * <ul>
	 *   <li>Display name in the native script (e.g., "हिन्दी", "தமிழ்")</li>
	 *   <li>ISO language code for locale configuration (e.g., "hi", "ta")</li>
	 *   <li>Background illustration drawable resource</li>
	 *   <li>Background color resource for visual styling</li>
	 * </ul>
	 *
	 * <p><b>Supported Languages:</b>
	 * English, Hindi, Tamil, Telugu, Punjabi, Marathi, Gujarati, Malayalam,
	 * Bengali, Odia, Kannada, Assamese, Bhojpuri, Haryanvi, Rajasthani
	 * </p>
	 */
	private void loadLanguages() {
		List<LanguageItem> list = new ArrayList<>();
		list.add(new LanguageItem("English", "en", R.drawable.img_english_lang_bg, R.color.lang_english_bg_color));
		list.add(new LanguageItem("हिन्दी", "hi", R.drawable.img_hindi_lang_bg, R.color.lang_hindi_bg_color));
		list.add(new LanguageItem("தமிழ்", "ta", R.drawable.img_tamil_lang_bg, R.color.lang_tamil_bg_color));
		list.add(new LanguageItem("తెలుగు", "te", R.drawable.img_telugu_lang_bg, R.color.lang_telugu_bg_color));
		list.add(new LanguageItem("ਪੰਜਾਬੀ", "pa", R.drawable.img_punjabi_lang_bg, R.color.lang_punjabi_bg_color));
		list.add(new LanguageItem("मराठी", "mr", R.drawable.img_marathi_lang_bg, R.color.lang_marathi_bg_color));
		list.add(new LanguageItem("ગુજરાતી", "gu", R.drawable.img_gujarati_lang_bg, R.color.lang_gujarati_bg_color));
		list.add(new LanguageItem("മലയാളം", "ml", R.drawable.img_malayalam_lang_bg, R.color.lang_malayalam_bg_color));
		list.add(new LanguageItem("বাংলা", "bn", R.drawable.img_bengali_lang_bg, R.color.lang_bengali_bg_color));
		list.add(new LanguageItem("ଓଡ଼ିଆ", "or", R.drawable.img_odia_lang_bg, R.color.lang_odia_bg_color));
		list.add(new LanguageItem("ಕನ್ನಡ", "kn", R.drawable.img_kannada_lang_bg, R.color.lang_kannada_bg_color));
		list.add(new LanguageItem("অসামীয়া", "as", R.drawable.img_assamese_lang_bg, R.color.lang_assamese_bg_color));
		list.add(new LanguageItem("भोजपुरी", "bho", R.drawable.img_bhojpuri_lang_bg, R.color.lang_bhojpuri_bg_color));
		list.add(new LanguageItem("हरियाणवी", "hry", R.drawable.img_haryanvi_lang_bg, R.color.lang_haryanvi_bg_color));
		list.add(new LanguageItem("राजस्थानी", "raj", R.drawable.img_rajasthani_lang_bg,
			R.color.lang_rajasthani_bg_color));
		languages.setValue(list);
	}
}