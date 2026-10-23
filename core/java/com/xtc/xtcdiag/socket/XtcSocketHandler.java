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

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Handles the read/write side of a diag socket, both for the PC connection and
 * for the BBAT baseband connection.
 *
 * @hide
 */
public class XtcSocketHandler {
    protected String TAG = "XtcDiag-SocketHandler";

    protected SocketChannel mChannel;
    private XtcDataCallback mDataCallback;
    private XtcSocketCallback mSocketCallback;
    private final Object syncObj = new Object();

    private void handleReceiveCommCmd(SocketChannel socketChannel, short cmdCode, int jsonLen,
            int rawLen) throws IOException {
        String json = null;
        if (jsonLen > 0) {
            ByteBuffer jsonBuf = readSocket(socketChannel, jsonLen);
            if (jsonBuf != null) {
                json = new String(jsonBuf.array(), XtcSocketMsg.charset);
            }
        }
        ByteBuffer rawBuf = rawLen > 0 ? readSocket(socketChannel, rawLen) : null;
        doReceiveCallback(cmdCode, json, rawBuf != null ? rawBuf.array() : null);
    }

    private void handleReceiveFileCmd(SocketChannel socketChannel, short cmdCode, int jsonLen,
            int rawLen) throws IOException {
        String json = null;
        if (jsonLen > 0) {
            ByteBuffer jsonBuf = readSocket(socketChannel, jsonLen);
            if (jsonBuf != null) {
                json = new String(jsonBuf.array(), XtcSocketMsg.charset);
            }
        }
        if (TextUtils.isEmpty(json)) {
            Log.w(TAG, "handleReceiveFileCmd has no json data !");
            return;
        }
        try {
            JSONObject object = new JSONObject(json);
            String destFilePath = object.getString(ItemJsonKey.ITEM_FILE_KEY_DEST_PATH);
            String fileName = object.getString(ItemJsonKey.ITEM_FILE_KEY_FILE_NAME);
            int opCode = object.getInt(ItemJsonKey.ITEM_FILE_KEY_OP_CODE);
            int fileSize = object.optInt(ItemJsonKey.ITEM_FILE_KEY_FILE_SIZE);
            Log.d(TAG, XtcSocketChannel.XTC_SOCKET_IN + " -> handleReceiveFileCmd destFilePath:"
                    + destFilePath + " ,fileName:" + fileName + " ,fileOpCode:" + opCode
                    + " ,fileSize:" + fileSize + " ,rawDataLen:" + rawLen);
            XtcFileUtil fileUtil = new XtcFileUtil();
            if (opCode == XtcSocketCmdCode.FileOperateCode.FILE_OPERATE_WRITE) {
                String saved = fileUtil.writeFile2Local(socketChannel, destFilePath, fileName,
                        fileSize);
                Log.d(TAG, XtcSocketChannel.XTC_SOCKET_IN + " -> writeFile2Local");
                notifyFileOperate2Pc(cmdCode, destFilePath, fileName, opCode,
                        fileUtil.getFileLength(destFilePath), fileUtil.isFileExist(saved));
            } else if (opCode == XtcSocketCmdCode.FileOperateCode.FILE_OPERATE_READ) {
                handleSendFileData(XtcSocketCmdCode.CMD_FILE_OPERATE, destFilePath,
                        new XtcFileOperate("", fileName,
                                XtcSocketCmdCode.FileOperateCode.FILE_OPERATE_READ));
            } else if (opCode == XtcSocketCmdCode.FileOperateCode.FILE_OPERATE_DELETE) {
                Log.d(TAG, XtcSocketChannel.XTC_SOCKET_IN + " -> deleteLocalFileOrDir result:"
                        + fileUtil.deleteLocalFileOrDir(destFilePath));
                notifyFileOperate2Pc(cmdCode, destFilePath, fileName, opCode,
                        fileUtil.getFileLength(destFilePath), fileUtil.isFileExist(destFilePath));
            } else if (opCode == XtcSocketCmdCode.FileOperateCode.FILE_OPERATE_EXIST) {
                notifyFileOperate2Pc(cmdCode, destFilePath, fileName, opCode,
                        fileUtil.getFileLength(destFilePath), fileUtil.isFileExist(destFilePath));
            } else if (opCode == XtcSocketCmdCode.FileOperateCode.FILE_OPERATE_FILELEN) {
                notifyFileOperate2Pc(cmdCode, destFilePath, fileName, opCode,
                        fileUtil.getFileLength(destFilePath), fileUtil.isFileExist(destFilePath));
            }
        } catch (Exception e) {
            Log.w(TAG, "handleReceiveFileCmd JSON Format wrong !");
        }
    }

