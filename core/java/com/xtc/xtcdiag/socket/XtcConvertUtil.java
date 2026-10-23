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
 * Byte/hex conversion helpers for the diag protocol.
 *
 * @hide
 */
public class XtcConvertUtil {
    public static String byte2Hex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuffer buffer = new StringBuffer();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() < 2) {
                buffer.append(0);
            }
            buffer.append(hex);
        }
        return buffer.toString();
    }

    public static byte[] hex2Bytes(String hex) {
        if (hex == null) {
            return null;
        }
        try {
            int length = hex.length();
            byte[] bytes = new byte[length / 2];
            for (int i = 0; i < length; i += 2) {
                bytes[i / 2] = (byte) ((hexChar2Int(hex.charAt(i)) << 4)
                        | hexChar2Int(hex.charAt(i + 1)));
            }
            return bytes;
        } catch (Exception e) {
            return null;
        }
    }

    public static int hexChar2Int(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'A' && c <= 'F') {
            return (c - 'A') + 10;
        }
        if (c < 'a' || c > 'f') {
            throw new RuntimeException("invalid hex char:" + c);
        }
        return (c - 'a') + 10;
    }

    public static byte[] int2Byte(int value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff),
                (byte) ((value >> 16) & 0xff),
                (byte) ((value >> 24) & 0xff)
        };
    }

    public static byte[] short2Byte(short value) {
        return new byte[] {
                (byte) (value & 0xff),
                (byte) ((value >> 8) & 0xff)
        };
    }
}