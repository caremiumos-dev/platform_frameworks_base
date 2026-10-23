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

package com.android.server.xgseserver.xss;

import android.content.Context;
import android.content.IIntentReceiver;
import android.content.IIntentSender;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.graphics.PixelFormat;
import android.net.NetworkPolicyManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Slog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.xtc.behavior.XtcBehavior;
import com.xtc.behavior.XtcBehaviorDataLevel;

import org.json.JSONObject;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

/**
 * Applies "security exception" actions pushed by the vendor server:
 * dialog, overlay, uninstall, disable app, deny network access to a package and
 * deny network access to an address.
 *
 * <p><b>Strong condemnation.</b> This is remote censorship and coercion of
 * third-party applications running on a device the user owns. A server the user
 * never chose is allowed to silently uninstall apps, disable apps and cut off
 * their network access without any user consent or due process. This tramples
 * user freedom and the neutrality of the platform, and must not exist on a
 * user-respecting device. It is reproduced only for behavioural compatibility.
 *
 * @hide
 */
public class SystemExceptionManager {
    public static final String EXCEPTION_TYPE_DIALOG = "dialog";
    public static final String EXCEPTION_TYPE_DISABLE_APP = "disableApp";
    public static final String EXCEPTION_TYPE_DISABLE_NET_ADDRESS = "disableNetAddress";
    public static final String EXCEPTION_TYPE_DISABLE_NET_APP = "disableNetApp";
    public static final String EXCEPTION_TYPE_OVERLAY = "overlay";
    public static final String EXCEPTION_TYPE_UNINSTALL = "uninstall";

    /** Local mirror of the performance subsystem's default scene name. */
    public static final String SCENE_DEF = "default";

    private static final String TAG = "SystemXses ExceptionManager";
    private static final String PROG_OV = "persist.sys.server.ov";
    private static final int WINDOW_TYPE_SYSTEM = 2015;
    private static final int PRIVATE_FLAG_TRUSTED = 0x10;

    private final Context mContext;
    private NetworkPolicyManager mPolicyManager;

    private static boolean sIsShowDialogUninstal = false;
    private static boolean sIsShowDialogDisableApp = false;
    private static boolean sIsShowDialogDisableNet = false;

