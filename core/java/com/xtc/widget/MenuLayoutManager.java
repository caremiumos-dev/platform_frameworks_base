package com.xtc.widget;

import android.content.Context;
import android.util.Log;

import com.android.internal.widget.LinearLayoutManager;
import com.android.internal.widget.LinearSmoothScroller;
import com.android.internal.widget.RecyclerView;

/**
 * XTC camera-menu vertical layout manager that centers the selected item.
 *
 * @hide
 */
public class MenuLayoutManager extends LinearLayoutManager {
    private static final String TAG = "MenuHolder";

    public static class MenuScroller extends LinearSmoothScroller {
        MenuScroller(Context context) {
            super(context);
        }

        @Override
        public int calculateDtToFit(int viewStart, int viewEnd, int boxStart, int boxEnd, int snapPreference) {
            return (((boxEnd - boxStart) / 2) + boxStart) - (((viewEnd - viewStart) / 2) + viewStart);
        }
    }

    public MenuLayoutManager(Context context) {
        super(context, VERTICAL, false);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        try {
            super.onLayoutChildren(recycler, state);
        } catch (Exception e) {
            Log.d(TAG, "layout err", e);
        }
    }

    public void scrollToCenter(int position, RecyclerView recyclerView) {
        MenuScroller scroller = new MenuScroller(recyclerView.getContext());
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
            int position) {
        MenuScroller scroller = new MenuScroller(recyclerView.getContext());
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }
}
