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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * Base listener for XTC diag events. Clients subclass this and override
 * {@link #onConnectStateChange} and {@link #onCmdReceive}; callbacks are
 * delivered on the {@link Looper} used to construct the listener.
 *
 * @hide
 */
public abstract class XtcDiagListener {
    private static final String KEY_CMD_CODE = "cmd_code";
    private static final String KEY_CONN_STATE = "conn_state";
    private static final String KEY_JSON_DATA = "json_data";
    private static final String KEY_RAW_DATA = "raw_data";
    private static final String KEY_REMOTE_IP = "remote_ip";

    /** Listener type for connection state changes. */
    public static final int LISTEN_CONNECT_STATE = 1;
    /** Listener type for command reception. */
    public static final int LISTEN_CMD_RECEIVE = 2;
    /** Socket is connected. */
    public static final int STATE_CONNECTED = 1;
    /** Socket is disconnected. */
    public static final int STATE_DISCONNECT = 2;

    final XtcDiagConnectCbStub mConnStateCb;
    final XtcDiagCmdReceiveCbStub mCmdReceiveCb;
    private final Handler mHandler;

    public XtcDiagListener() {
        this(Looper.myLooper());
    }

    public XtcDiagListener(Looper looper) {
        mConnStateCb = new XtcDiagConnectCbStub(this);
        mCmdReceiveCb = new XtcDiagCmdReceiveCbStub(this);
        mHandler = new Handler(looper) {
            @Override
            public void handleMessage(Message message) {
                Bundle data = message.getData();
                switch (message.what) {
                    case LISTEN_CONNECT_STATE:
                        onConnectStateChange(
                                data.getInt(KEY_CONN_STATE), data.getString(KEY_REMOTE_IP));
                        break;
                    case LISTEN_CMD_RECEIVE:
                        onCmdReceive(data.getInt(KEY_CMD_CODE), data.getString(KEY_JSON_DATA),
                                data.getByteArray(KEY_RAW_DATA));
                        break;
                }
            }
        };
    }

    public abstract void onCmdReceive(int cmd, String jsonData, byte[] rawData);

    public abstract void onConnectStateChange(int state, String remoteIp);

    private static class XtcDiagConnectCbStub extends IXtcDiagConnStateCallback.Stub {
        private final WeakReference<XtcDiagListener> mXtcDiagCbWeakRef;

        XtcDiagConnectCbStub(XtcDiagListener listener) {
            mXtcDiagCbWeakRef = new WeakReference<>(listener);
        }

        @Override
        public void onConnectStateChange(int state, String remoteIp) {
            XtcDiagListener listener = mXtcDiagCbWeakRef.get();
            if (listener != null) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                message.what = LISTEN_CONNECT_STATE;
                bundle.putInt(KEY_CONN_STATE, state);
                bundle.putString(KEY_REMOTE_IP, remoteIp);
                message.setData(bundle);
                listener.mHandler.sendMessage(message);
            }
        }
    }

    private static class XtcDiagCmdReceiveCbStub extends IXtcDiagCmdReceiveCallback.Stub {
        private final WeakReference<XtcDiagListener> mXtcDiagCbWeakRef;

        XtcDiagCmdReceiveCbStub(XtcDiagListener listener) {
            mXtcDiagCbWeakRef = new WeakReference<>(listener);
        }

        @Override
        public void onCmdReceive(int cmd, String jsonData, byte[] rawData) {
            XtcDiagListener listener = mXtcDiagCbWeakRef.get();
            if (listener != null) {
                Message message = new Message();
                Bundle bundle = new Bundle();
                message.what = LISTEN_CMD_RECEIVE;
                bundle.putInt(KEY_CMD_CODE, cmd);
                bundle.putString(KEY_JSON_DATA, jsonData);
                bundle.putByteArray(KEY_RAW_DATA, rawData);
                message.setData(bundle);
                listener.mHandler.sendMessage(message);
            }
        }
    }
}