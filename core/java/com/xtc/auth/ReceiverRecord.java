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
 * Tracks whether a package's broadcast receiver is enabled by the XTC mobile
 * manager.
 *
 * @hide
 */
public class ReceiverRecord implements Parcelable {
    public static final Parcelable.Creator<ReceiverRecord> CREATOR =
            new Parcelable.Creator<ReceiverRecord>() {
                @Override
                public ReceiverRecord createFromParcel(Parcel in) {
                    return new ReceiverRecord(in, null);
                }

                @Override
                public ReceiverRecord[] newArray(int size) {
                    return new ReceiverRecord[size];
                }
            };

    public boolean enabled;
    public final String packageName;

    public ReceiverRecord(String packageName) {
        enabled = true;
        this.packageName = packageName;
        enabled = true;
    }

    public ReceiverRecord(String packageName, boolean enabled) {
        this.enabled = true;
        this.packageName = packageName;
        this.enabled = enabled;
    }

    public ReceiverRecord(ReceiverRecord receiverRecord) {
        enabled = true;
        this.packageName = receiverRecord.packageName;
        this.enabled = receiverRecord.enabled;
    }

    public ReceiverRecord(Parcel in) {
        enabled = true;
        this.packageName = in.readString();
        this.enabled = in.readInt() == 1;
    }

    private ReceiverRecord(Parcel in, ReceiverRecord unused) {
        this(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(packageName);
        out.writeInt(enabled ? 1 : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ReceiverRecord)) {
            return false;
        }
        ReceiverRecord record = (ReceiverRecord) obj;
        return packageName.equals(record.packageName) && enabled == record.enabled;
    }

    @Override
    public int hashCode() {
        return (packageName == null ? 0 : packageName.hashCode()) + (enabled ? 31 : 0);
    }

    @Override
    public String toString() {
        return "ReceiverRecord {" + packageName + ", " + enabled + "}";
    }
}