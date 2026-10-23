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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 * File helpers used by the diag file transfer protocol.
 *
 * @hide
 */
public class XtcFileUtil {
    private static final String TAG = "XtcDiag-FileUtil";

    public static final String DEFAULT_SAVE_PATH = "/data/xtc_diag/";
    public static final String DEFAULT_FILE_NAME = "tmp";

    public void createDirIfNotExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }
        String normalized = filePath.replaceAll("\\\\", "/");
        File dir = new File(normalized.substring(0, normalized.lastIndexOf("/")));
        if (dir.exists()) {
            return;
        }
        dir.mkdirs();
    }

    public boolean deleteDir(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        boolean success = true;
        for (File file : files) {
            if (!file.isFile()) {
                success = deleteDir(file.getAbsolutePath());
            } else {
                success = deleteFile(file.getAbsolutePath());
            }
            if (!success) {
                break;
            }
        }
        if (success) {
            return dir.delete();
        }
        return false;
    }

    public boolean deleteFile(String filePath) {
        File file = new File(filePath);
        if (file.isFile() && file.exists()) {
            return file.delete();
        }
        return false;
    }

    public boolean deleteLocalFileOrDir(String path) {
        if (TextUtils.isEmpty(path)) {
            path = DEFAULT_SAVE_PATH;
        }
        File file = new File(path);
        if (!file.exists()) {
            return false;
        }
        return file.isFile() ? deleteFile(path) : deleteDir(path);
    }

    public long getFileLength(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return 0L;
        }
        File file = new File(filePath);
        if (file.exists()) {
            return file.length();
        }
        return 0L;
    }

    public boolean isFileExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        return new File(filePath).exists();
    }

    public String writeFile2Local(SocketChannel socketChannel, String destPath, String fileName,
            int totalSize) throws IOException {
        if (socketChannel == null) {
            return null;
        }
        if (TextUtils.isEmpty(fileName)) {
            fileName = DEFAULT_FILE_NAME;
        }
        if (TextUtils.isEmpty(destPath)) {
            destPath = DEFAULT_SAVE_PATH + fileName;
        }
        createDirIfNotExist(destPath);
        FileOutputStream outputStream = new FileOutputStream(destPath);
        FileChannel fileChannel = outputStream.getChannel();
        int chunk = totalSize <= 48 ? totalSize : 48;
        ByteBuffer buffer = ByteBuffer.allocate(chunk);
        int readSize = socketChannel.read(buffer);
        Log.d(TAG, "XtcFileUtil file write start fileTotalSize:" + totalSize
                + " ,readSize:" + readSize);
        int totalRead = readSize;
        while (totalRead <= totalSize) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                fileChannel.write(buffer);
            }
            if (totalRead == totalSize) {
                break;
            }
            buffer.clear();
            if (totalSize - totalRead <= chunk) {
                buffer = ByteBuffer.allocate(totalSize - totalRead);
            }
            totalRead += socketChannel.read(buffer);
        }
        fileChannel.force(true);
        outputStream.close();
        fileChannel.close();
        Log.d(TAG, "XtcFileUtil write file success:" + destPath + " ,readSize:" + totalRead);
        return destPath;
    }

    public void writeFile2Socket(SocketChannel socketChannel, String filePath) throws IOException {
        if (socketChannel == null || TextUtils.isEmpty(filePath)) {
            return;
        }
        FileInputStream inputStream = new FileInputStream(filePath);
        FileChannel fileChannel = inputStream.getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(48);
        for (int read = fileChannel.read(buffer); read != -1; read = fileChannel.read(buffer)) {
            buffer.flip();
            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }
            buffer.clear();
        }
        inputStream.close();
        fileChannel.close();
    }
}