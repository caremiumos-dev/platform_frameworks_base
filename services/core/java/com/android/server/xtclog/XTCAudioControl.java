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

package com.android.server.xtclog;

import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import com.xtc.moduleswitch.ModuleSwitch;

/**
 * Applies the vendor module switch controlling the audio feature flag.
 *
 * @hide
 */
public class XTCAudioControl {
    static final String TAG = "XTCAudioControl";
    public static final boolean DEBUG = true;

    private static final int AUDIO_CONTROL_MODULE_SWITCH = 3154;
    private static final String PROP_AUDIO_SWITCH = "persist.sys.audio.switch";

    private final Context mContext;

    public XTCAudioControl(Context context) {
        this.mContext = context;
    }

    private boolean getControlSwitch() {
        return ModuleSwitch.queryModuleSwitch(this.mContext, AUDIO_CONTROL_MODULE_SWITCH, false);
    }

    public void doAudioControl() {
        boolean controlSwitch = getControlSwitch();
        Log.d(TAG, "moduleswitch is " + controlSwitch + " prop is "
                + SystemProperties.get(PROP_AUDIO_SWITCH));
        if (controlSwitch) {
            SystemProperties.set(PROP_AUDIO_SWITCH, "true");
            Log.d(TAG, "prop is " + SystemProperties.get(PROP_AUDIO_SWITCH));
        } else {
            SystemProperties.set(PROP_AUDIO_SWITCH, "false");
            Log.d(TAG, "prop is " + SystemProperties.get(PROP_AUDIO_SWITCH));
        }
    }
}