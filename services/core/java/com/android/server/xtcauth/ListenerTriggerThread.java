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

package com.android.server.xtcauth;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Runs a {@link ListenerTriggerBase} on a background thread and hands the result
 * back to the waiting system-server thread.
 *
 * @hide
 */
class ListenerTriggerThread extends Thread {
    private static final String TAG = "ListenerTriggerThread";

    private ListenerTriggerBase mTrigger;
    private Handler mHandler;
    private Message mMsg;

    public ListenerTriggerThread(ListenerTriggerBase trigger, Handler handler, Message msg) {
        super("MOMS-Trigger" + trigger.getControllerID());
        mTrigger = trigger;
        mHandler = handler;
        mMsg = msg;
    }

    @Override
    public void run() {
        Log.d(TAG, "Start trigger listener: " + mTrigger.toString());
        int result = mTrigger.triggerListenerFunc();
        synchronized (mTrigger) {
            if (!mTrigger.isTimeouted()) {
                mTrigger.completed();
                mTrigger.setResult(result);
                mHandler.removeMessages(1, mMsg);
                mTrigger.notifyAll();
                mTrigger.onTriggerListenerDone();
            }
        }
    }
}