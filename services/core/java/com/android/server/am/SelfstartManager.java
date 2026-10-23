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

package com.android.server.am;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Vendor "self-start" (autostart) manager. It keeps a whitelist of packages that
 * are allowed to be started in the background and blocks the rest, and can be
 * controlled by a companion launcher application.
 *
 * <p>WARNING: This implements a whitelist that silently prevents arbitrary
 * third-party applications from starting processes / receiving broadcasts,
 * unless the user (or, in practice, the vendor launcher) has explicitly added
 * them. Restored for completeness of the framework rewrite; it is a
 * user-freedom-restricting mechanism and is not endorsed.
 *
 * @hide
 */
public class SelfstartManager {
    private static final String TAG = SelfstartManager.class.getSimpleName();

    private static final String CONTEXT_PACKAGE = "com.xtc.i3launcher";
    private static final String ENABLE_PROP_KEY = "persist.sys.selfstart.open";
    private static final String PERMISSION_PREFERENCE = "watch_boot_start";
    private static final String PREF_ACTION = "com.xtc.selfstart.PREF_CHANGE_ACTION";
    private static final int MSG_UPDATE_WHITELIST = 1;

    private final Context mContext;
    private final ActivityManagerService mAms;
    private final SelfstartThreadHandler mHandler;
    private final HandlerThread mHandlerThread = new HandlerThread("SelfstartManager",
            android.os.Process.THREAD_PRIORITY_FOREGROUND);
    private final AtomicFile mWhiteListFile;

    private final List<String> mWhiteList = new ArrayList<>();
    private final List<String> mDynamicList = new LinkedList<>();
    private final List<String> mBroadcastList = new ArrayList<>();
    private boolean isQmmi;

