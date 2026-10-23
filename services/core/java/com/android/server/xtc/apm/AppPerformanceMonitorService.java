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

package com.android.server.xtc.apm;

import android.content.ContentValues;
import android.content.Context;
import android.database.BulkCursorDescriptor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.internal.apm.APMBaseDate;
import com.android.internal.apm.APMConfiguration;
import com.android.internal.apm.APMDebugLog;
import com.android.internal.apm.APMGeneralData;
import com.android.internal.apm.APMOverThresholdData;
import com.android.internal.apm.BatteryMonitorData;
import com.android.internal.apm.IAppPerformanceMonitorCallback;
import com.android.internal.apm.IAppPerformanceMonitorManager;
import com.android.internal.apm.SQLiteMonitorData;
import com.android.internal.apm.UIMonitorData;
import com.android.server.ServiceThread;
import com.android.server.SystemService;

import java.util.HashMap;
import java.util.Iterator;

/**
 * System service that receives application performance reports from monitored
 * processes and persists them.
 *
 * <p><b>Condemnation.</b> This is an OEM monitoring/telemetry facility. The
 * collected per-application timing, SQL and UI data is written to a database
 * that the vendor uploads; users are not given a meaningful opt-out. Only
 * reproduced for behavioural compatibility.
 *
 * @hide
 */
public class AppPerformanceMonitorService extends SystemService {
    private static final String TAG = "APMService";

    private static final int MSG_LOAD_APM_CONFIG = 100001;
    private static final int MSG_UPDATE_APM_CONFIG_FOR_CLIENT = 100002;
    private static final int MSG_DATA_SAVE_SCHEDULE_TASK = 100003;
    private static final int MSG_SAVE_APM_DATA = 100004;
    private static final int MSG_SAVE_APM_OVER_THRESHOLD_DATA = 100005;
    private static final int MSG_INIT_DATABASE = 100006;

    private static final long DATA_SAVE_SCHEDULE_TIME = 3600000L;
    private static final long MIN_APM_DATA_DURATION = 3000L;

    private APMConfiguration mAPMConfiguration;
    private APMSHandler mAPMSHandler;
    private final HashMap<IBinder, AppPerformanceMonitorClient> mClientMap;
    private final Context mContext;
    private DataSaver mDataSaver;
    private SQLiteDatabase mDb;
    private final Object mLock;

