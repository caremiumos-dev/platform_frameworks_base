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

import android.os.Bundle;
import android.os.IBinder;
import android.os.UserHandle;
import android.util.Log;

import com.xtc.auth.IPermissionListener;
import com.xtc.auth.IRequestedPermissionCallback;
import com.xtc.auth.PermissionRecord;

/**
 * Asks the management application whether an application may be granted a
 * permission, and reports the answer back to the (possibly asynchronous) caller.
 *
 * @hide
 */
final class PermissionListenerTrigger extends ListenerTriggerBase {
    public static final String TAG = "PermissionListenerTrigger";

    private final PermissionHistoryHelper mHistoryHelper;

    public PermissionListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult) {
        super(controllerID, callingPid, listener, params, defaultResult);
        mHistoryHelper = PermissionHistoryHelper.getInstance();
    }

    public PermissionListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult, IBinder callback) {
        super(controllerID, callingPid, listener, params, defaultResult, callback);
        mHistoryHelper = PermissionHistoryHelper.getInstance();
    }

    private void triggerCallback() {
        IBinder callback = getCallback();
        if (callback != null) {
            int callingPid = getCallingPid();
            Bundle params = getParams();
            PermissionRecord record = params.getParcelable(
                    PermissionController.PARAM_PERMISSION_RECORD);
            int uid = params.getInt(PermissionController.PARAM_UID);
            int result = getResult();
            try {
                PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                        "triggerCallbackFunc MoMS >> Caller with result: "
                                + PermissionController.result2String(result));
                IRequestedPermissionCallback.Stub.asInterface(callback)
                        .onPermissionCheckResult(record.mPermissionName, uid, result);
            } catch (android.os.RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "onPermissionCheckResult() failed! ");
            }
            PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                    "triggerCallbackFunc MoMS << Caller");
        }
    }

    @Override
    public void onTriggerListenerDone() {
        triggerCallback();
    }

    @Override
    public void onTriggerListenerTimeout() {
        Bundle params = getParams();
        int uid = params.getInt(PermissionController.PARAM_UID);
        int callingPid = getCallingPid();
        int userId = UserHandle.getUserId(uid);
        PermissionRecord record = params.getParcelable(
                PermissionController.PARAM_PERMISSION_RECORD);
        if (record.getStatus() != 0) {
            mHistoryHelper.setEnd(userId,
                    params.getString(PermissionController.PARAM_PACKAGE),
                    record.mPermissionName, record.getStatus());
        }
        if (getCallback() == null) {
            PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                    "triggerListenerFunc MoMS >> MoMA is TIMEOUT, default result: "
                            + PermissionController.result2String(getResult()));
        } else {
            PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                    "triggerListenerFunc MoMS >> MoMA is TIMEOUT");
            triggerCallback();
        }
    }

    @Override
    public int triggerListenerFunc() {
        int result = 0;
        Bundle params = getParams();
        IBinder func = getFunc();
        String packageName = params.getString(PermissionController.PARAM_PACKAGE);
        PermissionRecord record = params.getParcelable(
                PermissionController.PARAM_PERMISSION_RECORD);
        int flag = params.getInt(PermissionController.PARAM_FLAG);
        int uid = params.getInt(PermissionController.PARAM_UID);
        Bundle data = params.getBundle(PermissionController.PARAM_DATA);
        int callingPid = getCallingPid();
        int userId = UserHandle.getUserId(uid);
        if (func != null) {
            PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                    "triggerListenerFunc MoMS >> MoMA " + ("with pr: " + record.toString()
                            + " flag: " + flag));
            boolean allowed;
            try {
                allowed = IPermissionListener.Stub.asInterface(func)
                        .onPermissionCheck(record, flag, uid, data);
            } catch (Exception e) {
                Log.e(TAG, "onPermissionCheck() failed!", e);
                allowed = true;
            }
            result = allowed ? 0 : -1;
            if (record.getStatus() == 2) {
                PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                        "triggerListenerFunc MoMS << MoMA with result: "
                                + PermissionController.result2String(result));
                mHistoryHelper.setEnd(userId, packageName, record.mPermissionName,
                        record.getStatus());
            } else if (record.getStatus() == 1) {
                PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                        "triggerListenerFunc MoMS << MoMA");
                mHistoryHelper.setEnd(userId, packageName, record.mPermissionName,
                        record.getStatus());
            }
        } else {
            PermissionController.PRINT_DEBUG_LOG(uid, callingPid, record.mPermissionName,
                    "triggerListenerFunc MoMS >> MoMA FAILED due to null binder, return result: "
                            + PermissionController.result2String(0));
        }
        return result;
    }
}