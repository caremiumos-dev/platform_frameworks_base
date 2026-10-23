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
import android.os.SystemProperties;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.util.Log;

import com.xtc.behavior.XtcBehavior;
import com.xtc.utils.IXTCUtilsManager;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.LinkedHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * XTC/厂商「开放功能安全校验」：通过串口与售后平台交互，校验设备绑定号后解锁 ADB/自检。
 * 依赖 native 库 libxtcSerial。
 */
public class OpenFeatureSecureCheck {
    private static final String TAG = "OpenFeatureSecureCheck";
    private static final String FUNC_NAME = "xtc_check_key_authen";
    private static final String REPORT_RESULT_KEY = "result";

    public static final String USB_CHECK_MODE = "serial_cdev";
    private final String USB_CHECK_MODE_CLEAR = "serial_cdev_clean";
    private final String USB_COM_PORT = "/dev/at_usb1";
    private final String SYS_USB_PROP = "sys.usb.config";
    private final String PERSIST_USB_PROR = "persist.sys.usb.config";
    private final int chunkSize = 24;
    private final byte[] serialHeadByte = {(byte) 0xa8, (byte) 0xa6};
    private final byte serialCheckCode = (byte) 0xff;
    private final String checkDataEndFlag = "end";
    private final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private final String KEY_ALIAS = "featureSecure_key";
    private final String transformation = "RSA/ECB/PKCS1Padding";

    private Context mContext;
    private KeyStore mKeyStore;
    private KeyPair mKeyPair = null;
    private String prePropPersistUsbconfig;
    private String prePropUsbconfig;
    private int fd = 0;
    private boolean runUsbCheckState = true;
    private ReadSerialThread mReadSerialThread = null;
    SelfTestVerifyModeActivity.SecureCheckInterface checkCallback = null;
    private String check_result = "";

    static {
        System.loadLibrary("xtcSerial");
    }

    public OpenFeatureSecureCheck(Context context) {
        mContext = context;
    }

    private class ReadSerialThread extends Thread {
        private final int CheckDataSendMaxCount;
        private int checkDataSendindex;
        private boolean startReceived;
        public boolean suspendFlag;
        ByteBuffer buffer;
        byte[] bufferData;
        int bufferLength;
        int bufferSize;

        private ReadSerialThread() {
            startReceived = false;
            suspendFlag = false;
            bufferSize = 512;
            bufferLength = 0;
            bufferData = new byte[bufferSize];
            buffer = ByteBuffer.allocateDirect(bufferSize);
            CheckDataSendMaxCount = 3;
            checkDataSendindex = 0;
        }

        private boolean handleSerailMessage(byte[] data, int len) {
            if (data == null || len < 1) {
                startDataCheck(data, len);
                return parsePackageData();
            }
            Log.d(TAG, "receive dateLen:" + len + " startReceived:" + startReceived
                    + Integer.toHexString(data[0] & 0xff) + " bufferLength:" + bufferLength);
            if (startReceived) {
                if (data != null && bufferData != null && data.length >= len
                        && bufferData.length >= bufferLength + len) {
                    System.arraycopy(data, 0, bufferData, bufferLength, len);
                }
                bufferLength += len;
            } else {
                startDataCheck(data, len);
            }
            Log.d(TAG, "buffer.length:" + bufferLength);
            if (bufferLength > serialHeadByte.length) {
                return parsePackageData();
            }
            return false;
        }

        private void parseFunctionInfo(byte[] data, int len) {
            Log.d(TAG, "parseFunctionInfo dataLen:" + len);
            if (data == null || len <= 0) {
                return;
            }
            if (Arrays.equals(data, "start".getBytes())) {
                if (checkDataSendindex >= CheckDataSendMaxCount) {
                    Log.d(TAG, "has sendCheckData");
                    return;
                }
                sendCheckData();
                check_result = "send check data " + checkDataSendindex;
                checkDataSendindex++;
                return;
            }
            stopReadSerialThread();
            boolean success = false;
            try {
                byte[] decryptData = decryptData(Base64.decode(data, Base64.NO_WRAP));
                if (decryptData == null || decryptData.length <= 0) {
                    check_result = "check fail key error";
                } else if (new String(decryptData).equals(
                        SystemProperties.get("ro.boot.bindnumber", "unknown"))) {
                    Log.d(TAG, "parseFunctionInfo check success");
                    check_result = "check success";
                    success = true;
                } else {
                    Log.d(TAG, "parseFunctionInfo check fail!");
                    check_result = "check fail respond error";
                }
            } catch (Exception e) {
                e.printStackTrace();
                check_result = "check fail other error";
            }
            if (success) {
                sendReplyData(true);
                if (checkCallback != null) {
                    checkCallback.onCallback(true);
                }
                return;
            }
            sendReplyData(false);
            if (checkCallback != null) {
                checkCallback.onCallback(false);
            }
        }

