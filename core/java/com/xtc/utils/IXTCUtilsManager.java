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

package com.xtc.utils;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Interface to the XTC utils service (mobile data, display density, battery
 * test, keep-awake and Bluetooth auto-close controls).
 *
 * @hide
 */
public interface IXTCUtilsManager extends IInterface {
    /** Binder transaction ids for {@link IXTCUtilsManager}. */
    int TRANSACTION_getMobileDataEnabled = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_setMobileDataEnabled = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_isBatteryCurrentTestEnable = IBinder.FIRST_CALL_TRANSACTION + 2;
    int TRANSACTION_isBatteryCurrentTestEnableWithLock = IBinder.FIRST_CALL_TRANSACTION + 3;
    int TRANSACTION_enableBatteryCurrentTestEnable = IBinder.FIRST_CALL_TRANSACTION + 4;
    int TRANSACTION_disableBatteryCurrentTestEnable = IBinder.FIRST_CALL_TRANSACTION + 5;
    int TRANSACTION_getBatteryCurrentTestRecord = IBinder.FIRST_CALL_TRANSACTION + 6;
    int TRANSACTION_isKeepSystemAwake = IBinder.FIRST_CALL_TRANSACTION + 7;
    int TRANSACTION_setKeepSystemAwake = IBinder.FIRST_CALL_TRANSACTION + 8;
    int TRANSACTION_isSupportChangeDisplayDensity = IBinder.FIRST_CALL_TRANSACTION + 9;
    int TRANSACTION_setDisplayDensity = IBinder.FIRST_CALL_TRANSACTION + 10;
    int TRANSACTION_clearDisplayDensity = IBinder.FIRST_CALL_TRANSACTION + 11;
    int TRANSACTION_isRoundedCornerScreen = IBinder.FIRST_CALL_TRANSACTION + 12;
    int TRANSACTION_getBluetoothAutoClosePolicy = IBinder.FIRST_CALL_TRANSACTION + 13;
    int TRANSACTION_setBluetoothAutoClosePolicy = IBinder.FIRST_CALL_TRANSACTION + 14;
    int TRANSACTION_getBluetoothAutoCloseTime = IBinder.FIRST_CALL_TRANSACTION + 15;
    int TRANSACTION_setBluetoothAutoCloseTime = IBinder.FIRST_CALL_TRANSACTION + 16;
    int TRANSACTION_isSecurityKeyManagerSupported = IBinder.FIRST_CALL_TRANSACTION + 17;
    int TRANSACTION_getSecurityKey = IBinder.FIRST_CALL_TRANSACTION + 18;
    int TRANSACTION_setSecurityKey = IBinder.FIRST_CALL_TRANSACTION + 19;
    int TRANSACTION_checkVersionDowngrade = IBinder.FIRST_CALL_TRANSACTION + 20;

    boolean getMobileDataEnabled() throws RemoteException;

    void setMobileDataEnabled(boolean enable) throws RemoteException;

    boolean isBatteryCurrentTestEnable() throws RemoteException;

    boolean isBatteryCurrentTestEnableWithLock() throws RemoteException;

    void enableBatteryCurrentTestEnable(boolean enable) throws RemoteException;

    void disableBatteryCurrentTestEnable() throws RemoteException;

    String getBatteryCurrentTestRecord() throws RemoteException;

    boolean isKeepSystemAwake() throws RemoteException;

    void setKeepSystemAwake(boolean keepAwake) throws RemoteException;

    boolean isSupportChangeDisplayDensity() throws RemoteException;

    void setDisplayDensity(int density, boolean isDefault) throws RemoteException;

    void clearDisplayDensity() throws RemoteException;

    boolean isRoundedCornerScreen() throws RemoteException;

    int getBluetoothAutoClosePolicy() throws RemoteException;

    void setBluetoothAutoClosePolicy(int policy) throws RemoteException;

    long getBluetoothAutoCloseTime() throws RemoteException;

    void setBluetoothAutoCloseTime(long time) throws RemoteException;

    boolean isSecurityKeyManagerSupported() throws RemoteException;

    String getSecurityKey(int type, String name) throws RemoteException;

    boolean setSecurityKey(String key) throws RemoteException;

    int checkVersionDowngrade(String version) throws RemoteException;

