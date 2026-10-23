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

package com.android.server.xtcdiag.qdiag;

import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.qualcomm.qti.diagservice.diag_dci_cbs;
import com.xtc.xtcdiag.socket.ItemJsonKey;
import com.xtc.xtcdiag.socket.XtcConvertUtil;
import com.xtc.xtcdiag.socket.XtcFileUtil;
import com.xtc.xtcdiag.socket.XtcSocketJsonFormat;
import com.xtc.xtcdiag.socket.XtcSocketUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Drives the Qualcomm diag (DCI) path: queues Wi-Fi test commands, collects the
 * results and relays them to the PC-side diag tool.
 *
 * @hide
 */
public class XtcQDiagUtil {
    private static final String TAG = "XtcDiag-QCOM";

    public static final String DIAG_DIR_PATH = "/data/xtc_diag/";

    private static final String DIAG_RESULT_FILE_PATH = "/data/xtc_diag/diag_result_2_pc.txt";
    private static final int MSG_RELOAD_WIFI_DRIVER = 5;

    private DiagDciCallBack mCallback;
    private Handler mServiceHandler;
    private XtcSocketUtil mXtcPcSocket;
    private XtcQDiagDci mXtcQDiagDci;

    private final ConcurrentLinkedQueue<String> mWifiCmdQueue = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<String> mWifiTestResultList = new CopyOnWriteArrayList<>();
    private boolean isWifiCmdFinish = false;
    private int mCurrentRfCmd = 32;

    private class DiagDciCallBack implements diag_dci_cbs {
        @Override
        public void process_dci_log_stream(byte[] data, int length) {
            Log.d(TAG, "process_dci_log_stream len:" + length);
        }

        @Override
        public void process_dci_event_stream(byte[] data, int length) {
            Log.d(TAG, "process_dci_event_stream len:" + length);
        }

        @Override
        public void process_dci_pkt_rsp(byte[] data, int length, long timeout) {
            Log.d(TAG, "process_dci_pkt_rsp len:" + length);
            if (mCurrentRfCmd == 32) {
                send2Pc((short) 32, null, data);
            } else if (mCurrentRfCmd == 33) {
                if (isWifiCmdFinish) {
                    Log.d(TAG, "has receive more wifi diag result msg when finish.");
                } else {
                    parseWifiDiagResult(data);
                }
            }
        }

        @Override
        public void process_dci_signal_cb(int signal) {
            Log.d(TAG, "process_dci_signal_cb signal:" + signal);
        }
    }

    public XtcQDiagUtil(XtcSocketUtil pcSocket, Handler handler) {
        mXtcPcSocket = pcSocket;
        mServiceHandler = handler;
        initDci();
    }

    public void initDci() {
        mCallback = new DiagDciCallBack();
        mXtcQDiagDci = new XtcQDiagDci(mCallback);
        mXtcQDiagDci.registerDciSignalData();
        mXtcQDiagDci.registerDciStream();
        Log.d(TAG, "xtc qcom dci init.");
    }

    public void release() {
        if (mXtcQDiagDci != null) {
            mXtcQDiagDci.release();
        }
        mCallback = null;
        mXtcQDiagDci = null;
        Log.d(TAG, "xtc qcom dci release.");
    }

    private void bbatConnStatus2Pc(String status) {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put(ItemJsonKey.ITEM_KEY_TEST_ID, 34);
        format.put(ItemJsonKey.ITEM_KEY_TEST_KEY, "bbat_connect");
        format.put(ItemJsonKey.ITEM_KEY_RESULE, "mmi_pass");
        XtcSocketJsonFormat item = new XtcSocketJsonFormat();
        item.put(ItemJsonKey.ITEM_BBAT_CONN_STATUS, status);
        format.put2Array(item.build());
        send2Pc((short) 34, format.build().toString(), null);
    }

    private void doWifiTestCmd(String cmd) {
        if (TextUtils.isEmpty(cmd)) {
            Log.d(TAG, "bbatcmd cmd is null.");
            return;
        }
        String hex = cmd.replace("\r\n", "");
        Log.d(TAG, "doWifiTestCmd tmp:" + hex);
        mCurrentRfCmd = 33;
        send2InterDiag((short) 33, null, XtcConvertUtil.hex2Bytes(hex));
    }

