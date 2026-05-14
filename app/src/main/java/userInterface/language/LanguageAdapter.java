package userInterface.language;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.nextgen.R;

import java.util.ArrayList;
import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {

    private final List<LanguageItem> languageList = new ArrayList<>();
    private final LanguageCallback callback;

    public LanguageAdapter(LanguageCallback callback) {
        this.callback = callback;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setLanguages(List<LanguageItem> languages) {
        this.languageList.clear();
        if (languages != null) {
            this.languageList.addAll(languages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.activity_language_1_it_1, parent, false);
        return new LanguageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
        LanguageItem item = languageList.get(position);
        holder.bind(item, callback);
    }

    @Override
    public int getItemCount() {
        return languageList.size();
    }

    public static class LanguageViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtLanguageName;
        private final ImageView imgLanguageIllustration;
        private final CardView languageContainer;

        public LanguageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtLanguageName = itemView.findViewById(R.id.language_name_text);
            imgLanguageIllustration = itemView.findViewById(R.id.language_flag_image);
            languageContainer = itemView.findViewById(R.id.language_item_card);
        }

        public void bind(LanguageItem languageItem, LanguageCallback languageCallback) {
            txtLanguageName.setText(languageItem.getLanguageName());
            imgLanguageIllustration.setImageResource(languageItem.getIllustrationResId());

            Resources resources = itemView.getContext().getResources();
            int backgroundColorResId = languageItem.getBackgroundColorResId();
            int cardBackgroundColor = resources.getColor(backgroundColorResId, resources.newTheme());
            languageContainer.setCardBackgroundColor(cardBackgroundColor);

            itemView.setOnClickListener(v -> {
                if (languageCallback != null) {
                    languageCallback.onLanguageSelected(languageItem);
                }
            });
        }
    }
}