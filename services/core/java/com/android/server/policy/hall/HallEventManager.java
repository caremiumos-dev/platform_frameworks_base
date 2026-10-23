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

package com.android.server.policy.hall;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.BoostFramework;
import android.util.Log;
import android.view.WindowManagerPolicy;

import com.android.server.policy.PhoneWindowManager;
import com.android.server.xgseserver.xss.HttpConfigOptions;
import com.xtc.behavior.XtcBehavior;
import com.xtc.moduleswitch.FunSwitchUtil;
import com.xtc.moduleswitch.ModuleSwitch;
import com.xtc.utils.HallUtil;
import com.xtc.widget.MenuHolder;
import com.xtc.widget.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import static com.android.server.policy.hall.HallAction.MSG_ABANDON_FOCUS;
import static com.android.server.policy.hall.HallAction.MSG_BOOT_COMPLETED;
import static com.android.server.policy.hall.HallAction.MSG_FUNCTION_SWITCH_CHANGED;
import static com.android.server.policy.hall.HallAction.MSG_GOTO_SLEEP;
import static com.android.server.policy.hall.HallAction.MSG_HALL_EVENT;
import static com.android.server.policy.hall.HallAction.MSG_HIDE_OPEN_ANIM;
import static com.android.server.policy.hall.HallAction.MSG_HIDE_TIP;
import static com.android.server.policy.hall.HallAction.MSG_HIDE_VIEW;
import static com.android.server.policy.hall.HallAction.MSG_HIDE_VIEW_FORCE;
import static com.android.server.policy.hall.HallAction.MSG_INIT;
import static com.android.server.policy.hall.HallAction.MSG_LOAD_ANIM;
import static com.android.server.policy.hall.HallAction.MSG_MODULE_SWITCH_CHANGED;
import static com.android.server.policy.hall.HallAction.MSG_PLAY_ANIM;
import static com.android.server.policy.hall.HallAction.MSG_PLAY_SOUND;
import static com.android.server.policy.hall.HallAction.MSG_RELOAD_MENU;
import static com.android.server.policy.hall.HallAction.MSG_SEND_BEHAVIOR;
import static com.android.server.policy.hall.HallAction.MSG_SHOW_CLOSE_VIEW;
import static com.android.server.policy.hall.HallAction.MSG_SHOW_TIP;
import static com.android.server.policy.hall.HallAction.MSG_SHOW_VIEW;
import static com.android.server.policy.hall.HallAction.MSG_STOP_SOUND;
import static com.android.server.policy.hall.HallAction.MSG_WAKE_UP;

/**
 * Central controller for the hall (flip cover) events. It receives sensor
 * events from {@code PhoneWindowManager}, drives the animation/sound/function
 * controllers and broadcasts or launches the configured function.
 *
 * @hide
 */
public class HallEventManager {
    private static final String TAG = "SysHallEventManager";

    private static final long BOOT_COMPLETED_DELAY = 5000L;
    private static final String ACTION_POWER_CONNECTED =
            "android.intent.action.ACTION_POWER_CONNECTED";
    private static final String TAG_PACKAGE = "package";

    private static BoostFramework mPerf = null;
    private static int mPerfLockDuration = 6000;
    private static int[] mPerfLockParam = HallUtil.getPerfLockParam();

    private Context mContext;
    private HallAnimation mHallAnimation;
    private HallFunction mHallFunction;
    private volatile boolean mHallRecentlyClosed;
    private HallSound mHallSound;
    private long mLastDisableTime;
    private volatile long mLastFlipTime;
    private long mLastOpenTime;
    private PhoneWindowManager mPolicy;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private WorkHandler mWorkHandler;
    private HandlerThread mWorkThread;
    private boolean mBootCompleted = false;
    private volatile boolean mNeedStartApp = false;
    private int FLIP_COUNT = 10;
    private int FLIP_DURATION = 60000;
    private int DISABLE_DURATION = 180000;
    private boolean mRestrictFlip = true;
    private int mOpenCount = 0;

