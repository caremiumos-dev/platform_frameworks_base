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

package com.xtc.secure.pm;

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.behavior.XtcBehavior;
import com.xtc.moduleswitch.ModuleSwitch;
import com.xtc.secure.XtcAppSignatureUtil;
import com.xtc.secure.cipher.RSAUtil;
import com.xtc.secure.walle.ChannelInfo;
import com.xtc.secure.walle.ChannelReader;
import com.xtc.xtcdiag.socket.XtcConvertUtil;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * XTC APK install verification against the channel/extra info embedded in the
 * APK signing block and a hard-coded public key.
 *
 * <p>WARNING / TRUST-MODEL CONDEMNATION: this path restricts installation based
 * on a vendor-controlled signature whitelist and a hard-coded RSA public key
 * embedded in the framework, and it silently reports verification outcomes to a
 * remote behaviour-collection service. That is a vendor lock-in / censorship
 * mechanism that discriminates against third-party applications; it is
 * reproduced only for device-compatibility and must not be considered a
 * meaningful security boundary.
 *
 * @hide
 */
public class AppInstallerVerify {
    private static final String TAG = "AppInstallerVerify";

    private static final int ADB_CONTROL_MODULE_SWITCH = 105;

    private static final String CHARSET_UTF8 = "UTF-8";
    private static final String PROP_BIND_NUM = "ro.boot.bindnumber";

    private static final String SECURE_KEY_AUTH_CODE = "authCode";
    private static final String SECURE_KEY_BIND_NUM_LIST = "bindnum";
    private static final String SECURE_KEY_DEBUG_APK = "isDebug";
    private static final String SECURE_KEY_ENCRYPT_SUMMARY_INFO = "encryptSummaryInfo";
    private static final String SECURE_KEY_KEY_VERSION = "keyVersion";
    private static final String SECURE_KEY_PACKAGE_NAME = "packageName";
    private static final String SECURE_KEY_SIGNATURE_LIST = "signature";
    private static final String SECURE_XTC_CHANNEL_NAME = "xtc";

    private static final int VERIFY_CODE_LOCAL_SUCCESS = 2;
    private static final int VERIFY_CODE_APK_PATH_INVALID = 3;
    private static final int VERIFY_CODE_INSTALLER_UID_INVALID = 4;
    private static final int VERIFY_CODE_105_INVALID = 5;
    private static final int VERIFY_CODE_CHANNEL_INFO_INVALID = 6;
    private static final int VERIFY_CODE_EXT_INFO_INVALID = 7;
    private static final int VERIFY_CODE_LOCAL_VERIFY_FAIL = 8;
    private static final int VERIFY_CODE_APK_SIGN_MATCH_FAIL = 9;
    private static final int VERIFY_CODE_LAST_VERIFY_FAIL = 10;

    private final Context mContext;

    public AppInstallerVerify(Context context) {
        mContext = context;
    }

    private boolean isAdbModuleSwitchOpen() {
        return ModuleSwitch.queryModuleSwitch(mContext, ADB_CONTROL_MODULE_SWITCH, false);
    }

