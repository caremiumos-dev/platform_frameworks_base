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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.security.keystore.KeyProperties;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * Helpers for reading APK metadata (MD5, package name).
 *
 * @hide
 */
public class ApkInfoUtil {
    private static final String TAG = "ApkInfoUtil";
    private static final int BUFFER_SIZE = 1024 * 1024;

    private final Context mContext;

    public ApkInfoUtil(Context context) {
        mContext = context;
    }

    public String getFileMd5(String path) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(KeyProperties.DIGEST_MD5);
        } catch (Exception e) {
            Log.e(TAG, "Compute MD5 Error = " + e.toString());
            return "0";
        }
        try (FileInputStream input = new FileInputStream(path)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        } catch (Exception e) {
            Log.e(TAG, "Compute MD5 Error = " + e.toString());
            return "0";
        }
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append("0");
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public String getPackgeName(String apkPath) {
        if (mContext == null || TextUtils.isEmpty(apkPath)) {
            return null;
        }
        if (!new File(apkPath).exists()) {
            Log.w(TAG, "getPackgeName apk not exists" + apkPath);
            return null;
        }
        PackageManager pm = mContext.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "getPackgeName pm is null.");
            return null;
        }
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_ACTIVITIES);
        if (info != null) {
            return info.packageName;
        }
        Log.w(TAG, "getPackgeName PackageInfo is null.");
        return null;
    }
}