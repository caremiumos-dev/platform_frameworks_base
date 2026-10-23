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

import java.util.ArrayList;
import java.util.List;

/**
 * A permission and its sub-permissions together with the current status and
 * decision flags, as tracked by the XTC mobile manager.
 *
 * @hide
 */
public class Permission implements Parcelable {
    private static final String TAG = "Permission";
    private static final int ODD_PRIME_NUMBER = 13;

    public static final Parcelable.Creator<Permission> CREATOR =
            new Parcelable.Creator<Permission>() {
                @Override
                public Permission createFromParcel(Parcel in) {
                    return new Permission(in, null);
                }

                @Override
                public Permission[] newArray(int size) {
                    return new Permission[size];
                }
            };

    public String mPermissionName;
    public List<Permission> mSubPermissions;
    private int mStatus;
    private int mFlag;
    private int mHashCode;

    public Permission(String permissionName, List<Permission> subPermissions, int status,
            int flag) {
        mPermissionName = null;
        mSubPermissions = null;
        mStatus = 2;
        mFlag = 0;
        mHashCode = 0;
        mPermissionName = permissionName;
        mSubPermissions = subPermissions;
        mStatus = status;
        mFlag = flag;
        mHashCode = makeHashCode();
    }

    public Permission(Parcel in) {
        mPermissionName = null;
        mSubPermissions = null;
        mStatus = 2;
        mFlag = 0;
        mHashCode = 0;
        mPermissionName = in.readString();
        if (in.readByte() == 1) {
            mSubPermissions = new ArrayList<>();
            in.readTypedList(mSubPermissions, CREATOR);
        } else {
            mSubPermissions = null;
        }
        mStatus = in.readInt();
        mFlag = in.readInt();
        mHashCode = makeHashCode();
    }

    private Permission(Parcel in, Permission unused) {
        this(in);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mPermissionName);
        if (mSubPermissions != null) {
            out.writeByte((byte) 1);
            out.writeTypedList(mSubPermissions);
        } else {
            out.writeByte((byte) 0);
        }
        out.writeInt(mStatus);
        out.writeInt(mFlag);
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

    public int getFlag() {
        return mFlag;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Permission)) {
            return false;
        }
        Permission permission = (Permission) obj;
        return mPermissionName.equals(permission.mPermissionName)
                && mStatus == permission.mStatus
                && mFlag == permission.mFlag
                && mSubPermissions.equals(permission.mSubPermissions);
    }

    @Override
    public int hashCode() {
        return mHashCode;
    }

    @Override
    public String toString() {
        return "Permission {" + mPermissionName + ", " + mStatus + ", " + mFlag + "}";
    }

    private int makeHashCode() {
        int hashCode = ((mStatus + 299) * ODD_PRIME_NUMBER) + mFlag;
        if (mPermissionName != null) {
            hashCode = (hashCode * ODD_PRIME_NUMBER) + mPermissionName.hashCode();
        }
        if (mSubPermissions == null || mSubPermissions.size() <= 0) {
            return hashCode;
        }
        int hashCodeWithSubs = hashCode;
        for (int i = 0; i < mSubPermissions.size(); i++) {
            if (mSubPermissions.get(i) != null) {
                hashCodeWithSubs = mPermissionName.hashCode() + (hashCodeWithSubs * ODD_PRIME_NUMBER);
            }
        }
        return hashCodeWithSubs;
    }
}