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

package com.xtc.health;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Interface to the XTC health service (MCU packet exchange).
 *
 * @hide
 */
public interface IXtcHealthManager extends IInterface {
    /** Binder transaction ids for {@link IXtcHealthManager}. */
    int TRANSACTION_sendDataMcu = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_registerHealthCallback = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_unregisterHealthCallback = IBinder.FIRST_CALL_TRANSACTION + 2;

    void registerHealthCallback(IXtcHealthCallback callback) throws RemoteException;

    void unregisterHealthCallback(IXtcHealthCallback callback) throws RemoteException;

    /**
     * Sends data to the MCU. The {@code data} buffer is an in/out parameter and
     * may be filled with the response from the MCU.
     */
    int sendDataMcu(String mcuCmd, byte[] data) throws RemoteException;

    abstract class Stub extends Binder implements IXtcHealthManager {
        private static final String DESCRIPTOR = "com.xtc.health.IXtcHealthManager";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IXtcHealthManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IXtcHealthManager) {
                return (IXtcHealthManager) iin;
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
                case TRANSACTION_sendDataMcu: {
                    data.enforceInterface(DESCRIPTOR);
                    String _arg0;
                    _arg0 = data.readString();
                    byte[] _arg1;
                    _arg1 = data.createByteArray();
                    int _result = sendDataMcu(_arg0, _arg1);
                    reply.writeNoException();
                    reply.writeInt(_result);
                    reply.writeByteArray(_arg1);
                    return true;
                }
                case TRANSACTION_registerHealthCallback: {
                    data.enforceInterface(DESCRIPTOR);
                    registerHealthCallback(
                            IXtcHealthCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregisterHealthCallback: {
                    data.enforceInterface(DESCRIPTOR);
                    unregisterHealthCallback(
                            IXtcHealthCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IXtcHealthManager {
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
            public void registerHealthCallback(IXtcHealthCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerHealthCallback, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterHealthCallback(IXtcHealthCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_unregisterHealthCallback, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int sendDataMcu(String mcuCmd, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                int _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(mcuCmd);
                    _data.writeByteArray(data);
                    mRemote.transact(TRANSACTION_sendDataMcu, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt();
                    _reply.readByteArray(data);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
        }
    }
}