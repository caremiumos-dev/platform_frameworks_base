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

package com.android.internal.apm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.BatteryStats;
import android.os.MemoryFile;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.util.Log;

import com.android.internal.app.IBatteryStats;
import com.android.internal.os.AudioPowerCalculator;
import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;
import com.android.internal.os.BatteryStatsImpl;
import com.android.internal.os.BluetoothPowerCalculator;
import com.android.internal.os.CameraPowerCalculator;
import com.android.internal.os.CpuPowerCalculator;
import com.android.internal.os.MobileRadioPowerCalculator;
import com.android.internal.os.PowerCalculator;
import com.android.internal.os.PowerProfile;
import com.android.internal.os.SensorPowerCalculator;
import com.android.internal.os.VideoPowerCalculator;
import com.android.internal.os.WakelockPowerCalculator;
import com.android.internal.os.WifiPowerCalculator;
import com.android.internal.os.WifiPowerEstimator;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

/**
 * Reads the platform battery stats and computes per-package power usage for
 * the application performance monitor.
 *
 * @hide
 */
public class BatteryMonitor {
    private static final String TAG = "BatteryMonitor";

    private final Context mContext;
    private final Object mLock = new Object();
    private final Object mLevelLock = new Object();
    private final HashMap<String, BatterySipper> mPkgBatterySipperMaps = new HashMap<>();

    private final IBatteryStats mBatteryStatsService =
            IBatteryStats.Stub.asInterface(ServiceManager.getService(BatteryStats.SERVICE_NAME));

