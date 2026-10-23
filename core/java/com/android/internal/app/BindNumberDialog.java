/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.internal.app;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.R;

/**
 * XTC/厂商绑定号对话框。
 */
public class BindNumberDialog extends AlertDialog implements View.OnClickListener {
    private static final String TAG = "BindNumberDialog";

    public BindNumberDialog(Context context) {
        super(context, R.style.BlackDialogStyle);
        getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.setTitle(getClass().getSimpleName());
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        attributes.width = displayMetrics.widthPixels;
        attributes.height = displayMetrics.heightPixels;
        getWindow().setAttributes(attributes);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_i_know) {
            dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bind_number_dialog);
        ((TextView) findViewById(R.id.tv_bind_number)).setText(
                SystemProperties.get("ro.boot.bindnumber", "unknown"));
        ((Button) findViewById(R.id.btn_i_know)).setOnClickListener(this);
    }
}
