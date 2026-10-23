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

package com.android.server.xgseserver.xss;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Slog;

import com.android.okhttp.Interceptor;
import com.android.okhttp.MediaType;
import com.android.okhttp.OkHttpClient;
import com.android.okhttp.Request;
import com.android.okhttp.RequestBody;
import com.android.server.content.SyncStorageEngine;
import com.xtc.utils.IXTCUtilsManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Performs the vendor "xses" HTTPS request that carries the security policy,
 * then hands the decoded policy to {@link SystemExceptionManager}.
 *
 * <p>See {@link SystemExceptionManager} for the condemnation of the remote
 * uninstall / disable / network-isolation behaviour this request enables. The
 * request body also silently reports device identifiers (bind number, chip id,
 * model) to a vendor endpoint the user never agreed to. Condemned.
 *
 * @hide
 */
public class XsesRequestHelper {
    private static final String TAG = "SystemXses Request";

    private static final int OP_DIALOG = 1;
    private static final int OP_OVERLAY = 2;
    private static final int OP_UNINSTALL = 3;
    private static final int OP_DISABLE_NET_APP = 4;
    private static final int OP_DISABLE_NET_ADDRESS = 5;
    private static final int OP_DISABLE_APP = 6;

    private static final String DEFAULT_PUBKEY =
            "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAL9n5AXhw1raL2B6O52LRKOcqjHydrFD4m+lFJW3xv/viRut"
                    + "Oim4twKFlamB/edfz1KqydsMTVqsDCRiz8UuKU0CAwEAAQ==";
    private static final String URL_NET_WHITE =
            "https://api.watch.okii.com/social-service/config/getWhiteH5";
    private static final String URL_SECURITY =
            "https://api.watch.okii.com/base-service/watchDecode/getConfig";
    private static final String XTC_UTILS_SERVICE = "XTCUtilsService";
    private static final String INIT_SERVICE_URI = "content://com.xtc.initservice/item";
    private static final String COLUMN_AE = "ae";
    private static final String KEY_ENCRYPT_PREFIX = "KeyEncrypt";
    private static final String NULL = "null";

    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_TYPE = "type";

    private static final int BYTE_MASK = 0xFF;

    private static final MediaType JSON =
            MediaType.parse("application/json; charset=utf-8");

    private static boolean sDebugRequest = false;
    private static String sKeyId = NULL;
    private static String sAk = NULL;
    private static String sPk = NULL;
    public static long lastRequestedTime = 0;

