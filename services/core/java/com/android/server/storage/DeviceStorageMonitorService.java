/*
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.server.storage;

import android.annotation.WorkerThread;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.TrafficStats;
import android.os.Binder;
import android.os.Environment;
import android.os.FileObserver;
import android.app.ActivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.Settings;
import android.view.View;
import android.text.format.DateUtils;
import android.util.ArrayMap;
import android.util.Slog;

import com.android.internal.messages.nano.SystemMessageProto.SystemMessage;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.EventLogTags;
import com.android.server.IoThread;
import com.android.server.SystemService;
import com.android.server.pm.InstructionSets;
import com.android.server.pm.PackageManagerService;
import com.android.server.power.ShutdownThread;

import dalvik.system.VMRuntime;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service that monitors and maintains free space on storage volumes.
 * <p>
 * As the free space on a volume nears the threshold defined by
 * {@link StorageManager#getStorageLowBytes(File)}, this service will clear out
 * cached data to keep the disk from entering this low state.
 */
public class DeviceStorageMonitorService extends SystemService {
    private static final String TAG = "DeviceStorageMonitorService";

    /**
     * Extra for {@link android.content.Intent#ACTION_BATTERY_CHANGED}:
     * Current int sequence number of the update.
     */
    public static final String EXTRA_SEQUENCE = "seq";

    private static final int MSG_CHECK = 1;

    private static final long DEFAULT_LOG_DELTA_BYTES = 64 * TrafficStats.MB_IN_BYTES;
    private static final long DEFAULT_CHECK_INTERVAL = DateUtils.MINUTE_IN_MILLIS;

    // com.android.internal.R.string.low_internal_storage_view_text_no_boot
    // hard codes 250MB in the message as the storage space required for the
    // boot image.
    private static final long BOOT_IMAGE_STORAGE_REQUIREMENT = 250 * TrafficStats.MB_IN_BYTES;

    // XTC device additions: storage-space-clean prompting.
    private static final String ACTION_CLASS_DISABLE = "com.xtc.setting.action.CLASS.ACTION";
    private static final String ACTION_HIGHTEMP_DISABLE = "android.intent.action.KILL_APP";
    private static final String ACTION_LONG_TIME_MODE =
            "com.xtc.i3launcher.action.LONG_BATTERY_LIFE_CHANGE";
    private static final String ACTION_LOSS = "com.xtc.setting.WATCH.LOSS";
    private static final String ACTION_SLEEP_MODE = "com.xtc.i3launcher.action.SLEEP_MODE_CHANGE";
    private static final String ACTION_VIDEO_CALL = "com.xtc.videochat.misscall";
    private static final String INTERFACE_JUDGE =
            "com.xtc.i3launcher.module.home.view.activity.HomeActivity";
    private static final long DEFAULT_FULL_THRESHOLD_BYTES = 50 * TrafficStats.MB_IN_BYTES;
    private static final long DEFAULT_THRESHOLD_MAX_BYTES = 300 * TrafficStats.MB_IN_BYTES;

    private static SpaceCleanDialog scdialog;
    private static String fullspaceButtonText = "Full_Space";
    private static String lowspaceButtonText = "Low_Space";
    private static long THRESHOLDTIME = 3000000;
    private static long SAVETIME = 0;
    private static int SETTIME = 259200;
    private static int TIME_LOWCALIBRATION_FLAG = 0;
    private static int TIME_FULLCALIBRATION_FLAG = 0;
    private static boolean status_detection = false;

    private NotificationManager mNotifManager;

    /** Sequence number used for testing */
    private final AtomicInteger mSeq = new AtomicInteger(1);
    /** Forced level used for testing */
    private volatile int mForceLevel = State.LEVEL_UNKNOWN;

    /** Map from storage volume UUID to internal state */
    private final ArrayMap<UUID, State> mStates = new ArrayMap<>();

    /**
     * State for a specific storage volume, including the current "level" that
     * we've alerted the user and apps about.
     */
    private static class State {
        private static final int LEVEL_UNKNOWN = -1;
        private static final int LEVEL_NORMAL = 0;
        private static final int LEVEL_LOW = 1;
        private static final int LEVEL_FULL = 2;

