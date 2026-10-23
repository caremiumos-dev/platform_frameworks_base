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

package com.xtc.xtcdiag.socket;

/**
 * Header of the command protocol spoken with the PC-side diag tool.
 *
 * @hide
 */
public class XtcSocketHeader {
    public static final short HEADER_MAGIC = 0x7f01;
    public static final int HEAD_BYTE_SIZE = 24;

    public short magic;
    public short cmdCode;
    public int jsonDataLen;
    public int rawDataLen;
    public int crc;
    public byte[] reserve = new byte[8];

    @Override
    public String toString() {
        return "XtcSocketHeader { maigc:" + ((int) magic) + " cmdCode:" + ((int) cmdCode)
                + " jsonDataLen:" + jsonDataLen + " rawDataLen:" + rawDataLen
                + " crc:" + crc + "}";
    }
}