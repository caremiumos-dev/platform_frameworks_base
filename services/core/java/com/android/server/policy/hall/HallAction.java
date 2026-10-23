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
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

/**
 * Base class shared by the hall (flip cover) sub-controllers. It provides the
 * setting helpers and the ui/work handler plumbing.
 *
 * @hide
 */
public abstract class HallAction {
    static final int DISABLE_INDEX = -1;
    static final String DISABLE_VALUE = "none";

    static final int MSG_INIT = 0;
    static final int MSG_SHOW_VIEW = 1;
    static final int MSG_HIDE_VIEW = 2;
    static final int MSG_PLAY_ANIM = 3;
    static final int MSG_GOTO_SLEEP = 4;
    static final int MSG_LOAD_ANIM = 5;
    static final int MSG_SHOW_CLOSE_VIEW = 6;
    static final int MSG_WAKE_UP = 7;
    static final int MSG_ABANDON_FOCUS = 8;
    static final int MSG_HIDE_VIEW_FORCE = 9;
    static final int MSG_STOP_SOUND = 10;
    static final int MSG_SHOW_TIP = 11;
    static final int MSG_HIDE_TIP = 12;
    static final int MSG_BOOT_COMPLETED = 13;
    static final int MSG_MODULE_SWITCH_CHANGED = 14;
    static final int MSG_FUNCTION_SWITCH_CHANGED = 15;
    static final int MSG_RELOAD_MENU = 16;
    static final int MSG_HALL_EVENT = 17;
    static final int MSG_PLAY_SOUND = 18;
    static final int MSG_SEND_BEHAVIOR = 19;
    static final int MSG_HIDE_OPEN_ANIM = 20;

    static final String PREFIX = "Sys";

    private static final String TAG = PREFIX + HallAction.class.getSimpleName();

    Context context;
    Handler uiHandler;
    Handler workHandler;

    public HallAction(Context context) {
        this.context = context;
    }

    boolean fileCanRead(String path) {
        if (TextUtils.isEmpty(path)) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.canRead();
    }

    String getSetting(String key) {
        return Settings.System.getString(this.context.getContentResolver(), key);
    }

    void putSetting(String key, String value) {
        if (!Settings.System.putString(this.context.getContentResolver(), key, value)) {
            Log.w(TAG, "put setting failed key:" + key + " value:" + value);
        }
    }

    public void setHandler(Handler uiHandler, Handler workHandler) {
        this.uiHandler = uiHandler;
        this.workHandler = workHandler;
    }

    public abstract void init();
}