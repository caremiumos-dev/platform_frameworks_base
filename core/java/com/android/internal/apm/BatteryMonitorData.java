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

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Battery usage attribution data collected by the battery monitor.
 *
 * @hide
 */
public class BatteryMonitorData implements Parcelable {
    public static final Parcelable.Creator<BatteryMonitorData> CREATOR =
            new Parcelable.Creator<BatteryMonitorData>() {
                @Override
                public BatteryMonitorData createFromParcel(Parcel in) {
                    return new BatteryMonitorData(in);
                }

                @Override
                public BatteryMonitorData[] newArray(int size) {
                    return new BatteryMonitorData[size];
                }
            };

    private int mStartBatteryLevel;
    private int mEndBatteryLevel;
    private long mCpuTimeMs;
    private long mCpuFgTimeMs;
    private double mCpuPowerMah;
    private long mGpsTimeMs;
    private double mGpsPowerMah;
    private long mWifiRxBytes;
    private long mWifiTxBytes;
    private long mWifiRunningTimeMs;
    private double mWifiPowerMah;
    private long mMobileRxBytes;
    private long mMobileTxBytes;
    private double mMobileRadioPowerMah;
    private long mWakeLockTimeMs;
    private double mWakeLockPowerMah;
    private long mCameraTimeMs;
    private double mCameraPowerMah;
    private long mBluetoothRunningTimeMs;
    private double mBluetoothPowerMah;
    private long mAudioTimeMs;
    private double mAudioPowerMah;
    private long mVideoTimeMs;
    private double mVideoPowerMah;

    public BatteryMonitorData() {
    }

    protected BatteryMonitorData(Parcel in) {
        mStartBatteryLevel = in.readInt();
        mEndBatteryLevel = in.readInt();
        mCpuTimeMs = in.readLong();
        mCpuFgTimeMs = in.readLong();
        mCpuPowerMah = in.readDouble();
        mGpsTimeMs = in.readLong();
        mGpsPowerMah = in.readDouble();
        mWifiRxBytes = in.readLong();
        mWifiTxBytes = in.readLong();
        mWifiRunningTimeMs = in.readLong();
        mWifiPowerMah = in.readDouble();
        mMobileRxBytes = in.readLong();
        mMobileTxBytes = in.readLong();
        mMobileRadioPowerMah = in.readDouble();
        mWakeLockTimeMs = in.readLong();
        mWakeLockPowerMah = in.readDouble();
        mCameraTimeMs = in.readLong();
        mCameraPowerMah = in.readDouble();
        mBluetoothRunningTimeMs = in.readLong();
        mBluetoothPowerMah = in.readDouble();
        mAudioTimeMs = in.readLong();
        mAudioPowerMah = in.readDouble();
        mVideoTimeMs = in.readLong();
        mVideoPowerMah = in.readDouble();
    }

    public BatteryMonitorData(BatteryMonitorData other) {
        mStartBatteryLevel = other.getStartBatteryLevel();
        mEndBatteryLevel = other.getEndBatteryLevel();
        mCpuTimeMs = other.getCpuTimeMs();
        mCpuFgTimeMs = other.getCpuFgTimeMs();
        mCpuPowerMah = other.getCpuPowerMah();
        mGpsTimeMs = other.getGpsTimeMs();
        mGpsPowerMah = other.getGpsPowerMah();
        mWifiRxBytes = other.getWifiRxBytes();
        mWifiTxBytes = other.getWifiTxBytes();
        mWifiRunningTimeMs = other.getWifiRunningTimeMs();
        mWifiPowerMah = other.getWifiPowerMah();
        mMobileRxBytes = other.getMobileRxBytes();
        mMobileTxBytes = other.getMobileTxBytes();
        mMobileRadioPowerMah = other.getMobileRadioPowerMah();
        mWakeLockTimeMs = other.getWakeLockTimeMs();
        mWakeLockPowerMah = other.getWakeLockPowerMah();
        mCameraTimeMs = other.getCameraTimeMs();
        mCameraPowerMah = other.getCameraPowerMah();
        mBluetoothRunningTimeMs = other.getBluetoothRunningTimeMs();
        mBluetoothPowerMah = other.getBluetoothPowerMah();
        mAudioTimeMs = other.getAudioTimeMs();
        mAudioPowerMah = other.getAudioPowerMah();
        mVideoTimeMs = other.getVideoTimeMs();
        mVideoPowerMah = other.getVideoPowerMah();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(mStartBatteryLevel);
        out.writeInt(mEndBatteryLevel);
        out.writeLong(mCpuTimeMs);
        out.writeLong(mCpuFgTimeMs);
        out.writeDouble(mCpuPowerMah);
        out.writeLong(mGpsTimeMs);
        out.writeDouble(mGpsPowerMah);
        out.writeLong(mWifiRxBytes);
        out.writeLong(mWifiTxBytes);
        out.writeLong(mWifiRunningTimeMs);
        out.writeDouble(mWifiPowerMah);
        out.writeLong(mMobileRxBytes);
        out.writeLong(mMobileTxBytes);
        out.writeDouble(mMobileRadioPowerMah);
        out.writeLong(mWakeLockTimeMs);
        out.writeDouble(mWakeLockPowerMah);
        out.writeLong(mCameraTimeMs);
        out.writeDouble(mCameraPowerMah);
        out.writeLong(mBluetoothRunningTimeMs);
        out.writeDouble(mBluetoothPowerMah);
        out.writeLong(mAudioTimeMs);
        out.writeDouble(mAudioPowerMah);
        out.writeLong(mVideoTimeMs);
        out.writeDouble(mVideoPowerMah);
    }