        /** Last "level" that we alerted about */
        public int level = LEVEL_NORMAL;
        /** Last {@link File#getUsableSpace()} that we logged about */
        public long lastUsableBytes = Long.MAX_VALUE;

        /**
         * Test if the given level transition is "entering" a specific level.
         * <p>
         * As an example, a transition from {@link #LEVEL_NORMAL} to
         * {@link #LEVEL_FULL} is considered to "enter" both {@link #LEVEL_LOW}
         * and {@link #LEVEL_FULL}.
         */
        private static boolean isEntering(int level, int oldLevel, int newLevel) {
            return newLevel >= level && (oldLevel < level || oldLevel == LEVEL_UNKNOWN);
        }

        /**
         * Test if the given level transition is "leaving" a specific level.
         * <p>
         * As an example, a transition from {@link #LEVEL_FULL} to
         * {@link #LEVEL_NORMAL} is considered to "leave" both
         * {@link #LEVEL_FULL} and {@link #LEVEL_LOW}.
         */
        private static boolean isLeaving(int level, int oldLevel, int newLevel) {
            return newLevel < level && (oldLevel >= level || oldLevel == LEVEL_UNKNOWN);
        }

        private static String levelToString(int level) {
            switch (level) {
                case State.LEVEL_UNKNOWN: return "UNKNOWN";
                case State.LEVEL_NORMAL: return "NORMAL";
                case State.LEVEL_LOW: return "LOW";
                case State.LEVEL_FULL: return "FULL";
                default: return Integer.toString(level);
            }
        }
    }

    private CacheFileDeletedObserver mCacheFileDeletedObserver;

    // XTC device additions: broadcast receivers for launcher-start and special modes.
    private final BroadcastReceiver mBroadcastReceiver;
    private final BroadcastReceiver SpaceCleanReceiver;

    /**
     * This string is used for ServiceManager access to this class.
     */
    static final String SERVICE = "devicestoragemonitor";

    private static final String TV_NOTIFICATION_CHANNEL_ID = "devicestoragemonitor.tv";

    private final HandlerThread mHandlerThread;
    private final Handler mHandler;

    private State findOrCreateState(UUID uuid) {
        State state = mStates.get(uuid);
        if (state == null) {
            state = new State();
            mStates.put(uuid, state);
        }
        return state;
    }

