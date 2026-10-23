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

package com.android.server.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkPolicyManager;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

/**
 * Shows the user a hint when an application tries to use the network while its
 * mobile data has been blocked by the vendor policy manager.
 *
 * @hide
 */
public class NetworkPolicyHelper {
    private static final String TAG = "NetworkPolicyHelper";
    private static final long TOAST_DELAY_MS = 2000L;

    private final Context mContext;
    private NetworkPolicyManager mPolicyMgr;

    public NetworkPolicyHelper(Context context) {
        mContext = context;
    }

    private void initPolicyManager() {
        if (mPolicyMgr != null) {
            return;
        }
        mPolicyMgr = NetworkPolicyManager.from(mContext);
    }

    private boolean isRuleEnabled(int uid, int rule) {
        return uid > 1000 && (mPolicyMgr.getUidPolicy(uid) & rule) != 0;
    }

    private boolean isMobileTrafficDisable(int uid) {
        return isRuleEnabled(uid, NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND);
    }

    private boolean isNetworkAvailable(int type) {
        try {
            ConnectivityManager cm =
                    (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo active = cm.getActiveNetworkInfo();
            if (active == null || !active.isAvailable()) {
                Log.i(TAG, "network not available, networkInfo is null:" + (active == null));
                return false;
            }
            NetworkInfo info = cm.getNetworkInfo(type);
            if (info != null && info.isAvailable()) {
                return info.getState() == NetworkInfo.State.CONNECTED
                        || info.getState() == NetworkInfo.State.CONNECTING;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "error get network info:" + e.getMessage());
            return false;
        }
    }

    private void showToastDelay(Handler handler, final String message) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
            }
        }, TOAST_DELAY_MS);
    }

    public void showMessageIfNecessary(int uid, Handler handler) {
        initPolicyManager();
        boolean useWifi = isNetworkAvailable(ConnectivityManager.TYPE_WIFI);
        boolean mobileDisabled = isMobileTrafficDisable(uid);
        boolean useMobile = isNetworkAvailable(ConnectivityManager.TYPE_MOBILE);
        Log.i(TAG, "uid:" + uid + " isUseWIFI:" + useWifi + " isUseMobile:" + useMobile
                + " isMobileTrafficDisable:" + mobileDisabled);
        if (mobileDisabled && useMobile) {
            showToastDelay(handler, mContext.getResources().getString(
                    com.android.internal.R.string.metered_network_disabled_tip));
        }
    }
}