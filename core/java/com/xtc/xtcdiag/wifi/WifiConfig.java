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

/**
 * Wi-Fi configuration persisted by the diag service in
 * {@code /data/xtc_diag/custom_ssid.txt}.
 *
 * @hide
 */
public class WifiConfig {
    private String ssid;
    private String password;
    private WifiCipherType.CipherType cipherType;
    private int brightness;

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public WifiCipherType.CipherType getCipherType() {
        return cipherType;
    }

    public void setCipherType(WifiCipherType.CipherType cipherType) {
        this.cipherType = cipherType;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int brightness) {
        this.brightness = brightness;
    }
}