    /**
     * Core logic that checks the storage state of every mounted private volume.
     * Since this can do heavy I/O, callers should invoke indirectly using
     * {@link #MSG_CHECK}.
     */
    @WorkerThread
    private void check() {
        final StorageManager storage = getContext().getSystemService(StorageManager.class);
        final int seq = mSeq.get();

        // XTC device addition: special modes in which the clean-up prompt is paused.
        final boolean classDisable =
                SystemProperties.getBoolean("persist.sys.classmodestatus", false);
        final boolean longBatteryLife =
                SystemProperties.getBoolean("persist.sys.longbatterylife", false);
        final boolean sleepMode =
                SystemProperties.getBoolean("persist.sys.sleepmodestatus", false);
        final boolean highTemp =
                SystemProperties.getBoolean("persist.sys.bt.isHighTempe", false);
        final boolean videoCall =
                SystemProperties.getBoolean("persist.sys.vc.calling", false);
        final boolean watchLoss = SystemProperties.getBoolean(
                ShutdownThread.PROPERTY_WATCH_LOSS_STATUS, false);
        long lastDiff = 0;

        // Check every mounted private volume to see if they're low on space
        for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
            final File file = vol.getPath();
            final long fullBytes = storage.getStorageFullBytes(file);
            final long lowBytes = storage.getStorageLowBytes(file);

            // Automatically trim cached data when nearing the low threshold;
            // when it's within 150% of the threshold, we try trimming usage
            // back to 200% of the threshold.
            if (file.getUsableSpace() < (lowBytes * 3) / 2) {
                final PackageManagerService pms = (PackageManagerService) ServiceManager
                        .getService("package");
                try {
                    pms.freeStorage(vol.getFsUuid(), lowBytes * 2, 0);
                } catch (IOException e) {
                    Slog.w(TAG, e);
                }
            }

            // Send relevant broadcasts and show notifications based on any
            // recently noticed state transitions.
            final UUID uuid = StorageManager.convert(vol.getFsUuid());
            final State state = findOrCreateState(uuid);
            final long totalBytes = file.getTotalSpace();
            final long usableBytes = file.getUsableSpace();

            int oldLevel = state.level;
            int newLevel;
            if (mForceLevel != State.LEVEL_UNKNOWN) {
                // When in testing mode, use unknown old level to force sending
                // of any relevant broadcasts.
                oldLevel = State.LEVEL_UNKNOWN;
                newLevel = mForceLevel;
            } else if (usableBytes <= fullBytes) {
                newLevel = State.LEVEL_FULL;
                TIME_LOWCALIBRATION_FLAG = 0;
            } else if (usableBytes <= lowBytes) {
                newLevel = State.LEVEL_LOW;
                TIME_FULLCALIBRATION_FLAG = 0;
            } else if (StorageManager.UUID_DEFAULT.equals(uuid) && !isBootImageOnDisk()
                    && usableBytes < BOOT_IMAGE_STORAGE_REQUIREMENT) {
                newLevel = State.LEVEL_LOW;
            } else {
                newLevel = State.LEVEL_NORMAL;
                TIME_LOWCALIBRATION_FLAG = 0;
                TIME_FULLCALIBRATION_FLAG = 0;
            }

            Slog.v(TAG, "classdisable=" + classDisable + "  longtimemode=" + longBatteryLife
                    + "  reportloss" + watchLoss + "  sleepmode=" + sleepMode
                    + "  hightempdisable=" + highTemp + "  videocall=" + videoCall);
            if (classDisable || longBatteryLife || sleepMode || highTemp || videoCall
                    || watchLoss) {
                Slog.v(TAG, "Special mode pause prompt");
                scheduledSpaceClean(null, false);
            }

            // Log whenever we notice drastic storage changes
            if ((Math.abs(state.lastUsableBytes - usableBytes) > DEFAULT_LOG_DELTA_BYTES)
                    || oldLevel != newLevel) {
                EventLogTags.writeStorageState(uuid.toString(), oldLevel, newLevel,
                        usableBytes, totalBytes);
                state.lastUsableBytes = usableBytes;
            }

            Slog.v(TAG, "oldLevel=" + State.levelToString(oldLevel)
                    + ", newLevel=" + State.levelToString(newLevel));
            if (oldLevel == State.LEVEL_FULL && newLevel == State.LEVEL_LOW) {
                TIME_LOWCALIBRATION_FLAG = 3;
            }

            updateNotifications(vol, oldLevel, newLevel, state);
            updateBroadcasts(vol, oldLevel, newLevel, seq);

            // XTC device addition: time-based clean-up prompting.
            if (TIME_LOWCALIBRATION_FLAG != 0 || TIME_FULLCALIBRATION_FLAG != 0) {
                status_detection = isAppRunning(getContext());
                Slog.i(TAG, "status_detection is =" + status_detection);
            }
            long diff;
            if (TIME_LOWCALIBRATION_FLAG != 0) {
                diff = Math.abs(SAVETIME - System.currentTimeMillis()) / 1000;
                if (diff > THRESHOLDTIME) {
                    diff = 0;
                }
            } else {
                diff = 0;
            }
            Slog.i(TAG, "current time :" + SAVETIME + "  difference value :" + diff);
            if (TIME_LOWCALIBRATION_FLAG == 1) {
                SAVETIME = System.currentTimeMillis();
                TIME_LOWCALIBRATION_FLAG = 2;
            }
            if (TIME_LOWCALIBRATION_FLAG != 0 && diff >= SETTIME && status_detection
                    && !classDisable && !watchLoss && !longBatteryLife && !sleepMode
                    && !highTemp && !videoCall) {
                scheduledSpaceClean(lowspaceButtonText, true);
                SAVETIME = System.currentTimeMillis();
                Slog.i(TAG, "Guide users to clean up ");
            }
            if (TIME_FULLCALIBRATION_FLAG != 0 && status_detection
                    && !classDisable && !watchLoss && !longBatteryLife && !sleepMode
                    && !highTemp && !videoCall) {
                scheduledSpaceClean(fullspaceButtonText, true);
                SAVETIME = System.currentTimeMillis();
                Slog.i(TAG, "Force users to be reminded to clean up ");
            }

            if (oldLevel == State.LEVEL_FULL && newLevel == State.LEVEL_LOW) {
                state.level = State.LEVEL_NORMAL;
            } else {
                state.level = newLevel;
            }
            lastDiff = diff;
        }

