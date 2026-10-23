package com.xtc.widget;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.widget.RecyclerView;
import com.android.internal.widget.helper.ItemTouchHelper;

import java.util.Collections;
import java.util.List;

/**
 * XTC camera-menu reorderable list.
 *
 * @hide
 */
public class SortRecyclerView extends RecyclerView {
    private DragItemCallback.ItemDragListener mDragListener;
    private MenuLayoutManager mLayoutManager;
    private ItemTouchHelper mTouchHelper;

    static class MyViewHolder extends RecyclerView.ViewHolder {
        StrokeLayout menuContainer;
        ImageView menuIcon;
        TextView menuTitle;

        MyViewHolder(View itemView) {
            super(itemView);
            menuIcon = (ImageView) itemView.findViewById(com.android.internal.R.id.menu_icon);
            menuTitle = (TextView) itemView.findViewById(com.android.internal.R.id.menu_title);
            menuContainer = (StrokeLayout) itemView.findViewById(
                    com.android.internal.R.id.menu_container);
        }
    }

    public static class SortAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private final List<MenuItem> mData;
        private final ColorMatrixColorFilter mGrayFilter;
        private final ItemTouchHelper mHelper;
        private MenuClickListener mListener;

        public SortAdapter(List<MenuItem> data, ItemTouchHelper helper) {
            mData = data;
            mHelper = helper;
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0.0f);
            mGrayFilter = new ColorMatrixColorFilter(colorMatrix);
        }

        private void setViewStatus(MenuItem item, MyViewHolder holder, Drawable drawable) {
            if (item.selected) {
                holder.menuContainer.enableStroke();
            } else {
                holder.menuContainer.disableStroke();
            }
            if (item.funEnable) {
                drawable.setColorFilter(null);
            } else {
                drawable.setColorFilter(mGrayFilter);
            }
        }

        public List<MenuItem> getData() {
            return mData;
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            final MenuItem item = mData.get(position);
            Drawable icon = item.getIcon();
            holder.menuIcon.setImageDrawable(icon);
            holder.menuTitle.setText(item.menuTitle);
            setViewStatus(item, holder, icon);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mListener != null) {
                        int position = holder.getAdapterPosition();
                        mListener.onClick(position, mData.get(position).getIntent());
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (mHelper == null) {
                        return true;
                    }
                    mHelper.startDrag(holder);
                    return true;
                }
            });
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(com.android.internal.R.layout.camera_menu_item_layout, parent, false));
        }

        public void setClickListener(MenuClickListener listener) {
            mListener = listener;
        }
    }

    public SortRecyclerView(Context context) {
        super(new ContextThemeWrapper(context, com.android.internal.R.style.ScrollbarRecyclerView));
        mDragListener = new DragItemCallback.ItemDragListener() {
            private boolean mChanged;

            @Override
            public void onClearView() {
                if (mChanged && getAdapter().mListener != null) {
                    getAdapter().mListener.onOrderChanged();
                }
                mChanged = false;
            }

            @Override
            public void onMove(int from, int to) {
                if (from < to) {
                    for (int i = from; i < to; i++) {
                        Collections.swap(getAdapter().getData(), i, i + 1);
                        MenuHolder.instance().swapOrder(
                                getAdapter().getData().get(i), getAdapter().getData().get(i + 1));
                    }
                } else {
                    for (int i = from; i > to; i--) {
                        Collections.swap(getAdapter().getData(), i, i - 1);
                        MenuHolder.instance().swapOrder(
                                getAdapter().getData().get(i), getAdapter().getData().get(i - 1));
                    }
                }
                getAdapter().notifyItemMoved(from, to);
                mChanged = true;
            }
        };
        initViewParameters();
        setLayoutManager();
    }

    private void initViewParameters() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = android.view.Gravity.CENTER;
        setLayoutParams(lp);
        addItemDecoration(new MenuItemDecoration(
                getContext().getResources().getDimensionPixelSize(
                        com.android.internal.R.dimen.menuItemPaddingVer) * 2));
        setBackgroundResource(com.android.internal.R.drawable.ic_selfie_menu_bg);
        setVerticalScrollBarEnabled(true);
    }

    private void setLayoutManager() {
        mLayoutManager = new MenuLayoutManager(getContext());
        setLayoutManager(mLayoutManager);
        setHasFixedSize(true);
        mTouchHelper = new ItemTouchHelper(new DragItemCallback(mDragListener));
        mTouchHelper.attachToRecyclerView(this);
    }

    @Override
    public SortAdapter getAdapter() {
        return (SortAdapter) super.getAdapter();
    }

    public void scrollToCenter(int position) {
        mLayoutManager.scrollToCenter(position, this);
    }

    public void setAdapter(List<MenuItem> list) {
        super.setAdapter(new SortAdapter(list, mTouchHelper));
    }
}
