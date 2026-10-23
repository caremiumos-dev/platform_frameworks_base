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

package com.android.server.performance;

/**
 * Debug switches for the XTC performance subsystem.
 *
 * @hide
 */
public class PerformanceManagerDebugConfig {
    public static String TAG = "xtcperformance";

    public static boolean DEBUG_ALL = false;
    public static boolean DEBUG_SERVICE;
    public static boolean DEBUG_USRHABIT;
    public static boolean DEBUG_PROCSTAT;
    public static boolean DEBUG_PRIOADJ;
    public static boolean DEBUG_RAMPOLICY;
    public static boolean DEBUG_CPUSTAT;

    static {
        DEBUG_SERVICE = DEBUG_ALL;
        DEBUG_USRHABIT = DEBUG_ALL;
        DEBUG_PROCSTAT = DEBUG_ALL;
        DEBUG_PRIOADJ = DEBUG_ALL;
        DEBUG_RAMPOLICY = DEBUG_ALL;
        DEBUG_CPUSTAT = DEBUG_ALL;
    }

    public static void enableDebug(boolean enable) {
        DEBUG_ALL = enable;
        DEBUG_SERVICE = DEBUG_ALL;
        DEBUG_USRHABIT = DEBUG_ALL;
        DEBUG_PROCSTAT = DEBUG_ALL;
        DEBUG_PRIOADJ = DEBUG_ALL;
        DEBUG_RAMPOLICY = DEBUG_ALL;
        DEBUG_CPUSTAT = DEBUG_ALL;
    }

    public static void handleDebugSwitch(String[] switches, boolean enable) {
        boolean service = false;
        boolean usrhabit = false;
        boolean procstat = false;
        boolean prioadj = false;
        boolean rampolicy = false;
        boolean cpustat = false;
        boolean all = false;
        int i = 0;
        while (i < switches.length) {
            String value = switches[i];
            if (value == null || value.length() <= 0) {
                break;
            }
            i++;
            if ("usrhabit".equals(value)) {
                usrhabit = enable;
            } else if ("service".equals(value)) {
                service = enable;
            } else if ("procstat".equals(value)) {
                procstat = enable;
            } else if ("prioadj".equals(value)) {
                prioadj = enable;
            } else if ("rampolicy".equals(value)) {
                rampolicy = enable;
            } else if ("cpustat".equals(value)) {
                cpustat = enable;
            } else if ("all".equals(value)) {
                all = enable;
            }
        }
        DEBUG_ALL = all;
        DEBUG_SERVICE = all || service;
        DEBUG_USRHABIT = all || usrhabit;
        DEBUG_PROCSTAT = all || procstat;
        DEBUG_PRIOADJ = all || prioadj;
        DEBUG_RAMPOLICY = all || rampolicy;
        DEBUG_CPUSTAT = all || cpustat;
    }
}