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

package com.android.server;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes the permission dialogs so that only one is shown at a time, and
 * fans the user's answer out to the waiting callers.
 *
 * @hide
 */
public class PermissionDialogReqQueue {
    private PermissionDialog mDialog = null;
    private final List<PermissionDialogReq> mResultList = new ArrayList<>();

    public static final class PermissionDialogReq {
        boolean mHasResult = false;
        int mResult;

        public int get() {
            synchronized (this) {
                while (!mHasResult) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                    }
                }
            }
            return mResult;
        }

        public void set(int result) {
            synchronized (this) {
                mHasResult = true;
                mResult = result;
                notifyAll();
            }
        }
    }

    public PermissionDialog getDialog() {
        return mDialog;
    }

    public void setDialog(PermissionDialog dialog) {
        mDialog = dialog;
    }

    public void register(PermissionDialogReq req) {
        synchronized (this) {
            mResultList.add(req);
        }
    }

    public void notifyAll(int result) {
        synchronized (this) {
            while (mResultList.size() != 0) {
                mResultList.get(0).set(result);
                mResultList.remove(0);
            }
        }
    }
}