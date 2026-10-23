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

package com.android.server.xgseserver.xss;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

/**
 * Thin client for the vendor {@code xtcpartitionverify} verity service, used to
 * check the integrity of the boot partitions.
 *
 * @hide
 */
public class VSManager {
    private static final String TAG = "xssVSManager";
    private static final String DESCRIPTOR = "xtcpartitionverify";
    private static final int TRANSACTION_VeritySinglePartition = 102;

    private static final int RESULT_ABOOT_ERROR = -7;
    private static final int RESULT_BOOT_ERROR = -2;
    private static final int RESULT_RECOVERY_ERROR = -3;

    private IBinder mVerityService;

    private int VSCheck(String partition) {
        int result = 0;
        if (getVerityService() != null) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(DESCRIPTOR);
            data.writeString(partition);
            try {
                getVerityService().transact(TRANSACTION_VeritySinglePartition, data, reply, 0);
                result = reply.readInt();
                Log.d(TAG, "VSCheck result = " + result);
            } catch (RemoteException e) {
                Log.e(TAG, "VSCheck ", e);
            } finally {
                data.recycle();
                reply.recycle();
            }
        }
        return result;
    }

    public boolean getPartitionFilesStatus() {
        return VSCheck("aboot") != RESULT_ABOOT_ERROR
                && VSCheck("boot") != RESULT_BOOT_ERROR
                && VSCheck("recovery") != RESULT_RECOVERY_ERROR;
    }

    public IBinder getVerityService() {
        if (this.mVerityService == null) {
            this.mVerityService = ServiceManager.getService(DESCRIPTOR);
        }
        return this.mVerityService;
    }
}