    private ByteBuffer readSocket(SocketChannel socketChannel, int size) throws IOException {
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
                }
                read += count;
            }
            return buffer;
        }
    }

    public void doReceiveCallback(short cmdCode, String json, byte[] raw) {
        if (mDataCallback != null) {
            mDataCallback.onCmdReceive(cmdCode, json, raw);
        }
    }

    public void doSocketStateCallBack(int state, String remoteIp) {
        if (mSocketCallback != null) {
            mSocketCallback.onConnectStateChange(state, remoteIp);
        }
    }

    protected String getRemoteIp(SocketChannel socketChannel) {
        if (socketChannel == null) {
            return null;
        }
        Socket socket = socketChannel.socket();
        if (socket == null || !socket.isConnected()) {
            return null;
        }
        return socket.getInetAddress().getHostAddress();
    }

    protected void handleAccept(SelectionKey selectionKey) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectionKey.channel();
        mChannel = serverSocketChannel.accept();
        mChannel.configureBlocking(false);
        mChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
        String remoteIp = getRemoteIp(mChannel);
        doSocketStateCallBack(XtcSocketCallback.STATE_CONNECTED, remoteIp);
        Log.d(TAG, "handleAccept serverSocketChannel:" + serverSocketChannel
                + " ,socketChannel:" + mChannel + " ,remote ip:" + remoteIp);
    }

    protected void handleConnect(SelectionKey selectionKey) throws IOException {
        mChannel = (SocketChannel) selectionKey.channel();
        if (mChannel.isConnectionPending()) {
            mChannel.finishConnect();
        }
        mChannel.configureBlocking(false);
        mChannel.register(selectionKey.selector(), SelectionKey.OP_READ);
        String remoteIp = getRemoteIp(mChannel);
        doSocketStateCallBack(XtcSocketCallback.STATE_CONNECTED, remoteIp);
        Log.d(TAG, "handleConnect channel:" + mChannel + "remote ip:" + remoteIp);
    }

    protected void handleDisconnect() throws IOException {
        if (mChannel != null) {
            String remoteIp = getRemoteIp(mChannel);
            Socket socket = mChannel.socket();
            Log.d(TAG, "handleDisconnect isConnected:" + socket.isConnected());
            if (socket.isConnected()) {
                socket.close();
                mChannel.close();
                Log.d(TAG, "handleDisconnect close:" + remoteIp);
            }
            doSocketStateCallBack(XtcSocketCallback.STATE_DISCONNECT, remoteIp);
        }
    }

    protected void handleRead(SelectionKey selectionKey) throws IOException {
        SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
        String remoteIp = getRemoteIp(socketChannel);
        XtcSocketHeader header = new XtcSocketMsg().parseHeader(readSocket(socketChannel,
                XtcSocketHeader.HEAD_BYTE_SIZE));
        if (header == null || header.magic != XtcSocketMsg.HEADER_MAGIC) {
            Log.w(TAG, "Client Msg Format Error !  header:" + header + " ,magic:"
                    + (header == null ? "null" : Short.valueOf(header.magic)));
            socketChannel.close();
            doSocketStateCallBack(XtcSocketCallback.STATE_DISCONNECT, remoteIp);
            return;
        }
        short cmdCode = header.cmdCode;
        int jsonLen = header.jsonDataLen;
        int rawLen = header.rawDataLen;
        Log.d(TAG, XtcSocketChannel.XTC_SOCKET_IN + " -> handleRead FromPc cmdCode:" + ((int) cmdCode)
                + " ,jsonDataLen:" + jsonLen + " ,rawDataLen:" + rawLen);
        if (cmdCode == XtcSocketCmdCode.CMD_FILE_OPERATE) {
            handleReceiveFileCmd(socketChannel, cmdCode, jsonLen, rawLen);
        } else {
            handleReceiveCommCmd(socketChannel, cmdCode, jsonLen, rawLen);
        }
    }

    protected void handleSendCommData(short cmdCode, String json, byte[] raw) throws IOException {
        if (mChannel == null || !mChannel.isConnected()) {
            Log.w(TAG, "you could not handleWrite data when socketChannel:" + mChannel);
            return;
        }
        ByteBuffer packet = new XtcSocketMsg().buildCommCmdPkg(XtcSocketMsg.HEADER_MAGIC, cmdCode,
                json, raw);
        Log.d(TAG, XtcSocketChannel.XTC_SOCKET_OUT + " <- handleSendCommData 2Pc cmdCode:"
                + ((int) cmdCode) + " ,rawLen:" + (raw == null ? 0 : raw.length));
        packet.flip();
        while (packet.hasRemaining()) {
            mChannel.write(packet);
        }
    }

    protected void handleSendFileData(short cmdCode, String srcFile, XtcFileOperate fileOperate)
            throws IOException {
        if (mChannel == null || !mChannel.isConnected()) {
            Log.w(TAG, "you could not handleWrite data when socketChannel:" + mChannel);
            return;
        }
        if (fileOperate == null) {
            Log.w(TAG, "the XtcFileOperate is null !");
            return;
        }
        String destFilePath = fileOperate.getDestFilePath();
        String fileName = fileOperate.getFileName();
        int operateCode = fileOperate.getOperateCode();
        File file = new File(srcFile);
        if (!file.exists()) {
            Log.w(TAG, "the srcFile is not exist !");
            notifyFileOperate2Pc(cmdCode, destFilePath, fileName, operateCode, 0L, false);
            return;
        }
        int fileSize = (int) file.length();
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put(ItemJsonKey.ITEM_KEY_TEST_ID, cmdCode);
        format.put(ItemJsonKey.ITEM_KEY_TEST_KEY, "file_operate");
        format.put(ItemJsonKey.ITEM_KEY_RESULE, "mmi_pass");
        XtcSocketJsonFormat item = new XtcSocketJsonFormat();
        item.put(ItemJsonKey.ITEM_FILE_KEY_DEST_PATH, destFilePath);
        item.put(ItemJsonKey.ITEM_FILE_KEY_FILE_NAME, fileName);
        item.put(ItemJsonKey.ITEM_FILE_KEY_OP_CODE, operateCode);
        item.put(ItemJsonKey.ITEM_FILE_KEY_FILE_SIZE, fileSize);
        item.put(ItemJsonKey.ITEM_FILE_KEY_FILE_EXIST, (Object) true);
        format.put2Array(item.build());
        String json = format.build().toString();
        Log.d(TAG, XtcSocketChannel.XTC_SOCKET_OUT + " <- handleSendFileData fileSize:" + fileSize
                + " ,jsonData:" + json);
        ByteBuffer packet = new XtcSocketMsg().buildPkgWithoutRaw(XtcSocketMsg.HEADER_MAGIC,
                cmdCode, json, fileSize);
        packet.flip();
        while (packet.hasRemaining()) {
            mChannel.write(packet);
        }
        new XtcFileUtil().writeFile2Socket(mChannel, srcFile);
    }

    public boolean isConnected() {
        if (mChannel != null) {
            return mChannel.isConnected();
        }
        return false;
    }

    public void notifyFileOperate2Pc(short cmdCode, String destFilePath, String fileName,
            int operateCode, long fileSize, boolean fileExist) {
        XtcSocketJsonFormat format = new XtcSocketJsonFormat();
        format.put(ItemJsonKey.ITEM_KEY_TEST_ID, cmdCode);
        format.put(ItemJsonKey.ITEM_KEY_TEST_KEY, "op_file_back");
        format.put(ItemJsonKey.ITEM_KEY_RESULE, "mmi_pass");
        XtcSocketJsonFormat item = new XtcSocketJsonFormat();
        item.put(ItemJsonKey.ITEM_FILE_KEY_DEST_PATH, destFilePath);
        item.put(ItemJsonKey.ITEM_FILE_KEY_FILE_NAME, fileName);
        item.put(ItemJsonKey.ITEM_FILE_KEY_OP_CODE, operateCode);
        item.put(ItemJsonKey.ITEM_FILE_KEY_FILE_SIZE, Long.valueOf(fileSize));
        item.put(ItemJsonKey.ITEM_FILE_KEY_FILE_EXIST, Boolean.valueOf(fileExist));
        format.put2Array(item.build());
        try {
            handleSendCommData(cmdCode, format.build().toString(), null);
        } catch (Exception e) {
            Log.w(TAG, "notifyFileOperate2Pc ex:" + e);
        }
    }

    public void setDataCallBack(XtcDataCallback dataCallback) {
        mDataCallback = dataCallback;
    }

    public void setSocketCallBack(XtcSocketCallback socketCallback) {
        mSocketCallback = socketCallback;
    }
}