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

package com.xtc.performance;

import android.app.ProcessInfo;
import android.content.Intent;
import android.content.pm.InstrumentationInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.List;

/**
 * Interface to the XTC performance (boost / prefork / LMK tuning) service.
 *
 * @hide
 */
public interface IPerformance extends IInterface {
    /** Binder transaction ids for {@link IPerformance}. */
    int TRANSACTION_acquireBoost = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_saveCatonStack = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_notifyActivityStateChange = IBinder.FIRST_CALL_TRANSACTION + 2;
    int TRANSACTION_scheduleBoostWhenTouch = IBinder.FIRST_CALL_TRANSACTION + 3;
    int TRANSACTION_notifyInstrumentationStatus = IBinder.FIRST_CALL_TRANSACTION + 4;
    int TRANSACTION_processLmkAdjTunningIfneeded = IBinder.FIRST_CALL_TRANSACTION + 5;
    int TRANSACTION_attachPreforkApplication = IBinder.FIRST_CALL_TRANSACTION + 6;
    int TRANSACTION_schedStartPreForkedProcesses = IBinder.FIRST_CALL_TRANSACTION + 7;
    int TRANSACTION_preforkStart = IBinder.FIRST_CALL_TRANSACTION + 8;
    int TRANSACTION_handlePreForkStartProcess = IBinder.FIRST_CALL_TRANSACTION + 9;

    void acquireBoost(int boost) throws RemoteException;

    void saveCatonStack(List<String> stack) throws RemoteException;

    void notifyActivityStateChange(Intent intent, Bundle extras) throws RemoteException;

    void scheduleBoostWhenTouch() throws RemoteException;

    void notifyInstrumentationStatus(boolean started, int pid, InstrumentationInfo info)
            throws RemoteException;

    int processLmkAdjTunningIfneeded(ProcessInfo processInfo, int expectedAdj, long pss,
            long lastPss) throws RemoteException;

    void attachPreforkApplication(int pid, IBinder bindApplicationTransaction, String processName)
            throws RemoteException;

    void schedStartPreForkedProcesses(int pid, int targetSdk, String processName)
            throws RemoteException;

    void preforkStart(int callingPid, String packageName, String processName, int targetSdk,
            int createdPid, int[] pids, int uid, int debugFlags, int mountExternal,
            String seInfo, String appDataDir, String instructionSet, String appInfoDir,
            String zygoteForkName, String[] disabledCompatChanges) throws RemoteException;

    int handlePreForkStartProcess(int callingPid, String packageName, String processName,
            int targetSdk, int createdPid, int[] pids, int uid, int debugFlags, int mountExternal,
            String seInfo, String appDataDir, String instructionSet, String appInfoDir,
            String zygoteForkName, String[] disabledCompatChanges) throws RemoteException;

