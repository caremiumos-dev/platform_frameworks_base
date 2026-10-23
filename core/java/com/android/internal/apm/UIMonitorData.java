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
 * Aggregated UI performance statistics (method cost time, drop frames, FPS
 * and activity launch times) reported by the UI monitors.
 *
 * @hide
 */
public class UIMonitorData implements Parcelable {
    public static final Parcelable.Creator<UIMonitorData> CREATOR =
            new Parcelable.Creator<UIMonitorData>() {
                @Override
                public UIMonitorData createFromParcel(Parcel in) {
                    return new UIMonitorData(in);
                }

                @Override
                public UIMonitorData[] newArray(int size) {
                    return new UIMonitorData[size];
                }
            };

    private long mMinMethodCostTime;
    private long mMaxMethodCostTime;
    private long mAvgMethodCostTime;
    private String mMinMethodCostTimeScene;
    private String mMaxMethodCostTimeScene;
    private int mMinDropFrame;
    private int mMaxDropFrame;
    private int mAvgDropFrame;
    private String mMinDropFrameScene;
    private String mMaxDropFrameScene;
    private int mAvgFPS;
    private long mMinActivityLaunchTime;
    private long mMaxActivityLaunchTime;
    private long mAvgActivityLaunchTime;
    private String mMinActivityLaunchTimeScene;
    private String mMaxActivityLaunchTimeScene;
    private long mActivityColdLaunchTime;
    private String mActivityColdLaunchTimeScene;

    public UIMonitorData() {
    }

    protected UIMonitorData(Parcel in) {
        mMinMethodCostTime = in.readLong();
        mMaxMethodCostTime = in.readLong();
        mAvgMethodCostTime = in.readLong();
        mMinMethodCostTimeScene = in.readString();
        mMaxMethodCostTimeScene = in.readString();
        mMinDropFrame = in.readInt();
        mMaxDropFrame = in.readInt();
        mAvgDropFrame = in.readInt();
        mMinDropFrameScene = in.readString();
        mMaxDropFrameScene = in.readString();
        mAvgFPS = in.readInt();
        mMinActivityLaunchTime = in.readLong();
        mMaxActivityLaunchTime = in.readLong();
        mAvgActivityLaunchTime = in.readLong();
        mMinActivityLaunchTimeScene = in.readString();
        mMaxActivityLaunchTimeScene = in.readString();
        mActivityColdLaunchTime = in.readLong();
        mActivityColdLaunchTimeScene = in.readString();
    }

    public UIMonitorData(UIMonitorData other) {
        mMinMethodCostTime = other.getMinMethodCostTime();
        mMaxMethodCostTime = other.getMaxMethodCostTime();
        mAvgMethodCostTime = other.getAvgMethodCostTime();
        mMinMethodCostTimeScene = other.getMinMethodCostTimeScene();
        mMaxMethodCostTimeScene = other.getMaxMethodCostTimeScene();
        mMinDropFrame = other.getMinDropFrame();
        mMaxDropFrame = other.getMaxDropFrame();
        mAvgDropFrame = other.getAvgDropFrame();
        mMinDropFrameScene = other.getMinDropFrameScene();
        mMaxDropFrameScene = other.getMaxDropFrameScene();
        mAvgFPS = other.getAvgFPS();
        mMinActivityLaunchTime = other.getMinActivityLaunchTime();
        mMaxActivityLaunchTime = other.getMaxActivityLaunchTime();
        mAvgActivityLaunchTime = other.getAvgActivityLaunchTime();
        mMinActivityLaunchTimeScene = other.getMinActivityLaunchTimeScene();
        mMaxActivityLaunchTimeScene = other.getMaxActivityLaunchTimeScene();
        mActivityColdLaunchTime = other.getActivityColdLaunchTime();
        mActivityColdLaunchTimeScene = other.getActivityColdLaunchTimeScene();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeLong(mMinMethodCostTime);
        out.writeLong(mMaxMethodCostTime);
        out.writeLong(mAvgMethodCostTime);
        out.writeString(mMinMethodCostTimeScene);
        out.writeString(mMaxMethodCostTimeScene);
        out.writeInt(mMinDropFrame);
        out.writeInt(mMaxDropFrame);
        out.writeInt(mAvgDropFrame);
        out.writeString(mMinDropFrameScene);
        out.writeString(mMaxDropFrameScene);
        out.writeInt(mAvgFPS);
        out.writeLong(mMinActivityLaunchTime);
        out.writeLong(mMaxActivityLaunchTime);
        out.writeLong(mAvgActivityLaunchTime);
        out.writeString(mMinActivityLaunchTimeScene);
        out.writeString(mMaxActivityLaunchTimeScene);
        out.writeLong(mActivityColdLaunchTime);
        out.writeString(mActivityColdLaunchTimeScene);
    }

    public long getMinMethodCostTime() {
        return mMinMethodCostTime;
    }

    public void setMinMethodCostTime(long time) {
        mMinMethodCostTime = time;
    }

    public long getMaxMethodCostTime() {
        return mMaxMethodCostTime;
    }

    public void setMaxMethodCostTime(long time) {
        mMaxMethodCostTime = time;
    }

    public long getAvgMethodCostTime() {
        return mAvgMethodCostTime;
    }

    public void setAvgMethodCostTime(long time) {
        mAvgMethodCostTime = time;
    }

    public String getMinMethodCostTimeScene() {
        return mMinMethodCostTimeScene;
    }

    public void setMinMethodCostTimeScene(String scene) {
        mMinMethodCostTimeScene = scene;
    }

    public String getMaxMethodCostTimeScene() {
        return mMaxMethodCostTimeScene;
    }

