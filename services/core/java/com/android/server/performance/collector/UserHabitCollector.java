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

import android.app.SectionHabit;
import android.app.UserHabit;
import android.os.Handler;
import android.os.UserHandle;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Xml;

import com.android.server.performance.PerformanceManagerDebugConfig;
import com.android.server.performance.PerformanceManagerServiceImpl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Collects per-application usage habits and persists them under
 * {@code /data/system/usrhabit/habit.xml}.
 *
 * <p><b>Condemnation.</b> Builds a long-lived profile of what the user runs,
 * when, and for how long. See {@link android.app.UserHabit} and
 * {@link android.app.SectionHabit}.
 *
 * @hide
 */
public class UserHabitCollector {
    private static final String TAG = "xtcperformance-Habit";

    private static final int EVENT_MOVE_TO_FG = 0;
    private static final int EVENT_MOVE_TO_BG = 1;

    private static final String TAG_USER_HABIT = "userhabit";
    private static final String TAG_SECTION_HABIT = "sectionhabit";
    private static final String TAG_USER = "user";
    private static final String ATTR_USER_ID = "user_id";
    private static final String USER_HABIT_FILE = "/data/system/usrhabit/habit.xml";
    private static final long POLLING_DELAY = 10800000L;

    final Handler mBgHandler;
    private final PerformanceManagerServiceImpl mService;
    final HashMap<Integer, HashMap<String, UserHabit>> mUserHabits = new HashMap<>();
    final HashMap<Integer, SectionHabit> mSectionHabits = new HashMap<>();

    public UserHabitCollector(PerformanceManagerServiceImpl service, Handler handler) {
        this.mService = service;
        this.mBgHandler = handler;
        loadUserHabitsFromFile();
        if (PerformanceManagerDebugConfig.DEBUG_USRHABIT) {
            dumpHabits();
        }
        scheduleSyncWithFile();
    }

    private void dumpHabits() {
        HashMap<Integer, SectionHabit> sections;
        synchronized (this.mSectionHabits) {
            sections = (HashMap<Integer, SectionHabit>) this.mSectionHabits.clone();
        }
        synchronized (this.mUserHabits) {
            for (Integer userId : this.mUserHabits.keySet()) {
                HashMap<String, UserHabit> map = this.mUserHabits.get(userId);
                for (String packageName : map.keySet()) {
                    Log.d(TAG, "habit->" + map.get(packageName));
                }
                sections.get(userId).dumpSectionHabits();
            }
        }
    }

    private void saveUserHabitsToFile() {
        if (PerformanceManagerDebugConfig.DEBUG_USRHABIT) {
            dumpHabits();
        }
        Log.d(TAG, "saveUserHabitsToFile start");
        StringWriter writer = new StringWriter();
        AtomicFile atomicFile = new AtomicFile(new File(USER_HABIT_FILE));
        FileOutputStream output = null;
        try {
            XmlSerializer serializer = XmlPullParserFactory.newInstance().newSerializer();
            serializer.setOutput(writer);
            serializer.startDocument(null, true);
            HashMap<Integer, HashMap<String, UserHabit>> userSnapshot;
            HashMap<Integer, SectionHabit> sectionSnapshot;
            synchronized (this.mUserHabits) {
                userSnapshot = (HashMap<Integer, HashMap<String, UserHabit>>) this.mUserHabits
                        .clone();
            }
            synchronized (this.mSectionHabits) {
                sectionSnapshot = (HashMap<Integer, SectionHabit>) this.mSectionHabits.clone();
            }
            for (Integer userId : userSnapshot.keySet()) {
                HashMap<String, UserHabit> map = userSnapshot.get(userId);
                if (map != null) {
                    serializer.startTag(null, TAG_USER);
                    serializer.attribute(null, ATTR_USER_ID, String.valueOf(userId));
                    for (String packageName : map.keySet()) {
                        UserHabit habit = map.get(packageName);
                        if (habit != null) {
                            serializer.startTag(null, TAG_USER_HABIT);
                            habit.writeToFile(packageName, serializer);
                            serializer.endTag(null, TAG_USER_HABIT);
                        }
                    }
                    if (PerformanceManagerDebugConfig.DEBUG_USRHABIT) {
                        Log.d(TAG, "saveUserHabitsToFile->" + sectionSnapshot.get(userId));
                    }
                    sectionSnapshot.get(userId).writeToFile(serializer);
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
        Log.d(TAG, "saveUserHabitsToFile end");
    }

    private void scheduleSyncWithFile() {
        this.mBgHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                saveUserHabitsToFile();
                scheduleSyncWithFile();
            }
        }, POLLING_DELAY);
    }

