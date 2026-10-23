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

import android.database.ContentObserver;
import android.content.ContentValues;
import android.database.BulkCursorDescriptor;
import android.database.Cursor;
import android.database.CursorToBulkCursorAdaptor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.android.internal.apm.APMBaseDate;
import com.android.internal.apm.APMDebugLog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Persists {@link APMBaseDate} records into the XTC APM database and serves
 * raw queries back to the monitoring app.
 *
 * @hide
 */
public class DataSaver {
    private static final String TAG = "DataSaver";
    private static final String DATA_PATH = "/xtcdata/Logs_Collector/APMData/";
    private static final long DATA_DELETE_SCHEDULE_TIME = 14400000L;
    private static final int MSG_DATA_DELETE_SCHEDULE_TASK = 100001;

    private final SQLiteDatabase mDb;
    private final Object mLock = new Object();
    private final LinkedBlockingQueue<APMBaseDate> mAPMDateQueue = new LinkedBlockingQueue<>();
    private final SimpleDateFormat mDateFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private final SimpleDateFormat mFileTimeFormat =
            new SimpleDateFormat("HH-mm-ss", Locale.ENGLISH);

    private DataSaverHandler mDataSaverHandler;
    private DeleteUploadedDataTask mDeleteUploadedDataTask;
    private MainHandler mHandler;

    private class DataSaverHandler extends Handler {
        DataSaverHandler(Looper looper) {
            super(looper, null, true);
        }

        @Override
        public void handleMessage(Message message) {
        }
    }

    private class DeleteExpiredDataTask extends AsyncTask<Void, Void, Void> {
        private static final long DEFAULT_EXPIRED_TIME = 604800000L;

        @Override
        protected Void doInBackground(Void... params) {
            APMDebugLog.d(TAG, "start DeleteExpiredDataTask");
            long start = System.currentTimeMillis();
            synchronized (mLock) {
                int rows = mDb.delete(APMDatabaseHelper.APM_DATA_TABLE,
                        "timestamp<" + (System.currentTimeMillis() - DEFAULT_EXPIRED_TIME), null);
                APMDebugLog.d(TAG, "DeleteExpiredDataTask done. used "
                        + (System.currentTimeMillis() - start) + "ms. delete row = " + rows);
            }
            return null;
        }
    }

