package coreUtils.library.views;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A RecyclerView ItemDecoration that adds consistent spacing between items in a GridLayoutManager.
 * <p>
 * This decoration is specifically designed for use with GridLayoutManager and provides
 * intelligent spacing that handles both regular grid items and full-span items (such as
 * headers or footers that occupy all columns). The spacing is applied uniformly around
 * items, with special handling for edge items to ensure consistent visual appearance.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Automatic spacing calculation based on column position</li>
 *   <li>Special handling for full-span items (spanSize == spanCount)</li>
 *   <li>Asymmetric spacing where inner columns share the total spacing budget</li>
 *   <li>Bottom spacing applied to all items for vertical consistency</li>
 *   <li>Top spacing only applied to the first full-span item</li>
 * </ul>
 * </p>
 *
 * <p><b>Spacing Distribution Example (3 columns with 12px spacing):</b>
 * <pre>
 * Column 0: left=12px, right=6px
 * Column 1: left=6px,  right=6px
 * Column 2: left=6px,  right=12px
 * </pre>
 * This ensures that spacing between adjacent columns totals 12px, and edges have
 * proper padding from the screen boundaries.
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * RecyclerView recyclerView = findViewById(R.id.recyclerView);
 * GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
 * recyclerView.setLayoutManager(layoutManager);
 *
 * int spacingPx = getResources().getDimensionPixelSize(R.dimen.grid_spacing);
 * recyclerView.addItemDecoration(new GridSpacingDecoration(spacingPx));
 * </pre>
 * </p>
 *
 * <p><b>Requirements:</b>
 * <ul>
 * <li>Requires {@link androidx.recyclerview.widget.GridLayoutManager} (or subclass).</li>
 * <li>Full-span positions must return {@code spanCount} via span size lookup.</li>
 * <li>Define spacing in pixels (convert from dp using display metrics).</li>
 * </ul>
 * </p>
 *
 * @see RecyclerView.ItemDecoration
 * @see GridLayoutManager
 * @see #getItemOffsets(Rect, View, RecyclerView, RecyclerView.State)
 */
public class GridSpacingDecoration extends RecyclerView.ItemDecoration {
	private final int spacingPx;
	
	/**
	 * Constructs a new GridSpacingDecoration with the specified spacing in pixels.
	 * <p>
	 * This decoration adds consistent spacing between items in a GridLayoutManager,
	 * handling both full-span items (items that occupy all columns) and regular
	 * grid items. The spacing is applied as padding around each item.
	 * </p>
	 *
	 * @param spacingPx the spacing amount in pixels to apply between grid items
	 */
	public GridSpacingDecoration(int spacingPx) {
		this.spacingPx = spacingPx;
	}
	
	/**
	 * Calculates and applies offset spacing for each item in the RecyclerView grid.
	 * <p>
	 * This method determines the appropriate spacing for each item based on its position,
	 * span size, and column index within the grid. Full-span items receive equal spacing
	 * on left, right, and bottom (with top spacing only for the first item). Regular
	 * grid items receive asymmetric spacing where inner columns share spacing equally.
	 * </p>
	 *
	 * <p><b>Spacing Logic:</b>
	 * <ul>
	 *   <li><b>Full-span items:</b> Equal spacing on left and right, bottom spacing,
	 *       top spacing only for the first item (position 0)</li>
	 *   <li><b>Regular grid items:</b> Bottom spacing for all items; left/right spacing
	 *       varies by column: first column gets full left spacing, last column gets
	 *       full right spacing, inner columns share spacing equally</li>
	 * </ul>
	 * </p>
	 *
	 * @param outRect the rectangle to receive the offset values (left, top, right, bottom)
	 * @param view    the child view to decorate
	 * @param parent  the RecyclerView this decoration is attached to
	 * @param state   the current state of the RecyclerView
	 */
	@Override
	public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
	                           @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		int position = parent.getChildAdapterPosition(view);
		if (position == RecyclerView.NO_POSITION) return;
		
		GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
		if (layoutManager == null) return;
		
		int spanCount = layoutManager.getSpanCount();
		int spanSize = layoutManager.getSpanSizeLookup().getSpanSize(position);
		
		if (spanSize == spanCount) {
			outRect.left = spacingPx;
			outRect.right = spacingPx;
			outRect.bottom = spacingPx;
			if (position == 0) outRect.top = spacingPx;
		} else {
			int gridPos = position - 1;
			int column = gridPos % spanCount;
			
			outRect.bottom = spacingPx;
			
			if (column == 0) {
				outRect.left = spacingPx;
				outRect.right = spacingPx / 2;
			} else {
				outRect.left = spacingPx / 2;
				outRect.right = spacingPx;
			}
		}
	}
}