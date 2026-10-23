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
 * SQL cost time statistics gathered by the APM sqlite monitor.
 *
 * @hide
 */
public class SQLiteMonitorData implements Parcelable {
    public static final Parcelable.Creator<SQLiteMonitorData> CREATOR =
            new Parcelable.Creator<SQLiteMonitorData>() {
                @Override
                public SQLiteMonitorData createFromParcel(Parcel in) {
                    return new SQLiteMonitorData(in);
                }

                @Override
                public SQLiteMonitorData[] newArray(int size) {
                    return new SQLiteMonitorData[size];
                }
            };

    private long mMinSqlCostTime;
    private long mMaxSqlCostTime;
    private long mAvgSqlCostTime;
    private String mMinSqlCostTimeScene;
    private String mMaxSqlCostTimeScene;

    public SQLiteMonitorData() {
    }

    protected SQLiteMonitorData(Parcel in) {
        mMinSqlCostTime = in.readLong();
        mMaxSqlCostTime = in.readLong();
        mAvgSqlCostTime = in.readLong();
        mMinSqlCostTimeScene = in.readString();
        mMaxSqlCostTimeScene = in.readString();
    }

    public SQLiteMonitorData(SQLiteMonitorData other) {
        mMinSqlCostTime = other.getMinSqlCostTime();
        mMaxSqlCostTime = other.getMaxSqlCostTime();
        mAvgSqlCostTime = other.getAvgSqlCostTime();
        mMinSqlCostTimeScene = other.getMinSqlCostTimeScene();
        mMaxSqlCostTimeScene = other.getMaxSqlCostTimeScene();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mMinSqlCostTime);
        out.writeLong(mMaxSqlCostTime);
        out.writeLong(mAvgSqlCostTime);
        out.writeString(mMinSqlCostTimeScene);
        out.writeString(mMaxSqlCostTimeScene);
    }

    public long getMinSqlCostTime() {
        return mMinSqlCostTime;
    }

    public void setMinSqlCostTime(long time) {
        mMinSqlCostTime = time;
    }

    public long getMaxSqlCostTime() {
        return mMaxSqlCostTime;
    }

    public void setMaxSqlCostTime(long time) {
        mMaxSqlCostTime = time;
    }

    public long getAvgSqlCostTime() {
        return mAvgSqlCostTime;
    }

    public void setAvgSqlCostTime(long time) {
        mAvgSqlCostTime = time;
    }

    public String getMinSqlCostTimeScene() {
        return mMinSqlCostTimeScene;
    }

    public void setMinSqlCostTimeScene(String scene) {
        mMinSqlCostTimeScene = scene;
    }

    public String getMaxSqlCostTimeScene() {
        return mMaxSqlCostTimeScene;
    }

    public void setMaxSqlCostTimeScene(String scene) {
        mMaxSqlCostTimeScene = scene;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("minSqlCostTime", mMinSqlCostTime);
            json.put("maxSqlCostTime", mMaxSqlCostTime);
            json.put("avgSqlCostTime", mAvgSqlCostTime);
            json.put("minSqlCostTimeScene", mMinSqlCostTimeScene + "");
            json.put("maxSqlCostTimeScene", mMaxSqlCostTimeScene + "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public String toString() {
        return "SQLiteMonitorData{mMinSqlCostTime=" + mMinSqlCostTime
                + ", mMaxSqlCostTime=" + mMaxSqlCostTime
                + ", mAvgSqlCostTime=" + mAvgSqlCostTime
                + ", mMinSqlCostTimeScene='" + mMinSqlCostTimeScene + DateFormat.QUOTE
                + ", mMaxSqlCostTimeScene='" + mMaxSqlCostTimeScene + DateFormat.QUOTE + '}';
    }
}