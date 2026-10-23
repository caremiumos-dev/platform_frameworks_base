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
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Vendor confirm dialog for the system-restore action.
 *
 * @hide
 */
public class SystemRestoreConfirmDialog extends AlertDialog implements View.OnClickListener {
    public Context mContext;
    private ShutdownDialogCallback mCallback;
    private TextView tv_dialog_positive;
    private TextView tv_dialog_negative;

    public SystemRestoreConfirmDialog(Context context) {
        super(context);
        mContext = context;
        getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        getWindow().addFlags(0x6a0201);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle(getClass().getSimpleName());
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onClick(View view) {
        if (view == tv_dialog_negative) {
            if (mCallback != null) {
                mCallback.dismissCallBack(view);
            }
            dismiss();
        } else if (view == tv_dialog_positive) {
            if (mCallback != null) {
                mCallback.clickCallBack(view);
            }
            dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.android.internal.R.layout.dialog_system_restore_confirm);
        tv_dialog_positive = (TextView) findViewById(
                com.android.internal.R.id.tv_confirm_positive);
        tv_dialog_positive.setOnClickListener(this);
        tv_dialog_negative = (TextView) findViewById(
                com.android.internal.R.id.tv_confirm_negative);
        tv_dialog_negative.setOnClickListener(this);
    }

    public void setCallback(ShutdownDialogCallback callback) {
        mCallback = callback;
    }

    public void setNegativeButton(int textResId, android.content.DialogInterface.OnClickListener listener) {
        setButton(BUTTON_NEGATIVE, mContext.getString(textResId), listener);
    }

    public void setPositiveButton(int textResId, android.content.DialogInterface.OnClickListener listener) {
        setButton(BUTTON_POSITIVE, mContext.getString(textResId), listener);
    }
}