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

package com.android.server.xtcdiag;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.android.server.xtcdiag.qdiag.XtcQDiagUtil;
import com.xtc.xtcdiag.IXtcDiag;
import com.xtc.xtcdiag.IXtcDiagCmdReceiveCallback;
import com.xtc.xtcdiag.IXtcDiagConnStateCallback;
import com.xtc.xtcdiag.XtcLaunchApp;
import com.xtc.xtcdiag.socket.XtcDataCallback;
import com.xtc.xtcdiag.socket.XtcFileUtil;
import com.xtc.xtcdiag.socket.XtcSocketCallback;
import com.xtc.xtcdiag.socket.XtcSocketJsonFormat;
import com.xtc.xtcdiag.socket.XtcSocketUtil;
import com.xtc.xtcdiag.wifi.WifiCipherType;
import com.xtc.xtcdiag.wifi.WifiConfig;
import com.xtc.xtcdiag.wifi.WifiUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Implementation of the XTC diag binder service. It manages the Wi-Fi AP used
 * during after-sales tests, the socket link to the PC-side diag tool and the
 * BBAT/diag baseband link.
 *
 * @hide
 */
public class XtcDiagServiceImpl extends IXtcDiag.Stub {
    private static final String TAG = "XtcDiag-Service";

    private static final String ACTION_START_SERVER = "com.xtc.diag.aftersale.start_server";
    private static final String PROP_CHARGE_USABLE_MODIFY = "persist.sys.charge.modify";
    private static final String PROP_CHARGE_USABLE = "persist.sys.charge.usable";
    private static final String WIFI_CONFIG_FILE = "/data/xtc_diag/custom_ssid.txt";

    private static final int MSG_ENABLE_WIFI = 1;
    private static final int MSG_CONNECT_WIFI = 2;
    private static final int MSG_INSMOD_WIFI_DRIVER = 3;
    private static final int MSG_RMMOD_WIFI_DRIVER = 4;
    private static final int MSG_RELOAD_WIFI_DRIVER = 5;
    private static final int MSG_INIT_QDIAG_DCI = 6;
    private static final int MSG_BBAT_START_RUN_WIFI_CMD = 7;
    private static final int MAX_RETRAY_CONN_WIFI_TIMES = 10;

    private static final int CMD_BBAT_RELEASE = 31;
    private static final int CMD_BBAT_RF_OPERATE = 32;
    private static final int CMD_BBAT_WIFI_OPERATE = 33;
    private static final int CMD_BBAT_RESTART = 34;
    private static final int CMD_SELF_TEST_OPERATE = 35;
    private static final int CMD_VALIDATION_TEST_OPERATE = 36;
    private static final int CMD_MMI_WIFI_DISCONNECT = 2002;

    private final Context mContext;
    private final Object syncObj = new Object();

    private XtcDiagHandler mDiagHandler;
    private XtcQDiagUtil mQDiagUtil;
    private WifiUtil mWifiUtil;
    private XtcSocketUtil mXtcSocketUtil;
    private WifiConfig mWifiConfig = null;
    private int mRetryConnWifiTimes = 0;

    private final RemoteCallbackList<IXtcDiagConnStateCallback> mConnStateRemoteCbs =
            new RemoteCallbackList<>();
    private final RemoteCallbackList<IXtcDiagCmdReceiveCallback> mCmdReceiveRemoteCbs =
            new RemoteCallbackList<>();

    private final XtcDataCallback mDataCallBack = new XtcDataCallback() {
        @Override
        public void onCmdReceive(short cmdCode, String json, byte[] raw) {
            switch (cmdCode) {
                case 2:
                    updateWifiConfig(json);
                    break;
                case 3:
                    stopSocketServer();
                    reloadWifiDriver();
                    break;
                case 4:
                    removeWifiConfigFile();
                    break;
                case CMD_BBAT_RELEASE:
                    releaseBbat();
                    break;
                case CMD_BBAT_RF_OPERATE:
                    initBbat();
                    if (mQDiagUtil == null) {
                        Log.w(TAG, "mDataCallBack runRfCmd but mQDiagUtil is NULL!");
                    } else {
                        mQDiagUtil.runRfCmd(cmdCode, json, raw);
                    }
                    break;
                case CMD_BBAT_WIFI_OPERATE:
                    initBbat();
                    dispatchBbatWifiCmd(json);
                    break;
                case CMD_BBAT_RESTART:
                    restartBbat();
                    break;
                case CMD_SELF_TEST_OPERATE:
                    dispatchSelfTestCode(json);
                    break;
                case CMD_VALIDATION_TEST_OPERATE:
                    dispatchValidationTestCode(json);
                    break;
                case CMD_MMI_WIFI_DISCONNECT:
                    stopSocketServer();
                    disableWifi();
                    break;
                default:
                    notifyCmdReceive(cmdCode, json, raw);
                    break;
            }
        }
    };