        private boolean parsePackageData() {
            if (bufferLength <= serialHeadByte.length + 2) {
                return false;
            }
            int length = (bufferData[2] * 255) + bufferData[3] + serialHeadByte.length + 2;
            Log.d(TAG, "parsePackageData buffer.length:" + bufferLength
                    + "  calculate dataLength:" + length);
            if (bufferLength < length || (bufferData[length - 1] & 0xff) != 0xff) {
                Log.d(TAG, "parsePackageData get flag:"
                        + Integer.toHexString(bufferData[length - 1] & 0xff));
                return false;
            }
            startReceived = false;
            Log.d(TAG, "receive success");
            if (length - serialHeadByte.length > 0) {
                int copyLength = ((length - serialHeadByte.length) - 2) - 1;
                byte[] functionData = new byte[copyLength];
                Log.d(TAG, "parsePackageData copy_length:" + copyLength);
                System.arraycopy(bufferData, serialHeadByte.length + 2, functionData, 0,
                        copyLength);
                parseFunctionInfo(functionData, copyLength);
            }
            if (bufferLength <= length) {
                Arrays.fill(bufferData, (byte) 0);
                bufferLength = 0;
                return false;
            }
            bufferLength -= length;
            System.arraycopy(bufferData, length, bufferData, 0, bufferLength);
            Log.d(TAG, "bufferLength:" + bufferLength);
            return true;
        }

        private void startDataCheck(byte[] data, int len) {
            if (len >= 2 && (data[0] & 0xff) == (serialHeadByte[0] & 0xff)
                    && (data[1] & 0xff) == (serialHeadByte[1] & 0xff)) {
                Log.d(TAG, "startDataCheck start receive first");
                if (data != null && bufferData != null && data.length >= len
                        && bufferData.length >= bufferLength + len) {
                    System.arraycopy(data, 0, bufferData, bufferLength, len);
                }
                bufferLength += len;
                startReceived = true;
                return;
            }
            if (len == 1 && (data[0] & 0xff) == (serialHeadByte[0] & 0xff)) {
                if (data != null && bufferData != null && data.length >= len
                        && bufferData.length >= bufferLength + len) {
                    System.arraycopy(data, 0, bufferData, bufferLength, len);
                }
                bufferLength += len;
                return;
            }
            if (len >= 1 && bufferLength == 1
                    && (bufferData[0] & 0xff) == (serialHeadByte[0] & 0xff)
                    && (data[0] & 0xff) == (serialHeadByte[1] & 0xff)) {
                if (data != null && bufferData != null && data.length >= len
                        && bufferData.length >= bufferLength + len) {
                    System.arraycopy(data, 0, bufferData, bufferLength, len);
                }
                bufferLength += len;
                Log.d(TAG, "startDataCheck start receive");
                startReceived = true;
                return;
            }
            if (len == 0 && bufferLength > 1
                    && (bufferData[0] & 0xff) == (serialHeadByte[0] & 0xff)
                    && (bufferData[1] & 0xff) == (serialHeadByte[1] & 0xff)) {
                Log.d(TAG, "startDataCheck start receive bufferData continue");
                startReceived = true;
            } else {
                Log.d(TAG, "startDataCheck dataLen=" + len + " bufferLength=" + bufferLength);
                if (bufferLength > 0) {
                    Log.d(TAG, "startDataCheck dataLen=" + len + " bufferData[0]="
                            + Integer.toHexString(bufferData[0] & 0xff));
                }
            }
        }

