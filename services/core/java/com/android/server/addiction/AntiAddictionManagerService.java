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

package com.android.server.addiction;

import android.app.AntiAddiction.IAddictionCallback;
import android.app.AntiAddiction.IAntiAddictionManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Slog;

import com.xtc.utils.KeyguardEventManager;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks how long applications are displayed / play audio, feeding the vendor
 * "anti-addiction" (screen-time) feature. The collected statistics are uploaded
 * through the behaviour-telemetry channel; this is part of the device's
 * behaviour-monitoring surface.
 *
 * @hide
 */
public class AntiAddictionManagerService extends IAntiAddictionManager.Stub {
    private static final String TAG = "AntiAddictionManager";

    private static final int MSG_STATS_DISPLAY = 0;
    private static final int MSG_STATS_BACKSTAGE = 1;
    private static final int MSG_STATS_MUSICPLAY = 2;

    private static final int ANTI_DISPLAY_START = 1;
    private static final int ANTI_DISPLAY_END = 2;
    private static final int ANTI_MUSIC_START = 3;
    private static final int ANTI_MUSIC_STOP = 4;
    private static final int ANTI_STARTPROCESS_START = 5;
    private static final int ANTI_STARTPROCESS_END = 6;
    private static final int ANTI_PROCESS_EMPTY = 7;
    private static final int ANTI_PROCESS_NO_EMPTY = 8;

    private static final String KEY_PROCESS_NAME = "processName";
    private static final String KEY_PACKAGE_NAME = "packageName";
    private static final String KEY_CLASS_NAME = "className";
    private static final String KEY_TYPE = "type";
    private static final String KEY_UID = "uid";

    private static boolean debug = false;
    private static String mMusicApp;
    private static int mMusicAppUid;
    private static List<String> attachAppList;
    private static List<String> mFilterDisplayList;
    private static List<String> mFilterMusicApp;
    private static ConcurrentHashMap<String, Integer> startBackstageApp;
    private static final List<String> mFilterWhiteList = new ArrayList<>();

    private final Context mContext;
    private final AntiAddictionThreadHandler mHandler;
    private final HandlerThread mHandlerThread;
    private final AntiAddictionManagerService mHost;
    private final KeyguardEventManager mKeyguard;

    private String[] mArgs;
    private int mNextArg;
    private boolean screenOff;
    private String startDisplayPackage;
    private String startDisplayProcess;
    private String startDisplayClassName;

    private final RemoteCallbackList<IAddictionCallback> mCallbackList =
            new RemoteCallbackList<>();

