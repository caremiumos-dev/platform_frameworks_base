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

import android.text.TextUtils;

/**
 * Wi-Fi cipher type helpers for the diag service.
 *
 * @hide
 */
public class WifiCipherType {
    public enum CipherType {
        WIFICIPHER_NOPASS,
        WIFICIPHER_WEP,
        WIFICIPHER_WPA,
        WIFICIPHER_INVALID
    }

    public CipherType convert2CipherType(String type) {
        if (TextUtils.isEmpty(type)) {
            return CipherType.WIFICIPHER_WPA;
        }
        if (type.contains("NOPASS")) {
            return CipherType.WIFICIPHER_NOPASS;
        }
        if (type.contains("WEP")) {
            return CipherType.WIFICIPHER_WEP;
        }
        return CipherType.WIFICIPHER_WPA;
    }

    public String convert2Str(CipherType cipherType) {
        switch (cipherType) {
            case WIFICIPHER_NOPASS:
                return "NOPASS";
            case WIFICIPHER_WEP:
                return "WEP";
            case WIFICIPHER_WPA:
            default:
                return "WPA";
        }
    }

    public int convert2mmiSecureType(CipherType cipherType) {
        switch (cipherType) {
            case WIFICIPHER_NOPASS:
                return 0;
            case WIFICIPHER_WEP:
                return 1;
            case WIFICIPHER_WPA:
            default:
                return 2;
        }
    }
}