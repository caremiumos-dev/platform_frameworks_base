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

package com.xtc.auth;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

import java.util.List;

/**
 * Binder interface of the XTC mobile manager (MoMS) service.
 *
 * @hide
 */
public interface IMobileManagerService extends IInterface {
    /** Binder transaction ids for {@link IMobileManagerService}. */
    int TRANSACTION_systemReady = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_getVersionName = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_attach = IBinder.FIRST_CALL_TRANSACTION + 2;
    int TRANSACTION_detach = IBinder.FIRST_CALL_TRANSACTION + 3;
    int TRANSACTION_clearAllSettings = IBinder.FIRST_CALL_TRANSACTION + 4;
    int TRANSACTION_clearPackageSettings = IBinder.FIRST_CALL_TRANSACTION + 5;
    int TRANSACTION_registerManagerApListener = IBinder.FIRST_CALL_TRANSACTION + 6;
    int TRANSACTION_triggerManagerApListener = IBinder.FIRST_CALL_TRANSACTION + 7;
    int TRANSACTION_triggerManagerApListenerAsync = IBinder.FIRST_CALL_TRANSACTION + 8;
    int TRANSACTION_enablePermissionController = IBinder.FIRST_CALL_TRANSACTION + 9;
    int TRANSACTION_getInstalledPackages = IBinder.FIRST_CALL_TRANSACTION + 10;
    int TRANSACTION_getPackageGrantedPermissions = IBinder.FIRST_CALL_TRANSACTION + 11;
    int TRANSACTION_setPermissionRecord = IBinder.FIRST_CALL_TRANSACTION + 12;
    int TRANSACTION_setPermissionRecords = IBinder.FIRST_CALL_TRANSACTION + 13;
    int TRANSACTION_setPermissionCache = IBinder.FIRST_CALL_TRANSACTION + 14;
    int TRANSACTION_checkPermission = IBinder.FIRST_CALL_TRANSACTION + 15;
    int TRANSACTION_checkPermissionAsync = IBinder.FIRST_CALL_TRANSACTION + 16;
    int TRANSACTION_checkPermissionWithData = IBinder.FIRST_CALL_TRANSACTION + 17;
    int TRANSACTION_checkPermissionAsyncWithData = IBinder.FIRST_CALL_TRANSACTION + 18;
    int TRANSACTION_getParentPermission = IBinder.FIRST_CALL_TRANSACTION + 19;
    int TRANSACTION_getUserConfirmTime = IBinder.FIRST_CALL_TRANSACTION + 20;
    int TRANSACTION_getBootReceiverList = IBinder.FIRST_CALL_TRANSACTION + 21;
    int TRANSACTION_setBootReceiverEnabledSettings = IBinder.FIRST_CALL_TRANSACTION + 22;
    int TRANSACTION_setBootReceiverEnabledSetting = IBinder.FIRST_CALL_TRANSACTION + 23;
    int TRANSACTION_getBootReceiverEnabledSetting = IBinder.FIRST_CALL_TRANSACTION + 24;
    int TRANSACTION_filterReceiver = IBinder.FIRST_CALL_TRANSACTION + 25;
    int TRANSACTION_startMonitorBootReceiver = IBinder.FIRST_CALL_TRANSACTION + 26;
    int TRANSACTION_stopMonitorBootReceiver = IBinder.FIRST_CALL_TRANSACTION + 27;
    int TRANSACTION_forceStopPackage = IBinder.FIRST_CALL_TRANSACTION + 28;
    int TRANSACTION_installPackage = IBinder.FIRST_CALL_TRANSACTION + 29;
    int TRANSACTION_deletePackage = IBinder.FIRST_CALL_TRANSACTION + 30;
    int TRANSACTION_cancelNotification = IBinder.FIRST_CALL_TRANSACTION + 31;
    int TRANSACTION_setNotificationEnabledSetting = IBinder.FIRST_CALL_TRANSACTION + 32;
    int TRANSACTION_getNotificationEnabledSetting = IBinder.FIRST_CALL_TRANSACTION + 33;
    int TRANSACTION_setNotificationCache = IBinder.FIRST_CALL_TRANSACTION + 34;
    int TRANSACTION_enableInterceptionController = IBinder.FIRST_CALL_TRANSACTION + 35;
    int TRANSACTION_getInterceptionEnabledSetting = IBinder.FIRST_CALL_TRANSACTION + 36;
    int TRANSACTION_setFirewallPolicy = IBinder.FIRST_CALL_TRANSACTION + 37;
    int TRANSACTION_killPid = IBinder.FIRST_CALL_TRANSACTION + 38;

