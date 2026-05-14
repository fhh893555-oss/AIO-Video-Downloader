package userInterface.fragments.homepage.musicSection;

import static coreUtils.library.views.TextViewsUtils.normalizeTallSymbols;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.nextgen.R;

import java.util.ArrayList;
import java.util.List;

import coreUtils.base.BaseApplication;
import coreUtils.library.process.ThreadTask;
import coreUtils.library.process.TimeFormats;
import coreUtils.library.process.ViewsCounter;
import sysModules.newPipeLib.cache.YtStreamInfo;
import sysModules.vidRecEngine.api.RecommendationEngine;
import sysModules.vidRecEngine.models.enums.VideoCategory;

public final class YtStreamInfoAdapter extends RecyclerView.Adapter<YtStreamInfoAdapter.ViewHolder> {
	
	private final List<YtStreamInfo> ytStreamInfos = new ArrayList<>();
	private OnItemClickListener onItemClickListener;
	
	public interface OnItemClickListener {
		void onItemClick(YtStreamInfo ytStreamInfo);
	}
	
	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}
	
	@SuppressLint("NotifyDataSetChanged")
	public void setYtStreamInfos(List<YtStreamInfo> newItems) {
		ThreadTask.executeOnMainThread(()->{
			ytStreamInfos.clear();
			if (newItems != null) {
				ytStreamInfos.addAll(newItems);
			}
			notifyDataSetChanged();
		});
	}
	
	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext())
			.inflate(R.layout.frag_homepage_1_vd_l1, parent, false);
		return new ViewHolder(view);
	}
	
	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		YtStreamInfo item = ytStreamInfos.get(position);
		holder.bind(item, onItemClickListener);
	}
	
	@Override
	public int getItemCount() {
		return ytStreamInfos.size();
	}
	
	public static final class ViewHolder extends RecyclerView.ViewHolder {
		private final ImageView thumbnail;
		private final TextView duration, name, uploader, count;
		
		public ViewHolder(@NonNull View itemView) {
			super(itemView);
			thumbnail = itemView.findViewById(R.id.homepage_video_item_thumbnail_icon);
			duration = itemView.findViewById(R.id.homepage_video_item_duration_label);
			name = itemView.findViewById(R.id.homepage_video_item_title_text);
			uploader = itemView.findViewById(R.id.homepage_video_item_uploader_text);
			count = itemView.findViewById(R.id.homepage_video_item_count_text);
		}
		
		public void bind(YtStreamInfo item, OnItemClickListener listener) {
			name.setText(item.name);
			normalizeTallSymbols(name, name.getText().toString(), 0.8f, name::setText);
			duration.setText(TimeFormats.formatDurationFromSecond(item.duration));
			uploader.setText(item.uploaderName);
			count.setText(ViewsCounter.formatViewsCount(item.viewCounts));
			
			Glide.with(itemView.getContext())
				.load(item.thumbnailUrl)
				.placeholder(R.drawable.img_empty_vid_preview)
				.transition(DrawableTransitionOptions.withCrossFade())
				.transform(new CenterCrop(), new GranularRoundedCorners(0, 0, 0, 0))
				.override(1600, 900)
				.into(thumbnail);
			
			reportImpression(item);
			itemView.setOnClickListener(v -> {
				if (listener != null) {
					listener.onItemClick(item);
				}
			});
		}
		
		/**
		 * Reports a recommendation impression for the given stream item to the recommendation engine.
		 * <p>
		 * This method notifies the system that a specific music stream has been displayed to the user,
		 * allowing the recommendation algorithm to track view impressions for the {@link VideoCategory#MUSIC} category.
		 * </p>
		 *
		 * @param item The stream information object containing the ID of the item being viewed.
		 */
		private void reportImpression(YtStreamInfo item) {
			RecommendationEngine recommendationEngine = BaseApplication.recommendationEngine;
			recommendationEngine.submitRecommendationImpression(item.streamId, VideoCategory.MUSIC);
		}
	}
}
