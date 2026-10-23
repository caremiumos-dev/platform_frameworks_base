package com.xtc.widget;

import com.android.internal.widget.RecyclerView;
import com.android.internal.widget.helper.ItemTouchHelper;

/**
 * XTC camera-menu drag callback: forwards moves to the listener and drives the
 * tile zoom animation while dragging.
 *
 * @hide
 */
public class DragItemCallback extends ItemTouchHelper.Callback {
    private final ItemDragListener mListener;

    public interface ItemDragListener {
        void onClearView();

        void onMove(int from, int to);
    }

    public DragItemCallback(ItemDragListener listener) {
        mListener = listener;
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        AppLinearLayout item = (AppLinearLayout) viewHolder.itemView;
        item.setDragging(false);
        item.startZoom(AppLinearLayout.TYPE_RESET_ZOOM);
        if (mListener != null) {
            mListener.onClearView();
        }
    }

    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {
        int from = viewHolder.getAdapterPosition();
        int to = target.getAdapterPosition();
        if (mListener == null) {
            return true;
        }
        mListener.onMove(from, to);
        return true;
    }

    @Override
    public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (viewHolder != null) {
            AppLinearLayout item = (AppLinearLayout) viewHolder.itemView;
            if (actionState != ItemTouchHelper.ACTION_STATE_DRAG) {
                item.setDragging(false);
            } else {
                item.setDragging(true);
                item.startZoom(AppLinearLayout.TYPE_ZOOM_IN);
            }
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
    }
}
