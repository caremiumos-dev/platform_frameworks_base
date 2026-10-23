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

package com.android.server.xtcauth;

import android.content.Context;
import android.content.Intent;
import android.content.pm.IPackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IUserManager;
import android.os.RemoteException;
import android.util.AtomicFile;
import android.util.Log;
import android.util.Xml;

import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.xtc.auth.BootReceiverPolicy;
import com.xtc.auth.ReceiverRecord;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists, per manager identity, which packages are allowed to receive the
 * boot-completed broadcasts. This is part of the vendor "mobile manager"
 * self-start control: a management application can stop third-party
 * applications from starting on boot.
 *
 * @hide
 */
class ReceiverRecordHelper {
    private static final String TAG = "ReceiverRecordHelper";
    private static final String FILE_NAME = "bootreceiver";
    private static final String FILE_EXTENSION = ".xml";

    public static final boolean DEFAULT_STATUS = true;
    public static boolean SUPPORT_SYSTEM_APP = false;

    private final ReceiverRecorder mRecorder;
    private final AtomicFile mFile;
    private final Context mContext;
    private final Handler mHandler;
    private final IUserManager mUm;
    private final IPackageManager mPm;

    private final Map<Integer, Map<String, ReceiverRecord>> mBootReceiverList = new HashMap<>();
    private final Map<Integer, List<String>> mPendingSettings = new HashMap<>();
    private boolean mReady = false;
    private boolean mIsDeleted = false;

    public ReceiverRecordHelper(ReceiverRecorder recorder, Context context, Handler handler,
            IUserManager um, IPackageManager pm) {
        mRecorder = recorder;
        mFile = new AtomicFile(new File(new File(Environment.getDataDirectory(), "system"),
                FILE_NAME + recorder.genPostfix() + FILE_EXTENSION));
        mContext = context;
        mHandler = handler;
        mUm = um;
        mPm = pm;
        Log.d(TAG, "ReceiverRecordHelper(" + mRecorder.getIdentity() + ")");
    }

    private void enforceBasicBootPolicy() {
        List<String> bootPolicy = BootReceiverPolicy.getBootPolicy();
        boolean ok = bootPolicy.contains(Intent.ACTION_BOOT_COMPLETED)
                && bootPolicy.contains("android.intent.action.ACTION_BOOT_IPO");
        if (!ok) {
            throw new RuntimeException("Should NOT remove basic boot policy!");
        }
    }

    private Map<String, ReceiverRecord> getBootReceiverListByUser(int userId) {
        synchronized (mBootReceiverList) {
            if (!mBootReceiverList.containsKey(userId)) {
                return null;
            }
            return mBootReceiverList.get(userId);
        }
    }

    private List<String> getBootReceivers(int userId) {
        ArrayList<String> result = new ArrayList<>();
        List<String> bootPolicy = BootReceiverPolicy.getBootPolicy();
        for (int i = 0; i < bootPolicy.size(); i++) {
            Intent intent = new Intent(bootPolicy.get(i));
            try {
                List<ResolveInfo> receivers = null;
                android.content.pm.ParceledListSlice<ResolveInfo> slice =
                        mPm.queryIntentReceivers(intent,
                                intent.resolveTypeIfNeeded(mContext.getContentResolver()), 128,
                                userId);
                if (slice != null) {
                    receivers = slice.getList();
                }
                if (receivers != null) {
                    for (int j = 0; j < receivers.size(); j++) {
                        ResolveInfo info = receivers.get(j);
                        String packageName = info.activityInfo != null
                                ? info.activityInfo.packageName : null;
                        if ((SUPPORT_SYSTEM_APP
                                || !PackageController.isSystemApp(userId, packageName))
                                && packageName != null
                                && !result.contains(packageName)) {
                            result.add(packageName);
                        }
                    }
                }
            } catch (RemoteException e) {
            }
        }
        return result;
    }

    private List<ReceiverRecord> getReceiverList(int userId) {
        ArrayList<ReceiverRecord> result = new ArrayList<>();
        synchronized (mBootReceiverList) {
            Map<String, ReceiverRecord> records = getBootReceiverListByUser(userId);
            if (records != null) {
                for (String key : records.keySet()) {
                    result.add(new ReceiverRecord(records.get(key)));
                }
            }
        }
        return result;
    }

