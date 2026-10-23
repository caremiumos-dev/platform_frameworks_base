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

package com.xtc.xtcdiag;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Interface to the XTC diag (socket diag) service.
 *
 * @hide
 */
public interface IXtcDiag extends IInterface {
    /** Binder transaction ids for {@link IXtcDiag}. */
    int TRANSACTION_startSocketServer = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_stopSocketServer = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_startSocketClient = IBinder.FIRST_CALL_TRANSACTION + 2;
    int TRANSACTION_stopSocketClient = IBinder.FIRST_CALL_TRANSACTION + 3;
    int TRANSACTION_isSocketConnected = IBinder.FIRST_CALL_TRANSACTION + 4;
    int TRANSACTION_send = IBinder.FIRST_CALL_TRANSACTION + 5;
    int TRANSACTION_sendFile = IBinder.FIRST_CALL_TRANSACTION + 6;
    int TRANSACTION_registerConnStateCallBack = IBinder.FIRST_CALL_TRANSACTION + 7;
    int TRANSACTION_unregisterConnStateCallBack = IBinder.FIRST_CALL_TRANSACTION + 8;
    int TRANSACTION_registerCmdReceiveCallBack = IBinder.FIRST_CALL_TRANSACTION + 9;
    int TRANSACTION_unregisterCmdReceiveCallBack = IBinder.FIRST_CALL_TRANSACTION + 10;

    void startSocketServer() throws RemoteException;

    void stopSocketServer() throws RemoteException;

    void startSocketClient(String serverAddr) throws RemoteException;

    void stopSocketClient() throws RemoteException;

    boolean isSocketConnected() throws RemoteException;

    void send(int cmd, String receiver, byte[] data) throws RemoteException;

    void sendFile(String path, String fileName, String receiver, int cmd) throws RemoteException;

    void registerConnStateCallBack(IXtcDiagConnStateCallback callback) throws RemoteException;

    void unregisterConnStateCallBack(IXtcDiagConnStateCallback callback) throws RemoteException;

    void registerCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback) throws RemoteException;

    void unregisterCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback) throws RemoteException;

    abstract class Stub extends Binder implements IXtcDiag {
        private static final String DESCRIPTOR = "com.xtc.xtcdiag.IXtcDiag";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IXtcDiag asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IXtcDiag) {
                return (IXtcDiag) iin;
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
                case TRANSACTION_startSocketServer: {
                    data.enforceInterface(DESCRIPTOR);
                    startSocketServer();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stopSocketServer: {
                    data.enforceInterface(DESCRIPTOR);
                    stopSocketServer();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_startSocketClient: {
                    data.enforceInterface(DESCRIPTOR);
                    startSocketClient(data.readString());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_stopSocketClient: {
                    data.enforceInterface(DESCRIPTOR);
                    stopSocketClient();
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_isSocketConnected: {
                    data.enforceInterface(DESCRIPTOR);
                    boolean _result = isSocketConnected();
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_send: {
                    data.enforceInterface(DESCRIPTOR);
                    send(data.readInt(), data.readString(), data.createByteArray());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_sendFile: {
                    data.enforceInterface(DESCRIPTOR);
                    sendFile(data.readString(), data.readString(), data.readString(), data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_registerConnStateCallBack: {
                    data.enforceInterface(DESCRIPTOR);
                    registerConnStateCallBack(
                            IXtcDiagConnStateCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregisterConnStateCallBack: {
                    data.enforceInterface(DESCRIPTOR);
                    unregisterConnStateCallBack(
                            IXtcDiagConnStateCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_registerCmdReceiveCallBack: {
                    data.enforceInterface(DESCRIPTOR);
                    registerCmdReceiveCallBack(
                            IXtcDiagCmdReceiveCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregisterCmdReceiveCallBack: {
                    data.enforceInterface(DESCRIPTOR);
                    unregisterCmdReceiveCallBack(
                            IXtcDiagCmdReceiveCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IXtcDiag {
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
            public void startSocketServer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_startSocketServer, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopSocketServer() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_stopSocketServer, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void startSocketClient(String serverAddr) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(serverAddr);
                    mRemote.transact(TRANSACTION_startSocketClient, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopSocketClient() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_stopSocketClient, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSocketConnected() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    mRemote.transact(TRANSACTION_isSocketConnected, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void send(int cmd, String receiver, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(cmd);
                    _data.writeString(receiver);
                    _data.writeByteArray(data);
                    mRemote.transact(TRANSACTION_send, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void sendFile(String path, String fileName, String receiver, int cmd)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(path);
                    _data.writeString(fileName);
                    _data.writeString(receiver);
                    _data.writeInt(cmd);
                    mRemote.transact(TRANSACTION_sendFile, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerConnStateCallBack(IXtcDiagConnStateCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerConnStateCallBack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterConnStateCallBack(IXtcDiagConnStateCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_unregisterConnStateCallBack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerCmdReceiveCallBack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_unregisterCmdReceiveCallBack, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}