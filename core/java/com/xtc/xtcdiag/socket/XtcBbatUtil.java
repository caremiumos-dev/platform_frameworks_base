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

package com.xtc.xtcdiag.socket;

import android.os.Handler;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.android.ims.ImsConferenceState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Drives the BBAT/diag baseband tool: queues AT/Wi-Fi test commands, collects the
 * results and relays them to the PC-side diag tool.
 *
 * @hide
 */
public class XtcBbatUtil {
    private static final String TAG = "XtcDiag-BBAT";

    public static final String DIAG_DIR_PATH = "/data/xtc_diag/";
    public static final int INTER_DIAG_SOCKET_PORT = 10000;

    private static final String PRO_BBAT_SERVICE_ENABLE = "log.bbat.service.enable";
    private static final String DIAG_RESULT_FILE_PATH = "/data/xtc_diag/diag_result_2_pc.txt";
    private static final int MSG_INSMOD_WIFI_DRIVER = 3;

    private Handler mServiceHandler;
    private XtcSocketUtil mXtcPcSocket;

    private final ConcurrentLinkedQueue<String> mWifiCmdQueue = new ConcurrentLinkedQueue<>();
    private final CopyOnWriteArrayList<String> mWifiTestResultList = new CopyOnWriteArrayList<>();
    private boolean isWifiCmdFinish = false;

    private final XtcDataCallback mDataCallBack = new XtcDataCallback() {
        @Override
        public void onCmdReceive(short cmdCode, String json, byte[] raw) {
            if (cmdCode == XtcSocketCmdCode.CMD_BBAT_RF_OPERATE) {
                send2Pc(cmdCode, json, raw);
            } else if (cmdCode == XtcSocketCmdCode.CMD_BBAT_WIFI_OPERATE) {
                if (isWifiCmdFinish) {
                    Log.d(TAG, "has receive more wifi diag result msg when finish.");
                } else {
                    parseWifiDiagResult(raw);
                }
            }
        }
    };

    private final XtcSocketCallback mSocketCallBack = new XtcSocketCallback() {
        @Override
        public void onConnectStateChange(int state, String remoteIp) {
            Log.d(TAG, "bbat socket onConnectStateChange:" + state);
            if (state == STATE_CONNECTED) {
                bbatConnStatus2Pc("connected");
            } else if (state == STATE_DISCONNECT) {
                bbatConnStatus2Pc(ImsConferenceState.STATUS_DISCONNECTED);
            }
        }
    };

    private final XtcBbatSocketHandler mBBatSocketHandler = new XtcBbatSocketHandler();
    private final XtcSocketUtil mXtcBbatSocket =
            new XtcSocketUtil(INTER_DIAG_SOCKET_PORT, mBBatSocketHandler);

    public XtcBbatUtil(XtcSocketUtil pcSocket, Handler handler) {
        mXtcPcSocket = pcSocket;
        mServiceHandler = handler;
        mXtcBbatSocket.setDataCallBack(mDataCallBack);
        mXtcBbatSocket.setSocketCallBack(mSocketCallBack);
    }

    private void bbatConnStatus2Pc(String status) {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put(ItemJsonKey.ITEM_KEY_TEST_ID, XtcSocketCmdCode.CMD_BBAT_RESTART);
        format.put(ItemJsonKey.ITEM_KEY_TEST_KEY, "bbat_connect");
        format.put(ItemJsonKey.ITEM_KEY_RESULE, "mmi_pass");
        XtcSocketJsonFormat item = new XtcSocketJsonFormat();
        item.put(ItemJsonKey.ITEM_BBAT_CONN_STATUS, status);
        format.put2Array(item.build());
        send2Pc(XtcSocketCmdCode.CMD_BBAT_RESTART, format.build().toString(), null);
    }

    private void doWifiTestCmd(String cmd) {
        if (TextUtils.isEmpty(cmd)) {
            Log.d(TAG, "bbatcmd cmd is null.");
            return;
        }
        String line = !cmd.endsWith("\r\n") ? cmd + "\r\n" : cmd;
        mBBatSocketHandler.setBBatCmdType(XtcSocketCmdCode.CMD_BBAT_WIFI_OPERATE);
        byte[] packet = new XtcBbatSocketMsg().buildDiagCmdPkg(XtcBbatSocketHeader.PKG_TAG, 0,
                XtcBbatSocketHeader.CMD_TYPE, XtcBbatSocketHeader.CMD_SUB_TYPE, line).array();
        Log.d(TAG, "=== doWifiTestCmd:" + line.replace("\r\n", ""));
        send2InterDiag(XtcSocketCmdCode.CMD_BBAT_WIFI_OPERATE, null, packet);
    }

    private void insmodWifiDriver() {
        mServiceHandler.removeMessages(MSG_INSMOD_WIFI_DRIVER);
        mServiceHandler.sendEmptyMessageDelayed(MSG_INSMOD_WIFI_DRIVER, 500L);
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
        if ("7e000000000e009c000d0a4f4b0d0a7e".equals(hex)) {
            runWifiCmdQueue();
        }
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
        insmodWifiDriver();
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
        if (cmdCode == XtcSocketCmdCode.CMD_BBAT_RF_OPERATE) {
            mBBatSocketHandler.setBBatCmdType(XtcSocketCmdCode.CMD_BBAT_RF_OPERATE);
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
        if (!isNumeric(cmd)) {
            doWifiTestCmd(cmd);
            return;
        }
        try {
            int sleepMs = Integer.parseInt(cmd);
            Log.d(TAG, "=== do cmd sleep =======" + sleepMs);
            Thread.sleep(sleepMs);
            runWifiCmdQueue();
        } catch (Exception e) {
            Log.w(TAG, "runWifiCmdQueue atCmd:" + cmd + " ,error:" + e);
        }
    }

    public void send2InterDiag(short cmdCode, String json, byte[] raw) {
        mXtcBbatSocket.send(cmdCode, json, raw);
    }

    public void send2Pc(short cmdCode, String json, byte[] raw) {
        mXtcPcSocket.send(cmdCode, json, raw);
    }

    public void sendReadWifiCmdSuccess2Pc() {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put(ItemJsonKey.ITEM_KEY_TEST_ID, XtcSocketCmdCode.CMD_BBAT_WIFI_OPERATE);
        format.put(ItemJsonKey.ITEM_KEY_TEST_KEY, "wifi_cmd");
        format.put(ItemJsonKey.ITEM_KEY_RESULE, "mmi_pass");
        send2Pc(XtcSocketCmdCode.CMD_BBAT_WIFI_OPERATE, format.build().toString(), null);
    }

    public void startBbatService() {
        SystemProperties.set(PRO_BBAT_SERVICE_ENABLE, "1");
    }

    public void startBbatSocket() {
        mXtcBbatSocket.startClient("127.0.0.1");
        Log.d(TAG, "startBbatSocket");
    }

    public void stopBbatService() {
        SystemProperties.set(PRO_BBAT_SERVICE_ENABLE, "0");
    }

    public void stopBbatSocket() {
        mXtcBbatSocket.stopClient();
        Log.d(TAG, "stopBbatSocket");
    }
}