    private static class LocalIntentReceiver {
        private final SynchronousQueue<Intent> mResult = new SynchronousQueue<>();
        private final IIntentSender.Stub mLocalSender = new IIntentSender.Stub() {
            @Override
            public void send(int code, Intent intent, String resolvedType,
                    IBinder whitelistToken, IIntentReceiver finishedReceiver,
                    String requiredPermission, Bundle options) {
                try {
                    mResult.offer(intent, 5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        public IntentSender getIntentSender() {
            return new IntentSender((IIntentSender) this.mLocalSender);
        }

        public Intent getResult() {
            try {
                return this.mResult.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public SystemExceptionManager(Context context) {
        this.mContext = context;
    }

    private int checkAppStatus(String packageName) {
        return this.mContext.getPackageManager().getApplicationEnabledSetting(packageName);
    }

    private void denyNetworkAccessApp(String packageName) {
        Slog.d(TAG, "denyNetworkAccess  start");
        if (packageName == null || packageName.length() == 0) {
            return;
        }
        if (!isPackageExists(packageName)) {
            Slog.d(TAG, "denyNetworkAccessApp packageName " + packageName + " is not exist!");
            return;
        }
        try {
            ApplicationInfo info = this.mContext.getPackageManager()
                    .getApplicationInfo(packageName, 1);
            if (info == null) {
                return;
            }
            int uid = info.uid;
            Slog.d(TAG, "denyNetworkAccess  appUid = " + uid);
            if (uid < 10000) {
                return;
            }
            this.mPolicyManager = NetworkPolicyManager.from(this.mContext);
            boolean success = this.mPolicyManager.setUidDenyNetworkAccess(uid);
            Slog.d(TAG, "denyNetworkAccess  isSuccess = " + success);
            if (success && !sIsShowDialogDisableNet) {
                new SystemExceptionDialog(this.mContext,
                        EXCEPTION_TYPE_DISABLE_NET_APP).show();
                sIsShowDialogDisableNet = true;
            }
            saveExceptionInfo(EXCEPTION_TYPE_DISABLE_NET_APP, success, packageName);
        } catch (Exception e) {
            // ignored
        }
    }

    private void disableApp(String packageName) {
        boolean success = true;
        if (packageName.isEmpty()) {
            return;
        }
        if (!isPackageExists(packageName)) {
            Slog.d(TAG, "disableApp packageName " + packageName + " is not exist!");
            return;
        }
        int status = checkAppStatus(packageName);
        if (status >= 2) {
            Slog.d(TAG, "disableApp return, the pacakge already disable, checkStatus = "
                    + status);
            return;
        }
        this.mContext.getPackageManager().setApplicationEnabledSetting(packageName, 2, 0);
        int newStatus = checkAppStatus(packageName);
        if (newStatus == 2) {
            Slog.d(TAG, "disableApp success ret = " + newStatus);
            if (!sIsShowDialogDisableApp) {
                new SystemExceptionDialog(this.mContext, EXCEPTION_TYPE_DISABLE_APP).show();
                Slog.d(TAG, "doDisableApp show dialog packageName " + packageName);
                sIsShowDialogDisableApp = true;
            }
        } else {
            success = false;
        }
        saveExceptionInfo(EXCEPTION_TYPE_DISABLE_APP, success, packageName);
    }

    public static boolean isIpValid(String address) {
        try {
            InetAddress.getByName(address);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isJson(String json) {
        try {
            new JSONObject(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPackageExists(String packageName) {
        try {
            this.mContext.getPackageManager().getApplicationInfo(packageName, 1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static LinkedHashMap<String, String> jsonToMap(String json) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        try {
            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, object.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private void saveExceptionInfo(String type, boolean success, String packageName) {
        Slog.d(TAG, "saveExceptionInfo");
        String time = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("exception_type", type);
        map.put("exec_success", success ? "true" : "false");
        if (!packageName.equals("null") && packageName.length() > 0) {
            map.put("package", packageName);
        }
        map.put("time", time);
        new XtcBehavior.Builder(this.mContext, "secure_exception_info", map)
                .dataCollectLevel(XtcBehaviorDataLevel.A).build().sendData();
    }

    private void uninstall(String packageName) {
        boolean success = true;
        if (packageName.isEmpty()) {
            return;
        }
        if (!isPackageExists(packageName)) {
            Slog.d(TAG, "uninstall packageName " + packageName + " is not exist!");
            return;
        }
        Slog.d(TAG, "uninstall start! packageName = " + packageName);
        PackageInstaller installer =
                this.mContext.getPackageManager().getPackageInstaller();
        LocalIntentReceiver receiver = new LocalIntentReceiver();
        installer.uninstall(packageName, receiver.getIntentSender());
        Intent result = receiver.getResult();
        if (result.getIntExtra(PackageInstaller.EXTRA_STATUS,
                PackageInstaller.STATUS_FAILURE) == PackageInstaller.STATUS_SUCCESS) {
            Slog.d(TAG, "uninstall Success");
            if (!sIsShowDialogUninstal) {
                new SystemExceptionDialog(this.mContext, EXCEPTION_TYPE_UNINSTALL).show();
                sIsShowDialogUninstal = true;
            }
        } else {
            Slog.d(TAG, "Failure [" + result.getStringExtra(
                    PackageInstaller.EXTRA_STATUS_MESSAGE) + "]");
            success = false;
        }
        saveExceptionInfo(EXCEPTION_TYPE_UNINSTALL, success, packageName);
    }

    public void denyNetworkAccessAddress(String address) {
        Slog.d(TAG, "denyNetworkAccessAddress  start");
        if (address == null || address.length() == 0) {
            return;
        }
        if (!isIpValid(address)) {
            Slog.d(TAG, "denyNetworkAccessAddress  address is error!");
            return;
        }
        try {
            this.mPolicyManager = NetworkPolicyManager.from(this.mContext);
            saveExceptionInfo(EXCEPTION_TYPE_DISABLE_NET_ADDRESS,
                    this.mPolicyManager.setAddressDenyNetworkAccess(address), "null");
        } catch (Exception e) {
            // ignored
        }
    }

    public void displaySPEUimode() {
        View view = LayoutInflater.from(this.mContext)
                .inflate(com.android.internal.R.layout.system_exception_overlay, null);
        ((TextView) view.findViewById(
                com.android.internal.R.id.system_exception_overylay_message_body))
                .setText(SystemProperties.get("ro.boot.bindnumber", "unknown"));
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WINDOW_TYPE_SYSTEM;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.BOTTOM | Gravity.START;
        params.format = view.getBackground().getOpacity();
        params.flags = 24;
        params.privateFlags |= PRIVATE_FLAG_TRUSTED;
        ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE))
                .addView(view, params);
    }

    public void doDenyNetworkAccessApp(String list) {
        Slog.d(TAG, "doDenyNetworkAccessApp  start");
        if (list.startsWith("[") && list.endsWith("]")) {
            list = list.substring(1, list.length() - 1);
        }
        for (String packageName : list.split(",")) {
            if (packageName.startsWith("\"") && packageName.endsWith("\"")) {
                packageName = packageName.substring(1, packageName.length() - 1);
            }
            Slog.d(TAG, "doDenyNetworkAccessApp packageName " + packageName);
            denyNetworkAccessApp(packageName);
        }
        sIsShowDialogDisableNet = false;
    }

    public void doDisableApp(String list) {
        Slog.d(TAG, "doDisableApp  start");
        if (list.startsWith("[") && list.endsWith("]")) {
            list = list.substring(1, list.length() - 1);
        }
        for (String packageName : list.split(",")) {
            if (packageName.startsWith("\"") && packageName.endsWith("\"")) {
                packageName = packageName.substring(1, packageName.length() - 1);
            }
            Slog.d(TAG, "doDisableApp packageName " + packageName);
            disableApp(packageName);
        }
        sIsShowDialogDisableApp = false;
    }

    public void doUninstall(String list) {
        Slog.d(TAG, "doUninstall  start");
        if (list.startsWith("[") && list.endsWith("]")) {
            list = list.substring(1, list.length() - 1);
        }
        for (String packageName : list.split(",")) {
            if (packageName.startsWith("\"") && packageName.endsWith("\"")) {
                packageName = packageName.substring(1, packageName.length() - 1);
            }
            Slog.d(TAG, "doUninstall packageName " + packageName);
            uninstall(packageName);
        }
        sIsShowDialogUninstal = false;
    }

    public void messageDialog(String message) {
        if (message.isEmpty()) {
            return;
        }
        try {
            String display = "dialog_default";
            if (message.equals(SCENE_DEF) || message.length() < 20) {
                Slog.d(TAG, "messageDialog use default message");
            }
            if (message.length() > 20) {
                display = new String(message.getBytes("UTF-8"), "UTF-8");
                Slog.d(TAG, "messageDialog start message = " + display);
            }
            new SystemExceptionDialog(this.mContext, display).show();
            saveExceptionInfo(EXCEPTION_TYPE_DIALOG, true, "null");
        } catch (java.io.UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public void showStrictModeOverlay(String message) {
        if (message.equals("cancel")) {
            SystemProperties.set(PROG_OV, "false");
            return;
        }
        Slog.d(TAG, "showStrictModeOverlay start message = " + message);
        View view = LayoutInflater.from(this.mContext)
                .inflate(com.android.internal.R.layout.system_exception_overlay, null);
        if (message != null && !message.equals(SCENE_DEF) && !message.equals("cancel")) {
            Slog.d(TAG, "showStrictModeOverlay  message error!");
            return;
        }
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.type = WINDOW_TYPE_SYSTEM;
        params.width = WindowManager.LayoutParams.WRAP_CONTENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        params.gravity = Gravity.TOP | Gravity.START;
        params.format = view.getBackground().getOpacity();
        params.windowAnimations = com.android.internal.R.style.NotificationEmphasizedAction;
        params.flags = 1048;
        params.privateFlags |= PRIVATE_FLAG_TRUSTED;
        ((WindowManager) this.mContext.getSystemService(Context.WINDOW_SERVICE))
                .addView(view, params);
        saveExceptionInfo(EXCEPTION_TYPE_OVERLAY, true, "null");
        if (SystemProperties.getBoolean(PROG_OV, false)) {
            return;
        }
        SystemProperties.set(PROG_OV, "true");
    }
}