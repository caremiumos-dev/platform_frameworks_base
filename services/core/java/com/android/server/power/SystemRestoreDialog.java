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

package com.android.server.power;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.xtc.moduleswitch.ModuleSwitch;

/**
 * Vendor system-restore dialog.
 *
 * <p>NOTE: the vendor dialog layouts {@code dialog_system_restore} / {@code dialog_tip}
 * were recovered from the device framework-res (see Phase3d).
 *
 * @hide
 */
public class SystemRestoreDialog extends AlertDialog implements View.OnClickListener {
    private static final String TAG = "ShutdownDialog";
    private static final int MSG_GONE_TIPS = 1;
    private static final long TIP_SHOW_TIMES = 3000L;
    private static final int MODULE_SWITCH_ID = 2851;

    public Context mContext;
    private final String text_content;
    private boolean allowPowerOff = true;
    private ShutdownDialogCallback mCallback;

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_GONE_TIPS && mTips != null) {
                mTips.setVisibility(View.GONE);
            }
        }
    };
    private final Runnable mDelayDismissRunnable = new Runnable() {
        @Override
        public void run() {
            dismiss();
        }
    };

    private TextView tv_dialog_negative;
    private ImageButton tv_dialog_positive;
    private TextView tv_dialog_positive_repair;
    private TextView mMoreAction;
    private RelativeLayout mTips;

    public SystemRestoreDialog(Context context, String textContent) {
        super(context);
        mContext = context;
        text_content = textContent;
        getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().addFlags(0x6a0200);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("shutDownDialogWindow");
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        attrs.width = metrics.widthPixels;
        attrs.height = metrics.heightPixels;
        getWindow().setAttributes(attrs);
    }

    private void showShutdownTips() {
        if (mTips != null) {
            mTips.setVisibility(View.VISIBLE);
            handler.removeMessages(MSG_GONE_TIPS);
            handler.sendEmptyMessageDelayed(MSG_GONE_TIPS, TIP_SHOW_TIMES);
        }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == tv_dialog_negative) {
            if (mCallback != null) {
                mCallback.dismissCallBack(view);
            }
            dismiss();
        } else if (view == tv_dialog_positive) {
            Log.v(TAG, "allowPowerOff:" + allowPowerOff);
            if (!allowPowerOff) {
                showShutdownTips();
            } else {
                if (mTips != null) {
                    mTips.setVisibility(View.GONE);
                }
                if (mCallback != null) {
                    mCallback.clickCallBack(view);
                }
            }
        } else if (view == tv_dialog_positive_repair) {
            if (mCallback != null) {
                mCallback.clickCallBack(view);
            }
        } else if (view == mMoreAction) {
            if (mCallback != null) {
                mCallback.clickCallBack(view);
            }
            if (!handler.hasCallbacks(mDelayDismissRunnable)) {
                handler.postDelayed(mDelayDismissRunnable, 500L);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        allowPowerOff = SystemProperties.getBoolean("persist.sys.allowpoweroff", true);
        Log.d(TAG, "onCreate: text_content=" + text_content);
        if (text_content != null) {
            if (text_content.equals(mContext.getResources().getString(
                    com.android.internal.R.string.reboot_to_restore))) {
                setContentView(com.android.internal.R.layout.dialog_system_restore);
                tv_dialog_positive_repair = (TextView) findViewById(
                        com.android.internal.R.id.tv_dialog_positive_repair);
                tv_dialog_positive_repair.setOnClickListener(this);
                mMoreAction = (TextView) findViewById(
                        com.android.internal.R.id.tv_more_action);
                mMoreAction.setOnClickListener(this);
                boolean moreAction = ModuleSwitch.queryModuleSwitch(
                        getContext(), MODULE_SWITCH_ID, true);
                mMoreAction.setVisibility(moreAction ? View.VISIBLE : View.GONE);
                Log.d(TAG, "onCreate: moreAction=" + moreAction);
            } else {
                setContentView(com.android.internal.R.layout.dialog_tip);
                ((ImageButton) findViewById(com.android.internal.R.id.tv_dialog_reboot))
                        .setOnClickListener(this);
                tv_dialog_positive = (ImageButton) findViewById(
                        com.android.internal.R.id.tv_dialog_positive);
                tv_dialog_positive.setOnClickListener(this);
                if (!allowPowerOff) {
                    tv_dialog_positive.setBackground(mContext.getResources().getDrawable(
                            com.android.internal.R.drawable.ic_lockscreen_decline_normal));
                }
                mTips = (RelativeLayout) findViewById(com.android.internal.R.id.dialog_toast);
                mTips.setVisibility(View.GONE);
            }
        }
        tv_dialog_negative = (TextView) findViewById(
                com.android.internal.R.id.tv_dialog_negative);
        tv_dialog_negative.setOnClickListener(this);
    }

    public void setCallback(ShutdownDialogCallback callback) {
        mCallback = callback;
    }

    public void setDialogSize(int width, int height) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.width = width;
        attrs.height = height;
        getWindow().setAttributes(attrs);
    }

    public void setNegativeButton(int textResId, android.content.DialogInterface.OnClickListener listener) {
        setButton(BUTTON_NEGATIVE, mContext.getString(textResId), listener);
    }

    public void setPositiveButton(int textResId, android.content.DialogInterface.OnClickListener listener) {
        setButton(BUTTON_POSITIVE, mContext.getString(textResId), listener);
    }
}