    public int getStartBatteryLevel() {
        return mStartBatteryLevel;
    }

    public void setStartBatteryLevel(int level) {
        mStartBatteryLevel = level;
    }

    public int getEndBatteryLevel() {
        return mEndBatteryLevel;
    }

    public void setEndBatteryLevel(int level) {
        mEndBatteryLevel = level;
    }

    public long getCpuTimeMs() {
        return mCpuTimeMs;
    }

    public void setCpuTimeMs(long timeMs) {
        mCpuTimeMs = timeMs;
    }

    public long getCpuFgTimeMs() {
        return mCpuFgTimeMs;
    }

    public void setCpuFgTimeMs(long timeMs) {
        mCpuFgTimeMs = timeMs;
    }

    public double getCpuPowerMah() {
        return mCpuPowerMah;
    }

    public void setCpuPowerMah(double powerMah) {
        mCpuPowerMah = powerMah;
    }

    public long getGpsTimeMs() {
        return mGpsTimeMs;
    }

    public void setGpsTimeMs(long timeMs) {
        mGpsTimeMs = timeMs;
    }

    public double getGpsPowerMah() {
        return mGpsPowerMah;
    }

    public void setGpsPowerMah(double powerMah) {
        mGpsPowerMah = powerMah;
    }

    public long getWifiRxBytes() {
        return mWifiRxBytes;
    }

    public void setWifiRxBytes(long bytes) {
        mWifiRxBytes = bytes;
    }

    public long getWifiTxBytes() {
        return mWifiTxBytes;
    }

    public void setWifiTxBytes(long bytes) {
        mWifiTxBytes = bytes;
    }

    public long getWifiRunningTimeMs() {
        return mWifiRunningTimeMs;
    }

    public void setWifiRunningTimeMs(long timeMs) {
        mWifiRunningTimeMs = timeMs;
    }

    public double getWifiPowerMah() {
        return mWifiPowerMah;
    }

    public void setWifiPowerMah(double powerMah) {
        mWifiPowerMah = powerMah;
    }

    public long getMobileRxBytes() {
        return mMobileRxBytes;
    }

    public void setMobileRxBytes(long bytes) {
        mMobileRxBytes = bytes;
    }

    public long getMobileTxBytes() {
        return mMobileTxBytes;
    }

    public void setMobileTxBytes(long bytes) {
        mMobileTxBytes = bytes;
    }

    public double getMobileRadioPowerMah() {
        return mMobileRadioPowerMah;
    }

    public void setMobileRadioPowerMah(double powerMah) {
        mMobileRadioPowerMah = powerMah;
    }

    public long getWakeLockTimeMs() {
        return mWakeLockTimeMs;
    }

    public void setWakeLockTimeMs(long timeMs) {
        mWakeLockTimeMs = timeMs;
    }

    public double getWakeLockPowerMah() {
        return mWakeLockPowerMah;
    }

    public void setWakeLockPowerMah(double powerMah) {
        mWakeLockPowerMah = powerMah;
    }

    public long getCameraTimeMs() {
        return mCameraTimeMs;
    }

    public void setCameraTimeMs(long timeMs) {
        mCameraTimeMs = timeMs;
    }

    public double getCameraPowerMah() {
        return mCameraPowerMah;
    }

    public void setCameraPowerMah(double powerMah) {
        mCameraPowerMah = powerMah;
    }

