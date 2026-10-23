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
import android.util.Log;

/**
 * Records the permission decision (accept/reject plus status) for a
 * package/permission pair made by the XTC mobile manager.
 *
 * @hide
 */
public class PermissionRecord implements Parcelable {
    private static final String TAG = "PermissionRecord";
    private static final int PRIM_NUMBER = 17;

    public static final int NOT_ACCEPT_CODE = 0;
    public static final int ACCEPT_CODE = 1;

    /** Base value used for hash computation (matches user permission record). */
    private static final int HASH_BASE = 391;

    public static final Parcelable.Creator<PermissionRecord> CREATOR =
            new Parcelable.Creator<PermissionRecord>() {
                @Override
                public PermissionRecord createFromParcel(Parcel in) {
                    return new PermissionRecord(in, null);
                }

                @Override
                public PermissionRecord[] newArray(int size) {
                    return new PermissionRecord[size];
                }
            };

    public String mPackageName;
    public String mPermissionName;
    private int mStatus;
    private int mAccept;

    public PermissionRecord(String packageName, String permissionName, int status, int accept) {
        mPackageName = null;
        mPermissionName = null;
        mStatus = 0;
        mAccept = 0;
        mPackageName = packageName;
        mPermissionName = permissionName;
        mStatus = status;
        mAccept = accept;
    }

    public PermissionRecord(Parcel in) {
        mPackageName = null;
        mPermissionName = null;
        mStatus = 0;
        mAccept = 0;
        mPackageName = in.readString();
        mPermissionName = in.readString();
        mStatus = in.readInt();
        mAccept = in.readInt();
    }

    private PermissionRecord(Parcel in, PermissionRecord unused) {
        this(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mPackageName);
        out.writeString(mPermissionName);
        out.writeInt(mStatus);
        out.writeInt(mAccept);
    }

    public void setStatus(int status) {
        if (status == 0 || status == 1 || status == 2) {
            mStatus = status;
        } else {
            Log.e(TAG, "Invalid permission status: " + status);
        }
    }

    public int getStatus() {
        return mStatus;
    }

    public int getAccept() {
        return mAccept;
    }

    public void setAccept(int accept) {
        mAccept = accept;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PermissionRecord)) {
            return false;
        }
        PermissionRecord record = (PermissionRecord) obj;
        if (mPackageName.equals(record.mPackageName)
                && mPermissionName.equals(record.mPermissionName)
                && mStatus == record.mStatus) {
            return mAccept == record.mAccept;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = mStatus + HASH_BASE;
        if (mPackageName != null) {
            hashCode = (hashCode * PRIM_NUMBER) + mPackageName.hashCode();
        }
        return mPermissionName != null
                ? (hashCode * PRIM_NUMBER) + mPermissionName.hashCode() : hashCode;
    }

    @Override
    public String toString() {
        return "PermissionRecord {" + mPackageName + ", " + mPermissionName + ", "
                + mStatus + ", " + mAccept + "}";
    }
}