    private final XtcSocketCallback mSocketCallBack = new XtcSocketCallback() {
        @Override
        public void onConnectStateChange(int state, String remoteIp) {
            notifyConnStateChanage(state, remoteIp);
        }
    };

    private final HandlerThread mHandlerThread = new HandlerThread("XtcDiagSer_thread");

    private final class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_SHUTDOWN.equals(intent.getAction()) || mQDiagUtil == null) {
                return;
            }
            Log.d(TAG, "BootReceiver shutdown release dci");
            mQDiagUtil.release();
        }
    }

    class MyWifiUtil extends WifiUtil {
        MyWifiUtil(Context context) {
            super(context);
        }

        @Override
        public void wifiNetworkConnChange(String ipAddress, NetworkInfo.DetailedState detailedState) {
            if (detailedState != NetworkInfo.DetailedState.CONNECTED) {
                if (detailedState == NetworkInfo.DetailedState.DISCONNECTED) {
                    Log.d(TAG, "wifiNetworkConnChange disconnect stop socket server, thread:"
                            + Thread.currentThread());
                    stopSocketServer();
                    notifyConnStateChanage(XtcSocketCallback.STATE_DISCONNECT, "0.0.0.0");
                }
                return;
            }
            if (mWifiConfig == null) {
                return;
            }
            Log.d(TAG, "wifiNetworkConnChange configSSID:" + mWifiConfig.getSsid()
                    + " ,connectSSID:" + mWifiUtil.getSSID() + ",isSame:"
                    + mWifiUtil.isSameSSID(mWifiConfig.getSsid()));
            if (!mWifiUtil.isSameSSID(mWifiConfig.getSsid())) {
                Log.d(TAG, "wifiNetworkConnChange start reconnect to config SSID, thread:"
                        + Thread.currentThread());
                mDiagHandler.sendEmptyMessageDelayed(MSG_CONNECT_WIFI, 100L);
            } else {
                mDiagHandler.removeMessages(MSG_CONNECT_WIFI);
                Log.d(TAG, "wifiNetworkConnChange connect start socket server, thread:"
                        + Thread.currentThread() + " ,ipAddr:" + ipAddress);
                startSocketServer();
                launchMmiAppShowWifiQr();
            }
        }

        @Override
        public void wifiStateChange(int newState) {
            Log.d(TAG, "wifiStateChange newState(1:disabled, 3:enabled):" + newState + " ,this:"
                    + this + " ,thread:" + Thread.currentThread());
            if (newState == 1) {
                stopSocketServer();
            }
        }
    }

    private final class StartServerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "StartServerReceiver and set persist.sys.charge.usable true");
            startSocketServer();
            if ("false".equals(SystemProperties.get(PROP_CHARGE_USABLE, "false"))) {
                SystemProperties.set(PROP_CHARGE_USABLE, "true");
                SystemProperties.set(PROP_CHARGE_USABLE_MODIFY, "true");
            }
        }
    }

    class XtcDiagHandler extends Handler {
        XtcDiagHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "XtcDiagHandler msg.what:" + msg.what);
            switch (msg.what) {
                case MSG_ENABLE_WIFI:
                    enableWifi();
                    break;
                case MSG_CONNECT_WIFI:
                    Log.d(TAG, "start wifi connect read file");
                    connectWifi();
                    break;
                case MSG_INSMOD_WIFI_DRIVER:
                    insmodWifiDriver();
                    break;
                case MSG_RMMOD_WIFI_DRIVER:
                    rmmodWifiDriver();
                    break;
                case MSG_RELOAD_WIFI_DRIVER:
                    reloadWifiDriver();
                    break;
                case MSG_INIT_QDIAG_DCI:
                    removeMessages(MSG_INIT_QDIAG_DCI);
                    if (mQDiagUtil == null) {
                        Log.w(TAG, "MSG_INIT_QDIAG_DCI but mQDiagUtil is NULL!");
                    } else {
                        mQDiagUtil.initDci();
                    }
                    break;
                case MSG_BBAT_START_RUN_WIFI_CMD:
                    if (mQDiagUtil == null) {
                        Log.w(TAG, "MSG_BBAT_START_RUN_WIFI_CMD but mQDiagUtil is NULL!");
                    } else {
                        stopSocketServer();
                        rmmodWifiDriver();
                        mQDiagUtil.runWifiCmdQueue();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public XtcDiagServiceImpl(Context context) {
        mContext = context;
        mHandlerThread.start();
        mDiagHandler = new XtcDiagHandler(mHandlerThread.getLooper());
        mXtcSocketUtil = new XtcSocketUtil();
        mWifiUtil = new MyWifiUtil(context);
        mXtcSocketUtil.setDataCallBack(mDataCallBack);
        mXtcSocketUtil.setSocketCallBack(mSocketCallBack);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BOOT_COMPLETED);
        filter.addAction(Intent.ACTION_SHUTDOWN);
        context.registerReceiver(new BootReceiver(), filter, null, mDiagHandler);
        context.registerReceiver(new StartServerReceiver(), new IntentFilter(ACTION_START_SERVER));
        Log.d(TAG, "XtcDiagServiceImpl init");
    }

    private void connectWifi() {
        if (mWifiConfig == null) {
            Log.d(TAG, "connectWifi fail, mWifiConfig is null !");
            return;
        }
        boolean result = mWifiUtil.connectWifi(mWifiConfig.getSsid(), mWifiConfig.getPassword(),
                mWifiConfig.getCipherType());
        Log.d(TAG, "connectWifi connResult:" + result + " ,connectSSID:" + mWifiUtil.getSSID()
                + " ,mRetryConnWifiTimes:" + mRetryConnWifiTimes);
        if (result || mRetryConnWifiTimes >= MAX_RETRAY_CONN_WIFI_TIMES) {
            mDiagHandler.removeMessages(MSG_CONNECT_WIFI);
            return;
        }
        mDiagHandler.removeMessages(MSG_CONNECT_WIFI);
        mDiagHandler.sendEmptyMessageDelayed(MSG_CONNECT_WIFI, 3000L);
        mRetryConnWifiTimes++;
    }

    private void disableWifi() {
        mWifiUtil.disableConfig();
        mWifiUtil.disconnectWifi();
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {
        }
    }

    private void dispatchBbatWifiCmd(String json) {
        try {
            String cmdFileName = new JSONObject(json).getString("bbatCmdFileName");
            if (TextUtils.isEmpty(cmdFileName)) {
                Log.w(TAG, "dispatchBbatWifiCmd bbatCmdFileName is null !");
                return;
            }
            if (mQDiagUtil == null
                    || !mQDiagUtil.parseBbatWifiCmdFile(XtcQDiagUtil.DIAG_DIR_PATH + cmdFileName)) {
                Log.w(TAG, "dispatchBbatWifiCmd mQDiagUtil=" + mQDiagUtil);
                return;
            }
            mQDiagUtil.sendReadWifiCmdSuccess2Pc();
            mDiagHandler.removeMessages(MSG_BBAT_START_RUN_WIFI_CMD);
            mDiagHandler.sendEmptyMessageDelayed(MSG_BBAT_START_RUN_WIFI_CMD, 2000L);
        } catch (Exception e) {
            Log.w(TAG, "dispatchBbatWifiCmd JSON Format wrong !");
        }
    }

    private void dispatchSelfTestCode(String json) {
        new XtcLaunchApp(mContext).launchSelfTestApp();
    }

    private void dispatchValidationTestCode(String json) {
        Log.d(TAG, "dispatchValidationTestCode JSON:" + json);
        ArrayList<Integer> cmdCodeList = new ArrayList<>();
        try {
            JSONArray array = new JSONObject(json).getJSONArray("alltestItem");
            for (int i = 0; i < array.length(); i++) {
                cmdCodeList.add(array.getJSONObject(i).getInt("itemTestCode"));
            }
            Log.d(TAG, "dispatchValidationTestCode cmdCodeList:" + cmdCodeList);
            new XtcLaunchApp(mContext).launchValidationApp(cmdCodeList);
        } catch (Exception e) {
            Log.w(TAG, "dispatchSelfTestCode JSON Format wrong !");
        }
    }

    private void enableWifi() {
        if (mWifiConfig == null) {
            mWifiConfig = mWifiUtil.readWifiConfig(WIFI_CONFIG_FILE);
        }
        if (mWifiConfig == null) {
            Log.d(TAG, "readWifiConfig read fail, wifi not connected !");
            return;
        }
        registerWifiReceiver();
        mWifiUtil.enableConfig();
        mDiagHandler.removeMessages(MSG_CONNECT_WIFI);
        mDiagHandler.sendEmptyMessageDelayed(MSG_CONNECT_WIFI, 3000L);
    }

    private void initBbat() {
        if (mWifiConfig == null) {
            mWifiConfig = mWifiUtil.readWifiConfig(WIFI_CONFIG_FILE);
        }
        Log.d(TAG, "initBbat mWifiConfig:" + mWifiConfig);
        if (mWifiConfig != null && mQDiagUtil == null) {
            mQDiagUtil = new XtcQDiagUtil(mXtcSocketUtil, mDiagHandler);
        }
    }

    private void insmodWifiDriver() {
        mWifiUtil.insmodWifiDriver();
        mDiagHandler.removeMessages(MSG_ENABLE_WIFI);
        mDiagHandler.sendEmptyMessageDelayed(MSG_ENABLE_WIFI, 1000L);
    }

    private void launchMmiAppShowWifiQr() {
        if (mWifiConfig == null) {
            return;
        }
        try {
            SystemProperties.set("persist.sys.special.process", "com.xtc.mmitest");
            Settings.Secure.putInt(mContext.getContentResolver(), "lock_pattern_autolock", 1);
            Log.w(TAG, "disable lock screen for mmitest");
            new WifiCipherType();
            JSONObject object = new JSONObject();
            object.put("brightness", mWifiConfig.getBrightness());
            new XtcLaunchApp(mContext).launchMmiApp(2001, object.toString());
        } catch (Exception e) {
            Log.w(TAG, "launchMmiAppShowWifiQr json format worng");
            SystemProperties.set("persist.sys.special.process", "null");
        }
    }

    private void notify2Pc(short cmdCode, String key, boolean result) {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put("id", cmdCode);
        format.put("key", key);
        format.put("result", result ? "mmi_pass" : "mmi_fail");
        try {
            send(cmdCode, format.build().toString(), null);
        } catch (Exception e) {
            Log.w(TAG, "notify2Pc Error cmdCode:" + ((int) cmdCode) + ",ex:" + e);
        }
    }

    private void notifyChangeWifi2Pc(String ssid) {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put("id", 2);
        format.put("key", "wifi_change");
        format.put("result", "mmi_pass");
        XtcSocketJsonFormat item = new XtcSocketJsonFormat();
        item.put("ssid", ssid);
        format.put2Array(item.build());
        try {
            send(2, format.build().toString(), null);
        } catch (Exception e) {
            Log.w(TAG, "notifyChangeWifi2Pc ex:" + e);
        }
    }

    private void notifyCmdReceive(int cmdCode, String json, byte[] raw) {
        synchronized (syncObj) {
            int count = mCmdReceiveRemoteCbs.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    mCmdReceiveRemoteCbs.getBroadcastItem(i).onCmdReceive(cmdCode, json, raw);
                } catch (RemoteException e) {
                    Log.d(TAG, "notifyCmdReceive ex:" + e);
                }
            }
            mCmdReceiveRemoteCbs.finishBroadcast();
        }
    }

    private void notifyConnStateChanage(int state, String remoteIp) {
        synchronized (syncObj) {
            int count = mConnStateRemoteCbs.beginBroadcast();
            for (int i = 0; i < count; i++) {
                try {
                    mConnStateRemoteCbs.getBroadcastItem(i).onConnectStateChange(state, remoteIp);
                } catch (RemoteException e) {
                    Log.d(TAG, "notifyConnStateChanage ex:" + e);
                }
            }
            mConnStateRemoteCbs.finishBroadcast();
        }
    }

    private void recoveryProp() {
        if ("true".equals(SystemProperties.get(PROP_CHARGE_USABLE_MODIFY, "false"))) {
            SystemProperties.set(PROP_CHARGE_USABLE, "false");
            SystemProperties.set(PROP_CHARGE_USABLE_MODIFY, "false");
        }
    }

    private void registerWifiReceiver() {
        mWifiUtil.unregisterAllReceiver();
        mWifiUtil.registerAllReceiver();
    }

    private void releaseBbat() {
        if (mQDiagUtil == null) {
            mQDiagUtil = new XtcQDiagUtil(mXtcSocketUtil, mDiagHandler);
        }
        mQDiagUtil.release();
        mQDiagUtil = null;
        notify2Pc((short) CMD_BBAT_RELEASE, "release_bbat", true);
    }

    private void reloadWifiDriver() {
        mDiagHandler.removeMessages(MSG_RMMOD_WIFI_DRIVER);
        mDiagHandler.removeMessages(MSG_INSMOD_WIFI_DRIVER);
        mDiagHandler.removeMessages(MSG_RELOAD_WIFI_DRIVER);
        mDiagHandler.sendEmptyMessageDelayed(MSG_RMMOD_WIFI_DRIVER, 500L);
        mDiagHandler.sendEmptyMessageDelayed(MSG_INSMOD_WIFI_DRIVER, 2000L);
    }

    private void removeWifiConfigFile() {
        notify2Pc((short) 4, "remove_wifi_config",
                new XtcFileUtil().deleteFile(WIFI_CONFIG_FILE));
    }

    private void restartBbat() {
        if (mWifiConfig == null) {
            mWifiConfig = mWifiUtil.readWifiConfig(WIFI_CONFIG_FILE);
        }
        Log.d(TAG, "restartBbat mWifiConfig:" + mWifiConfig);
        if (mWifiConfig == null) {
            return;
        }
        if (mQDiagUtil == null) {
            mQDiagUtil = new XtcQDiagUtil(mXtcSocketUtil, mDiagHandler);
        } else {
            mQDiagUtil.release();
            mDiagHandler.sendEmptyMessageDelayed(MSG_INIT_QDIAG_DCI, 1500L);
        }
    }

    private void rmmodWifiDriver() {
        disableWifi();
        mWifiUtil.rmmodWifiDriver();
    }

    private void updateWifiConfig(String json) {
        String ssid = null;
        String password = null;
        String cipher = null;
        int brightness = 0;
        try {
            JSONObject object = new JSONObject(json);
            ssid = object.getString("ssid");
            password = object.getString("pwd");
            cipher = object.getString("cipherType");
            brightness = object.getInt("brightness");
        } catch (Exception e) {
            Log.w(TAG, "rewriteWifiConfig JSON Format wrong !");
        }
        if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
            Log.w(TAG, "rewriteWifiConfig fail ! ssid:" + ssid + " ,pwd:" + password);
            return;
        }
        WifiConfig config = new WifiConfig();
        WifiCipherType cipherType = new WifiCipherType();
        config.setSsid(ssid);
        config.setPassword(password);
        config.setCipherType(cipherType.convert2CipherType(cipher));
        config.setBrightness(brightness);
        if (!mWifiUtil.rewriteWifiConfigFile(WIFI_CONFIG_FILE, config)) {
            Log.w(TAG, "rewriteWifiConfig updateRes fail !");
        } else {
            mWifiConfig = config;
            notifyChangeWifi2Pc(ssid);
        }
    }

    @Override
    public boolean isSocketConnected() {
        return mXtcSocketUtil.isSocketConnected();
    }

    @Override
    public void registerCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback) {
        synchronized (syncObj) {
            mCmdReceiveRemoteCbs.register(callback);
        }
    }

    @Override
    public void registerConnStateCallBack(IXtcDiagConnStateCallback callback) {
        synchronized (syncObj) {
            mConnStateRemoteCbs.register(callback);
        }
    }

    @Override
    public void send(int cmdCode, String json, byte[] raw) {
        Log.d(TAG, "send cmdCode:" + cmdCode + " ,json:" + json + " ,raw:" + raw);
        mXtcSocketUtil.send((short) cmdCode, json, raw);
    }

    @Override
    public void sendFile(String path, String fileName, String receiver, int cmd) {
        mXtcSocketUtil.sendFile(path, fileName, receiver, cmd);
    }

    @Override
    public void startSocketClient(String serverAddr) {
        mXtcSocketUtil.startClient(serverAddr);
        Log.d(TAG, "startSocketClient serverIp:" + serverAddr);
    }

    @Override
    public void startSocketServer() {
        mXtcSocketUtil.startServer();
        Log.d(TAG, "startSocketServer");
    }

    @Override
    public void stopSocketClient() {
        mXtcSocketUtil.stopClient();
        Log.d(TAG, "stopSocketClient");
    }

    @Override
    public void stopSocketServer() {
        mXtcSocketUtil.stopServer();
        Log.d(TAG, "stopSocketServer");
    }

    public void systemReady() {
        Log.d(TAG, "systemReady ok");
        mDiagHandler.sendEmptyMessageDelayed(MSG_ENABLE_WIFI, 1000L);
        recoveryProp();
    }

    @Override
    public void unregisterCmdReceiveCallBack(IXtcDiagCmdReceiveCallback callback) {
        synchronized (syncObj) {
            mCmdReceiveRemoteCbs.unregister(callback);
        }
    }

    @Override
    public void unregisterConnStateCallBack(IXtcDiagConnStateCallback callback) {
        synchronized (syncObj) {
            mConnStateRemoteCbs.unregister(callback);
        }
    }
}