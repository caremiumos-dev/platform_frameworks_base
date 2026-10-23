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

package com.android.server.performance.collector;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Slog;

import com.android.server.performance.PerformanceManagerServiceImpl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Collects blocking stack traces reported by the runtime and dumps the merged
 * call-tree to a file.
 *
 * <p><b>Condemnation.</b> This silently captures stack traces of running
 * applications and persists them under {@code /xtcdata}. It is an invasive
 * monitoring capability, reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class CatonStackCollector {
    public static final String CATON_OPEN_PROP = "persist.sys.caton.open";

    private static final String TAG = "xtcperformance-catonstack";

    private static final int MSG_UPDATE_CATON_STACK = 0;
    private static final String DEFAULT_LOG_DIR = "/xtcdata/Logs_Collector/";

    private final Handler mHandler;
    private final PerformanceManagerServiceImpl mService;
    private final Object mLock = new Object();
    private final HandlerThread mThread = new HandlerThread("catonstack-collector", 10);

    private boolean isRunningMonitor = false;

    public CatonStackCollector(PerformanceManagerServiceImpl service) {
        this.mService = service;
        this.mThread.start();
        this.mHandler = new Handler(this.mThread.getLooper()) {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_CATON_STACK:
                        handleUpdateCatonStack((ArrayList<String>) message.obj);
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private static void buildNodeAndAddToMainNode(ArrayList<String> lines) {
        BlockStackBuilder.mainNode = BlockStackBuilder.merge(BlockStackBuilder.mainNode,
                BlockStackBuilder.buildBlockNode(lines));
    }

    private void handleUpdateCatonStack(ArrayList<String> lines) {
        synchronized (this.mLock) {
            buildNodeAndAddToMainNode(lines);
        }
    }

    public static void print2File() {
        if (BlockStackBuilder.mainNode == null) {
            Slog.e(TAG, "print2File mainNode is null");
            return;
        }
        Slog.d(TAG, "print2File start");
        String dir = SystemProperties.get("log.tag.splog_dir");
        try {
            if (dir == null || "".equals(dir)) {
                dir = DEFAULT_LOG_DIR;
            }
            File dirFile = new File(dir);
            if (!dirFile.exists()) {
                dirFile.mkdirs();
            }
            Calendar calendar = Calendar.getInstance();
            String name = "block-tree_" + calendar.get(Calendar.YEAR) + "-"
                    + (calendar.get(Calendar.MONTH) + 1) + "-" + calendar.get(Calendar.DAY_OF_MONTH)
                    + "-" + calendar.get(Calendar.HOUR_OF_DAY) + "-" + calendar.get(Calendar.MINUTE)
                    + "-" + calendar.get(Calendar.SECOND) + ".txt";
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(new File(dirFile, name)))) {
                ArrayList<String> lines = new ArrayList<>();
                BlockStackBuilder.blockNodeList2StringList(BlockStackBuilder.mainNode, lines);
                for (String line : lines) {
                    writer.write(line);
                }
                BlockStackBuilder.mainNode = null;
            }
            Slog.d(TAG, "print2File end");
        } catch (IOException e) {
            Slog.e(TAG, "print2File e : " + e);
        }
    }

    public boolean isRunning() {
        return this.isRunningMonitor;
    }

    public void scheduleUpdateCatonStack(List<String> lines) {
        this.isRunningMonitor = true;
        this.mHandler.removeMessages(MSG_UPDATE_CATON_STACK);
        this.mHandler.obtainMessage(MSG_UPDATE_CATON_STACK, lines).sendToTarget();
    }
}