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

package com.xtc.secure.walle;

/**
 * Channel information embedded in an APK by the Walle build tool.
 *
 * @hide
 */
public class ChannelInfo {
    private final String channel;
    private final java.util.Map<String, String> extraInfo;

    public ChannelInfo(String channel, java.util.Map<String, String> extraInfo) {
        this.channel = channel;
        this.extraInfo = extraInfo;
    }

    public String getChannel() {
        return channel;
    }

    public java.util.Map<String, String> getExtraInfo() {
        return extraInfo;
    }
}