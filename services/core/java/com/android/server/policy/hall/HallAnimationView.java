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

package com.android.server.policy.hall;

import android.animation.Animator;
import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileInputStream;

/**
 * Base view for the hall open/close animation. Concrete subclasses render a
 * lottie animation or a video.
 *
 * @hide
 */
public abstract class HallAnimationView extends FrameLayout {
    public static final int SLOW_THRESHOLD = 10;

    private static final String TAG = "HallAnimationView";

    String animPath;
    private boolean isShow;
    private boolean removeWhenEnd;

    public interface OnDrawListener {
        void onFirstDraw();
    }

    public HallAnimationView(Context context) {
        super(context);
        this.isShow = false;
    }

    public HallAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.isShow = false;
    }

    public HallAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.isShow = false;
    }

    public abstract void cancelAnimation();

    public abstract void disableAnimator(boolean disable);

    public abstract long getDuration();

    public abstract boolean isAnimatorRunning();

    public boolean isShow() {
        return this.isShow;
    }

    public abstract void loadAnimation(String path);

    public abstract void playAnimation();

    protected String readFile(File file) {
        try {
            long start = SystemClock.uptimeMillis();
            FileInputStream inputStream = new FileInputStream(file);
            StringBuilder builder = new StringBuilder();
            byte[] buffer = new byte[256];
            while (true) {
                int read = inputStream.read(buffer);
                if (read == -1) {
                    break;
                }
                builder.append(new String(buffer, 0, read));
            }
            inputStream.close();
            long cost = SystemClock.uptimeMillis() - start;
            if (cost > SLOW_THRESHOLD) {
                Log.w(TAG, "read file cost:" + cost + " name:" + file.getPath());
            }
            return builder.toString();
        } catch (Exception e) {
            Log.w(TAG, "error read file", e);
            return "";
        }
    }

    public boolean removeWhenEnd() {
        return this.removeWhenEnd;
    }

    public abstract void requestHideAnimation();

    public abstract void setAnimation();

    public abstract void setAnimatorListener(Animator.AnimatorListener listener);

    public abstract void setOnDrawListener(OnDrawListener listener);

    public void setRemoveWhenEnd(boolean removeWhenEnd) {
        this.removeWhenEnd = removeWhenEnd;
    }

    public void setShow(boolean show) {
        this.isShow = show;
    }

    public abstract void setTitle(String title);
}