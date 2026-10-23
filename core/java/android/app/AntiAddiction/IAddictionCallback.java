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
 * One-way callback delivering anti-addiction duration information.
 *
 * @hide
 */
public interface IAddictionCallback extends IInterface {
    /** Binder transaction id for {@link #getAddictionDuration}. */
    int TRANSACTION_getAddictionDuration = IBinder.FIRST_CALL_TRANSACTION;

    void getAddictionDuration(String calendarDate, String eventType, String extra, int duration)
            throws RemoteException;

    abstract class Stub extends Binder implements IAddictionCallback {
        private static final String DESCRIPTOR = "android.app.AntiAddiction.IAddictionCallback";

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAddictionCallback asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && iin instanceof IAddictionCallback) {
                return (IAddictionCallback) iin;
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
                case TRANSACTION_getAddictionDuration: {
                    data.enforceInterface(DESCRIPTOR);
                    getAddictionDuration(data.readString(), data.readString(),
                            data.readString(), data.readInt());
                    return true;
                }
            }
            return super.onTransact(code, data, reply, flags);
        }

        private static class Proxy implements IAddictionCallback {
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
            public void getAddictionDuration(String calendarDate, String eventType, String extra,
                    int duration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeString(calendarDate);
                    _data.writeString(eventType);
                    _data.writeString(extra);
                    _data.writeInt(duration);
                    mRemote.transact(TRANSACTION_getAddictionDuration, _data, null,
                            IBinder.FLAG_ONEWAY);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}