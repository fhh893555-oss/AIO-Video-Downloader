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
 * * @see RecyclerView.Adapter
 *
 * @see LanguageViewHolder
 * @see LanguageCallback
 */
public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder> {
	
	private final List<LanguageItem> languageList = new ArrayList<>();
	private final LanguageCallback callback;
	
	/**
	 * Constructs a new LanguageAdapter instance with a specialized event callback.
	 *
	 * @param callback The listener interface used to handle language click selection events
	 *                 and communicate them back to the host Activity or Fragment.
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
	 * Called when RecyclerView needs a new {@link LanguageViewHolder} of the given type to represent
	 * an item. This new ViewHolder is constructed with a new View that can represent the items
	 * of the given type.
	 *
	 * @param parent   The ViewGroup into which the new View will be added after it is bound to
	 *                 an adapter position.
	 * @param viewType The view type of the new View.
	 * @return A new LanguageViewHolder that holds a View for a language item.
	 */
	@NonNull
	@Override
	public LanguageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
		View view = layoutInflater.inflate(R.layout.activity_language_1_it_1, parent, false);
		return new LanguageViewHolder(view);
	}
	
	/**
	 * Binds the language data at the specified position to the provided ViewHolder.
	 * <p>
	 * This method fetches the appropriate {@link LanguageItem} from the data list based
	 * on its position and invokes the holder's internal binding logic to update the
	 * UI components and register the click interaction listeners.
	 * </p>
	 *
	 * @param holder   The ViewHolder instance which should be updated to represent the
	 *                 contents of the item at the given position in the data set.
	 * @param position The absolute row position of the item within the adapter's data set.
	 */
	@Override
	public void onBindViewHolder(@NonNull LanguageViewHolder holder, int position) {
		LanguageItem item = languageList.get(position);
		holder.bind(item, callback);
	}
	
	/**
	 * Returns the total number of language items in the data set held by the adapter.
	 * <p>
	 * This count is heavily utilized by the layout manager layout system to calculate
	 * pagination limits, structural row constraints, and scroll boundaries.
	 * </p>
	 *
	 * @return The total size number of available language options inside the backing
	 * list collection.
	 */
	@Override
	public int getItemCount() {
		return languageList.size();
	}
	
	/**
	 * A ViewHolder implementation responsible for managing and displaying individual
	 * language options inside a language selection list.
	 * <p>
	 * This class caches references to the UI widgets of the item view layout to
	 * avoid expensive {@code findViewById()} operations during list scrolling, ensuring
	 * smooth scrolling performance.
	 * </p>
	 */
	public static class LanguageViewHolder extends RecyclerView.ViewHolder {
		
		private final TextView tvLangName;
		private final ImageView ivFlag;
		
		/**
		 * Constructs a new ViewHolder instance and initializes references to
		 * its child views.
		 *
		 * @param itemView The root view of the individual list item layout
		 *                 inflated into the RecyclerView.
		 */
		public LanguageViewHolder(@NonNull View itemView) {
			super(itemView);
			tvLangName = itemView.findViewById(R.id.tvLangName);
			ivFlag = itemView.findViewById(R.id.ivFlag);
		}
		
		/**
		 * Binds language data models to the UI widgets and configures click interaction behaviors.
		 * <p>
		 * This method applies text and image resources to the fields and registers a click
		 * listener on the root {@code itemView}. When clicked, it passes the associated data back
		 * to the registering component via the provided execution callback.
		 * </p>
		 *
		 * @param languageItem     The target data entity containing display fields like
		 *                         names and image references.
		 * @param languageCallback The runtime listener configuration used to dispatch
		 *                         click actions safely.
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