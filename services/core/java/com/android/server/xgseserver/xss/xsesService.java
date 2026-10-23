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

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.util.Slog;

import com.android.server.SystemService;
import com.android.server.pm.PackageManagerService;
import com.xtc.behavior.XtcBehavior;
import com.xtc.behavior.XtcBehaviorDataLevel;
import com.xtc.moduleswitch.ModuleSwitch;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * XGS "security service" (xses). It periodically asks a vendor server for a
 * policy blob and then applies it locally: shows dialogs, uninstalls packages,
 * disables packages, and revokes network access from packages/addresses.
 *
 * <p><b>Strong condemnation.</b> Large parts of this service are abusive and
 * should never have shipped on a device the user owns:
 * <ul>
 *   <li>{@link #checkRoot()} / {@link #xsesCR()} / {@link #doCheckUsbStatus} /
 *       {@link #disableAdbFromUsb} punish the legitimate act of rooting or
 *       unlocking one's own hardware and silently force ADB off. Treating a
 *       user's choice over their own device as a "security exception" is
 *       user-hostile and wrong.</li>
 *   <li>The remote policy can uninstall, disable and network-isolate arbitrary
 *       packages chosen by the vendor with no user consent (see
 *       {@code SystemExceptionManager}). This is censorship of third-party
 *       applications and an unacceptable restriction of user freedom.</li>
 *   <li>{@link #onBootPhase} can even force a reboot of the device.</li>
 * </ul>
 * This code is reproduced only to keep the firmware image behaviourally
 * compatible; the behaviour itself is condemned.
 *
 * @hide
 */
public class xsesService extends SystemService {
    private static final String TAG = "SystemXses Service";

    private static final String ACTION_BOOT_CHECK = "com.xtc.secure.BOOT.CHECK";
    private static final String ACTION_IM_SECURE = "com.xtc.sys.secure.im";
    private static final String ACTION_NET_PUSH = "com.xtc.secure.NET.PUSH";
    private static final String ACTION_NET_REQUEST = "com.xtc.secure.NET.REQUEST";
    private static final String PERSIST_PROR = "persist.sys.usb.config";
    private static final String SECURE_IM_CONTENT = "imContent";
    private static final String SECURE_IM_TYPE = "imType";
    private static final String SECURE_PERM = "com.xtc.sys.secure.IM_FORWARD_PERM";
    private static final String SYS_PROP = "sys.usb.config";

    private static final String ACTION_CONNECTIVITY_CHANGE =
            "android.net.conn.CONNECTIVITY_CHANGE";
    private static final String ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE";
    private static final String ACTION_MODULE_SWITCH_INIT =
            "com.xtc.launcher.moduleswitch.INIT_ACTION";

    private static final String EXTRA_NETWORK_INFO = "networkInfo";
    private static final String EXTRA_ADB = "adb";
    private static final String EXTRA_CONNECTED = "connected";

    private static final String PROP_ADB_ENABLE_FROM = "sys.adb.enable_from";
    private static final String ADB_FROM_ALTER_SALE_ON = "after_sale_on";
    private static final String ADB_FROM_SELF_TEST_ON = "self_test_on";

    private static final String PROP_DEV_SW = "log.all.dev_sw";
    private static final String PROP_ADB_PORT = "persist.sys.xtc.adb_port";
    private static final String PROP_OVERLAY = "persist.sys.server.ov";

    private static final int MODULE_ROOT_CHECK = 3400;
    private static final int MODULE_ADB_CHECK = 3402;
    private static final int MODULE_ADB_FROM_NETWORK = 105;
    private static final int MODULE_RUN_CHECK = 3404;
    private static final int MODULE_NET_WHITE = 3457;
    private static final int MODULE_SERVER_CONTROL = 3401;
    private static final int MODULE_REPORT_NET_ADDR = 3499;

    private static final long NET_REQUEST_INTERVAL_MS = 4 * 60 * 60 * 1000L;
    private static final long NET_REQUEST_DELAY_MS = 60 * 1000L;

    private final BroadcastReceiver mXsesServiceReceiver;
    private final BroadcastReceiver mInitModuleSwitchReceiver;

    private AlarmManager mAlarmManager;
    private final Context mContext;
    private boolean mNetRequestStarted;

    private static boolean sRootCheckSwitch = false;
    private static boolean sAdbCheckSwitch = false;
    private static boolean sInstallCheckSwitch = false;
    public static boolean runCheckSwitch = true;
    public static boolean serverControlPolicy = false;
    public static boolean adbFromNetwork = false;
    private static boolean sLastUsbConnected = false;
    public static boolean netWhiteSwitch = false;
    public static boolean reportNetAddrSwitch = true;

    public xsesService(Context context) {
        super(context);
        this.mXsesServiceReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                Slog.d(TAG, "xsesServiceReceiver onReceive = " + intent.getAction());
                String action = intent.getAction();
                if (ACTION_CONNECTIVITY_CHANGE.equals(action)) {
                    NetworkInfo networkInfo =
                            intent.getParcelableExtra(EXTRA_NETWORK_INFO);
                    Slog.d(TAG, "onReceive extras= " + networkInfo);
                    if (networkInfo.isConnected() && !mNetRequestStarted) {
                        prepareReqest();
                    } else if (mNetRequestStarted) {
                        cancelNetRequestAlarm();
                    }
                    return;
                }
                if (ACTION_NET_REQUEST.equals(action)) {
                    realRequest();
                    return;
                }
                if (ACTION_BOOT_CHECK.equals(action)) {
                    checkRoot();
                    return;
                }
                if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                    prepareSecurityCheck();
                    keepPushStatus();
                    return;
                }
                if (ACTION_USB_STATE.equals(action)) {
                    doCheckUsbStatus(intent);
                    return;
                }
                if (ACTION_IM_SECURE.equals(action)) {
                    intent.getIntExtra(SECURE_IM_TYPE, -1);
                    String content = intent.getStringExtra(SECURE_IM_CONTENT);
                    if (content != null) {
                        Slog.d(TAG, " ACTION_IM_SECURE content = " + content);
                        processServerPush(content);
                    }
                }
            }
        };
        this.mInitModuleSwitchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                updateModuleSwitch();
            }
        };
        this.mContext = context;
    }

    private void cancelNetRequestAlarm() {
        Slog.d(TAG, "netRequestIntent cancel");
        this.mNetRequestStarted = false;
        this.mAlarmManager.cancel(netRequestIntent());
    }

    private void checkAPPStartRun() {
    }

    private void checkBVC() {
        Slog.d(TAG, "checkBVC start");
        boolean notException = ((PackageManagerService) ServiceManager.getService("package"))
                .getXseCheckDeStatus();
        Slog.d(TAG, "checkBVC end bl = " + notException);
        if (notException) {
            return;
        }
        ActivityManager.logSeException();
    }

    private void checkInstall() {
    }

    private void checkPFStatus() {
        boolean partitionFilesStatus = new VSManager().getPartitionFilesStatus();
        boolean acsre = SystemProperties.getBoolean("persist.sys.xtc.alxcse", false);
        Slog.d(TAG, "qcheckPFStatus PFStatus = " + partitionFilesStatus + ",acsre = " + acsre);
        if (!partitionFilesStatus || acsre) {
            ActivityManager.logSeException();
        }
    }

    /**
     * Root detection. See the class-level condemnation: flagging a user's own
     * device as "compromised" because they exercised control over it is
     * user-hostile.
     */
    private void checkRoot() {
        if (!sRootCheckSwitch) {
            Slog.d(TAG, "rootCheckSwitch is false!  checkRoot return");
            return;
        }
        boolean isRoot = isDRed();
        Slog.d(TAG, "checkRoot start isRoot = " + isRoot);
        if (isRoot) {
            saveRootInfo("device_root");
            new SystemExceptionDialog(this.mContext, "root_dialog").show();
        }
    }

    private static boolean checkRootMethod2() throws Throwable {
        Slog.d(TAG, "checkRootMethod2");
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("which su");
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (reader.readLine() == null) {
                return false;
            }
            Slog.d(TAG, "checkRootMethod2 su file is exist!");
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private void checkSystemStatus() {
        int phoneCompileStatus = TelephonyManager.getPhoneCompileStatus();
        Slog.d(TAG, "phoneCS is = " + phoneCompileStatus);
        if (phoneCompileStatus == 1) {
            new SystemExceptionManager(this.mContext).displaySPEUimode();
        }
        if (TelephonyManager.getSystemExceptionStatus()) {
            ActivityManager.logSeException();
        }
    }

    private void checkUsbStatus() {
        Slog.d(TAG, "checkUsbStatus adbCheckSwitch = " + sAdbCheckSwitch);
        if (adbFromNetwork) {
            Slog.d(TAG, "checkUsbStatus return adbFromNetwork");
            return;
        }
        if (sAdbCheckSwitch) {
            if (!Build.IS_USER) {
                Slog.d(TAG, "checkUsbStatus return not user version");
                return;
            }
            boolean inFactory = SystemProperties.getBoolean(PROP_ADB_PORT, true);
            Slog.d(TAG, "checkUsbStatus in Factory = " + inFactory);
            if (inFactory) {
                return;
            }
            String adbFrom = SystemProperties.get(PROP_ADB_ENABLE_FROM, "null");
            Slog.d(TAG, "checkUsbStatus adbFrom = " + adbFrom);
            if (adbFrom.equals(ADB_FROM_ALTER_SALE_ON)
                    || adbFrom.equals(ADB_FROM_SELF_TEST_ON)) {
                return;
            }
            String usbPersistStatus = SystemProperties.get(PERSIST_PROR, "null");
            Slog.d(TAG, "checkUsbStatus usbPersistStatus = " + usbPersistStatus);
            if (usbPersistStatus.contains("adb")) {
                disableAdbFromUsb(PERSIST_PROR, usbPersistStatus);
            }
            String usbSysStatus = SystemProperties.get(SYS_PROP, "null");
            Slog.d(TAG, "checkUsbStatus usbSysStatus = " + usbSysStatus);
            if (usbSysStatus.contains("adb")) {
                disableAdbFromUsb(SYS_PROP, usbSysStatus);
            }
            if (usbPersistStatus.contains("adb") || usbSysStatus.contains("adb")) {
                saveUsbInfo();
            }
        }
    }

    /**
     * Force-disables ADB. See the class-level condemnation: silently revoking
     * a user's debugging access to their own device is an unacceptable
     * restriction of user freedom.
     */
    private void disableAdbFromUsb(String key, String value) {
        String newStatus = "";
        if (value.equals("adb")) {
            newStatus = "charging";
        } else if (value.contains("adb,")) {
            newStatus = value.replace("adb,", "");
            if (newStatus.endsWith(",")) {
                newStatus = newStatus.substring(0, newStatus.length() - 1);
            }
        } else if (value.contains("adb")) {
            newStatus = value.replace("adb", "");
            if (newStatus.endsWith(",")) {
                newStatus = newStatus.substring(0, newStatus.length() - 1);
            }
        }
        if (newStatus == null || newStatus.isEmpty()) {
            return;
        }
        SystemProperties.set(key, newStatus);
        Slog.d(TAG, "the device is OPENED ADB without permission!!. u_New_Status " + newStatus);
    }

    private void doCheckUsbStatus(Intent intent) {
        Slog.d(TAG, "doCheckUsbStatus adbCheckSwitch = " + sAdbCheckSwitch);
        boolean adb = intent.getBooleanExtra(EXTRA_ADB, false);
        Slog.d(TAG, "doCheckUsbStatus function_adb = " + adb);
        if (adb) {
            checkBVC();
        }
        if (!sAdbCheckSwitch) {
            Slog.d(TAG, "adbCheckSwitch is false!  doCheckUsbStatus return");
            return;
        }
        Slog.d(TAG, "doCheckUsbStatus ACTION_USB_STATE = " + intent);
        boolean connected = intent.getBooleanExtra(EXTRA_CONNECTED, false);
        Slog.d(TAG, "doCheckUsbStatus connected = " + connected
                + ",lastUsbConnected = " + sLastUsbConnected);
        if (sLastUsbConnected) {
            Slog.d(TAG, "doCheckUsbStatus lastUsbConnected return");
            sLastUsbConnected = false;
        } else if (connected) {
            sLastUsbConnected = connected;
            if (adb) {
                checkUsbStatus();
            }
        }
    }

    private static boolean isDRed() {
        boolean inFactory = SystemProperties.getBoolean(PROP_ADB_PORT, true);
        Slog.d(TAG, "isDeviceRooted in Factory = " + inFactory);
        if (inFactory) {
            return false;
        }
        boolean suFile = xsesCR();
        Slog.d(TAG, "isDeviceRooted isSuFile = " + suFile);
        return suFile;
    }

    private void keepPushStatus() {
        SystemExceptionManager manager = new SystemExceptionManager(this.mContext);
        boolean isShowOverlay = SystemProperties.getBoolean(PROP_OVERLAY, false);
        Slog.d(TAG, "keepPushStatus isShowOverlay = " + isShowOverlay);
        if (isShowOverlay) {
            manager.showStrictModeOverlay(SystemExceptionManager.SCENE_DEF);
        }
    }

    private PendingIntent netRequestIntent() {
        return PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(ACTION_NET_REQUEST), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void prepareReqest() {
        new XsesRequestHelper(this.mContext);
        long elapsed = SystemClock.elapsedRealtime() - XsesRequestHelper.lastRequestedTime;
        if (XsesRequestHelper.lastRequestedTime != 0 && elapsed < NET_REQUEST_INTERVAL_MS) {
            Slog.d(TAG, "prepareReqest time RETURN");
            return;
        }
        Slog.d(TAG, "netRequestIntent start");
        this.mNetRequestStarted = true;
        this.mAlarmManager =
                (AlarmManager) this.mContext.getSystemService(Context.ALARM_SERVICE);
        this.mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + NET_REQUEST_DELAY_MS, netRequestIntent());
    }

    private void prepareSecurityCheck() {
        Slog.d(TAG, "prepareSecurityCheck start");
        this.mAlarmManager =
                (AlarmManager) this.mContext.getSystemService(Context.ALARM_SERVICE);
        this.mAlarmManager.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime()
                        + com.android.server.job.controllers.JobStatus.DEFAULT_TRIGGER_MAX_DELAY,
                securityCheckIntent());
    }

    private void processServerPush(String pushMessage) {
        if (pushMessage.isEmpty()) {
            Slog.d(TAG, "processServerPush pushMessage is null");
            return;
        }
        SystemExceptionManager manager = new SystemExceptionManager(this.mContext);
        boolean isJson = SystemExceptionManager.isJson(pushMessage);
        Slog.d(TAG, "processServerPush pushMessage isjson = " + isJson);
        if (isJson) {
            LinkedHashMap<String, String> map = SystemExceptionManager.jsonToMap(pushMessage);
            for (String key : map.keySet()) {
                String value = map.get(key).toString();
                Slog.d(TAG, "processServerPush key = " + key);
                if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DIALOG)) {
                    manager.messageDialog(value);
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_OVERLAY)) {
                    manager.showStrictModeOverlay(value);
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_UNINSTALL)) {
                    manager.doUninstall(value);
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_NET_APP)) {
                    manager.doDenyNetworkAccessApp(value);
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_NET_ADDRESS)) {
                    manager.denyNetworkAccessAddress(value);
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_APP)) {
                    manager.doDisableApp(value);
                }
            }
        }
    }

    private void realRequest() {
        new XsesRequestHelper(this.mContext).realRequest();
    }

    private void saveRootInfo(String status) {
        Slog.d(TAG, "saveRootInfo");
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("device_status", status);
        map.put("time", time);
        new XtcBehavior.Builder(this.mContext, "secure_device_root", map)
                .dataCollectLevel(XtcBehaviorDataLevel.A).build().sendData();
    }

    private void saveUsbInfo() {
        Slog.d(TAG, "saveUsbInfo");
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("adb_status", "isOpenByUser");
        map.put("time", time);
        new XtcBehavior.Builder(this.mContext, "secure_adb_status", map)
                .dataCollectLevel(XtcBehaviorDataLevel.A).build().sendData();
    }

    private void secLog(String message) {
        if (SystemProperties.getBoolean("log.security.service", false)) {
            Slog.d(TAG, message);
        }
    }

    private PendingIntent securityCheckIntent() {
        return PendingIntent.getBroadcast(this.mContext, 0,
                new Intent(ACTION_BOOT_CHECK), PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Searches well-known locations for root binaries / root manager apps.
     * Condemned: see the class-level note. Checked paths include SuperSU,
     * Magisk and MT Manager.
     */
    private static boolean xsesCR() {
        Slog.d(TAG, "xsesCR start");
        String[] paths = {
                "/system/app/Superuser.apk", "/sbin/su", "/su/bin/su", "/system/bin/su",
                "/system/xbin/su", "/data/data/com.topjohnwu.magisk", "/data/data/bin.mt.plus",
                "/data/data/eu.chainfire.supersu", "/data/local/xbin/su", "/data/local/bin/su",
                "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su",
                "/sdcard/MagiskManager"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onBootPhase(int phase) {
        Slog.d(TAG, "xsesService phase = " + phase);
        if (phase == 550 && isDRed() && Build.IS_USER) {
            ((PowerManager) this.mContext.getSystemService(Context.POWER_SERVICE))
                    .reboot("xsebootloader");
        }
        if (phase == 600) {
            checkPFStatus();
        }
        if (phase == 1000) {
            checkSystemStatus();
        }
    }

    @Override
    public void onStart() {
        Slog.d(TAG, "xsesService start");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CONNECTIVITY_CHANGE);
        filter.addAction(ACTION_NET_REQUEST);
        filter.addAction(ACTION_BOOT_CHECK);
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(ACTION_USB_STATE);
        this.mContext.registerReceiver(this.mXsesServiceReceiver, filter);

        IntentFilter moduleFilter = new IntentFilter();
        moduleFilter.addAction(ACTION_MODULE_SWITCH_INIT);
        this.mContext.registerReceiver(this.mInitModuleSwitchReceiver, moduleFilter);

        updateModuleSwitch();
        this.mContext.registerReceiver(this.mXsesServiceReceiver,
                new IntentFilter(ACTION_IM_SECURE), SECURE_PERM, null);
    }

    public void updateModuleSwitch() {
        sRootCheckSwitch = ModuleSwitch.queryModuleSwitch(this.mContext, MODULE_ROOT_CHECK, true);
        sAdbCheckSwitch = ModuleSwitch.queryModuleSwitch(this.mContext, MODULE_ADB_CHECK, false);
        adbFromNetwork = ModuleSwitch.queryModuleSwitch(this.mContext,
                MODULE_ADB_FROM_NETWORK, false);
        runCheckSwitch = ModuleSwitch.queryModuleSwitch(this.mContext, MODULE_RUN_CHECK, true);
        netWhiteSwitch = ModuleSwitch.queryModuleSwitch(this.mContext, MODULE_NET_WHITE, false);
        serverControlPolicy = ModuleSwitch.queryModuleSwitch(this.mContext,
                MODULE_SERVER_CONTROL, false);
        reportNetAddrSwitch = ModuleSwitch.queryModuleSwitch(this.mContext,
                MODULE_REPORT_NET_ADDR, true);
        Slog.d(TAG, "updateModuleSwitch rootCheckSwitch = " + sRootCheckSwitch
                + ",adbCheckSwitch = " + sAdbCheckSwitch + ",netWhiteSwitch= " + netWhiteSwitch);
        if (Build.IS_USER) {
            if (!adbFromNetwork) {
                Slog.d(TAG, "updateModuleSwitch setDePermissions false");
                Build.setDePermissions(false);
                if (SystemProperties.getBoolean(PROP_DEV_SW, false)) {
                    SystemProperties.set(PROP_DEV_SW, "false");
                }
            } else if (!Build.isHasDePermissions()) {
                Slog.d(TAG, "updateModuleSwitch setDePermissions true");
                Build.setDePermissions(true);
                SystemProperties.set(PROP_DEV_SW, "true");
            }
        }
        if (sAdbCheckSwitch) {
            checkUsbStatus();
        }
    }
}