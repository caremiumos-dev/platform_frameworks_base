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

package com.android.server.xtclog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.StatFs;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.util.Log;
import android.view.IWindowManager;

import com.android.server.SystemService;
import com.android.server.job.controllers.JobStatus;
import com.android.server.policy.PhoneWindowManager;
import com.xtc.utils.IXTCUtilsManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

/**
 * XTC logging/diagnostics service. Hosts the {@code XTCUtilsService} binder,
 * which exposes ADB/bluetooth/battery/density controls to system apps.
 *
 * <p><b>Strong condemnation.</b> This service is the control hub for the
 * vendor's ADB lock-down ({@link XTCAdbControl}), super-log suppression,
 * remote network analysis/tcpdump capture and eMMC wear telemetry. It lets the
 * vendor remotely decide whether the device owner may use ADB and silently
 * captures network traffic. These capabilities are abusive and only
 * reproduced for behavioural compatibility.
 *
 * @hide
 */
public class XTCLogService extends SystemService {
    static final String TAG = "XTCLogService";
    public static final boolean DEBUG = true;

    public static final String ACTION_TCPDUMP_TRIGGERED = "com.android.server.xtclog.TCPDUMP_TRIGGERED";
    public static final String ACTION_SHUTDOWN_TRIGGERED =
            "com.android.server.xtclog.SHUTDOWN_TRIGGERED";
    public static final String ACTION_CLOSE_BLUETOOTH = "com.android.server.xtclog.BLUETOOTH_CLOSE";
    public static final String ACTION_BLUETOOTH_DELAY_CLOSE = "com.xtc.bluetooth.ACTION_DELAY_CLOSE";
    public static final String ACTION_BATTERY_WARNING =
            "com.android.server.xtclog.BATTERY_WARNING";
    public static final String ACTION_START_NETWORK_ANALYSIS =
            "com.android.server.xtclog.START_NETWORK_ANALYSIS";
    public static final String ACTION_END_NETWORK_ANALYSIS =
            "com.android.server.xtclog.END_NETWORK_ANALYSIS";
    public static final String ACTION_POWER_CONNECTED =
            "android.intent.action.ACTION_POWER_CONNECTED";

    private static final String TCP_DUMP_SERVICE = "tcp_dump";
    private static final String CMD_READ_CURRENT = "cat ./sys/class/power_supply/battery/current_now";

    private static final int MSG_TCPDUMP_TRIGGERED = 1;
    private static final int MSG_START_TCPDUMP_DELAY = 2;
    private static final int MSG_READ_CURRENT = 3;
    private static final int MSG_CLOSE_BLUETOOTH = 4;
    private static final int MSG_BATTERY_WARNING = 5;
    private static final int MSG_MODULE_INIT = 6;
    private static final int MSG_START_NETWORK_ANALYSIS = 7;
    private static final int MSG_END_NETWORK_ANALYSIS = 8;
    private static final int MSG_FACTORY_RESET_SHUTDOWN = 9;

    private static final long DELAY_TIME_CLOSE_BLUETOOTH = 120000L;
    private static final long MAX_DELAY_TIME_CLOSE_BLUETOOTH = 1800000L;
    private static final int BLUETOOTH_AUTO_CLOSE_DISABLE = 0;
    private static final int BLUETOOTH_AUTO_CLOSE_ENABLE = 1;
    private static final int BLUETOOTH_AUTO_CLOSE_UNDISCOVERABLE = 2;
    private static final int INTERVAL_READ_CURRENT = 1000;
    private static final int MAX_READ_CURRENT_COUNT = 30;
    private static final int MAX_AVERAGE_CURRENT_RECORD = 100;
    private static final long INTERVAL_24 = 86400000L;
    private static final long ONE_MEGABYTE = 1048576L;

    private static final int BLUETOOTH_STATE_OFF = 10;
    private static final int BLUETOOTH_STATE_ON = 12;
    private static final int BLUETOOTH_NO_CONNECTION = 0;

    private static final String PROP_BT_AUTO_CLOSE_POLICY = "persist.sys.btac.policy";
    private static final String PROP_FACTORY_RESET = "sys.xtc.factory_reset";

    private static final Object sBatteryCurrentTestLock = new Object();
    private static PowerManager.WakeLock sKeepAwakeLock;