    private final KeyguardEventManager.Listener mListener = new KeyguardEventManager.Listener() {
        @Override
        public void onFinishedGoingToSleep() {
            if (debug) {
                Slog.d(TAG, "screenOff packageName=" + startDisplayPackage);
            }
            if (!screenOff && startDisplayPackage != null
                    && !mFilterDisplayList.contains(startDisplayPackage)) {
                sendMessageData(startDisplayProcess, startDisplayPackage, startDisplayClassName,
                        ANTI_DISPLAY_END, MSG_STATS_DISPLAY);
            }
            screenOff = true;
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        HashMap<String, String> map = new HashMap<>();
                        PackageManager pm = mContext.getPackageManager();
                        Intent mainIntent = new Intent(Intent.ACTION_MAIN, (Uri) null);
                        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                        List<ResolveInfo> activities = pm.queryIntentActivities(mainIntent, 0);
                        if (activities != null) {
                            int size = activities.size();
                            for (int i = 0; i < size; i++) {
                                ResolveInfo info = activities.get(i);
                                if (map.containsKey(
                                        info.activityInfo.applicationInfo.packageName)) {
                                    attachAppList.add(info.activityInfo.name);
                                    for (Map.Entry<String, String> entry : map.entrySet()) {
                                        if (entry.getKey().equals(
                                                info.activityInfo.applicationInfo.packageName)
                                                && !attachAppList.contains(entry.getValue())) {
                                            attachAppList.add(entry.getValue());
                                        }
                                    }
                                }
                                map.put(info.activityInfo.applicationInfo.packageName,
                                        info.activityInfo.name);
                            }
                        }
                    }
                }, 2000L);
            }
        }
    };

    private class AntiAddictionThreadHandler extends Handler {
        AntiAddictionThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_STATS_DISPLAY:
                    sendDataToApp(msg.getData().getString(KEY_PROCESS_NAME),
                            msg.getData().getString(KEY_PACKAGE_NAME),
                            msg.getData().getString(KEY_CLASS_NAME),
                            msg.getData().getInt(KEY_TYPE));
                    break;
                case MSG_STATS_BACKSTAGE: {
                    String processName = msg.getData().getString(KEY_PROCESS_NAME);
                    String packageName = msg.getData().getString(KEY_PACKAGE_NAME);
                    String className = msg.getData().getString(KEY_CLASS_NAME);
                    int type = msg.getData().getInt(KEY_TYPE);
                    sendDataToApp(processName, packageName, className, type);
                    if (type == ANTI_STARTPROCESS_END) {
                        startBackstageApp.remove(processName);
                    }
                    break;
                }
                case MSG_STATS_MUSICPLAY:
                    statsMusicPlayTime(msg.getData().getInt(KEY_TYPE),
                            msg.getData().getInt(KEY_UID));
                    break;
                default:
                    break;
            }
        }
    }

    public enum AppState {
        startProcess,
        Stop,
        Paused,
        Resumed,
        Destroyed,
        Dead,
        shutdown,
        ProcessEmpty,
        ProcessNoEmpty
    }

    static {
        mFilterWhiteList.add("com.xtc.systemupdate_i11");
        mFilterWhiteList.add("com.qti.diagservices");
        mFilterWhiteList.add("android.ext.services");
        mFilterWhiteList.add("com.baidu.input.xtcime");
        mFilterMusicApp = new ArrayList<>();
        mFilterMusicApp.add("com.xtc.camera.app");
        mFilterMusicApp.add("com.xtc.intelligentIdentify");
        attachAppList = new ArrayList<>();
        mFilterDisplayList = new ArrayList<>();
        startBackstageApp = new ConcurrentHashMap<>();
    }

    public AntiAddictionManagerService(Context context) {
        Slog.i(TAG, "init AntiAddictionManagerService");
        mHandlerThread = new HandlerThread("AntiAddictionManagerService",
                android.os.Process.THREAD_PRIORITY_FOREGROUND);
        mHandlerThread.start();
        mHandler = new AntiAddictionThreadHandler(mHandlerThread.getLooper());
        mHost = this;
        mContext = context;
        mKeyguard = new KeyguardEventManager();
        mKeyguard.registerListener(mListener);
        IntentFilter filter = new IntentFilter();
        filter.setPriority(1000);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        context.registerReceiver(mBroadcastReceiver, filter);
    }

    private String getSimpleClassName(String className) {
        return className != null ? className.substring(className.lastIndexOf('.') + 1) : className;
    }

    private boolean isMainBootActivity(String className) {
        boolean result = false;
        if (!className.equals(startDisplayClassName) && attachAppList.contains(className)
                && attachAppList.contains(startDisplayClassName)) {
            result = true;
        }
        if (debug) {
            Slog.d(TAG, "className=" + className + ", startDisplayClassName="
                    + startDisplayClassName + ", result=" + result);
        }
        return result;
    }

    private String nextArg() {
        if (mNextArg >= mArgs.length) {
            return null;
        }
        String arg = mArgs[mNextArg];
        mNextArg++;
        return arg;
    }

    private void sendDataToApp(String processName, String packageName, String className, int type) {
        int count = mCallbackList.getRegisteredCallbackCount();
        Slog.d(TAG, "registered callback count = " + count);
        if (count > 0) {
            int broadcast = mCallbackList.beginBroadcast();
            for (int i = 0; i < broadcast; i++) {
                IAddictionCallback callback = mCallbackList.getBroadcastItem(i);
                if (callback != null) {
                    try {
                        callback.getAddictionDuration(processName, packageName, className, type);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
            mCallbackList.finishBroadcast();
        }
    }

    private void sendMessageData(String processName, String packageName, String className, int type,
            int what) {
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putString(KEY_PROCESS_NAME, processName);
        bundle.putString(KEY_PACKAGE_NAME, packageName);
        bundle.putString(KEY_CLASS_NAME, className);
        bundle.putInt(KEY_TYPE, type);
        message.setData(bundle);
        message.what = what;
        mHandler.sendMessage(message);
    }

    private void shutdown() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (startDisplayPackage != null
                        && !mFilterDisplayList.contains(startDisplayPackage) && !screenOff) {
                    sendMessageData(startDisplayProcess, startDisplayPackage,
                            getSimpleClassName(startDisplayClassName), ANTI_DISPLAY_END,
                            MSG_STATS_DISPLAY);
                }
                for (Map.Entry<String, Integer> entry : startBackstageApp.entrySet()) {
                    Slog.d(TAG, "shutdown or reboot packageName=" + entry.getKey() + ", uid="
                            + entry.getValue());
                    sendMessageData(entry.getKey(), entry.getKey(), "", ANTI_STARTPROCESS_END,
                            MSG_STATS_BACKSTAGE);
                }
            }
        });
    }

    private void statsMusicPlayTime(int type, int uid) {
        String packageName = null;
        if (mMusicAppUid != uid) {
            for (Map.Entry<String, Integer> entry : startBackstageApp.entrySet()) {
                if (entry.getValue() == uid) {
                    mMusicAppUid = entry.getValue();
                    mMusicApp = entry.getKey();
                    packageName = mMusicApp;
                }
            }
        } else {
            packageName = mMusicApp;
        }
        if (debug) {
            Slog.d(TAG, "packageName=" + packageName);
        }
        if (mFilterMusicApp.contains(packageName)) {
            Slog.d(TAG, "filter music app =" + packageName);
        } else {
            sendDataToApp(packageName, packageName, "", type);
        }
    }

    public void dump(java.io.FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("Dump time : " + DateFormat.getDateTimeInstance().format(new Date()));
        pw.println("AntiAddictionManagerService Dump Info");
        pw.println("========================================");
        pw.println("mMusicApp list: ");
        pw.println("PackageName [" + mMusicApp + "]  uid [" + mMusicAppUid + "]");
        pw.println("startBackstageApp list: ");
        for (Map.Entry<String, Integer> entry : startBackstageApp.entrySet()) {
            pw.println("pkgName [" + entry.getKey() + "]  uid [" + entry.getValue() + "]");
        }
        pw.println("attachAppList list: ");
        for (String className : attachAppList) {
            pw.println("className [" + className + "]");
        }
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

    public void notifyAppState(String processName, String packageName, String className, int uid,
            AppState appState) {
        if ((appState == AppState.startProcess || appState == AppState.Dead)
                && (mFilterWhiteList.contains(packageName) || packageName.startsWith("com.android")
                        || packageName.startsWith("com.qualcomm")
                        || packageName.startsWith("org"))) {
            Slog.d(TAG, packageName + " filter statistics");
            return;
        }
        if (debug) {
            Slog.d(TAG, "processName=" + processName + ", packageName = " + packageName
                    + ", className = " + className + ", uid=" + uid + ", appState=" + appState);
        }
        synchronized (mHost) {
            switch (appState) {
                case Dead:
                    sendMessageData(processName, packageName, className, ANTI_STARTPROCESS_END,
                            MSG_STATS_BACKSTAGE);
                    break;
                case ProcessEmpty:
                    sendMessageData(processName, packageName, className, ANTI_PROCESS_EMPTY,
                            MSG_STATS_BACKSTAGE);
                    break;
                case ProcessNoEmpty:
                    sendMessageData(processName, packageName, className, ANTI_PROCESS_NO_EMPTY,
                            MSG_STATS_BACKSTAGE);
                    break;
                case Resumed: {
                    boolean isMainBoot = isMainBootActivity(className);
                    if (!attachAppList.contains(className)) {
                        className = "";
                    }
                    if (startDisplayPackage == null || !startDisplayPackage.equals(packageName)
                            || screenOff || isMainBoot) {
                        if (startDisplayPackage != null
                                && !mFilterDisplayList.contains(startDisplayPackage) && !screenOff) {
                            sendMessageData(startDisplayProcess, startDisplayPackage,
                                    getSimpleClassName(startDisplayClassName), ANTI_DISPLAY_END,
                                    MSG_STATS_DISPLAY);
                        }
                        if (!mFilterDisplayList.contains(packageName)) {
                            sendMessageData(processName, packageName, getSimpleClassName(className),
                                    ANTI_DISPLAY_START, MSG_STATS_DISPLAY);
                        }
                        startDisplayPackage = packageName;
                        startDisplayProcess = processName;
                        startDisplayClassName = className;
                        screenOff = false;
                    }
                    break;
                }
                case shutdown:
                    shutdown();
                    break;
                case startProcess:
                    startBackstageApp.put(processName, Integer.valueOf(uid));
                    sendMessageData(processName, packageName, className, ANTI_STARTPROCESS_START,
                            MSG_STATS_BACKSTAGE);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void registerAddictionCallback(IAddictionCallback callback) {
        Slog.d(TAG, "registerAddictionCallback result=" + mCallbackList.register(callback));
    }

    @Override
    public void unregisterAddictionCallback(IAddictionCallback callback) {
        Slog.d(TAG, "unregisterAddictionCallback result=" + mCallbackList.unregister(callback));
    }

    @Override
    public void statisticalAudioDuration(int startTime, int duration) {
        if (debug) {
            Slog.d(TAG, "type=" + startTime + ", uid=" + duration);
        }
        Message message = Message.obtain();
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TYPE, startTime);
        bundle.putInt(KEY_UID, duration);
        message.setData(bundle);
        message.what = MSG_STATS_MUSICPLAY;
        mHandler.sendMessage(message);
    }

    public void systemReady() {
        Slog.i(TAG, "systemReady");
    }
}