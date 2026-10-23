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

package com.android.internal.os;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Regionalization (carrier package) environment: discovers the carrier-specific
 * package directories configured under {@code /persist/speccfg/spec} and exposes
 * their names, directories and excluded applications.
 *
 * @hide
 */
public class RegionalizationEnvironment {
    private static final String TAG = "RegionalizationEnvironment";
    private static final boolean DEBUG = true;

    private static final String SPEC_FILE_PATH = "/persist/speccfg/spec";
    private static final String PACK_STORAGE_PREFIX = "packStorage=";
    private static final String PACK_COUNT_PREFIX = "packCount=";

    public static final String ISREGIONAL_FRAMEWORK = "Framework";
    public static final String ISREGIONAL_APP = "app";

    private static final boolean SUPPORTED =
            android.os.SystemProperties.getBoolean("ro.regionalization.support", false);

    private static IRegionalizationService mRegionalizationService = null;
    private static final ArrayList<Package> mPackages = new ArrayList<>();
    private static final ArrayList<String> mExcludedApps = new ArrayList<>();
    private static boolean isLoaded = false;

    private static class Package {
        private final String mName;
        private final int mPriority;
        private final String mStorage;

        Package(String name, int priority, String storage) {
            mName = name;
            mPriority = priority;
            mStorage = storage;
        }

        public String getName() {
            return mName;
        }

        public int getPriority() {
            return mPriority;
        }

        public String getStoragePos() {
            return mStorage;
        }

        public File getDirectory() {
            return new File(mStorage, mName);
        }

        public String getExcludedListFilePath() {
            return getDirectory().getAbsolutePath() + "/exclude.list";
        }
    }

    public static boolean isSupported() {
        if (SUPPORTED && !isLoaded) {
            init();
        }
        return SUPPORTED;
    }

    private static void init() {
        mRegionalizationService = IRegionalizationService.Stub.asInterface(
                ServiceManager.getService("regionalization"));
        if (mRegionalizationService != null) {
            loadSwitchedPackages();
            loadExcludedApplist();
            isLoaded = true;
        }
    }

    public static IRegionalizationService getRegionalizationService() {
        return mRegionalizationService;
    }

    public static List<File> getAllPackageDirectories() {
        ArrayList<File> directories = new ArrayList<>();
        for (Package pkg : mPackages) {
            Log.v(TAG, "Package Directoriy(" + pkg.getPriority() + "):" + pkg.getDirectory());
            directories.add(pkg.getDirectory());
        }
        return directories;
    }

    public static List<String> getAllPackageNames() {
        ArrayList<String> names = new ArrayList<>();
        for (Package pkg : mPackages) {
            names.add(pkg.getName());
        }
        return names;
    }

    public static int getPackagesCount() {
        return mPackages.size();
    }

    public static String getStoragePos() {
        for (Package pkg : mPackages) {
            String storage = pkg.getStoragePos();
            if (!TextUtils.isEmpty(storage)) {
                return storage;
            }
        }
        try {
            mPackages.clear();
            throw new IOException("Read wrong package for Carrier!");
        } catch (IOException e) {
            Log.e(TAG, "Get storage pos error, caused by: " + e.getMessage());
            return "";
        }
    }

    public static boolean isExcludedApp(String app) {
        if (getPackagesCount() == 0) {
            return false;
        }
        return !app.endsWith(".apk") ? mExcludedApps.contains(app + ".apk")
                : mExcludedApps.contains(app);
    }

    public static boolean isRegionalizationCarrierOverlayPackage(String packageName,
            String type) {
        if (!isSupported() || packageName == null) {
            return false;
        }
        boolean matched = false;
        for (String name : getAllPackageNames()) {
            boolean current = packageName.indexOf(name) != -1 || matched;
            if (current && ((type.equals(ISREGIONAL_FRAMEWORK)
                    && packageName.indexOf(ISREGIONAL_FRAMEWORK) != -1)
                    || type.equals(ISREGIONAL_APP))) {
                return true;
            }
            matched = current;
        }
        return false;
    }

    private static void loadExcludedApplist() {
        Log.d(TAG, "loadExcludedApps!");
        if (getPackagesCount() == 0) {
            return;
        }
        for (Package pkg : mPackages) {
            Log.d(TAG, "load excluded apps for " + pkg.getDirectory());
            ArrayList<String> excluded;
            try {
                excluded = (ArrayList<String>) mRegionalizationService.readFile(
                        pkg.getExcludedListFilePath(), null);
            } catch (RemoteException e) {
                e.printStackTrace();
                excluded = null;
            }
            if (excluded != null && excluded.size() > 0) {
                for (String entry : excluded) {
                    if (TextUtils.isEmpty(entry)) {
                        continue;
                    }
                    int lastSlash = entry.lastIndexOf("/");
                    if (lastSlash != -1) {
                        String name = entry.substring(lastSlash + 1);
                        if (!TextUtils.isEmpty(name) && !mExcludedApps.contains(name)) {
                            mExcludedApps.add(name);
                        }
                    }
                }
            }
        }
    }

    private static void loadSwitchedPackages() {
        Log.d(TAG, "load packages for Carrier!");
        ArrayList<String> lines;
        try {
            lines = (ArrayList<String>) mRegionalizationService.readFile(SPEC_FILE_PATH, null);
        } catch (RemoteException e) {
            e.printStackTrace();
            lines = null;
        }
        try {
            if (lines == null || lines.size() <= 0) {
                return;
            }
            if (!lines.get(0).startsWith(PACK_STORAGE_PREFIX)) {
                throw new IOException("Can't read storage pos for Carrier package!");
            }
            String storage = lines.get(0).substring(PACK_STORAGE_PREFIX.length());
            if (TextUtils.isEmpty(storage)) {
                throw new IOException("Storage pos for Carrier package is wrong!");
            }
            if (!lines.get(1).matches("^packCount=[0-9]$")) {
                throw new IOException("Can't read package count of Carrier!");
            }
            int count = Integer.parseInt(lines.get(1).substring(PACK_COUNT_PREFIX.length()));
            if (count <= 0 || lines.size() <= count) {
                throw new IOException("Package count of Carrier is wrong!");
            }
            for (int i = 2; i < count + 2; i++) {
                if (!lines.get(i).matches("^strSpec[0-9]=\\w+$")) {
                    mPackages.clear();
                    throw new IOException("Read wrong packages for Carrier!");
                }
                String name = lines.get(i).substring("strSpec".length() + 2);
                if (TextUtils.isEmpty(name)) {
                    continue;
                }
                boolean exists;
                try {
                    exists = mRegionalizationService.checkFileExists(storage + "/" + name);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    exists = false;
                }
                if (!exists) {
                    mPackages.clear();
                    throw new IOException("Read wrong packages for Carrier!");
                }
                mPackages.add(new Package(name, i, storage));
            }
        } catch (IOException e) {
            Log.e(TAG, "Load package for carrier error, caused by: " + e.getMessage());
        }
    }
}