    public long getBluetoothRunningTimeMs() {
        return mBluetoothRunningTimeMs;
    }

    public void setBluetoothRunningTimeMs(long timeMs) {
        mBluetoothRunningTimeMs = timeMs;
    }

    public double getBluetoothPowerMah() {
        return mBluetoothPowerMah;
    }

    public void setBluetoothPowerMah(double powerMah) {
        mBluetoothPowerMah = powerMah;
    }

    public long getAudioTimeMs() {
        return mAudioTimeMs;
    }

    public void setAudioTimeMs(long timeMs) {
        mAudioTimeMs = timeMs;
    }

    public double getAudioPowerMah() {
        return mAudioPowerMah;
    }

    public void setAudioPowerMah(double powerMah) {
        mAudioPowerMah = powerMah;
    }

    public long getVideoTimeMs() {
        return mVideoTimeMs;
    }

    public void setVideoTimeMs(long timeMs) {
        mVideoTimeMs = timeMs;
    }

    public double getVideoPowerMah() {
        return mVideoPowerMah;
    }

    public void setVideoPowerMah(double powerMah) {
        mVideoPowerMah = powerMah;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("startBatteryLevel", mStartBatteryLevel);
            json.put("endBatteryLevel", mEndBatteryLevel);
            json.put("cpuTimeMs", mCpuTimeMs);
            json.put("cpuFgTimeMs", mCpuFgTimeMs);
            json.put("cpuPowerMah", mCpuPowerMah);
            json.put("gpsTimeMs", mGpsTimeMs);
            json.put("gpsPowerMah", mGpsPowerMah);
            json.put("wifiRxBytes", mWifiRxBytes);
            json.put("wifiTxBytes", mWifiTxBytes);
            json.put("wifiRunningTimeMs", mWifiRunningTimeMs);
            json.put("wifiPowerMah", mWifiPowerMah);
            json.put("mobileRxBytes", mMobileRxBytes);
            json.put("mobileTxBytes", mMobileTxBytes);
            json.put("mobileRadioPowerMah", mMobileRadioPowerMah);
            json.put("wakeLockTimeMs", mWakeLockTimeMs);
            json.put("wakeLockPowerMah", mWakeLockPowerMah);
            json.put("cameraTimeMs", mCameraTimeMs);
            json.put("cameraPowerMah", mCameraPowerMah);
            json.put("bluetoothRunningTimeMs", mBluetoothRunningTimeMs);
            json.put("bluetoothPowerMah", mBluetoothPowerMah);
            json.put("audioTimeMs", mAudioTimeMs);
            json.put("audioPowerMah", mAudioPowerMah);
            json.put("videoTimeMs", mVideoTimeMs);
            json.put("videoPowerMah", mVideoPowerMah);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public String toString() {
        return "BatteryMonitorData{mStartBatteryLevel=" + mStartBatteryLevel
                + ", mEndBatteryLevel=" + mEndBatteryLevel
                + ", mCpuTimeMs=" + mCpuTimeMs
                + ", mCpuFgTimeMs=" + mCpuFgTimeMs
                + ", mCpuPowerMah=" + mCpuPowerMah
                + ", mGpsTimeMs=" + mGpsTimeMs
                + ", mGpsPowerMah=" + mGpsPowerMah
                + ", mWifiRxBytes=" + mWifiRxBytes
                + ", mWifiTxBytes=" + mWifiTxBytes
                + ", mWifiRunningTimeMs=" + mWifiRunningTimeMs
                + ", mWifiPowerMah=" + mWifiPowerMah
                + ", mMobileRxBytes=" + mMobileRxBytes
                + ", mMobileTxBytes=" + mMobileTxBytes
                + ", mMobileRadioPowerMah=" + mMobileRadioPowerMah
                + ", mWakeLockTimeMs=" + mWakeLockTimeMs
                + ", mWakeLockPowerMah=" + mWakeLockPowerMah
                + ", mCameraTimeMs=" + mCameraTimeMs
                + ", mCameraPowerMah=" + mCameraPowerMah
                + ", mBluetoothRunningTimeMs=" + mBluetoothRunningTimeMs
                + ", mBluetoothPowerMah=" + mBluetoothPowerMah
                + ", mAudioTimeMs=" + mAudioTimeMs
                + ", mAudioPowerMah=" + mAudioPowerMah
                + ", mVideoTimeMs=" + mVideoTimeMs
                + ", mVideoPowerMah=" + mVideoPowerMah + '}';
    }
}