    void systemReady() throws RemoteException;

    String getVersionName() throws RemoteException;

    boolean attach(IMobileConnectionCallback callback) throws RemoteException;

    void detach() throws RemoteException;

    void clearAllSettings() throws RemoteException;

    void clearPackageSettings(String packageName) throws RemoteException;

    void registerManagerApListener(int uid, IBinder listener) throws RemoteException;

    int triggerManagerApListener(int type, Bundle data, int timeout) throws RemoteException;

    void triggerManagerApListenerAsync(int type, Bundle data, int timeout, IBinder listener)
            throws RemoteException;

    void enablePermissionController(boolean enable) throws RemoteException;

    List<PackageInfo> getInstalledPackages() throws RemoteException;

    List<Permission> getPackageGrantedPermissions(String packageName) throws RemoteException;

    void setPermissionRecord(PermissionRecord record) throws RemoteException;

    void setPermissionRecords(List<PermissionRecord> records) throws RemoteException;

    void setPermissionCache(List<PermissionRecord> records) throws RemoteException;

    int checkPermission(String permission, int uid) throws RemoteException;

    void checkPermissionAsync(String permission, int uid,
            IRequestedPermissionCallback callback) throws RemoteException;

    int checkPermissionWithData(String permission, int uid, Bundle data)
            throws RemoteException;

    void checkPermissionAsyncWithData(String permission, int uid, Bundle data,
            IRequestedPermissionCallback callback) throws RemoteException;

    String getParentPermission(String subPermission) throws RemoteException;

    long getUserConfirmTime(int uid, long timeout) throws RemoteException;

    List<ReceiverRecord> getBootReceiverList() throws RemoteException;

    void setBootReceiverEnabledSettings(List<ReceiverRecord> records) throws RemoteException;

    void setBootReceiverEnabledSetting(String packageName, boolean enable)
            throws RemoteException;

    boolean getBootReceiverEnabledSetting(String packageName) throws RemoteException;

    void filterReceiver(Intent intent, List<ResolveInfo> receivers, int allowedUid)
            throws RemoteException;

    void startMonitorBootReceiver(String packageName) throws RemoteException;

    void stopMonitorBootReceiver(String packageName) throws RemoteException;

    void forceStopPackage(String packageName) throws RemoteException;

    void installPackage(Uri packageUri, IPackageInstallCallback callback)
            throws RemoteException;

    void deletePackage(String packageName) throws RemoteException;

    void cancelNotification(String packageName) throws RemoteException;

    void setNotificationEnabledSetting(String packageName, boolean enable)
            throws RemoteException;

    boolean getNotificationEnabledSetting(String packageName) throws RemoteException;

    void setNotificationCache(List<NotificationCacheRecord> records) throws RemoteException;

    void enableInterceptionController(boolean enable) throws RemoteException;

    boolean getInterceptionEnabledSetting() throws RemoteException;

    void setFirewallPolicy(int uid, int policy, boolean enable) throws RemoteException;

    void killPid(int pid, String reason) throws RemoteException;

