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
 * Reports a single over-threshold event captured by the APM subsystem.
 *
 * @hide
 */
public class APMOverThresholdData extends APMBaseDate implements Parcelable {
    public static final String DATA_TYPE = "OverThreshold";

    public static final Parcelable.Creator<APMOverThresholdData> CREATOR =
            new Parcelable.Creator<APMOverThresholdData>() {
                @Override
                public APMOverThresholdData createFromParcel(Parcel in) {
                    return new APMOverThresholdData(in);
                }

                @Override
                public APMOverThresholdData[] newArray(int size) {
                    return new APMOverThresholdData[size];
                }
            };

    private long mTimestamp;
    private int mType;
    private String mThresholdValue;
    private String mOverThresholdValue;
    private String mOverThresholdScene;

    protected APMOverThresholdData(Parcel in) {
        super(in.readString(), in.readString(), in.readInt(), in.readString());
        mTimestamp = in.readLong();
        mType = in.readInt();
        mThresholdValue = in.readString();
        mOverThresholdValue = in.readString();
        mOverThresholdScene = in.readString();
    }

    public APMOverThresholdData(String packageName, String processName, int versionCode,
            String versionName, long timestamp, int type) {
        super(packageName, processName, versionCode, versionName);
        mTimestamp = timestamp;
        mType = type;
    }

    public APMOverThresholdData(APMOverThresholdData other) {
        super(other.getPackageName(), other.getProcessName(), other.getVersionCode(),
                other.getVersionName());
        mTimestamp = other.getTimestamp();
        mType = other.getType();
        mThresholdValue = other.getThresholdValue();
        mOverThresholdValue = other.getOverThresholdValue();
        mOverThresholdScene = other.getOverThresholdScene();
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
        out.writeLong(mTimestamp);
        out.writeInt(mType);
        out.writeString(mThresholdValue);
        out.writeString(mOverThresholdValue);
        out.writeString(mOverThresholdScene);
    }

    @Override
    public String getDataType() {
        return DATA_TYPE;
    }

    @Override
    public long getTimestamp() {
        return mTimestamp;
    }

    public int getType() {
        return mType;
    }

    public String getThresholdValue() {
        return mThresholdValue;
    }

    public void setThresholdValue(String value) {
        mThresholdValue = value;
    }

    public int getThresholdValueInt() {
        try {
            return Integer.parseInt(mThresholdValue);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setThresholdValueInt(int value) {
        mThresholdValue = String.valueOf(value);
    }

    public long getThresholdValueLong() {
        try {
            return Long.parseLong(mThresholdValue);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void setThresholdValueLong(long value) {
        mThresholdValue = String.valueOf(value);
    }

    public String getOverThresholdValue() {
        return mOverThresholdValue;
    }

    public void setOverThresholdValue(String value) {
        mOverThresholdValue = value;
    }

    public int getOverThresholdValueInt() {
        try {
            return Integer.parseInt(mOverThresholdValue);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void setOverThresholdValueInt(int value) {
        mOverThresholdValue = String.valueOf(value);
    }

    public long getOverThresholdValueLong() {
        try {
            return Long.parseLong(mOverThresholdValue);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public void setOverThresholdValueLong(long value) {
        mOverThresholdValue = String.valueOf(value);
    }

    public String getOverThresholdScene() {
        return mOverThresholdScene;
    }

    public void setOverThresholdScene(String scene) {
        mOverThresholdScene = scene;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("packageName", mPackageName);
            json.put("processName", mProcessName);
            json.put("versionCode", mVersionCode);
            json.put("versionName", mVersionName);
            json.put("timestamp", mTimestamp);
            json.put("type", mType);
            json.put("thresholdValue", mThresholdValue);
            json.put("overThresholdValue", mOverThresholdValue);
            json.put("overThresholdScene", mOverThresholdScene);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public String toString() {
        return "APMOverThresholdData{mPackageName='" + mPackageName + DateFormat.QUOTE
                + ", mProcessName='" + mProcessName + DateFormat.QUOTE
                + ", mVersionCode=" + mVersionCode
                + ", mVersionName='" + mVersionName + DateFormat.QUOTE
                + ", mTimestamp=" + mTimestamp
                + ", mType=" + mType
                + ", mThresholdValue='" + mThresholdValue + DateFormat.QUOTE
                + ", mOverThresholdValue='" + mOverThresholdValue + DateFormat.QUOTE
                + ", mOverThresholdScene='" + mOverThresholdScene + DateFormat.QUOTE + '}';
    }
}