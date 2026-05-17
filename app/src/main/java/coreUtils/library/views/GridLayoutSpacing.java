package coreUtils.library.views;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GridLayoutSpacing extends RecyclerView.ItemDecoration {
    private final int spacingPx;
    private final boolean includeEdge;

    public GridLayoutSpacing(int spacingPx) {
        this(spacingPx, true);
    }

    public GridLayoutSpacing(int spacingPx, boolean includeEdge) {
        this.spacingPx = spacingPx;
        this.includeEdge = includeEdge;
    }

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