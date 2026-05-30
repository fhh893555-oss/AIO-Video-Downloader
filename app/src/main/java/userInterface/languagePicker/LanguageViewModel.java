package userInterface.languagePicker;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;

import java.util.ArrayList;
import java.util.List;

import coreUtils.library.process.LoggerUtils;

/**
 * ViewModel for the language selection screen. This class is responsible for
 * preparing and managing the list of available languages for the UI layer
 * (typically {@link LanguageActivity}). It separates language data loading
 * logic from the Activity, ensuring data survives configuration changes
 * such as screen rotations (though orientation is locked in this specific
 * Activity).
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Loads the list of supported languages via {@link #loadLanguages()}.</li>
 * <li>Exposes the language list as a {@link LiveData} for UI observation.</li>
 * <li>Maintains the language data across Activity lifecycle events.</li>
 * </ul>
 *
 * <p>The ViewModel is scoped to the Activity's lifecycle and is typically
 * obtained using {@link ViewModelProvider}. The language
 * list is loaded once during ViewModel construction and retained until the
 * associated Activity is permanently destroyed.
 *
 * <p><strong>Usage example:</strong>
 * <pre>
 * LanguageViewModel viewModel = new ViewModelProvider(this)
 *     .get(LanguageViewModel.class);
 * viewModel.getLanguages().observe(this, languages -> {
 *     // Update adapter with new language list
 * });
 * </pre>
 *
 * @see ViewModel
 * @see LanguageItem
 * @see #getLanguages()
 * @see #loadLanguages()
 */
public class LanguageViewModel extends ViewModel {
	
	private final LoggerUtils logger = LoggerUtils.from(getClass());
	private final MutableLiveData<List<LanguageItem>> languages = new MutableLiveData<>();
	
	/**
	 * Constructs a new LanguageViewModel instance. Upon creation, this constructor
	 * immediately triggers the loading of available language data by calling
	 * {@link #loadLanguages()}. The ViewModel is typically instantiated via
	 * {@link ViewModelProvider} in the associated Activity.
	 *
	 * <p>The loaded language list is stored internally as a {@code MutableLiveData}
	 * and exposed via {@link #getLanguages()} for observation by the UI layer.
	 *
	 * @see #loadLanguages()
	 * @see #getLanguages()
	 */
	public LanguageViewModel() {
		loadLanguages();
	}
	
	/**
	 * Returns a LiveData object that holds the list of available languages for
	 * selection. The UI layer (typically {@link LanguageActivity}) observes this
	 * LiveData to receive updates when the language list is loaded or modified.
	 *
	 * <p>The returned LiveData is immutable from the observer's perspective,
	 * ensuring that the UI can only read the data while the ViewModel retains
	 * control over updates. Language items are populated asynchronously via
	 * {@link #loadLanguages()}, and the LiveData automatically notifies
	 * observers when the data becomes available.
	 *
	 * @return A LiveData containing the current list of {@link LanguageItem} objects.
	 * Never returns {@code null}, though the list may be empty during
	 * initial loading.
	 * @see LiveData
	 * @see #loadLanguages()
	 */
	public LiveData<List<LanguageItem>> getLanguages() {
		return languages;
	}
	
	/**
	 * Loads and populates the list of available languages for the language selection
	 * screen. This method creates a comprehensive collection of {@link LanguageItem}
	 * objects representing 15 languages, including major Indian languages and English.
	 * Each language item includes display name, ISO language code, flag illustration,
	 * and a unique background color resource for visual distinction in the grid.
	 *
	 * <p><strong>Languages included:</strong>
	 * English, Hindi, Tamil, Telugu, Punjabi, Marathi, Gujarati, Malayalam, Bengali,
	 * Odia, Kannada, Assamese, Bhojpuri, Haryanvi, Rajasthani.
	 *
	 * <p>After constructing the list, the method posts the collection to the
	 * {@code languages} MutableLiveData, which triggers observation in the
	 * UI layer via {@link LanguageActivity#bindLanguageData(LanguageViewModel)}.
	 * The LiveData pattern ensures the RecyclerView adapter is automatically
	 * updated when the language list is loaded.
	 *
	 * <p><strong>Resource references:</strong>
	 * Each language has corresponding assets:
	 * <ul>
	 * <li>Flag/illustration drawable (e.g., {@code R.drawable.img_english_lang_bg})</li>
	 * <li>Background color (e.g., {@code R.color.lang_english_bg_color})</li>
	 * </ul>
	 *
	 * @see LanguageItem
	 * @see #languages
	 * @see MutableLiveData#setValue(Object)
	 */
	private void loadLanguages() {
		List<LanguageItem> list = new ArrayList<>();
		list.add(new LanguageItem(
			"English", "en", R.drawable.img_english_lang_bg,
			R.color.lang_english_bg_color));
		
		list.add(new LanguageItem(
			"हिन्दी", "hi", R.drawable.img_hindi_lang_bg,
			R.color.lang_hindi_bg_color));
		
		list.add(new LanguageItem(
			"தமிழ்", "ta", R.drawable.img_tamil_lang_bg,
			R.color.lang_tamil_bg_color));
		
		list.add(new LanguageItem(
			"తెలుగు", "te", R.drawable.img_telugu_lang_bg,
			R.color.lang_telugu_bg_color));
		
		list.add(new LanguageItem(
			"ਪੰਜਾਬੀ", "pa", R.drawable.img_punjabi_lang_bg,
			R.color.lang_punjabi_bg_color));
		
		list.add(new LanguageItem(
			"मराठी", "mr",
			R.drawable.img_marathi_lang_bg,
			R.color.lang_marathi_bg_color));
		
		list.add(new LanguageItem(
			"ગુજરાતી", "gu", R.drawable.img_gujarati_lang_bg,
			R.color.lang_gujarati_bg_color));
		
		list.add(new LanguageItem(
			"മലയാളം", "ml", R.drawable.img_malayalam_lang_bg,
			R.color.lang_malayalam_bg_color));
		
		list.add(new LanguageItem(
			"বাংলা", "bn", R.drawable.img_bengali_lang_bg,
			R.color.lang_bengali_bg_color));
		
		list.add(new LanguageItem("ଓଡ଼ିଆ", "or",
			R.drawable.img_odia_lang_bg,
			R.color.lang_odia_bg_color));
		
		list.add(new LanguageItem("ಕನ್ನಡ", "kn",
			R.drawable.img_kannada_lang_bg,
			R.color.lang_kannada_bg_color));
		
		list.add(new LanguageItem("অসামীয়া", "as",
			R.drawable.img_assamese_lang_bg,
			R.color.lang_assamese_bg_color));
		
		list.add(new LanguageItem("भोजपुरी", "bho",
			R.drawable.img_bhojpuri_lang_bg,
			R.color.lang_bhojpuri_bg_color));
		
		list.add(new LanguageItem("हरियाणवी", "hry",
			R.drawable.img_haryanvi_lang_bg,
			R.color.lang_haryanvi_bg_color));
		
		list.add(new LanguageItem("राजस्थानी", "raj",
			R.drawable.img_rajasthani_lang_bg,
			R.color.lang_rajasthani_bg_color));
		
		languages.setValue(list);
	}
}