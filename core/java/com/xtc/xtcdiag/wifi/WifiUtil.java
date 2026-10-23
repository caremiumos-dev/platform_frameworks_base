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

package com.xtc.xtcdiag.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

/**
 * Wi-Fi control used by the diag service to bring up the after-sales test AP.
 *
 * @hide
 */
public class WifiUtil {
    private static final String TAG = "XtcDiag-WifiUtil";

    public static final int DELAY_TIME = 5000;
    public static final String ERROR_IP = "0.0.0.0";
    public static final String WIFI_CONFIG_FILE = "/data/xtc_diag/custom_ssid.txt";

    private static final String PRO_INSMODE_WIIF_DRIVER = "log.insmod.wifi.driver";

    private Context mContext;
    private WifiManager mWifiManager;
    private int mLastCount = 0;

    private WifiStateChangeReceiver mWifiStateChangeReceiver = null;
    private WifiScanReceiver mWifiScanReceiver = null;
    private WifiNetworkReceiver mWifiNetworkReceiver = null;
    private StartScanThread mStartScanThread = null;

    class StartScanThread extends Thread {
        @Override
        public void run() {
            try {
                SystemClock.sleep(DELAY_TIME);
                Log.d(TAG, "StartScanThread start scan wifi");
                mWifiManager.startScan();
            } catch (Exception e) {
            }
        }
    }

