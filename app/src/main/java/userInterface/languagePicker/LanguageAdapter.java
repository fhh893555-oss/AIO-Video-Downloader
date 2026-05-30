package userInterface.languagePicker;

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

/**
 * RecyclerView adapter for displaying a grid of language options in the
 * language selection screen. This adapter manages a list of {@link LanguageItem}
 * objects and binds them to {@link LanguageViewHolder} instances. It delegates
 * click events to a {@link LanguageCallback} to notify the parent activity
 * when a language is selected.
 *
 * <p><strong>Core responsibilities:</strong>
 * <ul>
 * <li>Manages the list of languages to display in the RecyclerView grid.</li>
 * <li>Inflates the layout for each language item via {@link #onCreateViewHolder}.</li>
 * <li>Binds language data to views via {@link #onBindViewHolder}.</li>
 * <li>Provides a method {@link #setLanguages(List)} to update the data set.</li>
 * <li>Forwards item click events to the provided {@link LanguageCallback}.</li>
 * </ul>
 *
 * <p><strong>Layout:</strong>
 * Each language item uses the layout file {@code R.layout.activity_language_1_it_1},
 * which is expected to contain a TextView for the language name and an ImageView
 * for a flag or illustration.
 *
 * <p>The adapter is designed to work with a grid layout manager showing
 * 3 columns per row, as configured in the parent activity.
 *
 * @see RecyclerView.Adapter
 * @see LanguageViewHolder
 * @see LanguageItem
 * @see LanguageCallback
 */
