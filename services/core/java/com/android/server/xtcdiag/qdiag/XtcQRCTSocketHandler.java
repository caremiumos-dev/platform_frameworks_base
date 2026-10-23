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

package com.android.server.xtcdiag.qdiag;

import android.util.Log;

import com.xtc.xtcdiag.socket.XtcConvertUtil;
import com.xtc.xtcdiag.socket.XtcSocketCallback;
import com.xtc.xtcdiag.socket.XtcSocketHandler;
import com.xtc.xtcdiag.socket.XtcSocketMsg;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * Socket handler for the Qualcomm diag socket, which forwards raw packets.
 *
 * @hide
 */
public class XtcQRCTSocketHandler extends XtcSocketHandler {
    protected String TAG = "XtcDiag-QDiagSocketHandler";

    private short CMD_BBAT_OPERATE = 32;
    private final int MAX_RECE_PKG_SIZE = 2048;
    private final Object syncObj = new Object();

    private ByteBuffer readSocket(SocketChannel socketChannel, int size, boolean returnOnce)
            throws IOException {
        synchronized (syncObj) {
            int read = 0;
            ByteBuffer buffer = ByteBuffer.allocate(size);
            while (read < size) {
                ByteBuffer part = ByteBuffer.allocate(size - read);
                int count = socketChannel.read(part);
                if (count == -1) {
                    String remoteIp = getRemoteIp(socketChannel);
                    Log.w(TAG, "no msg to read, xtc socket close ! remoteIp:" + remoteIp);
                    socketChannel.close();
                    doSocketStateCallBack(XtcSocketCallback.STATE_DISCONNECT, remoteIp);
                    return null;
                }
                if (count > 0) {
                    part.flip();
                    buffer.put(part);
                    if (returnOnce) {
                        ByteBuffer once = ByteBuffer.allocate(count);
                        part.flip();
                        once.put(part);
                        Log.d(TAG, "handleRead once buf:" + once + " , bufArray:"
                                + XtcConvertUtil.byte2Hex(once.array()));
                        return once;
                    }
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
        ByteBuffer buffer = readSocket(socketChannel, MAX_RECE_PKG_SIZE, true);
        doReceiveCallback(CMD_BBAT_OPERATE, null, buffer == null ? null : buffer.array());
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
        packet.flip();
        while (packet.hasRemaining()) {
            mChannel.write(packet);
        }
    }
}