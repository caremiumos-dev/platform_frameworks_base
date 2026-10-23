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

package com.android.internal.apm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

/**
 * One-way callback delivering APM configuration changes.
 *
 * @hide
 */
public interface IAppPerformanceMonitorCallback extends IInterface {
    /** Binder transaction id for {@link #onAPMConfigurationChanged}. */
    int TRANSACTION_onAPMConfigurationChanged = IBinder.FIRST_CALL_TRANSACTION;

    void onAPMConfigurationChanged(APMConfiguration config) throws RemoteException;

    abstract class Stub extends Binder implements IAppPerformanceMonitorCallback {
        private static final String DESCRIPTOR = "com.android.internal.apm.IAppPerformanceMonitorCallback";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAppPerformanceMonitorCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IAppPerformanceMonitorCallback) {
                return (IAppPerformanceMonitorCallback) iin;
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
                case TRANSACTION_onAPMConfigurationChanged: {
                    data.enforceInterface(DESCRIPTOR);
                    APMConfiguration _arg0;
                    if (data.readInt() != 0) {
                        _arg0 = APMConfiguration.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    onAPMConfigurationChanged(_arg0);
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IAppPerformanceMonitorCallback {
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
            public void onAPMConfigurationChanged(APMConfiguration config)
                    throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    mRemote.transact(TRANSACTION_onAPMConfigurationChanged, _data, null,
                            IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}