        @Override
        public void run() {
            super.run();
            while (runUsbCheckState) {
                synchronized (this) {
                    while (suspendFlag) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (!runUsbCheckState) {
                    Log.d(TAG, "runUsbCheckState:" + runUsbCheckState + " stop");
                    break;
                }
                if (fd <= 0) {
                    Log.d(TAG, "device is close fd=" + fd);
                    break;
                }
                int len;
                synchronized (OpenFeatureSecureCheck.this) {
                    len = read_port(fd, buffer, bufferSize);
                }
                if (len > 0) {
                    byte[] data = new byte[len];
                    buffer.rewind();
                    buffer.get(data);
                    boolean more = handleSerailMessage(data, len);
                    while (more) {
                        Log.d(TAG, "continue parse");
                        more = handleSerailMessage(null, 0);
                    }
                    buffer.clear();
                }
            }
            reportDataByXtcBehavior(mContext, check_result);
        }

        public synchronized void setResume() {
            suspendFlag = false;
            notify();
        }

        public synchronized void setStop() {
            suspendFlag = true;
        }

        public void stopReadSerialThread() {
            Log.d(TAG, "stopReadSerialThread enter");
            synchronized (this) {
                Log.d(TAG, "stopReadSerialThread notify");
                runUsbCheckState = false;
                notify();
            }
        }
    }

    private native int open_port(String path, int baudrate);

    private native int read_port(int fd, ByteBuffer buffer, int size);

    private native int write_port(int fd, byte[] data);

    private native void close_port(int fd);

    private void initKeyStore() {
        try {
            mKeyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            mKeyStore.load(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void generateLocalKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,
                            ANDROID_KEYSTORE);
            keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setKeySize(2048)
                    .setUserAuthenticationRequired(false)
                    .build());
            mKeyPair = keyPairGenerator.generateKeyPair();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        if (mKeyPair == null || mKeyPair.getPublic() == null || mKeyPair.getPrivate() == null) {
            Log.d(TAG, "keyPair init faild !!!");
        } else {
            Log.d(TAG, "Key generated successfully");
        }
    }