    private PowerProfile mPowerProfile;
    private BatteryStatsImpl mBatteryStats;
    private CpuPowerCalculator mCpuPowerCalculator;
    private WakelockPowerCalculator mWakelockPowerCalculator;
    private MobileRadioPowerCalculator mMobileRadioPowerCalculator;
    private PowerCalculator mWifiPowerCalculator;
    private PowerCalculator mBluetoothPowerCalculator;
    private SensorPowerCalculator mSensorPowerCalculator;
    private CameraPowerCalculator mCameraPowerCalculator;
    private AudioPowerCalculator mAudioPowerCalculator;
    private VideoPowerCalculator mVideoPowerCalculator;
    private boolean mHasWifiPowerReporting = false;
    private boolean mHasBluetoothPowerReporting = false;
    private int mStatsType = BatteryStats.STATS_SINCE_CHARGED;
    private int mBatteryLevel;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                synchronized (mLevelLock) {
                    mBatteryLevel = intent.getIntExtra("level", 0);
                }
            }
        }
    };

    public BatteryMonitor(Context context) {
        mContext = context;
        mPowerProfile = new PowerProfile(context);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mBroadcastReceiver, filter);
    }

    public boolean createBatteryStats() {
        boolean success = false;
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (mLock) {
            try {
                final ParcelFileDescriptor statisticsStream = mBatteryStatsService
                        .getStatisticsStream();
                if (statisticsStream != null) {
                    try (ParcelFileDescriptor.AutoCloseInputStream in =
                            new ParcelFileDescriptor.AutoCloseInputStream(statisticsStream)) {
                        byte[] fully = readFully(in,
                                MemoryFile.getSize(statisticsStream.getFileDescriptor()));
                        Parcel parcel = Parcel.obtain();
                        parcel.unmarshall(fully, 0, fully.length);
                        parcel.setDataPosition(0);
                        mBatteryStats = BatteryStatsImpl.CREATOR.createFromParcel(parcel);
                        success = true;
                    }
                }
            } catch (RemoteException e) {
                Log.w(TAG, "Unable to read statistics stream", e);
            } catch (IOException e) {
                Log.w(TAG, "Unable to read statistics stream", e);
            }
        }
        APMDebugLog.d(TAG, "createBatteryStats success = " + success + ", used = "
                + (System.currentTimeMillis() - currentTimeMillis) + "ms");
        return success;
    }

    public void destroyBatteryStats() {
        APMDebugLog.d(TAG, "destroyBatteryStats");
        synchronized (mLock) {
            mBatteryStats = null;
        }
    }

    public int getBatteryLevel() {
        synchronized (mLevelLock) {
            return mBatteryLevel;
        }
    }

    public void processAppUsage(APMGeneralData generalData) {
        long currentTimeMillis = System.currentTimeMillis();
        synchronized (mLock) {
            if (mBatteryStats == null) {
                Log.w(TAG, "mBatteryStats is null");
                return;
            }
            final long elapsedRealtime = SystemClock.elapsedRealtime() * 1000;
            final long uptime = SystemClock.uptimeMillis() * 1000;

            if (mCpuPowerCalculator == null) {
                mCpuPowerCalculator = new CpuPowerCalculator(mPowerProfile);
            }
            mCpuPowerCalculator.reset();
            if (mWakelockPowerCalculator == null) {
                mWakelockPowerCalculator = new WakelockPowerCalculator(mPowerProfile);
            }
            mWakelockPowerCalculator.reset();
            if (mMobileRadioPowerCalculator == null) {
                mMobileRadioPowerCalculator = new MobileRadioPowerCalculator(mPowerProfile,
                        mBatteryStats);
            }
            mMobileRadioPowerCalculator.reset(mBatteryStats);
            final boolean checkHasWifiPowerReporting =
                    checkHasWifiPowerReporting(mBatteryStats, mPowerProfile);
            if (mWifiPowerCalculator == null
                    || checkHasWifiPowerReporting != mHasWifiPowerReporting) {
                mWifiPowerCalculator = checkHasWifiPowerReporting
                        ? new WifiPowerCalculator(mPowerProfile)
                        : new WifiPowerEstimator(mPowerProfile);
                mHasWifiPowerReporting = checkHasWifiPowerReporting;
            }
            mWifiPowerCalculator.reset();
            final boolean checkHasBluetoothPowerReporting =
                    checkHasBluetoothPowerReporting(mBatteryStats, mPowerProfile);
            if (mBluetoothPowerCalculator == null
                    || checkHasBluetoothPowerReporting != mHasBluetoothPowerReporting) {
                mBluetoothPowerCalculator = new BluetoothPowerCalculator(mPowerProfile);
                mHasBluetoothPowerReporting = checkHasBluetoothPowerReporting;
            }
            mBluetoothPowerCalculator.reset();
            if (mSensorPowerCalculator == null) {
                mSensorPowerCalculator = new SensorPowerCalculator(mPowerProfile,
                        (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE));
            }
            mSensorPowerCalculator.reset();
            if (mCameraPowerCalculator == null) {
                mCameraPowerCalculator = new CameraPowerCalculator(mPowerProfile);
            }
            mCameraPowerCalculator.reset();
            if (mAudioPowerCalculator == null) {
                mAudioPowerCalculator = new AudioPowerCalculator(mPowerProfile);
            }
            mAudioPowerCalculator.reset();
            if (mVideoPowerCalculator == null) {
                mVideoPowerCalculator = new VideoPowerCalculator(mPowerProfile);
            }
            mVideoPowerCalculator.reset();

            try {
                BatteryStatsImpl.Uid uidStats =
                        mBatteryStats.getUidStatsLocked(mContext.getPackageManager()
                                .getPackageUid(generalData.getPackageName(), 0));
                BatterySipper batterySipper = new BatterySipper(BatterySipper.DrainType.APP,
                        uidStats, 0.0d);
                mCpuPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mWakelockPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mMobileRadioPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mWifiPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mBluetoothPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mSensorPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mCameraPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mAudioPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);
                mVideoPowerCalculator.calculateApp(batterySipper, uidStats, elapsedRealtime,
                        uptime, mStatsType);

                BatteryMonitorData data = generalData.getBatteryMonitorData();
                BatterySipper previous = mPkgBatterySipperMaps.get(generalData.getPackageName());
                if (previous != null) {
                    data.setCpuTimeMs(batterySipper.cpuTimeMs - previous.cpuTimeMs);
                    data.setCpuFgTimeMs(batterySipper.cpuFgTimeMs - previous.cpuFgTimeMs);
                    data.setCpuPowerMah(batterySipper.cpuPowerMah - previous.cpuPowerMah);
                    data.setGpsTimeMs(batterySipper.gpsTimeMs - previous.gpsTimeMs);
                    data.setGpsPowerMah(batterySipper.gpsPowerMah - previous.gpsPowerMah);
                    data.setWifiRxBytes(batterySipper.wifiRxBytes - previous.wifiRxBytes);
                    data.setWifiTxBytes(batterySipper.wifiTxBytes - previous.wifiTxBytes);
                    data.setWifiRunningTimeMs(
                            batterySipper.wifiRunningTimeMs - previous.wifiRunningTimeMs);
                    data.setWifiPowerMah(batterySipper.wifiPowerMah - previous.wifiPowerMah);
                    data.setMobileRxBytes(batterySipper.mobileRxBytes - previous.mobileRxBytes);
                    data.setMobileTxBytes(batterySipper.mobileTxBytes - previous.mobileTxBytes);
                    data.setMobileRadioPowerMah(
                            batterySipper.mobileRadioPowerMah - previous.mobileRadioPowerMah);
                    data.setWakeLockTimeMs(batterySipper.wakeLockTimeMs - previous.wakeLockTimeMs);
                    data.setWakeLockPowerMah(
                            batterySipper.wakeLockPowerMah - previous.wakeLockPowerMah);
                    data.setCameraTimeMs(batterySipper.cameraTimeMs - previous.cameraTimeMs);
                    data.setCameraPowerMah(batterySipper.cameraPowerMah - previous.cameraPowerMah);
                    data.setBluetoothRunningTimeMs(
                            batterySipper.bluetoothRunningTimeMs - previous.bluetoothRunningTimeMs);
                    data.setBluetoothPowerMah(
                            batterySipper.bluetoothPowerMah - previous.bluetoothPowerMah);
                    data.setAudioTimeMs(batterySipper.audioTimeMs - previous.audioTimeMs);
                    data.setAudioPowerMah(batterySipper.audioPowerMah - previous.audioPowerMah);
                    data.setVideoTimeMs(batterySipper.videoTimeMs - previous.videoTimeMs);
                    data.setVideoPowerMah(batterySipper.videoPowerMah - previous.videoPowerMah);
                } else {
                    data.setCpuTimeMs(batterySipper.cpuTimeMs);
                    data.setCpuFgTimeMs(batterySipper.cpuFgTimeMs);
                    data.setCpuPowerMah(batterySipper.cpuPowerMah);
                    data.setGpsTimeMs(batterySipper.gpsTimeMs);
                    data.setGpsPowerMah(batterySipper.gpsPowerMah);
                    data.setWifiRxBytes(batterySipper.wifiRxBytes);
                    data.setWifiTxBytes(batterySipper.wifiTxBytes);
                    data.setWifiRunningTimeMs(batterySipper.wifiRunningTimeMs);
                    data.setWifiPowerMah(batterySipper.wifiPowerMah);
                    data.setMobileRxBytes(batterySipper.mobileRxBytes);
                    data.setMobileTxBytes(batterySipper.mobileTxBytes);
                    data.setMobileRadioPowerMah(batterySipper.mobileRadioPowerMah);
                    data.setWakeLockTimeMs(batterySipper.wakeLockTimeMs);
                    data.setWakeLockPowerMah(batterySipper.wakeLockPowerMah);
                    data.setCameraTimeMs(batterySipper.cameraTimeMs);
                    data.setCameraPowerMah(batterySipper.cameraPowerMah);
                    data.setBluetoothRunningTimeMs(batterySipper.bluetoothRunningTimeMs);
                    data.setBluetoothPowerMah(batterySipper.bluetoothPowerMah);
                    data.setAudioTimeMs(batterySipper.audioTimeMs);
                    data.setAudioPowerMah(batterySipper.audioPowerMah);
                    data.setVideoTimeMs(batterySipper.videoTimeMs);
                    data.setVideoPowerMah(batterySipper.videoPowerMah);
                }
                mPkgBatterySipperMaps.put(generalData.getPackageName(), batterySipper);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            APMDebugLog.d(TAG, "processAppUsage used = "
                    + (System.currentTimeMillis() - currentTimeMillis) + "ms");
        }
    }

    private static boolean checkHasWifiPowerReporting(BatteryStats stats, PowerProfile profile) {
        return stats.hasWifiActivityReporting()
                && profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_IDLE) != 0.0d
                && profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_RX) != 0.0d
                && profile.getAveragePower(PowerProfile.POWER_WIFI_CONTROLLER_TX) != 0.0d;
    }

    private static boolean checkHasBluetoothPowerReporting(BatteryStats stats,
            PowerProfile profile) {
        return stats.hasBluetoothActivityReporting()
                && profile.getAveragePower(
                        PowerProfile.POWER_BLUETOOTH_CONTROLLER_IDLE) != 0.0d
                && profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_RX) != 0.0d
                && profile.getAveragePower(PowerProfile.POWER_BLUETOOTH_CONTROLLER_TX) != 0.0d;
    }

    private static byte[] readFully(FileInputStream stream, int size) throws IOException {
        byte[] buffer = new byte[size];
        int count = 0;
        while (true) {
            int n = stream.read(buffer, count, buffer.length - count);
            if (n <= 0) {
                return buffer;
            }
            count += n;
            int available = stream.available();
            if (available > buffer.length - count) {
                byte[] newBuffer = new byte[available + count];
                System.arraycopy(buffer, 0, newBuffer, 0, count);
                buffer = newBuffer;
            }
        }
    }
}