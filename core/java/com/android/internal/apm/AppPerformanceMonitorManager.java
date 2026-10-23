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

package com.android.internal.apm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;

import com.android.internal.apm.EvilMethodTracer.EvilMethodTracerData;
import com.android.internal.apm.FrameTracer.FrameTracerData;
import com.android.internal.apm.StartupTracer.StartupTracerData;

/**
 * Per-process application performance monitor manager. Manages the lifetime
 * of the UI / SQL / battery monitors, aggregates their data and reports it to
 * the on-device APM service.
 *
 * @hide
 */
public class AppPerformanceMonitorManager {
    private static final String TAG = "APMManager";
    private static final long REPORT_MONITOR_DATA_INTERVAL = 20000L;

    private static final int MSG_INIT_MONITORS = 1000001;
    private static final int MSG_UPDATE_MONITORS = 1000002;
    private static final int MSG_REPORT_MONITOR_DATA = 1000003;
    private static final int MSG_REPORT_OVER_THRESHOLD_DATA = 1000004;
    private static final int MSG_UPDATE_UI_MONITOR_DATA = 1000005;
    private static final int MSG_UPDATE_SQL_MONITOR_DATA = 1000006;

    private static AppPerformanceMonitorManager sInstance;

    private final Object mLock = new Object();
    private final Object mDataLock = new Object();
    private final Object mCallbackLock = new Object();
    private long mLastReportMonitorDataTime = 0;

    private final IAppPerformanceMonitorCallback mCallback = new IAppPerformanceMonitorCallback.Stub() {
        @Override
        public void onAPMConfigurationChanged(APMConfiguration config) {
            APMDebugLog.d(TAG, "onAPMConfigurationChanged = " + config);
            synchronized (mLock) {
                boolean firstConfig = mAPMConfiguration == null;
                mAPMConfiguration = config;
                if (firstConfig) {
                    mAPMHandler.sendEmptyMessage(MSG_INIT_MONITORS);
                } else {
                    mAPMHandler.sendEmptyMessage(MSG_UPDATE_MONITORS);
                }
            }
        }
    };

    private Context mContext;
    private String mPackageName;
    private String mProcessName;
    private int mVersionCode;
    private String mVersionName;
    private IAppPerformanceMonitorManager mService;
    private APMConfiguration mAPMConfiguration;
    private APMHandler mAPMHandler;
    private CallBackHandler mCallBackHandler;
    private UIMonitor mUIMonitor;
    private SQLiteMonitor mSQLiteMonitor;
    private UIMonitorData mPendingUIMonitorData;
    private SQLiteMonitorData mPendingSQLiteMonitorData;
    private UIMonitorCallback mUIMonitorCallback;
    private OverThresholdCallback mOverThresholdCallback;

    protected AppPerformanceMonitorManager() {
        try {
            mService = IAppPerformanceMonitorManager.Stub.asInterface(
                    ServiceManager.getServiceOrThrow(Context.XTC_APM_SERVICE));
        } catch (ServiceManager.ServiceNotFoundException e) {
            e.printStackTrace();
        }
        HandlerThread handlerThread = new HandlerThread("APMHandlerThread");
        handlerThread.start();
        mAPMHandler = new APMHandler(handlerThread.getLooper());
        HandlerThread callbackThread = new HandlerThread("CallbackHandlerThread");
        callbackThread.start();
        mCallBackHandler = new CallBackHandler(callbackThread.getLooper());
    }

    public static void init() {
        sInstance = new AppPerformanceMonitorManager();
    }

    public static AppPerformanceMonitorManager getInstance() {
        return sInstance;
    }