    private class WifiNetworkReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    String ssid = getSSID();
                    String ipAddress = getIpAddress();
                    wifiNetworkConnChange(ipAddress, NetworkInfo.DetailedState.CONNECTED);
                    Log.d(TAG, "WifiNetworkReceiver ipAddr:" + ipAddress + ",ssid:" + ssid);
                } else if (networkInfo.getDetailedState()
                        == NetworkInfo.DetailedState.DISCONNECTED) {
                    Log.d(TAG, "WifiNetworkReceiver wifi disconnected");
                    wifiNetworkConnChange(null, NetworkInfo.DetailedState.DISCONNECTED);
                }
            }
        }
    }

    private class WifiScanReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                return;
            }
            List<ScanResult> scanResults = mWifiManager.getScanResults();
            if (scanResults == null || scanResults.size() == mLastCount) {
                return;
            }
            wifiDeviceListChange(scanResults);
            mLastCount = scanResults.size();
            Log.w(TAG, "WifiScanReceiver size:" + mLastCount);
        }
    }

    private class WifiStateChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int newState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            intent.getIntExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE, -1);
            int wifiState = mWifiManager.getWifiState();
            if (newState == WifiManager.WIFI_STATE_ENABLED) {
                wifiStartDiscovery();
            }
            wifiStateChange(wifiState);
        }
    }

    public WifiUtil(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
    }

    private synchronized boolean connectWifi(WifiConfiguration config) {
        if (mWifiManager == null) {
            return false;
        }
        Log.d(TAG, "connectWifi wifi enable? " + mWifiManager.isWifiEnabled());
        if (!mWifiManager.isWifiEnabled()) {
            enableWifi(true);
            for (int i = 0; mWifiManager.getWifiState() != WifiManager.WIFI_STATE_ENABLED && i < 5;
                    i++) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                }
            }
            if (!mWifiManager.isWifiEnabled()) {
                Log.w(TAG, "connect wifi fail ! can not open wifi.");
                return false;
            }
        }
        int netId = mWifiManager.addNetwork(config);
        Log.d(TAG, "connectWifi netId:" + netId + " ,config:" + config);
        if (netId == -1) {
            return false;
        }
        return mWifiManager.enableNetwork(netId, true) ? mWifiManager.reconnect() : false;
    }

    private WifiConfiguration createWifiInfo(String ssid, String password,
            WifiCipherType.CipherType cipherType) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + ssid + "\"";
        switch (cipherType) {
            case WIFICIPHER_NOPASS:
                config.wepKeys[0] = "";
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                return config;
            case WIFICIPHER_WEP:
                config.preSharedKey = "\"" + password + "\"";
                config.hiddenSSID = true;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.wepTxKeyIndex = 0;
                return config;
            case WIFICIPHER_WPA:
                config.preSharedKey = "\"" + password + "\"";
                config.hiddenSSID = true;
                config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                return config;
            default:
                return config;
        }
    }

    private void enableAlwaysScan(boolean enable) {
        Settings.Global.putInt(mContext.getContentResolver(),
                Settings.Global.WIFI_SCAN_ALWAYS_AVAILABLE, enable ? 1 : 0);
    }

    private String int2StrIp(int ip) {
        if (ip == 0) {
            return null;
        }
        return (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "."
                + ((ip >> 24) & 0xff);
    }

    private WifiConfiguration isExist(String ssid) {
        if (mWifiManager == null) {
            return null;
        }
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        if (configuredNetworks == null) {
            return null;
        }
        Log.d(TAG, "isExist wifiConfig size: " + configuredNetworks.size());
        for (WifiConfiguration config : configuredNetworks) {
            if (config.SSID.equals("\"" + ssid + "\"")) {
                return config;
            }
        }
        return null;
    }

    private void setXtcWifiProp(boolean enable) {
        String value = enable ? "true" : "false";
        SystemProperties.set("persist.sys.wifi_visible_status", value);
        SystemProperties.set("persist.sys.wifi_user_operate", value);
        SystemProperties.set("persist.sys.bt.enable", enable ? "false" : "true");
        SystemProperties.set("persist.sys.wifi.disable.assist", value);
        SystemProperties.set("persist.sys.wificonnect.enable", value);
    }

    private void wifiStartDiscovery() {
        if (mWifiManager == null) {
            return;
        }
        if (mStartScanThread != null) {
            mStartScanThread.interrupt();
            mStartScanThread = null;
        }
        mStartScanThread = new StartScanThread();
        mStartScanThread.start();
    }

    public void clearWifiInfo() {
        if (mWifiManager == null) {
            return;
        }
        try {
            WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
            final int networkId = connectionInfo.getNetworkId();
            if (connectionInfo != null && networkId >= 0) {
                mWifiManager.forget(networkId, new WifiManager.ActionListener() {
                    @Override
                    public void onFailure(int reason) {
                        Log.d(TAG, "clearWifiInfo forget fail networkId= " + networkId
                                + " , reason:" + reason);
                    }

                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "clearWifiInfo forget success networkId= " + networkId);
                    }
                });
            }
            for (int i = 0; i < 20; i++) {
                mWifiManager.removeNetwork(i);
            }
        } catch (Exception e) {
        } finally {
            mWifiManager.saveConfiguration();
            enableWifi(false);
        }
    }

    public boolean connectWifi(String ssid, String password,
            WifiCipherType.CipherType cipherType) {
        if (mWifiManager == null) {
            return false;
        }
        WifiConfiguration config = createWifiInfo(ssid, password, cipherType);
        if (config == null) {
            return false;
        }
        Log.d(TAG, "connectWifi isWifiConnected:" + isWifiConnected() + " ,isSameSSID:"
                + isSameSSID(ssid) + " ,ip:" + getIpAddress());
        if (isWifiConnected() && isSameSSID(ssid) && !ERROR_IP.equals(getIpAddress())) {
            Log.d(TAG, "the wifi(" + ssid + ") has connected ! we return direct.");
            return true;
        }
        disconnectWifi();
        Log.d(TAG, "connectWifi SSID:" + ssid + " ,password:" + password);
        WifiConfiguration existing = isExist(ssid);
        if (existing != null) {
            Log.d(TAG, "connectWifi remove id:" + existing.networkId);
            mWifiManager.removeNetwork(existing.networkId);
        }
        return connectWifi(config);
    }

    public void disableConfig() {
        enableAlwaysScan(false);
        clearWifiInfo();
        setXtcWifiProp(false);
    }

    public boolean disconnectWifi() {
        if (mWifiManager != null && mWifiManager.isWifiEnabled()) {
            return mWifiManager.disconnect();
        }
        return false;
    }

    public void enableConfig() {
        setXtcWifiProp(true);
        Log.d(TAG, "enableConfig wifiEnable: " + mWifiManager.isWifiEnabled());
        if (mWifiManager.isWifiEnabled()) {
            wifiStateChange(WifiManager.WIFI_STATE_ENABLED);
            wifiStartDiscovery();
        } else {
            wifiStateChange(WifiManager.WIFI_STATE_DISABLED);
            enableWifi(true);
        }
    }

    public boolean enableWifi(boolean enable) {
        if (mWifiManager == null) {
            return false;
        }
        Log.d(TAG, "enableWifi --enable:" + enable);
        return mWifiManager.setWifiEnabled(enable);
    }

    public String getIpAddress() {
        if (mWifiManager == null) {
            return null;
        }
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        if (connectionInfo == null) {
            return null;
        }
        return int2StrIp(connectionInfo.getIpAddress());
    }

    public String getSSID() {
        if (mWifiManager == null) {
            return null;
        }
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();
        if (connectionInfo == null) {
            return null;
        }
        return connectionInfo.getSSID();
    }

    public WifiManager getWifiManager() {
        return mWifiManager;
    }

    public void insmodWifiDriver() {
    }

    public boolean isSameSSID(String ssid) {
        if (TextUtils.isEmpty(ssid)) {
            return false;
        }
        if (!ssid.startsWith("\"")) {
            ssid = "\"" + ssid + "\"";
        }
        return ssid.equals(getSSID());
    }

    public boolean isWifiConnected() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo == null) {
            return false;
        }
        return networkInfo.isConnected();
    }

    public WifiConfig readWifiConfig(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            Log.d(TAG, "readWifiConfigFile fail ! filePath is null");
            return null;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            if (TextUtils.isEmpty(line)) {
                return null;
            }
            String[] parts = line.split("/r/n");
            if (parts == null || parts.length == 0) {
                return null;
            }
            String ssid = parts[0];
            String password = parts.length > 1 ? parts[1] : null;
            String cipher = parts.length > 2 ? parts[2] : null;
            String brightnessStr = parts.length > 3 ? parts[3] : null;
            int brightness = brightnessStr != null ? Integer.parseInt(brightnessStr) : 0;
            Log.d(TAG, "readWifiConfigFile ssid:" + ssid + " ,pwd:" + password + " ,type:" + cipher
                    + " ,brightness:" + brightness);
            if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
                Log.d(TAG, "readWifiConfigFile fail ! ssid:" + ssid + " ,pwd:" + password);
                return null;
            }
            WifiConfig config = new WifiConfig();
            WifiCipherType cipherType = new WifiCipherType();
            config.setSsid(ssid);
            config.setPassword(password);
            config.setCipherType(cipherType.convert2CipherType(cipher));
            config.setBrightness(brightness);
            return config;
        } catch (Exception e) {
            Log.w(TAG, "!!! readWifiConfigFile fail !" + e);
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    Log.w(TAG, "!!! readWifiConfigFile fail !" + e);
                }
            }
        }
    }

    public void registerAllReceiver() {
        Log.d(TAG, "registerAllReceiver thread: " + Thread.currentThread());
        mWifiStateChangeReceiver = new WifiStateChangeReceiver();
        mContext.registerReceiver(mWifiStateChangeReceiver,
                new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        mWifiScanReceiver = new WifiScanReceiver();
        mContext.registerReceiver(mWifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mWifiNetworkReceiver = new WifiNetworkReceiver();
        mContext.registerReceiver(mWifiNetworkReceiver,
                new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION));
    }

    public boolean rewriteWifiConfigFile(String filePath, WifiConfig config) {
        if (TextUtils.isEmpty(filePath) || config == null) {
            Log.d(TAG, "updateWifiConfigFile fail ! filePath:" + filePath + " ,config:" + config);
            return false;
        }
        WifiCipherType cipherType = new WifiCipherType();
        String ssid = config.getSsid();
        String password = config.getPassword();
        String cipher = cipherType.convert2Str(config.getCipherType());
        int brightness = config.getBrightness();
        if (TextUtils.isEmpty(ssid) || TextUtils.isEmpty(password)) {
            Log.d(TAG, "updateWifiConfigFile fail ! ssid:" + ssid + " ,pwd:" + password);
            return false;
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(ssid).append("/r/n").append(password).append("/r/n").append(cipher);
        if (brightness > 0) {
            buffer.append("/r/n").append(brightness);
        }
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filePath, false));
            writer.write(buffer.toString());
            writer.flush();
            Log.d(TAG, "updateWifiConfigFile success ! filePath:" + filePath + " ,str:"
                    + buffer.toString());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "!!! updateWifiConfigFile fail !" + e);
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception e) {
                    Log.w(TAG, "!!! updateWifiConfigFile fail !" + e);
                }
            }
        }
    }

    public void rmmodWifiDriver() {
        SystemProperties.set(PRO_INSMODE_WIIF_DRIVER, "0");
    }

    public void unregisterAllReceiver() {
        Log.d(TAG, "unregisterAllReceiver thread: " + Thread.currentThread());
        if (mWifiStateChangeReceiver != null) {
            mContext.unregisterReceiver(mWifiStateChangeReceiver);
            mWifiStateChangeReceiver = null;
        }
        if (mWifiScanReceiver != null) {
            mContext.unregisterReceiver(mWifiScanReceiver);
            mWifiScanReceiver = null;
        }
        if (mWifiNetworkReceiver != null) {
            mContext.unregisterReceiver(mWifiNetworkReceiver);
            mWifiNetworkReceiver = null;
        }
    }

    public void wifiDeviceListChange(List<ScanResult> scanResults) {
    }

    public void wifiDiscoveryFinished() {
    }

    public void wifiNetworkConnChange(String ipAddress, NetworkInfo.DetailedState detailedState) {
    }

    public void wifiStateChange(int state) {
    }
}