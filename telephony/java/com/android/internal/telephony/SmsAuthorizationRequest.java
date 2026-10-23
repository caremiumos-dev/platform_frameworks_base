/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.telephony;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;

/**
 * XTC/厂商短信发送授权请求。由框架在发送短信前创建并交给 {@link ISmsSecurityService}，
 * 由短信安全应用调用 {@link #accept()} / {@link #reject()} 决定是否放行。
 */
public class SmsAuthorizationRequest implements Parcelable {
    private final ISmsSecurityService service;
    private final IBinder token;
    public final String packageName;
    public final String destinationAddress;
    public final String message;

    public SmsAuthorizationRequest(ISmsSecurityService service, IBinder token,
            String packageName, String destinationAddress, String message) {
        this.service = service;
        this.token = token;
        this.packageName = packageName;
        this.destinationAddress = destinationAddress;
        this.message = message;
    }

    private SmsAuthorizationRequest(Parcel in) {
        this.service = ISmsSecurityService.Stub.asInterface(in.readStrongBinder());
        this.token = in.readStrongBinder();
        this.packageName = in.readString();
        this.destinationAddress = in.readString();
        this.message = in.readString();
    }

    public IBinder getToken() {
        return token;
    }

    public void accept() throws RemoteException {
        service.sendResponse(this, true);
    }

    public void reject() throws RemoteException {
        service.sendResponse(this, false);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeStrongBinder(service.asBinder());
        out.writeStrongBinder(token);
        out.writeString(packageName);
        out.writeString(destinationAddress);
        out.writeString(message);
    }

    public static final Parcelable.Creator<SmsAuthorizationRequest> CREATOR =
            new Parcelable.Creator<SmsAuthorizationRequest>() {
                @Override
                public SmsAuthorizationRequest createFromParcel(Parcel in) {
                    return new SmsAuthorizationRequest(in);
                }

                @Override
                public SmsAuthorizationRequest[] newArray(int size) {
                    return new SmsAuthorizationRequest[size];
                }
            };

    @Override
    public String toString() {
        return String.format("[%s] (%s) # %s", packageName, destinationAddress, message);
    }
}
