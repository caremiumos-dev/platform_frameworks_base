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

import android.app.ActivityManager;
import android.app.ProcState;
import android.os.Debug;
import android.os.Handler;
import android.os.UserHandle;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import com.android.server.performance.PerformanceManagerDebugConfig;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Collects per-process top-PSS statistics and persists them under
 * {@code /data/system/usrhabit/procs.xml}.
 *
 * @hide
 */
public class ProcessStateCollector {
    private static final String TAG = "xtcperformance-ProcState";

    private static final String TAG_PROC = "process";
    private static final String TAG_USER = "user";
    private static final String ATTR_USER_ID = "user_id";
    private static final String PROCSTATE_FILE = "/data/system/usrhabit/procs.xml";
    private static final long POLLING_DELAY = 21600000L;

    private static final int PROCESS_STATE_TOP = 2;

    final ActivityManager mAm;
    final Handler mHandler;
    final HashMap<Integer, HashMap<String, ProcState>> mProcStates = new HashMap<>();

    public ProcessStateCollector(ActivityManager am, Handler handler) {
        this.mAm = am;
        this.mHandler = handler;
        loadProcStatesFromFile();
        scheduleSyncWithFile();
    }

    private void dumpProcStates() {
        synchronized (this.mProcStates) {
            for (Integer userId : this.mProcStates.keySet()) {
                HashMap<String, ProcState> map = this.mProcStates.get(userId);
                for (String processName : map.keySet()) {
                    Log.d(TAG, "procState->" + map.get(processName));
                }
            }
        }
    }

    private void loadProcStatesFromFile() {
        File file = new File(PROCSTATE_FILE);
        if (!file.exists()) {
            Log.d(TAG, "maybe first boot, config file not exist");
            return;
        }
        Log.d(TAG, "loadProcStatesFromFile start");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(reader);
            HashMap<String, ProcState> map = new HashMap<>();
            int userId = 0;
            while (true) {
                int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                String tag = parser.getName();
                if (event == XmlPullParser.START_TAG) {
                    if (TAG_USER.equals(tag)) {
                        map = new HashMap<>();
                        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
                            if (ATTR_USER_ID.equals(parser.getAttributeName(i))) {
                                userId = Integer.valueOf(parser.getAttributeValue(i));
                            }
                        }
                    } else if (TAG_PROC.equals(tag)) {
                        ProcState state = ProcState.restoreFromFile(parser);
                        map.put(state.processName, state);
                        if (PerformanceManagerDebugConfig.DEBUG_PROCSTAT) {
                            Log.d(TAG, "loadProcStatesFromFile procState-->" + state);
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && TAG_USER.equals(tag)) {
                    synchronized (this.mProcStates) {
                        this.mProcStates.put(userId, map);
                    }
                }
            }
        } catch (Exception e) {
            file.delete();
            synchronized (this.mProcStates) {
                this.mProcStates.clear();
            }
        }
        Log.d(TAG, "loadProcStatesFromFile end");
    }

