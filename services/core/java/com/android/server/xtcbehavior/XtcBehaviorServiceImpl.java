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

package com.android.server.xtcbehavior;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;

import com.xtc.behavior.IXtcBehavior;
import com.xtc.behavior.XtcBehaviorResolver;
import com.xtc.behavior.XtcBehaviorUtil;
import com.xtc.moduleswitch.ModuleSwitch;

/**
 * Binder implementation of {@code xtcbehavior}. Receives behavior records from
 * apps, applies the module switch, and forwards them to the vendor launcher
 * provider.
 *
 * <p><b>Condemnation.</b> This is the sink of the OEM behavior-tracking
 * pipeline; see {@link XtcBehaviorResolver} and {@link XtcBehaviorInit}. It
 * collects application usage data without meaningful user consent.
 *
 * @hide
 */
public class XtcBehaviorServiceImpl extends IXtcBehavior.Stub {
    private static final boolean DEBUG = false;
    private static final String TAG = "XtcBehaviorServiceImpl";

    private static final int BEHAVIOR_MODULE_ID = 2201;
    private static final int MSG_LISTEN_NATIVE = 1;
    private static final int MSG_INSERT_DATA_DB = 2;
    private static final int MSG_LOAD_INIT = 3;
    private static final int MSG_QUERY_MODULE_SWITCH = 4;

    private static final long LOAD_INIT_DELAY = 30000L;
    private static final String PROP_KERNEL_BEHAVIOR_SWITCH = "log.tag.msghub";
    private static final String ACTION_MODULE_SWITCH_INIT =
            "com.xtc.launcher.moduleswitch.INIT_ACTION";

    private final Context mContext;
    private final BehaviorHandler mHandler;
    private XtcBehaviorResolver mResolver;
    private final XtcBehaviorUtil mXtcBehaviorUtil;
    private boolean mLastBehaviorModuleState = true;
    private boolean mModuleSwitch = true;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            logd("Receieved: " + action);
            if (ACTION_MODULE_SWITCH_INIT.equals(action)) {
                mHandler.removeMessages(MSG_QUERY_MODULE_SWITCH);
                mHandler.obtainMessage(MSG_QUERY_MODULE_SWITCH).sendToTarget();
            }
        }
    };

    private final HandlerThread mHandlerThread = new HandlerThread("XtcBehaviorService thread");

    private class BehaviorHandler extends Handler {
        BehaviorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            logd("BehaviorHandler msg.what: " + message.what);
            switch (message.what) {
                case MSG_LISTEN_NATIVE:
                    logd("listen_recv_msg handler thread: "
                            + Thread.currentThread().getName() + " ,threadId: "
                            + Thread.currentThread().getId());
                    if (mXtcBehaviorUtil != null) {
                        mXtcBehaviorUtil.listen_recv_msg();
                    }
                    break;
                case MSG_INSERT_DATA_DB:
                    logd("send data handler thread: " + Thread.currentThread().getName()
                            + " ,threadId: " + Thread.currentThread().getId());
                    logd("MSG_INSERT_DATA_DB !mModuleSwitch: " + !mModuleSwitch);
                    if (mModuleSwitch) {
                        if (mResolver == null) {
                            mResolver = new XtcBehaviorResolver();
                        }
                        ContentValues values = (ContentValues) message.obj;
                        if (isSecureData(values)) {
                            Slog.d(TAG, "before insert data is secure. functionName:"
                                    + (values == null ? "null"
                                            : values.getAsString("functionName")));
                        } else {
                            Slog.d(TAG, "before insert data handler context: " + mContext
                                    + " , data:" + values);
                        }
                        mResolver.insert(mContext, values);
                        logd("send data handler context: " + mContext + " , data:" + values);
                    }
                    break;
                case MSG_LOAD_INIT:
                    doLoadInit();
                    break;
                case MSG_QUERY_MODULE_SWITCH:
                    doModuleSwitch();
                    break;
                default:
                    break;
            }
        }
    }

    public XtcBehaviorServiceImpl(Context context) {
        this.mContext = context;
        this.mHandlerThread.start();
        this.mHandler = new BehaviorHandler(this.mHandlerThread.getLooper());
        this.mXtcBehaviorUtil = new XtcBehaviorUtil();
        this.mResolver = new XtcBehaviorResolver();
        logd("XtcBehaviorServiceImpl context: " + context + " , main thread: "
                + Thread.currentThread().getName() + " , main threadId: "
                + Thread.currentThread().getId());
    }

    private void doLoadInit() {
        logd("load behavior init msg.");
        XtcBehaviorInit init = new XtcBehaviorInit();
        init.sendShutdownOrAppCrashBehavior(this.mContext);
        init.sendEmmcEraseCountBehavior(this.mContext);
        init.sendAdbStateBehavior(this.mContext);
        init.sendHardwareIssueBehavior(this.mContext);
    }

    private void doModuleSwitch() {
        this.mModuleSwitch = ModuleSwitch.queryModuleSwitch(this.mContext, BEHAVIOR_MODULE_ID,
                true);
        logd("doModuleSwitch moduleSwitch: " + this.mModuleSwitch
                + " , mLastBehaviorModuleState: " + this.mLastBehaviorModuleState);
        if (this.mLastBehaviorModuleState == this.mModuleSwitch) {
            return;
        }
        this.mLastBehaviorModuleState = this.mModuleSwitch;
        if (this.mModuleSwitch) {
            SystemProperties.set(PROP_KERNEL_BEHAVIOR_SWITCH, "0");
            return;
        }
        SystemProperties.set(PROP_KERNEL_BEHAVIOR_SWITCH, "1");
        this.mHandler.removeMessages(MSG_LOAD_INIT);
        Slog.d(TAG, "2201 behavior module switch close!");
    }

    private boolean isSecureData(ContentValues values) {
        if (values == null) {
            return false;
        }
        String functionName = values.getAsString("functionName");
        if (TextUtils.isEmpty(functionName)) {
            return false;
        }
        return "call_mt_function".equals(functionName)
                || "call_mo_function".equals(functionName)
                || "call_fail_ld".equals(functionName);
    }

    private void logd(String message) {
    }

    public void listenNativeMsg() {
        if (this.mHandler != null) {
            this.mHandler.sendEmptyMessage(MSG_LISTEN_NATIVE);
        } else {
            Slog.w(TAG, "mHandler is null, need check.");
        }
    }

    public void loadInit() {
        if (this.mHandler != null) {
            this.mHandler.sendEmptyMessageDelayed(MSG_LOAD_INIT, LOAD_INIT_DELAY);
        } else {
            Slog.w(TAG, "mHandler is null, need check.");
        }
    }

    public void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_MODULE_SWITCH_INIT);
        this.mContext.registerReceiverAsUser(this.mBroadcastReceiver, UserHandle.ALL, filter,
                null, null);
    }

    @Override
    public void sendData(ContentValues values) {
        if (values == null || this.mContext == null || this.mHandler == null) {
            Slog.w(TAG, "send data to launcher but values: " + values + " , context: "
                    + this.mContext + " , mHandler: " + this.mHandler);
            return;
        }
        Message message = this.mHandler.obtainMessage(MSG_INSERT_DATA_DB);
        message.obj = values;
        message.sendToTarget();
        logd("send data thread: " + Thread.currentThread().getName() + " ,threadId: "
                + Thread.currentThread().getId());
    }
}