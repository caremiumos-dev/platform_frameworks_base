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

package android.app;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Time-of-day ("section") usage statistics collected by the XTC performance
 * subsystem. Usage is bucketed into six day segments and persisted.
 *
 * <p><b>Condemnation.</b> This builds a detailed time-of-day profile of the
 * user's application usage. Reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class SectionHabit implements Serializable {
    public static final String TAG_NAME = "SectionHabit";

    public static final String ATTR_NAME = "name";
    public static final String ATTR_AR_TYPE = "type";
    public static final String ATTR_LAUNCHCOUNT = "launchcount";
    public static final String ATTR_FGTIME = "fgtime";
    public static final String ATTR_LAUNCHCOUNT_INDEX = "launchcount_index";
    public static final String ATTR_FGTIME_INDEX = "fgtime_index";

    public static final String CONF_TAG_SECTIONHABIT = "sectionhabit";
    public static final String CONF_TAG_APP_DATA = "appdata";
    public static final String CONF_TAG_PKG = "pkg";

    private static final String SECTION_HABIT_TAG_CURRENT = "current";
    private static final String SECTION_HABIT_TAG_HISTORY = "history";
    private static final String TAG = "xtcperformance-Habit";
    private static final boolean DEBUG_SERVICE = false;

    public static final int TYPE_NOT_CARE = -1;
    public static final int TYPE_SUNRISE = 0;
    public static final int TYPE_MORNING = 1;
    public static final int TYPE_NOON = 2;
    public static final int TYPE_AFTERNOON = 3;
    public static final int TYPE_SUNSET = 4;
    public static final int TYPE_NIGHT = 5;
    public static final int TYPE_COUNT = 6;

    public static final long DURATION_1MIN = 60000L;
    public static final long DURATION_2MIN = 120000L;
    public static final long DURATION_5MIN = 300000L;
    public static final long DURATION_15MIN = 900000L;
    public static final long DURATION_30MIN = 1800000L;
    public static final long DATA_DURATION = 604800000L;

    private static final Object mDataLock = new Object();

    public long mStartTime = 0;
    private HashMap<Integer, SectionInfo> mCurrentData = new HashMap<>();
    private HashMap<Integer, SectionInfo> mHistoryData = new HashMap<>();

    public static class CloneUtils {
        public static <T extends Serializable> T clone(T object) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(object);
                oos.close();
                ObjectInputStream ois = new ObjectInputStream(
                        new ByteArrayInputStream(baos.toByteArray()));
                T copy = (T) ois.readObject();
                try {
                    ois.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return copy;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public static class HabitDate implements Serializable {
        public String packageName;
        public long launchCount;
        public long fgtime;
        public long launchCountIndex;
        public long fgtimeIndex;
        public long lastUsedTime;

        public static HabitDate createFromXml(String packageName, XmlPullParser parser) {
            if (parser == null) {
                return null;
            }
            HabitDate date = new HabitDate();
            date.packageName = packageName;
            if (getAttrValue(parser, ATTR_LAUNCHCOUNT) != null) {
                date.launchCount = Long.valueOf(getAttrValue(parser, ATTR_LAUNCHCOUNT));
            }
            if (getAttrValue(parser, ATTR_FGTIME) != null) {
                date.fgtime = Long.valueOf(getAttrValue(parser, ATTR_FGTIME));
            }
            if (getAttrValue(parser, ATTR_LAUNCHCOUNT_INDEX) != null) {
                date.launchCountIndex = Long.valueOf(getAttrValue(parser, ATTR_LAUNCHCOUNT_INDEX));
            }
            if (getAttrValue(parser, ATTR_FGTIME_INDEX) != null) {
                date.fgtimeIndex = Long.valueOf(getAttrValue(parser, ATTR_FGTIME_INDEX));
            }
            return date;
        }

        @Override
        public String toString() {
            return "package:" + this.packageName + " ,count :" + this.launchCount
                    + ",launchCountIndex:" + this.launchCountIndex + ",fgtime:" + this.fgtime
                    + ",fgtimeIndex:" + this.fgtimeIndex;
        }

        public void writeToFile(XmlSerializer serializer)
                throws XmlPullParserException, IOException {
            serializer.attribute(null, ATTR_LAUNCHCOUNT, String.valueOf(this.launchCount));
            serializer.attribute(null, ATTR_FGTIME, String.valueOf(this.fgtime));
            serializer.attribute(null, ATTR_LAUNCHCOUNT_INDEX,
                    String.valueOf(this.launchCountIndex));
            serializer.attribute(null, ATTR_FGTIME_INDEX, String.valueOf(this.fgtimeIndex));
        }
    }

    public static class SectionInfo implements Serializable {
        public long startTime;
        public long endTime;
        public HashMap<String, HabitDate> sections = new HashMap<>();

        public SectionInfo(int type) {
            switch (type) {
                case TYPE_SUNRISE:
                    this.startTime = 0L;
                    this.endTime = android.app.job.JobInfo.MAX_BACKOFF_DELAY_MILLIS;
                    break;
                case TYPE_MORNING:
                    this.startTime = 18001000L;
                    this.endTime = 39600000L;
                    break;
                case TYPE_NOON:
                    this.startTime = 39601000L;
                    this.endTime = 54000000L;
                    break;
                case TYPE_AFTERNOON:
                    this.startTime = 54001000L;
                    this.endTime = 61200000L;
                    break;
                case TYPE_SUNSET:
                    this.startTime = 61201000L;
                    this.endTime = 68400000L;
                    break;
                case TYPE_NIGHT:
                    this.startTime = 68401000L;
                    this.endTime = 86400000L;
                    break;
                default:
                    break;
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder("");
            synchronized (mDataLock) {
                if (this.sections != null) {
                    for (Map.Entry<String, HabitDate> entry : this.sections.entrySet()) {
                        builder.append("key:").append(entry.getKey())
                                .append(",data:").append(entry.getValue());
                    }
                }
            }
            return "SectionInfo, startTime:" + this.startTime + " ,endTime :" + this.endTime
                    + ",sections:" + builder.toString();
        }
    }

    public SectionHabit() {
        for (int i = 0; i < TYPE_COUNT; i++) {
            this.mCurrentData.put(i, new SectionInfo(i));
            this.mHistoryData.put(i, new SectionInfo(i));
        }
    }

    private void dumpDateLocked(PrintWriter writer, HashMap<Integer, SectionInfo> map) {
        for (Map.Entry<Integer, SectionInfo> entry : map.entrySet()) {
            HashMap<String, HabitDate> sections = entry.getValue().sections;
            writer.println("---type " + entry.getKey() + "---");
            for (Map.Entry<String, HabitDate> section : sections.entrySet()) {
                writer.println(section.getKey() + " " + section.getValue());
            }
        }
    }

    private void dumpSectionHabitsDateLocked(HashMap<Integer, SectionInfo> map) {
        for (Map.Entry<Integer, SectionInfo> entry : map.entrySet()) {
            for (Map.Entry<String, HabitDate> section : entry.getValue().sections.entrySet()) {
                Log.d(TAG, "dumpSectionHabits->" + section.getKey() + ",data:"
                        + section.getValue());
            }
        }
    }

    private static String getAttrValue(XmlPullParser parser, String name) {
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
            if (name.equals(parser.getAttributeName(i))) {
                return parser.getAttributeValue(i);
            }
        }
        return null;
    }

    private HabitDate getHabitDate(HashMap<Integer, SectionInfo> map, int type,
            String packageName) {
        synchronized (mDataLock) {
            if (type >= 0 && type < TYPE_COUNT && map.get(type) != null) {
                return map.get(type).sections.get(packageName);
            }
            return null;
        }
    }

    private int getTypeForTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int minutes = calendar.get(Calendar.MINUTE) + calendar.get(Calendar.HOUR_OF_DAY) * 60;
        if (minutes > 300 && minutes <= 660) {
            return TYPE_MORNING;
        }
        if (minutes > 660 && minutes <= 900) {
            return TYPE_NOON;
        }
        if (minutes <= 900 || minutes > 1020) {
            return (minutes <= 1020 || minutes > 1140) ? TYPE_NIGHT : TYPE_SUNSET;
        }
        return TYPE_AFTERNOON;
    }

    public static SectionHabit restoreFromFile(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        SectionHabit habit = new SectionHabit();
        HashMap<Integer, SectionInfo> targetData = habit.mCurrentData;
        HashMap<String, HabitDate> sections = null;
        HabitDate pending = null;
        String pendingPkg = "";
        while (true) {
            int event = parser.next();
            if (event == XmlPullParser.END_DOCUMENT) {
                return null;
            }
            String name = parser.getName();
            synchronized (mDataLock) {
                if (event == XmlPullParser.START_TAG) {
                    if (SECTION_HABIT_TAG_CURRENT.equals(name)) {
                        targetData = habit.mCurrentData;
                    } else if (SECTION_HABIT_TAG_HISTORY.equals(name)) {
                        targetData = habit.mHistoryData;
                    } else if (CONF_TAG_APP_DATA.equals(name)) {
                        int type = Integer.valueOf(getAttrValue(parser, ATTR_AR_TYPE));
                        sections = targetData.get(type).sections;
                    } else if (CONF_TAG_PKG.equals(name)) {
                        pendingPkg = getAttrValue(parser, ATTR_NAME);
                        pending = HabitDate.createFromXml(pendingPkg, parser);
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    if (CONF_TAG_PKG.equals(name)) {
                        if (sections != null && pending != null) {
                            sections.put(pendingPkg, pending);
                        }
                    } else if (CONF_TAG_SECTIONHABIT.equals(name)) {
                        return habit;
                    }
                }
            }
        }
    }

    private void updateFgTime(String packageName, long startTime) {
        int type = getTypeForTime(startTime);
        int typeNow = getTypeForTime(System.currentTimeMillis());
        Calendar calendar = Calendar.getInstance();
        long nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        calendar.setTimeInMillis(startTime);
        long startMinutes = calendar.get(Calendar.MINUTE) + calendar.get(Calendar.HOUR_OF_DAY) * 60;
        synchronized (mDataLock) {
            if (typeNow >= 0 && typeNow < TYPE_COUNT && type >= 0 && type < TYPE_COUNT) {
                if (type != typeNow) {
                    for (int i = type; i <= typeNow; i++) {
                        if (i == type) {
                            updateFgTimeInner(packageName, typeNow, 1000L * 60 * startMinutes,
                                    this.mCurrentData.get(typeNow).endTime);
                        } else if (i == typeNow) {
                            updateFgTimeInner(packageName, typeNow,
                                    this.mCurrentData.get(typeNow).startTime, 1000L * 60 * nowMinutes);
                        } else {
                            updateFgTimeInner(packageName, typeNow,
                                    this.mCurrentData.get(typeNow).startTime,
                                    this.mCurrentData.get(typeNow).endTime);
                        }
                    }
                } else {
                    updateFgTimeInner(packageName, typeNow, startTime,
                            System.currentTimeMillis());
                }
            }
        }
    }

    private void updateFgTimeIndexLmd(int type) {
        ArrayList<HabitDate> dates = new ArrayList<>();
        HashMap<String, HabitDate> sections = this.mCurrentData.get(type).sections;
        if (sections != null) {
            for (Map.Entry<String, HabitDate> entry : sections.entrySet()) {
                dates.add(entry.getValue());
            }
            Collections.sort(dates, new Comparator<HabitDate>() {
                @Override
                public int compare(HabitDate a, HabitDate b) {
                    return a.fgtime >= b.fgtime ? -1 : 1;
                }
            });
            for (HabitDate date : dates) {
                date.fgtimeIndex = dates.indexOf(date);
            }
        }
    }

    private void updateFgTimeInner(String packageName, int type, long start, long end) {
        HabitDate date = getHabitDate(this.mCurrentData, type, packageName);
        if (date == null) {
            date = new HabitDate();
            this.mCurrentData.get(type).sections.put(packageName, date);
        }
        date.packageName = packageName;
        date.fgtime = end - start;
        date.lastUsedTime = end;
        updateFgTimeIndexLmd(type);
    }

    private void updateFoucusChangesCounts(String packageName) {
        int type = getTypeForTime(System.currentTimeMillis());
        synchronized (mDataLock) {
            if (type >= 0 && type < TYPE_COUNT) {
                HabitDate date = getHabitDate(this.mCurrentData, type, packageName);
                if (date == null) {
                    date = new HabitDate();
                    this.mCurrentData.get(type).sections.put(packageName, date);
                }
                date.packageName = packageName;
                date.launchCount++;
                updateLaunchCountIndexLmd(type);
            }
        }
    }

    private void updateLaunchCountIndexLmd(int type) {
        ArrayList<HabitDate> dates = new ArrayList<>();
        HashMap<String, HabitDate> sections = this.mCurrentData.get(type).sections;
        if (sections != null) {
            for (Map.Entry<String, HabitDate> entry : sections.entrySet()) {
                dates.add(entry.getValue());
            }
            Collections.sort(dates, new Comparator<HabitDate>() {
                @Override
                public int compare(HabitDate a, HabitDate b) {
                    return a.launchCount >= b.launchCount ? -1 : 1;
                }
            });
            for (HabitDate date : dates) {
                date.launchCountIndex = dates.indexOf(date);
            }
        }
    }

    private void writeToFileLocked(XmlSerializer serializer,
            HashMap<Integer, SectionInfo> map, boolean current)
            throws XmlPullParserException, IOException {
        if (current) {
            serializer.startTag(null, SECTION_HABIT_TAG_CURRENT);
        } else {
            serializer.startTag(null, SECTION_HABIT_TAG_HISTORY);
        }
        for (Map.Entry<Integer, SectionInfo> entry : map.entrySet()) {
            HashMap<String, HabitDate> sections = entry.getValue().sections;
            serializer.startTag(null, CONF_TAG_APP_DATA);
            serializer.attribute(null, ATTR_AR_TYPE, String.valueOf(entry.getKey()));
            for (Map.Entry<String, HabitDate> section : sections.entrySet()) {
                serializer.startTag(null, CONF_TAG_PKG);
                serializer.attribute(null, ATTR_NAME, section.getKey());
                HabitDate date = section.getValue();
                if (date != null) {
                    date.writeToFile(serializer);
                }
                serializer.endTag(null, CONF_TAG_PKG);
            }
            serializer.endTag(null, CONF_TAG_APP_DATA);
        }
        if (current) {
            serializer.endTag(null, SECTION_HABIT_TAG_CURRENT);
        } else {
            serializer.endTag(null, SECTION_HABIT_TAG_HISTORY);
        }
    }

    public void dump(FileDescriptor fd, PrintWriter writer) {
        synchronized (mDataLock) {
            HashMap<Integer, SectionInfo> current =
                    (HashMap<Integer, SectionInfo>) this.mCurrentData.clone();
            writer.println("dump  New SectionHabit:");
            dumpDateLocked(writer, current);
            HashMap<Integer, SectionInfo> history =
                    (HashMap<Integer, SectionInfo>) this.mHistoryData.clone();
            writer.println("dump  Old SectionHabit:");
            dumpDateLocked(writer, history);
        }
    }

    public void dumpSectionHabits() {
        synchronized (mDataLock) {
            HashMap<Integer, SectionInfo> current =
                    (HashMap<Integer, SectionInfo>) this.mCurrentData.clone();
            Log.d(TAG, "dumpNewSectionHabits-> mCurrentData");
            dumpSectionHabitsDateLocked(current);
            HashMap<Integer, SectionInfo> history =
                    (HashMap<Integer, SectionInfo>) this.mHistoryData.clone();
            Log.d(TAG, "dumpOldSectionHabits-> mHistoryData");
            dumpSectionHabitsDateLocked(history);
        }
    }

    public HabitDate getHistoryHabitDate(String packageName) {
        int type = getTypeForTime(System.currentTimeMillis());
        synchronized (mDataLock) {
            HabitDate date = getHabitDate(this.mHistoryData, type, packageName);
            if (date == null) {
                date = getHabitDate(this.mCurrentData, type, packageName);
            }
            return date;
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("");
        synchronized (mDataLock) {
            for (Map.Entry<Integer, SectionInfo> entry : this.mCurrentData.entrySet()) {
                builder.append("key:").append(entry.getKey()).append(",data:")
                        .append(entry.getValue());
            }
            for (Map.Entry<Integer, SectionInfo> entry : this.mHistoryData.entrySet()) {
                builder.append("key:").append(entry.getKey()).append(",data:")
                        .append(entry.getValue());
            }
        }
        return "data:" + builder.toString();
    }

    public void updateSectionInfo(String packageName, long time, boolean isLaunch) {
        synchronized (mDataLock) {
            long now = System.currentTimeMillis();
            if (now > this.mStartTime + DATA_DURATION) {
                this.mHistoryData.clear();
                this.mHistoryData = CloneUtils.clone(this.mCurrentData);
                this.mCurrentData.clear();
                for (int i = 0; i < TYPE_COUNT; i++) {
                    this.mCurrentData.put(i, new SectionInfo(i));
                }
                this.mStartTime = now;
            }
        }
        if (isLaunch) {
            updateFoucusChangesCounts(packageName);
        } else {
            updateFgTime(packageName, time);
        }
    }

    public void writeToFile(XmlSerializer serializer)
            throws XmlPullParserException, IOException {
        serializer.startTag(null, CONF_TAG_SECTIONHABIT);
        synchronized (mDataLock) {
            HashMap<Integer, SectionInfo> current =
                    (HashMap<Integer, SectionInfo>) this.mCurrentData.clone();
            HashMap<Integer, SectionInfo> history =
                    (HashMap<Integer, SectionInfo>) this.mHistoryData.clone();
            writeToFileLocked(serializer, current, true);
            writeToFileLocked(serializer, history, false);
        }
        serializer.endTag(null, CONF_TAG_SECTIONHABIT);
    }
}