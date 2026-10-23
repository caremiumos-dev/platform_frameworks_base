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

import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * Dispatches listener queries to the management application, synchronously or
 * asynchronously, with a timeout.
 *
 * @hide
 */
class ListenerTriggerHelper {
    public static final String TAG = "ListenerTriggerHelper";
    private static final int DEFAULT_RESULT = 0;
    private static final long TIMEOUT_MS = 35000L;

    private static ListenerTriggerHelper mInstance = null;
    private static Handler mHandler = null;

    ListenerTriggerHelper() {
    }

    public static ListenerTriggerHelper getInstance() {
        if (mInstance == null) {
            mInstance = new ListenerTriggerHelper();
        }
        return mInstance;
    }

    public static void setHandler(Handler handler) {
        mHandler = handler;
    }

    public static int triggerListener(int controllerID, IBinder listener, Bundle params,
            int defaultResult) {
        if (listener != null) {
            return triggerListenerInner(ListenerTriggerFactory.createTrigger(controllerID,
                    Binder.getCallingPid(), listener, params, defaultResult), false);
        }
        Log.e(TAG, "Trigger null listener!");
        return DEFAULT_RESULT;
    }

    public static void triggerListenerAsync(int controllerID, IBinder listener, Bundle params,
            int defaultResult, IBinder callback) {
        ListenerTriggerBase trigger = null;
        if (callback == null) {
            Log.e(TAG, "Triger null callback");
        }
        if (listener != null) {
            trigger = ListenerTriggerFactory.createAsyncTrigger(controllerID, Binder.getCallingPid(),
                    listener, params, defaultResult, callback);
        } else {
            Log.e(TAG, "Trigger null listener function!");
        }
        triggerListenerInner(trigger, true);
    }

    private static int triggerListenerInner(ListenerTriggerBase trigger, boolean isAsync) {
        int result = DEFAULT_RESULT;
        if (mHandler == null) {
            Log.e(TAG, "Handler should be set before triggerListener() isAsync: " + isAsync);
            return DEFAULT_RESULT;
        }
        Log.d(TAG, "triggerListenerInner(), Start monitoring listener");
        Message message = mHandler.obtainMessage(1);
        message.obj = trigger;
        mHandler.sendMessageDelayed(message, TIMEOUT_MS);
        Log.d(TAG, "triggerListenerInner(), Start a trigger thread and run");
        ListenerTriggerThread thread = new ListenerTriggerThread(trigger, mHandler, message);
        if (isAsync) {
            thread.start();
            Log.d(TAG, "triggerListenerInner() Async End.");
        } else {
            synchronized (trigger) {
                thread.start();
                try {
                    trigger.wait();
                    result = trigger.getResult();
                    Log.d(TAG, "triggerListenerInner() Sync End. result: " + result);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Trigger wait failed!", e);
                    result = trigger.getResult();
                    Log.d(TAG, "triggerListenerInner() Sync End. result: " + result);
                }
            }
        }
        return result;
    }
}