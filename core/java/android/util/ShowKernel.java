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

package android.util;

import android.os.SystemProperties;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Vendor helper that writes a status string to a kernel power-supply node and
 * mirrors it into the {@code log.tag.shutdown} system property.
 *
 * @hide
 */
public final class ShowKernel {
    private static final String TAG = "ShowKernel";
    private static final String mPowerString = "/sys/class/power_supply/battery/target_info";

    private ShowKernel() {
    }

    public static int v(String value, String logTag) {
        wToKernelLog(mPowerString, value);
        SystemProperties.set("log.tag.shutdown", logTag);
        return 0;
    }

    public static void wToKernelLog(String path, String content) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException start");
        } catch (IOException e) {
            Log.e(TAG, "IOException");
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }
}