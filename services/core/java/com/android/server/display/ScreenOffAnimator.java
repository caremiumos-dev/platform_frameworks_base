/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.display;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/**
 * Full-screen black overlay used to animate the screen turning off.
 *
 * @hide
 */
public class ScreenOffAnimator {
    private static final String TAG = ScreenOffAnimator.class.getSimpleName();

    private static final int TYPE_BLACK_OVERLAY = 2006;
    private static final int FORMAT_OPAQUE = 1;
    private static final int FLAG_NOT_FOCUSABLE = 8;

    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mWindowParams;
    private View mCoverView;
    private volatile boolean mIsShow = false;

    private void init(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowParams = new WindowManager.LayoutParams();
        if (mWindowManager.getDefaultDisplay() != null) {
            Point size = new Point();
            mWindowManager.getDefaultDisplay().getRealSize(size);
            mWindowParams.width = size.x;
            mWindowParams.height = size.y;
        } else {
            mWindowParams.width = 320;
            mWindowParams.height = 360;
        }
        mWindowParams.x = 0;
        mWindowParams.y = 0;
        mWindowParams.type = TYPE_BLACK_OVERLAY;
        mWindowParams.format = FORMAT_OPAQUE;
        mWindowParams.flags = FLAG_NOT_FOCUSABLE;
        mCoverView = new View(mContext);
        mCoverView.setBackgroundColor(0xff000000);
        mCoverView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        Log.d(TAG, "init done");
    }

    public void addView() {
        if (mIsShow) {
            return;
        }
        mIsShow = true;
        Log.d(TAG, "add screen off animation view");
        mWindowParams.alpha = 0.0f;
        mWindowManager.addView(mCoverView, mWindowParams);
    }

    public boolean prepare(Context context) {
        if (mContext == null) {
            init(context);
        }
        addView();
        return true;
    }

    public void removeView() {
        if (mIsShow) {
            mIsShow = false;
            Log.d(TAG, "remove screen off animation view");
            mWindowManager.removeView(mCoverView);
        }
    }

    public void setAlpha(float alpha) {
        mWindowParams.alpha = alpha;
        if (mIsShow) {
            mWindowManager.updateViewLayout(mCoverView, mWindowParams);
        }
    }
}