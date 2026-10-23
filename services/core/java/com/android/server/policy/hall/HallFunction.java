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

package com.android.server.policy.hall;

import android.content.Context;
import android.os.Message;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import com.xtc.utils.HallUtil;
import com.xtc.widget.MenuHolder;
import com.xtc.widget.MenuItem;

/**
 * Tracks the currently selected hall function (the entry that is launched when
 * the cover is opened) and keeps it in sync with the menu configuration.
 *
 * @hide
 */
public class HallFunction extends HallAction {
    private static final String TAG = "Sys" + HallFunction.class.getSimpleName();

    private int functionIndex;
    private String functionKey;

    public HallFunction(Context context) {
        super(context);
        this.functionIndex = SystemProperties.getInt(HallUtil.FUN_PROP, HallUtil.DEFAULT_FUNCTION);
        this.functionKey = HallUtil.KEY_PHOTO;
    }

    private void handleFunctionChanged(int value) {
        if (MenuHolder.instance().itemDisable(value)) {
            Log.w(TAG, "function value:" + value + " has been disabled");
            SystemProperties.set(HallUtil.FUN_PROP, String.valueOf(this.functionIndex));
        } else {
            this.functionIndex = value;
            this.functionKey = MenuHolder.instance().getItemByIndex(value).key;
            Log.d(TAG, "set function index:" + this.functionIndex + " key:" + this.functionKey
                    + " res:" + Settings.System.putString(this.context.getContentResolver(),
                            HallUtil.FUN_PROP, this.functionKey));
        }
    }

    private void setFunctionByKey(String key) {
        this.functionIndex = MenuHolder.instance().getItemIndex(key);
        this.functionKey = MenuHolder.instance().getItemByIndex(this.functionIndex).key;
        Log.d(TAG, "set key index:" + this.functionIndex + " key:" + this.functionKey
                + " res:" + Settings.System.putString(this.context.getContentResolver(),
                        HallUtil.FUN_PROP, this.functionKey));
    }

    private void updateFunctionKey() {
        this.functionKey = Settings.System.getString(this.context.getContentResolver(),
                HallUtil.FUN_PROP);
        MenuItem item = MenuHolder.instance().getItemByIndex(this.functionIndex);
        if (this.functionKey == null) {
            if (this.functionIndex == HallUtil.DEFAULT_FUNCTION
                    && !HallUtil.KEY_PHOTO.equals(item.key)) {
                Log.d(TAG, "detected photo index changed");
                this.functionKey = HallUtil.KEY_PHOTO;
                this.functionIndex = MenuHolder.instance().getItemIndex(this.functionKey);
            } else {
                this.functionKey = item.key;
            }
        } else if (!item.key.equals(this.functionKey)) {
            Log.d(TAG, "index changed ori index:" + this.functionIndex);
            this.functionIndex = MenuHolder.instance().getItemIndex(this.functionKey);
            this.functionKey = MenuHolder.instance().getItemByIndex(this.functionIndex).key;
        }
        Settings.System.putString(this.context.getContentResolver(), HallUtil.FUN_PROP,
                this.functionKey);
        Log.d(TAG, "current function index:" + this.functionIndex + " key:" + this.functionKey);
    }

    public void checkFunctionSwitch() {
        Log.d(TAG, "update function switch");
        MenuHolder.instance().init(this.context, false);
        MenuHolder.instance().updateFunSwitchValue(this.context);
        checkNeedResetToDefault();
    }

    public void checkModuleSwitch() {
        Log.d(TAG, "start check module switch status");
        MenuHolder.instance().init(this.context, false);
        MenuHolder.instance().queryListByModuleSwitch(this.context);
        MenuHolder.instance().updateFunSwitchValue(this.context);
        updateFunctionKey();
        checkNeedResetToDefault();
    }

    public void checkNeedResetToDefault() {
        if (MenuHolder.instance().itemDisable(this.functionIndex)) {
            Log.d(TAG, "current function disabled reset to default FUN:" + this.functionIndex);
            int itemIndex = MenuHolder.instance().getItemIndex(HallUtil.KEY_PHOTO);
            if (itemIndex != HallUtil.DEFAULT_FUNCTION) {
                Log.d(TAG, "default function index changed new:" + itemIndex);
                this.functionIndex = itemIndex;
            } else {
                this.functionIndex = HallUtil.DEFAULT_FUNCTION;
            }
            SystemProperties.set(HallUtil.FUN_PROP, String.valueOf(this.functionIndex));
            this.functionKey = HallUtil.KEY_PHOTO;
            Settings.System.putString(this.context.getContentResolver(), HallUtil.FUN_PROP,
                    this.functionKey);
            Message.obtain(this.uiHandler, MSG_HIDE_VIEW_FORCE, Boolean.TRUE).sendToTarget();
        }
    }

    public int getIndex() {
        return this.functionIndex;
    }

    public MenuItem getItem() {
        return MenuHolder.instance().getItemByIndex(this.functionIndex);
    }

    @Override
    public void init() {
    }

    public boolean invalid() {
        if (this.functionIndex < 0 || this.functionIndex >= MenuHolder.instance().size()) {
            return true;
        }
        return DISABLE_VALUE.equals(getItem().key);
    }

    public void reloadFunctionMenu(String packageName) {
        if (MenuHolder.RES_PACKAGE.equals(packageName)
                || MenuHolder.instance().contains(packageName)) {
            Log.d(TAG, packageName + " changed reload menu");
            MenuHolder.instance().reload(this.context);
            MenuHolder.instance().queryListByModuleSwitch(this.context);
            MenuHolder.instance().updateFunSwitchValue(this.context);
            updateFunctionKey();
            checkNeedResetToDefault();
        }
    }

    public void setHallFunction(String value) {
        try {
            handleFunctionChanged(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            setFunctionByKey(value);
        }
    }
}