    private final class APMSHandler extends Handler {
        APMSHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_LOAD_APM_CONFIG:
                    loadAPMConfigurationLocked();
                    break;
                case MSG_UPDATE_APM_CONFIG_FOR_CLIENT:
                    updateAPMConfigurationForClient((AppPerformanceMonitorClient) message.obj);
                    break;
                case MSG_DATA_SAVE_SCHEDULE_TASK:
                    dataSaveScheduleTaskLocked();
                    break;
                case MSG_SAVE_APM_DATA:
                    saveAPMDataLocked((AppPerformanceMonitorClient) message.obj);
                    break;
                case MSG_SAVE_APM_OVER_THRESHOLD_DATA:
                    saveAPMOverThresholdData((APMOverThresholdData) message.obj);
                    break;
                case MSG_INIT_DATABASE:
                    initDatabaseLocked();
                    break;
                default:
                    break;
            }
        }
    }

    private final class BinderService extends IAppPerformanceMonitorManager.Stub {
        @Override
        public void bindAppPerformanceMonitorService(String packageName, int versionCode,
                String versionName, String processName,
                IAppPerformanceMonitorCallback callback) {
            APMDebugLog.d(TAG, "bindAppPerformanceMonitorService pkgName = " + packageName
                    + ", versionCode = " + versionCode + ", versionName = " + versionName
                    + ", procName = " + processName);
            if (callback == null) {
                Log.e(TAG, "callback == null");
                return;
            }
            synchronized (mLock) {
                IBinder binder = callback.asBinder();
                if (mClientMap.containsKey(binder)) {
                    Log.w(TAG, "This application is already bind");
                } else {
                    AppPerformanceMonitorClient client = new AppPerformanceMonitorClient(
                            AppPerformanceMonitorService.this, packageName, versionCode,
                            versionName, processName, callback);
                    client.getAPMData().setBatteryMonitorData(new BatteryMonitorData());
                    mClientMap.put(binder, client);
                    mAPMSHandler.obtainMessage(MSG_UPDATE_APM_CONFIG_FOR_CLIENT, client)
                            .sendToTarget();
                }
            }
        }

        @Override
        public BulkCursorDescriptor queryAPMDatabase(String[] projection, String selection,
                String[] selectionArgs, String sortOrder, String groupBy, String having) {
            return mDataSaver.queryAPMDatabase(projection, selection, selectionArgs, groupBy,
                    having, sortOrder);
        }

        @Override
        public void reportOverThresholdData(IAppPerformanceMonitorCallback callback,
                APMOverThresholdData data) {
            APMDebugLog.d(TAG, "reportOverThresholdData");
            if (callback == null || data == null) {
                Log.e(TAG, "invalid data or callback");
            } else {
                mAPMSHandler.obtainMessage(MSG_SAVE_APM_OVER_THRESHOLD_DATA, data).sendToTarget();
            }
        }

        @Override
        public void reportSQLiteMonitorData(IAppPerformanceMonitorCallback callback,
                SQLiteMonitorData data) {
            APMDebugLog.d(TAG, "reportSQLiteMonitorData data = " + data);
            if (data == null || callback == null) {
                Log.e(TAG, "invalid data or callback");
                return;
            }
            long start = System.currentTimeMillis();
            synchronized (mLock) {
                AppPerformanceMonitorClient client = mClientMap.get(callback.asBinder());
                if (client != null) {
                    client.updateSQLiteMonitorData(data);
                } else {
                    Log.e(TAG, "client not found");
                }
            }
            APMDebugLog.d(TAG, "reportSQLiteMonitorData used = "
                    + (System.currentTimeMillis() - start) + "ms");
        }

        @Override
        public void reportUIMonitorData(IAppPerformanceMonitorCallback callback,
                UIMonitorData data) {
            APMDebugLog.d(TAG, "reportUIMonitorData data = " + data);
            if (data == null || callback == null) {
                Log.e(TAG, "invalid data or callback");
                return;
            }
            long start = System.currentTimeMillis();
            synchronized (mLock) {
                AppPerformanceMonitorClient client = mClientMap.get(callback.asBinder());
                if (client != null) {
                    client.updateUIMonitorData(data);
                } else {
                    Log.e(TAG, "client not found");
                }
            }
            APMDebugLog.d(TAG, "reportUIMonitorData used = "
                    + (System.currentTimeMillis() - start) + "ms");
        }

        @Override
        public int updateAPMDatabase(ContentValues values, String where, String[] whereArgs) {
            return mDataSaver.updateAPMDatabase(values, where, whereArgs);
        }
    }

    public AppPerformanceMonitorService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mClientMap = new HashMap<>();
        this.mContext = context;
        ServiceThread serviceThread = new ServiceThread(TAG, -2, false);
        serviceThread.start();
        this.mAPMSHandler = new APMSHandler(serviceThread.getLooper());
        this.mDb = this.mContext.openOrCreateDatabase(APMDatabaseHelper.DB_NAME, 0, null);
        this.mDataSaver = new DataSaver(this.mDb);
        this.mAPMSHandler.sendEmptyMessage(MSG_INIT_DATABASE);
    }

    private void dataSaveScheduleTaskLocked() {
        APMDebugLog.d(TAG, "dataSaveScheduleTaskLocked");
        long start = System.currentTimeMillis();
        synchronized (this.mLock) {
            Iterator<AppPerformanceMonitorClient> iterator = this.mClientMap.values().iterator();
            while (iterator.hasNext()) {
                APMGeneralData data = iterator.next().getAPMData();
                if (System.currentTimeMillis() - data.getStartTimestamp()
                        >= DATA_SAVE_SCHEDULE_TIME) {
                    data.setEndTimestamp(System.currentTimeMillis());
                    this.mDataSaver.notifyDataAvailable(new APMGeneralData(data));
                    data.setStartTimestamp(System.currentTimeMillis());
                }
            }
        }
        this.mAPMSHandler.sendEmptyMessageDelayed(MSG_DATA_SAVE_SCHEDULE_TASK,
                DATA_SAVE_SCHEDULE_TIME);
        APMDebugLog.d(TAG, "dataSaveScheduleTaskLocked used = "
                + (System.currentTimeMillis() - start) + "ms");
    }

    private void initDatabaseLocked() {
        APMDebugLog.d(TAG, "initDatabaseLocked");
        long start = System.currentTimeMillis();
        synchronized (this.mLock) {
            try {
                this.mDb.execSQL(APMDatabaseHelper.CREATE_CONFIGURATION_TABLE);
                this.mDb.execSQL(APMDatabaseHelper.CREATE_APM_DATA_TABLE);
                if (this.mDb.getVersion() == 0) {
                    this.mDb.setVersion(APMDatabaseHelper.DB_VERSION);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        APMDebugLog.d(TAG, "initDatabaseLocked done. used = "
                + (System.currentTimeMillis() - start) + "ms");
    }

    private void loadAPMConfigurationLocked() {
        APMDebugLog.d(TAG, "loadAPMConfigurationLocked");
        long start = System.currentTimeMillis();
        synchronized (this.mLock) {
            this.mAPMConfiguration = new APMConfiguration();
            Cursor cursor = this.mDb.query(APMDatabaseHelper.CONFIGURATION_TABLE,
                    APMDatabaseHelper.CONFIGURATION_TABLE_COLUMNS, null, null, null, null, null);
            if (cursor == null || !cursor.moveToFirst()) {
                this.mAPMConfiguration.setUIMonitorEnable(true);
                this.mAPMConfiguration.setEvilMethodTracerEnable(true);
                this.mAPMConfiguration.setFrameTracerEnable(true);
                this.mAPMConfiguration.setStartupTracerEnable(true);
                this.mAPMConfiguration.setSQLiteMonitorEnable(true);
                APMDebugLog.d(TAG, "mAPMConfiguration default = " + this.mAPMConfiguration);
            } else {
                cursor.getString(0);
                boolean ui = cursor.getInt(1) == 1;
                boolean evil = cursor.getInt(2) == 1;
                boolean frame = cursor.getInt(3) == 1;
                boolean startup = cursor.getInt(4) == 1;
                cursor.getInt(5);
                long methodThreshold = cursor.getLong(6);
                int dropFrameThreshold = cursor.getInt(7);
                long activityThreshold = cursor.getLong(8);
                this.mAPMConfiguration.setUIMonitorEnable(ui);
                this.mAPMConfiguration.setEvilMethodTracerEnable(evil);
                this.mAPMConfiguration.setFrameTracerEnable(frame);
                this.mAPMConfiguration.setStartupTracerEnable(startup);
                this.mAPMConfiguration.setMethodCostTimeThreshold(methodThreshold);
                this.mAPMConfiguration.setDropFrameThreshold(dropFrameThreshold);
                this.mAPMConfiguration.setActivityLaunchTimeThreshold(activityThreshold);
                boolean sql = cursor.getInt(9) == 1;
                long sqlThreshold = cursor.getLong(10);
                this.mAPMConfiguration.setSQLiteMonitorEnable(sql);
                this.mAPMConfiguration.setMainThreadSQLCostTimeThreshold(sqlThreshold);
                cursor.close();
                APMDebugLog.d(TAG, "mAPMConfiguration from DB = " + this.mAPMConfiguration);
            }
        }
        APMDebugLog.d(TAG, "loadAPMConfigurationLocked done. used = "
                + (System.currentTimeMillis() - start) + "ms");
    }

    private void saveAPMDataLocked(AppPerformanceMonitorClient client) {
        APMDebugLog.d(TAG, "saveAPMDataLocked");
        if (client == null) {
            Log.w(TAG, "client is null");
            return;
        }
        long start = System.currentTimeMillis();
        synchronized (this.mLock) {
            APMGeneralData data = client.getAPMData();
            long duration = data.getEndTimestamp() - client.getStartTimestamp();
            if (duration < MIN_APM_DATA_DURATION) {
                Log.w(TAG, "duration = " + duration + "ms, low than " + MIN_APM_DATA_DURATION
                        + "ms, ignore this apm data");
            } else {
                this.mDataSaver.notifyDataAvailable(data);
                APMDebugLog.d(TAG, "saveAPMDataLocked used = "
                        + (System.currentTimeMillis() - start) + "ms");
            }
        }
    }

    private void saveAPMOverThresholdData(APMOverThresholdData data) {
        APMDebugLog.d(TAG, "saveAPMOverThresholdData = " + data);
        if (data == null) {
            Log.w(TAG, "saveAPMOverThresholdData Error : Invalid data");
        } else {
            this.mDataSaver.notifyDataAvailable(data);
        }
    }

    private void updateAPMConfigurationForClient(AppPerformanceMonitorClient client) {
        APMDebugLog.d(TAG, "updateAPMConfigurationForClient = " + client);
        try {
            client.mCallback.onAPMConfigurationChanged(this.mAPMConfiguration);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void onAppDied(AppPerformanceMonitorClient client) {
        APMDebugLog.d(TAG, "onAppDied = " + client);
        synchronized (this.mLock) {
            if (client != null) {
                this.mClientMap.remove(client.mCallback.asBinder(), client);
                client.getAPMData().setEndTimestamp(System.currentTimeMillis());
                this.mAPMSHandler.obtainMessage(MSG_SAVE_APM_DATA, client).sendToTarget();
            }
        }
    }

    @Override
    public void onBootPhase(int phase) {
        if (phase == 1000) {
            this.mDataSaver.onBootCompleted();
        }
    }

    @Override
    public void onStart() {
        APMDebugLog.d(TAG, "onStart");
        this.mAPMSHandler.sendEmptyMessage(MSG_LOAD_APM_CONFIG);
        this.mAPMSHandler.sendEmptyMessageDelayed(MSG_DATA_SAVE_SCHEDULE_TASK,
                DATA_SAVE_SCHEDULE_TIME);
        publishBinderService(TAG, new BinderService());
    }
}