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

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import com.xtc.moduleswitch.DisabledPeriod;
import com.xtc.moduleswitch.FunSwitchUtil;

/**
 * Describes a single entry of the XTC flip cover menu.
 *
 * @hide
 */
public class MenuItem {
    public final String key;
    public final String menuTitle;
    public final String pkgName;
    private final Resources resources;
    private final int iconId;
    String clsName;
    java.lang.String action;
    String uri;
    java.lang.String realPkg;
    int order;
    int showVersion;
    boolean moduleDefault;
    int moduleId;
    boolean funDefault;
    String funPkg;
    boolean funEnable = false;
    boolean moduleEnable = false;
    DisabledPeriod period;
    String hideLanguage = "";
    boolean showMenu;
    public boolean initMenu;
    public boolean enableAnim;
    boolean selected;

    public MenuItem(Resources resources, String key, int iconId, String menuTitle,
            String pkgName, String clsName) {
        this.resources = resources;
        this.key = key;
        this.iconId = iconId;
        this.menuTitle = menuTitle;
        this.pkgName = pkgName;
        this.clsName = clsName;
    }

    private boolean hasValue(String value) {
        return !MenuHolder.NULL.equals(value);
    }

    public Drawable getIcon() {
        return resources.getDrawable(iconId).mutate();
    }

    public Intent getIntent() {
        Intent intent = new Intent();
        if (hasValue(action)) {
            intent.setAction(action);
        }
        if (hasValue(uri)) {
            intent.setData(Uri.parse(uri));
        }
        if (hasValue(pkgName)) {
            intent.setPackage(pkgName);
            if (hasValue(clsName)) {
                intent.setClassName(pkgName, clsName);
            }
        }
        return intent;
    }

    public boolean periodDisabled() {
        return FunSwitchUtil.queryInDisabledPeriod(period, false);
    }
}