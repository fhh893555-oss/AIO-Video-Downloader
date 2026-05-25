package coreUtils.library.views;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Proportional item spacing decoration for {@link androidx.recyclerview.widget.GridLayoutManager}.
 * <p>
 * This decoration distributes spacing proportionally across columns, creating visually
 * balanced gaps between grid items. It supports two modes: with edge spacing (spacing
 * applied to left/right edges of the RecyclerView) or without edge spacing (spacing
 * only between items). The spacing calculation uses integer division to ensure
 * consistent pixel values across different screen widths and column counts.
 * </p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Proportional spacing distribution across columns</li>
 *   <li>Option to include or exclude edge spacing</li>
 *   <li>Automatic top spacing for rows after the first</li>
 *   <li>Works with any span count (number of columns)</li>
 *   <li>Uses integer-based pixel calculations for precise layout</li>
 * </ul>
 * </p>
 *
 * <p><b>Spacing Examples (spanCount = 3, spacingPx = 12):</b>
 * <pre>
 * With edge spacing (includeEdge = true):
 * Column 0: left=12px, right=4px
 * Column 1: left=4px,  right=4px
 * Column 2: left=4px,  right=12px
 *
 * Without edge spacing (includeEdge = false):
 * Column 0: left=0px,  right=4px
 * Column 1: left=4px,  right=4px
 * Column 2: left=8px,  right=4px
 * </pre>
 * </p>
 *
 * <p><b>Usage Example:</b>
 * <pre>
 * RecyclerView recyclerView = findViewById(R.id.recyclerView);
 * GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
 * recyclerView.setLayoutManager(layoutManager);
 *
 * // Convert dp to pixels
 * int spacingPx = (int) TypedValue.applyDimension(
 *     TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
 *
 * // Add decoration with edge spacing
 * recyclerView.addItemDecoration(new GridLayoutSpacing(spacingPx, true));
 * </pre>
 * </p>
 *
 * <p><b>Requirements:</b>
 * <ul>
 *   <li>RecyclerView must use GridLayoutManager (or a subclass)</li>
 *   <li>Spacing should be defined in pixels (convert from dp using display metrics)</li>
 *   <li>For optimal results, spacingPx should be divisible by spanCount</li>
 * </ul>
 * </p>
 *
 * @see RecyclerView.ItemDecoration
 * @see GridLayoutManager
 * @see #getItemOffsets(Rect, View, RecyclerView, RecyclerView.State)
 */
public class GridLayoutSpacing extends RecyclerView.ItemDecoration {
	
	private final int spacingPx;
	private final boolean includeEdge;
	
	/**
	 * Constructs a new GridLayoutSpacing with the specified spacing and edge spacing enabled.
	 * <p>
	 * This convenience constructor calls {@link #GridLayoutSpacing(int, boolean)} with
	 * includeEdge set to true, meaning spacing will be applied to the edges of the grid
	 * (left and right sides of the RecyclerView).
	 * </p>
	 *
	 * @param spacingPx the spacing amount in pixels between grid items
	 */
	public GridLayoutSpacing(int spacingPx) {
		this(spacingPx, true);
	}
	
	/**
	 * Constructs a new GridLayoutSpacing with the specified spacing and edge spacing option.
	 * <p>
	 * This decoration adds consistent spacing between items in a GridLayoutManager,
	 * with the option to include or exclude spacing at the edges (left/right sides).
	 * The spacing is distributed proportionally across columns.
	 * </p>
	 *
	 * @param spacingPx   the spacing amount in pixels between grid items
	 * @param includeEdge true to add spacing at the edges of the grid, false to leave no edge spacing
	 */
	public GridLayoutSpacing(int spacingPx, boolean includeEdge) {
		this.spacingPx = spacingPx;
		this.includeEdge = includeEdge;
	}
	
	/**
	 * Calculates and applies offset spacing for each item in the RecyclerView grid.
	 * <p>
	 * This method distributes spacing proportionally across columns based on the item's
	 * position within the grid. When includeEdge is true, edge items receive outer spacing;
	 * when false, spacing is only between items with no edge padding.
	 * </p>
	 *
	 * <p><b>Spacing Calculation (includeEdge = true):</b>
	 * <pre>
	 * left = spacingPx - (column * spacingPx / spanCount)
	 * right = (column + 1) * spacingPx / spanCount
	 * top = spacingPx (for first row)
	 * bottom = spacingPx (for all items)
	 * </pre>
	 *
	 * <b>Spacing Calculation (includeEdge = false):</b>
	 * <pre>
	 * left = column * spacingPx / spanCount
	 * right = spacingPx - (column + 1) * spacingPx / spanCount
	 * top = spacingPx (for rows after the first)
	 * bottom = 0 (no bottom spacing)
	 * </pre>
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
		int column = position % spanCount;
		
		if (includeEdge) {
			outRect.left = spacingPx - column * spacingPx / spanCount;
			outRect.right = (column + 1) * spacingPx / spanCount;
			
			if (position < spanCount) {
				outRect.top = spacingPx;
			}
			outRect.bottom = spacingPx;
		} else {
			outRect.left = column * spacingPx / spanCount;
			outRect.right = spacingPx - (column + 1) * spacingPx / spanCount;
			if (position >= spanCount) {
				outRect.top = spacingPx;
			}
		}
	}
}