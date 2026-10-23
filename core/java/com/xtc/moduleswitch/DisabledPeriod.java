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

package com.xtc.moduleswitch;

import android.provider.SettingsStringUtil;
import android.text.format.DateFormat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

/**
 * Describes a time window (with optional week selection) during which a
 * feature switch is disabled.
 *
 * @hide
 */
public class DisabledPeriod {
    private static final String TAG = "SysDisabledPeriod";

    public static final int ENABLE = 0;
    public static final int DISABLE = 1;

    public static final String JSON_KEY_ENABLE = "enable";
    public static final String JSON_KEY_BEGIN_TIME = "beginTime";
    public static final String JSON_KEY_END_TIME = "endTime";
    public static final String JSON_KEY_WEEKS = "weeks";

    int id;
    int weeks;
    int beginTimeHour;
    int beginTimeMinute;
    int endTimeHour;
    int endTimeMinute;
    float fBeginTime;
    float fEndTime;
    String beginTime;
    String endTime;
    private int enable;
    private Integer action;

    public interface ActionType {
        int ACTION_TYPE_CLOSE = 0;
        int ACTION_TYPE_OPEN = 1;
        int ACTION_TYPE_ADD = 2;
        int ACTION_TYPE_UPDATE = 3;
        int ACTION_TYPE_DELETE = 4;
    }

    static DisabledPeriod fromJson(String json) throws JSONException {
        JSONObject object = new JSONObject(json);
        DisabledPeriod period = new DisabledPeriod();
        if (!object.has(JSON_KEY_ENABLE)) {
            throw new JSONException("Not has key: " + JSON_KEY_ENABLE);
        }
        period.enable = object.optInt(JSON_KEY_ENABLE);
        if (!object.has(JSON_KEY_BEGIN_TIME)) {
            throw new JSONException("Not has key: " + JSON_KEY_BEGIN_TIME);
        }
        period.beginTime = object.optString(JSON_KEY_BEGIN_TIME);
        if (!object.has(JSON_KEY_END_TIME)) {
            throw new JSONException("Not has key: " + JSON_KEY_END_TIME);
        }
        period.endTime = object.optString(JSON_KEY_END_TIME);
        if (!object.has(JSON_KEY_WEEKS)) {
            throw new JSONException("Not has key: " + JSON_KEY_WEEKS);
        }
        period.weeks = object.optInt(JSON_KEY_WEEKS);
        String[] begin = period.beginTime.split(SettingsStringUtil.DELIMITER);
        String[] end = period.endTime.split(SettingsStringUtil.DELIMITER);
        if (begin.length < 2 || end.length < 2) {
            throw new JSONException("time format error.");
        }
        period.beginTimeHour = Integer.parseInt(begin[0]);
        period.beginTimeMinute = Integer.parseInt(begin[1]);
        period.endTimeHour = Integer.parseInt(end[0]);
        period.endTimeMinute = Integer.parseInt(end[1]);
        period.fBeginTime = period.beginTimeHour + (period.beginTimeMinute / 60.0f);
        period.fEndTime = period.endTimeHour + (period.endTimeMinute / 60.0f);
        return period;
    }

    private static boolean checkTodaySwitch(int weeks, int day) {
        return (weeks & (1 << day)) == (1 << day);
    }

    private static boolean isDisabledPeriod(Calendar calendar, DisabledPeriod period) {
        int day = ((calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        if (period.enable == 0) {
            Log.i(TAG, "enable is false");
            return false;
        }
        if (!checkTodaySwitch(period.weeks, day)) {
            Log.d(TAG, "today not disable");
            return false;
        }
        if (hour < period.beginTimeHour || hour > period.endTimeHour) {
            return false;
        }
        if (hour > period.beginTimeHour && hour < period.endTimeHour) {
            return true;
        }
        if (period.beginTimeHour == period.endTimeHour) {
            return minute >= period.beginTimeMinute && minute < period.endTimeMinute;
        }
        if (hour == period.beginTimeHour) {
            return minute >= period.beginTimeMinute;
        }
        return hour == period.endTimeHour && minute < period.endTimeMinute;
    }

    public boolean isDisabledPeriod(Calendar calendar) {
        return isDisabledPeriod(calendar, this);
    }

    public boolean isEnable() {
        return this.enable == 0;
    }

    public int getEnable() {
        return this.enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getWeeks() {
        return this.weeks;
    }

    public void setWeeks(int weeks) {
        this.weeks = weeks;
    }

    public String getBeginTime() {
        return this.beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return this.endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getAction() {
        return this.action;
    }

    public void setAction(Integer action) {
        this.action = action;
    }

    @Override
    public String toString() {
        return "DisabledPeriod{action=" + this.action + ", enable=" + this.enable
                + ", id=" + this.id + ", weeks=" + this.weeks
                + ", beginTime='" + this.beginTime + DateFormat.QUOTE
                + ", endTime='" + this.endTime + DateFormat.QUOTE
                + ", beginTimeHour=" + this.beginTimeHour
                + ", beginTimeMinute=" + this.beginTimeMinute
                + ", endTimeHour=" + this.endTimeHour
                + ", endTimeMinute=" + this.endTimeMinute
                + ", fBeginTime=" + this.fBeginTime + ", fEndTime=" + this.fEndTime + '}';
    }
}