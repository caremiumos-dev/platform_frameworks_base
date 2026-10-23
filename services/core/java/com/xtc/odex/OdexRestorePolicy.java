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

package com.xtc.odex;

import android.app.AppGlobals;
import android.app.ApplicationErrorReport;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.am.ActivityManagerService;
import com.xtc.moduleswitch.ModuleSwitch;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Re-optimizes (force dex-opt) packages that repeatedly crash with errors
 * typically caused by a corrupt odex/vdex. The policy is enabled and tuned
 * through the vendor module-switch configuration.
 *
 * @hide
 */
public class OdexRestorePolicy {
    private static final String TAG = "OdexRestorePolicy";
    private static final boolean DBG = true;

    private static final int DEFAULT_THRESHOLD = 5;
    private static final int MODULE_SWITCH_ID = 3351;
    private static final int MSG_PERFORM_DEX_OPT = 10001;

    private static final String EXTRA_TRIGGER_COUNT = "triggerCount";
    private static final String EXTRA_INTERVAL = "interval";
    private static final String EXTRA_EXCEPTION_LIST = "exceptionList";

    private static final ArrayList<String> sJavaExceptionList = new ArrayList<>(Arrays.asList(
            VerifyError.class.getName(),
            NoClassDefFoundError.class.getName(),
            NoSuchFieldError.class.getName(),
            NoSuchMethodError.class.getName(),
            AbstractMethodError.class.getName(),
            IllegalAccessError.class.getName(),
            IncompatibleClassChangeError.class.getName(),
            InstantiationError.class.getName()));

    private static PerformDexOptHandler sPerformDexOptHandler;

    private final Context mContext;
    private final ActivityManagerService mService;
    private final HashMap<String, OdexExceptionPackage> mPackageMap = new HashMap<>();
    private final Object mLock = new Object();
    private final Object mDexOptLock = new Object();
    private final ArrayList<String> mTmpExceptionList = new ArrayList<>();

    private int mTriggerThreshold;

