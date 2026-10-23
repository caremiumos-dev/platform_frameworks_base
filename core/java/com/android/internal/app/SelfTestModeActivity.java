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
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.internal.R;
import com.xtc.utils.WatchCheckCode;

/**
 * XTC/厂商自检模式：展示动态校验码并跳转到输入校验码页。
 */
public class SelfTestModeActivity extends Activity implements View.OnClickListener {
    private static final String TAG = "SelfTestModeActivity";

    private FrameLayout mFlShowCheckCode;
    private TextView mTvCheckCode;
    private Button mBtnNext;
    private int mVerifyMode = 1;

    private void showCheckCode() {
        String checkCode = WatchCheckCode.generateWatchCheckCode(mVerifyMode);
        mTvCheckCode.setText(checkCode);
        WatchCheckCode.reportDataByXtcBehavior(getApplicationContext(), checkCode, mVerifyMode,
                false, false);
    }

    private void nextToInputCheckCode() {
        Log.d(TAG, "nextToInputCheckCode watchCheckCode = " + mTvCheckCode.getText().toString());
        Intent intent = new Intent(getApplicationContext(), SelfTestVerifyModeActivity.class);
        intent.putExtra(MoreActionActivity.EXTRA_VERIFY_MODE, mVerifyMode);
        startActivity(intent);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_next) {
            nextToInputCheckCode();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selftest_mode);
        mTvCheckCode = (TextView) findViewById(R.id.tv_check_code);
        mBtnNext = (Button) findViewById(R.id.btn_next);
        mBtnNext.setOnClickListener(this);
        Intent intent = getIntent();
        if (intent != null) {
            mVerifyMode = intent.getIntExtra(MoreActionActivity.EXTRA_VERIFY_MODE, 1);
        }
        Window window = getWindow();
        WindowManager.LayoutParams attributes = window.getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        window.setAttributes(attributes);
        Log.d(TAG, "onCreate mVerifyMode = " + mVerifyMode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        showCheckCode();
    }
}
