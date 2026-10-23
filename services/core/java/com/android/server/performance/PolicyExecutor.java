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

package com.android.server.performance;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.android.server.performance.status.CpuStatus;
import com.android.server.performance.status.RamStatus;

import java.io.PrintWriter;

/**
 * Base class for the performance policy executors. Each executor runs on its
 * own thread and reacts to system/activity/cpu/instrumentation changes.
 *
 * @hide
 */
public class PolicyExecutor {
    private static final String LOG_TAG = "PolicyExecutor";

    private static final int MSG_SYSTEMSTATUS_CHANGED = 0;
    private static final int MSG_ACTIVITY_STATE_CHANGED = 1;
    private static final int MSG_CPUSTATUS_CHANGED = 2;
    private static final int MSG_INSTRUMENTATION_CHANGE = 3;

    protected Handler mHandler;
    protected final HandlerThread mThread = new HandlerThread(getThreadName());
    boolean mInProgress = false;

    public PolicyExecutor() {
        this.mThread.start();
        this.mHandler = new Handler(this.mThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                super.handleMessage(message);
                switch (message.what) {
                    case MSG_SYSTEMSTATUS_CHANGED:
                        RamStatus ramStatus = (RamStatus) message.obj;
                        mInProgress = true;
                        executorPolicy(ramStatus);
                        mInProgress = false;
                        break;
                    case MSG_ACTIVITY_STATE_CHANGED:
                        handleActivityStateChange(
                                (PerformanceManagerServiceImpl.ActivityStateData) message.obj);
                        break;
                    case MSG_CPUSTATUS_CHANGED:
                        handleCpuStatusChange((CpuStatus) message.obj);
                        break;
                    case MSG_INSTRUMENTATION_CHANGE:
                        handleInStrumentationChange(message);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    public static PolicyExecutor createFromConfig(PolicyItem item,
            PerformanceManagerServiceImpl service) {
        return null;
    }

    public void dump(PrintWriter writer, String[] args) {
    }

    public void executorPolicy(RamStatus ramStatus) {
    }

    public String getThreadName() {
        return LOG_TAG;
    }

    protected void handleActivityStateChange(
            PerformanceManagerServiceImpl.ActivityStateData data) {
    }

    protected void handleCpuStatusChange(CpuStatus status) {
    }

    protected void handleInStrumentationChange(Message message) {
    }

    public void onActivityStateChanged(PerformanceManagerServiceImpl.ActivityStateData data) {
        Message message = new Message();
        message.what = MSG_ACTIVITY_STATE_CHANGED;
        message.obj = data;
        this.mHandler.sendMessage(message);
    }

    public void onCpusStatusChanged(CpuStatus status) {
        Message message = new Message();
        message.what = MSG_CPUSTATUS_CHANGED;
        message.obj = status;
        this.mHandler.sendMessage(message);
    }

    public void onInStrumentationChange(
            PerformanceManagerServiceImpl.InStrumentationData data) {
        Message message = new Message();
        message.what = MSG_INSTRUMENTATION_CHANGE;
        message.obj = data;
        this.mHandler.sendMessage(message);
    }

    public void onRamStatusChanged(RamStatus ramStatus) {
        if (this.mInProgress) {
            return;
        }
        Message message = new Message();
        message.what = MSG_SYSTEMSTATUS_CHANGED;
        message.obj = ramStatus;
        this.mHandler.sendMessage(message);
    }
}