    private AlarmManager mAlarmManager;
    private final LinkedList<String> mAverageCurrentDateQueue = new LinkedList<>();
    private final LinkedList<String> mAverageCurrentValueQueue = new LinkedList<>();
    private boolean mBatteryCurrentTestEnable = false;
    private PowerManager.WakeLock mBatteryCurrentTestWakeLock;
    private long mBluetoothAutoCloseTime = DELAY_TIME_CLOSE_BLUETOOTH;
    private int mBluetoothState = Integer.MIN_VALUE;
    private final BroadcastReceiver mBroadcastReceiver;
    private PendingIntent mCloseBtPendingIntent;
    private int mCurrentSum;
    private LogHandler mHandler;
    private final Object mLock = new Object();
    private PowerManager.WakeLock mNetworkAnalysisWakeLock;
    private PowerManager mPowerManager;
    private int mReadCurrentCount;
    private XTCUtilsService mXTCUtilsService;

    private class LogHandler extends Handler {
        LogHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_TCPDUMP_TRIGGERED: {
                    boolean start = (Boolean) message.obj;
                    Log.d(TAG, "Process cmd start = " + start);
                    mHandler.removeMessages(MSG_START_TCPDUMP_DELAY);
                    if (start) {
                        if (android.os.SystemService.isRunning(TCP_DUMP_SERVICE)) {
                            Log.w(TAG, "tcp_dump is already running, stop and restart later.");
                            SystemProperties.set("ctl.stop", TCP_DUMP_SERVICE);
                            mHandler.sendEmptyMessageDelayed(MSG_START_TCPDUMP_DELAY, 1000L);
                        } else {
                            SystemProperties.set("ctl.start", TCP_DUMP_SERVICE);
                        }
                    } else if (android.os.SystemService.isRunning(TCP_DUMP_SERVICE)) {
                        SystemProperties.set("ctl.stop", TCP_DUMP_SERVICE);
                    } else {
                        Log.w(TAG, "tcp_dump is not running");
                    }
                    Log.d(TAG, "Process cmd done ");
                    return;
                }
                case MSG_START_TCPDUMP_DELAY:
                    SystemProperties.set("ctl.start", TCP_DUMP_SERVICE);
                    return;
                case MSG_READ_CURRENT:
                    readBatteryCurrent();
                    return;
                case MSG_CLOSE_BLUETOOTH:
                    handleCloseBluetoothIfNeed();
                    return;
                case MSG_BATTERY_WARNING:
                    if (getSDCardFreeSize() > 300L) {
                        Log.e(TAG, "start batterywarning....");
                        SystemProperties.set("ctl.start", "batterywarning");
                        mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "batterywarning")
                                .acquire(60000L);
                    }
                    return;
                case MSG_MODULE_INIT:
                    new XTCEmmcControlLog(getContext()).doControlLog();
                    new XTCSuperLogControl(getContext()).doSuperLogControl();
                    new XTCAdbControl(getContext()).doAdbControl();
                    new XTCMcuControlLog(getContext()).doMcuControl();
                    new XTCCatonControl(getContext()).doCatonControl();
                    new XTCMachineTypeControl(getContext()).doMachineTypeControl();
                    new XTCAudioControl(getContext()).doAudioControl();
                    new XTCSystraceControl(getContext()).doSystraceControl();
                    return;
                case MSG_START_NETWORK_ANALYSIS:
                    if (getSDCardFreeSize() > 300L) {
                        Log.e(TAG, "start networkanalysis....");
                        SystemProperties.set("ctl.start", "networkanalysis");
                        mNetworkAnalysisWakeLock = mPowerManager.newWakeLock(
                                PowerManager.PARTIAL_WAKE_LOCK, "networkanalysis");
                        mNetworkAnalysisWakeLock.acquire(60000L);
                    }
                    return;
                case MSG_END_NETWORK_ANALYSIS:
                    if (mNetworkAnalysisWakeLock != null) {
                        if (mNetworkAnalysisWakeLock.isHeld()) {
                            mNetworkAnalysisWakeLock.release();
                        }
                        mNetworkAnalysisWakeLock = null;
                    }
                    return;
                case MSG_FACTORY_RESET_SHUTDOWN:
                    if (SystemProperties.getBoolean(PROP_FACTORY_RESET, false)) {
                        mPowerManager.shutdown(false, "xtc factory reset", false);
                    }
                    return;
                default:
                    Log.w(TAG, "Invalid message: " + message);
                    return;
            }
        }
    }

    private final class XTCUtilsService extends IXTCUtilsManager.Stub {
        @Override
        public int checkVersionDowngrade(String version) {
            synchronized (mLock) {
                int callingUid = Binder.getCallingUid();
                if (callingUid != android.os.Process.SYSTEM_UID) {
                    Log.w(TAG, "Non-system Uid: " + callingUid
                            + " tried to call checkVersionDowngrade, do nothing.");
                    return 0;
                }
                Log.d(TAG, "checkVersionDowngrade version = " + version);
                return checkVersionDowngradeNative(version);
            }
        }

        @Override
        public void clearDisplayDensity() {
            IWindowManager wm = IWindowManager.Stub.asInterface(
                    ServiceManager.checkService(Context.WINDOW_SERVICE));
            if (wm == null) {
                Log.w(TAG, "Can't connect to window manager; is the system running?");
                return;
            }
            try {
                wm.clearForcedDisplayDensityForUser(0, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void disableBatteryCurrentTestEnable() {
            if (!mBatteryCurrentTestEnable) {
                Log.d(TAG, "BatteryCurrentTest is already disable");
                return;
            }
            mBatteryCurrentTestEnable = false;
            mHandler.removeMessages(MSG_READ_CURRENT);
            if (mBatteryCurrentTestWakeLock != null) {
                if (mBatteryCurrentTestWakeLock.isHeld()) {
                    mBatteryCurrentTestWakeLock.release();
                }
                mBatteryCurrentTestWakeLock = null;
            }
        }

        @Override
        public void enableBatteryCurrentTestEnable(boolean acquireWakeLock) {
            if (mBatteryCurrentTestEnable) {
                Log.d(TAG, "BatteryCurrentTest is already enable");
                return;
            }
            mBatteryCurrentTestEnable = true;
            mReadCurrentCount = 0;
            mCurrentSum = 0;
            mAverageCurrentValueQueue.clear();
            mAverageCurrentDateQueue.clear();
            mHandler.sendEmptyMessage(MSG_READ_CURRENT);
            if (acquireWakeLock) {
                if (mBatteryCurrentTestWakeLock != null) {
                    if (!mBatteryCurrentTestWakeLock.isHeld()) {
                        mBatteryCurrentTestWakeLock.acquire();
                    }
                } else {
                    mBatteryCurrentTestWakeLock = mPowerManager.newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK, "BatteryCurrentTest");
                    mBatteryCurrentTestWakeLock.acquire();
                }
            }
        }

        @Override
        public String getBatteryCurrentTestRecord() {
            StringBuilder builder = new StringBuilder();
            synchronized (sBatteryCurrentTestLock) {
                for (int i = 0; i < mAverageCurrentValueQueue.size(); i++) {
                    builder.append(mAverageCurrentDateQueue.get(i)).append("  ")
                            .append(mAverageCurrentValueQueue.get(i));
                    builder.append("\n");
                }
            }
            return builder.toString();
        }

        @Override
        public int getBluetoothAutoClosePolicy() {
            return SystemProperties.getInt(PROP_BT_AUTO_CLOSE_POLICY, BLUETOOTH_AUTO_CLOSE_ENABLE);
        }

        @Override
        public long getBluetoothAutoCloseTime() {
            return mBluetoothAutoCloseTime;
        }

        @Override
        public boolean getMobileDataEnabled() {
            return android.telephony.TelephonyManager.from(getContext()).getDataEnabled();
        }

        @Override
        public String getSecurityKey(int type, String name) {
            synchronized (mLock) {
                int callingUid = Binder.getCallingUid();
                if (callingUid == android.os.Process.SYSTEM_UID) {
                    return getSecurityKeyFromXtcInfo(type, name);
                }
                Log.w(TAG, "Non-system Uid: " + callingUid
                        + " tried to call getSecurityKey, just return null.");
                return null;
            }
        }

        @Override
        public boolean isBatteryCurrentTestEnable() {
            return mBatteryCurrentTestEnable;
        }

        @Override
        public boolean isBatteryCurrentTestEnableWithLock() {
            if (mBatteryCurrentTestWakeLock != null) {
                return mBatteryCurrentTestWakeLock.isHeld();
            }
            return false;
        }

        @Override
        public boolean isKeepSystemAwake() {
            if (sKeepAwakeLock != null) {
                return sKeepAwakeLock.isHeld();
            }
            return false;
        }

        @Override
        public boolean isRoundedCornerScreen() {
            return SystemProperties.getBoolean("persist.sys.roundedcorner", false);
        }

        @Override
        public boolean isSecurityKeyManagerSupported() {
            int callingUid = Binder.getCallingUid();
            if (callingUid == android.os.Process.SYSTEM_UID) {
                return true;
            }
            Log.w(TAG, "Non-system Uid: " + callingUid
                    + " tried to call isSecurityKeyManagerSupported, just return false.");
            return false;
        }

        @Override
        public boolean isSupportChangeDisplayDensity() {
            return true;
        }

        @Override
        public void setBluetoothAutoClosePolicy(int policy) {
            SystemProperties.set(PROP_BT_AUTO_CLOSE_POLICY, String.valueOf(policy));
        }

        @Override
        public void setBluetoothAutoCloseTime(long time) {
            handleCloseBluetoothDelay(time);
        }

        @Override
        public void setDisplayDensity(int density, boolean isDefault) {
            IWindowManager wm = IWindowManager.Stub.asInterface(
                    ServiceManager.checkService(Context.WINDOW_SERVICE));
            if (wm == null) {
                Log.w(TAG, "Can't connect to window manager; is the system running?");
                return;
            }
            try {
                // The AOSP 8.1 interface has no temporary-density call, so the
                // persistent call is used for both cases.
                wm.setForcedDisplayDensityForUser(0, density, UserHandle.USER_CURRENT);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void setKeepSystemAwake(boolean keepAwake) {
            if (sKeepAwakeLock == null) {
                sKeepAwakeLock = mPowerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE,
                        "KeepSystemAwake");
                sKeepAwakeLock.setReferenceCounted(false);
            }
            if (keepAwake) {
                sKeepAwakeLock.acquire();
            } else {
                sKeepAwakeLock.release();
            }
        }

        @Override
        public void setMobileDataEnabled(boolean enabled) {
            android.telephony.TelephonyManager.from(getContext()).setDataEnabled(enabled);
        }

        @Override
        public boolean setSecurityKey(String key) {
            synchronized (mLock) {
                int callingUid = Binder.getCallingUid();
                if (callingUid != android.os.Process.SYSTEM_UID) {
                    Log.w(TAG, "Non-system Uid: " + callingUid
                            + " tried to call setSecurityKey, do nothing.");
                    return false;
                }
                Log.d(TAG, "setSecurityKey key = " + key);
                return setSecurityKeyToXtcInfo(key);
            }
        }
    }

    public XTCLogService(Context context) {
        super(context);
        this.mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Receieved: " + action);
                if (ACTION_TCPDUMP_TRIGGERED.equals(action)) {
                    mHandler.obtainMessage(MSG_TCPDUMP_TRIGGERED,
                            intent.getBooleanExtra("start", false)).sendToTarget();
                    return;
                }
                if (ACTION_SHUTDOWN_TRIGGERED.equals(action)) {
                    mPowerManager.shutdown(false,
                            intent.getStringExtra(PhoneWindowManager.SYSTEM_DIALOG_REASON_KEY),
                            false);
                    return;
                }
                if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    handleBluetoothStateChanged(intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE, Integer.MIN_VALUE));
                    return;
                }
                if (ACTION_CLOSE_BLUETOOTH.equals(action)) {
                    mHandler.sendEmptyMessage(MSG_CLOSE_BLUETOOTH);
                    return;
                }
                if (ACTION_BLUETOOTH_DELAY_CLOSE.equals(action)) {
                    handleCloseBluetoothDelay(intent.getLongExtra("time",
                            DELAY_TIME_CLOSE_BLUETOOTH));
                    return;
                }
                if (ACTION_BATTERY_WARNING.equals(action)) {
                    mHandler.sendEmptyMessage(MSG_BATTERY_WARNING);
                    return;
                }
                if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
                    Log.d(TAG, "setBatteryWarningAlarm ");
                    setBatteryWarningAlarm();
                    mHandler.sendEmptyMessageDelayed(MSG_FACTORY_RESET_SHUTDOWN,
                            JobStatus.DEFAULT_TRIGGER_UPDATE_DELAY);
                } else if ("com.xtc.launcher.moduleswitch.INIT_ACTION".equals(action)) {
                    Log.d(TAG, "ACTION_LAUNCHER_MODULE_INIT");
                    mHandler.removeMessages(MSG_MODULE_INIT);
                    mHandler.obtainMessage(MSG_MODULE_INIT).sendToTarget();
                } else if (ACTION_START_NETWORK_ANALYSIS.equals(action)
                        || ACTION_POWER_CONNECTED.equals(action)) {
                    mHandler.sendEmptyMessage(MSG_START_NETWORK_ANALYSIS);
                } else if (ACTION_END_NETWORK_ANALYSIS.equals(action)) {
                    mHandler.sendEmptyMessage(MSG_END_NETWORK_ANALYSIS);
                }
            }
        };
    }

    private void cancelCloseBluetoothAlarm() {
        Log.d(TAG, "cancelCloseBluetoothAlarm");
        mAlarmManager.cancel(mCloseBtPendingIntent);
    }

    private static native int checkVersionDowngradeNative(String version);

    public static String exec(String cmd) throws IOException {
        Process process = Runtime.getRuntime().exec(cmd);
        StringBuilder builder = new StringBuilder("");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append("\n");
            }
        }
        try {
            if (process.waitFor() != 0) {
                Log.e(TAG, "exec (" + cmd + ") error, exit = " + process.exitValue());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    private static native String getSecurityKeyFromXtcInfo(int type, String name);

    private void handleBluetoothStateChanged(int state) {
        int policy = SystemProperties.getInt(PROP_BT_AUTO_CLOSE_POLICY,
                BLUETOOTH_AUTO_CLOSE_ENABLE);
        Log.d(TAG, "handleBluetoothStateChanged state = " + state + ", policy = " + policy);
        mBluetoothState = state;
        if (policy == BLUETOOTH_AUTO_CLOSE_DISABLE) {
            cancelCloseBluetoothAlarm();
        } else if (mBluetoothState == BLUETOOTH_STATE_ON) {
            setCloseBluetoothAlarm(DELAY_TIME_CLOSE_BLUETOOTH);
        } else if (mBluetoothState == BLUETOOTH_STATE_OFF) {
            cancelCloseBluetoothAlarm();
        }
    }

    private void handleCloseBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int policy = SystemProperties.getInt(PROP_BT_AUTO_CLOSE_POLICY,
                BLUETOOTH_AUTO_CLOSE_ENABLE);
        Log.d(TAG, "handleCloseBluetooth policy = " + policy);
        if (policy == BLUETOOTH_AUTO_CLOSE_ENABLE) {
            adapter.disable(true);
        } else if (policy == BLUETOOTH_AUTO_CLOSE_UNDISCOVERABLE) {
            adapter.setDiscoverableTimeout(1);
            adapter.setScanMode(20, 1);
        }
    }

    private void handleCloseBluetoothDelay(long delayTime) {
        Log.d(TAG, "handleCloseBluetoothDelay state = " + mBluetoothState
                + ", delayTime = " + delayTime);
        if (mBluetoothState != BLUETOOTH_STATE_ON || delayTime <= 0) {
            return;
        }
        cancelCloseBluetoothAlarm();
        setCloseBluetoothAlarm(delayTime);
    }

    private void handleCloseBluetoothIfNeed() {
        int connectionState = BluetoothAdapter.getDefaultAdapter().getConnectionState();
        Log.d(TAG, "handleCloseBluetoothIfNeed state = " + connectionState);
        if (connectionState == BLUETOOTH_NO_CONNECTION) {
            handleCloseBluetooth();
        } else {
            setCloseBluetoothAlarm(DELAY_TIME_CLOSE_BLUETOOTH);
        }
    }

    private void readBatteryCurrent() {
        try {
            String value = exec(CMD_READ_CURRENT).trim();
            if (mBatteryCurrentTestEnable) {
                mHandler.sendEmptyMessageDelayed(MSG_READ_CURRENT, INTERVAL_READ_CURRENT);
            }
            try {
                int current = Integer.parseInt(value) / 1000;
                mReadCurrentCount++;
                mCurrentSum = current + mCurrentSum;
                if (mReadCurrentCount >= MAX_READ_CURRENT_COUNT) {
                    int average = mCurrentSum / mReadCurrentCount;
                    String date = new SimpleDateFormat("MM-dd_HH:mm:ss")
                            .format(new Date(System.currentTimeMillis()));
                    synchronized (sBatteryCurrentTestLock) {
                        if (mAverageCurrentValueQueue.size() >= MAX_AVERAGE_CURRENT_RECORD) {
                            mAverageCurrentValueQueue.poll();
                        }
                        if (mAverageCurrentDateQueue.size() >= MAX_AVERAGE_CURRENT_RECORD) {
                            mAverageCurrentDateQueue.poll();
                        }
                        String valueStr = String.format(Locale.ENGLISH, "%d mA", average);
                        mAverageCurrentValueQueue.add(valueStr);
                        mAverageCurrentDateQueue.add(date);
                        Log.d(TAG, "Current average = " + valueStr + ", date = " + date);
                    }
                    mCurrentSum = 0;
                    mReadCurrentCount = 0;
                }
            } catch (NumberFormatException e) {
                Log.w(TAG, "fail to read current : " + value);
            }
        } catch (IOException e) {
            Log.e(TAG, "fail to read current", e);
        }
    }

    private void setCloseBluetoothAlarm(long delayTime) {
        if (delayTime < DELAY_TIME_CLOSE_BLUETOOTH) {
            delayTime = DELAY_TIME_CLOSE_BLUETOOTH;
        } else if (delayTime > MAX_DELAY_TIME_CLOSE_BLUETOOTH) {
            delayTime = MAX_DELAY_TIME_CLOSE_BLUETOOTH;
        }
        Log.d(TAG, "setCloseBluetoothAlarm delayTime = " + delayTime);
        mBluetoothAutoCloseTime = delayTime;
        mAlarmManager.setExact(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + mBluetoothAutoCloseTime, mCloseBtPendingIntent);
    }

    private static native boolean setSecurityKeyToXtcInfo(String key);

    long getSDCardFreeSize() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return 0L;
        }
        StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
        long blockSize = statFs.getBlockSize();
        statFs.getBlockCount();
        long availableBlocks = statFs.getAvailableBlocks();
        long size = ((availableBlocks * blockSize) / ONE_MEGABYTE) / ONE_MEGABYTE;
        Log.d(TAG, ",free_size:" + size + "M");
        return size;
    }

    @Override
    public void onBootPhase(int phase) {
        Log.d(TAG, "onBootPhase: " + phase);
        if (phase == 500) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_TCPDUMP_TRIGGERED);
            filter.addAction(ACTION_SHUTDOWN_TRIGGERED);
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(ACTION_CLOSE_BLUETOOTH);
            filter.addAction(ACTION_BLUETOOTH_DELAY_CLOSE);
            filter.addAction("com.xtc.launcher.moduleswitch.INIT_ACTION");
            getContext().registerReceiverAsUser(mBroadcastReceiver, UserHandle.ALL, filter,
                    null, null);
            mCloseBtPendingIntent = PendingIntent.getBroadcast(getContext(), 1001,
                    new Intent(ACTION_CLOSE_BLUETOOTH), PendingIntent.FLAG_UPDATE_CURRENT);
            filter.addAction(ACTION_BATTERY_WARNING);
            filter.addAction(Intent.ACTION_BOOT_COMPLETED);
            filter.addAction(ACTION_START_NETWORK_ANALYSIS);
            filter.addAction(ACTION_END_NETWORK_ANALYSIS);
            filter.addAction(ACTION_POWER_CONNECTED);
            getContext().registerReceiverAsUser(mBroadcastReceiver, UserHandle.ALL, filter,
                    null, null);
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread("XTCLog Handler Thread");
            thread.start();
            mHandler = new LogHandler(thread.getLooper());
        }
        mPowerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        mAlarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        mXTCUtilsService = new XTCUtilsService();
        publishBinderService("XTCUtilsService", mXTCUtilsService);
    }

    void setBatteryWarningAlarm() {
        PendingIntent broadcast = PendingIntent.getBroadcast(getContext(), 0,
                new Intent(ACTION_BATTERY_WARNING), PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager alarmManager =
                (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 20);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (System.currentTimeMillis() > calendar.getTimeInMillis()) {
            calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) + 1);
        }
        Log.v(TAG, "BATTERY_WARNING_TIME=" + calendar.getTimeInMillis() + "");
        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                INTERVAL_24, broadcast);
    }
}