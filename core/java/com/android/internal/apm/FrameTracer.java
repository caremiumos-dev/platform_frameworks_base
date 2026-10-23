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

package com.android.internal.apm;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Choreographer;
import android.view.WindowManagerGlobal;

/**
 * Tracks dropped frames and the frame rate observed by the choreographer and
 * reports them to the UI monitor.
 *
 * @hide
 */
public class FrameTracer {
    private static final String TAG = "FrameTracer";

    private final UIMonitor mUIMonitor;
    private final Object mLock = new Object();
    private final FrameTracerCallback mFrameTracerCallback = new FrameTracerCallback() {
        @Override
        public void doFrame(int droppedFrameCount, int fps) {
            long currentTimeMillis = System.currentTimeMillis();
            synchronized (mLock) {
                if (droppedFrameCount > 0) {
                    String focusedWindowInfo =
                            WindowManagerGlobal.getInstance().getFocusedWindowInfo();
                    if (droppedFrameCount > mDropFrameThreshold) {
                        reportOverThresholdLocked(currentTimeMillis, droppedFrameCount,
                                focusedWindowInfo);
                    }
                    boolean dataChanged = false;
                    if (droppedFrameCount <= mFrameTracerData.mMinDropFrame
                            || mFrameTracerData.mMinDropFrame == -1) {
                        mFrameTracerData.mMinDropFrame = droppedFrameCount;
                        mFrameTracerData.mMinDropFrameScene = focusedWindowInfo;
                        dataChanged = true;
                    } else if (droppedFrameCount >= mFrameTracerData.mMaxDropFrame) {
                        mFrameTracerData.mMaxDropFrame = droppedFrameCount;
                        mFrameTracerData.mMaxDropFrameScene = focusedWindowInfo;
                        dataChanged = true;
                    }
                    mTotalDropFrame += droppedFrameCount;
                    mDropFrameCount++;
                    if (mDropFrameCount > 0
                            && mFrameTracerData.mAvgDropFrame
                                    != mTotalDropFrame / mDropFrameCount) {
                        mFrameTracerData.mAvgDropFrame = mTotalDropFrame / mDropFrameCount;
                        dataChanged = true;
                    }
                    if (fps > 0) {
                        mTotalFPS += fps;
                        mFPSCount++;
                        if (mFPSCount > 0
                                && mFrameTracerData.mAvgFPS != mTotalFPS / mFPSCount) {
                            mFrameTracerData.mAvgFPS = mTotalFPS / mFPSCount;
                            dataChanged = true;
                        }
                    }
                    if (dataChanged) {
                        mUIMonitor.onFrameTracerDataChanged(mFrameTracerData);
                    }
                } else {
                    if (fps > 0) {
                        mTotalFPS += fps;
                        mFPSCount++;
                        if (mFPSCount > 0
                                && mFrameTracerData.mAvgFPS != mTotalFPS / mFPSCount) {
                            mFrameTracerData.mAvgFPS = mTotalFPS / mFPSCount;
                            mUIMonitor.onFrameTracerDataChanged(mFrameTracerData);
                        }
                    }
                }
            }
        }
    };

    private final MainHandler mMainHandler = new MainHandler(Looper.getMainLooper());
    private Choreographer mChoreographer;
    private int mDropFrameThreshold;
    private int mTotalDropFrame;
    private int mDropFrameCount;
    private int mTotalFPS;
    private int mFPSCount;
    private FrameTracerData mFrameTracerData;

    public FrameTracer(UIMonitor uiMonitor, int dropFrameThreshold) {
        mUIMonitor = uiMonitor;
        mDropFrameThreshold = dropFrameThreshold;
        mMainHandler.post(new Runnable() {
            @Override
            public void run() {
                mChoreographer = Choreographer.getInstance();
            }
        });
        mFrameTracerData = new FrameTracerData();
    }

    public void enableFrameMonitor() {
        APMDebugLog.d(TAG, "enableFrameMonitor");
        if (mChoreographer != null) {
            mChoreographer.setFrameTracerCallback(mFrameTracerCallback);
        } else {
            mMainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mChoreographer.setFrameTracerCallback(mFrameTracerCallback);
                }
            }, 500L);
        }
    }

    public void disableFrameMonitor() {
        APMDebugLog.d(TAG, "disableFrameMonitor");
        if (mChoreographer != null) {
            mChoreographer.setFrameTracerCallback(null);
        }
    }

    public void updateDropFrameThreshold(int threshold) {
        synchronized (mLock) {
            mDropFrameThreshold = threshold;
        }
    }

    private void reportOverThresholdLocked(long timestamp, int droppedFrameCount, String scene) {
        APMDebugLog.d(TAG, "reportOverThresholdLocked");
        mUIMonitor.reportOverThresholdData(APMOverThresholdEventType.TYPE_DROP_FRAME, timestamp,
                String.valueOf(mDropFrameThreshold), String.valueOf(droppedFrameCount), scene);
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
        }
    }

    public class FrameTracerData {
        public int mMinDropFrame = -1;
        public int mMaxDropFrame = 0;
        public int mAvgDropFrame;
        public int mAvgFPS;
        public String mMinDropFrameScene;
        public String mMaxDropFrameScene;

        public FrameTracerData() {
        }
    }
}