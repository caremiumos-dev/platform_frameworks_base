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

package com.android.server.performance.status;

import android.os.StrictMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

/**
 * Snapshot of {@code /proc/meminfo} used by the performance policies.
 *
 * @hide
 */
public class RamStatus {
    private static final String TAG_MEM_AVAILABLE = "MemAvailable";
    private static final String TAG_MEM_FREE = "MemFree";
    private static final String TAG_MEM_TOTAL = "MemTotal";
    private static final String TAG_MEM_CACHED = "Cached";

    private static final long ONE_MEGABYTE = 1048576L;

    private final HashMap<String, Long> mMeminfoMap = new HashMap<>();

    public RamStatus() {
        resetMemMap();
    }

    private void resetMemMap() {
        synchronized (this.mMeminfoMap) {
            this.mMeminfoMap.clear();
            this.mMeminfoMap.put(TAG_MEM_AVAILABLE, 0L);
            this.mMeminfoMap.put(TAG_MEM_FREE, 0L);
            this.mMeminfoMap.put(TAG_MEM_TOTAL, 0L);
            this.mMeminfoMap.put(TAG_MEM_CACHED, 0L);
        }
    }

    private void update() {
        resetMemMap();
        synchronized (this.mMeminfoMap) {
            try (BufferedReader reader = new BufferedReader(new FileReader(
                    new File("/proc/meminfo")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    for (String key : this.mMeminfoMap.keySet()) {
                        if (line.startsWith(key)) {
                            long value = Long.parseLong(line.replace(key + ":", "")
                                    .replace("kB", "").trim());
                            this.mMeminfoMap.put(key, value);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public long getAvailableMemKb() {
        synchronized (this.mMeminfoMap) {
            return this.mMeminfoMap.get(TAG_MEM_AVAILABLE);
        }
    }

    public long getCachedMemKb() {
        synchronized (this.mMeminfoMap) {
            return this.mMeminfoMap.get(TAG_MEM_CACHED);
        }
    }

    public long getFreeMemKb() {
        synchronized (this.mMeminfoMap) {
            return this.mMeminfoMap.get(TAG_MEM_FREE);
        }
    }

    public int getRamConfig() {
        int totalMemMb = (int) (getTotalMemKb() / ONE_MEGABYTE);
        if (totalMemMb > 0 && totalMemMb <= 512) {
            return 512;
        }
        if (totalMemMb > 512 && totalMemMb <= 768) {
            return 768;
        }
        if (totalMemMb > 768 && totalMemMb <= 1024) {
            return 1024;
        }
        if (totalMemMb > 1024 && totalMemMb <= 2048) {
            return 2048;
        }
        if (totalMemMb <= 2048 || totalMemMb > 3072) {
            return totalMemMb > 3072 ? 4096 : 1024;
        }
        return 3072;
    }

    public long getTotalMemKb() {
        synchronized (this.mMeminfoMap) {
            return this.mMeminfoMap.get(TAG_MEM_TOTAL);
        }
    }

    public void updateMemInfo() {
        StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
        update();
        StrictMode.setThreadPolicy(oldPolicy);
    }
}