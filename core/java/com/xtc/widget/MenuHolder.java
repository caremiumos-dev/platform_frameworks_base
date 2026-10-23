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

package com.xtc.widget;

import android.app.ActivityManagerNative;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.provider.Telephony;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.ImsConfig;
import com.xtc.moduleswitch.FunSwitchUtil;
import com.xtc.moduleswitch.ModuleSwitch;
import com.xtc.utils.HallUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * Holds the flip cover menu entries loaded from the XTC setting package
 * asset, and applies the module / function / language visibility filters.
 *
 * @hide
 */
public class MenuHolder {
    private static final String TAG = "SysMenuHolder";
    private static final String DEFAULT_PACKAGE = "android";
    private static final String FILE_PREFIX = "flip_config/";
    private static final String FILE_SUFFIX = "_flip_config.json";
    private static final String NONE = "none";
    private static final int NO_MODULE_ID = 0;

    public static final String NULL = "null";
    public static final String RES_PACKAGE = "com.xtc.setting";

    private static volatile MenuHolder sMenuHolder;

    private List<MenuItem> mAllItem = new Vector<>(11);
    private List<MenuItem> mDataList = new Vector<>(11);
    private int mAnimShowCount = 50;
    private boolean mLoadIcon = true;

    private MenuHolder() {
    }

    public static MenuHolder instance() {
        if (sMenuHolder == null) {
            synchronized (MenuHolder.class) {
                if (sMenuHolder == null) {
                    sMenuHolder = new MenuHolder();
                }
            }
        }
        return sMenuHolder;
    }

    public synchronized void init(Context context, boolean loadIcon) {
        this.mLoadIcon = loadIcon;
        if (this.mAllItem.size() > 0) {
            return;
        }
        initDataFromJson(context, RES_PACKAGE, true);
    }

    public synchronized void reload(Context context) {
        this.mAllItem.clear();
        this.mDataList.clear();
        initDataFromJson(context, RES_PACKAGE, true);
    }

