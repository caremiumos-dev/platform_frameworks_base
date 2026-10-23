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

package com.android.server.xtcdiag.qdiag;

import android.util.Log;

import com.qualcomm.qti.diagservice.diag_dci;
import com.qualcomm.qti.diagservice.diag_dci_cbs;
import com.qualcomm.qti.diagservice.diag_dci_health_stats;
import com.qualcomm.qti.diagservice.diag_lsm;

/**
 * Wraps the Qualcomm diag DCI client for the XTC diag service.
 *
 * @hide
 */
public class XtcQDiagDci {
    private static final String TAG = "XtcDiag-QDiagDci";
    private static final int DIAG_SIGNAL = 48;

    private diag_dci_cbs mCallBack;
    private int mError = -1;
    private int mClientId = -1;
    private long mPeripList = 0;
    private int mChannel = 0;
    private final diag_dci mDci = new diag_dci();

    public XtcQDiagDci(diag_dci_cbs callback) {
        mCallBack = callback;
        mDci.register_dci_callback(mCallBack);
        init();
    }

    public void init() {
        mPeripList = 11L;
        mChannel = 0;
        boolean initResult = diag_lsm.Diag_LSM_Init(mChannel);
        mClientId = diag_dci.diag_register_dci_client(mPeripList, mChannel, 0);
        Log.d(TAG, "init:" + initResult + " ,mClientId:" + mClientId);
    }

    public void release() {
        if (mClientId != -1) {
            diag_dci.diag_release_dci_client(mClientId);
        }
        diag_lsm.Diag_LSM_DeInit();
        mClientId = -1;
    }

    public void registerDciSignalData() {
        mError = diag_dci.diag_register_dci_signal_data(mClientId, DIAG_SIGNAL);
        Log.d(TAG, "registerDciSignalData mError(1001:NoError):" + mError);
    }

    public void registerDciStream() {
        mError = diag_dci.diag_register_dci_stream_proc(mCallBack, mClientId);
        Log.d(TAG, "registerDciStream mError(1001:NoError):" + mError);
    }

    public void configLogStream(boolean enable, int[] logCodes) {
        if (logCodes == null || mClientId == -1) {
            return;
        }
        diag_dci.diag_log_stream_config(mClientId, enable ? 1 : 0, logCodes, logCodes.length);
    }

    public void configEventStream(boolean enable, int[] eventCodes) {
        if (eventCodes == null || mClientId == -1) {
            return;
        }
        diag_dci.diag_event_stream_config(mClientId, enable ? 1 : 0, eventCodes,
                eventCodes.length);
    }

    public int checkLogCodeEnable(long logCode) {
        if (mClientId == -1) {
            return 0;
        }
        return diag_dci.diag_get_log_status(mClientId, logCode);
    }

    public int checkEventCodeEnable(long eventCode) {
        if (mClientId == -1) {
            return 0;
        }
        return diag_dci.diag_get_event_status(mClientId, eventCode);
    }

    public void sendDciAsyncCommand(byte[] command) {
        if (command == null || mClientId == -1) {
            return;
        }
        mError = diag_dci.diag_send_dci_async_req(mClientId, command, command.length, 0L);
        Log.d(TAG, "sendDciAsyncCommand mError(1001:NoError):" + mError);
    }

    public void getSupportPeripList() {
        int list = diag_dci.diag_get_dci_support_list_proc(mChannel);
        Log.d(TAG, "getSupportPeripList list:" + list);
        Log.d(TAG, " MPSS: \t" + ((list & 2) != 0 ? "UP" : "DOWN"));
        Log.d(TAG, " LPASS: \t" + ((list & 4) != 0 ? "UP" : "DOWN"));
        Log.d(TAG, " WCNSS: \t" + ((list & 8) != 0 ? "UP" : "DOWN"));
        Log.d(TAG, " APSS: \t" + ((list & 1) != 0 ? "UP" : "DOWN"));
    }

    public void printProcHealthStats(int proc) {
        Object stats = diag_dci.diag_get_health_stats_proc(mClientId, proc);
        try {
            if (stats instanceof diag_dci_health_stats) {
                diag_dci_health_stats healthStats = (diag_dci_health_stats) stats;
                Log.d(TAG, "   Log Drop Count for Modem:" + healthStats.dropped_logs);
                Log.d(TAG, "   Log Total Count for Modem:" + healthStats.received_logs);
                Log.d(TAG, "   Event Drop Count for Modem:" + healthStats.dropped_events);
                Log.d(TAG, "   Event Total Count for Modem:" + healthStats.received_events);
            } else {
                Log.d(TAG, "printProcHealthStatus Happen Error:" + stats);
            }
        } catch (Exception e) {
        }
    }
}