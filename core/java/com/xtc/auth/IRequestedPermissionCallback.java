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

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Callback for asynchronous XTC permission check results.
 *
 * @hide
 */
public interface IRequestedPermissionCallback extends IInterface {
    /** Binder transaction id for {@link #onPermissionCheckResult}. */
    int TRANSACTION_onPermissionCheckResult = IBinder.FIRST_CALL_TRANSACTION;

    void onPermissionCheckResult(String permission, int callerUid, int result)
            throws RemoteException;

    abstract class Stub extends Binder implements IRequestedPermissionCallback {
        private static final String DESCRIPTOR = "com.xtc.auth.IRequestedPermissionCallback";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IRequestedPermissionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IRequestedPermissionCallback) {
                return (IRequestedPermissionCallback) iin;
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
                case TRANSACTION_onPermissionCheckResult: {
                    data.enforceInterface(DESCRIPTOR);
                    onPermissionCheckResult(data.readString(), data.readInt(), data.readInt());
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IRequestedPermissionCallback {
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
            public void onPermissionCheckResult(String permission, int callerUid, int result)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(permission);
                    _data.writeInt(callerUid);
                    _data.writeInt(result);
                    mRemote.transact(TRANSACTION_onPermissionCheckResult, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}