    public void bindApplication(Context context, String packageName, String processName) {
        APMDebugLog.d(TAG, "bindApplication pkgName = " + packageName
                + ", procName = " + processName);
        mContext = context;
        mPackageName = packageName;
        mProcessName = processName;
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
            if (packageInfo != null) {
                mVersionCode = packageInfo.versionCode;
                mVersionName = packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (mService != null) {
            try {
                mService.bindAppPerformanceMonitorService(mPackageName, mVersionCode,
                        mVersionName, mProcessName, mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void setAPMConfiguration(APMConfiguration config) {
        synchronized (mLock) {
            APMDebugLog.d(TAG, "setAPMConfiguration = " + config);
            mAPMConfiguration = config;
            mAPMHandler.sendEmptyMessage(MSG_UPDATE_MONITORS);
        }
    }

    public APMConfiguration getAPMConfiguration() {
        synchronized (mLock) {
            return mAPMConfiguration;
        }
    }

    public void setUIMonitorCallback(UIMonitorCallback callback) {
        synchronized (mCallbackLock) {
            mUIMonitorCallback = callback;
        }
    }

    public void setOverThresholdCallback(OverThresholdCallback callback) {
        synchronized (mCallbackLock) {
            mOverThresholdCallback = callback;
        }
    }

    public CallBackHandler getCallBackHandler() {
        return mCallBackHandler;
    }

    public void onUIMonitorDataChanged(UIMonitorData data) {
        if (mAPMHandler.hasMessages(MSG_UPDATE_UI_MONITOR_DATA)) {
            mAPMHandler.removeMessages(MSG_UPDATE_UI_MONITOR_DATA);
        }
        mAPMHandler.obtainMessage(MSG_UPDATE_UI_MONITOR_DATA, data).sendToTarget();
    }

    public void onSQLiteMonitorDataChanged(SQLiteMonitorData data) {
        if (mAPMHandler.hasMessages(MSG_UPDATE_SQL_MONITOR_DATA)) {
            mAPMHandler.removeMessages(MSG_UPDATE_SQL_MONITOR_DATA);
        }
        mAPMHandler.obtainMessage(MSG_UPDATE_SQL_MONITOR_DATA, data).sendToTarget();
    }

    public void reportActivityLaunchTimeLocked(LaunchTimeData launchTimeData) {
        APMDebugLog.d(TAG, "reportActivityLaunchTimeLocked = " + launchTimeData);
        synchronized (mLock) {
            if (mUIMonitor != null) {
                mUIMonitor.reportActivityLaunchTimeLocked(launchTimeData);
            }
        }
    }

    public void reportOverThresholdData(int type, long timestamp, String threshold,
            String value, String scene) {
        APMDebugLog.d(TAG, "reportOverThresholdData type = " + type);
        APMOverThresholdData data = new APMOverThresholdData(mPackageName, mProcessName,
                mVersionCode, mVersionName, timestamp, type);
        data.setThresholdValue(threshold);
        data.setOverThresholdValue(value);
        data.setOverThresholdScene(scene);
        mAPMHandler.obtainMessage(MSG_REPORT_OVER_THRESHOLD_DATA, data).sendToTarget();
        mCallBackHandler.onOverThredholdDataChanged(data);
    }

    private void initMonitorsLocked() {
        APMDebugLog.d(TAG, "initMonitors");
        synchronized (mLock) {
            mUIMonitor = new UIMonitor(mAPMConfiguration, this);
            SQLiteMonitor.init(mAPMConfiguration, this);
            mSQLiteMonitor = SQLiteMonitor.getInstance();
        }
    }

    private void updateMonitorsLocked() {
        APMDebugLog.d(TAG, "updateMonitors");
        synchronized (mLock) {
            if (mUIMonitor != null) {
                mUIMonitor.onAPMConfigurationChanged(mAPMConfiguration);
            }
            if (mSQLiteMonitor != null) {
                mSQLiteMonitor.onAPMConfigurationChanged(mAPMConfiguration);
            }
        }
    }

    private void reportMonitorDataLocked() {
        APMDebugLog.d(TAG, "reportMonitorDataLocked");
        final long currentTimeMillis = System.currentTimeMillis();
        synchronized (mDataLock) {
            if (mService != null) {
                try {
                    if (mPendingUIMonitorData != null) {
                        mService.reportUIMonitorData(mCallback, mPendingUIMonitorData);
                        mPendingUIMonitorData = null;
                    }
                    if (mPendingSQLiteMonitorData != null) {
                        mService.reportSQLiteMonitorData(mCallback, mPendingSQLiteMonitorData);
                        mPendingSQLiteMonitorData = null;
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            mLastReportMonitorDataTime = currentTimeMillis;
        }
        APMDebugLog.d(TAG, "reportMonitorDataLocked used = "
                + (System.currentTimeMillis() - currentTimeMillis) + "ms");
    }

    private void reportOverThresholdDataLocked(APMOverThresholdData data) {
        APMDebugLog.d(TAG, "reportOverThresholdDataLocked = " + data);
        if (data == null) {
            return;
        }
        synchronized (mLock) {
            if (mService != null) {
                try {
                    mService.reportOverThresholdData(mCallback, data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void updateUIMonitorDataLocked(UIMonitorData data) {
        synchronized (mDataLock) {
            mPendingUIMonitorData = data;
            checkWhetherToReportMonitorDataLocked();
        }
    }

    private void updateSQLiteMonitorDataLocked(SQLiteMonitorData data) {
        synchronized (mDataLock) {
            mPendingSQLiteMonitorData = data;
            checkWhetherToReportMonitorDataLocked();
        }
    }

    private void checkWhetherToReportMonitorDataLocked() {
        if (mAPMHandler.hasMessages(MSG_REPORT_MONITOR_DATA)) {
            return;
        }
        final long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mLastReportMonitorDataTime >= REPORT_MONITOR_DATA_INTERVAL) {
            mAPMHandler.sendEmptyMessage(MSG_REPORT_MONITOR_DATA);
        } else {
            mAPMHandler.sendEmptyMessageDelayed(MSG_REPORT_MONITOR_DATA,
                    (mLastReportMonitorDataTime + REPORT_MONITOR_DATA_INTERVAL)
                            - currentTimeMillis);
        }
    }

    private class APMHandler extends Handler {
        public APMHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_INIT_MONITORS:
                    initMonitorsLocked();
                    break;
                case MSG_UPDATE_MONITORS:
                    updateMonitorsLocked();
                    break;
                case MSG_REPORT_MONITOR_DATA:
                    reportMonitorDataLocked();
                    break;
                case MSG_REPORT_OVER_THRESHOLD_DATA:
                    reportOverThresholdDataLocked((APMOverThresholdData) msg.obj);
                    break;
                case MSG_UPDATE_UI_MONITOR_DATA:
                    updateUIMonitorDataLocked((UIMonitorData) msg.obj);
                    break;
                case MSG_UPDATE_SQL_MONITOR_DATA:
                    updateSQLiteMonitorDataLocked((SQLiteMonitorData) msg.obj);
                    break;
            }
        }
    }

    protected class CallBackHandler extends Handler {
        private static final int MSG_METHOD_TRACER_DATA_CALLBACK = 2000001;
        private static final int MSG_FRAME_TRACER_DATA_CALLBACK = 2000002;
        private static final int MSG_STARTUP_TRACER_DATA_CALLBACK = 2000003;
        private static final int MSG_OVER_THRESHOLD_DATA_CALLBACK = 2000004;

        public CallBackHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            synchronized (mCallbackLock) {
                switch (msg.what) {
                    case MSG_METHOD_TRACER_DATA_CALLBACK:
                        if (mUIMonitorCallback != null) {
                            mUIMonitorCallback.onEvilMethodTracerDataChanged(
                                    (EvilMethodTracerData) msg.obj);
                        }
                        break;
                    case MSG_FRAME_TRACER_DATA_CALLBACK:
                        if (mUIMonitorCallback != null) {
                            mUIMonitorCallback.onFrameTracerDataChanged(
                                    (FrameTracerData) msg.obj);
                        }
                        break;
                    case MSG_STARTUP_TRACER_DATA_CALLBACK:
                        if (mUIMonitorCallback != null) {
                            mUIMonitorCallback.onStartupTracerDataChanged(
                                    (StartupTracerData) msg.obj);
                        }
                        break;
                    case MSG_OVER_THRESHOLD_DATA_CALLBACK:
                        if (mOverThresholdCallback != null) {
                            APMOverThresholdData data = (APMOverThresholdData) msg.obj;
                            mOverThresholdCallback.onOverThreshold(data.getType(),
                                    data.getTimestamp(), data.getThresholdValue(),
                                    data.getOverThresholdValue(), data.getOverThresholdScene());
                        }
                        break;
                }
            }
        }

        public void onEvilMethodTracerDataChanged(EvilMethodTracerData data) {
            obtainMessage(MSG_METHOD_TRACER_DATA_CALLBACK, data).sendToTarget();
        }

        public void onFrameTracerDataChanged(FrameTracerData data) {
            obtainMessage(MSG_FRAME_TRACER_DATA_CALLBACK, data).sendToTarget();
        }

        public void onStartupTracerDataChanged(StartupTracerData data) {
            obtainMessage(MSG_STARTUP_TRACER_DATA_CALLBACK, data).sendToTarget();
        }

        public void onOverThredholdDataChanged(APMOverThresholdData data) {
            obtainMessage(MSG_OVER_THRESHOLD_DATA_CALLBACK, data).sendToTarget();
        }
    }
}