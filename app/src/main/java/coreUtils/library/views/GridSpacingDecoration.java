package coreUtils.library.views;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GridSpacingDecoration extends RecyclerView.ItemDecoration {
    private final int spacingPx;

    public GridSpacingDecoration(int spacingPx) {
        this.spacingPx = spacingPx;
    }

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