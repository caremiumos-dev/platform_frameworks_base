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

package com.xtc.behavior;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Builds and submits a single behavior report to the XTC behavior service.
 *
 * @hide
 */
public class XtcBehavior {
    private static final boolean DEBUG = false;
    public static final int EVENT_TYPE_BUSINESS = 1001;
    private static final int EVENT_TYPE_SYS_DEFAULT = 1000;
    private static final String TAG = "XtcBehavior";

    private String mAppVer;
    private Context mContext;
    private XtcBehaviorDataLevel mDataCollectLevel;
    private XtcBehaviorDataLevel mDataSecurityLevel;
    private int mEventType;
    private String mExtend;
    private String mFunctionName;
    private String mModuleName;
    private String mPackageName;
    private XtcBehaviorManager mXtcbehaviorMgr;

    public static class Builder {
        private String app_version;
        private Context context;
        private XtcBehaviorDataLevel data_collect_level;
        private XtcBehaviorDataLevel data_security_level;
        private int event_type;
        private final HashMap extend;
        private final String function_name;
        private String module_name;
        private String package_name;

        public Builder(Context context, String functionName, HashMap extend) {
            this.context = context;
            this.function_name = functionName;
            this.extend = extend;
        }

        public Builder appVer(String version) {
            this.app_version = version;
            return this;
        }

        public XtcBehavior build() {
            return new XtcBehavior(this);
        }

        public Builder dataCollectLevel(XtcBehaviorDataLevel level) {
            this.data_collect_level = level;
            return this;
        }

        public Builder dataSecurityLevel(XtcBehaviorDataLevel level) {
            this.data_security_level = level;
            return this;
        }

        public Builder eventType(int type) {
            this.event_type = type;
            return this;
        }

        public Builder moduleName(String name) {
            this.module_name = name;
            return this;
        }

        public Builder packageName(String name) {
            this.package_name = name;
            return this;
        }
    }

    private XtcBehavior(Builder builder) {
        this.mContext = builder.context;
        this.mPackageName = builder.package_name;
        this.mModuleName = builder.module_name;
        this.mAppVer = builder.app_version;
        this.mEventType = builder.event_type;
        this.mDataCollectLevel = builder.data_collect_level;
        this.mDataSecurityLevel = builder.data_security_level;
        this.mFunctionName = builder.function_name;
        this.mExtend = hashMapToJson(builder.extend);
        this.mXtcbehaviorMgr = (XtcBehaviorManager) this.mContext
                .getSystemService(Context.XTC_BEHAVIOR_SERVICE);
    }

    private String getAppVersion() {
        if (!TextUtils.isEmpty(this.mAppVer)) {
            return this.mAppVer;
        }
        String callerAppVersion = getCallerAppVersion();
        logd("XtcBehavior getAppVersion: " + callerAppVersion);
        return TextUtils.isEmpty(callerAppVersion) ? "sys_app_version" : callerAppVersion;
    }

    private String getCallerAppVersion() {
        if (this.mContext == null) {
            return null;
        }
        try {
            return this.mContext.getPackageManager()
                    .getPackageInfo(this.mContext.getPackageName(), 0).versionName;
        } catch (Exception e) {
            Log.w(TAG, "XtcBehavior getCallerAppVersion exception: " + e);
            return "";
        }
    }

    private String getCallerModuleName() {
        if (this.mContext == null) {
            return null;
        }
        try {
            PackageManager packageManager = this.mContext.getPackageManager();
            return packageManager.getPackageInfo(this.mContext.getPackageName(), 0).applicationInfo
                    .loadLabel(packageManager).toString();
        } catch (Exception e) {
            Log.w(TAG, "XtcBehavior getCallerModuleName exception: " + e);
            return "";
        }
    }

    private String getCallerPackageName() {
        if (this.mContext == null) {
            return null;
        }
        String packageName = this.mContext.getPackageName();
        logd("XtcBehavior getCallerPackage: " + packageName);
        return packageName;
    }

    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
                .format(new Date());
    }

    private XtcBehaviorDataLevel getDataCollectLevel() {
        return this.mDataCollectLevel != null ? this.mDataCollectLevel : XtcBehaviorDataLevel.B;
    }

    private XtcBehaviorDataLevel getDataSecurityLevel() {
        return this.mDataSecurityLevel != null ? this.mDataSecurityLevel : XtcBehaviorDataLevel.C;
    }

    private String getEventType() {
        return (this.mEventType == 0 || this.mEventType <= EVENT_TYPE_SYS_DEFAULT)
                ? String.valueOf(EVENT_TYPE_BUSINESS) : String.valueOf(this.mEventType);
    }

    private String getModuleName() {
        if (!TextUtils.isEmpty(this.mModuleName)) {
            return this.mModuleName;
        }
        String callerModuleName = getCallerModuleName();
        logd("XtcBehavior getModuleName: " + callerModuleName);
        return TextUtils.isEmpty(callerModuleName) ? "sys_module_name" : callerModuleName;
    }

    private String getPackageName() {
        if (!TextUtils.isEmpty(this.mPackageName)) {
            return this.mPackageName;
        }
        String callerPackageName = getCallerPackageName();
        return TextUtils.isEmpty(callerPackageName) ? "sys_package_name" : callerPackageName;
    }

    private String hashMapToJson(HashMap map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        try {
            String result = "{";
            for (Object entryObject : map.entrySet()) {
                Map.Entry entry = (Map.Entry) entryObject;
                result = (result + "\"" + entry.getKey() + "\":") + "\"" + entry.getValue() + "\",";
            }
            return result.substring(0, result.lastIndexOf(",")) + "}";
        } catch (Exception e) {
            Log.w(TAG, "XtcBehavior hashMapToJson exception: " + e);
            return "";
        }
    }

    private void logd(String message) {
    }

    private void release() {
        this.mContext = null;
        this.mPackageName = null;
        this.mModuleName = null;
        this.mAppVer = null;
        this.mEventType = 0;
        this.mDataCollectLevel = null;
        this.mDataSecurityLevel = null;
        this.mFunctionName = null;
        this.mExtend = null;
    }

    public void sendData() {
        if (this.mContext == null) {
            return;
        }
        String packageName = getPackageName();
        String moduleName = getModuleName();
        String appVersion = getAppVersion();
        String eventType = getEventType();
        String dataCollectLevel = getDataCollectLevel().toString();
        String dataSecurityLevel = getDataSecurityLevel().toString();
        String currentTime = getCurrentTime();
        if ("audio_mic".equals(this.mFunctionName) || "user_shell".equals(this.mFunctionName)) {
            dataCollectLevel = XtcBehaviorDataLevel.A.toString();
        }
        ContentValues values = new ContentValues();
        values.put("packageName", packageName);
        values.put("moduleName", moduleName);
        values.put("appVer", appVersion);
        values.put("eventType", eventType);
        values.put("dataCollectLevel", dataCollectLevel);
        values.put("dataSecurityLevel", dataSecurityLevel);
        values.put("functionName", this.mFunctionName);
        values.put("extend", this.mExtend);
        values.put("trigTime", currentTime);
        try {
            this.mXtcbehaviorMgr.sendData(values);
            logd("XtcBehavior send data values: " + values);
        } catch (Exception e) {
            Log.w(TAG, "XtcBehavior send data failed !! ex: " + e);
        } finally {
            release();
        }
    }
}