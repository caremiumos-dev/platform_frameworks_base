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
import android.text.format.DateFormat;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Aggregate performance data snapshot for one monitored application.
 *
 * @hide
 */
public class APMGeneralData extends APMBaseDate implements Parcelable {
    public static final String DATA_TYPE = "General";

    public static final Parcelable.Creator<APMGeneralData> CREATOR =
            new Parcelable.Creator<APMGeneralData>() {
                @Override
                public APMGeneralData createFromParcel(Parcel in) {
                    return new APMGeneralData(in);
                }

                @Override
                public APMGeneralData[] newArray(int size) {
                    return new APMGeneralData[size];
                }
            };

    private long mStartTimestamp;
    private long mEndTimestamp;
    private UIMonitorData mUIMonitorData;
    private SQLiteMonitorData mSQLiteMonitorData;
    private BatteryMonitorData mBatteryMonitorData;

    protected APMGeneralData(Parcel in) {
        super(in.readString(), in.readString(), in.readInt(), in.readString());
        mStartTimestamp = in.readLong();
        mEndTimestamp = in.readLong();
        mUIMonitorData = in.readParcelable(UIMonitorData.class.getClassLoader());
        mSQLiteMonitorData = in.readParcelable(SQLiteMonitorData.class.getClassLoader());
        mBatteryMonitorData = in.readParcelable(BatteryMonitorData.class.getClassLoader());
    }

    public APMGeneralData(String packageName, String processName, int versionCode,
            String versionName) {
        super(packageName, processName, versionCode, versionName);
    }

    public APMGeneralData(APMGeneralData other) {
        super(other.getPackageName(), other.getProcessName(), other.getVersionCode(),
                other.getVersionName());
        mStartTimestamp = other.getStartTimestamp();
        mEndTimestamp = other.getEndTimestamp();
        final UIMonitorData uiData = other.getUIMonitorData();
        if (uiData != null) {
            mUIMonitorData = new UIMonitorData(uiData);
        }
        final SQLiteMonitorData sqlData = other.getSQLiteMonitorData();
        if (sqlData != null) {
            mSQLiteMonitorData = new SQLiteMonitorData(sqlData);
        }
        final BatteryMonitorData batteryData = other.getBatteryMonitorData();
        if (batteryData != null) {
            mBatteryMonitorData = new BatteryMonitorData(batteryData);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mPackageName);
        out.writeString(mProcessName);
        out.writeInt(mVersionCode);
        out.writeString(mVersionName);
        out.writeLong(mStartTimestamp);
        out.writeLong(mEndTimestamp);
        out.writeParcelable(mUIMonitorData, 0);
        out.writeParcelable(mSQLiteMonitorData, 0);
        out.writeParcelable(mBatteryMonitorData, 0);
    }

    @Override
    public String getDataType() {
        return DATA_TYPE;
    }

    public long getStartTimestamp() {
        return mStartTimestamp;
    }

    public void setStartTimestamp(long timestamp) {
        mStartTimestamp = timestamp;
    }

    public long getEndTimestamp() {
        return mEndTimestamp;
    }

    public void setEndTimestamp(long timestamp) {
        mEndTimestamp = timestamp;
    }

    @Override
    public long getTimestamp() {
        return mEndTimestamp;
    }

    public UIMonitorData getUIMonitorData() {
        return mUIMonitorData;
    }

    public void setUIMonitorData(UIMonitorData data) {
        mUIMonitorData = data;
    }

    public SQLiteMonitorData getSQLiteMonitorData() {
        return mSQLiteMonitorData;
    }

    public void setSQLiteMonitorData(SQLiteMonitorData data) {
        mSQLiteMonitorData = data;
    }

    public BatteryMonitorData getBatteryMonitorData() {
        return mBatteryMonitorData;
    }

    public void setBatteryMonitorData(BatteryMonitorData data) {
        mBatteryMonitorData = data;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("packageName", mPackageName);
            json.put("processName", mProcessName);
            json.put("versionCode", mVersionCode);
            json.put("versionName", mVersionName);
            json.put("startTimestamp", mStartTimestamp);
            json.put("endTimestamp", mEndTimestamp);
            json.put("uiMonitorData", mUIMonitorData != null ? mUIMonitorData.toJson()
                    : JSONObject.NULL);
            json.put("sqliteMonitorData", mSQLiteMonitorData != null ? mSQLiteMonitorData.toJson()
                    : JSONObject.NULL);
            json.put("batteryMonitorData", mBatteryMonitorData != null
                    ? mBatteryMonitorData.toJson() : JSONObject.NULL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public String toString() {
        return "APMGeneralData{mPackageName='" + mPackageName + DateFormat.QUOTE
                + ", mProcessName='" + mProcessName + DateFormat.QUOTE
                + ", mVersionCode=" + mVersionCode
                + ", mVersionName='" + mVersionName + DateFormat.QUOTE
                + ", mStartTimestamp=" + mStartTimestamp
                + ", mEndTimestamp=" + mEndTimestamp
                + ", mUIMonitorData=" + mUIMonitorData
                + ", mSQLiteMonitorData=" + mSQLiteMonitorData
                + ", mBatteryMonitorData=" + mBatteryMonitorData + '}';
    }
}