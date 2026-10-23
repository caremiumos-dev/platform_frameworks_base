/*
 * Copyright (C) 2008 The Android Open Source Project
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


package com.android.server.power;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.IActivityManager;
import android.app.KeyguardManager;
import android.app.ProgressDialog;
import android.app.WallpaperColors;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.IBluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.nfc.INfcAdapter;
import android.nfc.NfcAdapter;
import android.os.FileUtils;
import android.os.Handler;
import android.os.PowerManager;
import android.os.RecoverySystem;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.SystemVibrator;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.os.Vibrator;
import android.os.storage.IStorageManager;
import android.provider.Settings;
import android.os.storage.IStorageShutdownObserver;
import android.util.ArrayMap;
import android.util.Log;
import android.util.TimingsTraceLog;
import android.view.WindowManager;

import com.android.internal.telephony.ITelephony;
import com.android.server.RescueParty;
import com.android.server.LocalServices;
import com.android.server.pm.PackageManagerService;
import com.android.server.statusbar.StatusBarManagerInternal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class ShutdownThread extends Thread {
    // constants
    private static final String TAG = "ShutdownThread";
    private static final int PHONE_STATE_POLL_SLEEP_MSEC = 500;
    // maximum time we wait for the shutdown broadcast before going on.
    private static final int MAX_BROADCAST_TIME = 10*1000;
    private static final int MAX_SHUTDOWN_WAIT_TIME = 20*1000;
    private static final int MAX_RADIO_WAIT_TIME = 12*1000;
    private static final int MAX_UNCRYPT_WAIT_TIME = 15*60*1000;
    // constants for progress bar. the values are roughly estimated based on timeout.
    private static final int BROADCAST_STOP_PERCENT = 2;
    private static final int ACTIVITY_MANAGER_STOP_PERCENT = 4;
    private static final int PACKAGE_MANAGER_STOP_PERCENT = 6;
    private static final int RADIO_STOP_PERCENT = 18;
    private static final int MOUNT_SERVICE_STOP_PERCENT = 20;

    // length of vibration before shutting down
    private static final int SHUTDOWN_VIBRATE_MS = 500;

    // state tracking
    private static final Object sIsStartedGuard = new Object();
    private static boolean sIsStarted = false;

    private static boolean mReboot;
    private static boolean mRebootSafeMode;
    private static boolean mRebootHasProgressBar;
    private static String mReason;

    // Provides shutdown assurance in case the system_server is killed
    public static final String SHUTDOWN_ACTION_PROPERTY = "sys.shutdown.requested";

    // Indicates whether we are rebooting into safe mode
    public static final String REBOOT_SAFEMODE_PROPERTY = "persist.sys.safemode";
    public static final String RO_SAFEMODE_PROPERTY = "ro.sys.safemode";

    // XTC device addition: indicates the watch has been reported lost.
    public static final String PROPERTY_WATCH_LOSS_STATUS = "persist.sys.watchlossstatus";

    // static instance of this thread
    private static final ShutdownThread sInstance = new ShutdownThread();

    private static final AudioAttributes VIBRATION_ATTRIBUTES = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .build();

    // Metrics that will be reported to tron after reboot
    private static final ArrayMap<String, Long> TRON_METRICS = new ArrayMap<>();

    // File to use for save metrics
    private static final String METRICS_FILE_BASENAME = "/data/system/shutdown-metrics";

    // Metrics names to be persisted in shutdown-metrics file
    private static String METRIC_SYSTEM_SERVER = "shutdown_system_server";
    private static String METRIC_SEND_BROADCAST = "shutdown_send_shutdown_broadcast";
    private static String METRIC_AM = "shutdown_activity_manager";
    private static String METRIC_PM = "shutdown_package_manager";
    private static String METRIC_RADIOS = "shutdown_radios";
    private static String METRIC_BT = "shutdown_bt";
    private static String METRIC_RADIO = "shutdown_radio";
    private static String METRIC_NFC = "shutdown_nfc";
    private static String METRIC_SM = "shutdown_storage_manager";

    // XTC device additions: vendor shutdown / system-restore confirmation flow.
    private static final int MAX_LAUNCHER_WAIT_TIME = 3000;
    private static final String XTC_SHUTDOWN_STATE = "xtc_shutdown";
    private static final String POWEROFFALARM_ACTION_SHUTDOWN =
            "org.codeaurora.poweroffalarm.ACTION_SHUTDOWN";
    private static final int VENDOR_SUBSYS_MAX_WAIT_MS = 600;
    private static final int VENDOR_SUBSYS_STATE_CHECK_INTERVAL_MS = 100;

    // XTC device addition: button ids used to distinguish the vendor dialogs' actions.
    public static final int ID_SHUTDOWN = 1001;
    public static final int ID_REPAIR = 1002;
    public static final int ID_MORE = 1003;
    public static final int ID_CANCEL = 1004;
    public static final int ID_REBOOT = 1005;
    public static final int ID_RESTORE = 1005;

    private static AlertDialog lpd;
    private static AlertDialog srd;
    private static com.xtc.utils.KeyguardEventManager mKeyguard;
    private static boolean isRestore = false;
    private static boolean isRestoreAlready = false;
    private static boolean isShutdown = true;
    private static String positiveButtonText = "true";
    private static boolean isLauncherShutdown = false;
    private static ShutdownObserver shutdownObserver;
    private static com.xtc.utils.KeyguardEventManager.Listener mListener =
            new com.xtc.utils.KeyguardEventManager.Listener() {
                @Override
                public void onFinishedGoingToSleep() {
                    if (lpd != null) {
                        lpd.dismiss();
                        lpd = null;
                        isShutdown = true;
                        isRestore = false;
                        isRestoreAlready = false;
                    }
                    if (srd != null) {
                        srd.dismiss();
                        srd = null;
                        isShutdown = true;
                        isRestore = false;
                        isRestoreAlready = false;
                    }
                }
            };

    private final Object mActionDoneSync = new Object();
    private boolean mActionDone;
    private Context mContext;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mCpuWakeLock;
    private PowerManager.WakeLock mScreenWakeLock;
    private Handler mHandler;

    private static AlertDialog sConfirmDialog;
    private ProgressDialog mProgressDialog;

    private ShutdownThread() {
    }

    /**
     * Request a clean shutdown, waiting for subsystems to clean up their
     * state etc.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog. This must be a context
     *                suitable for displaying UI (aka Themable).
     * @param reason code to pass to android_reboot() (e.g. "userrequested"), or null.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void shutdown(final Context context, String reason, boolean confirm) {
        // XTC device addition: dismiss the shutdown dialogs when the device sleeps.
        if (mKeyguard == null) {
            Log.d(TAG, "init mKeyguard...");
            mKeyguard = new com.xtc.utils.KeyguardEventManager();
            mKeyguard.registerListener(mListener);
        }
        mReboot = false;
        mRebootSafeMode = false;
        mReason = reason;
        shutdownInner(context, confirm);
    }

    private static void shutdownInner(final Context context, boolean confirm) {
        // ShutdownThread is called from many places, so best to verify here that the context passed
        // in is themed.
        context.assertRuntimeOverlayThemable();

        // ensure that only one thread is trying to power down.
        // any additional calls are just returned
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Request to shutdown already running, returning.");
                return;
            }
        }

        final int longPressBehavior = context.getResources().getInteger(
                        com.android.internal.R.integer.config_longPressOnPowerBehavior);
        final int resourceId = mRebootSafeMode
                ? com.android.internal.R.string.reboot_safemode_confirm
                : (longPressBehavior == 2
                        ? com.android.internal.R.string.shutdown_confirm_question
                        : com.android.internal.R.string.shutdown_confirm);

        Log.d(TAG, "Notifying thread to start shutdown longPressBehavior=" + longPressBehavior);

        if (confirm) {
            // XTC device addition: vendor shutdown / system-restore confirmation flow.
            if (isRestoreAlready) {
                return;
            }
            if (sConfirmDialog != null) {
                sConfirmDialog.dismiss();
            }
            if (srd != null) {
                srd.dismiss();
            }
            if (isRestore && !SystemProperties.getBoolean(PROPERTY_WATCH_LOSS_STATUS, false)) {
                positiveButtonText = context.getResources().getString(
                        com.android.internal.R.string.reboot_to_restore);
                isShutdown = false;
            } else {
                positiveButtonText = context.getResources().getString(
                        com.android.internal.R.string.power_off);
                isRestore = true;
                isShutdown = true;
            }
            boolean clearAppDataOnly =
                    SystemProperties.getBoolean("sys.clearAppDataOnly", false);
            if (com.xtc.moduleswitch.ModuleSwitch.queryModuleSwitch(context, 2875, true)) {
                clearAppDataOnly = true;
            }
            final boolean allowRepair =
                    SystemProperties.getBoolean("persist.sys.allowrepair", true);
            final Context ctx = context;
            final boolean clearApp = clearAppDataOnly;

            final ShutdownDialogCallback restoreCallback = new ShutdownDialogCallback() {
                @Override
                public void clickCallBack(android.view.View view) {
                    Log.v(TAG, "isRestore>>>>>>>>>>");
                    if (lpd != null) {
                        lpd.dismiss();
                    }
                    isRestoreAlready = true;
                    Intent restoreType = new Intent("com.xtc.i11.restore.type");
                    restoreType.putExtra("restore_type", "HAND_RESTORE");
                    ctx.sendBroadcast(restoreType);
                    Intent reset = new Intent(Intent.ACTION_FACTORY_RESET);
                    reset.setPackage("android");
                    reset.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    reset.putExtra(Intent.EXTRA_REASON,
                            "Long press power key to factory reset!");
                    reset.putExtra("clearAppDataOnly", clearApp);
                    ctx.sendBroadcast(reset);
                }

                @Override
                public void dismissCallBack(android.view.View view) {
                    isShutdown = true;
                    isRestore = false;
                    isRestoreAlready = false;
                }
            };

            ShutdownDialogCallback callback = new ShutdownDialogCallback() {
                @Override
                public void clickCallBack(android.view.View view) {
                    switch (view.getId()) {
                        case ID_SHUTDOWN:
                            if (isShutdown) {
                                Log.v(TAG, "Shutdown>>>>>>>>>>");
                                beginShutdownSequence(ctx);
                                mReboot = false;
                                Intent i = new Intent("com.xtc.i11.shutdown.type");
                                i.putExtra("shutdown_type", "HAND_SHUT_DOWN");
                                ctx.sendBroadcast(i);
                            }
                            break;
                        case ID_REPAIR: {
                            AlertDialog d;
                            if (!allowRepair) {
                                SystemRestoreForbiddenDialog fd =
                                        new SystemRestoreForbiddenDialog(ctx);
                                fd.setCallback(restoreCallback);
                                d = fd;
                            } else if (clearApp) {
                                SystemRestoreConfirmDialog cd =
                                        new SystemRestoreConfirmDialog(ctx);
                                cd.setCallback(restoreCallback);
                                d = cd;
                            } else {
                                ShutdownOrRebootConfirmDialog od =
                                        new ShutdownOrRebootConfirmDialog(ctx);
                                od.setCallback(restoreCallback);
                                d = od;
                            }
                            d.show();
                            srd = d;
                            break;
                        }
                        case ID_MORE:
                            try {
                                isShutdown = true;
                                isRestore = false;
                                isRestoreAlready = false;
                                ctx.startActivity(new Intent(
                                        "com.android.internal.intent.action.MORE_ACTION"));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            break;
                        case ID_REBOOT:
                            Log.v(TAG, "reboot>>>>>>>>>>");
                            Intent rebootIntent = new Intent("com.xtc.i11.shutdown.type");
                            rebootIntent.putExtra("shutdown_type", "HAND_REBOOT");
                            ctx.sendBroadcast(rebootIntent);
                            reboot(ctx, TAG, false);
                            break;
                    }
                }

                @Override
                public void dismissCallBack(android.view.View view) {
                    isShutdown = true;
                    isRestore = false;
                    isRestoreAlready = false;
                }
            };

            AlertDialog dialog;
            if (clearApp) {
                SystemRestoreDialog d = new SystemRestoreDialog(ctx, positiveButtonText);
                d.setCallback(restoreCallback);
                dialog = d;
            } else {
                ShutdownDialog d = new ShutdownDialog(ctx, positiveButtonText);
                d.setCallback(callback);
                dialog = d;
            }
            final AlertDialog shownDialog = dialog;
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface d) {
                    new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (lpd != null) {
                                lpd.dismiss();
                            }
                            lpd = shownDialog;
                        }
                    }, 500L);
                }
            });
            dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            dialog.show();
        } else {
            beginShutdownSequence(context);
        }
    }

    private static class CloseDialogReceiver extends BroadcastReceiver
            implements DialogInterface.OnDismissListener {
        private Context mContext;
        public Dialog dialog;

        CloseDialogReceiver(Context context) {
            mContext = context;
            IntentFilter filter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.registerReceiver(this, filter);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            dialog.cancel();
        }

        public void onDismiss(DialogInterface unused) {
            mContext.unregisterReceiver(this);
        }
    }

    /**
     * XTC device addition: observes the launcher's shutdown state so shutdown can
     * wait for the launcher to close cleanly.
     */
    static class ShutdownObserver extends ContentObserver {
        private Context context;
        private Handler handler;

        ShutdownObserver(Context context, Handler handler) {
            super(handler);
            this.context = context;
            this.handler = handler;
        }

        void observe() {
            context.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(XTC_SHUTDOWN_STATE), false, this, -1);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (Settings.System.getIntForUser(context.getContentResolver(), XTC_SHUTDOWN_STATE,
                    -1, UserHandle.USER_ALL) == 1) {
                Log.d(TAG, "Launcher is closed");
                isLauncherShutdown = true;
            }
        }
    }

    /**
     * XTC device addition: write {@code content} to the file at {@code path}.
     */
    public static void RebootreasonToxtcdata(String path, String content) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            writer.write(content);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException start");
        } catch (IOException e) {
            Log.e(TAG, "IOException");
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static boolean getShutDownDialogState() {
        return isRestore;
    }

    /**
     * Request a clean shutdown, waiting for subsystems to clean up their
     * state etc.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog. This must be a context
     *                suitable for displaying UI (aka Themable).
     * @param reason code to pass to the kernel (e.g. "recovery"), or null.
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void reboot(final Context context, String reason, boolean confirm) {
        mReboot = true;
        mRebootSafeMode = false;
        mRebootHasProgressBar = false;
        mReason = reason;
        shutdownInner(context, confirm);
    }

    /**
     * Request a reboot into safe mode.  Must be called from a Looper thread in which its UI
     * is shown.
     *
     * @param context Context used to display the shutdown progress dialog. This must be a context
     *                suitable for displaying UI (aka Themable).
     * @param confirm true if user confirmation is needed before shutting down.
     */
    public static void rebootSafeMode(final Context context, boolean confirm) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        if (um.hasUserRestriction(UserManager.DISALLOW_SAFE_BOOT)) {
            return;
        }

        mReboot = true;
        mRebootSafeMode = true;
        mRebootHasProgressBar = false;
        mReason = null;
        shutdownInner(context, confirm);
    }

    private static ProgressDialog showShutdownDialog(Context context) {
        // XTC device addition: tell the anti-addiction service the device is shutting down.
        com.android.server.addiction.AntiAddictionManagerService antiAddictionManagerService =
                (com.android.server.addiction.AntiAddictionManagerService)
                        ServiceManager.getService("anti_addiction");
        if (antiAddictionManagerService != null) {
            antiAddictionManagerService.notifyAppState("", "", "", -1,
                    com.android.server.addiction.AntiAddictionManagerService.AppState.shutdown);
        }
        // Throw up a system dialog to indicate the device is rebooting / shutting down.
        ProgressDialog pd = new ProgressDialog(context);

        // Path 1: Reboot to recovery for update
        //   Condition: mReason startswith REBOOT_RECOVERY_UPDATE
        //
        //  Path 1a: uncrypt needed
        //   Condition: if /cache/recovery/uncrypt_file exists but
        //              /cache/recovery/block.map doesn't.
        //   UI: determinate progress bar (mRebootHasProgressBar == True)
        //
        // * Path 1a is expected to be removed once the GmsCore shipped on
        //   device always calls uncrypt prior to reboot.
        //
        //  Path 1b: uncrypt already done
        //   UI: spinning circle only (no progress bar)
        //
        // Path 2: Reboot to recovery for factory reset
        //   Condition: mReason == REBOOT_RECOVERY
        //   UI: spinning circle only (no progress bar)
        //
        // Path 3: Regular reboot / shutdown
        //   Condition: Otherwise
        //   UI: spinning circle only (no progress bar)

        // mReason could be "recovery-update" or "recovery-update,quiescent".
        if (mReason != null && mReason.startsWith(PowerManager.REBOOT_RECOVERY_UPDATE)) {
            // We need the progress bar if uncrypt will be invoked during the
            // reboot, which might be time-consuming.
            mRebootHasProgressBar = RecoverySystem.UNCRYPT_PACKAGE_FILE.exists()
                    && !(RecoverySystem.BLOCK_MAP_FILE.exists());
            pd.setTitle(context.getText(com.android.internal.R.string.reboot_to_update_title));
            if (mRebootHasProgressBar) {
                pd.setMax(100);
                pd.setProgress(0);
                pd.setIndeterminate(false);
                pd.setProgressNumberFormat(null);
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pd.setMessage(context.getText(
                            com.android.internal.R.string.reboot_to_update_prepare));
            } else {
                if (showSysuiReboot()) {
                    return null;
                }
                pd.setIndeterminate(true);
                pd.setMessage(context.getText(
                            com.android.internal.R.string.reboot_to_update_reboot));
            }
        } else if (mReason != null && mReason.equals(PowerManager.REBOOT_RECOVERY)) {
            if (RescueParty.isAttemptingFactoryReset()) {
                // We're not actually doing a factory reset yet; we're rebooting
                // to ask the user if they'd like to reset, so give them a less
                // scary dialog message.
                pd.setTitle(context.getText(com.android.internal.R.string.power_off));
                pd.setMessage(context.getText(com.android.internal.R.string.shutdown_progress));
                pd.setIndeterminate(true);
            } else {
                // Factory reset path. Set the dialog message accordingly.
                pd.setTitle(context.getText(com.android.internal.R.string.reboot_to_reset_title));
                pd.setMessage(context.getText(
                            com.android.internal.R.string.reboot_to_reset_message));
                pd.setIndeterminate(true);
            }
        } else {
            if (showSysuiReboot()) {
                return null;
            }
            pd.setTitle(context.getText(com.android.internal.R.string.power_off));
            pd.setMessage(context.getText(com.android.internal.R.string.shutdown_progress));
            pd.setIndeterminate(true);
        }
        pd.setCancelable(false);
        pd.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);

        pd.show();
        return pd;
    }

    private static boolean showSysuiReboot() {
        Log.d(TAG, "Attempting to use SysUI shutdown UI");
        try {
            StatusBarManagerInternal service = LocalServices.getService(
                    StatusBarManagerInternal.class);
            if (service.showShutdownUi(mReboot, mReason)) {
                // Sysui will handle shutdown UI.
                Log.d(TAG, "SysUI handling shutdown UI");
                return true;
            }
        } catch (Exception e) {
            // If anything went wrong, ignore it and use fallback ui
        }
        Log.d(TAG, "SysUI is unavailable");
        return false;
    }

    private static void beginShutdownSequence(Context context) {
        synchronized (sIsStartedGuard) {
            if (sIsStarted) {
                Log.d(TAG, "Shutdown sequence already running, returning.");
                return;
            }
            sIsStarted = true;
        }

        sInstance.mProgressDialog = showShutdownDialog(context);
        sInstance.mContext = context;
        sInstance.mPowerManager = (PowerManager)context.getSystemService(Context.POWER_SERVICE);

        // make sure we never fall asleep again
        sInstance.mCpuWakeLock = null;
        try {
            sInstance.mCpuWakeLock = sInstance.mPowerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK, TAG + "-cpu");
            sInstance.mCpuWakeLock.setReferenceCounted(false);
            sInstance.mCpuWakeLock.acquire();
        } catch (SecurityException e) {
            Log.w(TAG, "No permission to acquire wake lock", e);
            sInstance.mCpuWakeLock = null;
        }

        // also make sure the screen stays on for better user experience
        sInstance.mScreenWakeLock = null;
        if (sInstance.mPowerManager.isScreenOn()) {
            try {
                sInstance.mScreenWakeLock = sInstance.mPowerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK, TAG + "-screen");
                sInstance.mScreenWakeLock.setReferenceCounted(false);
                sInstance.mScreenWakeLock.acquire();
            } catch (SecurityException e) {
                Log.w(TAG, "No permission to acquire wake lock", e);
                sInstance.mScreenWakeLock = null;
            }
        }

        // start the thread that initiates shutdown
        sInstance.mHandler = new Handler() {
        };
        sInstance.start();
    }

    /**
     * XTC device addition: wait up to {@code timeoutMs} for the launcher to close.
     */
    private void waitLauncherShutdown(int timeoutMs) {
        final boolean[] done = { false };
        final long deadline = SystemClock.elapsedRealtime() + timeoutMs;
        Thread thread = new Thread() {
            @Override
            public void run() {
                long end = deadline;
                for (long now = SystemClock.elapsedRealtime(); end - now > 0;
                        now = SystemClock.elapsedRealtime()) {
                    if (isLauncherShutdown) {
                        done[0] = true;
                        return;
                    }
                    end = deadline;
                }
            }
        };
        thread.start();
        try {
            thread.join(timeoutMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!done[0]) {
            Log.w(TAG, "waiting for Launcher shutdown.");
        }
    }

    void actionDone() {
        synchronized (mActionDoneSync) {
            mActionDone = true;
            mActionDoneSync.notifyAll();
        }
    }

    /**
     * Makes sure we handle the shutdown gracefully.
     * Shuts off power regardless of radio and bluetooth state if the alloted time has passed.
     */
    public void run() {
        TimingsTraceLog shutdownTimingLog = newTimingsLog();
        shutdownTimingLog.traceBegin("SystemServerShutdown");
        metricStarted(METRIC_SYSTEM_SERVER);

        BroadcastReceiver br = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                // We don't allow apps to cancel this, so ignore the result.
                actionDone();
            }
        };

        /*
         * Write a system property in case the system_server reboots before we
         * get to the actual hardware restart. If that happens, we'll retry at
         * the beginning of the SystemServer startup.
         */
        {
            String reason = (mReboot ? "1" : "0") + (mReason != null ? mReason : "");
            SystemProperties.set(SHUTDOWN_ACTION_PROPERTY, reason);
            // XTC device addition: report the shutdown/reboot target.
            if (mReboot) {
                Log.d(TAG, "Target--reboot. '" + reason + "'");
                android.util.ShowKernel.v("5", "1reboot," + (mReason != null ? mReason : ""));
                RebootreasonToxtcdata("/xtcdata/app_reboot.txt",
                        SystemProperties.get("ro.boot.xtc.bootcounter") + "," + mReason + ",");
            } else {
                Log.d(TAG, "Target--shutdown.");
                android.util.ShowKernel.v("1", "1shutdown");
            }
        }

        /*
         * If we are rebooting into safe mode, write a system property
         * indicating so.
         */
        if (mRebootSafeMode) {
            SystemProperties.set(REBOOT_SAFEMODE_PROPERTY, "1");
        }

        metricStarted(METRIC_SEND_BROADCAST);
        shutdownTimingLog.traceBegin("SendShutdownBroadcast");
        Log.i(TAG, "Sending shutdown broadcast...");

        // First send the high-level shut down broadcast.
        mActionDone = false;
        Intent intent = new Intent(Intent.ACTION_SHUTDOWN);
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND
                | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        mContext.sendOrderedBroadcastAsUser(intent,
                UserHandle.ALL, null, br, mHandler, 0, null, null);

        // XTC device addition: wait for the launcher to close before proceeding.
        if (mReason == null || !mReason.contains("AutoTest")) {
            Log.d(TAG, "waitLauncherShutdown start...");
            waitLauncherShutdown(MAX_LAUNCHER_WAIT_TIME);
            Log.d(TAG, "waitLauncherShutdown end...");
        } else {
            Log.d(TAG, "auto reboot test, remove waitLauncherShutdown");
        }

        final long endTime = SystemClock.elapsedRealtime() + MAX_BROADCAST_TIME;
        synchronized (mActionDoneSync) {
            while (!mActionDone) {
                long delay = endTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.w(TAG, "Shutdown broadcast timed out");
                    break;
                } else if (mRebootHasProgressBar) {
                    int status = (int)((MAX_BROADCAST_TIME - delay) * 1.0 *
                            BROADCAST_STOP_PERCENT / MAX_BROADCAST_TIME);
                    sInstance.setRebootProgress(status, null);
                }
                try {
                    mActionDoneSync.wait(Math.min(delay, PHONE_STATE_POLL_SLEEP_MSEC));
                } catch (InterruptedException e) {
                }
            }
        }
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(BROADCAST_STOP_PERCENT, null);
        }
        shutdownTimingLog.traceEnd(); // SendShutdownBroadcast
        metricEnded(METRIC_SEND_BROADCAST);

        Log.i(TAG, "Shutting down activity manager...");
        shutdownTimingLog.traceBegin("ShutdownActivityManager");
        metricStarted(METRIC_AM);

        final IActivityManager am =
                IActivityManager.Stub.asInterface(ServiceManager.checkService("activity"));
        if (am != null) {
            try {
                am.shutdown(MAX_BROADCAST_TIME);
            } catch (RemoteException e) {
            }
        }
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(ACTIVITY_MANAGER_STOP_PERCENT, null);
        }
        shutdownTimingLog.traceEnd();// ShutdownActivityManager
        metricEnded(METRIC_AM);

        Log.i(TAG, "Shutting down package manager...");
        shutdownTimingLog.traceBegin("ShutdownPackageManager");
        metricStarted(METRIC_PM);

        final PackageManagerService pm = (PackageManagerService)
            ServiceManager.getService("package");
        if (pm != null) {
            pm.shutdown();
        }
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(PACKAGE_MANAGER_STOP_PERCENT, null);
        }
        shutdownTimingLog.traceEnd(); // ShutdownPackageManager
        metricEnded(METRIC_PM);

        // Shutdown radios.
        shutdownTimingLog.traceBegin("ShutdownRadios");
        metricStarted(METRIC_RADIOS);
        shutdownRadios(MAX_RADIO_WAIT_TIME);
        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(RADIO_STOP_PERCENT, null);
        }
        shutdownTimingLog.traceEnd(); // ShutdownRadios
        metricEnded(METRIC_RADIOS);

        // Shutdown StorageManagerService to ensure media is in a safe state
        IStorageShutdownObserver observer = new IStorageShutdownObserver.Stub() {
            public void onShutDownComplete(int statusCode) throws RemoteException {
                Log.w(TAG, "Result code " + statusCode + " from StorageManagerService.shutdown");
                actionDone();
            }
        };

        Log.i(TAG, "Shutting down StorageManagerService");
        shutdownTimingLog.traceBegin("ShutdownStorageManager");
        metricStarted(METRIC_SM);

        // Set initial variables and time out time.
        mActionDone = false;
        final long endShutTime = SystemClock.elapsedRealtime() + MAX_SHUTDOWN_WAIT_TIME;
        synchronized (mActionDoneSync) {
            try {
                final IStorageManager storageManager = IStorageManager.Stub.asInterface(
                        ServiceManager.checkService("mount"));
                if (storageManager != null) {
                    storageManager.shutdown(observer);
                } else {
                    Log.w(TAG, "StorageManagerService unavailable for shutdown");
                }
            } catch (Exception e) {
                Log.e(TAG, "Exception during StorageManagerService shutdown", e);
            }
            while (!mActionDone) {
                long delay = endShutTime - SystemClock.elapsedRealtime();
                if (delay <= 0) {
                    Log.w(TAG, "StorageManager shutdown wait timed out");
                    break;
                } else if (mRebootHasProgressBar) {
                    int status = (int)((MAX_SHUTDOWN_WAIT_TIME - delay) * 1.0 *
                            (MOUNT_SERVICE_STOP_PERCENT - RADIO_STOP_PERCENT) /
                            MAX_SHUTDOWN_WAIT_TIME);
                    status += RADIO_STOP_PERCENT;
                    sInstance.setRebootProgress(status, null);
                }
                try {
                    mActionDoneSync.wait(Math.min(delay, PHONE_STATE_POLL_SLEEP_MSEC));
                } catch (InterruptedException e) {
                }
            }
        }
        shutdownTimingLog.traceEnd(); // ShutdownStorageManager
        metricEnded(METRIC_SM);

        if (mRebootHasProgressBar) {
            sInstance.setRebootProgress(MOUNT_SERVICE_STOP_PERCENT, null);

            // If it's to reboot to install an update and uncrypt hasn't been
            // done yet, trigger it now.
            uncrypt();
        }

        shutdownTimingLog.traceEnd(); // SystemServerShutdown
        metricEnded(METRIC_SYSTEM_SERVER);
        saveMetrics(mReboot);
        rebootOrShutdown(mContext, mReboot, mReason);
    }

    private static TimingsTraceLog newTimingsLog() {
        return new TimingsTraceLog("ShutdownTiming", Trace.TRACE_TAG_SYSTEM_SERVER);
    }

    private static void metricStarted(String metricKey) {
        synchronized (TRON_METRICS) {
            TRON_METRICS.put(metricKey, -1 * SystemClock.elapsedRealtime());
        }
    }

    private static void metricEnded(String metricKey) {
        synchronized (TRON_METRICS) {
            TRON_METRICS
                    .put(metricKey, SystemClock.elapsedRealtime() + TRON_METRICS.get(metricKey));
        }
    }

    private void setRebootProgress(final int progress, final CharSequence message) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mProgressDialog != null) {
                    mProgressDialog.setProgress(progress);
                    if (message != null) {
                        mProgressDialog.setMessage(message);
                    }
                }
            }
        });
    }

    private void shutdownRadios(final int timeout) {
        // If a radio is wedged, disabling it may hang so we do this work in another thread,
        // just in case.
        final long endTime = SystemClock.elapsedRealtime() + timeout;
        final boolean[] done = new boolean[1];
        Thread t = new Thread() {
            public void run() {
                TimingsTraceLog shutdownTimingsTraceLog = newTimingsLog();
                boolean nfcOff;
                boolean bluetoothReadyForShutdown;
                boolean radioOff;

                final INfcAdapter nfc =
                        INfcAdapter.Stub.asInterface(ServiceManager.checkService("nfc"));
                final ITelephony phone =
                        ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
                final IBluetoothManager bluetooth =
                        IBluetoothManager.Stub.asInterface(ServiceManager.checkService(
                                BluetoothAdapter.BLUETOOTH_MANAGER_SERVICE));
                try {
                    nfcOff = nfc == null ||
                             nfc.getState() == NfcAdapter.STATE_OFF;
                    if (!nfcOff) {
                        Log.w(TAG, "Turning off NFC...");
                        metricStarted(METRIC_NFC);
                        nfc.disable(false); // Don't persist new state
                    }
                } catch (RemoteException ex) {
                Log.e(TAG, "RemoteException during NFC shutdown", ex);
                    nfcOff = true;
                }

                try {
                    bluetoothReadyForShutdown = bluetooth == null ||
                            bluetooth.getState() == BluetoothAdapter.STATE_OFF;
                    if (!bluetoothReadyForShutdown) {
                        Log.w(TAG, "Disabling Bluetooth...");
                        metricStarted(METRIC_BT);
                        bluetooth.disable(mContext.getPackageName(), false);  // disable but don't persist new state
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "RemoteException during bluetooth shutdown", ex);
                    bluetoothReadyForShutdown = true;
                }

                try {
                    radioOff = phone == null || !phone.needMobileRadioShutdown();
                    if (!radioOff) {
                        Log.w(TAG, "Turning off cellular radios...");
                        metricStarted(METRIC_RADIO);
                        phone.shutdownMobileRadios();
                    }
                } catch (RemoteException ex) {
                    Log.e(TAG, "RemoteException during radio shutdown", ex);
                    radioOff = true;
                }

                Log.i(TAG, "Waiting for NFC, Bluetooth and Radio...");

                long delay = endTime - SystemClock.elapsedRealtime();
                while (delay > 0) {
                    if (mRebootHasProgressBar) {
                        int status = (int)((timeout - delay) * 1.0 *
                                (RADIO_STOP_PERCENT - PACKAGE_MANAGER_STOP_PERCENT) / timeout);
                        status += PACKAGE_MANAGER_STOP_PERCENT;
                        sInstance.setRebootProgress(status, null);
                    }

                    if (!bluetoothReadyForShutdown) {
                        try {
                          // BLE only mode can happen when BT is turned off
                          // We will continue shutting down in such case
                          bluetoothReadyForShutdown =
                                  bluetooth.getState() == BluetoothAdapter.STATE_OFF ||
                                  bluetooth.getState() == BluetoothAdapter.STATE_BLE_TURNING_OFF ||
                                  bluetooth.getState() == BluetoothAdapter.STATE_BLE_ON;
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during bluetooth shutdown", ex);
                            bluetoothReadyForShutdown = true;
                        }
                        if (bluetoothReadyForShutdown) {
                            Log.i(TAG, "Bluetooth turned off.");
                            metricEnded(METRIC_BT);
                            shutdownTimingsTraceLog
                                    .logDuration("ShutdownBt", TRON_METRICS.get(METRIC_BT));
                        }
                    }
                    if (!radioOff) {
                        try {
                            radioOff = !phone.needMobileRadioShutdown();
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during radio shutdown", ex);
                            radioOff = true;
                        }
                        if (radioOff) {
                            Log.i(TAG, "Radio turned off.");
                            metricEnded(METRIC_RADIO);
                            shutdownTimingsTraceLog
                                    .logDuration("ShutdownRadio", TRON_METRICS.get(METRIC_RADIO));
                        }
                    }
                    if (!nfcOff) {
                        try {
                            nfcOff = nfc.getState() == NfcAdapter.STATE_OFF;
                        } catch (RemoteException ex) {
                            Log.e(TAG, "RemoteException during NFC shutdown", ex);
                            nfcOff = true;
                        }
                        if (nfcOff) {
                            Log.i(TAG, "NFC turned off.");
                            metricEnded(METRIC_NFC);
                            shutdownTimingsTraceLog
                                    .logDuration("ShutdownNfc", TRON_METRICS.get(METRIC_NFC));
                        }
                    }

                    if (radioOff && bluetoothReadyForShutdown && nfcOff) {
                        Log.i(TAG, "NFC, Radio and Bluetooth shutdown complete.");
                        done[0] = true;
                        break;
                    }
                    SystemClock.sleep(PHONE_STATE_POLL_SLEEP_MSEC);

                    delay = endTime - SystemClock.elapsedRealtime();
                }
            }
        };

        t.start();
        try {
            t.join(timeout);
        } catch (InterruptedException ex) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for NFC, Radio and Bluetooth shutdown.");
        }
    }

    /**
     * Do not call this directly. Use {@link #reboot(Context, String, boolean)}
     * or {@link #shutdown(Context, String, boolean)} instead.
     *
     * @param context Context used to vibrate or null without vibration
     * @param reboot true to reboot or false to shutdown
     * @param reason reason for reboot/shutdown
     */
    public static void rebootOrShutdown(final Context context, boolean reboot, String reason) {
        // XTC device addition: wait briefly for critical vendor subsystems to power down.
        String subsysList = SystemProperties.get(
                "vendor.peripheral.shutdown_critical_list", "ERROR");
        if (!subsysList.equals("ERROR")) {
            Log.i(TAG, "Shutdown critical subsyslist is :" + subsysList + ": ");
            Log.i(TAG, "Waiting for a maximum of " + VENDOR_SUBSYS_MAX_WAIT_MS + "ms");
            String[] subsys = subsysList.split(" ");
            int count = 0;
            boolean allDone;
            do {
                allDone = true;
                for (String s : subsys) {
                    if (SystemProperties.get("vendor.peripheral." + s + ".state", "ERROR")
                            .equals("ONLINE")) {
                        allDone = false;
                    }
                }
                if (!allDone) {
                    SystemClock.sleep(VENDOR_SUBSYS_STATE_CHECK_INTERVAL_MS);
                    count++;
                }
                if (allDone) {
                    break;
                }
            } while (count < VENDOR_SUBSYS_MAX_WAIT_MS / VENDOR_SUBSYS_STATE_CHECK_INTERVAL_MS);
            if (allDone) {
                Log.i(TAG, "Vendor subsystem(s) shutdown successful");
            } else {
                for (String s : subsys) {
                    if (SystemProperties.get("vendor.peripheral." + s + ".state", "ERROR")
                            .equals("ONLINE")) {
                        Log.w(TAG, "Subsystem " + s + "did not shut down within timeout");
                    }
                }
            }
        }
        if (reboot) {
            Log.i(TAG, "Rebooting, reason: " + reason);
            PowerManagerService.lowLevelReboot(reason);
            Log.e(TAG, "Reboot failed, will attempt shutdown instead");
            reason = null;
        } else if (SHUTDOWN_VIBRATE_MS > 0 && context != null) {
            // vibrate before shutting down
            Vibrator vibrator = new SystemVibrator(context);
            try {
                vibrator.vibrate(SHUTDOWN_VIBRATE_MS, VIBRATION_ATTRIBUTES);
            } catch (Exception e) {
                // Failure to vibrate shouldn't interrupt shutdown.  Just log it.
                Log.w(TAG, "Failed to vibrate during shutdown.", e);
            }

            // vibrator is asynchronous so we need to wait to avoid shutting down too soon.
            try {
                Thread.sleep(SHUTDOWN_VIBRATE_MS);
            } catch (InterruptedException unused) {
            }
        }
        // Shutdown power
        Log.i(TAG, "Performing low-level shutdown...");
        PowerManagerService.lowLevelShutdown(reason);
    }

    private static void saveMetrics(boolean reboot) {
        StringBuilder metricValue = new StringBuilder();
        metricValue.append("reboot:");
        metricValue.append(reboot ? "y" : "n");
        final int metricsSize = TRON_METRICS.size();
        for (int i = 0; i < metricsSize; i++) {
            final String name = TRON_METRICS.keyAt(i);
            final long value = TRON_METRICS.valueAt(i);
            if (value < 0) {
                Log.e(TAG, "metricEnded wasn't called for " + name);
                continue;
            }
            metricValue.append(',').append(name).append(':').append(value);
        }
        File tmp = new File(METRICS_FILE_BASENAME + ".tmp");
        boolean saved = false;
        try (FileOutputStream fos = new FileOutputStream(tmp)) {
            fos.write(metricValue.toString().getBytes(StandardCharsets.UTF_8));
            saved = true;
        } catch (IOException e) {
            Log.e(TAG,"Cannot save shutdown metrics", e);
        }
        if (saved) {
            tmp.renameTo(new File(METRICS_FILE_BASENAME + ".txt"));
        }
    }

    private void uncrypt() {
        Log.i(TAG, "Calling uncrypt and monitoring the progress...");

        final RecoverySystem.ProgressListener progressListener =
                new RecoverySystem.ProgressListener() {
            @Override
            public void onProgress(int status) {
                if (status >= 0 && status < 100) {
                    // Scale down to [MOUNT_SERVICE_STOP_PERCENT, 100).
                    status = (int)(status * (100.0 - MOUNT_SERVICE_STOP_PERCENT) / 100);
                    status += MOUNT_SERVICE_STOP_PERCENT;
                    CharSequence msg = mContext.getText(
                            com.android.internal.R.string.reboot_to_update_package);
                    sInstance.setRebootProgress(status, msg);
                } else if (status == 100) {
                    CharSequence msg = mContext.getText(
                            com.android.internal.R.string.reboot_to_update_reboot);
                    sInstance.setRebootProgress(status, msg);
                } else {
                    // Ignored
                }
            }
        };

        final boolean[] done = new boolean[1];
        done[0] = false;
        Thread t = new Thread() {
            @Override
            public void run() {
                RecoverySystem rs = (RecoverySystem) mContext.getSystemService(
                        Context.RECOVERY_SERVICE);
                String filename = null;
                try {
                    filename = FileUtils.readTextFile(RecoverySystem.UNCRYPT_PACKAGE_FILE, 0, null);
                    rs.processPackage(mContext, new File(filename), progressListener);
                } catch (IOException e) {
                    Log.e(TAG, "Error uncrypting file", e);
                }
                done[0] = true;
            }
        };
        t.start();

        try {
            t.join(MAX_UNCRYPT_WAIT_TIME);
        } catch (InterruptedException unused) {
        }
        if (!done[0]) {
            Log.w(TAG, "Timed out waiting for uncrypt.");
            final int uncryptTimeoutError = 100;
            String timeoutMessage = String.format("uncrypt_time: %d\n" + "uncrypt_error: %d\n",
                    MAX_UNCRYPT_WAIT_TIME / 1000, uncryptTimeoutError);
            try {
                FileUtils.stringToFile(RecoverySystem.UNCRYPT_STATUS_FILE, timeoutMessage);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write timeout message to uncrypt status", e);
            }
        }
    }
}
