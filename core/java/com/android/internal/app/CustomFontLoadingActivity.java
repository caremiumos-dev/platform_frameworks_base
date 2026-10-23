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

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.android.internal.R;

/**
 * XTC/厂商自定义字体加载提示页（定时自动关闭）。
 */
public class CustomFontLoadingActivity extends Activity {
    private static final String TAG = "CustomFontLoading";
    private static final String KEY_HINT_TEXT = "key_hint_text";
    private static final String KEY_CLOSE_TIME = "key_close_time";
    private static final int DEFAULT_CLOSE_TIME = 10000;

    private Handler mHandler;
    private TextView mTvMessage;
    private ObjectAnimator mProgressAnimator;
    private int mCloseTime = DEFAULT_CLOSE_TIME;

    private final Runnable mFinishDelayed = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) {
                return;
            }
            Log.d(TAG, "It's time to finish.");
            finish();
        }
    };

    private void setupFinishDelayed() {
        mHandler.postDelayed(mFinishDelayed, mCloseTime);
    }

    private void clearFinishDelayed() {
        mHandler.removeCallbacks(mFinishDelayed);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.activity_custom_font_loading);
        mTvMessage = (TextView) findViewById(R.id.text);
        Intent intent = getIntent();
        if (intent != null) {
            String hint = intent.getStringExtra(KEY_HINT_TEXT);
            mCloseTime = intent.getIntExtra(KEY_CLOSE_TIME, DEFAULT_CLOSE_TIME);
            mTvMessage.setText(hint);
            Log.d(TAG, "onCreate hint = " + hint + ", mCloseTime = " + mCloseTime);
        }
        mHandler = new Handler(getMainLooper());
        setupFinishDelayed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            boolean close = intent.getBooleanExtra("close", false);
            String hint = intent.getStringExtra(KEY_HINT_TEXT);
            if (!TextUtils.isEmpty(hint)) {
                mTvMessage.setText(hint);
            }
            Log.d(TAG, "onNewIntent close = " + close + ", hint = " + hint);
            if (close) {
                clearFinishDelayed();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