    private class SelfstartThreadHandler extends Handler {
        SelfstartThreadHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_UPDATE_WHITELIST) {
                updateWhiteListAll();
            }
        }
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (isQmmi) {
                return null;
            }
            readWhiteListFromApp();
            return null;
        }
    }

    @SuppressLint({"NewApi"})
    public SelfstartManager(Context context, ActivityManagerService ams) {
        mContext = context;
        mAms = ams;
        mHandlerThread.start();
        mHandler = new SelfstartThreadHandler(mHandlerThread.getLooper());
        isQmmi = "ffbm-02".equals(SystemProperties.get("ro.bootmode", "00"));
        mWhiteListFile = new AtomicFile(new File(getSystemDir(), "xtcwhitelist.xml"));
    }

    private static File getSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    private String[] getPackagesForUid(int uid) {
        try {
            return AppGlobals.getPackageManager().getPackagesForUid(uid);
        } catch (RemoteException e) {
            Log.d(TAG, "get pkg for uid error", e);
            return null;
        }
    }

    private void addToList(List<String> list, String value) {
        synchronized (list) {
            if (!list.contains(value)) {
                list.add(value);
            }
        }
    }

    private void initDefaultList() {
        synchronized (mWhiteList) {
            if (!isQmmi) {
                mWhiteList.add("com.xtc.i3launcher");
                mWhiteList.add("com.xtc.setting");
                mWhiteList.add("com.xtc.validationtest");
            }
            mWhiteList.add("com.xtc.mmitest");
            mWhiteList.add("org.simalliance.openmobileapi.service");
        }
        synchronized (mBroadcastList) {
            mBroadcastList.add("com.xtc.push.THIRD_MESSAGE");
            mBroadcastList.add("android.appwidget.action.APPWIDGET_ENABLED");
            mBroadcastList.add("android.appwidget.action.APPWIDGET_UPDATE");
        }
    }

    private void initService() {
        Slog.d(TAG, "initService start");
        mWhiteList.clear();
        initDefaultList();
        readWhiteListAll();
        Slog.d(TAG, "initService end");
    }

    private void readWhiteListAll() {
        Slog.v(TAG, "readWhiteListAll start");
        FileInputStream stream = null;
        BufferedReader reader = null;
        try {
            stream = mWhiteListFile.openRead();
            reader = new BufferedReader(new InputStreamReader(stream, Charset.defaultCharset()));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.d(TAG, "readWhiteListAll: packageName=" + line);
                mWhiteList.add(line);
            }
        } catch (Exception e) {
            Log.wtf(TAG, "problem reading readWhiteListAll", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                libcore.io.IoUtils.closeQuietly(stream);
            }
        }
        Slog.v(TAG, "readWhiteListAll end");
    }

    private void readWhiteListFromApp() {
        synchronized (mWhiteList) {
            Log.d(TAG, "readWhiteListFromApp start");
            Context packageContext;
            try {
                packageContext = mContext.createPackageContext(CONTEXT_PACKAGE,
                        Context.CONTEXT_IGNORE_SECURITY);
            } catch (android.content.pm.PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                packageContext = null;
            }
            if (packageContext == null) {
                Log.e(TAG, "can not create safe context");
                return;
            }
            Map<String, ?> all = packageContext.getSharedPreferences(PERMISSION_PREFERENCE,
                    Context.MODE_MULTI_PROCESS).getAll();
            for (String key : all.keySet()) {
                boolean enabled = (Boolean) all.get(key);
                if (!mWhiteList.contains(key) && enabled) {
                    mWhiteList.add(key);
                    Log.d(TAG, "add to list pkg:" + key + " value:" + enabled);
                }
            }
        }
    }

    public void checkBroadcast(Intent intent, int uid, int targetUid) {
        if (intent == null || intent.getAction() == null
                || !mBroadcastList.contains(intent.getAction())) {
            return;
        }
        String[] packages = getPackagesForUid(targetUid);
        if (packages == null) {
            return;
        }
        for (String packageName : packages) {
            addToList(mDynamicList, packageName);
        }
    }

    public boolean enable() {
        return SystemProperties.getBoolean(ENABLE_PROP_KEY, true);
    }

    public boolean isPackageWhiteList(String packageName) {
        Log.d(TAG, "isPackageWhiteList: packageName =" + packageName);
        return mWhiteList.contains(packageName);
    }

    public boolean isInternalPackage(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "empty package");
            return false;
        }
        return packageName.startsWith("android")
                || packageName.startsWith("com.android")
                || packageName.startsWith("com.qualcomm")
                || packageName.startsWith("com.qti")
                || packageName.startsWith("org.codeaurora")
                || packageName.startsWith("com.cttl")
                || packageName.startsWith("com.tsinghua")
                || packageName.startsWith("com.cghs.stresstest");
    }

    public boolean restrictStartProcess(String packageName, String processName) {
        if (!enable() || mWhiteList.contains(packageName) || mDynamicList.contains(packageName)
                || isInternalPackage(packageName)) {
            return false;
        }
        Log.d(TAG, "restrict start process pkg:" + packageName + " process:" + processName);
        return true;
    }

    public boolean restrictRetrieveService(Intent intent, String packageName, int uid, int targetUid,
            String targetPackage, boolean isExported) {
        if (intent == null || !isExported || targetPackage == null
                || targetPackage.equals(packageName)
                || !restrictStartProcess(targetPackage, targetPackage)) {
            return false;
        }
        Log.d(TAG, "restrictRetrieveService pkg:" + packageName + " targetPkg:" + targetPackage);
        return true;
    }

    public void resumeTopActivity(String packageName) {
        Log.d(TAG, "resumeTopActivity:" + packageName);
        addToList(mDynamicList, packageName);
    }

    public void handlePackageKilled(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return;
        }
        boolean alive = false;
        Iterator<ActivityManager.RunningAppProcessInfo> iterator =
                mAms.getRunningAppProcesses().iterator();
        while (iterator.hasNext() && !alive) {
            String[] pkgList = iterator.next().pkgList;
            for (String pkg : pkgList) {
                if (packageName.equals(pkg)) {
                    alive = true;
                    break;
                }
            }
        }
        if (!alive) {
            synchronized (mDynamicList) {
                mDynamicList.remove(packageName);
            }
        }
        Log.d(TAG, "handlePackageKilled:" + packageName + " alive:" + alive);
    }

    public boolean setSelfstartPackage(String packageName, boolean enable) {
        Log.d(TAG, "setSelfstartPackage: packageName=" + packageName + ", enable=" + enable);
        synchronized (mWhiteList) {
            if (enable) {
                if (!mWhiteList.contains(packageName)) {
                    mWhiteList.add(packageName);
                    Log.d(TAG, "setSelfstartPackage add to list pkg:" + packageName);
                    mHandler.sendEmptyMessage(MSG_UPDATE_WHITELIST);
                }
            } else if (mWhiteList.contains(packageName)) {
                mWhiteList.remove(packageName);
                Log.d(TAG, "remove to list pkg:" + packageName);
                mHandler.sendEmptyMessage(MSG_UPDATE_WHITELIST);
            }
        }
        return true;
    }

    public boolean setSelfstartPackageList(List<String> packages) {
        Log.d(TAG, "setSelfstartPackageList");
        boolean changed = false;
        synchronized (mWhiteList) {
            for (String packageName : packages) {
                if (!mWhiteList.contains(packageName)) {
                    mWhiteList.add(packageName);
                    Log.d(TAG, "setSelfstartPackageList add to list pkg:" + packageName);
                    changed = true;
                }
            }
            if (changed) {
                mHandler.sendEmptyMessage(MSG_UPDATE_WHITELIST);
            }
        }
        return true;
    }

    public void printSelfstartWhiteList(java.io.PrintWriter pw) {
        pw.println("print SelfstartWhiteList whitelist:");
        for (String packageName : mWhiteList) {
            pw.println("packageName [" + packageName + "]");
        }
    }

    public void systemReady() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                initService();
            }
        });
    }

    void updateWhiteListAll() {
        Log.d(TAG, "updateWhiteListAll start");
        FileOutputStream stream = null;
        BufferedWriter writer = null;
        try {
            stream = mWhiteListFile.startWrite();
            writer = new BufferedWriter(new OutputStreamWriter(stream, Charset.defaultCharset()));
            StringBuilder sb = new StringBuilder();
            for (String packageName : mWhiteList) {
                sb.append(packageName);
                sb.append("\n");
            }
            writer.append(sb);
            writer.flush();
            mWhiteListFile.finishWrite(stream);
        } catch (Exception e) {
            if (stream != null) {
                mWhiteListFile.failWrite(stream);
            }
            Log.wtf(TAG, "problem reading updateWhiteListAll", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "updateWhiteListAll end");
    }
}