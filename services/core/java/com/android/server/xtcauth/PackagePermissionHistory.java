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

package com.android.server.xtcauth;

/**
 * A permission-check interval for a specific package/permission/user.
 *
 * @hide
 */
class PackagePermissionHistory extends PermissionHistoryBase {
    private static final int ODD_PRIME_NUMBER = 13;

    private int mUserId;
    private String mPackageName;
    private String mPermissionName;
    private int mStatus;

    public PackagePermissionHistory(int userId, String packageName, String permissionName,
            long startTime, int status) {
        super(startTime, 0L);
        mUserId = userId;
        mPackageName = packageName;
        mPermissionName = permissionName;
        mStatus = status;
    }

    public PackagePermissionHistory(int userId, String packageName, String permissionName,
            long startTime, long endTime, int status) {
        super(startTime, endTime);
        mUserId = userId;
        mPackageName = packageName;
        mPermissionName = permissionName;
        mStatus = status;
    }

    public int getUserId() {
        return mUserId;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public String getPermissionName() {
        return mPermissionName;
    }

    public int getStatus() {
        return mStatus;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PackagePermissionHistory)) {
            return false;
        }
        PackagePermissionHistory other = (PackagePermissionHistory) obj;
        return mStatus == other.getStatus()
                && mUserId == other.getUserId()
                && mPackageName.equals(other.getPackageName())
                && mPermissionName.equals(other.getPermissionName());
    }

    @Override
    public int hashCode() {
        int result = mUserId + 299;
        if (mPackageName != null) {
            result = (result * ODD_PRIME_NUMBER) + mPackageName.hashCode();
        }
        if (mPermissionName != null) {
            result = (result * ODD_PRIME_NUMBER) + mPermissionName.hashCode();
        }
        return (result * ODD_PRIME_NUMBER) + mStatus;
    }

    @Override
    public String toString() {
        return "PackagePermissionHistory {" + mUserId + ", " + mPackageName + ", "
                + mPermissionName + ", " + getStartTime() + ", " + getEndTime() + ", " + mStatus
                + "}";
    }
}