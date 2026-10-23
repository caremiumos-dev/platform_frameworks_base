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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Vendor dialog asking the user to allow or ignore an app-op request. The
 * answer is delivered asynchronously to {@link AppOpsService}.
 *
 * <p>NOTE: the vendor dialog layout {@code permission_confirmation_dialog} was
 * recovered from the device framework-res (see Phase3d).
 *
 * @hide
 */
public class PermissionDialog extends BasePermissionDialog {
    private static final int ALLOWED_REQ = 2;
    private static final int IGNORED_REQ = 4;
    private static final int IGNORED_REQ_TIMEOUT = 8;
    private static final long TIMEOUT_WAIT = 15000L;

    private final AppOpsService opsServ;
    private final int inputId;
    private final String inputPackage;
    private final int mDef;
    private final Context contId;
    private final Handler myHandle;
    private final CharSequence[] mOpLabels;
    private final View viewId;

    public PermissionDialog(Context context, AppOpsService opsService, int opCode, int uid,
            String packageName) {
        super(context);
        opsServ = opsService;
        inputPackage = packageName;
        contId = context;
        mDef = opCode;
        inputId = uid;
        myHandle = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int mode = 1;
                if (msg.what == ALLOWED_REQ) {
                    mode = 0;
                }
                opsServ.notifyOperation(mDef, inputId, inputPackage, mode);
                dismiss();
            }
        };
        mOpLabels = context.getResources().getTextArray(
                com.android.internal.R.array.app_ops_labels);
        setCancelable(false);
        setButton(BUTTON_POSITIVE, context.getString(com.android.internal.R.string.allow),
                myHandle.obtainMessage(ALLOWED_REQ));
        setButton(BUTTON_NEGATIVE, context.getString(com.android.internal.R.string.deny),
                myHandle.obtainMessage(IGNORED_REQ));
        setTitle(" ");
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("PermissionXXX: " + getAppName(inputPackage));
        attrs.privateFlags |= 0x110;
        getWindow().setAttributes(attrs);
        viewId = getLayoutInflater().inflate(
                com.android.internal.R.layout.permission_confirmation_dialog, null);
        TextView text = (TextView) viewId.findViewById(
                com.android.internal.R.id.permission_text);
        String appName = getAppName(inputPackage);
        text.setText((appName == null ? inputPackage : appName) + ": " + mOpLabels[mDef - 70]);
        setView(viewId);
        myHandle.sendMessageDelayed(myHandle.obtainMessage(IGNORED_REQ_TIMEOUT), TIMEOUT_WAIT);
    }

    private String getAppName(String packageName) {
        PackageManager pm = contId.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 8704);
            if (info != null) {
                return (String) pm.getApplicationLabel(info);
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }
}