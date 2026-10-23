package com.xtc.widget;

import android.graphics.Rect;
import android.view.View;

import com.android.internal.widget.RecyclerView;

/**
 * XTC camera-menu item spacing decoration.
 *
 * @hide
 */
public class MenuItemDecoration extends RecyclerView.ItemDecoration {
    private int mPadding;

    public MenuItemDecoration(int padding) {
        mPadding = padding;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        outRect.top = mPadding;
        if (parent.getChildLayoutPosition(view) == parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = mPadding;
        }
    }
}
