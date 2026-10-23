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

package com.xtc.xtcdiag.selftest;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.R;
import com.xtc.xtcdiag.XtcDiagManager;
import com.xtc.xtcdiag.bean.TestItem;
import com.xtc.xtcdiag.socket.ItemTestResult;
import com.xtc.xtcdiag.socket.XtcSocketJsonFormat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Activity driving the interactive part of the factory self-test. The PC-side
 * diag tool requests a list of test codes; each one is launched in the vendor
 * self-test application and the operator confirms the result here.
 *
 * @hide
 */
public class SelfTestUtilActivity extends Activity {
    private static final String TAG = "XtcDiag-SelfTestActivity";

    public static final String EXTRA_ALL_TEST_LIST = "alltestList";

    private ArrayList<TestItem> mAllTestList;
    private XtcDiagManager mDiagManager;
    private final ConcurrentLinkedQueue<TestItem> mQueue = new ConcurrentLinkedQueue<>();

    private void parseIntent() {
        Log.d(TAG, "--parseIntent--");
        mAllTestList = getIntent().getParcelableArrayListExtra(EXTRA_ALL_TEST_LIST);
        if (mAllTestList == null || mAllTestList.isEmpty()) {
            Log.d(TAG, "startTest no test item !");
            finish();
            return;
        }
        ArrayList<Integer> pending = new ArrayList<>();
        Iterator<TestItem> iterator = mQueue.iterator();
        while (iterator.hasNext()) {
            pending.add(iterator.next().getCmdCode());
        }
        for (TestItem item : mAllTestList) {
            if (!pending.contains(item.getCmdCode())) {
                mQueue.add(item);
            }
        }
        runCmdQueue();
    }

    private void runCmdQueue() {
        if (!mQueue.isEmpty()) {
            startTest(mQueue.poll());
        } else {
            Log.d(TAG, "self test cmd queue is empty.");
            finish();
        }
    }

    private void sendMsg2Pc(int cmdCode, ItemTestResult result) {
        if (mDiagManager == null || !mDiagManager.isSocketConnected()) {
            Log.d(TAG, "sendMsg2Pc return mDiagManager:"
                    + (mDiagManager == null ? "null" : mDiagManager.isSocketConnected()));
            return;
        }
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put("id", 35);
        format.put("key", "selftest_operate");
        format.put("result", result);
        mDiagManager.send(cmdCode, format.build().toString(), null);
    }

    private void showOpDialog(final int cmdCode) {
        String message = getString(R.string.xtc_diag_test_result);
        if (mAllTestList != null && !mAllTestList.isEmpty()) {
            for (TestItem item : mAllTestList) {
                if (cmdCode == item.getCmdCode()) {
                    message = item.getDesc() + "\n" + message;
                }
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        View view = LayoutInflater.from(this).inflate(R.layout.layout_op_dialog, null);
        TextView title = (TextView) view.findViewById(R.id.tv_dialog_title);
        TextView content = (TextView) view.findViewById(R.id.tv_dialog_content);
        Button positive = (Button) view.findViewById(R.id.btn_dialog_positive);
        Button negative = (Button) view.findViewById(R.id.btn_dialog_negative);
        title.setText(String.valueOf(cmdCode));
        content.setText(message);
        final AlertDialog dialog = builder.create();
        dialog.show();
        Window window = dialog.getWindow();
        window.setContentView(view);
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = metrics.widthPixels;
        params.height = metrics.heightPixels;
        window.setAttributes(params);
        positive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "setPositiveButton cmdCode:" + cmdCode);
                dialog.dismiss();
                sendMsg2Pc(cmdCode, ItemTestResult.TEST_PASS);
                runCmdQueue();
            }
        });
        negative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "setNegativeButton cmdCode:" + cmdCode);
                dialog.dismiss();
                sendMsg2Pc(cmdCode, ItemTestResult.TEST_FAIL);
                runCmdQueue();
            }
        });
    }

    private void startTest(TestItem item) {
        if (item == null) {
            Log.d(TAG, "startTest item is null");
            return;
        }
        int cmdCode = item.getCmdCode();
        Log.d(TAG, "startTest testCode:" + cmdCode);
        Intent intent = new Intent("com.xtc.selftest.PC_TEST_MODE");
        intent.setPackage("com.xtc.selftest");
        intent.setData(Uri.parse("test_code://" + cmdCode));
        intent.putExtra("key", UserHandle.myUserId());
        try {
            startActivityForResult(intent, cmdCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parseIntent();
        mDiagManager = new XtcDiagManager(this);
        setTitle("SelfTest");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        parseIntent();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult requestCode:" + requestCode + " ,result:" + resultCode
                + " ,data:" + data);
        showOpDialog(requestCode);
    }
}