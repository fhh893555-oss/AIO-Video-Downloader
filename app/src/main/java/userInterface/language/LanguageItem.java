package userInterface.language;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public class LanguageItem {
    private final String languageName;
    private final String languageCode;
    @DrawableRes private final int illustrationResId;
    @ColorRes private final int backgroundColorResId;

    public LanguageItem(String languageName, String languageCode,
                        @DrawableRes int illustrationResId,
                        @ColorRes int backgroundColorResId) {
        this.languageName = languageName;
        this.languageCode = languageCode;
        this.illustrationResId = illustrationResId;
        this.backgroundColorResId = backgroundColorResId;
    }

    public String getLanguageName() {
        return languageName;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public int getIllustrationResId() {
        return illustrationResId;
    }

    public int getBackgroundColorResId() {
        return backgroundColorResId;
    }
}