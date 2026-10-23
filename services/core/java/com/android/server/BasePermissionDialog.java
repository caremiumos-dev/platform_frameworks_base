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

package com.android.server;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;

/**
 * Base class for the vendor permission dialogs. It disables the dialog buttons
 * until the window is ready so that an accidental tap cannot be delivered.
 *
 * @hide
 */
public class BasePermissionDialog extends AlertDialog {
    private static final int MSG_ENABLE_BUTTONS = 0;

    private final Handler mInfoHandler;
    private boolean mState;

    public BasePermissionDialog(Context context) {
        super(context, com.android.internal.R.style.Theme_DeviceDefault_Dialog_Alert);
        mState = true;
        mInfoHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_ENABLE_BUTTONS) {
                    mState = false;
                    setEnabled(true);
                }
            }
        };
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        attrs.setTitle("Permission");
        setIconAttribute(android.R.attr.alertDialogIcon);
        getWindow().setAttributes(attrs);
    }

    private void setEnabled(boolean enabled) {
        Button button1 = (Button) findViewById(android.R.id.button1);
        if (button1 != null) {
            button1.setEnabled(enabled);
        }
        Button button2 = (Button) findViewById(android.R.id.button2);
        if (button2 != null) {
            button2.setEnabled(enabled);
        }
        Button button3 = (Button) findViewById(android.R.id.button3);
        if (button3 != null) {
            button3.setEnabled(enabled);
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (mState) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onStart() {
        super.onStart();
        setEnabled(false);
        mInfoHandler.sendMessage(mInfoHandler.obtainMessage(MSG_ENABLE_BUTTONS));
    }
}