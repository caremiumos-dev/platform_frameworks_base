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

package com.android.server.xtcbehavior;

import android.content.Context;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.behavior.XtcBehavior;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

/**
 * Collects boot / shutdown / hardware diagnostics and reports them as behavior
 * events.
 *
 * <p><b>Condemnation.</b> Reads device hardware identifiers (eMMC manfid,
 * erase counts), ADB state and shutdown/crash reasons and uploads them to the
 * vendor. This is silent device telemetry without meaningful user consent.
 * Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class XtcBehaviorInit {
    private static final String TAG = "XtcBehaviorInit";

    private static final String PROPERTY_APP_CRASH = "persist.sys.appcrash";
    private static final String PROPERTY_BOOT_POWEROFF_MODE = "ro.boot.xtc.poffmode";
    private static final String PROPERTY_BOOT_VERITY_MODE = "ro.boot.veritymode";
    private static final String PROPERTY_HARDWARE_ISSUE = "persist.sys.xtc_hardware_issue";
    private static final String PROPERTY_HARDWARE_ISSUE_TOTAL_COUNT =
            "persist.sys.xtc_hardware_total_count";
    private static final String PROPERTY_HARDWARE_ISSUE_TYPE_COUNT = "persist.sys.xtc_hardware_type";
    private static final String PROPERTY_LEAVE_FACTORY = "persist.sys.xtc.adb_port";
    private static final String PROPERTY_POWER_OFF_REASON = "persist.sys.poffreason";
    private static final String PROPERTY_POWER_ON_REASON = "persist.sys.ponreason";
    private static final String PROPERTY_SHUTDOWN_REBOOT_BIGDATA = "persist.sys.occurdump";
    private static final String PROPERTY_USB_STATE = "sys.usb.state";
    private static final String PROPERTY_ADB_ENABLE_FROM = "sys.adb.enable_from";

    private static final String EMMC_BASE_PATH =
            "/sys/devices/platform/soc/7824900.sdhci/mmc_host/mmc0/mmc0:0001/";

    private int getAppCrashProperties() {
        return SystemProperties.getInt(PROPERTY_APP_CRASH, 0);
    }

    private String getPowerOffReasonProperties() {
        return SystemProperties.get(PROPERTY_POWER_OFF_REASON, "None");
    }

    private String getPowerOnReasonProperties() {
        return SystemProperties.get(PROPERTY_POWER_ON_REASON, "None");
    }

    private String getRootPoweroffModeProperties() {
        return SystemProperties.get(PROPERTY_BOOT_POWEROFF_MODE, "None");
    }

    private String getRootVerityModeProperties() {
        return SystemProperties.get(PROPERTY_BOOT_VERITY_MODE, "None");
    }

    private int getShutDownAndRebootProperties() {
        return SystemProperties.getInt(PROPERTY_SHUTDOWN_REBOOT_BIGDATA, 0);
    }

    private String readEmmcDeviceReport() {
        return readSysFile("device_report");
    }

    private String readEmmcManfid() {
        return readSysFile("manfid");
    }

    private String readSysFile(String name) {
        try (BufferedReader reader = new BufferedReader(new FileReader(EMMC_BASE_PATH + name))) {
            return reader.readLine();
        } catch (Exception e) {
            Log.e(TAG, "read " + name + " ex:" + e);
            return null;
        }
    }

    public void sendAdbStateBehavior(Context context) {
        String adbState = SystemProperties.get(PROPERTY_USB_STATE, "None");
        String adbEnableFrom = SystemProperties.get(PROPERTY_ADB_ENABLE_FROM, "None");
        String adbFactoryState = SystemProperties.get(PROPERTY_LEAVE_FACTORY, "None");
        HashMap<String, String> map = new HashMap<>();
        map.put("adbState", adbState);
        map.put("adbEnableFrom", adbEnableFrom);
        map.put("adbFactoryState", adbFactoryState);
        new XtcBehavior.Builder(context, "xtc_adb_state", map).build().sendData();
    }

    public void sendEmmcEraseCountBehavior(Context context) {
        HashMap<String, String> map = new HashMap<>();
        String manfid = readEmmcManfid();
        String deviceReport = readEmmcDeviceReport();
        if (TextUtils.isEmpty(manfid) || TextUtils.isEmpty(deviceReport)) {
            Log.w(TAG, "sendEmmcEraseCountBehavior No DATA ! manfid:" + manfid
                    + ", deviceReport:" + deviceReport);
            return;
        }
        String[] parts = deviceReport.split(",");
        if (parts == null || parts.length <= 0) {
            Log.w(TAG, "sendEmmcEraseCountBehavior spilt null.");
            return;
        }
        try {
            for (String part : parts) {
                int index = part.indexOf(":");
                map.put(part.substring(0, index), part.substring(index + 1));
            }
        } catch (Exception e) {
            Log.e(TAG, "sendEmmcEraseCountBehavior ex:" + e);
        }
        map.put("manfid", manfid);
        new XtcBehavior.Builder(context, "emmc_erasecount", map).build().sendData();
    }

    public void sendHardwareIssueBehavior(Context context) {
        String check = SystemProperties.get(PROPERTY_HARDWARE_ISSUE, "0");
        String typeCount = SystemProperties.get(PROPERTY_HARDWARE_ISSUE_TYPE_COUNT, "0");
        String totalCount = SystemProperties.get(PROPERTY_HARDWARE_ISSUE_TOTAL_COUNT, "0");
        HashMap<String, String> map = new HashMap<>();
        map.put("hardware_check", check);
        map.put("type_count", typeCount);
        map.put("total_count", totalCount);
        try {
            if (check.equals("1") && !typeCount.equals("0") && !totalCount.equals("0")) {
                new XtcBehavior.Builder(context, "hardware_issue", map).build().sendData();
            }
        } catch (Exception e) {
            Log.e(TAG, "sendHardwareIssueBehavior ex:" + e);
        }
    }

    public void sendShutdownOrAppCrashBehavior(Context context) {
        HashMap<String, String> map = new HashMap<>();
        int occurdump = getShutDownAndRebootProperties();
        int appcrash = getAppCrashProperties();
        String veritymode = getRootVerityModeProperties();
        String ponreason = getPowerOnReasonProperties();
        String poffreason = getPowerOffReasonProperties();
        map.put("occurdump", String.valueOf(occurdump));
        map.put("appcrash", String.valueOf(appcrash));
        map.put("veritymode", veritymode);
        map.put("ponreason", ponreason);
        map.put("poffreason", poffreason);
        map.put("poweroffmode", getRootPoweroffModeProperties());
        SystemProperties.set(PROPERTY_SHUTDOWN_REBOOT_BIGDATA, String.valueOf(0));
        SystemProperties.set(PROPERTY_APP_CRASH, String.valueOf(0));
        new XtcBehavior.Builder(context, "System_RestartMonitor", map).build().sendData();
    }
}