        // Loop around to check again in future; we don't remove messages since
        // there might be an immediate request pending.
        if (!mHandler.hasMessages(MSG_CHECK)) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CHECK),
                    DEFAULT_CHECK_INTERVAL);
        }
    }

    public DeviceStorageMonitorService(Context context) {
        super(context);

        // XTC device addition: re-run the storage check when the launcher starts.
        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(
                        "com.xtc.i3launcher.action.ACTION_LAUNCHER_START")) {
                    Slog.d(TAG, "SERVICES_READY>>>>>>>>>>>>");
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                }
            }
        };
        // XTC device addition: pause the space-clean prompt in special modes.
        SpaceCleanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    return;
                }
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                if (action.equals(ACTION_CLASS_DISABLE)
                        || action.equals(ACTION_LONG_TIME_MODE)
                        || action.equals(ACTION_LOSS)
                        || action.equals(ACTION_SLEEP_MODE)
                        || action.equals(ACTION_HIGHTEMP_DISABLE)
                        || action.equals(ACTION_VIDEO_CALL)) {
                    Slog.d(TAG, "Special mode pause prompt,the action is " + action);
                    scheduledSpaceClean(null, false);
                }
            }
        };

        mHandlerThread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        mHandlerThread.start();

        mHandler = new Handler(mHandlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CHECK:
                        check();
                        return;
                }
            }
        };
    }

    private static boolean isBootImageOnDisk() {
        for (String instructionSet : InstructionSets.getAllDexCodeInstructionSets()) {
            if (!VMRuntime.isBootClassPathOnDisk(instructionSet)) {
                return false;
            }
        }
        return true;
    }

    /**
     * XTC device addition: whether the launcher's home activity is in the foreground.
     */
    public static boolean isAppRunning(Context context) {
        List<ActivityManager.RunningTaskInfo> runningTasks =
                ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE))
                        .getRunningTasks(1);
        if (runningTasks.size() <= 0) {
            return false;
        }
        Slog.i(TAG, "classname is " + runningTasks.get(0).topActivity.getClassName());
        return runningTasks.get(0).topActivity.getClassName().equals(INTERFACE_JUDGE);
    }

    /**
     * XTC device addition: listen for special-mode broadcasts that pause the prompt.
     */
    private void registerReceive() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_CLASS_DISABLE);
        filter.addAction(ACTION_LONG_TIME_MODE);
        filter.addAction(ACTION_SLEEP_MODE);
        filter.addAction(ACTION_HIGHTEMP_DISABLE);
        filter.addAction(ACTION_VIDEO_CALL);
        filter.addAction(ACTION_LOSS);
        getContext().registerReceiver(SpaceCleanReceiver, filter);
    }

    /**
     * XTC device addition: show (or dismiss) the storage clean-up dialog.
     */
    private void scheduledSpaceClean(final String text, final boolean show) {
        final StorageManager storage = getContext().getSystemService(StorageManager.class);
        final CloseDialogReceiver closeDialogReceiver = new CloseDialogReceiver(getContext());
        // Dialog UI must be created/shown on the main looper; callers may run on
        // the storage-check handler thread.
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                for (VolumeInfo vol : storage.getWritablePrivateVolumes()) {
                    if (scdialog != null) {
                        scdialog.dismiss();
                    }
                    final State state = findOrCreateState(
                            StorageManager.convert(vol.getFsUuid()));
                    SpaceCleanDialog dialog = new SpaceCleanDialog(getContext(), text) {
                        @Override
                        public void clickCallBack(View view) {
                            Slog.v(TAG, "go to clean up>>>>>>>>>>");
                            Intent intent = new Intent("android.intent.action.fullMem.xtc");
                            intent.putExtra("memory", state.lastUsableBytes);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getContext().startActivity(intent);
                            dismiss();
                        }

                        @Override
                        public void dismissCallBack(View view) {
                            Slog.v(TAG, "dismissCallBack");
                        }
                    };
                    closeDialogReceiver.dialog = dialog;
                    dialog.getWindow().addFlags(
                            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
                    dialog.getWindow().setDimAmount(100.0f);
                    if (show) {
                        dialog.show();
                        scdialog = dialog;
                    } else {
                        dialog.dismiss();
                        closeDialogReceiver.unRegister();
                    }
                }
            }
        });
    }

    @Override
    public void onStart() {
        final Context context = getContext();
        mNotifManager = context.getSystemService(NotificationManager.class);

        mCacheFileDeletedObserver = new CacheFileDeletedObserver();
        mCacheFileDeletedObserver.startWatching();

        // Ensure that the notification channel is set up
        PackageManager packageManager = context.getPackageManager();
        boolean isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK);

        if (isTv) {
            mNotifManager.createNotificationChannel(new NotificationChannel(
                    TV_NOTIFICATION_CHANNEL_ID,
                    context.getString(
                        com.android.internal.R.string.device_storage_monitor_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH));
        }

        publishBinderService(SERVICE, mRemoteService);
        publishLocalService(DeviceStorageMonitorInternal.class, mLocalService);
        // XTC device addition: register the special-mode receiver.
        registerReceive();

        // Kick off pass to examine storage state
        mHandler.removeMessages(MSG_CHECK);
        mHandler.obtainMessage(MSG_CHECK).sendToTarget();
    }

    @Override
    public void onBootPhase(int phase) {
        // XTC device addition: re-check storage when the launcher starts.
        Slog.d(TAG, "onBootPhase: " + phase);
        if (phase == SystemService.PHASE_BOOT_COMPLETED) {
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.xtc.i3launcher.action.ACTION_LAUNCHER_START");
            getContext().registerReceiverAsUser(mBroadcastReceiver, UserHandle.ALL,
                    filter, null, null);
        }
    }

    private final DeviceStorageMonitorInternal mLocalService = new DeviceStorageMonitorInternal() {
        @Override
        public void checkMemory() {
            // Kick off pass to examine storage state
            mHandler.removeMessages(MSG_CHECK);
            mHandler.obtainMessage(MSG_CHECK).sendToTarget();
        }

        @Override
        public boolean isMemoryLow() {
            return Environment.getDataDirectory().getUsableSpace() < getMemoryLowThreshold();
        }

        @Override
        public long getMemoryLowThreshold() {
            return getContext().getSystemService(StorageManager.class)
                    .getStorageLowBytes(Environment.getDataDirectory());
        }
    };

    private final Binder mRemoteService = new Binder() {
        @Override
        protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
            if (!DumpUtils.checkDumpPermission(getContext(), TAG, pw)) return;
            dumpImpl(fd, pw, args);
        }

        @Override
        public void onShellCommand(FileDescriptor in, FileDescriptor out,
                FileDescriptor err, String[] args, ShellCallback callback,
                ResultReceiver resultReceiver) {
            (new Shell()).exec(this, in, out, err, args, callback, resultReceiver);
        }
    };

    class Shell extends ShellCommand {
        @Override
        public int onCommand(String cmd) {
            return onShellCommand(this, cmd);
        }

        @Override
        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            dumpHelp(pw);
        }
    }

    static final int OPTION_FORCE_UPDATE = 1<<0;

    int parseOptions(Shell shell) {
        String opt;
        int opts = 0;
        while ((opt = shell.getNextOption()) != null) {
            if ("-f".equals(opt)) {
                opts |= OPTION_FORCE_UPDATE;
            }
        }
        return opts;
    }

    int onShellCommand(Shell shell, String cmd) {
        if (cmd == null) {
            return shell.handleDefaultCommands(cmd);
        }
        PrintWriter pw = shell.getOutPrintWriter();
        switch (cmd) {
            case "force-low": {
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission(
                        android.Manifest.permission.DEVICE_POWER, null);
                mForceLevel = State.LEVEL_LOW;
                int seq = mSeq.incrementAndGet();
                if ((opts & OPTION_FORCE_UPDATE) != 0) {
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                    pw.println(seq);
                }
            } break;
            case "force-not-low": {
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission(
                        android.Manifest.permission.DEVICE_POWER, null);
                mForceLevel = State.LEVEL_NORMAL;
                int seq = mSeq.incrementAndGet();
                if ((opts & OPTION_FORCE_UPDATE) != 0) {
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                    pw.println(seq);
                }
            } break;
            case "reset": {
                int opts = parseOptions(shell);
                getContext().enforceCallingOrSelfPermission(
                        android.Manifest.permission.DEVICE_POWER, null);
                mForceLevel = State.LEVEL_UNKNOWN;
                int seq = mSeq.incrementAndGet();
                if ((opts & OPTION_FORCE_UPDATE) != 0) {
                    mHandler.removeMessages(MSG_CHECK);
                    mHandler.obtainMessage(MSG_CHECK).sendToTarget();
                    pw.println(seq);
                }
            } break;
            default:
                return shell.handleDefaultCommands(cmd);
        }
        return 0;
    }

    static void dumpHelp(PrintWriter pw) {
        pw.println("Device storage monitor service (devicestoragemonitor) commands:");
        pw.println("  help");
        pw.println("    Print this help text.");
        pw.println("  force-low [-f]");
        pw.println("    Force storage to be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  force-not-low [-f]");
        pw.println("    Force storage to not be low, freezing storage state.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
        pw.println("  reset [-f]");
        pw.println("    Unfreeze storage state, returning to current real values.");
        pw.println("    -f: force a storage change broadcast be sent, prints new sequence.");
    }

    void dumpImpl(FileDescriptor fd, PrintWriter _pw, String[] args) {
        final IndentingPrintWriter pw = new IndentingPrintWriter(_pw, "  ");
        if (args == null || args.length == 0 || "-a".equals(args[0])) {
            pw.println("Known volumes:");
            pw.increaseIndent();
            for (int i = 0; i < mStates.size(); i++) {
                final UUID uuid = mStates.keyAt(i);
                final State state = mStates.valueAt(i);
                if (StorageManager.UUID_DEFAULT.equals(uuid)) {
                    pw.println("Default:");
                } else {
                    pw.println(uuid + ":");
                }
                pw.increaseIndent();
                pw.printPair("level", State.levelToString(state.level));
                pw.printPair("lastUsableBytes", state.lastUsableBytes);
                pw.println();
                pw.decreaseIndent();
            }
            pw.decreaseIndent();
            pw.println();

            pw.printPair("mSeq", mSeq.get());
            pw.printPair("mForceState", State.levelToString(mForceLevel));
            pw.println();
            pw.println();

        } else {
            Shell shell = new Shell();
            shell.exec(mRemoteService, null, fd, null, args, null, new ResultReceiver(null));
        }
    }

    private void updateNotifications(VolumeInfo vol, int oldLevel, int newLevel, State state) {
        final Context context = getContext();
        final UUID uuid = StorageManager.convert(vol.getFsUuid());

        if (State.isEntering(State.LEVEL_FULL, oldLevel, newLevel)) {
            Slog.i(TAG, "Sending memory full notification");
            if (TIME_FULLCALIBRATION_FLAG == 0) {
                scheduledSpaceClean(fullspaceButtonText, true);
                TIME_FULLCALIBRATION_FLAG = 1;
                TIME_LOWCALIBRATION_FLAG = 0;
            }
            showStorageNotification(context, uuid, state);
            return;
        }
        if (State.isEntering(State.LEVEL_LOW, oldLevel, newLevel)) {
            Slog.i(TAG, "Running low on memory. Sending notification");
            if (TIME_LOWCALIBRATION_FLAG == 0) {
                scheduledSpaceClean(lowspaceButtonText, true);
                TIME_LOWCALIBRATION_FLAG = 1;
                TIME_FULLCALIBRATION_FLAG = 0;
            }
            showStorageNotification(context, uuid, state);
            return;
        }
        if (State.isLeaving(State.LEVEL_LOW, oldLevel, newLevel)
                || State.isLeaving(State.LEVEL_FULL, oldLevel, newLevel)) {
            Slog.i(TAG, "cancel storage monitor notification");
            mNotifManager.cancelAsUser(uuid.toString(), SystemMessage.NOTE_LOW_STORAGE,
                    UserHandle.ALL);
        }
    }

    private void showStorageNotification(Context context, UUID uuid, State state) {
        Intent lowMemIntent = new Intent(StorageManager.ACTION_MANAGE_STORAGE);
        lowMemIntent.putExtra(StorageManager.EXTRA_UUID, uuid);
        lowMemIntent.putExtra("memory", state.lastUsableBytes);
        lowMemIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        final CharSequence title = context.getText(
                com.android.internal.R.string.low_internal_storage_view_title);

        final CharSequence details;
        if (StorageManager.UUID_DEFAULT.equals(uuid)) {
            details = context.getText(isBootImageOnDisk()
                    ? com.android.internal.R.string.low_internal_storage_view_text
                    : com.android.internal.R.string.low_internal_storage_view_text_no_boot);
        } else {
            details = context.getText(
                    com.android.internal.R.string.low_internal_storage_view_text);
        }

        PendingIntent intent = PendingIntent.getActivityAsUser(context, 0, lowMemIntent, 0,
                null, UserHandle.CURRENT);
        Notification notification =
                new Notification.Builder(context, SystemNotificationChannels.ALERTS)
                        .setSmallIcon(com.android.internal.R.drawable.stat_notify_disk_full)
                        .setTicker(title)
                        .setColor(context.getColor(
                            com.android.internal.R.color.system_notification_accent_color))
                        .setContentTitle(title)
                        .setContentText(details)
                        .setContentIntent(intent)
                        .setStyle(new Notification.BigTextStyle()
                              .bigText(details))
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setCategory(Notification.CATEGORY_SYSTEM)
                        .extend(new Notification.TvExtender()
                                .setChannelId(TV_NOTIFICATION_CHANNEL_ID))
                        .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        mNotifManager.notifyAsUser(uuid.toString(), SystemMessage.NOTE_LOW_STORAGE,
                notification, UserHandle.ALL);
    }

    private void updateBroadcasts(VolumeInfo vol, int oldLevel, int newLevel, int seq) {
        if (!Objects.equals(StorageManager.UUID_PRIVATE_INTERNAL, vol.getFsUuid())) {
            // We don't currently send broadcasts for secondary volumes
            return;
        }

        final Intent lowIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_LOW)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                        | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS)
                .putExtra(EXTRA_SEQUENCE, seq);
        final Intent notLowIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_OK)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT
                        | Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND
                        | Intent.FLAG_RECEIVER_VISIBLE_TO_INSTANT_APPS)
                .putExtra(EXTRA_SEQUENCE, seq);

        if (State.isEntering(State.LEVEL_LOW, oldLevel, newLevel)) {
            getContext().sendStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
        } else if (State.isLeaving(State.LEVEL_LOW, oldLevel, newLevel)) {
            getContext().removeStickyBroadcastAsUser(lowIntent, UserHandle.ALL);
            getContext().sendBroadcastAsUser(notLowIntent, UserHandle.ALL);
        }

        final Intent fullIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_FULL)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT)
                .putExtra(EXTRA_SEQUENCE, seq);
        final Intent notFullIntent = new Intent(Intent.ACTION_DEVICE_STORAGE_NOT_FULL)
                .addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY_BEFORE_BOOT)
                .putExtra(EXTRA_SEQUENCE, seq);

        if (State.isEntering(State.LEVEL_FULL, oldLevel, newLevel)) {
            getContext().sendStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
        } else if (State.isLeaving(State.LEVEL_FULL, oldLevel, newLevel)) {
            getContext().removeStickyBroadcastAsUser(fullIntent, UserHandle.ALL);
            getContext().sendBroadcastAsUser(notFullIntent, UserHandle.ALL);
        }
    }

    /**
     * XTC device addition: closes a shown dialog when the system dialogs are closed.
     */
    private static class CloseDialogReceiver extends BroadcastReceiver
            implements DialogInterface.OnDismissListener {
        public Dialog dialog;
        private Context mContext;

        CloseDialogReceiver(Context context) {
            mContext = context;
            context.registerReceiver(this,
                    new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            mContext.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            dialog.cancel();
        }

        public void unRegister() {
            mContext.unregisterReceiver(this);
        }
    }

    private static class CacheFileDeletedObserver extends FileObserver {
        public CacheFileDeletedObserver() {
            super(Environment.getDownloadCacheDirectory().getAbsolutePath(), FileObserver.DELETE);
        }

        @Override
        public void onEvent(int event, String path) {
            EventLogTags.writeCacheFileDeleted(path);
        }
    }
}