    abstract class Stub extends Binder implements IMobileManagerService {
        private static final String DESCRIPTOR = "com.xtc.auth.IMobileManagerService";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMobileManagerService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IMobileManagerService) {
                return (IMobileManagerService) iin;
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
                case TRANSACTION_systemReady: {
                    data.enforceInterface(DESCRIPTOR);
                    systemReady();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getVersionName: {
                    data.enforceInterface(DESCRIPTOR);
                    String _result = getVersionName();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_attach: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = attach(
                            IMobileConnectionCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_detach: {
                    data.enforceInterface(DESCRIPTOR);
                    detach();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_clearAllSettings: {
                    data.enforceInterface(DESCRIPTOR);
                    clearAllSettings();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_clearPackageSettings: {
                    data.enforceInterface(DESCRIPTOR);
                    clearPackageSettings(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_registerManagerApListener: {
                    data.enforceInterface(DESCRIPTOR);
                    registerManagerApListener(data.readInt(), data.readStrongBinder());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_triggerManagerApListener: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = triggerManagerApListener(data.readInt(), _bundle(data),
                            data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_triggerManagerApListenerAsync: {
                    data.enforceInterface(DESCRIPTOR);
                    triggerManagerApListenerAsync(data.readInt(), _bundle(data), data.readInt(),
                            data.readStrongBinder());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_enablePermissionController: {
                    data.enforceInterface(DESCRIPTOR);
                    enablePermissionController(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getInstalledPackages: {
                    data.enforceInterface(DESCRIPTOR);
                    List<PackageInfo> _result = getInstalledPackages();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_getPackageGrantedPermissions: {
                    data.enforceInterface(DESCRIPTOR);
                    List<Permission> _result = getPackageGrantedPermissions(data.readString());
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_setPermissionRecord: {
                    data.enforceInterface(DESCRIPTOR);
                    setPermissionRecord(_permissionRecord(data));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setPermissionRecords: {
                    data.enforceInterface(DESCRIPTOR);
                    setPermissionRecords(data.createTypedArrayList(PermissionRecord.CREATOR));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setPermissionCache: {
                    data.enforceInterface(DESCRIPTOR);
                    setPermissionCache(data.createTypedArrayList(PermissionRecord.CREATOR));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_checkPermission: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = checkPermission(data.readString(), data.readInt());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_checkPermissionAsync: {
                    data.enforceInterface(DESCRIPTOR);
                    checkPermissionAsync(data.readString(), data.readInt(),
                            IRequestedPermissionCallback.Stub.asInterface(
                                    data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_checkPermissionWithData: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = checkPermissionWithData(data.readString(), data.readInt(),
                            _bundle(data));
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_checkPermissionAsyncWithData: {
                    data.enforceInterface(DESCRIPTOR);
                    checkPermissionAsyncWithData(data.readString(), data.readInt(), _bundle(data),
                            IRequestedPermissionCallback.Stub.asInterface(
                                    data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getParentPermission: {
                    data.enforceInterface(DESCRIPTOR);
                    String _result = getParentPermission(data.readString());
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_getUserConfirmTime: {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = getUserConfirmTime(data.readInt(), data.readLong());
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_getBootReceiverList: {
                    data.enforceInterface(DESCRIPTOR);
                    List<ReceiverRecord> _result = getBootReceiverList();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_setBootReceiverEnabledSettings: {
                    data.enforceInterface(DESCRIPTOR);
                    setBootReceiverEnabledSettings(
                            data.createTypedArrayList(ReceiverRecord.CREATOR));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setBootReceiverEnabledSetting: {
                    data.enforceInterface(DESCRIPTOR);
                    setBootReceiverEnabledSetting(data.readString(), data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getBootReceiverEnabledSetting: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = getBootReceiverEnabledSetting(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_filterReceiver: {
                    data.enforceInterface(DESCRIPTOR);
                    Intent _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    List<ResolveInfo> _arg1 = data.createTypedArrayList(ResolveInfo.CREATOR);
                    int _arg2 = data.readInt();
                    filterReceiver(_arg0, _arg1, _arg2);
                    reply.writeNoException();
                    reply.writeTypedList(_arg1);
                    return true;
                }
                case TRANSACTION_startMonitorBootReceiver: {
                    data.enforceInterface(DESCRIPTOR);
                    startMonitorBootReceiver(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stopMonitorBootReceiver: {
                    data.enforceInterface(DESCRIPTOR);
                    stopMonitorBootReceiver(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_forceStopPackage: {
                    data.enforceInterface(DESCRIPTOR);
                    forceStopPackage(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_installPackage: {
                    data.enforceInterface(DESCRIPTOR);
                    Uri _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = Uri.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    installPackage(_arg0, IPackageInstallCallback.Stub.asInterface(
                            data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_deletePackage: {
                    data.enforceInterface(DESCRIPTOR);
                    deletePackage(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_cancelNotification: {
                    data.enforceInterface(DESCRIPTOR);
                    cancelNotification(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setNotificationEnabledSetting: {
                    data.enforceInterface(DESCRIPTOR);
                    setNotificationEnabledSetting(data.readString(), data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getNotificationEnabledSetting: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = getNotificationEnabledSetting(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setNotificationCache: {
                    data.enforceInterface(DESCRIPTOR);
                    setNotificationCache(
                            data.createTypedArrayList(NotificationCacheRecord.CREATOR));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_enableInterceptionController: {
                    data.enforceInterface(DESCRIPTOR);
                    enableInterceptionController(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getInterceptionEnabledSetting: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = getInterceptionEnabledSetting();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setFirewallPolicy: {
                    data.enforceInterface(DESCRIPTOR);
                    setFirewallPolicy(data.readInt(), data.readInt(), data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_killPid: {
                    data.enforceInterface(DESCRIPTOR);
                    killPid(data.readInt(), data.readString());
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static Bundle _bundle(Parcel data) {
            if (data.readInt() != 0) {
                return Bundle.CREATOR.createFromParcel(data);
            }
            return null;
        }

        private static PermissionRecord _permissionRecord(Parcel data) {
            if (data.readInt() != 0) {
                return PermissionRecord.CREATOR.createFromParcel(data);
            }
            return null;
        }

        private static class Proxy implements IMobileManagerService {
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
            public void systemReady() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_systemReady, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getVersionName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getVersionName, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean attach(IMobileConnectionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_attach, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void detach() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_detach, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearAllSettings() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_clearAllSettings, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearPackageSettings(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_clearPackageSettings, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerManagerApListener(int uid, IBinder listener)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(listener);
                    mRemote.transact(TRANSACTION_registerManagerApListener, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int triggerManagerApListener(int type, Bundle data, int timeout)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(type);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(timeout);
                    mRemote.transact(TRANSACTION_triggerManagerApListener, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void triggerManagerApListenerAsync(int type, Bundle data, int timeout,
                    IBinder listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(type);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(timeout);
                    _data.writeStrongBinder(listener);
                    mRemote.transact(TRANSACTION_triggerManagerApListenerAsync, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enablePermissionController(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_enablePermissionController, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<PackageInfo> getInstalledPackages() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                List<PackageInfo> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getInstalledPackages, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createTypedArrayList(PackageInfo.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public List<Permission> getPackageGrantedPermissions(String packageName)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                List<Permission> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_getPackageGrantedPermissions, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createTypedArrayList(Permission.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setPermissionRecord(PermissionRecord record) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (record != null) {
                        _data.writeInt(1);
                        record.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_setPermissionRecord, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPermissionRecords(List<PermissionRecord> records)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedList(records);
                    mRemote.transact(TRANSACTION_setPermissionRecords, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPermissionCache(List<PermissionRecord> records)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedList(records);
                    mRemote.transact(TRANSACTION_setPermissionCache, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkPermission(String permission, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeInt(uid);
                    mRemote.transact(TRANSACTION_checkPermission, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void checkPermissionAsync(String permission, int uid,
                    IRequestedPermissionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeInt(uid);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_checkPermissionAsync, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int checkPermissionWithData(String permission, int uid, Bundle data)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeInt(uid);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_checkPermissionWithData, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void checkPermissionAsyncWithData(String permission, int uid, Bundle data,
                    IRequestedPermissionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeInt(uid);
                    if (data != null) {
                        _data.writeInt(1);
                        data.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_checkPermissionAsyncWithData, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getParentPermission(String subPermission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(subPermission);
                    mRemote.transact(TRANSACTION_getParentPermission, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public long getUserConfirmTime(int uid, long timeout) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeLong(timeout);
                    mRemote.transact(TRANSACTION_getUserConfirmTime, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public List<ReceiverRecord> getBootReceiverList() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                List<ReceiverRecord> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getBootReceiverList, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.createTypedArrayList(ReceiverRecord.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setBootReceiverEnabledSettings(List<ReceiverRecord> records)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedList(records);
                    mRemote.transact(TRANSACTION_setBootReceiverEnabledSettings, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setBootReceiverEnabledSetting(String packageName, boolean enable)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_setBootReceiverEnabledSetting, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getBootReceiverEnabledSetting(String packageName)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_getBootReceiverEnabledSetting, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void filterReceiver(Intent intent, List<ResolveInfo> receivers,
                    int allowedUid) throws RemoteException {
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
                    _data.writeTypedList(receivers);
                    _data.writeInt(allowedUid);
                    mRemote.transact(TRANSACTION_filterReceiver, _data, _reply, 0);
                    _reply.readException();
                    _reply.readTypedList(receivers, ResolveInfo.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startMonitorBootReceiver(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_startMonitorBootReceiver, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopMonitorBootReceiver(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_stopMonitorBootReceiver, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void forceStopPackage(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_forceStopPackage, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void installPackage(Uri packageUri, IPackageInstallCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (packageUri != null) {
                        _data.writeInt(1);
                        packageUri.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_installPackage, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deletePackage(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_deletePackage, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void cancelNotification(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_cancelNotification, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setNotificationEnabledSetting(String packageName, boolean enable)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_setNotificationEnabledSetting, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getNotificationEnabledSetting(String packageName)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(packageName);
                    mRemote.transact(TRANSACTION_getNotificationEnabledSetting, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setNotificationCache(List<NotificationCacheRecord> records)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeTypedList(records);
                    mRemote.transact(TRANSACTION_setNotificationCache, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enableInterceptionController(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_enableInterceptionController, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean getInterceptionEnabledSetting() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getInterceptionEnabledSetting, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setFirewallPolicy(int uid, int policy, boolean enable)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeInt(policy);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_setFirewallPolicy, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void killPid(int pid, String reason) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(pid);
                    _data.writeString(reason);
                    mRemote.transact(TRANSACTION_killPid, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}