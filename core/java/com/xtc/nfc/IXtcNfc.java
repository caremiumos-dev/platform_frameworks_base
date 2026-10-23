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

package com.xtc.nfc;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Interface to the XTC NFC secure element service.
 *
 * @hide
 */
public interface IXtcNfc extends IInterface {
    /** Binder transaction ids for {@link IXtcNfc}. */
    int TRANSACTION_configRoute = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_selectSE = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_setHceUid = IBinder.FIRST_CALL_TRANSACTION + 2;
    int TRANSACTION_startRfDiscovery = IBinder.FIRST_CALL_TRANSACTION + 3;
    int TRANSACTION_setDiscoveryTech = IBinder.FIRST_CALL_TRANSACTION + 4;
    int TRANSACTION_setConfig = IBinder.FIRST_CALL_TRANSACTION + 5;

    void configRoute(String route) throws RemoteException;

    void selectSE(String seId, String aid) throws RemoteException;

    boolean setHceUid(byte[] uid) throws RemoteException;

    void startRfDiscovery(boolean on, int tech) throws RemoteException;

    void setDiscoveryTech(String tech) throws RemoteException;

    int setConfig(String key, String value) throws RemoteException;

    abstract class Stub extends Binder implements IXtcNfc {
        private static final String DESCRIPTOR = "com.xtc.nfc.IXtcNfc";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IXtcNfc asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IXtcNfc) {
                return (IXtcNfc) iin;
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
                case TRANSACTION_configRoute: {
                    data.enforceInterface(DESCRIPTOR);
                    configRoute(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_selectSE: {
                    data.enforceInterface(DESCRIPTOR);
                    selectSE(data.readString(), data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setHceUid: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = setHceUid(data.createByteArray());
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_startRfDiscovery: {
                    data.enforceInterface(DESCRIPTOR);
                    startRfDiscovery(data.readInt() != 0, data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setDiscoveryTech: {
                    data.enforceInterface(DESCRIPTOR);
                    setDiscoveryTech(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_setConfig: {
                    data.enforceInterface(DESCRIPTOR);
                    int _result = setConfig(data.readString(), data.readString());
                    reply.writeNoException();
                    reply.writeInt(_result);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IXtcNfc {
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
            public void configRoute(String route) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(route);
                    mRemote.transact(TRANSACTION_configRoute, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void selectSE(String seId, String aid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(seId);
                    _data.writeString(aid);
                    mRemote.transact(TRANSACTION_selectSE, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean setHceUid(byte[] uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeByteArray(uid);
                    mRemote.transact(TRANSACTION_setHceUid, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void startRfDiscovery(boolean on, int tech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(on ? 1 : 0);
                    _data.writeInt(tech);
                    mRemote.transact(TRANSACTION_startRfDiscovery, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setDiscoveryTech(String tech) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(tech);
                    mRemote.transact(TRANSACTION_setDiscoveryTech, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int setConfig(String key, String value) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(key);
                    _data.writeString(value);
                    mRemote.transact(TRANSACTION_setConfig, _data, _reply, 0);
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