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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Slog;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Full-screen system dialog used to surface a vendor pushed "security
 * exception". See {@link SystemExceptionManager} for the condemnation of the
 * remote-uninstall / remote-disable behaviour this dialog announces.
 *
 * @hide
 */
public class SystemExceptionDialog extends AlertDialog {
    private static final String TAG = "SystemXses Dialog";

    private static final int WINDOW_TYPE_SYSTEM = 2014;

    private static String sShowMessage = "";

    public SystemExceptionDialog(Context context, String message) {
        super(context, com.android.internal.R.style.NotificationEmphasizedAction);
        getWindow().setType(WINDOW_TYPE_SYSTEM);
        getWindow().addFlags(6947969);
        sShowMessage = message;
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("SystemExceptionDialog");
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        attrs.width = metrics.widthPixels;
        attrs.height = metrics.heightPixels;
        getWindow().setAttributes(attrs);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (sShowMessage.equals("root_dialog")) {
            setContentView(com.android.internal.R.layout.system_root_dialog);
            ((TextView) findViewById(com.android.internal.R.id.system_root_ok_btn))
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dismiss();
                        }
                    });
            return;
        }
        setContentView(com.android.internal.R.layout.system_exception_dialog);
        TextView textView = (TextView) findViewById(
                com.android.internal.R.id.system_exception_message);
        Slog.d(TAG, "showMessage = " + sShowMessage);
        if (sShowMessage.equals("dialog_default")) {
            textView.setText(com.android.internal.R.string.exception_default_message);
        } else if (sShowMessage.equals(SystemExceptionManager.EXCEPTION_TYPE_UNINSTALL)) {
            textView.setText(com.android.internal.R.string.exception_uninstall_message);
        } else if (sShowMessage.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_NET_APP)) {
            textView.setText(com.android.internal.R.string.exception_disable_net_message);
        } else if (sShowMessage.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_APP)) {
            textView.setText(com.android.internal.R.string.exception_disable_app_message);
        } else if (sShowMessage.isEmpty() || sShowMessage.length() <= 20) {
            textView.setText(com.android.internal.R.string.exception_default_message);
        } else {
            textView.setText(sShowMessage);
        }
        ((TextView) findViewById(com.android.internal.R.id.system_exception_ok_btn))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dismiss();
                    }
                });
    }
}