    private boolean isNumeric(String value) {
        return Pattern.compile("[0-9]+").matcher(value).matches();
    }

    private void parseWifiDiagResult(byte[] raw) {
        String hex = XtcConvertUtil.byte2Hex(raw);
        Log.d(TAG, "parseDiagBackCmd: "
                + (raw == null ? "null" : new String(raw, 9, raw.length - 10).replace("\r\n", ""))
                + " ,hex:" + hex);
        mWifiTestResultList.add(hex);
        runWifiCmdQueue();
    }

    private void reloadWifiDriver() {
        mServiceHandler.removeMessages(MSG_RELOAD_WIFI_DRIVER);
        mServiceHandler.sendEmptyMessageDelayed(MSG_RELOAD_WIFI_DRIVER, 500L);
    }

    private void runAllWifiCmdFinish() {
        if (mWifiTestResultList.isEmpty()) {
            Log.d(TAG, "runAllWifiCmdFinish no result diag cmd.");
            return;
        }
        Log.d(TAG, "=== runAllWifiCmdFinish ===");
        isWifiCmdFinish = true;
        saveDiagResult2File(DIAG_RESULT_FILE_PATH);
        mWifiTestResultList.clear();
        reloadWifiDriver();
    }

    private void saveDiagResult2File(String filePath) {
        new XtcFileUtil().createDirIfNotExist(filePath);
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath));
            for (String line : mWifiTestResultList) {
                writer.write(line);
                writer.newLine();
            }
            writer.flush();
        } catch (Exception e) {
            Log.w(TAG, "saveDiagResult2File fail !" + e);
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean parseBbatWifiCmdFile(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            Log.d(TAG, "parseBbatWifiCmdFile fail ! bbatFilePath is null");
            return false;
        }
        isWifiCmdFinish = false;
        BufferedReader reader = null;
        boolean hasCmd = false;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!TextUtils.isEmpty(line)) {
                    mWifiCmdQueue.add(line.trim());
                    hasCmd = true;
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "readWifiConfigFile fail !" + e);
            e.printStackTrace();
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return hasCmd;
    }

    public void runRfCmd(short cmdCode, String json, byte[] raw) {
        if (cmdCode == 32) {
            mCurrentRfCmd = 32;
            send2InterDiag(cmdCode, null, raw);
        }
    }

    public void runWifiCmdQueue() {
        if (mWifiCmdQueue.isEmpty()) {
            Log.d(TAG, "bbatcmd queue is empty.");
            runAllWifiCmdFinish();
            return;
        }
        String cmd = mWifiCmdQueue.poll();
        if (TextUtils.isEmpty(cmd)) {
            Log.d(TAG, "bbatcmd cmd is null.");
            return;
        }
        if (!cmd.contains("sleep")) {
            doWifiTestCmd(cmd);
            return;
        }
        try {
            int sleepMs = Integer.parseInt(cmd.replace("sleep", "").trim());
            Log.d(TAG, "=== do cmd sleep =======" + sleepMs);
            Thread.sleep(sleepMs);
            runWifiCmdQueue();
        } catch (Exception e) {
            Log.w(TAG, "runWifiCmdQueue atCmd:" + cmd + " ,error:" + e);
        }
    }

    public void send2InterDiag(short cmdCode, String json, byte[] raw) {
        if (mXtcQDiagDci == null) {
            Log.d(TAG, "send2InterDiag fail. The mXtcQDiagDci may has release.");
        } else {
            mXtcQDiagDci.sendDciAsyncCommand(raw);
        }
    }

    public void send2Pc(short cmdCode, String json, byte[] raw) {
        mXtcPcSocket.send(cmdCode, json, raw);
    }

    public void sendReadWifiCmdSuccess2Pc() {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put(ItemJsonKey.ITEM_KEY_TEST_ID, 33);
        format.put(ItemJsonKey.ITEM_KEY_TEST_KEY, "wifi_cmd");
        format.put(ItemJsonKey.ITEM_KEY_RESULE, "mmi_pass");
        send2Pc((short) 33, format.build().toString(), null);
    }
}