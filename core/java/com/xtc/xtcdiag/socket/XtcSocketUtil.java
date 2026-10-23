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

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the NIO selector threads that serve the PC-side diag tool (and, as a
 * client, the BBAT baseband diag socket).
 *
 * @hide
 */
public class XtcSocketUtil {
    private static final String TAG = "XtcDiag-SocketUtil";

    private static final int XTC_SOCKET_PORT = 9988;
    private static final int MAX_RESTART_SERVER_TIMES = 5;

    private static final String KEY_CMD_CODE = "cmdCode";
    private static final String KEY_JSON_DATA = "jsonData";
    private static final String KEY_RAW_DATA = "rawData";
    private static final String KEY_SRC_FILE = "srcFile";
    private static final String KEY_FILE_OPERATE = "fileOperate";

    private static final Object syncObj = new Object();
    private static volatile boolean hasStopServer = false;
    private static volatile boolean hasStopClient = false;
    private static final AtomicInteger restartServerAtomicTime = new AtomicInteger(0);

    private int mSocketPort = XTC_SOCKET_PORT;
    private XtcSocketHandler mSocketHandler;
    private SocketServerThread mSocketServerThread;
    private SocketClientThread mSocketClientThread;

    class SendMsgTask extends AsyncTask<Bundle, Void, Void> {
        private void sendCommonCmd(Bundle bundle) {
            short cmdCode = bundle.getShort(KEY_CMD_CODE);
            String json = bundle.getString(KEY_JSON_DATA);
            byte[] raw = bundle.getByteArray(KEY_RAW_DATA);
            if (mSocketServerThread != null) {
                mSocketServerThread.handleSendCommData(cmdCode, json, raw);
            } else if (mSocketClientThread != null) {
                mSocketClientThread.handleSendCommData(cmdCode, json, raw);
            }
        }

        private void sendFileCmd(Bundle bundle) {
            short cmdCode = bundle.getShort(KEY_CMD_CODE);
            String srcFile = bundle.getString(KEY_SRC_FILE);
            XtcFileOperate fileOperate = bundle.getParcelable(KEY_FILE_OPERATE);
            Log.d(TAG, "srcFilePath:" + srcFile + " ,opCode:" + fileOperate);
            if (mSocketServerThread != null) {
                mSocketServerThread.handleSendFileData(cmdCode, srcFile, fileOperate);
            } else if (mSocketClientThread != null) {
                mSocketClientThread.handleSendFileData(cmdCode, srcFile, fileOperate);
            }
        }

        @Override
        protected Void doInBackground(Bundle... bundles) {
            if (bundles == null || bundles.length == 0 || bundles[0] == null) {
                return null;
            }
            short cmdCode = bundles[0].getShort(KEY_CMD_CODE);
            Log.d(TAG, "SendMsgTask cmdCode:" + ((int) cmdCode) + " ,mSocketPort:" + mSocketPort
                    + " ,workThread" + Thread.currentThread() + " ,id:"
                    + Thread.currentThread().getId());
            if (cmdCode == XtcSocketCmdCode.CMD_FILE_OPERATE) {
                sendFileCmd(bundles[0]);
            } else {
                sendCommonCmd(bundles[0]);
            }
            return null;
        }
    }

    class SocketClientThread extends Thread {
        private final String serverIp;
        private Selector selector;
        private SocketChannel socketChannel;

        SocketClientThread(String serverIp) {
            this.serverIp = serverIp;
        }

        public void handleSendCommData(short cmdCode, String json, byte[] raw) {
            try {
                if (mSocketHandler != null) {
                    mSocketHandler.handleSendCommData(cmdCode, json, raw);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "SocketClientThread send: " + e);
            }
        }

        public void handleSendFileData(short cmdCode, String srcFile, XtcFileOperate fileOperate) {
            try {
                if (mSocketHandler != null) {
                    mSocketHandler.handleSendFileData(cmdCode, srcFile, fileOperate);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "SocketClientThread send: " + e);
            }
        }

