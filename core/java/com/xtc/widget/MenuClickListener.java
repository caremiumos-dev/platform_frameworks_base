package com.xtc.widget;

import android.content.Intent;

/**
 * XTC camera-menu click / reorder callbacks.
 *
 * @hide
 */
public interface MenuClickListener {
    void onClick(int position, Intent intent);

    void onOrderChanged();
}