    private void initDataFromJson(Context context, String packageName, boolean fallbackToAosp) {
        try {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            String fileName = FILE_PREFIX + HallUtil.INNER_MODEL + FILE_SUFFIX;
            Resources resources = context.createPackageContext(packageName,
                    Context.CONTEXT_INCLUDE_CODE).getResources();
            InputStream inputStream = resources.getAssets().open(fileName);
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    reader.close();
                    initItemByJson(resources, new JSONObject(sb.toString()), packageName);
                    Log.d(TAG, "load " + fileName + " from " + packageName + " cost:"
                            + (SystemClock.elapsedRealtime() - elapsedRealtime));
                    return;
                }
                sb.append(line);
            }
        } catch (Exception e) {
            if (fallbackToAosp) {
                initDataFromJson(context, DEFAULT_PACKAGE, false);
            } else {
                Log.w(TAG, "load json err:" + packageName, e);
            }
        }
    }

    private void initItemByJson(Resources resources, JSONObject object, String packageName)
            throws JSONException {
        this.mAnimShowCount = object.getInt("showCount");
        JSONArray array = object.getJSONArray(ImsConfig.EXTRA_CHANGED_ITEM);
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            MenuItem menuItem = new MenuItem(this.mLoadIcon ? resources : null,
                    item.getString("key"),
                    getIconId(resources, item.getString("icon"), packageName),
                    getTitle(resources, item.getString("title"), packageName),
                    item.getString(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME),
                    item.getString("class"));
            menuItem.order = item.getInt("order");
            menuItem.realPkg = item.getString("realPkg");
            menuItem.showVersion = item.getInt("showVersion");
            menuItem.action = item.getString(android.hardware.Camera.Parameters.SCENE_MODE_ACTION);
            menuItem.uri = item.getString("uri");
            menuItem.moduleId = item.getInt("moduleSwitch");
            menuItem.moduleDefault = item.getBoolean("moduleDefault");
            menuItem.funPkg = item.getString("funSwitch");
            menuItem.funDefault = item.getBoolean("funDefault");
            menuItem.hideLanguage = item.getString("hideLanguage");
            menuItem.showMenu = item.getBoolean("showMenu");
            menuItem.initMenu = item.getBoolean("initMenu");
            menuItem.enableAnim = item.getBoolean("enableAnim");
            this.mAllItem.add(menuItem);
        }
    }

    private int getIconId(Resources resources, String name, String packageName) {
        return resources.getIdentifier(name, "drawable", packageName);
    }

    private String getTitle(Resources resources, String name, String packageName) {
        return resources.getString(resources.getIdentifier(name, "string", packageName));
    }

    private boolean moduleEnabled(Context context, MenuItem item) {
        return item.moduleId == NO_MODULE_ID ? item.moduleDefault
                : ModuleSwitch.queryModuleSwitch(context, item.moduleId, item.moduleDefault);
    }

    private boolean functionEnabled(Context context, MenuItem item) {
        return NULL.equals(item.funPkg) ? item.funDefault
                : FunSwitchUtil.queryFunSwitchByBoolean(context, item.funPkg, item.funDefault);
    }

    private boolean showInLanguage(MenuItem item) {
        return !item.hideLanguage.contains(SystemProperties.get("ro.product.locale"));
    }

    private boolean hideItem(MenuItem item, List<PackageInfo> installedPackages) {
        for (PackageInfo packageInfo : installedPackages) {
            if (packageInfo.packageName.equals(item.realPkg)
                    && packageInfo.versionCode > item.showVersion) {
                return false;
            }
        }
        return true;
    }

    public synchronized List<MenuItem> queryListByModuleSwitch(Context context) {
        List<PackageInfo> installedPackages = context.getPackageManager().getInstalledPackages(0);
        this.mDataList.clear();
        for (MenuItem item : this.mAllItem) {
            if (moduleEnabled(context, item) && showInLanguage(item)) {
                if (hideItem(item, installedPackages)) {
                    Log.d(TAG, "hide item key:" + item.key + " version:" + item.showVersion);
                } else {
                    this.mDataList.add(item);
                }
                item.moduleEnable = true;
            } else {
                item.moduleEnable = false;
                Log.d(TAG, "key:" + item.key + " hideLanguage:" + item.hideLanguage);
            }
        }
        checkUserOrder(context);
        sortListByOrder(this.mDataList);
        return this.mDataList;
    }

    public synchronized void updateFunSwitchValue(Context context) {
        for (MenuItem item : this.mAllItem) {
            item.funEnable = functionEnabled(context, item);
            item.period = FunSwitchUtil.queryDisabledPeriod(context, item.funPkg);
            if (!item.funEnable || item.periodDisabled()) {
                Log.d(TAG, "key:" + item.key + " function:" + item.funEnable
                        + " period:" + item.periodDisabled());
            }
        }
    }

    private void checkUserOrder(Context context) {
        String order = Settings.System.getString(context.getContentResolver(),
                HallUtil.MENU_ORDER);
        Log.d(TAG, "last order:" + order);
        if (TextUtils.isEmpty(order)) {
            return;
        }
        try {
            JSONArray array = new JSONArray(order);
            int min = Math.min(array.length(), this.mAllItem.size());
            for (int i = 0; i < min; i++) {
                this.mAllItem.get(i).order = array.getInt(i);
            }
            if (this.mAllItem.size() > array.length()) {
                sortNewItem(array.length());
            }
            ensureCorrectOrder();
        } catch (JSONException e) {
            Log.w(TAG, " parse order err", e);
        }
    }

    private void sortNewItem(int start) {
        while (start < this.mAllItem.size()) {
            MenuItem item = this.mAllItem.get(start);
            for (MenuItem other : this.mAllItem) {
                if (other.order >= item.order && other != item) {
                    other.order++;
                }
            }
            start++;
        }
    }

    private void ensureCorrectOrder() {
        Vector<MenuItem> vector = new Vector<>(this.mAllItem);
        sortListByOrder(vector);
        int i = 0;
        while (true) {
            int index = i;
            if (index >= vector.size()) {
                saveMenuOrder();
                vector.clear();
                return;
            } else {
                vector.get(index).order = index;
                i = index + 1;
            }
        }
    }

    private void sortListByOrder(List<MenuItem> list) {
        Collections.sort(list, new Comparator<MenuItem>() {
            @Override
            public int compare(MenuItem menuItem, MenuItem menuItem2) {
                return menuItem.order - menuItem2.order;
            }
        });
    }

    public MenuItem getItemByKey(String key) {
        for (MenuItem item : this.mAllItem) {
            if (item.key.equals(key)) {
                return item;
            }
        }
        Log.d(TAG, "not found key:" + key + "in list ");
        return new MenuItem(null, NONE, 0, NULL, NULL, NULL);
    }

    public MenuItem getItemByIndex(int index) {
        return (index < 0 || index >= this.mAllItem.size()) ? getItemByKey(NONE)
                : this.mAllItem.get(index);
    }

    public int getItemIndex(String key) {
        int i = 0;
        while (true) {
            int index = i;
            if (index >= this.mAllItem.size()) {
                if (HallUtil.KEY_PHOTO.equals(key) || NONE.equals(key)) {
                    return -1;
                }
                return getItemIndex(HallUtil.KEY_PHOTO);
            }
            if (this.mAllItem.get(index).key.equals(key)) {
                return index;
            }
            i = index + 1;
        }
    }

    public boolean itemDisable(int index) {
        MenuItem item = getItemByIndex(index);
        if (NONE.equals(item.key)) {
            return false;
        }
        boolean disabled = (!item.moduleEnable || !item.funEnable || item.periodDisabled())
                || !showInLanguage(item);
        if (disabled) {
            Log.d(TAG, "title:" + item.menuTitle + " fun:" + item.funEnable
                    + " module:" + item.moduleEnable + " language:" + item.hideLanguage);
        }
        return disabled;
    }

    public int size() {
        return this.mAllItem.size();
    }

    public void swapOrder(MenuItem menuItem, MenuItem menuItem2) {
        int temp = menuItem.order;
        menuItem.order = menuItem2.order;
        menuItem2.order = temp;
    }

    public int animShowCount() {
        return this.mAnimShowCount;
    }

    public void saveMenuOrder() {
        JSONArray array = new JSONArray();
        Iterator<MenuItem> iterator = this.mAllItem.iterator();
        while (iterator.hasNext()) {
            array.put(iterator.next().order);
        }
        try {
            ActivityManagerNative.getDefault().requestHallEvent(HallUtil.MENU_ORDER,
                    array.toString());
        } catch (RemoteException e) {
            Log.e(TAG, "save menu order err", e);
        }
    }

    public synchronized boolean contains(String packageName) {
        Iterator<MenuItem> it = this.mAllItem.iterator();
        while (it.hasNext()) {
            if (it.next().pkgName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    public void dumpItemByOrder() {
        Iterator<MenuItem> it = this.mDataList.iterator();
        while (it.hasNext()) {
            Log.d(TAG, "order : " + it.next().key);
        }
    }
}