    private List<UserInfo> getUserList() {
        try {
            return mUm.getUsers(false);
        } catch (RemoteException e) {
            Log.e(TAG, "getUsers() failed!", e);
            return null;
        }
    }

    private void initReceiverCache(int userId, boolean readFile) {
        Log.d(TAG, "initReceiverCache(" + mRecorder.getIdentity() + ") at User(" + userId + ")");
        List<String> bootReceivers = getBootReceivers(userId);
        HashMap<String, ReceiverRecord> records = new HashMap<>();
        synchronized (mBootReceiverList) {
            mBootReceiverList.put(userId, records);
            for (int i = 0; i < bootReceivers.size(); i++) {
                String packageName = bootReceivers.get(i);
                records.put(packageName, new ReceiverRecord(packageName, true));
                Log.d(TAG, "initReceiverCache() packageName: " + packageName);
            }
            if (readFile) {
                readReceiverList();
            }
        }
    }

    private boolean isPendingSetting(int userId, String packageName) {
        synchronized (mPendingSettings) {
            List<String> list = mPendingSettings.get(userId);
            if (list == null || !list.contains(packageName)) {
                return false;
            }
            Log.d(TAG, "Found a pending setting for pkg: " + packageName + " at User(" + userId
                    + ")");
            return true;
        }
    }

    private void removePendingSetting(int userId, String packageName) {
        synchronized (mPendingSettings) {
            List<String> list = mPendingSettings.get(userId);
            if (list != null && list.contains(packageName)) {
                list.remove(packageName);
            }
        }
    }