    private boolean isSignInServerList(String serverSignatureList, String apkPath) {
        if (TextUtils.isEmpty(serverSignatureList) || TextUtils.isEmpty(apkPath)) {
            return false;
        }
        String upperList = serverSignatureList.toUpperCase();
        List<byte[]> signatureSHA;
        try {
            signatureSHA = XtcAppSignatureUtil.getSignatureSHA(
                    XtcAppSignatureUtil.getApkSignatures(mContext, apkPath));
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "isSignInServerList sha error", e);
            return false;
        }
        if (signatureSHA == null || signatureSHA.isEmpty()) {
            Log.d(TAG, "isSignInServerList the local appCertHashes is null.");
            return false;
        }
        for (byte[] sha : signatureSHA) {
            String hex = XtcConvertUtil.byte2Hex(sha);
            if (!TextUtils.isEmpty(hex) && upperList.contains(hex.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean verifyXtcSecureData(File apkFile, int installerUid) {
        if (apkFile == null || !apkFile.exists()) {
            return false;
        }
        String apkPath = apkFile.getAbsolutePath();
        ChannelInfo channelInfo = ChannelReader.get(apkFile);
        if (channelInfo == null) {
            Log.d(TAG, "verify fail: ChannelInfo is null. maybe not encrypt by xtc server.");
            sendVerifyBehavior(apkPath, VERIFY_CODE_CHANNEL_INFO_INVALID, "channel invalid",
                    installerUid);
            return false;
        }
        Log.d(TAG, "verifyXtcSecureData channelInfo:" + channelInfo.getChannel());
        Map<String, String> extraInfo = channelInfo.getExtraInfo();
        if (extraInfo == null || extraInfo.isEmpty()) {
            Log.d(TAG, "verify fail: ChannelInfo extInfo is null");
            sendVerifyBehavior(apkPath, VERIFY_CODE_EXT_INFO_INVALID, "extInfo invalid",
                    installerUid);
            return false;
        }
        boolean isDebugApk = Boolean.parseBoolean(extraInfo.get(SECURE_KEY_DEBUG_APK));
        int keyVersion;
        try {
            keyVersion = Integer.parseInt(extraInfo.get(SECURE_KEY_KEY_VERSION));
        } catch (NumberFormatException e) {
            keyVersion = 1;
        }
        String authCode = extraInfo.get(SECURE_KEY_AUTH_CODE);
        String signatureList = extraInfo.get(SECURE_KEY_SIGNATURE_LIST);
        String packageName = extraInfo.get(SECURE_KEY_PACKAGE_NAME);
        String bindNumList = extraInfo.get(SECURE_KEY_BIND_NUM_LIST);
        String encryptSummaryInfo = extraInfo.get(SECURE_KEY_ENCRYPT_SUMMARY_INFO);
        Log.d(TAG, "isDebugApk:" + isDebugApk + ", keyVersion:" + keyVersion + ", authCode:"
                + authCode + ", signatureList:" + signatureList + ", packageName:" + packageName
                + ", bindnumList:" + bindNumList + ", encryptSummaryInfo:" + encryptSummaryInfo);
        StringBuffer buffer = new StringBuffer();
        if (!TextUtils.isEmpty(signatureList)) {
            buffer.append(signatureList);
        }
        buffer.append(keyVersion);
        if (!TextUtils.isEmpty(packageName)) {
            buffer.append(packageName);
        }
        buffer.append(isDebugApk);
        if (!TextUtils.isEmpty(bindNumList)) {
            buffer.append(bindNumList);
        }
        if (!TextUtils.isEmpty(authCode)) {
            buffer.append(authCode);
        }
        String sha256Value = RSAUtil.generateSHA256Signature(buffer.toString());
        Log.d(TAG, "sha256Value:" + sha256Value);
        boolean verified = false;
        try {
            verified = RSAUtil.verifySignature(sha256Value, encryptSummaryInfo,
                    RSAUtil.getPublicKey(keyVersion), CHARSET_UTF8);
        } catch (Exception e) {
            // Verification failed; handled below.
        }
        if (!verified) {
            Log.d(TAG, "verify fail: verify local and server not match ! "
                    + "the app not allowed to install.");
            sendVerifyBehavior(apkPath, VERIFY_CODE_LOCAL_VERIFY_FAIL, "local verify fail",
                    installerUid);
            return false;
        }
        if (!isSignInServerList(signatureList, apkPath)) {
            Log.d(TAG, "verify fail: the Signature SHA Not equal to local.");
            sendVerifyBehavior(apkPath, VERIFY_CODE_APK_SIGN_MATCH_FAIL, "sign not match",
                    installerUid);
            return false;
        }
        String localPackageName = new ApkInfoUtil(mContext).getPackgeName(apkPath);
        if (packageName == null || !packageName.equalsIgnoreCase(localPackageName)) {
            Log.d(TAG, "verify fail: the PackageName Not equal to local.");
            return false;
        }
        if (!isDebugApk) {
            Log.d(TAG, "verify success: is release apk.");
            sendVerifyBehavior(apkPath, VERIFY_CODE_LOCAL_SUCCESS, "debug apk", installerUid);
            return true;
        }
        if (TextUtils.isEmpty(bindNumList)) {
            Log.d(TAG, "verify fail: the server bindnumList is null.");
            return false;
        }
        String bindNum = SystemProperties.get(PROP_BIND_NUM);
        if (TextUtils.isEmpty(bindNum)) {
            Log.d(TAG, "verify fail: the bindnum prop value is null.");
            return false;
        }
        String lowerList = bindNumList.toLowerCase();
        String lowerBindNum = bindNum.toLowerCase();
        Log.d(TAG, "server bindnumList:" + lowerList + ", bindnum:" + lowerBindNum);
        if (lowerList.contains(lowerBindNum)) {
            Log.d(TAG, "verify success: the bindnum is in server bindnumList");
            sendVerifyBehavior(apkPath, VERIFY_CODE_LOCAL_SUCCESS, "bindnum in list", installerUid);
            return true;
        }
        Log.d(TAG, "verify fail: verifyXtcSecureData not match.");
        sendVerifyBehavior(apkPath, VERIFY_CODE_LAST_VERIFY_FAIL, "verify fail", installerUid);
        return false;
    }

    public void sendVerifyBehavior(String apkPath, int result, String reason, int installerUid) {
        int installFrom = 0;
        LinkedHashMap<String, String> extend = new LinkedHashMap<>();
        String packageName = new ApkInfoUtil(mContext).getPackgeName(apkPath);
        if (installerUid == 1000) {
            installFrom = 1;
        } else if (installerUid == 2000 || installerUid != 0) {
            installFrom = 2;
        }
        if (TextUtils.isEmpty(packageName)) {
            packageName = "none";
        }
        extend.put(SECURE_KEY_PACKAGE_NAME, packageName);
        extend.put("result", String.valueOf(result));
        extend.put("reason", reason);
        extend.put("installFrom", String.valueOf(installFrom));
        extend.put("origin", StorageManager.UUID_SYSTEM);
        new XtcBehavior.Builder(mContext, "Operate_AppUpdate_AppVerify", extend)
                .build().sendData();
    }

    public boolean verify(String apkPath, int installerUid) {
        Log.d(TAG, "verify: apkFilePath:" + apkPath + ", installerUid:" + installerUid);
        if (TextUtils.isEmpty(apkPath)) {
            Log.d(TAG, "verify fail: apkFilePath is null.");
            sendVerifyBehavior(apkPath, VERIFY_CODE_APK_PATH_INVALID, "apk path is null",
                    installerUid);
            return false;
        }
        File apkFile = new File(apkPath);
        if (!apkFile.exists()) {
            Log.d(TAG, "verify fail: apkFile not exists.");
            sendVerifyBehavior(apkPath, VERIFY_CODE_APK_PATH_INVALID, "apk not exist",
                    installerUid);
            return false;
        }
        if (installerUid != 1000 && installerUid != 2000 && installerUid != 0) {
            Log.d(TAG, "verify fail: caller-uid not allow:" + installerUid);
            sendVerifyBehavior(apkPath, VERIFY_CODE_INSTALLER_UID_INVALID,
                    "installer uid invalid", installerUid);
            return false;
        }
        if (installerUid == 1000) {
            if (!XtcAppSignatureUtil.verify(mContext, apkFile)) {
                return verifyXtcSecureData(apkFile, installerUid);
            }
            Log.d(TAG, "verify success: SYSTEM_UID and isXtcSignature:true");
            sendVerifyBehavior(apkPath, VERIFY_CODE_LOCAL_SUCCESS, "is xtc sign app", installerUid);
            return true;
        }
        if (Build.IS_USERDEBUG) {
            Log.d(TAG, "verify success: IS_USERDEBUG system");
            sendVerifyBehavior(apkPath, VERIFY_CODE_LOCAL_SUCCESS, "adb, debug system",
                    installerUid);
            return true;
        }
        if (isAdbModuleSwitchOpen()) {
            return verifyXtcSecureData(apkFile, installerUid);
        }
        Log.d(TAG, "verify fail: adb module switch not open.");
        sendVerifyBehavior(apkPath, VERIFY_CODE_105_INVALID, "105 closed", installerUid);
        return false;
    }
}