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
import android.os.Bundle;
import android.os.SystemProperties;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.R;

/**
 * XTC/厂商绑定号展示页。读取 {@code ro.boot.bindnumber} 并显示。
 */
public class BindNumberActivity extends Activity implements View.OnClickListener {
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_i_know) {
            finish();
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
