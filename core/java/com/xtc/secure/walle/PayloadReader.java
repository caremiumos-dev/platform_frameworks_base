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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Map;

/**
 * Reads an APK Signing Block entry payload by block id.
 *
 * @hide
 */
public final class PayloadReader {
    private PayloadReader() {
    }

    public static byte[] get(File apkFile, int id)
            throws IOException, SignatureNotFoundException {
        Map<Integer, ByteBuffer> idValues = getAll(apkFile);
        if (idValues == null) {
            return null;
        }
        ByteBuffer buffer = idValues.get(id);
        if (buffer == null) {
            return null;
        }
        return getBytes(buffer);
    }

    private static Map<Integer, ByteBuffer> getAll(File apkFile)
            throws IOException, SignatureNotFoundException {
        try (RandomAccessFile raf = new RandomAccessFile(apkFile, "r")) {
            FileChannel channel = raf.getChannel();
            try {
                return ApkUtil.findIdValues(ApkUtil.findApkSigningBlock(channel).getFirst());
            } finally {
                channel.close();
            }
        }
    }

    private static byte[] getBytes(ByteBuffer buffer) {
        byte[] array = buffer.array();
        int offset = buffer.arrayOffset();
        return Arrays.copyOfRange(array, buffer.position() + offset, offset + buffer.limit());
    }

    public static String getString(File apkFile, int id)
            throws IOException, SignatureNotFoundException {
        byte[] bytes = get(apkFile, id);
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, ApkUtil.DEFAULT_CHARSET);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
}