        public boolean isSocketConnected() {
            if (mSocketHandler == null) {
                return false;
            }
            return mSocketHandler.isConnected();
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "SocketClientThread run: " + Thread.currentThread()
                        + " ,!hasStopClient:" + (!hasStopClient));
                selector = Selector.open();
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
                socketChannel.connect(new InetSocketAddress(serverIp, mSocketPort));
                socketChannel.register(selector, SelectionKey.OP_CONNECT);
                Log.d(TAG, "ClientSelectorThread socketChannel: " + socketChannel);
                while (!hasStopClient) {
                    if (selector.select() != 0) {
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            if (key.isConnectable() && mSocketHandler != null) {
                                mSocketHandler.handleConnect(key);
                            }
                            if (key.isReadable() && mSocketHandler != null) {
                                mSocketHandler.handleRead(key);
                            }
                            iterator.remove();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "startClient exception: " + e);
                if (mSocketHandler != null) {
                    mSocketHandler.doSocketStateCallBack(XtcSocketCallback.STATE_DISCONNECT, null);
                }
            }
        }

        public void shutdown() {
            hasStopClient = true;
            try {
                if (mSocketHandler != null) {
                    mSocketHandler.handleDisconnect();
                }
                if (socketChannel != null) {
                    socketChannel.socket().close();
                    socketChannel.close();
                    socketChannel = null;
                }
                if (selector != null) {
                    selector.close();
                    selector = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "clientThread shutdown thread:" + Thread.currentThread()
                        + " ,ex:" + e);
            }
        }
    }

    class SocketServerThread extends Thread {
        private Selector selector;
        private ServerSocketChannel serverSocketChannel;

        public void handleSendCommData(short cmdCode, String json, byte[] raw) {
            try {
                if (mSocketHandler != null) {
                    mSocketHandler.handleSendCommData(cmdCode, json, raw);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "SocketServerThread handleSendCommData exception thread:"
                        + Thread.currentThread() + " ,port:" + mSocketPort + " ,ex:" + e);
            }
        }

        public void handleSendFileData(short cmdCode, String srcFile, XtcFileOperate fileOperate) {
            try {
                if (mSocketHandler != null) {
                    mSocketHandler.handleSendFileData(cmdCode, srcFile, fileOperate);
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "SocketServerThread handleSendFileData exception thread:"
                        + Thread.currentThread() + " ,port:" + mSocketPort + " ,ex:" + e);
            }
        }

        public boolean isSocketConnected() {
            if (mSocketHandler == null) {
                return false;
            }
            return mSocketHandler.isConnected();
        }

        @Override
        public void run() {
            try {
                Log.d(TAG, "startServer port:" + mSocketPort + " ,!hasStopServer:" + (!hasStopServer)
                        + " ,thread:" + Thread.currentThread());
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.socket().setReuseAddress(true);
                serverSocketChannel.socket().bind(new InetSocketAddress(mSocketPort));
                selector = Selector.open();
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
                while (!hasStopServer && selector != null && selector.isOpen()
                        && serverSocketChannel != null) {
                    int readyChannels = selector.select();
                    if (readyChannels != 0) {
                        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        Log.d(TAG, "readyChannels:" + readyChannels + " ,iterator.size:"
                                + selector.selectedKeys().size());
                        while (iterator.hasNext()) {
                            SelectionKey key = iterator.next();
                            if (key.isAcceptable() && mSocketHandler != null) {
                                mSocketHandler.handleAccept(key);
                                restartServerAtomicTime.set(0);
                            }
                            if (key.isReadable() && mSocketHandler != null) {
                                mSocketHandler.handleRead(key);
                            }
                            iterator.remove();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "startServer exception thread:" + Thread.currentThread()
                        + " ,port:" + mSocketPort + " restartServerAtomicTime:"
                        + restartServerAtomicTime.get() + " ,ex:" + e);
                if (restartServerAtomicTime.incrementAndGet() < MAX_RESTART_SERVER_TIMES) {
                    stopServer();
                    startServer();
                }
                if (mSocketHandler != null) {
                    mSocketHandler.doSocketStateCallBack(XtcSocketCallback.STATE_DISCONNECT, null);
                }
            }
        }

        public void shutdown() {
            try {
                Log.d(TAG, "startServer shutdown mSocketHandler:" + mSocketHandler
                        + " ,serverSocketChannel:" + serverSocketChannel + " ,selector:" + selector
                        + " ,port:" + mSocketPort + " ,thread:" + Thread.currentThread());
                if (mSocketHandler != null) {
                    mSocketHandler.handleDisconnect();
                }
                if (serverSocketChannel != null) {
                    serverSocketChannel.socket().close();
                    serverSocketChannel.close();
                    serverSocketChannel = null;
                }
                if (selector != null) {
                    selector.close();
                    selector = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "startServer shutdown exception thread:" + Thread.currentThread()
                        + " ,port:" + mSocketPort + " ,ex:" + e);
            }
        }
    }

    public XtcSocketUtil() {
        this(XTC_SOCKET_PORT, new XtcSocketHandler());
    }

    public XtcSocketUtil(int port) {
        this(port, new XtcSocketHandler());
    }

    public XtcSocketUtil(int port, XtcSocketHandler handler) {
        mSocketPort = port;
        mSocketHandler = handler;
    }

    public boolean isSocketConnected() {
        if (mSocketServerThread != null) {
            return mSocketServerThread.isSocketConnected();
        }
        if (mSocketClientThread != null) {
            return mSocketClientThread.isSocketConnected();
        }
        return false;
    }

    public void release() {
        stopServer();
        stopClient();
    }

    public void send(short cmdCode, String json, byte[] raw) {
        Bundle bundle = new Bundle();
        bundle.putShort(KEY_CMD_CODE, cmdCode);
        bundle.putString(KEY_JSON_DATA, json);
        bundle.putByteArray(KEY_RAW_DATA, raw);
        new SendMsgTask().execute(bundle);
    }

    public void sendFile(String srcFile, String fileName, String destFilePath, int operateCode) {
        XtcFileOperate fileOperate = new XtcFileOperate(destFilePath, fileName, operateCode);
        Bundle bundle = new Bundle();
        bundle.putShort(KEY_CMD_CODE, XtcSocketCmdCode.CMD_FILE_OPERATE);
        bundle.putString(KEY_SRC_FILE, srcFile);
        bundle.putParcelable(KEY_FILE_OPERATE, fileOperate);
        new SendMsgTask().execute(bundle);
    }

    public void setDataCallBack(XtcDataCallback dataCallback) {
        if (mSocketHandler != null) {
            mSocketHandler.setDataCallBack(dataCallback);
        }
    }

    public void setListenPort(int port) {
        mSocketPort = port;
    }

    public void setSocketCallBack(XtcSocketCallback socketCallback) {
        if (mSocketHandler != null) {
            mSocketHandler.setSocketCallBack(socketCallback);
        }
    }

    public void setSocketHandler(XtcSocketHandler handler) {
        mSocketHandler = handler;
    }

    public void startClient(String serverIp) {
        synchronized (syncObj) {
            if (mSocketClientThread != null) {
                Log.d(TAG, "startClient already to start.");
                return;
            }
            hasStopClient = false;
            mSocketClientThread = new SocketClientThread(serverIp);
            mSocketClientThread.start();
        }
    }

    public void startServer() {
        synchronized (syncObj) {
            if (mSocketServerThread != null) {
                Log.d(TAG, "startServer already to start.");
                return;
            }
            hasStopServer = false;
            mSocketServerThread = new SocketServerThread();
            mSocketServerThread.start();
        }
    }

    public void stopClient() {
        synchronized (syncObj) {
            hasStopClient = true;
            if (mSocketClientThread != null) {
                mSocketClientThread.shutdown();
                mSocketClientThread.interrupt();
                mSocketClientThread = null;
            }
        }
    }

    public void stopServer() {
        synchronized (syncObj) {
            hasStopServer = true;
            if (mSocketServerThread != null) {
                mSocketServerThread.shutdown();
                mSocketServerThread.interrupt();
                mSocketServerThread = null;
            }
        }
    }
}