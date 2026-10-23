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

/**
 * Health statistics returned by the Qualcomm diag DCI client library.
 *
 * @hide
 */
public class diag_dci_health_stats {
    public int ret_val;
    public int dropped_logs;
    public int dropped_events;
    public int received_logs;
    public int received_events;
    public int reset_status;

    public diag_dci_health_stats(int retVal, int droppedLogs, int droppedEvents, int receivedLogs,
            int receivedEvents, int resetStatus) {
        ret_val = retVal;
        dropped_logs = droppedLogs;
        dropped_events = droppedEvents;
        received_logs = receivedLogs;
        received_events = receivedEvents;
        reset_status = resetStatus;
    }
}