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

package android.app.AntiAddiction;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Interface to the anti-addiction manager service.
 *
 * @hide
 */
public interface IAntiAddictionManager extends IInterface {
    /** Binder transaction ids for {@link IAntiAddictionManager}. */
    int TRANSACTION_statisticalAudioDuration = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_registerAddictionCallback = IBinder.FIRST_CALL_TRANSACTION + 1;
    int TRANSACTION_unregisterAddictionCallback = IBinder.FIRST_CALL_TRANSACTION + 2;

    void statisticalAudioDuration(int startTime, int duration) throws RemoteException;

    void registerAddictionCallback(IAddictionCallback callback) throws RemoteException;

    void unregisterAddictionCallback(IAddictionCallback callback) throws RemoteException;

    abstract class Stub extends Binder implements IAntiAddictionManager {
        private static final String DESCRIPTOR = "android.app.AntiAddiction.IAntiAddictionManager";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAntiAddictionManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IAntiAddictionManager) {
                return (IAntiAddictionManager) iin;
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
                case TRANSACTION_statisticalAudioDuration: {
                    data.enforceInterface(DESCRIPTOR);
                    statisticalAudioDuration(data.readInt(), data.readInt());
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_registerAddictionCallback: {
                    data.enforceInterface(DESCRIPTOR);
                    registerAddictionCallback(
                            IAddictionCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregisterAddictionCallback: {
                    data.enforceInterface(DESCRIPTOR);
                    unregisterAddictionCallback(
                            IAddictionCallback.Stub.asInterface(data.readStrongBinder()));
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IAntiAddictionManager {
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
            public void statisticalAudioDuration(int startTime, int duration)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeInt(startTime);
                    _data.writeInt(duration);
                    mRemote.transact(TRANSACTION_statisticalAudioDuration, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerAddictionCallback(IAddictionCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_registerAddictionCallback, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterAddictionCallback(IAddictionCallback callback)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    mRemote.transact(TRANSACTION_unregisterAddictionCallback, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}