    abstract class Stub extends Binder implements IXTCUtilsManager {
        private static final String DESCRIPTOR = "com.xtc.utils.IXTCUtilsManager";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IXTCUtilsManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IXTCUtilsManager) {
                return (IXTCUtilsManager) iin;
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
                case TRANSACTION_getMobileDataEnabled: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = getMobileDataEnabled();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setMobileDataEnabled: {
                    data.enforceInterface(DESCRIPTOR);
                    setMobileDataEnabled(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isBatteryCurrentTestEnable: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isBatteryCurrentTestEnable();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_isBatteryCurrentTestEnableWithLock: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isBatteryCurrentTestEnableWithLock();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_enableBatteryCurrentTestEnable: {
                    data.enforceInterface(DESCRIPTOR);
                    enableBatteryCurrentTestEnable(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_disableBatteryCurrentTestEnable: {
                    data.enforceInterface(DESCRIPTOR);
                    disableBatteryCurrentTestEnable();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getBatteryCurrentTestRecord: {
                    data.enforceInterface(DESCRIPTOR);
                    String _result = getBatteryCurrentTestRecord();
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_isKeepSystemAwake: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isKeepSystemAwake();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setKeepSystemAwake: {
                    data.enforceInterface(DESCRIPTOR);
                    setKeepSystemAwake(data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isSupportChangeDisplayDensity: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isSupportChangeDisplayDensity();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_setDisplayDensity: {
                    data.enforceInterface(DESCRIPTOR);
                    setDisplayDensity(data.readInt(), data.readInt() != 0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_clearDisplayDensity: {
                    data.enforceInterface(DESCRIPTOR);
                    clearDisplayDensity();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isRoundedCornerScreen: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isRoundedCornerScreen();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getBluetoothAutoClosePolicy: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = getBluetoothAutoClosePolicy();
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
                case TRANSACTION_setBluetoothAutoClosePolicy: {
                    data.enforceInterface(DESCRIPTOR);
                    setBluetoothAutoClosePolicy(data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_getBluetoothAutoCloseTime: {
                    data.enforceInterface(DESCRIPTOR);
                    long _result = getBluetoothAutoCloseTime();
                    reply.writeNoException();
                    reply.writeLong(_result);
                    return true;
                }
                case TRANSACTION_setBluetoothAutoCloseTime: {
                    data.enforceInterface(DESCRIPTOR);
                    setBluetoothAutoCloseTime(data.readLong());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isSecurityKeyManagerSupported: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isSecurityKeyManagerSupported();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_getSecurityKey: {
                    data.enforceInterface(DESCRIPTOR);
                    String _result = getSecurityKey(data.readInt(), data.readString());
                    reply.writeNoException();
                    reply.writeString(_result);
                    return true;
                }
                case TRANSACTION_setSecurityKey: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = setSecurityKey(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_checkVersionDowngrade: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = checkVersionDowngrade(data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IXTCUtilsManager {
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
            public boolean getMobileDataEnabled() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getMobileDataEnabled, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setMobileDataEnabled(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_setMobileDataEnabled, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isBatteryCurrentTestEnable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isBatteryCurrentTestEnable, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean isBatteryCurrentTestEnableWithLock() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(
                            TRANSACTION_isBatteryCurrentTestEnableWithLock, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void enableBatteryCurrentTestEnable(boolean enable) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(enable ? 1 : 0);
                    mRemote.transact(TRANSACTION_enableBatteryCurrentTestEnable, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void disableBatteryCurrentTestEnable() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_disableBatteryCurrentTestEnable, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public String getBatteryCurrentTestRecord() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getBatteryCurrentTestRecord, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean isKeepSystemAwake() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isKeepSystemAwake, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setKeepSystemAwake(boolean keepAwake) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(keepAwake ? 1 : 0);
                    mRemote.transact(TRANSACTION_setKeepSystemAwake, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSupportChangeDisplayDensity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isSupportChangeDisplayDensity, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setDisplayDensity(int density, boolean isDefault) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(density);
                    _data.writeInt(isDefault ? 1 : 0);
                    mRemote.transact(TRANSACTION_setDisplayDensity, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void clearDisplayDensity() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_clearDisplayDensity, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isRoundedCornerScreen() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isRoundedCornerScreen, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int getBluetoothAutoClosePolicy() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getBluetoothAutoClosePolicy, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setBluetoothAutoClosePolicy(int policy) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(policy);
                    mRemote.transact(TRANSACTION_setBluetoothAutoClosePolicy, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public long getBluetoothAutoCloseTime() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                long _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_getBluetoothAutoCloseTime, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readLong();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void setBluetoothAutoCloseTime(long time) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeLong(time);
                    mRemote.transact(TRANSACTION_setBluetoothAutoCloseTime, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSecurityKeyManagerSupported() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isSecurityKeyManagerSupported, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public String getSecurityKey(int type, String name) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                String _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(type);
                    _data.writeString(name);
                    mRemote.transact(TRANSACTION_getSecurityKey, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readString();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public boolean setSecurityKey(String key) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(key);
                    mRemote.transact(TRANSACTION_setSecurityKey, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public int checkVersionDowngrade(String version) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(version);
                    mRemote.transact(TRANSACTION_checkVersionDowngrade, _data, _reply, 0);
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