    public void setMaxMethodCostTimeScene(String scene) {
        mMaxMethodCostTimeScene = scene;
    }

    public int getMinDropFrame() {
        return mMinDropFrame;
    }

    public void setMinDropFrame(int count) {
        mMinDropFrame = count;
    }

    public int getMaxDropFrame() {
        return mMaxDropFrame;
    }

    public void setMaxDropFrame(int count) {
        mMaxDropFrame = count;
    }

    public int getAvgDropFrame() {
        return mAvgDropFrame;
    }

    public void setAvgDropFrame(int count) {
        mAvgDropFrame = count;
    }

    public String getMinDropFrameScene() {
        return mMinDropFrameScene;
    }

    public void setMinDropFrameScene(String scene) {
        mMinDropFrameScene = scene;
    }

    public String getMaxDropFrameScene() {
        return mMaxDropFrameScene;
    }

    public void setMaxDropFrameScene(String scene) {
        mMaxDropFrameScene = scene;
    }

    public int getAvgFPS() {
        return mAvgFPS;
    }

    public void setAvgFPS(int fps) {
        mAvgFPS = fps;
    }

    public long getMinActivityLaunchTime() {
        return mMinActivityLaunchTime;
    }

    public void setMinActivityLaunchTime(long time) {
        mMinActivityLaunchTime = time;
    }

    public long getMaxActivityLaunchTime() {
        return mMaxActivityLaunchTime;
    }

    public void setMaxActivityLaunchTime(long time) {
        mMaxActivityLaunchTime = time;
    }

    public long getAvgActivityLaunchTime() {
        return mAvgActivityLaunchTime;
    }

    public void setAvgActivityLaunchTime(long time) {
        mAvgActivityLaunchTime = time;
    }

    public String getMinActivityLaunchTimeScene() {
        return mMinActivityLaunchTimeScene;
    }

    public void setMinActivityLaunchTimeScene(String scene) {
        mMinActivityLaunchTimeScene = scene;
    }

    public String getMaxActivityLaunchTimeScene() {
        return mMaxActivityLaunchTimeScene;
    }

    public void setMaxActivityLaunchTimeScene(String scene) {
        mMaxActivityLaunchTimeScene = scene;
    }

    public long getActivityColdLaunchTime() {
        return mActivityColdLaunchTime;
    }

    public void setActivityColdLaunchTime(long time) {
        mActivityColdLaunchTime = time;
    }

    public String getActivityColdLaunchTimeScene() {
        return mActivityColdLaunchTimeScene;
    }

    public void setActivityColdLaunchTimeScene(String scene) {
        mActivityColdLaunchTimeScene = scene;
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("minMethodCostTime", mMinMethodCostTime);
            json.put("maxMethodCostTime", mMaxMethodCostTime);
            json.put("avgMethodCostTime", mAvgMethodCostTime);
            json.put("minMethodCostTimeScene", mMinMethodCostTimeScene + "");
            json.put("maxMethodCostTimeScene", mMaxMethodCostTimeScene + "");
            json.put("minDropFrame", mMinDropFrame);
            json.put("maxDropFrame", mMaxDropFrame);
            json.put("avgDropFrame", mAvgDropFrame);
            json.put("minDropFrameScene", mMinDropFrameScene + "");
            json.put("maxDropFrameScene", mMaxDropFrameScene + "");
            json.put("avgFPS", mAvgFPS);
            json.put("minActivityLaunchTime", mMinActivityLaunchTime);
            json.put("maxActivityLaunchTime", mMaxActivityLaunchTime);
            json.put("avgActivityLaunchTime", mAvgActivityLaunchTime);
            json.put("minActivityLaunchTimeScene", mMinActivityLaunchTimeScene + "");
            json.put("maxActivityLaunchTimeScene", mMaxActivityLaunchTimeScene + "");
            json.put("activityColdLaunchTime", mActivityColdLaunchTime);
            json.put("activityColdLaunchTimeScene", mActivityColdLaunchTimeScene + "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public String toString() {
        return "UIMonitorData{mMinMethodCostTime=" + mMinMethodCostTime
                + ", mMaxMethodCostTime=" + mMaxMethodCostTime
                + ", mAvgMethodCostTime=" + mAvgMethodCostTime
                + ", mMinMethodCostTimeScene='" + mMinMethodCostTimeScene + DateFormat.QUOTE
                + ", mMaxMethodCostTimeScene='" + mMaxMethodCostTimeScene + DateFormat.QUOTE
                + ", mMinDropFrame=" + mMinDropFrame
                + ", mMaxDropFrame=" + mMaxDropFrame
                + ", mAvgDropFrame=" + mAvgDropFrame
                + ", mMinDropFrameScene='" + mMinDropFrameScene + DateFormat.QUOTE
                + ", mMaxDropFrameScene='" + mMaxDropFrameScene + DateFormat.QUOTE
                + ", mAvgFPS=" + mAvgFPS
                + ", mMinActivityLaunchTime=" + mMinActivityLaunchTime
                + ", mMaxActivityLaunchTime=" + mMaxActivityLaunchTime
                + ", mAvgActivityLaunchTime=" + mAvgActivityLaunchTime
                + ", mMinActivityLaunchTimeScene='" + mMinActivityLaunchTimeScene + DateFormat.QUOTE
                + ", mMaxActivityLaunchTimeScene='" + mMaxActivityLaunchTimeScene + DateFormat.QUOTE
                + ", mActivityColdLaunchTime=" + mActivityColdLaunchTime
                + ", mActivityColdLaunchTimeScene='" + mActivityColdLaunchTimeScene
                + DateFormat.QUOTE + '}';
    }
}