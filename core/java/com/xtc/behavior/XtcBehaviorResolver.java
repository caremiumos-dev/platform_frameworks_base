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
import android.net.Uri;
import android.util.Log;

import java.io.File;

/**
 * Writes behavior records into the vendor launcher content provider.
 *
 * <p><b>Condemnation.</b> This silently funnels application usage/telemetry to
 * a vendor provider. Users have no meaningful control over this data
 * collection. Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class XtcBehaviorResolver {
    private static final boolean DEBUG = false;
    private static final String TAG = "XtcBehaviorResolver";

    private static final Uri URI_BEHAVIOR =
            Uri.parse("content://com.xtc.i3launcher.provider" + File.separator + "userbehavior");

    private void logd(String message) {
    }

    public void insert(Context context, ContentValues values) {
        if (context == null || values == null) {
            return;
        }
        try {
            context.getContentResolver().insert(URI_BEHAVIOR, values);
            logd("XtcBehavior send data values: " + values);
        } catch (Exception e) {
            Log.w(TAG, "XtcBehavior send data failed !!");
        }
    }
}