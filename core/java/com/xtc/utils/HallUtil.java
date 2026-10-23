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

package com.xtc.utils;

import android.bluetooth.BluetoothClass;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Helpers and property names for the XTC hall (flip cover) switch.
 *
 * @hide
 */
public class HallUtil {
    private static final String TAG = "Sys" + HallUtil.class.getSimpleName();

    public static final String HALL_STATUS = "persist.sys.hall.opened";
    public static final String FUN_PROP = "persist.sys.hall.fun";
    public static final String ANIM_PROP = "persist.sys.hall.anim";
    public static final String SOUND_PROP = "persist.sys.hall.sound";
    public static final String SCREEN_LOCK_PROP = "sys.unlock.attach";
    public static final String PERMIT_USE = "persist.sys.permit.use";
    public static final String CAMERA_STATUS = "persist.sys.forbid.album";
    public static final String DEMO_MODE = "persist.sys.bt.follolocal";
    public static final String HIGH_TEMPERATURE = "persist.sys.bt.isHighTempe";
    public static final String WATCH_LOSS = "persist.sys.watchlossstatus";
    public static final String CLASS_MODE = "persist.sys.classmodestatus";
    public static final String SAVE_POWER = "persist.sys.setting_savepower";
    public static final String VIDEO_CALL = "persist.sys.vc.calling";
    public static final String ALARM_PROP = "persist.sys.alarmring";
    public static final String WATCH_COLOR = "ro.xtcwatch.color";

    public static final String ANIM_SHOW = "1";
    public static final String ANIM_HIDE = "0";
    public static final String OPEN_ANIM = "open.animation";
    public static final String CLOSE_ANIM = "close.animation";

    public static final String MENU_ORDER = "camera.menu.order";
    public static final String MENU_VISIBILITY = "menu.visibility";
    public static final String KEY_PHOTO = "take_photo";
    public static final String HALL_EXTRA_KEY = "xtc_hall_open";
    public static final String SWITCH_INIT_COMPLETE = "com.xtc.funmanager.FUN_DATA_INIT_COMPLETE";
    public static final String ALARM_START = "com.xtc.alarmclock.action.ALARM_VIEW_SHOWING";

    public static final int KEY_MENU_OPEN = 1000;
    public static final int KEY_MENU_CLOSE = 1001;
    public static final int KEY_MENU_CLICK = 1002;
    public static final int KEY_HALL_OPEN = 1003;
    public static final int KEY_HALL_CLOSE = 1004;
    public static final int DEFAULT_FUNCTION = 1;
    public static final int NO_FUNCTION = -1;
    public static final int FROZEN_COLOR = 4;
    public static final int GIRL_COLOR = 1;
    public static final int FLIP_RESTRICT_MODULE = 2207;

    public static final String FLIP_LIMIT_JSON =
            "{\"enable\":true, \"flipCount\":10, \"flipDuration\":60,\"disableDuration\":180}";

    public static final boolean IS_SDW2700 = Build.VERSION.SDK_INT >= 26;
    public static final long DELAY_TIME = IS_SDW2700 ? 5000 : 10000;

    public static String INNER_MODEL = SystemProperties.get("ro.product.innermodel");

    public static boolean hallOpen() {
        return SystemProperties.getBoolean(HALL_STATUS, false);
    }

    public static void setHall(boolean open) {
        SystemProperties.set(HALL_STATUS, String.valueOf(open));
    }

    public static boolean kernelHallOpen() {
        boolean result = false;
        try {
            FileReader fileReader = new FileReader("/proc/xtchall");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            result = "1".equals(bufferedReader.readLine());
            bufferedReader.close();
            fileReader.close();
        } catch (FileNotFoundException e) {
            Log.w(TAG, "no hall dev", e);
        } catch (IOException e) {
            Log.w(TAG, "get hall status error", e);
        }
        return result;
    }

    public static boolean demoMode() {
        return SystemProperties.getBoolean(DEMO_MODE, false);
    }

    public static boolean specialMode() {
        return SystemProperties.getBoolean(HIGH_TEMPERATURE, false)
                || SystemProperties.getBoolean(WATCH_LOSS, false)
                || SystemProperties.getBoolean(CLASS_MODE, false)
                || SystemProperties.getBoolean(SAVE_POWER, false)
                || SystemProperties.getBoolean(CAMERA_STATUS, false);
    }

    public static boolean secureLock() {
        return SystemProperties.getBoolean("persist.sys.passwordlock", false);
    }

    public static boolean lockShow() {
        return SystemProperties.getBoolean(SCREEN_LOCK_PROP, false) || secureLock();
    }

    public static boolean isHallSnapshot() {
        return SystemProperties.getBoolean("persist.sys.hall.snapshot", false);
    }

    public static void printStatusLog(boolean boot, String fun) {
        Log.d(TAG, "status permitUse:" + SystemProperties.getBoolean(PERMIT_USE, false)
                + " boot:" + boot + " fun:" + fun
                + " rejectCall:" + SystemProperties.getBoolean(HIGH_TEMPERATURE, false)
                + " watchLoss:" + SystemProperties.getBoolean(WATCH_LOSS, false)
                + " classStatus:" + SystemProperties.getBoolean(CLASS_MODE, false)
                + " savePower:" + SystemProperties.getBoolean(SAVE_POWER, false)
                + " vcCalling:" + SystemProperties.getBoolean(VIDEO_CALL, false)
                + " isAlarming:" + SystemProperties.getBoolean(ALARM_PROP, false)
                + " camera:" + SystemProperties.getBoolean(CAMERA_STATUS, false));
    }

    public static void enablePalmSleep() {
        SystemProperties.set("persist.sys.bigobject", String.valueOf(1));
        Log.d(TAG, "enable big object");
    }

    public static void sendHallKeyEventAsync(boolean open) {
        int keyCode = open ? KEY_HALL_OPEN : KEY_HALL_CLOSE;
        long now = SystemClock.uptimeMillis();
        InputManager.getInstance().injectInputEvent(
                new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0, 0, 5, 0, 8, 257), 0);
        InputManager.getInstance().injectInputEvent(
                new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0, 0, 5, 0, 8, 257), 0);
    }

    public static int[] getPerfLockParam() {
        return IS_SDW2700
                ? new int[]{1082130432, WindowManager.LayoutParams.TYPE_DRAG, 1090519040, 4}
                : new int[]{523, 2059, BluetoothClass.Device.WEARABLE_WRIST_WATCH};
    }
}