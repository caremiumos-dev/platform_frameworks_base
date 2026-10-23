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

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;

import com.android.internal.R;

/**
 * XTC/厂商「更多操作」页：绑定号、自检模式、ADB 开关入口。
 */
public class MoreActionActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "MoreActionActivity";

    public static final String EXTRA_VERIFY_MODE = "verify_mode";
    public static final String PROP_UNLOCK_SELFTEST = "sys.unlock.selftest";
    public static final String PROP_UNLOCK_ADB = "sys.unlock.adb";

    private void showBindNumberDialog() {
        Log.d(TAG, "showBindNumberDialog");
        startActivity(new Intent(getApplicationContext(), BindNumberActivity.class));
    }

    private void gotoSelfTest() {
        Log.d(TAG, "gotoSelfTest");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xtc.selftest",
                "com.xtc.selftest.MainActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void gotoAdbSwitch() {
        Log.d(TAG, "gotoAdbSwitch");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xtc.selftest",
                "com.xtc.selftest.ui.DebugActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showSelfTestModeVerify() {
        Log.d(TAG, "showSelfTestModeVerify");
        Intent intent = new Intent(getApplicationContext(), SelfTestModeActivity.class);
        intent.putExtra(EXTRA_VERIFY_MODE, 1);
        startActivity(intent);
    }

    private void showAdbSwitchVerify() {
        Log.d(TAG, "showAdbSwitchVerify");
        Intent intent = new Intent(getApplicationContext(), SelfTestModeActivity.class);
        intent.putExtra(EXTRA_VERIFY_MODE, 2);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.ll_bind_number) {
            showBindNumberDialog();
        } else if (id == R.id.ll_selftest_mode) {
            if (SystemProperties.getBoolean(PROP_UNLOCK_SELFTEST, false)) {
                gotoSelfTest();
            } else {
                showSelfTestModeVerify();
            }
        } else if (id == R.id.ll_adb_switch) {
            if (SystemProperties.getBoolean(PROP_UNLOCK_ADB, false)) {
                gotoAdbSwitch();
            } else {
                showAdbSwitchVerify();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_more_action);
        findViewById(R.id.ll_bind_number).setOnClickListener(this);
        findViewById(R.id.ll_selftest_mode).setOnClickListener(this);
        findViewById(R.id.ll_adb_switch).setOnClickListener(this);
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}
