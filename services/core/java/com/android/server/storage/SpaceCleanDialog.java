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

package com.android.server.storage;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

/**
 * Dialog prompting the user to clean up storage when it runs low.
 *
 * @hide
 */
public abstract class SpaceCleanDialog extends AlertDialog implements View.OnClickListener {
    private static final int GONE_TIPS = 1;
    private static final long TIP_SHOW_TIMES = 3000L;

    public Context mContext;
    private final String text_content;
    private final Handler handler = new Handler();

    private Button btn_spaceclean;
    private Button btn_cancel;
    private TextView tv_spaceclean_low;
    private TextView tv_spaceclean_low_one;
    private TextView tv_spaceclean_low_two;
    private TextView tv_spaceclean_full;
    private TextView tv_low_tip;

    protected SpaceCleanDialog(Context context, String textContent) {
        super(context);
        mContext = context;
        text_content = textContent;
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.alpha = 1.0f;
        attrs.dimAmount = 1.0f;
        attrs.setTitle("SpaceCleanDialogWindow");
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        attrs.width = metrics.widthPixels;
        attrs.height = metrics.heightPixels;
        getWindow().setAttributes(attrs);
    }

    public abstract void clickCallBack(View view);

    public abstract void dismissCallBack(View view);

    @Override
    public void dismiss() {
        super.dismiss();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case com.android.internal.R.id.dialog_btn_cancel:
                dismissCallBack(view);
                dismiss();
                break;
            case com.android.internal.R.id.dialog_btn_spaceclean:
                clickCallBack(view);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (text_content == null) {
            return;
        }
        setContentView(com.android.internal.R.layout.dialog_spaceclean);
        btn_spaceclean = (Button) findViewById(
                com.android.internal.R.id.dialog_btn_spaceclean);
        btn_cancel = (Button) findViewById(com.android.internal.R.id.dialog_btn_cancel);
        tv_spaceclean_low = (TextView) findViewById(
                com.android.internal.R.id.tv_spaceclean_low);
        tv_spaceclean_low_one = (TextView) findViewById(
                com.android.internal.R.id.tv_spaceclean_low_tipone);
        tv_spaceclean_low_two = (TextView) findViewById(
                com.android.internal.R.id.tv_spaceclean_low_tiptwo);
        tv_spaceclean_full = (TextView) findViewById(
                com.android.internal.R.id.tv_spaceclean_full);
        tv_low_tip = (TextView) findViewById(com.android.internal.R.id.tv_low_tip);
        btn_spaceclean.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);
        if ("Full_Space".equals(text_content)) {
            btn_cancel.setVisibility(View.GONE);
            tv_spaceclean_low.setVisibility(View.GONE);
            tv_spaceclean_low_one.setVisibility(View.GONE);
            tv_spaceclean_low_two.setVisibility(View.GONE);
            tv_spaceclean_full.setVisibility(View.VISIBLE);
        } else {
            btn_cancel.setVisibility(View.VISIBLE);
            tv_spaceclean_low.setVisibility(View.VISIBLE);
            tv_spaceclean_low_one.setVisibility(View.VISIBLE);
            tv_spaceclean_low_two.setVisibility(View.VISIBLE);
            tv_spaceclean_full.setVisibility(View.GONE);
        }
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