    private final BroadcastReceiver mStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive:" + intent.getAction());
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                mWorkHandler.sendEmptyMessageDelayed(MSG_BOOT_COMPLETED, BOOT_COMPLETED_DELAY);
            } else if (HallUtil.SWITCH_INIT_COMPLETE.equals(intent.getAction())) {
                mWorkHandler.sendEmptyMessage(MSG_MODULE_SWITCH_CHANGED);
            } else {
                Message.obtain(mHandler, MSG_HIDE_VIEW_FORCE, Boolean.TRUE).sendToTarget();
                mHandler.sendEmptyMessage(MSG_HIDE_TIP);
            }
        }
    };

    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            mWorkHandler.removeMessages(MSG_RELOAD_MENU);
            mWorkHandler.sendMessageDelayed(
                    Message.obtain(mWorkHandler, MSG_RELOAD_MENU,
                            intent.getData().getSchemeSpecificPart()),
                    1000L);
        }
    };

    private final ContentObserver mFunctionObserver = new ContentObserver(null) {
        @Override
        public boolean deliverSelfNotifications() {
            return false;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            mWorkHandler.sendEmptyMessage(MSG_FUNCTION_SWITCH_CHANGED);
        }
    };

    private final HallHandler mHandler = new HallHandler(Looper.getMainLooper());

    private class HallHandler extends Handler {
        HallHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_INIT:
                    mHallAnimation.initView();
                    break;
                case MSG_SHOW_VIEW:
                    mHallAnimation.setTitle(mHallFunction.getItem().menuTitle);
                    mHallAnimation.showView((HallAnimationView) message.obj);
                    break;
                case MSG_HIDE_VIEW:
                    mHallAnimation.hideView((HallAnimationView) message.obj);
                    break;
                case MSG_PLAY_ANIM:
                    ((HallAnimationView) message.obj).playAnimation();
                    break;
                case MSG_LOAD_ANIM:
                    mHallAnimation.setHallAnimation((String) message.obj);
                    break;
                case MSG_SHOW_CLOSE_VIEW:
                    mHallAnimation.showCloseView((HallAnimationView) message.obj);
                    break;
                case MSG_HIDE_VIEW_FORCE:
                    mHallAnimation.hideViewForce((Boolean) message.obj);
                    break;
                case MSG_SHOW_TIP:
                    mHallSound.stopSound();
                    mHallAnimation.showTipView(DISABLE_DURATION);
                    break;
                case MSG_HIDE_TIP:
                    mHallAnimation.hideTipView();
                    break;
                default:
                    Log.w(TAG, "invalid msg:" + message.what);
                    break;
            }
        }
    }

    private class WorkHandler extends Handler {
        WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MSG_GOTO_SLEEP:
                    if (!mHallAnimation.tipShow()) {
                        mPowerManager.goToSleep(SystemClock.uptimeMillis(), 4, 0);
                    }
                    break;
                case MSG_WAKE_UP:
                    Log.d(TAG, "wakeup from hall event");
                    mPowerManager.wakeUp(SystemClock.uptimeMillis(), "HallEvent");
                    if (mWakeLock.isHeld()) {
                        mWakeLock.release();
                    }
                    break;
                case MSG_ABANDON_FOCUS:
                    mHallSound.abandonAudioFocus();
                    break;
                case MSG_STOP_SOUND:
                    Message.obtain(mHandler, MSG_HIDE_VIEW_FORCE, Boolean.TRUE).sendToTarget();
                    mHallSound.stopOpenSound();
                    break;
                case MSG_BOOT_COMPLETED:
                    handleBootCompleted();
                    android.os.Process.setThreadPriority(-2);
                    break;
                case MSG_MODULE_SWITCH_CHANGED:
                    checkModuleSwitch();
                    break;
                case MSG_FUNCTION_SWITCH_CHANGED:
                    mHallFunction.checkFunctionSwitch();
                    break;
                case MSG_RELOAD_MENU:
                    mHallFunction.reloadFunctionMenu((String) message.obj);
                    break;
                case MSG_HALL_EVENT:
                    boolean open = message.arg1 == 289;
                    if (!handleHallSensorEvent(open,
                            (WindowManagerPolicy.WindowState) message.obj)) {
                        HallUtil.sendHallKeyEventAsync(open);
                        if (open) {
                            HallUtil.enablePalmSleep();
                        }
                    }
                    break;
                case MSG_PLAY_SOUND:
                    if (System.currentTimeMillis() - mLastDisableTime > DISABLE_DURATION) {
                        mHallSound.playFlipSound(((Boolean) message.obj).booleanValue());
                    }
                    break;
                case MSG_SEND_BEHAVIOR:
                    sendXtcBehavior(((Boolean) message.obj).booleanValue());
                    break;
                case MSG_HIDE_OPEN_ANIM:
                    String packageName = (String) message.obj;
                    if (packageName != null && MenuHolder.instance().contains(packageName)
                            && !packageName.startsWith("com.xtc")) {
                        Log.d(TAG, "hideOpenAnimation:" + packageName);
                        Message.obtain(mHandler, MSG_HIDE_VIEW,
                                mHallAnimation.getView(true)).sendToTarget();
                    }
                    break;
                default:
                    Log.w(TAG, "invalid msg:" + message.what);
                    break;
            }
        }
    }

    public HallEventManager(Context context, PhoneWindowManager phoneWindowManager) {
        this.mContext = context;
        this.mPolicy = phoneWindowManager;
        this.mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.mWakeLock = this.mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        this.mHallAnimation = new HallAnimation(this.mContext);
        this.mHallSound = new HallSound(this.mContext);
        this.mHallFunction = new HallFunction(this.mContext);
        HallDeviceManager.instance().init(this.mContext);
        registerReceiver();
        initHandlerThread();
        HallUtil.setHall(HallUtil.kernelHallOpen());
        Log.d(TAG, "hall status in init:" + HallUtil.hallOpen());
        this.mHandler.sendEmptyMessage(MSG_INIT);
        mPerf = new BoostFramework();
        this.mHallAnimation.setHandler(this.mHandler, this.mWorkHandler);
        this.mHallSound.setHandler(this.mHandler, this.mWorkHandler);
        this.mHallFunction.setHandler(this.mHandler, this.mWorkHandler);
    }

    private static void acquireHallPerfLock() {
        if (mPerf == null) {
            mPerf = new BoostFramework();
        }
        mPerf.perfLockAcquire(mPerfLockDuration, mPerfLockParam);
    }

    private boolean animationDisabled() {
        return !this.mHallFunction.getItem().enableAnim;
    }

    private void checkModuleSwitch() {
        this.mHallFunction.checkModuleSwitch();
        Log.d(TAG, "start check flip restrict status");
        this.mRestrictFlip = ModuleSwitch.queryModuleSwitch(this.mContext, 2207, true);
        setRestrictFlipFromJson(ModuleSwitch.queryModuleSwitchExtraByInt(this.mContext, 2207,
                HallUtil.FLIP_LIMIT_JSON));
    }

    private void checkStartCount(Intent intent) {
        String key = "hall-" + intent.getPackage() + "-" + this.mHallFunction.getIndex();
        int count = Settings.System.getInt(this.mContext.getContentResolver(), key, 0);
        if (count >= MenuHolder.instance().animShowCount()) {
            this.mHallAnimation.getView(true).disableAnimator(true);
            intent.putExtra("xtc_show_gesture_tip", false);
        } else {
            Settings.System.putInt(this.mContext.getContentResolver(), key, count + 1);
            intent.putExtra("xtc_show_gesture_tip", true);
            this.mHallAnimation.getView(true).disableAnimator(false);
        }
    }

    private String getTopActivity() {
        List<ActivityManager.RunningTaskInfo> runningTasks =
                ((ActivityManager) this.mContext.getSystemService(Context.ACTIVITY_SERVICE))
                        .getRunningTasks(1);
        if (runningTasks.isEmpty() || runningTasks.get(0) == null
                || runningTasks.get(0).topActivity == null) {
            return null;
        }
        return runningTasks.get(0).topActivity.getClassName();
    }

    private void handleBootCompleted() {
        this.mHallSound.init();
        this.mHallFunction.init();
        this.mHallAnimation.init();
        checkModuleSwitch();
        HallUtil.setHall(HallUtil.kernelHallOpen());
        Log.d(TAG, "hall status in bootCompleted:" + HallUtil.hallOpen());
        ContentResolver resolver = this.mContext.getContentResolver();
        resolver.registerContentObserver(FunSwitchUtil.FUN_URI, true, this.mFunctionObserver);
        resolver.registerContentObserver(FunSwitchUtil.EXTRA_URI, true, this.mFunctionObserver);
        if (HallUtil.hallOpen()) {
            HallUtil.enablePalmSleep();
        }
        this.mBootCompleted = true;
    }

    private void handleCloseAnimation(String value) {
        HallAnimationView view = this.mHallAnimation.getView(false);
        if ("0".equals(value)) {
            Message.obtain(this.mHandler, MSG_HIDE_VIEW, view).sendToTarget();
            return;
        }
        if (!"1".equals(value)) {
            Message.obtain(this.mHandler, MSG_SHOW_CLOSE_VIEW, view).sendToTarget();
            return;
        }
        if (this.mPowerManager.isInteractive()) {
            this.mHallRecentlyClosed = true;
            acquireHallPerfLock();
            this.mLastFlipTime = SystemClock.uptimeMillis();
            Message.obtain(this.mWorkHandler, MSG_PLAY_SOUND, Boolean.FALSE).sendToTarget();
            Message.obtain(this.mHandler, MSG_SHOW_VIEW, view).sendToTarget();
        }
    }

    private boolean handleHallSensorEvent(boolean open,
            WindowManagerPolicy.WindowState windowState) {
        HallUtil.setHall(open);
        Log.d(TAG, "handle hall event open:" + open + " status:" + HallUtil.hallOpen()
                + " time:" + SystemClock.uptimeMillis());
        if (restrictFlip(open, windowState)) {
            return false;
        }
        if (SystemClock.uptimeMillis() - this.mLastFlipTime < 1000) {
            Log.d(TAG, "flip too fast, ignore event");
            return false;
        }
        Message.obtain(this.mWorkHandler, MSG_SEND_BEHAVIOR, Boolean.valueOf(open)).sendToTarget();
        if (!open || !needOpen(windowState)) {
            Log.d(TAG, "event ignore open:" + open + " topActivity:" + getTopActivity());
            return false;
        }
        this.mWakeLock.acquire(BOOT_COMPLETED_DELAY);
        acquireHallPerfLock();
        this.mLastFlipTime = SystemClock.uptimeMillis();
        if (animationDisabled()) {
            sendHallEventBroadcast();
            return true;
        }
        this.mWorkHandler.removeMessages(MSG_STOP_SOUND);
        Message.obtain(this.mHandler, MSG_SHOW_VIEW,
                this.mHallAnimation.getView(true)).sendToTarget();
        if (this.mPowerManager.isInteractive() || !isProcessRunning()) {
            startApplication();
        } else {
            this.mNeedStartApp = true;
        }
        return true;
    }

    private boolean ignoreFlipEvent(WindowManagerPolicy.WindowState windowState) {
        boolean ignore = true;
        if (windowState == null || windowState.getAttrs() == null) {
            ignore = false;
        } else if ((windowState.getAttrs().flags & 1) == 0
                && (windowState.getAttrs().flagsEx & 2) == 0) {
            ignore = false;
        }
        if (ignore) {
            Log.d(TAG, "ignore event due to flag pkg:" + windowState.getOwningPackage());
        }
        return ignore;
    }

    private void initHandlerThread() {
        this.mWorkThread = new HandlerThread("HallWorkThread");
        this.mWorkThread.start();
        this.mWorkHandler = new WorkHandler(this.mWorkThread.getLooper());
    }

    private boolean isProcessRunning() {
        try {
            List runningAppProcesses = ActivityManagerNative.getDefault().getRunningAppProcesses();
            MenuItem item = this.mHallFunction.getItem();
            Iterator it = runningAppProcesses.iterator();
            while (it.hasNext()) {
                if (((ActivityManager.RunningAppProcessInfo) it.next()).processName
                        .equals(item.pkgName)) {
                    return true;
                }
            }
            return false;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean needDispatchEvent(WindowManagerPolicy.WindowState windowState) {
        if (!this.mBootCompleted || this.mHallFunction.invalid() || HallUtil.specialMode()
                || ignoreFlipEvent(windowState)) {
            return false;
        }
        boolean lockShow = HallUtil.lockShow();
        boolean isLauncher = windowState != null
                && HttpConfigOptions.PackageName.PACKAGE_NAME_LAUNCHER
                        .equals(windowState.getOwningPackage());
        if (lockShow && isLauncher) {
            windowState = this.mPolicy.getWindowBehindFocused();
        }
        return !ignoreFlipEvent(windowState);
    }

    private boolean needOpen(WindowManagerPolicy.WindowState windowState) {
        if ("com.xtc.i3launcher.module.home.view.activity.HomeActivity".equals(getTopActivity())
                || !this.mPowerManager.isInteractive()) {
            return true;
        }
        return HallUtil.lockShow();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.xtc.videochat.start");
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction("android.intent.action.PHONE_STATE");
        filter.addAction(HallUtil.ALARM_START);
        filter.addAction("com.xtc.workplan.ring.start");
        filter.addAction(HallUtil.SWITCH_INIT_COMPLETE);
        filter.addAction(ACTION_POWER_CONNECTED);
        this.mContext.registerReceiver(this.mStateReceiver, filter);
        IntentFilter updateFilter = new IntentFilter();
        updateFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        updateFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        updateFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        updateFilter.addDataScheme(TAG_PACKAGE);
        this.mContext.registerReceiver(this.mUpdateReceiver, updateFilter);
    }

    private boolean restrictFlip(boolean open, WindowManagerPolicy.WindowState windowState) {
        if (!open) {
            return false;
        }
        if (!needDispatchEvent(windowState)) {
            HallUtil.printStatusLog(this.mBootCompleted, this.mHallFunction.getItem().key);
            return true;
        }
        if (!this.mRestrictFlip || HallUtil.demoMode()) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - this.mLastDisableTime < this.DISABLE_DURATION) {
            Log.d(TAG, "flip disabled temporary");
            return true;
        }
        this.mLastDisableTime = 0L;
        if (now - this.mLastOpenTime < this.FLIP_DURATION) {
            this.mOpenCount++;
        } else {
            this.mLastOpenTime = now;
            this.mOpenCount = 1;
        }
        if (this.mOpenCount <= this.FLIP_COUNT - 1) {
            if (this.mHallAnimation.tipShow()) {
                this.mHandler.sendEmptyMessage(MSG_HIDE_TIP);
            }
            return false;
        }
        this.mLastDisableTime = System.currentTimeMillis();
        Log.d(TAG, "flip too fast exceed the limit");
        this.mOpenCount = 0;
        this.mLastOpenTime = 0L;
        this.mHallSound.stopSound();
        this.mHandler.sendEmptyMessage(MSG_SHOW_TIP);
        sendFlipRestrictBehavior();
        return true;
    }

    private void sendFlipRestrictBehavior() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        map.put("flipCount", String.valueOf(this.FLIP_COUNT));
        map.put("flipDuration", this.FLIP_DURATION + " ms");
        map.put("disableDuration", this.DISABLE_DURATION + " ms");
        new XtcBehavior.Builder(this.mContext, "camera_control_flip_restrict_function", map)
                .build().sendData();
    }

    private void sendHallEventBroadcast() {
        this.mContext.sendBroadcastAsUser(this.mHallFunction.getItem().getIntent(),
                UserHandle.CURRENT_OR_SELF);
        Log.d(TAG, "send broadcast for hall event");
    }

    private void sendXtcBehavior(boolean open) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("timeStamp", String.valueOf(System.currentTimeMillis()));
        map.put("hallStatus", open ? "open" : "close");
        map.put("currentApp", getTopActivity());
        map.put("screenOn", this.mPowerManager.isInteractive() ? "on" : "off");
        map.put("currentFun", "Fun:" + this.mHallFunction.getItem().menuTitle);
        new XtcBehavior.Builder(this.mContext, "camera_control_hall_event_function", map)
                .build().sendData();
    }

    private void setRestrictFlipFromJson(String json) {
        if (TextUtils.isEmpty(json)) {
            Log.w(TAG, "json str is empty");
            return;
        }
        try {
            JSONObject object = new JSONObject(json);
            this.FLIP_COUNT = object.getInt("flipCount");
            this.FLIP_DURATION = object.getInt("flipDuration") * 1000;
            this.DISABLE_DURATION = object.getInt("disableDuration") * 1000;
            Log.d(TAG, "flip restrict update enable:" + this.mRestrictFlip
                    + " count:" + this.FLIP_COUNT + " d1:" + this.FLIP_DURATION
                    + " d2:" + this.DISABLE_DURATION);
        } catch (JSONException e) {
            Log.d(TAG, "parse json failed", e);
        }
    }

    private void startApplication() {
        MenuItem item = this.mHallFunction.getItem();
        Intent intent = item.getIntent();
        intent.addFlags(32768);
        intent.putExtra("SHOW_WHEN_LOCKED", true);
        intent.putExtra("xtc_hall_open", true);
        if (item.initMenu) {
            checkStartCount(intent);
            intent.putExtra("xtc_show_menu", true);
        }
        if (this.mContext.getPackageManager().resolveActivity(intent, 65536) == null) {
            Log.e(TAG, "no activity found:" + intent.toString());
            return;
        }
        try {
            Log.d(TAG, "start activity from hall event");
            this.mContext.startActivityAsUser(intent, UserHandle.CURRENT_OR_SELF);
        } catch (Exception e) {
            Log.e(TAG, "start activity failed", e);
        }
    }

    private void unlockScreen(boolean hide) {
        if (HallUtil.secureLock()) {
            return;
        }
        if (hide) {
            this.mPolicy.getKeyguardListener().hideKeyguard("HALL");
        } else {
            this.mPolicy.unlockScreenWhenWakeUp("HALL");
        }
    }

    public boolean bootCompleted() {
        return this.mBootCompleted;
    }

    public void finishGoToSleep() {
        this.mHallRecentlyClosed = false;
        Message.obtain(this.mHandler, MSG_HIDE_VIEW_FORCE, Boolean.FALSE).sendToTarget();
        if (this.mHallAnimation.tipShow()) {
            this.mHandler.sendEmptyMessage(MSG_HIDE_TIP);
        }
    }

    public void finishWakeUp() {
        if (this.mNeedStartApp) {
            this.mNeedStartApp = false;
            startApplication();
        }
    }

    public boolean hallRecentlyClosed() {
        boolean recentlyClosed = this.mHallRecentlyClosed;
        if (this.mHallRecentlyClosed) {
            this.mHallRecentlyClosed = false;
        }
        return recentlyClosed;
    }

    public void handleHallSensorEvent(int keyCode, WindowManagerPolicy.WindowState windowState) {
        this.mWorkHandler.removeMessages(MSG_HALL_EVENT);
        Message.obtain(this.mWorkHandler, MSG_HALL_EVENT, keyCode, 0, windowState).sendToTarget();
    }

    public void hideOpenAnimation(String packageName) {
        Message.obtain(this.mWorkHandler, MSG_HIDE_OPEN_ANIM, packageName).sendToTarget();
    }

    public void onPowerLongPress() {
        onPowerPressed();
    }

    public void onPowerPressed() {
        Message.obtain(this.mHandler, MSG_HIDE_VIEW_FORCE, Boolean.TRUE).sendToTarget();
        if (this.mHallAnimation.tipShow()) {
            this.mHandler.sendEmptyMessage(MSG_HIDE_TIP);
        }
        this.mHallSound.stopSound();
    }

    public void requestHallEvent(String type, String value) {
        Log.d(TAG, "hall event:" + type + " value:" + value);
        if (type.equals(HallUtil.OPEN_ANIM)) {
            HallAnimationView view = this.mHallAnimation.getView(true);
            if ("0".equals(value)) {
                Message.obtain(this.mHandler, MSG_HIDE_VIEW, view).sendToTarget();
                return;
            } else {
                Message.obtain(this.mHandler, MSG_SHOW_VIEW, view).sendToTarget();
                return;
            }
        }
        if (type.equals(HallUtil.CLOSE_ANIM)) {
            handleCloseAnimation(value);
            return;
        }
        if (type.equals(HallUtil.FUN_PROP)) {
            this.mHallFunction.setHallFunction(value);
            return;
        }
        if (type.equals(HallUtil.SOUND_PROP)) {
            this.mHallSound.setHallSound(value);
            return;
        }
        if (type.equals(HallUtil.ANIM_PROP)) {
            Message.obtain(this.mHandler, MSG_LOAD_ANIM, value).sendToTarget();
        } else if (!type.equals(HallUtil.MENU_ORDER)) {
            Log.w(TAG, "no tag matched, ignore");
        } else {
            Log.d(TAG, "last save order:" + value + " res:"
                    + Settings.System.putString(this.mContext.getContentResolver(), type, value));
        }
    }

    public void startGoToSleep() {
        this.mWorkHandler.sendEmptyMessageDelayed(MSG_STOP_SOUND, 500L);
    }
}