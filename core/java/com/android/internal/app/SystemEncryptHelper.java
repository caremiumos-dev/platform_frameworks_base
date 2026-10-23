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

import android.content.Context;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.SettingsStringUtil;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Slog;

import com.xtc.utils.IXTCUtilsManager;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * XTC/厂商系统级加密辅助类：从 XTCUtilsService 取系统 RSA 公钥并做加密。
 */
public class SystemEncryptHelper {
    private static final String TAG = "SystemEncryptHelper";

    private final String transformation = "RSA/ECB/PKCS1Padding";
    private final int systemKeyNumber = 2;
    private final int SYSTEM_KEYID_IDNEX = 0;
    private final int SYSTEM_KEY_IDNEX = 1;

    private static byte[] getBase64Decode(String str) {
        if (str == null) {
            return null;
        }
        try {
            return Base64.decode(str, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getBase64Encode(byte[] data) {
        if (data != null) {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        }
        return null;
    }

    private String getSystemRsaPubkey() {
        String securityKey;
        try {
            securityKey = IXTCUtilsManager.Stub.asInterface(
                    ServiceManager.getService(Context.XTC_UTILS_SERVICE))
                    .getSecurityKey(SYSTEM_KEYID_IDNEX, null);
        } catch (RemoteException e) {
            Slog.e(TAG, "getPsaPubkey e = " + e);
            securityKey = null;
        }
        if (securityKey == null || securityKey.length() == 0) {
            return null;
        }
        String[] keys = securityKey.split(SettingsStringUtil.DELIMITER);
        if (keys.length < systemKeyNumber) {
            return null;
        }
        return keys[SYSTEM_KEY_IDNEX];
    }

    public String bytes2hexDisplayHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (byte b : data) {
            String hexString = Integer.toHexString(b & 0xff);
            if (hexString.length() == 1) {
                hexString = "0" + hexString;
            }
            sb.append(hexString).append(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER);
        }
        sb.delete(sb.length() - 1, sb.length());
        sb.append("]");
        return sb.toString();
    }

    public PublicKey getRASPublicKey() {
        try {
            return KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
                    .generatePublic(new X509EncodedKeySpec(getBase64Decode(getSystemRsaPubkey())));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String systemKeyEncryptData(String data) {
        if (getSystemRsaPubkey() == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, getRASPublicKey());
            return getBase64Encode(cipher.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public String systemKeyEncryptData(byte[] data, PublicKey publicKey) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return getBase64Encode(cipher.doFinal(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
