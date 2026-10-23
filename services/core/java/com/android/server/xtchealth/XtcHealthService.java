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

package com.android.server.xtchealth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.util.Slog;

import com.xtc.health.IXtcHealthCallback;
import com.xtc.health.IXtcHealthManager;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.Date;

/**
 * System service bridging the baseband "BBD" health monitor and the vendor
 * self-test application.
 *
 * @hide
 */
public class XtcHealthService extends IXtcHealthManager.Stub {
    private static final String TAG = "XtcHealthService";

    private static final String ACTION_MODEM_CAT_SWITCH = "com.xtc.selftest.modem_cat_switch";
    private static final String CONTEXT_PACKAGE = "com.xtc.selftest";
    private static final String PERMISSION_PREFERENCE = "switch";

    private static final int MODULE_UTI = 3;
    private static final int MODULE_FPD = 4;
    private static final int MODULE_UTI_CRASH_APP = 4;
    private static final int MODULE_FPD_APP = 5;
    private static final int MSG_PARSE_MODULE_UTI = 1;

    public static final byte UTI_MODULE = 6;
    public static final byte UTI_DST = 18;
    public static final byte UTI_TYPE_REBOOT = 21;

    private static boolean debug = false;
    private static int mPpgReboot = 0;

    private final Context mContext;
    private final XtcHealthThreadHandler mHandler;
    private final HandlerThread mHandlerThread;
    private final XtcHealthService mHost;
    private String[] mArgs;
    private int mNextArg;

    private final RemoteCallbackList<IXtcHealthCallback> mCallbackList = new RemoteCallbackList<>();

    private final BroadcastReceiver prefReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                setBootStatus();
            }
        }
    };

    private class XtcHealthThreadHandler extends Handler {
        XtcHealthThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PARSE_MODULE_UTI:
                    byte[] data = (byte[]) msg.obj;
                    try {
                        if (data[1] == UTI_DST && data[2] == UTI_MODULE
                                && data[8] == UTI_TYPE_REBOOT) {
                            mPpgReboot++;
                            SystemProperties.set("persist.sys.ppg_bootTime",
                                    String.valueOf(mPpgReboot));
                            Slog.d(TAG, "ppg boot time=" + mPpgReboot);
                            sendDataToApp(MODULE_FPD, data);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendDataToApp(MODULE_UTI, data);
                    break;
                default:
                    Slog.e(TAG, "Parameters of the abnormal");
                    break;
            }
        }
    }

    public XtcHealthService(Context context) {
        Slog.i(TAG, "init XtcHealthService");
        mHandlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        mHandler = new XtcHealthThreadHandler(mHandlerThread.getLooper());
        mHost = this;
        mContext = context;
    }

    private static native boolean init_bbd(XtcHealthService host);

    private static native int sendDataMcu(String packageName, int length, byte[] data);

    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }

    private void sendBbdDataNative(int type, byte[] data) {
        Slog.d(TAG, "notifyCallback type:" + type + " msg:" + data);
        if (type == MODULE_UTI) {
            Message message = Message.obtain();
            message.obj = data;
            message.what = MSG_PARSE_MODULE_UTI;
            mHandler.sendMessage(message);
            return;
        }
        if (type == MODULE_FPD) {
            sendDataToApp(MODULE_FPD_APP, data);
        } else {
            sendDataToApp(type, data);
        }
    }

    private void sendDataToApp(int type, byte[] data) {
        int count = mCallbackList.getRegisteredCallbackCount();
        Slog.d(TAG, "registered callback count = " + count);
        if (count > 0) {
            int broadcast = mCallbackList.beginBroadcast();
            for (int i = 0; i < broadcast; i++) {
                IXtcHealthCallback callback = mCallbackList.getBroadcastItem(i);
                if (callback != null) {
                    try {
                        callback.onPacketData(type, data);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            mCallbackList.finishBroadcast();
        }
    }

    private void setBootStatus() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Slog.d(TAG, "send ACTION_MODEM_CAT_SWITCH broadcast");
                Intent intent = new Intent();
                intent.setAction(ACTION_MODEM_CAT_SWITCH);
                intent.putExtra(PERMISSION_PREFERENCE, 0);
                mContext.sendBroadcast(intent);
                try {
                    Context packageContext = mContext.createPackageContext(CONTEXT_PACKAGE,
                            Context.CONTEXT_IGNORE_SECURITY);
                    if (packageContext == null) {
                        Slog.e(TAG, "can not create safe context");
                        return;
                    }
                    SharedPreferences preferences =
                            packageContext.getSharedPreferences(PERMISSION_PREFERENCE,
                                    Context.MODE_MULTI_PROCESS);
                    if (preferences != null) {
                        Slog.d(TAG, "clear selftest sp");
                        preferences.edit().clear().commit();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Slog.e(TAG, "sp open failed!");
                }
            }
        }).start();
    }

    public void dump(java.io.FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump time : " + DateFormat.getDateTimeInstance().format(new Date()));
        pw.println("XtcHealthService Dump Info");
        pw.println("========================================");
        for (String arg : args) {
            mArgs = args;
            mNextArg = 1;
            if (arg == null || arg.length() <= 0 || arg.charAt(0) != '-') {
                return;
            }
            if ("-d".equals(arg) && nextArg().equals("debug")) {
                String value = nextArg();
                if (value.equals("on")) {
                    debug = true;
                } else if (value.equals("off")) {
                    debug = false;
                }
            }
        }
    }

    public void registerHealthCallback(IXtcHealthCallback callback) {
        Slog.d(TAG, "registerHealthCallback pid=" + Binder.getCallingPid() + ", uid="
                + Binder.getCallingUid() + ", result=" + mCallbackList.register(callback));
    }

    public void unregisterHealthCallback(IXtcHealthCallback callback) {
        Slog.d(TAG, "unregisterHealthCallback pid=" + Binder.getCallingPid() + ", uid="
                + Binder.getCallingUid() + ", result=" + mCallbackList.unregister(callback));
    }

    public int sendDataMcu(String packageName, byte[] data) {
        Slog.d(TAG, "send data packageName:" + packageName + ", data:" + data);
        return sendDataMcu(packageName, data.length, data);
    }

    public void systemReady() {
        Slog.i(TAG, "systemReady");
        init_bbd(this);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        mContext.registerReceiver(prefReceiver, filter);
        SystemProperties.set("persist.sys.ppg_bootTime", "0");
        mPpgReboot = 0;
    }
}