    private final Context mContext;
    private SystemExceptionManager mExceptionManager;

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case OP_DIALOG: {
                    Slog.d(TAG, "SystemExceptionManager doToast start ");
                    String value = (String) message.obj;
                    if (value != null) {
                        mExceptionManager.messageDialog(value);
                    }
                    break;
                }
                case OP_OVERLAY: {
                    Slog.d(TAG, "SystemExceptionManager doToast start ");
                    String value = (String) message.obj;
                    if (value != null) {
                        mExceptionManager.showStrictModeOverlay(value);
                    }
                    break;
                }
                case OP_UNINSTALL: {
                    Slog.d(TAG, "SystemExceptionManager start ");
                    String value = (String) message.obj;
                    if (value != null) {
                        mExceptionManager.doUninstall(value);
                    }
                    break;
                }
                case OP_DISABLE_NET_APP: {
                    Slog.d(TAG, "SystemExceptionManager start ");
                    String value = (String) message.obj;
                    if (value != null) {
                        mExceptionManager.doDenyNetworkAccessApp(value);
                    }
                    break;
                }
                case OP_DISABLE_NET_ADDRESS: {
                    Slog.d(TAG, "SystemExceptionManager start ");
                    String value = (String) message.obj;
                    if (value != null) {
                        mExceptionManager.denyNetworkAccessAddress(value);
                    }
                    break;
                }
                case OP_DISABLE_APP: {
                    String value = (String) message.obj;
                    if (value != null) {
                        mExceptionManager.doDisableApp(value);
                    }
                    break;
                }
                default:
                    Slog.d(TAG, "The server sent an error message type!!");
                    break;
            }
        }
    };

    public static class OkhttpInterceptor implements Interceptor {
        private final int mMaxRetry;

        public OkhttpInterceptor(int maxRetry) {
            this.mMaxRetry = maxRetry;
        }

        @Override
        public com.android.okhttp.Response intercept(Chain chain) throws IOException {
            return retry(chain, 0);
        }

        com.android.okhttp.Response retry(Chain chain, int retryCount) {
            Request request = chain.request();
            try {
                Slog.d(TAG, "retryCent = " + retryCount);
                return chain.proceed(request);
            } catch (Exception e) {
                Slog.d(TAG, " catch retryCent = " + retryCount);
                if (this.mMaxRetry > retryCount) {
                    return retry(chain, retryCount + 1);
                }
                return null;
            }
        }
    }

    public XsesRequestHelper(Context context) {
        this.mContext = context;
        System.loadLibrary("xtcSecurity");
    }

    private static JSONObject GetRequestTokenBody() {
        String message = getMessageFromT(1, String.valueOf(System.currentTimeMillis()));
        if (TextUtils.isEmpty(message) || !message.contains(":")) {
            return null;
        }
        String[] parts = message.split(":");
        if (parts.length < 2) {
            return null;
        }
        try {
            JSONObject object = new JSONObject();
            object.put("keyId", parts[0]);
            object.put("encryptContent", parts[1]);
            object.put("systemMark", 1);
            if (sDebugRequest) {
                Slog.d(TAG, "GetRequestTokenBody jsonBody = " + object);
            }
            return object;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildBaseRequestParam() {
        JSONObject object = new JSONObject();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        try {
            object.put("ImFlag ", "");
            object.put("appId", "2");
            object.put(HttpConfigOptions.HeaderKey.PROGRAM, "watch");
            object.put(COLUMN_TIMESTAMP, time);
            object.put("deviceId", SystemProperties.get("ro.boot.bindnumber"));
            object.put("token", SystemProperties.get("ro.boot.xtc.chipid"));
            object.put("mac", "unknow");
            object.put("accountId", "");
            object.put("registId", "0");
            return object.toString();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static Request.Builder createRequestBuilder(String signPath, JSONObject body,
            String aesKey, String pubKey) {
        String baseParam = buildBaseRequestParam();
        String encryptedBaseParam = encryptAES(baseParam, aesKey);
        Slog.d(TAG, "encryptedBaseParam = " + encryptedBaseParam);
        String sign = generateSign(aesKey, signPath, baseParam,
                body != null ? body.toString() : null);
        String model = SystemProperties.get("ro.product.innermodel");
        Request.Builder builder = new Request.Builder();
        builder.addHeader(HttpConfigOptions.HeaderKey.CONTENT_TYPE,
                HttpConfigOptions.HeaderKey.MEDIA_TYPE);
        builder.addHeader(HttpConfigOptions.HeaderKey.MODEL, model);
        builder.addHeader(HttpConfigOptions.HeaderKey.EEBBK_SIGN, sign);
        if (!sKeyId.equals(NULL) && sKeyId != null) {
            builder.addHeader(HttpConfigOptions.HeaderKey.EEBBK_KEY_ID, sKeyId);
        }
        if (!pubKey.equals(NULL) && pubKey != null) {
            if (sDebugRequest) {
                Slog.d(TAG, "encryptedBaseParam eebbkKey= " + pubKey);
            }
            builder.addHeader(HttpConfigOptions.HeaderKey.EEBBK_KEY, pubKey);
        }
        if (!encryptedBaseParam.equals(NULL) && encryptedBaseParam != null) {
            builder.addHeader(HttpConfigOptions.HeaderKey.BASE_REQUEST_PARAM, encryptedBaseParam);
        }
        String version = "W_" + SystemProperties.get("ro.product.current.softversion", "unknown");
        if (sDebugRequest) {
            Slog.d(TAG, "encryptedBaseParam ve= " + version);
        }
        builder.addHeader(HttpConfigOptions.HeaderKey.VERSION, version);
        builder.addHeader(HttpConfigOptions.HeaderKey.ENCRYPTED,
                HttpConfigOptions.HeaderKey.ENCRYPTED);
        return builder;
    }

    private static byte[] decrypt(byte[] data, String key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return cipher.doFinal(data);
    }

    private static byte[] decryptAESToByte(String data, String key) {
        try {
            return decrypt(getBase64Decode(data), key);
        } catch (Exception e) {
            Slog.d(TAG, "decryptAESToByte e= " + e);
            return null;
        }
    }

    private static String disposeResponse(String response, String key) {
        if (response == null || response.isEmpty()) {
            return NULL;
        }
        if (!isEmptyString(response)) {
            response = response.replace("\"", "");
        }
        byte[] decrypted = decryptAESToByte(response, key);
        if (decrypted == null) {
            return NULL;
        }
        byte[] uncompressed = uncompress(decrypted);
        return uncompressed != null ? new String(uncompressed) : response;
    }

    private static byte[] encrypt(String data, String key) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        byte[] bytes = data.getBytes("utf-8");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        return cipher.doFinal(bytes);
    }

    private static String encryptAES(String data, String key) {
        try {
            return getBase64Encode(encrypt(data, key));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String encryptAESKey(String data, String pubKey) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getPublicKey(pubKey));
            return getBase64Encode(cipher.doFinal(data.getBytes("UTF-8")));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String encryptBody(String body, String key, boolean encrypt, String iv) {
        if (!isEmptyString(body) && !isEmptyString(key) && encrypt) {
            body = generateEncryptBody(body, key, iv);
        }
        Slog.i(TAG, "encrypt body:" + body);
        return body;
    }

    private static String execRequest(Request request) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(10L, TimeUnit.SECONDS);
        client.setReadTimeout(10L, TimeUnit.SECONDS);
        client.setWriteTimeout(10L, TimeUnit.SECONDS);
        client.interceptors().add(new OkhttpInterceptor(3));
        String result = "";
        try {
            com.android.okhttp.Response response = client.newCall(request).execute();
            if (response != null) {
                result = response.body().string();
                Slog.d(TAG, "responseBodyStr = " + result);
            } else {
                Slog.d(TAG, "request timeout");
            }
            return result;
        } catch (Exception e) {
            Slog.d(TAG, "body e= " + e);
            return "";
        }
    }

    private static String generateEncryptBody(String body, String key, String iv) {
        if (isEmptyString(key)) {
            Slog.w(TAG, "eebbkKey is null,eebbkKey:" + key);
        } else {
            encryptAES(body, iv);
        }
        return body;
    }

    private static String generateSign(String key, String path, String baseParam, String body) {
        byte[] bodyBytes = null;
        if (body != null && !"".equals(body)) {
            bodyBytes = body.getBytes(Charset.forName("utf-8"));
        }
        return generateSignWithBodyBytes(key, path, baseParam, bodyBytes);
    }

    private static String generateSignWithBodyBytes(String key, String path, String baseParam,
            byte[] body) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Charset charset = Charset.forName("utf-8");
        byte[] pathBytes = path.getBytes(charset);
        output.write(pathBytes, 0, pathBytes.length);
        byte[] baseBytes = baseParam.getBytes(charset);
        output.write(baseBytes, 0, baseBytes.length);
        if (body != null && body.length > 0) {
            output.write(body, 0, body.length);
        }
        byte[] keyBytes = key.getBytes(charset);
        output.write(keyBytes, 0, keyBytes.length);
        try {
            output.close();
        } catch (IOException e) {
            Slog.d(TAG, " generateSignWithBodyBytes ", e);
        }
        return md5(output.toString());
    }

    private static byte[] getBase64Decode(String data) {
        if (data == null) {
            return null;
        }
        try {
            return Base64.decode(data, Base64.NO_WRAP);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getBase64Encode(byte[] data) {
        if (data != null) {
            return Base64.encodeToString(data, Base64.NO_WRAP);
        }
        return null;
    }

    private static String getMessageFromT(int type, String seed) {
        String securityKey = "";
        try {
            securityKey = IXTCUtilsManager.Stub.asInterface(
                    ServiceManager.getService(XTC_UTILS_SERVICE)).getSecurityKey(type, seed);
            if (sDebugRequest) {
                Slog.d(TAG, "getMessageFromT type = " + type + ",result =  " + securityKey);
            }
        } catch (RemoteException e) {
            Slog.e(TAG, "getMessageFromT e = " + e);
        }
        return securityKey;
    }

    private static PublicKey getPublicKey(String key) throws Exception {
        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(getBase64Decode(key)));
    }

    private static String getResponseData(String response) {
        if (response == null || response.isEmpty() || !isJsonSting(response)) {
            return NULL;
        }
        HashMap<String, String> map = jsonToMap(response);
        return SyncStorageEngine.MESG_SUCCESS.equals(map.get("desc"))
                ? map.get("data").replaceAll("\\\\", "") : NULL;
    }

    private static boolean isEmptyString(CharSequence value) {
        return value == null || value.length() == 0;
    }

    private static boolean isJsonSting(String value) {
        try {
            new JSONObject(value);
            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    private static HashMap<String, String> jsonToMap(String json) {
        HashMap<String, String> map = new HashMap<>();
        try {
            JSONObject object = new JSONObject(json);
            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                map.put(key, object.getString(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private static String md5(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                if ((b & BYTE_MASK) < 16) {
                    builder.append("0");
                }
                builder.append(Integer.toHexString(b & BYTE_MASK));
            }
            return builder.toString();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException", e);
        }
    }

    private void processServerResponse(String response) {
        if (response == null || response.isEmpty()) {
            Slog.d(TAG, "processServerResponse response is null.");
            return;
        }
        this.mExceptionManager = new SystemExceptionManager(this.mContext);
        if (SystemExceptionManager.isJson(response)) {
            HashMap<String, String> map = SystemExceptionManager.jsonToMap(response);
            for (String key : map.keySet()) {
                String value = map.get(key).toString();
                if (sDebugRequest) {
                    Slog.d(TAG, "processServerResponse key = " + key + ",  value = " + value);
                }
                if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DIALOG)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(OP_DIALOG, value));
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_OVERLAY)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(OP_OVERLAY, value));
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_UNINSTALL)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(OP_UNINSTALL, value));
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_NET_APP)) {
                    this.mHandler.sendMessage(
                            this.mHandler.obtainMessage(OP_DISABLE_NET_APP, value));
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_NET_ADDRESS)) {
                    this.mHandler.sendMessage(
                            this.mHandler.obtainMessage(OP_DISABLE_NET_ADDRESS, value));
                } else if (key.equals(SystemExceptionManager.EXCEPTION_TYPE_DISABLE_APP)) {
                    this.mHandler.sendMessage(this.mHandler.obtainMessage(OP_DISABLE_APP, value));
                }
            }
        }
    }

    public static native String secretKeyDecrypt(String data);

    private boolean setRsaKeyFromL() {
        Cursor cursor = null;
        try {
            ContentResolver resolver = this.mContext.getContentResolver();
            cursor = resolver.query(Uri.parse(INIT_SERVICE_URI), null, null, null, null);
            if (cursor != null && cursor.moveToNext()) {
                int columnIndex = cursor.getColumnIndex(COLUMN_AE);
                if (columnIndex < 0) {
                    Slog.d(TAG, "  columnIndex <0 ");
                    return false;
                }
                String ae = cursor.getString(columnIndex);
                if (ae == null || ae.isEmpty()) {
                    Slog.d(TAG, "ae is null! ");
                    return false;
                }
                Slog.d(TAG, "ae =" + ae);
                if (!ae.startsWith(KEY_ENCRYPT_PREFIX)) {
                    Slog.d(TAG, "query ae error, ae = " + ae);
                    return false;
                }
                String enKey = ae.substring(KEY_ENCRYPT_PREFIX.length());
                Slog.d(TAG, "enKey = " + enKey);
                String decrypted = secretKeyDecrypt(enKey);
                if (decrypted == null) {
                    Slog.d(TAG, "de is null! ");
                    return false;
                }
                if (TextUtils.isEmpty(decrypted) || !decrypted.contains(":")) {
                    Slog.d(TAG, "de is error! ");
                    return false;
                }
                String[] parts = decrypted.split(":");
                if (parts.length != 3) {
                    Slog.d(TAG, "request ak failed!");
                    return false;
                }
                sKeyId = parts[0];
                sAk = parts[1];
                sPk = parts[2];
                if (!sKeyId.equals(NULL) && !sAk.equals(NULL) && !sPk.equals(NULL)) {
                    return true;
                }
                Slog.d(TAG, "de split error!");
                return false;
            }
        } catch (Exception e) {
            Slog.w(TAG, "Exception in setRsaKeyFromL", e);
        } finally {
            if (cursor != null) {
                Slog.d(TAG, "close cursor");
                cursor.close();
            }
        }
        return false;
    }

    public static byte[] uncompress(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data))) {
            byte[] buffer = new byte[256];
            int count;
            while ((count = gzip.read(buffer)) >= 0) {
                output.write(buffer, 0, count);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output.toByteArray();
    }

    public void realRequest() {
        new Thread() {
            @Override
            public void run() {
                sDebugRequest = SystemProperties.getBoolean("log.de.xses_re_net", false);
                boolean rsaOk = setRsaKeyFromL();
                Slog.d(TAG, "realRequest serverControlPolicy = " + xsesService.serverControlPolicy
                        + ",netWhiteSwitch = " + xsesService.netWhiteSwitch
                        + ",reportNetAddrSwitch = " + xsesService.reportNetAddrSwitch
                        + ", bl = " + rsaOk);
                if (xsesService.serverControlPolicy && rsaOk) {
                    securityRequest();
                    Slog.d(TAG, "doReqest securityRequest completed.");
                }
                if ((xsesService.netWhiteSwitch || xsesService.reportNetAddrSwitch) && rsaOk) {
                    requestNetWhite();
                    Slog.d(TAG, "doReqest requestNetWhite completed.");
                }
                lastRequestedTime = SystemClock.elapsedRealtime();
            }
        }.start();
    }

    public void requestNetWhite() {
        if (sAk.equals(NULL) || sPk.equals(NULL) || sKeyId.equals(NULL)) {
            return;
        }
        if (sDebugRequest) {
            Slog.d(TAG, "requestNetWhite   ak = " + sAk + ", pk = " + sPk + ",keyId = " + sKeyId);
        }
        String response = disposeResponse(execRequest(createRequestBuilder(URL_NET_WHITE, null,
                sAk, sPk).get().url(URL_NET_WHITE).build()), sAk);
        if (response.equals(NULL)) {
            Slog.d(TAG, "requestNetWhite responseStr is null");
            return;
        }
        if (sDebugRequest) {
            Slog.d(TAG, "requestNetWhite responseStr= " + response);
        }
        String data = getResponseData(response);
        if (sDebugRequest) {
            Slog.d(TAG, "requestNetWhite resData= " + data);
        }
        if (data.isEmpty() || data.equals(NULL)) {
            return;
        }
        try {
            android.webkit.IWebViewUpdateService.Stub.asInterface(
                    ServiceManager.getService("webviewupdate")).setNetWhiteList(data);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void securityRequest() {
        if (sAk.equals(NULL) || sPk.equals(NULL) || sKeyId.equals(NULL)) {
            return;
        }
        if (sDebugRequest) {
            Slog.d(TAG, "securityRequest aeskey = " + sAk + ", pubkey = " + sPk);
        }
        JSONObject body = new JSONObject();
        try {
            body.put(HttpConfigOptions.HeaderKey.MODEL,
                    SystemProperties.get("ro.product.model"));
            body.put("bindNumber", SystemProperties.get("ro.boot.bindnumber"));
            body.put(COLUMN_TYPE, SystemProperties.get("ro.build.type"));
            body.put("factory", SystemProperties.get("persist.sys.xtc.adb_port"));
            String encryptedBody = encryptAES(body.toString(), sAk);
            if (sDebugRequest) {
                Slog.d(TAG, "encryptedBody  = " + encryptedBody);
            }
            String response = disposeResponse(execRequest(createRequestBuilder(URL_SECURITY, body,
                    sAk, sPk).url(URL_SECURITY).post(
                    RequestBody.create(JSON, encryptedBody)).build()), sAk);
            if (response.equals(NULL)) {
                Slog.d(TAG, "securityRequest responseStr is null");
                return;
            }
            if (sDebugRequest) {
                Slog.d(TAG, "securityRequest responseStr= " + response);
            }
            processServerResponse(getResponseData(response));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}