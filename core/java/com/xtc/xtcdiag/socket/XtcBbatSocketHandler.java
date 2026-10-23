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

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Socket handler for the escaped binary protocol spoken with the BBAT/diag
 * baseband tool.
 *
 * @hide
 */
public class XtcBbatSocketHandler extends XtcSocketHandler {
    private String TAG = "XtcDiag-BbatSocketHandler";

    private short CMD_BBAT_OPERATE = XtcSocketCmdCode.CMD_BBAT_RF_OPERATE;
    private final byte BBAT_SEND_HEADER = XtcBbatSocketHeader.PKG_TAG;
    private final int BBAT_HEADER_SIZE = 7;
    private final Object syncBbatObj = new Object();

    private ByteBuffer readBbatSocket(SocketChannel socketChannel, int size) throws IOException {
        synchronized (syncBbatObj) {
            int read = 0;
            ByteBuffer buffer = ByteBuffer.allocate(size);
            while (read < size) {
                ByteBuffer part = ByteBuffer.allocate(size - read);
                int count = socketChannel.read(part);
                if (count == -1) {
                    String remoteIp = getRemoteIp(socketChannel);
                    Log.w(TAG, "no msg to read, bbat socket close ! remoteIp:" + remoteIp);
                    socketChannel.close();
                    doSocketStateCallBack(XtcSocketCallback.STATE_DISCONNECT, remoteIp);
                    return null;
                }
                if (count > 0) {
                    part.flip();
                    buffer.put(part);
                }
                read += count;
            }
            return buffer;
        }
    }

    @Override
    protected void handleRead(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        getRemoteIp(socketChannel);
        ByteBuffer headerBuf = readBbatSocket(socketChannel, BBAT_HEADER_SIZE);
        if (headerBuf == null) {
            Log.d(TAG, XtcSocketChannel.BBAT_SOCKET_IN + " <- Bbat handleRead fail !!");
            return;
        }
        int escapeCount = 0;
        byte prev = 0;
        for (int i = 0; i < headerBuf.limit(); i++) {
            byte current = headerBuf.get(i);
            if (prev == 125) {
                Log.d(TAG, "Bbat handleRead headerByte(7d:125, 5e:94, 5d:93):" + ((int) current));
                if (current == 94 || current == 93) {
                    escapeCount++;
                }
            }
            prev = current;
        }
        ByteBuffer trueHeader = ByteBuffer.allocate(escapeCount + BBAT_HEADER_SIZE);
        headerBuf.flip();
        trueHeader.put(headerBuf);
        if (escapeCount > 0) {
            Log.d(TAG, "Bbat handleRead header7eCount>0:" + escapeCount);
            ByteBuffer extra = readBbatSocket(socketChannel, escapeCount);
            if (extra != null) {
                extra.flip();
                trueHeader.put(extra);
            }
        }
        Log.d(TAG, "Bbat handleRead trueHeaderBuf:" + trueHeader + " ,trueHeaderBufArray:"
                + XtcConvertUtil.byte2Hex(trueHeader.array()));
        ByteBuffer lenBuf = ByteBuffer.allocate(2);
        lenBuf.put(trueHeader.array(), trueHeader.limit() - 2, 2);
        lenBuf.flip();
        lenBuf.order(ByteOrder.LITTLE_ENDIAN);
        int bbatMsgLen = lenBuf.getShort() + 2 + escapeCount;
        Log.d(TAG, "Bbat handleRead bbatMsgLen:" + bbatMsgLen + " ,header7eCount:" + escapeCount);
        trueHeader.flip();
        ByteBuffer msgBuf = ByteBuffer.allocate(bbatMsgLen);
        msgBuf.put(trueHeader);
        ByteBuffer result;
        if (bbatMsgLen <= 0) {
            result = msgBuf;
        } else {
            int remain = bbatMsgLen - trueHeader.limit();
            ByteBuffer body = readBbatSocket(socketChannel, remain);
            if (body != null) {
                body.flip();
                msgBuf.put(body);
            }
            if (body == null || body.get(body.limit() - 1) == XtcBbatSocketHeader.PKG_TAG) {
                result = msgBuf;
            } else {
                Log.d(TAG, "Bbat handleRead normalData the last byte not 7e !!");
                ByteBuffer tail = ByteBuffer.allocate(remain);
                ByteBuffer one = ByteBuffer.allocate(1);
                int tailLen = socketChannel.read(one);
                one.flip();
                tail.put(one);
                while (one.get(0) != XtcBbatSocketHeader.PKG_TAG) {
                    one.clear();
                    tailLen += socketChannel.read(one);
                    one.flip();
                    tail.put(one);
                }
                if (tailLen > 0) {
                    ByteBuffer combined = ByteBuffer.allocate(bbatMsgLen + tailLen);
                    msgBuf.flip();
                    combined.put(msgBuf);
                    tail.flip();
                    combined.put(tail);
                    combined.flip();
                    result = ByteBuffer.wrap(combined.array());
                } else {
                    result = msgBuf;
                }
            }
        }
        Log.d(TAG, XtcSocketChannel.BBAT_SOCKET_IN + " <- Bbat handleReadData fromBbat:" + result);
        doReceiveCallback(CMD_BBAT_OPERATE, null, result.array());
        headerBuf.clear();
        trueHeader.clear();
        result.clear();
    }

    @Override
    protected void handleSendCommData(short cmdCode, String json, byte[] raw) throws IOException {
        if (mChannel == null || !mChannel.isConnected()) {
            Log.w(TAG, "you could not handleWrite data when socketChannel:" + mChannel);
            return;
        }
        ByteBuffer packet = new XtcSocketMsg().buildJsonAndRaw(json, raw);
        if (packet == null) {
            return;
        }
        Log.d(TAG, XtcSocketChannel.BBAT_SOCKET_OUT + " -> BbathandleSendCommData send2Bbat cmdCode:"
                + ((int) cmdCode) + " ,rawLen:" + (raw == null ? 0 : raw.length));
        packet.flip();
        while (packet.hasRemaining()) {
            mChannel.write(packet);
        }
    }

    public void setBBatCmdType(short cmdType) {
        CMD_BBAT_OPERATE = cmdType;
    }
}