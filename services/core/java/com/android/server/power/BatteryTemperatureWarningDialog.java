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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Dialog shown when the battery temperature is abnormal.
 *
 * <p>NOTE: the vendor dialog layout {@code dialog_battery_temperature_warning}
 * was recovered from the device framework-res (see Phase3d).
 *
 * @hide
 */
public abstract class BatteryTemperatureWarningDialog extends AlertDialog
        implements View.OnClickListener {
    public Context mContext;
    private Button mButton;

    protected BatteryTemperatureWarningDialog(Context context) {
        super(context);
        mContext = context;
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.alpha = 1.0f;
        attrs.dimAmount = 1.0f;
        attrs.setTitle("BatteryTemperatureWarningDialogWindow");
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        attrs.width = metrics.widthPixels;
        attrs.height = metrics.heightPixels;
        getWindow().setAttributes(attrs);
    }

    public abstract void dismissCallBack(View view);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.android.internal.R.layout.dialog_battery_temperature_warning);
        mButton = (Button) findViewById(
                com.android.internal.R.id.dialog_battery_warning_confirm_button);
        mButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case com.android.internal.R.id.dialog_battery_warning_confirm_button:
                dismissCallBack(view);
                dismiss();
                break;
        }
    }

    public void setDialogSize(int width, int height) {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.width = width;
        attrs.height = height;
        getWindow().setAttributes(attrs);
    }
}