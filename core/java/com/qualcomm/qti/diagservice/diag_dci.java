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

package com.qualcomm.qti.diagservice;

import android.util.Log;

/**
 * JNI wrapper around the Qualcomm diag DCI client library.
 *
 * @hide
 */
public class diag_dci {
    private static final String TAG = "LibDiagWrapper";

    public static final int DIAG_DCI_NO_ERROR = 1001;
    public static final int DIAG_DCI_NO_REG = 1002;
    public static final int DIAG_DCI_NO_MEM = 1003;
    public static final int DIAG_DCI_NOT_SUPPORTED = 1004;
    public static final int DIAG_DCI_HUGE_PACKET = 1005;
    public static final int DIAG_DCI_SEND_DATA_FAIL = 1006;
    public static final int DIAG_DCI_ERR_DEREG = 1007;
    public static final int DIAG_DCI_PARAM_FAIL = 1008;
    public static final int DIAG_DCI_DUP_CLIENT = 1009;

    diag_dci_cbs reg_obj = null;

    static {
        try {
            System.loadLibrary("diagjni");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "WARNING: Could not load libdiagjni");
        }
    }

    public static final native int diag_dci_get_version();

    public static final native int diag_dci_set_version(int version, int proc);

    public static final native int diag_dci_vote_real_time(int vote, int proc);

    public static final native int diag_dci_get_real_time_status_proc(int proc);

    public static final native int diag_register_dci_client(long peripheralList, int proc, int mode);

    public static final native int diag_release_dci_client(int clientId);

    public static final native int diag_register_dci_signal_data(int clientId, int signalType);

    public static final native int diag_register_dci_stream_proc(Object callback, int clientId);

    public static final native int diag_deregister_dci_signal_data(int clientId);

    public static final native int diag_send_dci_async_req(int clientId, byte[] buffer, int length,
            long timeout);

    public static final native int diag_log_stream_config(int clientId, int enable, int[] logCodes,
            int count);

    public static final native int diag_event_stream_config(int clientId, int enable,
            int[] eventCodes, int count);

    public static final native int diag_disable_all_logs(int clientId);

    public static final native int diag_disable_all_events(int clientId);

    public static final native int diag_get_log_status(int clientId, long logCode);

    public static final native int diag_get_event_status(int clientId, long eventCode);

    public static final native int diag_get_dci_support_list_proc(int proc);

    public static final native Object diag_get_health_stats_proc(int clientId, int proc);

    public void diag_dci_log_cb(byte[] data, int length) {
        reg_obj.process_dci_log_stream(data, length);
    }

    public void diag_dci_event_cb(byte[] data, int length) {
        reg_obj.process_dci_event_stream(data, length);
    }

    public void diag_dci_pkt_rsp_cb(byte[] data, int length, long timeout) {
        reg_obj.process_dci_pkt_rsp(data, length, timeout);
    }

    public void diag_dci_signal_cb_handler(int signal) {
        reg_obj.process_dci_signal_cb(signal);
    }

    public void register_dci_callback(diag_dci_cbs callbacks) {
        reg_obj = callbacks;
    }
}