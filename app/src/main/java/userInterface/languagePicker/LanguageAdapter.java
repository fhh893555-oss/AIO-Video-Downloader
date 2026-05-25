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
 * A customized RecyclerView Adapter responsible for managing, data-binding, and displaying
 * a collection of language options within a language selection interface.
 * <p>
 * This adapter coordinates with {@link LanguageViewHolder} to build structural rows dynamically
 * and dispatches click interactions back to host components via a registered {@link LanguageCallback}.
 * It features clean data instantiation routines that optimize performance for localized configurations.
 * </p>
 *
 * <p><b>Key Responsibilities:</b>
 * <ul>
 *   <li>Manages the list of language items to be displayed in the RecyclerView</li>
 *   <li>Inflates the language item layout and creates ViewHolder instances</li>
 *   <li>Binds language data to views at specific positions</li>
 *   <li>Dispatches language selection events to the registered callback</li>
 *   <li>Handles efficient view recycling and reuse during scrolling</li>
 * </ul>
 * </p>
 *
 * <p><b>Data Binding:</b>
 * The adapter receives a list of {@link LanguageItem} objects, each containing
 * language name, code, and flag illustration resource ID. When a user clicks
 * on a language item, the adapter calls {@link LanguageCallback#onLanguageSelected(LanguageItem)}
 * to notify the parent component of the selection.
 * </p>
 *
 * @see RecyclerView.Adapter
 * @see LanguageViewHolder
 * @see LanguageCallback
 * @see LanguageItem
 */
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {
	
	private final List<LanguageItem> languageList = new ArrayList<>();
	private final LanguageCallback callback;
	
	/**
	 * Constructs a new LanguageAdapter with the specified callback listener.
	 * <p>
	 * This constructor initializes the adapter with a LanguageCallback that will be
	 * invoked when a language item is selected by the user. The callback is passed
	 * to each ViewHolder during binding to handle click events at the item level.
	 * </p>
	 *
	 * @param callback The LanguageCallback interface for notifying language selection events.
	 *                 This callback is typically implemented by the parent Activity or Fragment.
	 */
	public LanguageAdapter(LanguageCallback callback) {
		this.callback = callback;
	}
	
	/**
	 * Updates the underlying data set with a new list of languages and refreshes the view.
	 * <p>
	 * This method safely clears any existing entries, adds the structural elements of the
	 * incoming collection, and forces a complete structural re-draw of the visible list rows.
	 * </p>
	 * <p>
	 * <b>Note on Performance:</b> The {@code @SuppressLint("NotifyDataSetChanged")} annotation
	 * is used here because the entire dataset is replaced concurrently. For small dataset lists
	 * like language configurations,this full re-draw operation has a negligible execution cost.
	 * </p>
	 *
	 * @param languages The fresh list of {@link LanguageItem} models to be displayed, or
	 *                  {@code null} to safely clear the entire selection view.
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
	 * Creates and inflates a new ViewHolder for language items in the RecyclerView.
	 * <p>
	 * This method inflates the language item layout resource ({@code activity_language_1_it_1})
	 * and creates a new LanguageViewHolder instance that holds references to the
	 * TextView and ImageView for that item. The inflated view is not attached to
	 * the parent immediately; attachment happens later by the RecyclerView.
	 * </p>
	 *
	 * @param parent   The ViewGroup into which the new view will be added
	 * @param viewType The view type of the new view (not used in this adapter)
	 * @return A new LanguageViewHolder instance containing the inflated item view
	 */
	@NonNull
	@Override
	public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
		View view = layoutInflater.inflate(R.layout.activity_language_1_it_1, parent, false);
		return new LanguageViewHolder(view);
	}
	
	/**
	 * Binds language item data to the ViewHolder at the specified position.
	 * <p>
	 * This method retrieves the LanguageItem from the list at the given position
	 * and binds it to the ViewHolder by calling its {@link LanguageViewHolder#bind(LanguageItem,
	 * LanguageCallback)}
	 * method. The callback interface is passed through to handle click events when
	 * a language item is selected by the user.
	 * </p>
	 *
	 * @param holder   The LanguageViewHolder to update with item data
	 * @param position The position of the item within the adapter's data set
	 */
	@Override
	public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
		LanguageItem item = languageList.get(position);
		holder.bind(item, callback);
	}
	
	/**
	 * Returns the total number of language items in the data set.
	 * <p>
	 * This method is called by the RecyclerView to determine how many items
	 * are available to display in the language selection grid. The count
	 * corresponds to the size of the languageList containing all available
	 * languages.
	 * </p>
	 *
	 * @return The number of language items in the list
	 */
	@Override
	public int getItemCount() {
		return languageList.size();
	}
	
	/**
	 * ViewHolder class for caching and managing language item views in the RecyclerView.
	 * <p>
	 * This static ViewHolder holds references to the UI components of a single language
	 * item in the language selection grid. It efficiently reuses views during scrolling
	 * by avoiding repeated findViewById calls. The ViewHolder also handles click events,
	 * delegating language selection callbacks to the parent activity or fragment.
	 * </p>
	 *
	 * <p><b>UI Components:</b>
	 * <ul>
	 *   <li>tvLangName - TextView displaying the language name (e.g., "English", "Spanish")</li>
	 *   <li>ivFlag - ImageView showing the country or language flag illustration</li>
	 * </ul>
	 * </p>
	 *
	 * <p><b>Binding Process:</b>
	 * The {@link #bind(LanguageItem, LanguageCallback)} method populates the views with
	 * language data and sets up click handling. When clicked, the selected language
	 * is passed back through the LanguageCallback interface.
	 * </p>
	 *
	 * @see RecyclerView.ViewHolder
	 * @see LanguageAdapter
	 * @see LanguageItem
	 * @see LanguageCallback
	 */
	public static class LanguageViewHolder extends RecyclerView.ViewHolder {
		
		private final TextView tvLangName;
		private final ImageView ivFlag;
		
		/**
		 * Constructs a new LanguageViewHolder to hold and manage language item views.
		 * <p>
		 * This constructor initializes the ViewHolder by finding and storing references
		 * to the language name TextView and flag ImageView from the provided item view.
		 * These references are reused when binding data to avoid expensive findViewById calls.
		 * </p>
		 *
		 * @param itemView The root view of the language item layout
		 */
		public LanguageViewHolder(@NonNull View itemView) {
			super(itemView);
			tvLangName = itemView.findViewById(R.id.tvLangName);
			ivFlag = itemView.findViewById(R.id.ivFlag);
		}
		
		/**
		 * Binds language item data to the ViewHolder's views and sets up click handling.
		 * <p>
		 * This method populates the TextView with the language name, sets the flag icon
		 * using the provided resource ID, and configures a click listener on the item view.
		 * When the item is clicked, the callback's {@link LanguageCallback#onLanguageSelected(LanguageItem)}
		 * method is invoked with the selected language item, allowing the parent activity
		 * to handle language selection events.
		 * </p>
		 *
		 * @param languageItem     The language item containing name, code, and flag illustration
		 * @param languageCallback The callback interface for notifying language selection events
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