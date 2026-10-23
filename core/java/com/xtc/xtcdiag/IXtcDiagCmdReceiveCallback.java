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
 * Callback for XTC diag command reception.
 *
 * @hide
 */
public interface IXtcDiagCmdReceiveCallback extends IInterface {
    /** Binder transaction id for {@link #onCmdReceive}. */
    int TRANSACTION_onCmdReceive = IBinder.FIRST_CALL_TRANSACTION;

    void onCmdReceive(int cmd, String receiver, byte[] data) throws RemoteException;

    abstract class Stub extends Binder implements IXtcDiagCmdReceiveCallback {
        private static final String DESCRIPTOR = "com.xtc.xtcdiag.IXtcDiagCmdReceiveCallback";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IXtcDiagCmdReceiveCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IXtcDiagCmdReceiveCallback) {
                return (IXtcDiagCmdReceiveCallback) iin;
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
                case TRANSACTION_onCmdReceive: {
                    data.enforceInterface(DESCRIPTOR);
                    onCmdReceive(data.readInt(), data.readString(), data.createByteArray());
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IXtcDiagCmdReceiveCallback {
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
            public void onCmdReceive(int cmd, String receiver, byte[] data) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(cmd);
                    _data.writeString(receiver);
                    _data.writeByteArray(data);
                    mRemote.transact(TRANSACTION_onCmdReceive, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}