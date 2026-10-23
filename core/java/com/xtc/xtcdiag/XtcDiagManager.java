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

package com.xtc.xtcdiag;

import android.content.Context;
import android.util.Log;

/**
 * Public manager facade for the XTC diag service.
 *
 * @hide
 */
public class XtcDiagManager {
    private static final String TAG = "XtcDiag-Manager";

    private Context mContext;
    private XtcDiagMgr mDiagMgr;

    public XtcDiagManager(Context context) {
        mContext = context;
        mDiagMgr = (XtcDiagMgr) context.getSystemService(Context.XTC_DIAG_SERVICE);
    }

    public boolean isSocketConnected() {
        if (mDiagMgr != null) {
            return mDiagMgr.isSocketConnected();
        }
        Log.d(TAG, "isSocketConnected mDiagMgr is not create!");
        return false;
    }

    public void registerDiagListener(XtcDiagListener listener) {
        mDiagMgr.registerDiagListener(listener);
    }

    public void unregisterDiagListener(XtcDiagListener listener) {
        mDiagMgr.unregisterDiagListener(listener);
    }

    public void send(int cmd, String receiver, byte[] data) {
        if (mDiagMgr == null) {
            Log.d(TAG, "isSocketConnected mDiagMgr is not create!");
        } else {
            mDiagMgr.send(cmd, receiver, data);
        }
    }

    public void sendFile(String path, String fileName, String receiver, int cmd) {
        if (mDiagMgr == null) {
            Log.d(TAG, "isSocketConnected mDiagMgr is not create!");
        } else {
            mDiagMgr.sendFile(path, fileName, receiver, cmd);
        }
    }
}