    private class PerformDexOptHandler extends Handler {
        PerformDexOptHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_PERFORM_DEX_OPT) {
                performDexOptLocked((String) msg.obj);
            }
        }
    }

    public OdexRestorePolicy(Context context, ActivityManagerService service) {
        mContext = context;
        mService = service;
    }

    private PerformDexOptHandler getPerformDexOptHandler() {
        if (sPerformDexOptHandler == null) {
            HandlerThread thread = new HandlerThread("PerformDexOpt Handler Thread");
            thread.start();
            sPerformDexOptHandler = new PerformDexOptHandler(thread.getLooper());
        }
        return sPerformDexOptHandler;
    }

    public void handleAppCrashInOdexRestorePolicy(String packageName,
            ApplicationErrorReport.CrashInfo crashInfo) {
        if (packageName == null) {
            Slog.w(TAG, "Unknown package");
            return;
        }
        if (crashInfo == null) {
            Slog.w(TAG, "Invalid crashInfo");
            return;
        }
        if (!isModuleSwitchEnable()) {
            Slog.w(TAG, "Module switch(3351) is disable");
            return;
        }
        updateParamsFromModuleSwitch();
        if ("Native crash".equals(crashInfo.exceptionClassName)) {
            handleNativeCrashUnchecked(packageName, crashInfo);
        } else {
            handleJavaCrashUnchecked(packageName, crashInfo);
        }
    }

    private void handleJavaCrashUnchecked(String packageName,
            ApplicationErrorReport.CrashInfo crashInfo) {
        Slog.d(TAG, "handleJavaCrashUnchecked pkgName = " + packageName
                + ", exceptionClassName = " + crashInfo.exceptionClassName
                + ", mTriggerThreshold = " + mTriggerThreshold);
        synchronized (mLock) {
            if (!sJavaExceptionList.contains(crashInfo.exceptionClassName)
                    && !mTmpExceptionList.contains(crashInfo.exceptionClassName)) {
                return;
            }
            OdexExceptionPackage pkg = mPackageMap.get(packageName);
            if (pkg == null) {
                pkg = makeOdexExceptionPackage(packageName);
            }
            pkg.recordJavaException();
            if (pkg.getJECount() < mTriggerThreshold || pkg.getState() != OdexRestoreState.NONE) {
                OdexRestoreMonitorData.reportDataByXtcBehavior(mContext, packageName,
                        pkg.getVersionCode(), pkg.getVersionName(), crashInfo.exceptionClassName,
                        OdexRestoreState.NONE);
            } else {
                getPerformDexOptHandler().obtainMessage(MSG_PERFORM_DEX_OPT, packageName)
                        .sendToTarget();
                pkg.setState(OdexRestoreState.RESTORING);
                OdexRestoreMonitorData.reportDataByXtcBehavior(mContext, packageName,
                        pkg.getVersionCode(), pkg.getVersionName(), crashInfo.exceptionClassName,
                        OdexRestoreState.RESTORING);
            }
            mPackageMap.put(packageName, pkg);
            Slog.d(TAG, "handleJavaCrashUnchecked OdexExceptionPackage = " + pkg);
        }
    }

    private void handleNativeCrashUnchecked(String packageName,
            ApplicationErrorReport.CrashInfo crashInfo) {
        Slog.d(TAG, "handleNativeCrashUnchecked pkgName = " + packageName
                + ", exceptionClassName = " + crashInfo.exceptionClassName
                + ", mTriggerThreshold = " + mTriggerThreshold);
        synchronized (mLock) {
            if (!crashInfo.stackTrace.contains(".odex") && !crashInfo.stackTrace.contains(".vdex")) {
                return;
            }
            OdexExceptionPackage pkg = mPackageMap.get(packageName);
            if (pkg == null) {
                pkg = makeOdexExceptionPackage(packageName);
            }
            pkg.recordNativeException();
            if (pkg.getNECount() < mTriggerThreshold || pkg.getState() != OdexRestoreState.NONE) {
                OdexRestoreMonitorData.reportDataByXtcBehavior(mContext, packageName,
                        pkg.getVersionCode(), pkg.getVersionName(), crashInfo.exceptionClassName,
                        OdexRestoreState.NONE);
            } else {
                getPerformDexOptHandler().obtainMessage(MSG_PERFORM_DEX_OPT, packageName)
                        .sendToTarget();
                pkg.setState(OdexRestoreState.RESTORING);
                OdexRestoreMonitorData.reportDataByXtcBehavior(mContext, packageName,
                        pkg.getVersionCode(), pkg.getVersionName(), crashInfo.exceptionClassName,
                        OdexRestoreState.RESTORING);
            }
            mPackageMap.put(packageName, pkg);
            Slog.d(TAG, "handleNativeCrashUnchecked OdexExceptionPackage = " + pkg);
        }
    }

    private boolean isModuleSwitchEnable() {
        return ModuleSwitch.queryModuleSwitch(mContext, MODULE_SWITCH_ID, true);
    }

    private OdexExceptionPackage makeOdexExceptionPackage(String packageName) {
        int versionCode = -1;
        String versionName = null;
        IPackageManager pm = AppGlobals.getPackageManager();
        if (pm != null) {
            try {
                PackageInfo info = pm.getPackageInfo(packageName, 0, UserHandle.myUserId());
                versionCode = info.versionCode;
                versionName = info.versionName;
            } catch (RemoteException e) {
            }
        }
        return new OdexExceptionPackage(packageName, versionCode, versionName);
    }

    private void performDexOptLocked(String packageName) {
        synchronized (mDexOptLock) {
            if (packageName == null) {
                Slog.e(TAG, "performDexOptLocked error: unknown package");
                return;
            }
            long start = System.currentTimeMillis();
            int state = OdexRestoreState.RESTORED_SUCCESS;
            IPackageManager pm = AppGlobals.getPackageManager();
            if (pm != null) {
                try {
                    pm.forceDexOpt(packageName);
                } catch (RemoteException e) {
                    Slog.e(TAG, "performDexOptLocked error: RemoteException ", e);
                    state = OdexRestoreState.RESTORED_FAILURE;
                } catch (IllegalStateException e) {
                    Slog.e(TAG, "performDexOptLocked error: IllegalStateException ", e);
                    state = OdexRestoreState.RESTORED_FAILURE;
                }
            } else {
                Slog.e(TAG, "performDexOptLocked error: missing package manager");
                state = OdexRestoreState.RESTORED_FAILURE;
            }
            String elapsed = String.valueOf(System.currentTimeMillis() - start);
            synchronized (mLock) {
                OdexExceptionPackage pkg = mPackageMap.get(packageName);
                if (pkg != null) {
                    pkg.setState(state);
                    mPackageMap.put(packageName, pkg);
                    OdexRestoreMonitorData.reportDataByXtcBehavior(mContext, packageName,
                            pkg.getVersionCode(), pkg.getVersionName(), elapsed, state);
                }
            }
            Slog.e(TAG, "performDexOptLocked(" + packageName + ") done, uesd " + elapsed + "ms");
        }
    }

    private void updateParamsFromModuleSwitch() {
        String extra = ModuleSwitch.queryModuleSwitchExtraByInt(mContext, MODULE_SWITCH_ID, null);
        mTriggerThreshold = DEFAULT_THRESHOLD;
        if (extra != null) {
            try {
                JSONObject object = new JSONObject(extra);
                mTriggerThreshold = object.getInt(EXTRA_TRIGGER_COUNT);
                mTmpExceptionList.clear();
                String exceptionList = object.getString(EXTRA_EXCEPTION_LIST);
                Slog.d(TAG, "updateParamsFromModuleSwitch mTriggerThreshold = " + mTriggerThreshold
                        + ", exceptionListStr = " + exceptionList);
                if (TextUtils.isEmpty(exceptionList)) {
                    return;
                }
                String[] entries = exceptionList.split(",");
                for (String entry : entries) {
                    if (!mTmpExceptionList.contains(entry)) {
                        mTmpExceptionList.add(entry);
                    }
                }
            } catch (JSONException e) {
            }
        }
    }
}