    abstract class Stub extends Binder implements IPerformance {
        private static final String DESCRIPTOR = "com.xtc.performance.IPerformance";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPerformance asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IPerformance) {
                return (IPerformance) iin;
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
                case TRANSACTION_acquireBoost: {
                    data.enforceInterface(DESCRIPTOR);
                    acquireBoost(data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_saveCatonStack: {
                    data.enforceInterface(DESCRIPTOR);
                    saveCatonStack(data.createStringArrayList());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_notifyActivityStateChange: {
                    data.enforceInterface(DESCRIPTOR);
                    Intent _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    Bundle _arg1;
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    notifyActivityStateChange(_arg0, _arg1);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_scheduleBoostWhenTouch: {
                    data.enforceInterface(DESCRIPTOR);
                    scheduleBoostWhenTouch();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_notifyInstrumentationStatus: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg0 = data.readInt() != 0;
                    int _arg1 = data.readInt();
                    InstrumentationInfo _arg2;
                    if (data.readInt() != 0) {
                        _arg2 = InstrumentationInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    notifyInstrumentationStatus(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_processLmkAdjTunningIfneeded: {
                    data.enforceInterface(DESCRIPTOR);
                    ProcessInfo _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = ProcessInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _result = processLmkAdjTunningIfneeded(_arg0, data.readInt(),
                            data.readLong(), data.readLong());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_attachPreforkApplication: {
                    data.enforceInterface(DESCRIPTOR);
                    attachPreforkApplication(data.readInt(), data.readStrongBinder(),
                            data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_schedStartPreForkedProcesses: {
                    data.enforceInterface(DESCRIPTOR);
                    schedStartPreForkedProcesses(data.readInt(), data.readInt(),
                            data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_preforkStart: {
                    data.enforceInterface(DESCRIPTOR);
                    preforkStart(data.readInt(), data.readString(), data.readString(),
                            data.readInt(), data.readInt(), data.createIntArray(),
                            data.readInt(), data.readInt(), data.readInt(),
                            data.readString(), data.readString(), data.readString(),
                            data.readString(), data.readString(), data.createStringArray());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_handlePreForkStartProcess: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = handlePreForkStartProcess(data.readInt(), data.readString(),
                            data.readString(), data.readInt(), data.readInt(),
                            data.createIntArray(), data.readInt(), data.readInt(),
                            data.readInt(), data.readString(), data.readString(),
                            data.readString(), data.readString(), data.readString(),
                            data.createStringArray());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IPerformance {
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
            public void acquireBoost(int boost) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(boost);
                    mRemote.transact(TRANSACTION_acquireBoost, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void saveCatonStack(List<String> stack) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStringList(stack);
                    mRemote.transact(TRANSACTION_saveCatonStack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyActivityStateChange(Intent intent, Bundle extras)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_notifyActivityStateChange, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void scheduleBoostWhenTouch() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_scheduleBoostWhenTouch, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyInstrumentationStatus(boolean started, int pid,
                    InstrumentationInfo info) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(started ? 1 : 0);
                    _data.writeInt(pid);
                    if (info != null) {
                        _data.writeInt(1);
                        info.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_notifyInstrumentationStatus, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int processLmkAdjTunningIfneeded(ProcessInfo processInfo, int expectedAdj,
                    long pss, long lastPss) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (processInfo != null) {
                        _data.writeInt(1);
                        processInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(expectedAdj);
                    _data.writeLong(pss);
                    _data.writeLong(lastPss);
                    mRemote.transact(TRANSACTION_processLmkAdjTunningIfneeded, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void attachPreforkApplication(int pid, IBinder bindApplicationTransaction,
                    String processName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeStrongBinder(bindApplicationTransaction);
                    _data.writeString(processName);
                    mRemote.transact(TRANSACTION_attachPreforkApplication, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void schedStartPreForkedProcesses(int pid, int targetSdk, String processName)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeInt(targetSdk);
                    _data.writeString(processName);
                    mRemote.transact(TRANSACTION_schedStartPreForkedProcesses, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void preforkStart(int callingPid, String packageName, String processName,
                    int targetSdk, int createdPid, int[] pids, int uid, int debugFlags,
                    int mountExternal, String seInfo, String appDataDir, String instructionSet,
                    String appInfoDir, String zygoteForkName, String[] disabledCompatChanges)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(callingPid);
                    _data.writeString(packageName);
                    _data.writeString(processName);
                    _data.writeInt(targetSdk);
                    _data.writeInt(createdPid);
                    _data.writeIntArray(pids);
                    _data.writeInt(uid);
                    _data.writeInt(debugFlags);
                    _data.writeInt(mountExternal);
                    _data.writeString(seInfo);
                    _data.writeString(appDataDir);
                    _data.writeString(instructionSet);
                    _data.writeString(appInfoDir);
                    _data.writeString(zygoteForkName);
                    _data.writeStringArray(disabledCompatChanges);
                    mRemote.transact(TRANSACTION_preforkStart, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int handlePreForkStartProcess(int callingPid, String packageName,
                    String processName, int targetSdk, int createdPid, int[] pids, int uid,
                    int debugFlags, int mountExternal, String seInfo, String appDataDir,
                    String instructionSet, String appInfoDir, String zygoteForkName,
                    String[] disabledCompatChanges) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(callingPid);
                    _data.writeString(packageName);
                    _data.writeString(processName);
                    _data.writeInt(targetSdk);
                    _data.writeInt(createdPid);
                    _data.writeIntArray(pids);
                    _data.writeInt(uid);
                    _data.writeInt(debugFlags);
                    _data.writeInt(mountExternal);
                    _data.writeString(seInfo);
                    _data.writeString(appDataDir);
                    _data.writeString(instructionSet);
                    _data.writeString(appInfoDir);
                    _data.writeString(zygoteForkName);
                    _data.writeStringArray(disabledCompatChanges);
                    mRemote.transact(TRANSACTION_handlePreForkStartProcess, _data, _reply, 0);
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