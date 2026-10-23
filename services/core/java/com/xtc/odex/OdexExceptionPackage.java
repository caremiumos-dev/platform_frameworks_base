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

package com.xtc.odex;

/**
 * Tracks the crash history of a package so that the odex-restore policy can
 * decide to re-optimize (force dex-opt) it.
 *
 * @hide
 */
public class OdexExceptionPackage {
    private final String mPackageName;
    private final int mVersionCode;
    private final String mVersionName;

    private int mJECount;
    private long mLastJETimestamp;
    private int mNECount;
    private long mLastNETimestamp;
    private int mState;

    public OdexExceptionPackage(String packageName, int versionCode, String versionName) {
        mPackageName = packageName;
        mVersionCode = versionCode;
        mVersionName = versionName == null ? "unknown" : versionName;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public int getVersionCode() {
        return mVersionCode;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public int getJECount() {
        return mJECount;
    }

    public long getLastJETimestamp() {
        return mLastJETimestamp;
    }

    public int getNECount() {
        return mNECount;
    }

    public long getLastNETimestamp() {
        return mLastNETimestamp;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }

    public void recordJavaException() {
        mJECount++;
        mLastJETimestamp = System.currentTimeMillis();
    }

    public void recordNativeException() {
        mNECount++;
        mLastNETimestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "OdexExceptionPackage{mPackageName='" + mPackageName + "', mVersionCode="
                + mVersionCode + ", mVersionName='" + mVersionName + "', mJECount=" + mJECount
                + ", mLastJETimestamp=" + mLastJETimestamp + ", mNECount=" + mNECount
                + ", mLastNETimestamp=" + mLastNETimestamp + ", mState=" + mState + '}';
    }
}