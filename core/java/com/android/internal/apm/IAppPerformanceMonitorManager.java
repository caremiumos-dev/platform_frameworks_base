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

import android.content.ContentValues;
import android.database.BulkCursorDescriptor;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Binder interface of the application performance monitor (APM) service.
 *
 * @hide
 */
public interface IAppPerformanceMonitorManager extends IInterface {
    /** Binder transaction ids for {@link IAppPerformanceMonitorManager}. */
    int TRANSACTION_bindAppPerformanceMonitorService = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_reportUIMonitorData = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_reportSQLiteMonitorData = IBinder.FIRST_CALL_TRANSACTION + 2;
    int TRANSACTION_reportOverThresholdData = IBinder.FIRST_CALL_TRANSACTION + 3;
    int TRANSACTION_queryAPMDatabase = IBinder.FIRST_CALL_TRANSACTION + 4;
    int TRANSACTION_updateAPMDatabase = IBinder.FIRST_CALL_TRANSACTION + 5;

    void bindAppPerformanceMonitorService(String packageName, int uid, String processName,
            String versionName, IAppPerformanceMonitorCallback callback) throws RemoteException;

    void reportUIMonitorData(IAppPerformanceMonitorCallback callback, UIMonitorData data)
            throws RemoteException;

    void reportSQLiteMonitorData(IAppPerformanceMonitorCallback callback, SQLiteMonitorData data)
            throws RemoteException;

    void reportOverThresholdData(IAppPerformanceMonitorCallback callback,
            APMOverThresholdData data) throws RemoteException;

    BulkCursorDescriptor queryAPMDatabase(String[] projection, String selection,
            String[] selectionArgs, String sortOrder, String groupBy, String having)
            throws RemoteException;

    int updateAPMDatabase(ContentValues values, String where, String[] selectionArgs)
            throws RemoteException;

    abstract class Stub extends Binder implements IAppPerformanceMonitorManager {
        private static final String DESCRIPTOR = "com.android.internal.apm.IAppPerformanceMonitorManager";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAppPerformanceMonitorManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IAppPerformanceMonitorManager) {
                return (IAppPerformanceMonitorManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            switch (code) {
                case INTERFACE_TRANSACTION: {
                    reply.writeString(DESCRIPTOR);
                    return true;
                }
                case TRANSACTION_bindAppPerformanceMonitorService: {
                    data.enforceInterface(DESCRIPTOR);
                    bindAppPerformanceMonitorService(data.readString(), data.readInt(),
                            data.readString(), data.readString(),
                            IAppPerformanceMonitorCallback.Stub.asInterface(
                                    data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_reportUIMonitorData: {
                    data.enforceInterface(DESCRIPTOR);
                    reportUIMonitorData(
                            IAppPerformanceMonitorCallback.Stub.asInterface(
                                    data.readStrongBinder()),
                            _uiMonitorData(data));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_reportSQLiteMonitorData: {
                    data.enforceInterface(DESCRIPTOR);
                    reportSQLiteMonitorData(
                            IAppPerformanceMonitorCallback.Stub.asInterface(
                                    data.readStrongBinder()),
                            _sqliteMonitorData(data));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_reportOverThresholdData: {
                    data.enforceInterface(DESCRIPTOR);
                    reportOverThresholdData(
                            IAppPerformanceMonitorCallback.Stub.asInterface(
                                    data.readStrongBinder()),
                            _overThresholdData(data));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_queryAPMDatabase: {
                    data.enforceInterface(DESCRIPTOR);
                    BulkCursorDescriptor _result = queryAPMDatabase(data.createStringArray(),
                            data.readString(), data.createStringArray(), data.readString(),
                            data.readString(), data.readString());
                    reply.writeNoException();
                    if (_result != null) {
                        reply.writeInt(1);
                        _result.writeToParcel(reply, 1);
                    } else {
                        reply.writeInt(0);
                    }
                    return true;
                }
                case TRANSACTION_updateAPMDatabase: {
                    data.enforceInterface(DESCRIPTOR);
                    ContentValues _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = ContentValues.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _result = updateAPMDatabase(_arg0, data.readString(),
                            data.createStringArray());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static UIMonitorData _uiMonitorData(Parcel data) {
            if (data.readInt() != 0) {
                return UIMonitorData.CREATOR.createFromParcel(data);
            }
            return null;
        }

        private static SQLiteMonitorData _sqliteMonitorData(Parcel data) {
            if (data.readInt() != 0) {
                return SQLiteMonitorData.CREATOR.createFromParcel(data);
            }
            return null;
        }

        private static APMOverThresholdData _overThresholdData(Parcel data) {
            if (data.readInt() != 0) {
                return APMOverThresholdData.CREATOR.createFromParcel(data);
            }
            return null;
        }

        private static class Proxy implements IAppPerformanceMonitorManager {
            private final IBinder mRemote;

            Proxy(IBinder remote) {
                mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return mRemote;
            }

            public String getInterfaceDescriptor() {
                return DESCRIPTOR;
            }

            @Override
            public void bindAppPerformanceMonitorService(String packageName, int uid,
                    String processName, String versionName, IAppPerformanceMonitorCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(uid);
                    _data.writeString(processName);
                    _data.writeString(versionName);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_bindAppPerformanceMonitorService, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportUIMonitorData(IAppPerformanceMonitorCallback callback,
                    UIMonitorData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_reportUIMonitorData, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportSQLiteMonitorData(IAppPerformanceMonitorCallback callback,
                    SQLiteMonitorData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_reportSQLiteMonitorData, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void reportOverThresholdData(IAppPerformanceMonitorCallback callback,
                    APMOverThresholdData data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_reportOverThresholdData, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public BulkCursorDescriptor queryAPMDatabase(String[] projection, String selection,
                    String[] selectionArgs, String sortOrder, String groupBy, String having)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                BulkCursorDescriptor _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStringArray(projection);
                    _data.writeString(selection);
                    _data.writeStringArray(selectionArgs);
                    _data.writeString(sortOrder);
                    _data.writeString(groupBy);
                    _data.writeString(having);
                    mRemote.transact(TRANSACTION_queryAPMDatabase, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0
                            ? BulkCursorDescriptor.CREATOR.createFromParcel(_reply) : null;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int updateAPMDatabase(ContentValues values, String where,
                    String[] selectionArgs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (values != null) {
                        _data.writeInt(1);
                        values.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(where);
                    _data.writeStringArray(selectionArgs);
                    mRemote.transact(TRANSACTION_updateAPMDatabase, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}