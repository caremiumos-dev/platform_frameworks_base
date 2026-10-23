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

import android.os.Bundle;
import android.os.IBinder;

/**
 * Base class for a synchronous/asynchronous query sent to the management
 * application ("MoMA"). The system server blocks (or continues) until the
 * application answers or the trigger times out.
 *
 * @hide
 */
public abstract class ListenerTriggerBase {
    private int mControllerID = 0;
    private int mCallingPid = 0;
    private IBinder mFunc = null;
    private Bundle mParams = null;
    private int mReturnValue = 0;
    private boolean mCompleted = false;
    private boolean mIsTimeout = false;
    private IBinder mCallback = null;

    public ListenerTriggerBase(int controllerID, int callingPid, IBinder func, Bundle params,
            int returnValue) {
        initListenerTriggerBase(controllerID, callingPid, func, params, returnValue);
        mCallback = null;
    }

    public ListenerTriggerBase(int controllerID, int callingPid, IBinder func, Bundle params,
            int returnValue, IBinder callback) {
        initListenerTriggerBase(controllerID, callingPid, func, params, returnValue);
        mCallback = callback;
    }

    private void initListenerTriggerBase(int controllerID, int callingPid, IBinder func,
            Bundle params, int returnValue) {
        mControllerID = controllerID;
        mCallingPid = callingPid;
        mFunc = func;
        mParams = params;
        mReturnValue = returnValue;
        mCompleted = false;
        mIsTimeout = false;
    }

    public int getControllerID() {
        return mControllerID;
    }

    public int getCallingPid() {
        return mCallingPid;
    }

    public IBinder getFunc() {
        return mFunc;
    }

    public Bundle getParams() {
        return mParams;
    }

    public int getResult() {
        return mReturnValue;
    }

    public void setResult(int result) {
        mReturnValue = result;
    }

    public IBinder getCallback() {
        return mCallback;
    }

    public boolean isCompleted() {
        return mCompleted;
    }

    public void completed() {
        mCompleted = true;
    }

    public boolean isTimeouted() {
        return mIsTimeout;
    }

    public void timeouted() {
        mIsTimeout = true;
    }

    public abstract int triggerListenerFunc();

    public abstract void onTriggerListenerDone();

    public abstract void onTriggerListenerTimeout();

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        return (obj instanceof ListenerTriggerBase)
                && mControllerID == ((ListenerTriggerBase) obj).getControllerID();
    }

    @Override
    public int hashCode() {
        int result = ((mControllerID + 299) * 13) + mCallingPid;
        if (mFunc != null) {
            result = (result * 13) + mFunc.hashCode();
        }
        if (mParams != null) {
            result = (result * 13) + mParams.hashCode();
        }
        result = (((mCompleted ? 13 : 0) + (((result * 13) + mReturnValue) * 13)) * 13)
                + (mIsTimeout ? 13 : 0);
        return mCallback != null ? (result * 13) + mParams.hashCode() : result;
    }

    @Override
    public String toString() {
        return "ListenerTriggerBase {" + mControllerID + ", " + mCallingPid + ", " + mReturnValue
                + ", " + mReturnValue + ", " + mCompleted + ", " + mIsTimeout + " }";
    }
}