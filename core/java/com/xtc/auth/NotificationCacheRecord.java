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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Records whether a package's notifications are enabled by the XTC mobile
 * manager.
 *
 * @hide
 */
public class NotificationCacheRecord implements Parcelable {
    private static final String TAG = "NotificationCacheRecord";

    public static final Parcelable.Creator<NotificationCacheRecord> CREATOR =
            new Parcelable.Creator<NotificationCacheRecord>() {
                @Override
                public NotificationCacheRecord createFromParcel(Parcel in) {
                    return new NotificationCacheRecord(in, null);
                }

                @Override
                public NotificationCacheRecord[] newArray(int size) {
                    return new NotificationCacheRecord[size];
                }
            };

    public String packageName;
    public boolean enable;

    public NotificationCacheRecord(String packageName, boolean enable) {
        this.packageName = null;
        this.enable = false;
        this.packageName = packageName;
        this.enable = enable;
    }

    public NotificationCacheRecord(Parcel in) {
        packageName = null;
        enable = false;
        packageName = in.readString();
        enable = in.readInt() == 1;
    }

    private NotificationCacheRecord(Parcel in, NotificationCacheRecord unused) {
        this(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(packageName);
        out.writeInt(enable ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NotificationCacheRecord)) {
            return false;
        }
        NotificationCacheRecord record = (NotificationCacheRecord) obj;
        return packageName.equals(record.packageName) && enable == record.enable;
    }

    @Override
    public int hashCode() {
        return (packageName == null ? 0 : packageName.hashCode()) + (enable ? 31 : 0);
    }

    @Override
    public String toString() {
        return "NotificationCacheRecord {" + packageName + ", " + enable + "}";
    }
}