    private class DeleteUploadedDataTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            APMDebugLog.d(TAG, "start DeleteUploadedDataTask");
            long start = System.currentTimeMillis();
            synchronized (mLock) {
                int rows = mDb.delete(APMDatabaseHelper.APM_DATA_TABLE, "uploaded=1", null);
                APMDebugLog.d(TAG, "DeleteUploadedDataTask done. used "
                        + (System.currentTimeMillis() - start) + "ms. delete row = " + rows);
            }
            return null;
        }
    }

    private class MainHandler extends Handler {
        MainHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == MSG_DATA_DELETE_SCHEDULE_TASK) {
                deleteAPMDate();
            }
        }
    }

    public DataSaver(SQLiteDatabase db) {
        this.mDb = db;
        HandlerThread thread = new HandlerThread("DataSaverThread");
        thread.start();
        this.mDataSaverHandler = new DataSaverHandler(thread.getLooper());
        this.mDataSaverHandler.post(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    APMBaseDate data;
                    try {
                        data = mAPMDateQueue.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        data = null;
                    }
                    saveAPMDataToDatabase(data);
                }
            }
        });
        this.mHandler = new MainHandler(Looper.getMainLooper());
    }

    private void deleteAPMDate() {
        APMDebugLog.d(TAG, "deleteAPMDate");
        this.mDeleteUploadedDataTask = new DeleteUploadedDataTask();
        this.mDeleteUploadedDataTask.execute();
        this.mHandler.sendEmptyMessageDelayed(MSG_DATA_DELETE_SCHEDULE_TASK,
                DATA_DELETE_SCHEDULE_TIME);
    }

    private void deleteExpiredData() {
        APMDebugLog.d(TAG, "deleteExpiredData");
        new DeleteExpiredDataTask().execute();
    }

    private void saveAPMDataToDatabase(APMBaseDate data) {
        APMDebugLog.d(TAG, "saveAPMDataToDatabase data = " + data);
        if (data == null) {
            return;
        }
        long start = System.currentTimeMillis();
        synchronized (this.mLock) {
            long timestamp = data.getTimestamp();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(new Date(timestamp));
            ContentValues values = new ContentValues();
            values.put(APMDatabaseHelper.COLUMN_TIMESTAMP, timestamp);
            values.put(APMDatabaseHelper.COLUMN_YEAR, calendar.get(Calendar.YEAR));
            values.put(APMDatabaseHelper.COLUMN_MONTH, calendar.get(Calendar.MONTH));
            values.put(APMDatabaseHelper.COLUMN_DAY, calendar.get(Calendar.DAY_OF_MONTH));
            values.put(APMDatabaseHelper.COLUMN_HOUR, calendar.get(Calendar.HOUR_OF_DAY));
            values.put(APMDatabaseHelper.COLUMN_MINUTE, calendar.get(Calendar.MINUTE));
            values.put(APMDatabaseHelper.COLUMN_SECOND, calendar.get(Calendar.SECOND));
            values.put(APMDatabaseHelper.COLUMN_PKG_NAME, data.getPackageName());
            values.put(APMDatabaseHelper.COLUMN_TYPE, data.getDataType());
            values.put(APMDatabaseHelper.COLUMN_JSON_DATA, data.toJson().toString());
            values.put(APMDatabaseHelper.COLUMN_UPLOADED, 0);
            this.mDb.insert(APMDatabaseHelper.APM_DATA_TABLE, null, values);
        }
        APMDebugLog.d(TAG, "saveAPMDataToDatabase json data =  " + data.toJson().toString());
        APMDebugLog.d(TAG, "saveAPMDataToDatabase done. used "
                + (System.currentTimeMillis() - start) + "ms");
    }

    private void saveAPMDataToFile(APMBaseDate data) {
        APMDebugLog.d(TAG, "saveAPMDataToFile data = " + data);
        if (data == null) {
            return;
        }
        long start = System.currentTimeMillis();
        String dir = DATA_PATH
                + this.mDateFormat.format(new Date(data.getTimestamp()))
                + File.separator + data.getPackageName();
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            try {
                if (!dirFile.mkdirs()) {
                    APMDebugLog.e(TAG, "saveAPMDataToFile mkdirs fail : " + dir);
                    return;
                }
                dirFile.setReadable(true, false);
                dirFile.setWritable(true, false);
                File parent = dirFile.getParentFile();
                parent.setReadable(true, false);
                parent.setWritable(true, false);
                File grandParent = parent.getParentFile();
                grandParent.setReadable(true, false);
                grandParent.setWritable(true, false);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        File file = new File(dirFile, data.getDataType() + "_"
                + this.mFileTimeFormat.format(new Date(data.getTimestamp())) + ".json");
        boolean append = false;
        if (file.exists()) {
            append = true;
        } else {
            try {
                if (!file.createNewFile()) {
                    APMDebugLog.e(TAG, "saveAPMDataToFile newFile fail : " + file.getPath());
                    return;
                }
                file.setReadable(true, false);
                file.setWritable(true, false);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, append))) {
            if (append) {
                writer.newLine();
            }
            writer.write(data.toJson().toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        APMDebugLog.d(TAG, "saveAPMDataToFile json data =  " + data.toJson().toString());
        APMDebugLog.d(TAG, "saveAPMDataToFile done. used "
                + (System.currentTimeMillis() - start) + "ms");
    }

    public void notifyDataAvailable(APMBaseDate data) {
        APMDebugLog.d(TAG, "notifyDataAvailable = " + data);
        if (data == null) {
            return;
        }
        try {
            this.mAPMDateQueue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onBootCompleted() {
        APMDebugLog.d(TAG, "onBootCompleted");
        deleteAPMDate();
        deleteExpiredData();
    }

    public BulkCursorDescriptor queryAPMDatabase(String[] projection, String selection,
            String[] selectionArgs, String groupBy, String having, String sortOrder) {
        APMDebugLog.d(TAG, "queryAPMDatabase");
        synchronized (this.mLock) {
            Cursor cursor = this.mDb.query(APMDatabaseHelper.APM_DATA_TABLE, projection, selection,
                    selectionArgs, groupBy, having, sortOrder);
            CursorToBulkCursorAdaptor adaptor = null;
            try {
                ContentObserver observer = new ContentObserver(this.mHandler) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                    }
                };
                adaptor = new CursorToBulkCursorAdaptor(cursor, observer.getContentObserver(),
                        "APMDatabaseHelper");
                cursor = null;
                BulkCursorDescriptor descriptor = adaptor.getBulkCursorDescriptor();
                APMDebugLog.d(TAG, "queryAPMDatabase descriptor = " + descriptor);
                adaptor = null;
                return descriptor;
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
                if (adaptor != null) {
                    adaptor.close();
                }
            }
        }
    }

    public int updateAPMDatabase(ContentValues values, String where, String[] whereArgs) {
        APMDebugLog.d(TAG, "updateAPMDatabase");
        synchronized (this.mLock) {
            try {
                return this.mDb.update(APMDatabaseHelper.APM_DATA_TABLE, values, where, whereArgs);
            } catch (Exception e) {
                return -1;
            }
        }
    }
}