    private void updateHabitStatusInnder(int event, Integer userId, String packageName,
            boolean isDifferent) {
        UserHabit habit = getOrCreatUserHabit(packageName, userId);
        SectionHabit sectionHabit = getOrCreatSectionHabit(userId);
        if (PerformanceManagerDebugConfig.DEBUG_USRHABIT) {
            Log.d(TAG, "updateHabitStatusInnder:" + habit + " event:" + event);
        }
        long now = System.currentTimeMillis();
        synchronized (this.mUserHabits) {
            synchronized (this.mSectionHabits) {
                switch (event) {
                    case EVENT_MOVE_TO_FG:
                        habit.mLastEvent = 0;
                        habit.launchCount++;
                        habit.lastForegroundTime = now;
                        if (this.mService.isExecutableApp(packageName)) {
                            sectionHabit.updateSectionInfo(packageName, now, true);
                        }
                        break;
                    case EVENT_MOVE_TO_BG:
                        if (habit.mLastEvent == 0) {
                            habit.totalForegroundTime += now - habit.lastForegroundTime;
                            if (this.mService.isExecutableApp(packageName)) {
                                sectionHabit.updateSectionInfo(packageName,
                                        habit.lastForegroundTime, false);
                            }
                        }
                        habit.mLastEvent = 1;
                        habit.lastUseTime = now;
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void updateHabitsLaunchTimeInner(Integer userId, String packageName, long launchTime) {
        UserHabit habit = getOrCreatUserHabit(packageName, userId);
        if (PerformanceManagerDebugConfig.DEBUG_USRHABIT) {
            Log.d(TAG, "updateHabitsLaunchTimeInner:" + habit + " time:" + launchTime);
        }
        synchronized (this.mUserHabits) {
            habit.updateLaunchTime(String.valueOf(launchTime));
        }
    }

    public void dump(java.io.FileDescriptor fd, PrintWriter writer, String[] args) {
        HashMap<Integer, HashMap<String, UserHabit>> userSnapshot;
        HashMap<Integer, SectionHabit> sectionSnapshot;
        writer.println("USAGE STATICS:");
        writer.println("----------------------------");
        synchronized (this.mUserHabits) {
            userSnapshot = (HashMap<Integer, HashMap<String, UserHabit>>) this.mUserHabits.clone();
        }
        synchronized (this.mSectionHabits) {
            sectionSnapshot = (HashMap<Integer, SectionHabit>) this.mSectionHabits.clone();
        }
        for (Integer userId : userSnapshot.keySet()) {
            HashMap<String, UserHabit> map = userSnapshot.get(userId);
            for (String packageName : map.keySet()) {
                writer.println("habit->" + map.get(packageName));
            }
            sectionSnapshot.get(userId).dump(fd, writer);
        }
    }

    public SectionHabit getOrCreatSectionHabit(Integer userId) {
        synchronized (this.mSectionHabits) {
            SectionHabit habit = this.mSectionHabits.get(userId);
            if (habit == null) {
                habit = new SectionHabit();
                habit.mStartTime = System.currentTimeMillis();
                this.mSectionHabits.put(userId, habit);
            }
            return habit;
        }
    }

    UserHabit getOrCreatUserHabit(String packageName, Integer userId) {
        synchronized (this.mUserHabits) {
            HashMap<String, UserHabit> map = getUserHabitHashMap(userId);
            UserHabit habit = map.get(packageName);
            if (habit == null) {
                habit = new UserHabit(packageName, userId);
                map.put(packageName, habit);
            }
            return habit;
        }
    }

    public UserHabit getPackageUserHabits(Integer userId, String packageName) {
        return getOrCreatUserHabit(packageName, userId);
    }

    HashMap<String, UserHabit> getUserHabitHashMap(int userId) {
        synchronized (this.mUserHabits) {
            HashMap<String, UserHabit> map = this.mUserHabits.get(userId);
            if (map == null) {
                map = new HashMap<>();
                this.mUserHabits.put(userId, map);
            }
            return map;
        }
    }

    public ArrayList<UserHabit> getUserHabits(Integer userId) {
        ArrayList<UserHabit> result = new ArrayList<>();
        synchronized (this.mUserHabits) {
            HashMap<String, UserHabit> map = getUserHabitHashMap(userId);
            for (String packageName : map.keySet()) {
                UserHabit habit = map.get(packageName);
                if (habit != null) {
                    result.add(habit);
                }
            }
        }
        return result;
    }

    public void loadUserHabitsFromFile() {
        File file = new File(USER_HABIT_FILE);
        if (!file.exists()) {
            Log.d(TAG, "maybe first boot, config file not exist");
            return;
        }
        Log.d(TAG, "loadUserHabitsFromFile start");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(reader);
            HashMap<String, UserHabit> map = new HashMap<>();
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
                    } else if (TAG_USER_HABIT.equals(tag)) {
                        UserHabit habit = UserHabit.restoreFromFile(parser);
                        map.put(habit.packageName, habit);
                    } else if (TAG_SECTION_HABIT.equals(tag)) {
                        SectionHabit sectionHabit = SectionHabit.restoreFromFile(parser);
                        synchronized (this.mSectionHabits) {
                            this.mSectionHabits.put(userId, sectionHabit);
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && TAG_USER.equals(tag)) {
                    synchronized (this.mUserHabits) {
                        this.mUserHabits.put(userId, map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            file.delete();
            synchronized (this.mUserHabits) {
                this.mUserHabits.clear();
            }
            synchronized (this.mSectionHabits) {
                this.mSectionHabits.clear();
            }
            Log.e(TAG, "unable get userhabit date.. deleting");
        }
        Log.d(TAG, "loadUserHabitsFromFile end");
    }

    public void onShutDown() {
        synchronized (this.mUserHabits) {
            saveUserHabitsToFile();
        }
    }

    public void updateHabitStatus(String packageName, boolean foreground, boolean isDifferent) {
        if (packageName == null) {
            return;
        }
        try {
            updateHabitStatusInnder(foreground ? EVENT_MOVE_TO_FG : EVENT_MOVE_TO_BG,
                    UserHandle.myUserId(), packageName, isDifferent);
        } catch (Exception e) {
            if (PerformanceManagerDebugConfig.DEBUG_USRHABIT) {
                Log.d(TAG, "updateHabitStatus:" + e);
            }
        }
    }

    public void updateHabitsLaunchTime(String packageName, long launchTime) {
        try {
            updateHabitsLaunchTimeInner(UserHandle.myUserId(), packageName, launchTime);
        } catch (Exception e) {
            // ignored
        }
    }
}