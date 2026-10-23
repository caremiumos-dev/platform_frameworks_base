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
 * Identifies a boot-receiver record owner.
 *
 * @hide
 */
class ReceiverRecorder {
    private String mName;
    private int mAppId = -1;
    private int mIdentity;

    public ReceiverRecorder(ManagerIdentityHelper.ManagerIdentity identity) {
        mName = identity.getName();
        mAppId = identity.getAppId();
        mIdentity = identity.getIdentity();
    }

    public ReceiverRecorder(ReceiverRecorder recorder) {
        mName = recorder.getName();
        mAppId = recorder.getAppId();
        mIdentity = recorder.getIdentity();
    }

    public ReceiverRecorder(String name, int appId, int identity) {
        mName = name;
        mAppId = appId;
        mIdentity = identity;
    }

    public String getName() {
        return mName;
    }

    public int getAppId() {
        return mAppId;
    }

    public int getIdentity() {
        return mIdentity;
    }

    public String genPostfix() {
        return "_" + Integer.toString(mIdentity);
    }

    @Override
    public String toString() {
        return "ReceiverRecorder {" + mName + ", " + mAppId + ", " + mIdentity + "}";
    }
}