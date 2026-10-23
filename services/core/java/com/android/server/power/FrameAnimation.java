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

package com.android.server.power;

import android.os.Handler;
import android.widget.ImageView;

/**
 * Frame-by-frame image animation used by the shutdown / restore dialogs.
 *
 * @hide
 */
public class FrameAnimation {
    public interface AnimationListener {
        void onAnimationStart();

        void onAnimationRepeat();

        void onAnimationEnd();
    }

    private ImageView mImageView;
    private int[] mFrameRess;
    private int[] mDurations;
    private int mDuration;
    private int mDelay;
    private int mLastFrame;
    private boolean mIsRepeat;
    private boolean mNext;
    private AnimationListener mAnimationListener;
    private final Handler mHandler = new Handler();

    public FrameAnimation(ImageView imageView, int[] frameRess, int duration, int delay) {
        mImageView = imageView;
        mFrameRess = frameRess;
        mDuration = duration;
        mDelay = delay;
        mLastFrame = frameRess.length - 1;
        playAndDelay(0);
    }

    public FrameAnimation(ImageView imageView, int[] frameRess, int duration, boolean repeat) {
        mImageView = imageView;
        mFrameRess = frameRess;
        mDuration = duration;
        mLastFrame = frameRess.length - 1;
        mIsRepeat = repeat;
        play(0);
    }

    public FrameAnimation(ImageView imageView, int[] frameRess, int[] durations, int delay) {
        mImageView = imageView;
        mFrameRess = frameRess;
        mDurations = durations;
        mDelay = delay;
        mLastFrame = frameRess.length - 1;
        playByDurationsAndDelay(0);
    }

    public FrameAnimation(ImageView imageView, int[] frameRess, int[] durations, boolean repeat) {
        mImageView = imageView;
        mFrameRess = frameRess;
        mDurations = durations;
        mLastFrame = frameRess.length - 1;
        mIsRepeat = repeat;
        playByDurations(0);
    }

    public void setAnimationListener(AnimationListener listener) {
        mAnimationListener = listener;
    }

    private void play(final int frame) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (frame == 0 && mAnimationListener != null) {
                    mAnimationListener.onAnimationStart();
                }
                mImageView.setBackgroundResource(mFrameRess[frame]);
                if (frame != mLastFrame) {
                    play(frame + 1);
                } else if (mIsRepeat) {
                    if (mAnimationListener != null) {
                        mAnimationListener.onAnimationRepeat();
                    }
                    play(0);
                } else if (mAnimationListener != null) {
                    mAnimationListener.onAnimationEnd();
                }
            }
        }, mDuration);
    }

    private void playAndDelay(final int frame) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mNext = false;
                if (frame == 0 && mAnimationListener != null) {
                    mAnimationListener.onAnimationStart();
                }
                mImageView.setBackgroundResource(mFrameRess[frame]);
                if (frame != mLastFrame) {
                    playAndDelay(frame + 1);
                    return;
                }
                if (mAnimationListener != null) {
                    mAnimationListener.onAnimationRepeat();
                }
                mNext = true;
                playAndDelay(0);
            }
        }, (!mNext || mDelay <= 0) ? mDuration : mDelay);
    }

    private void playByDurations(final int frame) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (frame == 0 && mAnimationListener != null) {
                    mAnimationListener.onAnimationStart();
                }
                mImageView.setBackgroundResource(mFrameRess[frame]);
                if (frame != mLastFrame) {
                    playByDurations(frame + 1);
                } else if (mIsRepeat) {
                    if (mAnimationListener != null) {
                        mAnimationListener.onAnimationRepeat();
                    }
                    playByDurations(0);
                } else if (mAnimationListener != null) {
                    mAnimationListener.onAnimationEnd();
                }
            }
        }, mDurations[frame]);
    }

    private void playByDurationsAndDelay(final int frame) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (frame == 0 && mAnimationListener != null) {
                    mAnimationListener.onAnimationStart();
                }
                mImageView.setBackgroundResource(mFrameRess[frame]);
                if (frame != mLastFrame) {
                    playByDurationsAndDelay(frame + 1);
                    return;
                }
                if (mAnimationListener != null) {
                    mAnimationListener.onAnimationRepeat();
                }
                mNext = true;
                playByDurationsAndDelay(0);
            }
        }, (!mNext || mDelay <= 0) ? mDurations[frame] : mDelay);
    }
}