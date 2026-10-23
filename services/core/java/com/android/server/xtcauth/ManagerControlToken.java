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

import com.xtc.auth.IMobileConnectionCallback;

/**
 * Tracks the currently attached management application connection.
 *
 * @hide
 */
class ManagerControlToken {
    public static final String TAG = "ManagerControlToken";

    private int mUid;
    private int mIdentity;
    private IMobileConnectionCallback mCallback;
    private boolean mEnabled;

    public ManagerControlToken(int uid, int identity, IMobileConnectionCallback callback) {
        mUid = uid;
        mIdentity = identity;
        mCallback = callback;
        mEnabled = false;
    }

    public int getUid() {
        return mUid;
    }

    public int getIdentity() {
        return mIdentity;
    }

    public IMobileConnectionCallback getCallback() {
        return mCallback;
    }

    public void setCallback(IMobileConnectionCallback callback) {
        mCallback = callback;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public void setEnable(boolean enable) {
        mEnabled = enable;
    }

    public void updateToken(int uid, int identity, IMobileConnectionCallback callback) {
        mUid = uid;
        mIdentity = identity;
        mCallback = callback;
        mEnabled = false;
    }

    public boolean notifyConnectionEnded() {
        if (mCallback == null) {
            return true;
        }
        try {
            mCallback.onConnectionEnded();
            return true;
        } catch (android.os.RemoteException e) {
            return false;
        }
    }

    public boolean notifyConnectionResume() {
        if (mCallback == null) {
            return true;
        }
        try {
            mCallback.onConnectionResume();
            return true;
        } catch (android.os.RemoteException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "ManagerControlToken {" + mUid + ", " + mIdentity + ", " + mEnabled + ", "
                + mCallback + "}";
    }
}