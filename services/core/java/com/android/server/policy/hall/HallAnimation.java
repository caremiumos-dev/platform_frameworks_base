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

import android.animation.Animator;
import android.content.Context;
import android.graphics.Point;
import android.os.Message;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.xtc.utils.HallUtil;

import java.io.File;

/**
 * Drives the hall open/close animation windows.
 *
 * @hide
 */
public class HallAnimation extends HallAction {
    private static final String TAG = "Sys" + HallAnimation.class.getSimpleName();

    private int animationIndex;
    private String[] animationList;
    private String animationPath;
    private HallAnimationView closeView;
    private Animator.AnimatorListener mCloseListener;
    private HallAnimationView.OnDrawListener mOnDrawListener;
    private Animator.AnimatorListener mOpenListener;
    private HallAnimationView openView;
    private PowerManager powerManager;
    private TelecomManager telecomManager;
    private View tipLayout;
    private long tipShowTime;
    private WindowManager windowManager;
    private WindowManager.LayoutParams windowParams;

    public HallAnimation(Context context) {
        super(context);
        this.mOnDrawListener = new HallAnimationView.OnDrawListener() {
            @Override
            public void onFirstDraw() {
                workHandler.sendEmptyMessage(MSG_WAKE_UP);
                Message.obtain(workHandler, MSG_PLAY_SOUND, Boolean.TRUE).sendToTarget();
            }
        };
        this.mOpenListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (openView.isShow()) {
                    Log.d(TAG, "remove open view when animation end");
                    openView.setShow(false);
                    openView.cancelAnimation();
                    windowManager.removeView(openView);
                }
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }
        };
        this.mCloseListener = new Animator.AnimatorListener() {
            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                if (closeView.isShow()) {
                    if (!closeView.removeWhenEnd() && powerManager.isInteractive() && tipLayout == null) {
                        Log.d(TAG, "remove close view after go to sleep");
                        workHandler.sendEmptyMessage(MSG_GOTO_SLEEP);
                    } else {
                        Log.d(TAG, "remove close view on animation end");
                        closeView.setShow(false);
                        closeView.setRemoveWhenEnd(false);
                        windowManager.removeView(closeView);
                    }
                }
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }

            @Override
            public void onAnimationStart(Animator animator) {
            }
        };
        this.powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        this.telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
    }

    private void initTipView(int duration) {
        this.tipLayout = LayoutInflater.from(this.context)
                .inflate(com.android.internal.R.layout.flip_restrict_tip_layout, null, false);
        ((TextView) this.tipLayout.findViewById(com.android.internal.R.id.tv_flip_msg)).setText(String.format(
                this.context.getResources().getString(
                        com.android.internal.R.string.flip_restrict_msg),
                (duration / 1000) / 60));
        ((TextView) this.tipLayout.findViewById(com.android.internal.R.id.tv_flip_ok))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        powerManager.goToSleep(SystemClock.uptimeMillis(), 4, 0);
                    }
                });
    }

    private boolean isExtraAnimation() {
        if (TextUtils.isEmpty(this.animationPath)) {
            return false;
        }
        return !this.animationPath.equals(this.animationList[this.animationIndex]);
    }

    private boolean notShow(HallAnimationView view) {
        return view.isShow() || this.tipLayout != null;
    }

    private void setAnimationFromPath(String path) {
        Log.d(TAG, "set animation:" + path);
        if (!fileCanRead(path)) {
            Log.w(TAG, "can not load animation:" + path);
            return;
        }
        this.animationPath = path;
        putSetting(HallUtil.ANIM_PROP, this.animationPath);
        this.openView.loadAnimation(path);
        this.closeView.loadAnimation(path);
    }

    private void setHallAnimation(int index) {
        if (validIndex(index)) {
            this.animationIndex = index;
            setAnimationFromPath(this.animationList[this.animationIndex]);
        }
    }

    private boolean validIndex(int index) {
        return index >= 0 && index < this.animationList.length;
    }

    public HallAnimationView getView(boolean open) {
        return open ? this.openView : this.closeView;
    }

    public void hideTipView() {
        if (this.tipLayout != null) {
            if (SystemClock.uptimeMillis() - this.tipShowTime < 2000) {
                Log.d(TAG, "do not remove tip because display time is too short");
                return;
            }
            Log.d(TAG, "remove tip layout");
            this.windowManager.removeView(this.tipLayout);
            this.windowParams.flags |= 128;
            this.tipLayout = null;
        }
    }

    public void hideView(HallAnimationView view) {
        if (view.isShow()) {
            if (view == this.openView) {
                Log.d(TAG, "open view:requestHideAnimation");
                view.requestHideAnimation();
            } else {
                view.setShow(false);
                view.cancelAnimation();
                this.windowManager.removeView(view);
                Log.d(TAG, "remove animation view openView:" + (view == this.openView));
            }
        }
    }

    public void hideViewForce(Boolean open) {
        HallAnimationView view = getView(open.booleanValue());
        if (view.isShow()) {
            view.setShow(false);
            this.windowManager.removeView(view);
            view.cancelAnimation();
            Log.d(TAG, "force remove view openView:" + open);
        }
    }

    @Override
    public void init() {
        int defaultAnimIndex = HallDeviceManager.instance().getDefaultAnimIndex();
        this.animationPath = getSetting(HallUtil.ANIM_PROP);
        this.animationIndex = SystemProperties.getInt(HallUtil.ANIM_PROP, defaultAnimIndex);
        this.animationList = HallDeviceManager.instance().getAnimationList(this.context);
        if (!validIndex(this.animationIndex)) {
            this.animationIndex = defaultAnimIndex;
        }
        if (this.animationPath != null) {
            File file = new File(this.animationPath);
            if (file.exists() && file.isFile()) {
                Log.d(TAG, "old path:" + this.animationPath);
                this.animationPath = this.animationList[this.animationIndex];
                putSetting(HallUtil.ANIM_PROP, this.animationPath);
                Log.d(TAG, "new path:" + this.animationPath);
            }
        }
        String path = isExtraAnimation() ? this.animationPath : this.animationList[this.animationIndex];
        Log.d(TAG, "defVal:" + defaultAnimIndex + " index:" + this.animationIndex
                + " path:" + this.animationPath + " res:" + path);
        Message.obtain(this.uiHandler, MSG_LOAD_ANIM, path).sendToTarget();
    }

    public void initView() {
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.windowParams = new WindowManager.LayoutParams();
        if (this.windowManager.getDefaultDisplay() != null) {
            Point point = new Point();
            this.windowManager.getDefaultDisplay().getRealSize(point);
            this.windowParams.width = point.x;
            this.windowParams.height = point.y;
        } else {
            this.windowParams.width = 320;
            this.windowParams.height = 360;
        }
        this.windowParams.x = 0;
        this.windowParams.y = 0;
        this.windowParams.setTitle("com.xtc.hall.HallAnimationView");
        this.windowParams.type = 2003;
        this.windowParams.format = 1;
        this.windowParams.flags = 16777352;
        this.windowParams.flagsEx = 1;
        if (this.openView == null) {
            this.openView = HallDeviceManager.instance().createView(this.context, true);
            this.openView.setAnimatorListener(this.mOpenListener);
            this.openView.setOnDrawListener(this.mOnDrawListener);
        }
        if (this.closeView == null) {
            this.closeView = HallDeviceManager.instance().createView(this.context, false);
            this.closeView.setAnimatorListener(this.mCloseListener);
        }
        Log.d(TAG, "view init done");
    }

    public void playAnimation(HallAnimationView view) {
        Message.obtain(this.uiHandler, MSG_PLAY_ANIM, view).sendToTarget();
    }

    public void setHallAnimation(String value) {
        try {
            setHallAnimation(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            setAnimationFromPath(value);
        }
    }

    public void setTitle(String title) {
        this.openView.setTitle(title);
    }

    public void showCloseView(HallAnimationView view) {
        if (notShow(view)) {
            return;
        }
        if (this.openView.isShow()) {
            this.openView.setShow(false);
            this.openView.cancelAnimation();
            this.windowManager.removeView(this.openView);
        }
        view.setShow(true);
        view.setRemoveWhenEnd(true);
        this.windowManager.addView(view, this.windowParams);
        view.playAnimation();
        Log.d(TAG, "add close view without go to sleep");
    }

    public void showTipView(int duration) {
        if (this.tipLayout != null) {
            return;
        }
        initTipView(duration);
        this.workHandler.removeMessages(MSG_GOTO_SLEEP);
        this.workHandler.removeMessages(MSG_PLAY_SOUND);
        this.uiHandler.removeMessages(MSG_HIDE_TIP);
        Log.d(TAG, "show tip layout");
        this.tipShowTime = SystemClock.uptimeMillis();
        this.windowParams.flags &= -129;
        this.windowManager.addView(this.tipLayout, this.windowParams);
        this.powerManager.wakeUp(SystemClock.uptimeMillis(), "HallEvent");
    }

    public void showView(HallAnimationView view) {
        HallAnimationView other;
        if (notShow(view)) {
            return;
        }
        if (view == this.closeView) {
            other = this.openView;
        } else {
            other = this.closeView;
            this.uiHandler.removeMessages(MSG_HIDE_VIEW);
            this.uiHandler.sendMessageDelayed(
                    Message.obtain(this.uiHandler, MSG_HIDE_VIEW, this.openView),
                    HallUtil.DELAY_TIME);
        }
        if (other.isShow()) {
            other.setShow(false);
            other.cancelAnimation();
            this.windowManager.removeView(other);
        }
        if (this.telecomManager.isInCall()) {
            Log.d(TAG, "in call, do not show animation view");
            return;
        }
        view.setShow(true);
        this.windowManager.addView(view, this.windowParams);
        view.playAnimation();
        Log.d(TAG, "add animation view openView:" + (view == this.openView));
    }

    public boolean tipShow() {
        return this.tipLayout != null;
    }
}