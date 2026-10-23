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
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.R;
import com.xtc.utils.WatchCheckCode;

/**
 * XTC/厂商自检校验模式：输入动态校验码，成功后解锁 ADB（USB 串口校验）或自检。
 */
public class SelfTestVerifyModeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "SelfTestVerifyModeActivity";
    private static final int MAX_ERROR_TIMES = 3;

    interface SecureCheckInterface {
        void onCallback(boolean success);
    }

    public class SecureCheckCallback implements SecureCheckInterface {
        @Override
        public void onCallback(final boolean success) {
            if (success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        gotoAdbSwitch();
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
            }
        }
    }

    public class VerificaTionThread extends Thread {
        private final Context mContext;
        private OpenFeatureSecureCheck secureCheck = null;

        public VerificaTionThread(Context context) {
            mContext = context;
        }

        @Override
        public void run() {
            secureCheck = new OpenFeatureSecureCheck(mContext);
            SecureCheckCallback callback = new SecureCheckCallback();
            secureCheck.initSecureCheck();
            secureCheck.sendCheckRequest(callback);
        }

        public void stopVerify() {
            if (secureCheck != null) {
                secureCheck.stopCheckRequest();
            }
        }
    }

    private EditText mEtCheckCode;
    private Button mBtnOk;
    private int mVerifyMode = 1;
    private int mErrorTimes;
    private Toast mErrorToast;
    private Toast mInputToast;
    private Toast mVerifyingToast;
    private VerificaTionThread mVerificaTionThread = null;

    private void gotoSelfTest() {
        Log.d(TAG, "gotoSelfTest");
        SystemProperties.set(MoreActionActivity.PROP_UNLOCK_SELFTEST, Boolean.toString(true));
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xtc.selftest",
                "com.xtc.selftest.MainActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
    }

    private void gotoAdbSwitch() {
        Log.d(TAG, "gotoAdbSwitch");
        SystemProperties.set(MoreActionActivity.PROP_UNLOCK_ADB, Boolean.toString(true));
        mVerifyingToast.cancel();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.xtc.selftest",
                "com.xtc.selftest.ui.DebugActivity"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
    }

    private void startSecureCheck() {
        Log.d(TAG, "startSecureCheck");
        synchronized (this) {
            if (mVerificaTionThread == null) {
                mVerificaTionThread = new VerificaTionThread(getApplicationContext());
                mVerificaTionThread.start();
                mVerifyingToast.show();
            }
        }
    }

    private void verifyCheckCode() {
        Log.d(TAG, "verifyCheckCode");
        String checkCode = mEtCheckCode.getText().toString();
        if (TextUtils.isEmpty(checkCode)) {
            mInputToast.show();
            return;
        }
        boolean success = WatchCheckCode.verifyCheckCode(checkCode, mVerifyMode);
        if (!success) {
            Log.d(TAG, "verifyCheckCode failure");
            mErrorTimes++;
            if (mErrorTimes >= MAX_ERROR_TIMES) {
                mErrorToast.cancel();
                Toast.makeText(this, R.string.check_code_error_too_many_times,
                        Toast.LENGTH_LONG).show();
                finish();
            } else {
                mErrorToast.show();
            }
        } else if (mVerifyMode == 1) {
            gotoSelfTest();
        } else if (mVerifyMode == 2) {
            startSecureCheck();
        } else {
            Log.w(TAG, "Invalid verify mode : " + mVerifyMode);
        }
        WatchCheckCode.reportDataByXtcBehavior(getApplicationContext(), checkCode, mVerifyMode,
                true, success);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_ok) {
            verifyCheckCode();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selftest_verify_mode);
        mEtCheckCode = (EditText) findViewById(R.id.et_check_code);
        mBtnOk = (Button) findViewById(R.id.btn_ok);
        mBtnOk.setOnClickListener(this);
        Intent intent = getIntent();
        if (intent != null) {
            mVerifyMode = intent.getIntExtra(MoreActionActivity.EXTRA_VERIFY_MODE, 1);
        }
        mErrorTimes = 0;
        mErrorToast = Toast.makeText(this, R.string.check_code_error, Toast.LENGTH_SHORT);
        mInputToast = Toast.makeText(this, R.string.input_check_code, Toast.LENGTH_SHORT);
        mVerifyingToast = Toast.makeText(this, R.string.code_verifying, Toast.LENGTH_LONG);
        Log.d(TAG, "onCreate mVerifyMode = " + mVerifyMode);
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        window.setAttributes(attributes);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy stop");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mErrorTimes = 0;
        Log.d(TAG, "onNewIntent");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mVerificaTionThread != null) {
            mVerificaTionThread.stopVerify();
        }
        Log.d(TAG, "onStop");
    }
}