    private byte[] decryptData(byte[] data) {
        if (data == null || data.length <= 0) {
            return null;
        }
        if (mKeyPair == null) {
            Log.d(TAG, "keypair is null");
            return null;
        }
        try {
            PrivateKey privateKey = ((KeyStore.PrivateKeyEntry)
                    mKeyStore.getEntry(KEY_ALIAS, null)).getPrivateKey();
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] encryptData(byte[] data, KeyPair keyPair) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            return cipher.doFinal(data);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String encryptDataBySystemKey(byte[] data) {
        try {
            return IXTCUtilsManager.Stub.asInterface(
                    ServiceManager.getService(Context.XTC_UTILS_SERVICE))
                    .getSecurityKey(3, getBase64Encode(data));
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

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

    private String getBase64Encode(String str) {
        if (str != null) {
            return Base64.encodeToString(str.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
        }
        return null;
    }

    private String getBase64Encode(byte[] data) {
        if (data != null) {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        }
        return null;
    }

    private byte[] getPublicKey() {
        return mKeyPair.getPublic().getEncoded();
    }

    private boolean openUsbCeckMode() {
        String persistUsb = SystemProperties.get(PERSIST_USB_PROR, "null");
        String usb = SystemProperties.get(SYS_USB_PROP, "null");
        Log.d(TAG, "openUsbCeckMode usbPersistStatus:" + persistUsb + " usbStatus:" + usb);
        if (usb.equals(USB_CHECK_MODE)) {
            Log.d(TAG, "Mode has been turned on");
            return true;
        }
        prePropPersistUsbconfig = persistUsb;
        prePropUsbconfig = usb;
        SystemProperties.set(PERSIST_USB_PROR, USB_CHECK_MODE);
        SystemProperties.set(SYS_USB_PROP, USB_CHECK_MODE);
        String current = SystemProperties.get(SYS_USB_PROP, "null");
        Log.d(TAG, "openUsbCeckMode sys.usb.config: " + current);
        if (!current.equals(USB_CHECK_MODE)) {
            return false;
        }
        Log.d(TAG, "Mode turned on success");
        check_result = "open port success";
        return true;
    }

    private void restorePreUsbState() {
        if (prePropUsbconfig == null || prePropUsbconfig.length() <= 0) {
            Log.d(TAG, "Mode not switched");
            return;
        }
        Log.d(TAG, "restorePreUsbState prePropUsbconfig:" + prePropUsbconfig
                + " current sys.usb.config: " + SystemProperties.get(SYS_USB_PROP, "null"));
        SystemProperties.set(SYS_USB_PROP, USB_CHECK_MODE_CLEAR);
        Log.d(TAG, "restorePreUsbState current sys.usb.config: "
                + SystemProperties.get(SYS_USB_PROP, "null"));
        if (prePropPersistUsbconfig != null) {
            SystemProperties.set(PERSIST_USB_PROR, prePropPersistUsbconfig);
        }
        SystemProperties.set(SYS_USB_PROP, prePropUsbconfig);
        if (SystemProperties.get(SYS_USB_PROP, "null").equals(prePropUsbconfig)) {
            Log.d(TAG, "Mode switch back successful");
        }
    }

    private void sendCheckData() {
        String bindNumber = SystemProperties.get("ro.boot.bindnumber", "unknown");
        Log.d(TAG, "sendCheckData: " + bindNumber);
        byte[] publicKey = getPublicKey();
        int length = publicKey.length;
        int numOfChunks = ((length + chunkSize) - 1) / chunkSize;
        Log.d(TAG, "numOfChunks:" + numOfChunks);
        sendDataToSerial(bindNumber);
        sendDataToSerial("connect request");
        for (int i = 0; i < numOfChunks; i++) {
            int offset = i * chunkSize;
            byte[] chunk = Arrays.copyOfRange(publicKey, offset,
                    Math.min(length - offset, chunkSize) + offset);
            sendDataToSerial(encryptDataBySystemKey(chunk));
        }
        sendDataToSerial(checkDataEndFlag);
    }

    private boolean sendDataToSerial(String str) {
        if (str == null) {
            return false;
        }
        if (str.length() <= 0) {
            Log.d(TAG, "sendDataToSerial Protocol not supported");
            return false;
        }
        if (fd <= 0) {
            return false;
        }
        byte[] bytes = str.getBytes();
        int length = bytes.length + 1;
        byte[] lengthBytes = {(byte) (length / 255), (byte) (length % 255)};
        byte[] tailBytes = {serialCheckCode};
        byte[] packet = new byte[bytes.length + serialHeadByte.length + tailBytes.length
                + lengthBytes.length];
        System.arraycopy(serialHeadByte, 0, packet, 0, serialHeadByte.length);
        System.arraycopy(lengthBytes, 0, packet, serialHeadByte.length, lengthBytes.length);
        System.arraycopy(bytes, 0, packet, serialHeadByte.length + lengthBytes.length,
                bytes.length);
        System.arraycopy(tailBytes, 0, packet,
                bytes.length + serialHeadByte.length + lengthBytes.length, tailBytes.length);
        return write_port(fd, packet) > 0;
    }

    private void sendReplyData(Boolean success) {
        if (success) {
            sendDataToSerial("sucess");
        } else {
            sendDataToSerial("fail");
        }
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

    public void initSecureCheck() {
        initKeyStore();
        generateLocalKeyPair();
    }

    public boolean sendCheckRequest(
            SelfTestVerifyModeActivity.SecureCheckInterface secureCheckInterface) {
        checkCallback = secureCheckInterface;
        if (!openUsbCeckMode()) {
            Log.d(TAG, "sendCheckRequest openUsbCeckMode return false");
            reportDataByXtcBehavior(mContext, "open port error");
            return false;
        }
        fd = open_port(USB_COM_PORT, 115200);
        if (fd <= 0) {
            Log.d(TAG, "port open fail");
            return false;
        }
        synchronized (this) {
            if (mReadSerialThread == null) {
                mReadSerialThread = new ReadSerialThread();
                mReadSerialThread.start();
            } else {
                reportDataByXtcBehavior(mContext, "thread error");
            }
        }
        return true;
    }

    public void stopCheckRequest() {
        if (mReadSerialThread != null) {
            mReadSerialThread.stopReadSerialThread();
        }
        unInitSecureCheck();
    }

    public void unInitSecureCheck() {
        if (fd > 0) {
            close_port(fd);
            fd = 0;
        }
        restorePreUsbState();
    }

    public static void reportDataByXtcBehavior(Context context, String result) {
        LinkedHashMap<String, String> data = new LinkedHashMap<>();
        data.put(REPORT_RESULT_KEY, result);
        new XtcBehavior.Builder(context, FUNC_NAME, data).build().sendData();
    }
}
