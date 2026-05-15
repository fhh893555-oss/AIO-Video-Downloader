package userInterface.language;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
		
		private final TextView tvLangName;
		private final ImageView ivFlag;
		
		public LanguageViewHolder(@NonNull View itemView) {
			super(itemView);
			tvLangName = itemView.findViewById(R.id.tvLangName);
			ivFlag = itemView.findViewById(R.id.ivFlag);
		}
		
		public void bind(LanguageItem languageItem, LanguageCallback languageCallback) {
			tvLangName.setText(languageItem.languageName());
			ivFlag.setImageResource(languageItem.illustrationResId());
			itemView.setOnClickListener(v -> {
				if (languageCallback != null) {
					languageCallback.onLanguageSelected(languageItem);
				}
			});
		}
	}
}