public final class LanguageAdapter extends
	RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {
	
	private final List<LanguageItem> languageList = new ArrayList<>();
	private final LanguageCallback callback;
	
	/**
	 * Constructs a new LanguageAdapter with the specified callback listener.
	 * The adapter holds a reference to the callback to notify the parent
	 * activity or fragment when a language item is selected by the user.
	 *
	 * <p>The adapter initially starts with an empty language list. Data must
	 * be populated via {@link #setLanguages(List)} before the RecyclerView
	 * can display any items.
	 *
	 * @param callback The {@link LanguageCallback} to invoke when a language
	 *                 item is clicked. Must not be {@code null} for selection
	 *                 events to be delivered, though the adapter does not
	 *                 enforce non-null at construction.
	 * @see #setLanguages(List)
	 * @see LanguageCallback#onLanguageSelected(LanguageItem)
	 */
	public LanguageAdapter(LanguageCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * Updates the adapter's language data set and refreshes the RecyclerView
	 * display. This method clears the existing list, adds all items from the
	 * provided collection (if non-null), and notifies the RecyclerView that
	 * the entire data set has changed.
	 *
	 * <p><strong>Performance note:</strong>
	 * The method uses {@code @SuppressLint("NotifyDataSetChanged")} to suppress
	 * the lint warning about using {@link #notifyDataSetChanged()} instead of
	 * more granular notifications (e.g., {@code notifyItemRangeInserted()}) because
	 * the entire data set is replaced at once. For small language lists (typically
	 * under 20 items), this approach is acceptable and simpler to maintain.
	 *
	 * @param languages The new list of {@link LanguageItem} objects to display.
	 *                  May be {@code null}, in which case the adapter clears
	 *                  all existing items and shows an empty list.
	 * @see #notifyDataSetChanged()
	 * @see #languageList
	 */
	@SuppressLint("NotifyDataSetChanged")
	public void setLanguages(List<LanguageItem> languages) {
		this.languageList.clear();
		if (languages != null) {
			this.languageList.addAll(languages);
		}
		notifyDataSetChanged();
	}
	
	/**
	 * Creates and returns a new ViewHolder instance for a language item.
	 * This method inflates the layout resource {@code R.layout.activity_language_1_it_1}
	 * (a single language grid cell) and wraps it in a new {@link LanguageViewHolder}.
	 *
	 * <p>The layout is not attached to the parent immediately; the parent is used
	 * only to provide proper layout parameters. This follows the standard RecyclerView
	 * adapter pattern for efficient view recycling.
	 *
	 * @param parent   The ViewGroup into which the new view will be added after
	 *                 binding to an adapter position. Must not be {@code null}.
	 * @param viewType The view type of the new view (not used in this implementation,
	 *                 as all language items share the same layout).
	 * @return A new {@link LanguageViewHolder} instance containing the inflated view.
	 * @see #onBindViewHolder(LanguageViewHolder, int)
	 * @see LayoutInflater#inflate(int, ViewGroup, boolean)
	 */
	@NonNull
	@Override
	public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
		View view = layoutInflater.inflate(R.layout.activity_language_1_it_1, parent, false);
		return new LanguageViewHolder(view);
	}
	
	/**
	 * Binds language data to the ViewHolder at the specified position.
	 * This method retrieves the {@link LanguageItem} from the internal list
	 * using the given position and calls
	 * {@link LanguageViewHolder#bind(LanguageItem, LanguageCallback)}
	 * to populate the view with the language name, flag/illustration, and
	 * attach the click listener that notifies the callback when selected.
	 *
	 * @param holder   The ViewHolder that should be updated to represent the
	 *                 contents of the item at the given position. Must not be
	 *                 {@code null}.
	 * @param position The position of the item within the adapter's data set.
	 * @see #onCreateViewHolder(ViewGroup, int)
	 * @see LanguageViewHolder#bind(LanguageItem, LanguageCallback)
	 */
	@Override
	public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
		LanguageItem item = languageList.get(position);
		holder.bind(item, callback);
	}
	
	/**
	 * Returns the total number of language items available in the adapter.
	 * This method is called by the RecyclerView to determine the size of
	 * the data set and to know when it has reached the end of the list.
	 *
	 * @return The size of the {@code languageList} collection. Returns {@code 0}
	 * if the list is {@code null} or empty.
	 * @see #setLanguages(List)
	 * @see #languageList
	 */
	@Override
	public int getItemCount() {
		return languageList.size();
	}
	
	/**
	 * ViewHolder class for displaying a single language item within the RecyclerView.
	 * This inner static class holds references to the UI components of a language
	 * grid cell, including the language name TextView and the flag/illustration
	 * ImageView. It provides a {@link #bind(LanguageItem, LanguageCallback)} method
	 * to populate the views with data and handle click events.
	 *
	 * <p><strong>Layout expectations:</strong>
	 * The associated item layout must contain:
	 * <ul>
	 * <li>A TextView with ID {@code R.id.tvLangName} for displaying the language name.</li>
	 * <li>An ImageView with ID {@code R.id.ivFlag} for showing a flag or illustration.</li>
	 * </ul>
	 *
	 * <p>The class is declared as {@code static} to prevent memory leaks by avoiding
	 * an implicit reference to the outer {@link LanguageAdapter} instance.
	 *
	 * @see RecyclerView.ViewHolder
	 * @see LanguageAdapter
	 * @see LanguageItem
	 * @see LanguageCallback
	 */
	public final static class LanguageViewHolder extends RecyclerView.ViewHolder {
		
		private final TextView tvLangName;
		private final ImageView ivFlag;
		
		/**
		 * Constructs a new ViewHolder for a language item in the RecyclerView.
		 * This constructor initializes the UI component references by finding
		 * the views within the provided item view layout. The layout is expected
		 * to contain a TextView with ID {@code tvLangName} and an ImageView with
		 * ID {@code ivFlag}.
		 *
		 * @param itemView The root view of the language item layout. Must not be
		 *                 {@code null} and should contain the expected child views.
		 * @see #bind(LanguageItem, LanguageCallback)
		 */
		public LanguageViewHolder(@NonNull View itemView) {
			super(itemView);
			tvLangName = itemView.findViewById(R.id.tvLangName);
			ivFlag = itemView.findViewById(R.id.ivFlag);
		}
		
		/**
		 * Binds language data to the ViewHolder's UI components and sets up the
		 * click listener to notify the callback when the item is selected.
		 * This method populates the TextView with the language name, sets the
		 * flag/illustration image from the resource ID, and attaches a click
		 * listener to the entire item view.
		 *
		 * <p><strong>Click behavior:</strong>
		 * When the user taps on a language item, the
		 * {@link LanguageCallback#onLanguageSelected(LanguageItem)}
		 * method is invoked with the selected {@link LanguageItem} object. If the
		 * provided callback is {@code null}, the click listener still attaches but
		 * silently does nothing when clicked.
		 *
		 * @param languageItem     The language data containing name, code, and illustration
		 *                         resource ID to display. Must not be {@code null}.
		 * @param languageCallback The callback interface to notify when the language
		 *                         item is clicked. May be {@code null}, in which case
		 *                         clicks are ignored.
		 * @see LanguageItem#languageName()
		 * @see LanguageItem#illustrationResId()
		 * @see LanguageCallback#onLanguageSelected(LanguageItem)
		 */
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