    private void readReceiverList() {
        Log.d(TAG, "readReceiverList(" + mRecorder.getIdentity() + ")");
        synchronized (mFile) {
            synchronized (mBootReceiverList) {
                FileInputStream stream;
                try {
                    stream = mFile.openRead();
                } catch (FileNotFoundException e) {
                    Log.i(TAG, "No existing " + mFile.getBaseFile() + "; starting empty");
                    return;
                }
                boolean success = false;
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream, null);
                    int type;
                    while ((type = parser.next()) != XmlPullParser.START_TAG
                            && type != XmlPullParser.END_DOCUMENT) {
                    }
                    if (type != XmlPullParser.START_TAG) {
                        throw new IllegalStateException("no start tag found");
                    }
                    int depth = parser.getDepth();
                    while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                            && (type != XmlPullParser.END_TAG || parser.getDepth() > depth)) {
                        if (type != XmlPullParser.START_TAG) {
                            continue;
                        }
                        if (parser.getName().equals("pkg")) {
                            String packageName = parser.getAttributeValue(null, "n");
                            int userId = Integer.parseInt(parser.getAttributeValue(null, "u"));
                            boolean enabled = Boolean.parseBoolean(
                                    parser.getAttributeValue(null, "e"));
                            Log.d(TAG, "Read package name: " + packageName + " enabled: " + enabled
                                    + " at User(" + userId + ")");
                            if (!setReceiverRecord(userId, packageName, enabled, false)) {
                                Log.w(TAG, "Found a pending settings for package: " + packageName);
                                synchronized (mPendingSettings) {
                                    if (!mPendingSettings.containsKey(userId)) {
                                        mPendingSettings.put(userId, new ArrayList<String>());
                                    }
                                    List<String> list = mPendingSettings.get(userId);
                                    if (!list.contains(packageName)) {
                                        list.add(packageName);
                                    }
                                }
                            }
                        } else {
                            Log.w(TAG, "Unknown element under <boot-receiver>: "
                                    + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    success = true;
                } catch (IllegalStateException | IndexOutOfBoundsException | NullPointerException
                        | NumberFormatException | XmlPullParserException | IOException e) {
                    Log.w(TAG, "Failed parsing " + e);
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
                if (!success) {
                    mBootReceiverList.clear();
                }
            }
        }
    }

    private void scheduleWriteReceiverList() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                writeReceiverList();
            }
        });
    }

    private boolean setReceiverRecord(int userId, String packageName, boolean enabled,
            boolean write) {
        boolean found = false;
        synchronized (mBootReceiverList) {
            Map<String, ReceiverRecord> records = getBootReceiverListByUser(userId);
            if (records != null && records.containsKey(packageName)) {
                records.get(packageName).enabled = enabled;
                found = true;
            }
        }
        if (found && write) {
            scheduleWriteReceiverList();
        }
        return found;
    }

    private void writeReceiverList() {
        Log.d(TAG, "writeReceiverList(" + mRecorder.getIdentity() + ")");
        synchronized (mFile) {
            if (mIsDeleted) {
                Log.d(TAG, "No need to write with identity: " + mRecorder.getIdentity());
                return;
            }
            FileOutputStream stream = null;
            try {
                stream = mFile.startWrite();
                long token = Binder.clearCallingIdentity();
                List<UserInfo> users = getUserList();
                try {
                    FastXmlSerializer serializer = new FastXmlSerializer();
                    serializer.setOutput(stream, "utf-8");
                    serializer.startDocument(null, true);
                    serializer.startTag(null, "boot-receiver");
                    for (int i = 0; i < users.size(); i++) {
                        int userId = users.get(i).id;
                        List<ReceiverRecord> receivers = getReceiverList(userId);
                        if (receivers != null) {
                            for (int j = 0; j < receivers.size(); j++) {
                                ReceiverRecord record = receivers.get(j);
                                if (!record.enabled) {
                                    serializer.startTag(null, "pkg");
                                    serializer.attribute(null, "u", String.valueOf(userId));
                                    serializer.attribute(null, "n", record.packageName);
                                    serializer.attribute(null, "e",
                                            String.valueOf(record.enabled));
                                    serializer.endTag(null, "pkg");
                                }
                            }
                        }
                    }
                    serializer.endTag(null, "boot-receiver");
                    serializer.endDocument();
                    mFile.finishWrite(stream);
                    Binder.restoreCallingIdentity(token);
                } catch (IOException e) {
                    Log.w(TAG, "Failed to write state, restoring backup.", e);
                    mFile.failWrite(stream);
                    Binder.restoreCallingIdentity(token);
                }
            } catch (IOException e) {
                Log.w(TAG, "Failed to write state: " + e);
            }
        }
    }

    public void addReceiverRecord(int userId, String packageName) {
        addReceiverRecord(userId, packageName, true);
    }

    public void addReceiverRecord(int userId, String packageName, boolean enabled) {
        Log.d(TAG, "addReceiverRecord(" + mRecorder.getIdentity() + ") with " + packageName
                + " at User(" + userId + ") enabled: " + enabled);
        synchronized (mBootReceiverList) {
            Map<String, ReceiverRecord> records = getBootReceiverListByUser(userId);
            if (records == null) {
                records = new HashMap<>();
                mBootReceiverList.put(userId, records);
            }
            records.put(packageName, new ReceiverRecord(packageName, enabled));
        }
    }

    public void addReceiverRecordWrite(int userId, String packageName) {
        addReceiverRecord(userId, packageName, true);
        scheduleWriteReceiverList();
    }

    public boolean contains(int userId, String packageName) {
        synchronized (mBootReceiverList) {
            Map<String, ReceiverRecord> records = getBootReceiverListByUser(userId);
            return records != null && records.containsKey(packageName);
        }
    }

    public List<ReceiverRecord> getBootReceiverList(int userId) {
        if (mReady) {
            return getReceiverList(userId);
        }
        Log.e(TAG, "getBootReceiverList() not ready!");
        return null;
    }

    public boolean getReceiverDataEnabled(int userId, String packageName) {
        if (!mReady) {
            Log.e(TAG, "getReceiverDataEnabled() not ready!");
            return true;
        }
        synchronized (mBootReceiverList) {
            Map<String, ReceiverRecord> records = getBootReceiverListByUser(userId);
            if (records == null || !records.containsKey(packageName)) {
                return true;
            }
            ReceiverRecord record = records.get(packageName);
            if (record == null) {
                return true;
            }
            return record.enabled;
        }
    }

    public void initReceiverCache(int userId) {
        initReceiverCache(userId, false);
    }

    public void initReceiverList() {
        enforceBasicBootPolicy();
        List<UserInfo> users = getUserList();
        synchronized (mBootReceiverList) {
            for (int i = 0; i < users.size(); i++) {
                initReceiverCache(users.get(i).id, false);
            }
            readReceiverList();
        }
        mReady = true;
    }

    public boolean matchBootPolicy(int userId, String packageName) {
        boolean match = getBootReceivers(userId).contains(packageName);
        Log.d(TAG, "matchBootPolicy(" + mRecorder.getIdentity() + ") " + match
                + " with packageName: " + packageName + " at User(" + userId + ")");
        return match;
    }

    public void removeReceiverCache(int userId) {
        Log.d(TAG, "removeReceiverCache(" + mRecorder.getIdentity() + ") at User(" + userId + ")");
        synchronized (mBootReceiverList) {
            mBootReceiverList.remove(userId);
            scheduleWriteReceiverList();
        }
    }

    public void removeReceiverRecordWrite(int userId, String packageName) {
        Log.d(TAG, "removeReceiverRecordWrite(" + mRecorder.getIdentity() + ") with " + packageName
                + " at User(" + userId + ")");
        synchronized (mBootReceiverList) {
            Map<String, ReceiverRecord> records = getBootReceiverListByUser(userId);
            if (records != null && records.containsKey(packageName)) {
                records.remove(packageName);
                scheduleWriteReceiverList();
                if (records.size() == 0) {
                    mBootReceiverList.remove(userId);
                }
            }
        }
    }

    public boolean setReceiverDataEnabled(int userId, String packageName, boolean enabled) {
        if (mReady) {
            return setReceiverRecord(userId, packageName, enabled, true);
        }
        Log.d(TAG, "setReceiverDataEnabled() not ready!");
        return false;
    }

    public void setReceiverDataEnabledBatch(int userId, List<ReceiverRecord> records) {
        if (!mReady) {
            Log.d(TAG, "setReceiverDataEnabledBatch() not ready!");
            return;
        }
        if (records == null || records.size() <= 0) {
            return;
        }
        for (int i = 0; i < records.size(); i++) {
            ReceiverRecord record = records.get(i);
            if (record != null) {
                setReceiverRecord(userId, record.packageName, record.enabled, false);
            }
        }
        scheduleWriteReceiverList();
    }

    public boolean uninitReceiverCache(int userId) {
        synchronized (mBootReceiverList) {
            if (mBootReceiverList.containsKey(userId)) {
                Log.d(TAG, "uninitReceiverCache(" + mRecorder.getIdentity() + ") at User(" + userId
                        + ")");
                mBootReceiverList.remove(userId);
            }
            if (mBootReceiverList.size() != 0) {
                return false;
            }
            Log.d(TAG, "uninitReceiverCache(" + mRecorder.getIdentity() + ") uninited");
            mFile.delete();
            mIsDeleted = true;
            return true;
        }
    }

    public void updateReceiverCache(int userId, boolean readFile) {
        Log.d(TAG, "updateReceiverCache(" + mRecorder.getIdentity() + ") at User(" + userId + ") "
                + readFile);
        synchronized (mBootReceiverList) {
            List<ReceiverRecord> receiverList = getReceiverList(userId);
            List<String> bootReceivers = getBootReceivers(userId);
            for (int i = 0; i < bootReceivers.size(); i++) {
                String packageName = bootReceivers.get(i);
                boolean found = false;
                boolean enabled = true;
                for (ReceiverRecord record : receiverList) {
                    if (record != null && packageName.equals(record.packageName)) {
                        enabled = record.enabled;
                        found = true;
                        break;
                    }
                }
                if (found) {
                    setReceiverRecord(userId, packageName, enabled, false);
                } else if (!readFile) {
                    addReceiverRecord(userId, packageName);
                } else if (isPendingSetting(userId, packageName)) {
                    addReceiverRecord(userId, packageName, false);
                    removePendingSetting(userId, packageName);
                } else {
                    addReceiverRecord(userId, packageName);
                }
            }
        }
    }
}