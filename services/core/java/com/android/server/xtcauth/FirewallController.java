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
import android.util.Log;

import java.util.HashMap;

/**
 * Placeholder firewall controller. The vendor intended to map packages to uids
 * for per-application network control; the actual enforcement is delegated to
 * the network management service.
 *
 * @hide
 */
class FirewallController extends ControllerBase implements IPackageBasedController {
    public static final String NAME = "FirewallController";
    private static final int MAX_APP_NUM = 255;

    private static final HashMap mApplicationPool = new HashMap(MAX_APP_NUM);
    private static ControllerBase mInstance = null;

    private Context mContext;

    FirewallController() {
    }

    public static final ControllerBase getInstance() {
        if (mInstance == null) {
            mInstance = new FirewallController();
            Log.i(NAME, "Create MobileManagerController:FirewallController");
        }
        if (mInstance == null) {
            throw new RuntimeException("null FirewallController");
        }
        return mInstance;
    }

    public void setFirewallPolicy(int uid, int policy, boolean enabled) {
        Log.d(NAME, "setFirewallPolicy:" + uid + ":" + policy + ":" + enabled);
        String[] packages = mContext.getPackageManager().getPackagesForUid(uid);
        if (packages == null) {
            Log.e(NAME, "null packages");
            return;
        }
        for (String packageName : packages) {
            mApplicationPool.put(packageName, Integer.valueOf(uid));
        }
    }

    @Override
    public boolean clearAllSettings() {
        mApplicationPool.clear();
        return true;
    }

    @Override
    public boolean clearPackageSettings(String packageName) {
        return false;
    }

    @Override
    public void onPackageStatusChange(int uid, String packageName, int status) {
        Integer.valueOf(0);
        Log.d(NAME, "onPackageStatusChange(" + packageName + ", " + status + ")");
        if (status == 1) {
            android.os.INetworkManagementService.Stub.asInterface(
                    android.os.ServiceManager.getService("network_management"));
            try {
                Integer removedUid = (Integer) mApplicationPool.get(packageName);
                if (removedUid != null) {
                    Log.d(NAME, "remove uid " + removedUid);
                    mApplicationPool.remove(packageName);
                } else {
                    Log.d(NAME, "No found");
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onUserStatusChange(int userId, int status) {
    }

    @Override
    public void systemReady(Context context) {
        mContext = context;
    }
}