    private void saveProcStatesToFile() {
        Log.d(TAG, "saveProcStatesToFile start");
        StringWriter writer = new StringWriter();
        AtomicFile atomicFile = new AtomicFile(new File(PROCSTATE_FILE));
        FileOutputStream output = null;
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(writer);
            serializer.startDocument(null, true);
            HashMap<Integer, HashMap<String, ProcState>> snapshot;
            synchronized (this.mProcStates) {
                snapshot = (HashMap<Integer, HashMap<String, ProcState>>) this.mProcStates.clone();
            }
            for (Integer userId : snapshot.keySet()) {
                HashMap<String, ProcState> map = snapshot.get(userId);
                if (map != null) {
                    serializer.startTag(null, TAG_USER);
                    serializer.attribute(null, ATTR_USER_ID, String.valueOf(userId));
                    for (String processName : map.keySet()) {
                        ProcState state = map.get(processName);
                        if (state != null) {
                            serializer.startTag(null, TAG_PROC);
                            state.writeToFile(serializer);
                            serializer.endTag(null, TAG_PROC);
                        }
                    }
                    serializer.endTag(null, TAG_USER);
                }
            }
            serializer.endDocument();
            serializer.flush();
            output = atomicFile.startWrite();
            output.write(writer.toString().getBytes());
            output.write(10);
            atomicFile.finishWrite(output);
        } catch (Exception e) {
            e.printStackTrace();
            if (output != null) {
                atomicFile.failWrite(output);
            }
        }
        Log.d(TAG, "saveProcStatesToFile end");
    }

    private void scheduleSyncWithFile() {
        this.mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                saveProcStatesToFile();
                scheduleSyncWithFile();
            }
        }, POLLING_DELAY);
    }

    private void updateProcStatesInnder() {
        if (PerformanceManagerDebugConfig.DEBUG_PROCSTAT) {
            Slog.d(TAG, "updateProcStatesInnder");
        }
        try {
            java.util.List<ActivityManager.RunningAppProcessInfo> processes =
                    this.mAm.getRunningAppProcesses();
            if (processes == null || processes.size() == 0) {
                Slog.e(TAG, "processesList is null");
                return;
            }
            ActivityManager.RunningAppProcessInfo top = null;
            for (int i = 0; i < processes.size(); i++) {
                ActivityManager.RunningAppProcessInfo info = processes.get(i);
                if (info != null && info.processState == PROCESS_STATE_TOP) {
                    top = info;
                    break;
                }
            }
            if (top == null) {
                return;
            }
            ProcState state = getOrCreatProcState(top.processName, UserHandle.myUserId());
            synchronized (this.mProcStates) {
                state.avgTopPss = (Debug.getPss(top.pid, null, null)
                        + state.avgTopPss * state.topPssSampleCount)
                        / (state.topPssSampleCount + 1);
                state.topPssSampleCount++;
            }
            if (PerformanceManagerDebugConfig.DEBUG_PROCSTAT) {
                Slog.d(TAG, "updateProcStatesInnder top.processName=" + top.processName
                        + " avgTopPss=" + state.avgTopPss);
            }
        } catch (Exception e) {
            // ignored
        }
    }

    public void dump(java.io.FileDescriptor fd, java.io.PrintWriter writer, String[] args) {
        writer.println("PROCESS STATUS:");
        writer.println("----------------------------");
        synchronized (this.mProcStates) {
            for (Integer userId : this.mProcStates.keySet()) {
                HashMap<String, ProcState> map = this.mProcStates.get(userId);
                for (String processName : map.keySet()) {
                    writer.println("procState->" + map.get(processName));
                }
            }
        }
        writer.println("----------------------------");
    }

    ProcState getOrCreatProcState(String processName, Integer userId) {
        synchronized (this.mProcStates) {
            HashMap<String, ProcState> map = getProcStateHashMap(userId);
            ProcState state = map.get(processName);
            if (state == null) {
                state = new ProcState(processName, userId);
                map.put(processName, state);
            }
            return state;
        }
    }

    HashMap<String, ProcState> getProcStateHashMap(int userId) {
        synchronized (this.mProcStates) {
            HashMap<String, ProcState> map = this.mProcStates.get(userId);
            if (map == null) {
                map = new HashMap<>();
                this.mProcStates.put(userId, map);
            }
            return map;
        }
    }

    public ProcState getProcStates(Integer userId, String processName) {
        return getOrCreatProcState(processName, userId);
    }

    public ArrayList<ProcState> getProcStates(Integer userId) {
        ArrayList<ProcState> result = new ArrayList<>();
        synchronized (this.mProcStates) {
            HashMap<String, ProcState> map = getProcStateHashMap(userId);
            for (String processName : map.keySet()) {
                ProcState state = map.get(processName);
                if (state != null) {
                    result.add(state);
                }
            }
        }
        return result;
    }

    public void onShutDown() {
        synchronized (this.mProcStates) {
            saveProcStatesToFile();
        }
    }

    public void updateProcStates() {
        try {
            updateProcStatesInnder();
        } catch (Exception e) {
            // ignored
        }
    }
}