package userInterface.language;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;

public record LanguageItem(String languageName,
                           String languageCode,
                           @DrawableRes int illustrationResId,
                           @ColorRes int backgroundColorResId) {
    
    @Override public int illustrationResId() {
        return illustrationResId;
    }
    
    @Override public int backgroundColorResId() {
        return backgroundColorResId;
    }
}