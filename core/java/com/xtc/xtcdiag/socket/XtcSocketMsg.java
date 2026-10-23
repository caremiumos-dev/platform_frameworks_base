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

import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

/**
 * Serializes and parses the command protocol spoken with the PC-side diag tool.
 *
 * @hide
 */
public class XtcSocketMsg {
    private static final String TAG = "XtcDiag-SocketMsg";

    public static final short HEADER_MAGIC = 0x7f01;
    public static final String MSG_CHARSET = "UTF-8";
    public static final Charset charset = Charset.forName(MSG_CHARSET);

    private ByteBuffer buildHeader(short magic, short cmdCode, int jsonLen, int rawLen) {
        ByteBuffer buffer = ByteBuffer.allocate(XtcSocketHeader.HEAD_BYTE_SIZE);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(magic);
        buffer.putShort(cmdCode);
        buffer.putInt(jsonLen);
        buffer.putInt(rawLen);
        buffer.putInt(calculationCrc(magic, cmdCode, jsonLen, rawLen));
        buffer.put(new byte[8]);
        return buffer;
    }

    private ByteBuffer buildJson(String json) {
        if (TextUtils.isEmpty(json)) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(json.getBytes(charset).length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(charset.encode(json));
        return buffer;
    }

    private ByteBuffer buildRaw(byte[] raw) {
        if (raw == null) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(raw.length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(raw);
        return buffer;
    }

    private int calcCrc(ByteBuffer buffer) {
        int sum = 0;
        for (byte b : buffer.array()) {
            Log.d(TAG, "calcCrc:" + ((int) b));
            sum += b;
        }
        Log.d(TAG, "calcCrc: " + sum);
        return sum;
    }

    private int calculationCrc(short magic, short cmdCode, int jsonLen, int rawLen) {
        byte[] magicBytes = XtcConvertUtil.short2Byte(magic);
        byte[] cmdBytes = XtcConvertUtil.short2Byte(cmdCode);
        byte[] jsonLenBytes = XtcConvertUtil.int2Byte(jsonLen);
        byte[] rawLenBytes = XtcConvertUtil.int2Byte(rawLen);
        int sum = 0;
        for (byte b : magicBytes) {
            sum += b;
        }
        for (byte b : cmdBytes) {
            sum += b;
        }
        for (byte b : jsonLenBytes) {
            sum += b;
        }
        for (byte b : rawLenBytes) {
            sum += b;
        }
        Log.d(TAG, "calculationCrc: " + sum);
        return sum;
    }

    public ByteBuffer buildCommCmdPkg(short magic, short cmdCode, String json, byte[] raw) {
        int jsonLen = !TextUtils.isEmpty(json) ? json.getBytes(charset).length : 0;
        int rawLen = raw != null ? raw.length : 0;
        ByteBuffer header = buildHeader(magic, cmdCode, jsonLen, rawLen);
        ByteBuffer jsonBuf = buildJson(json);
        ByteBuffer rawBuf = buildRaw(raw);
        ByteBuffer packet = ByteBuffer.allocate(jsonLen + XtcSocketHeader.HEAD_BYTE_SIZE + rawLen);
        header.flip();
        packet.put(header);
        if (jsonLen != 0) {
            jsonBuf.flip();
            packet.put(jsonBuf);
        }
        if (rawLen != 0) {
            rawBuf.flip();
            packet.put(rawBuf);
        }
        return packet;
    }

    public ByteBuffer buildJsonAndRaw(String json, byte[] raw) {
        int jsonLen = !TextUtils.isEmpty(json) ? json.getBytes(charset).length : 0;
        int rawLen = raw != null ? raw.length : 0;
        if (jsonLen + rawLen == 0) {
            return null;
        }
        ByteBuffer jsonBuf = buildJson(json);
        ByteBuffer rawBuf = buildRaw(raw);
        ByteBuffer packet = ByteBuffer.allocate(jsonLen + rawLen);
        if (jsonLen != 0) {
            jsonBuf.flip();
            packet.put(jsonBuf);
        }
        if (rawLen != 0) {
            rawBuf.flip();
            packet.put(rawBuf);
        }
        return packet;
    }

    public ByteBuffer buildPkgWithoutRaw(short magic, short cmdCode, String json, int rawLen) {
        int jsonLen = TextUtils.isEmpty(json) ? 0 : json.getBytes(charset).length;
        ByteBuffer header = buildHeader(magic, cmdCode, jsonLen, rawLen);
        ByteBuffer jsonBuf = buildJson(json);
        ByteBuffer packet = ByteBuffer.allocate(jsonLen + XtcSocketHeader.HEAD_BYTE_SIZE);
        header.flip();
        packet.put(header);
        if (jsonLen != 0) {
            jsonBuf.flip();
            packet.put(jsonBuf);
        }
        return packet;
    }

    public XtcSocketHeader parseHeader(ByteBuffer buffer) {
        if (buffer == null || buffer.limit() != XtcSocketHeader.HEAD_BYTE_SIZE) {
            return null;
        }
        Log.d(TAG, "handleRead parseHeader:" + buffer);
        buffer.flip();
        XtcSocketHeader header = new XtcSocketHeader();
        header.magic = buffer.getShort();
        header.cmdCode = buffer.getShort();
        header.jsonDataLen = buffer.getInt();
        header.rawDataLen = buffer.getInt();
        header.crc = buffer.getInt();
        buffer.get(header.reserve);
        return header;
    }
}