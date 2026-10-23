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
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * Listener for XTC permission checks and permission state changes.
 *
 * @hide
 */
public interface IPermissionListener extends IInterface {
    /** Binder transaction ids for {@link IPermissionListener}. */
    int TRANSACTION_onPermissionCheck = IBinder.FIRST_CALL_TRANSACTION;
    int TRANSACTION_onPermissionChange = IBinder.FIRST_CALL_TRANSACTION + 1;

    boolean onPermissionCheck(PermissionRecord permission, int uid, int pid, Bundle extra)
            throws RemoteException;

    void onPermissionChange(PermissionRecord permission) throws RemoteException;

    abstract class Stub extends Binder implements IPermissionListener {
        private static final String DESCRIPTOR = "com.xtc.auth.IPermissionListener";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPermissionListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IPermissionListener) {
                return (IPermissionListener) iin;
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
                case TRANSACTION_onPermissionCheck: {
                    data.enforceInterface(DESCRIPTOR);
                    PermissionRecord _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = PermissionRecord.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int _arg1 = data.readInt();
                    int _arg2 = data.readInt();
                    Bundle _arg3;
                    if (data.readInt() != 0) {
                        _arg3 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    boolean _result = onPermissionCheck(_arg0, _arg1, _arg2, _arg3);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                }
                case TRANSACTION_onPermissionChange: {
                    data.enforceInterface(DESCRIPTOR);
                    PermissionRecord _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = PermissionRecord.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    onPermissionChange(_arg0);
                    reply.writeNoException();
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IPermissionListener {
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
            public boolean onPermissionCheck(PermissionRecord permission, int uid, int pid,
                    Bundle extra) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                boolean _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (permission != null) {
                        _data.writeInt(1);
                        permission.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(uid);
                    _data.writeInt(pid);
                    if (extra != null) {
                        _data.writeInt(1);
                        extra.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_onPermissionCheck, _data, _reply, 0);
                    _reply.readException();
                    _result = _reply.readInt() != 0;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }

            @Override
            public void onPermissionChange(PermissionRecord permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (permission != null) {
                        _data.writeInt(1);
                        permission.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_onPermissionChange, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}