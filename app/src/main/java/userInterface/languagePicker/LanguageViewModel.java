package userInterface.languagePicker;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.nextgen.R;

import java.util.ArrayList;
import java.util.List;

import coreUtils.library.process.LoggerUtils;

public class LanguageViewModel extends ViewModel {

    private final LoggerUtils logger = LoggerUtils.from(getClass());
    private final MutableLiveData<List<LanguageItem>> languages = new MutableLiveData<>();

    public LanguageViewModel() {
        loadLanguages();
    }

    public LiveData<List<LanguageItem>> getLanguages() {
        return languages;
    }

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