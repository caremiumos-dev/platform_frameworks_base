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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Low-level helpers for locating and parsing the APK Signing Block
 * (APK Signature Scheme v2), used to read the embedded channel info.
 *
 * @hide
 */
final class ApkUtil {
    public static final int ANDROID_COMMON_PAGE_ALIGNMENT_BYTES = 4096;
    public static final int APK_CHANNEL_BLOCK_ID = 1903654775;
    public static final int APK_SIGNATURE_SCHEME_V2_BLOCK_ID = 1896449818;
    public static final long APK_SIG_BLOCK_MAGIC_HI = 3617552046287187010L;
    public static final long APK_SIG_BLOCK_MAGIC_LO = 2334950737559900225L;
    private static final int APK_SIG_BLOCK_MIN_SIZE = 32;
    public static final String DEFAULT_CHARSET = "UTF-8";
    private static final int UINT16_MAX_VALUE = 65535;
    public static final int VERITY_PADDING_BLOCK_ID = 1114793335;
    private static final int ZIP_EOCD_COMMENT_LENGTH_FIELD_OFFSET = 20;
    private static final int ZIP_EOCD_REC_MIN_SIZE = 22;
    private static final int ZIP_EOCD_REC_SIG = 101010256;

    private ApkUtil() {
    }

    private static void checkByteOrderLittleEndian(ByteBuffer buffer) {
        if (buffer.order() != ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("ByteBuffer byte order must be little endian");
        }
    }

    public static Pair<ByteBuffer, Long> findApkSigningBlock(FileChannel channel)
            throws IOException, SignatureNotFoundException {
        return findApkSigningBlock(channel, findCentralDirStartOffset(channel));
    }

    public static Pair<ByteBuffer, Long> findApkSigningBlock(FileChannel channel, long centralDirOffset)
            throws IOException, SignatureNotFoundException {
        if (centralDirOffset < APK_SIG_BLOCK_MIN_SIZE) {
            throw new SignatureNotFoundException(
                    "APK too small for APK Signing Block. ZIP Central Directory offset: "
                            + centralDirOffset);
        }
        channel.position(centralDirOffset - 24);
        ByteBuffer footer = ByteBuffer.allocate(24);
        channel.read(footer);
        footer.order(ByteOrder.LITTLE_ENDIAN);
        if (footer.getLong(8) != APK_SIG_BLOCK_MAGIC_LO
                || footer.getLong(16) != APK_SIG_BLOCK_MAGIC_HI) {
            throw new SignatureNotFoundException(
                    "No APK Signing Block before ZIP Central Directory");
        }
        long size = footer.getLong(0);
        if (size < footer.capacity() || size > Integer.MAX_VALUE - 8) {
            throw new SignatureNotFoundException("APK Signing Block size out of range: " + size);
        }
        int totalSize = (int) (8 + size);
        long offset = centralDirOffset - totalSize;
        if (offset < 0) {
            throw new SignatureNotFoundException(
                    "APK Signing Block offset out of range: " + offset);
        }
        channel.position(offset);
        ByteBuffer block = ByteBuffer.allocate(totalSize);
        channel.read(block);
        block.order(ByteOrder.LITTLE_ENDIAN);
        long headerSize = block.getLong(0);
        if (headerSize != size) {
            throw new SignatureNotFoundException(
                    "APK Signing Block sizes in header and footer do not match: "
                            + headerSize + " vs " + size);
        }
        return Pair.of(block, offset);
    }

    public static long findCentralDirStartOffset(FileChannel channel) throws IOException {
        return findCentralDirStartOffset(channel, getCommentLength(channel));
    }

    public static long findCentralDirStartOffset(FileChannel channel, long commentLength)
            throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        channel.position(channel.size() - commentLength - 6);
        channel.read(buffer);
        return buffer.getInt(0);
    }

    public static Map<Integer, ByteBuffer> findIdValues(ByteBuffer block)
            throws SignatureNotFoundException {
        checkByteOrderLittleEndian(block);
        ByteBuffer entries = sliceFromTo(block, 8, block.capacity() - 24);
        Map<Integer, ByteBuffer> idValues = new LinkedHashMap<>();
        int entryCount = 0;
        while (entries.hasRemaining()) {
            entryCount++;
            if (entries.remaining() < 8) {
                throw new SignatureNotFoundException(
                        "Insufficient data to read size of APK Signing Block entry #" + entryCount);
            }
            long length = entries.getLong();
            if (length < 4 || length > Integer.MAX_VALUE) {
                throw new SignatureNotFoundException(
                        "APK Signing Block entry #" + entryCount + " size out of range: " + length);
            }
            int len = (int) length;
            int nextEntryPos = entries.position() + len;
            if (len > entries.remaining()) {
                throw new SignatureNotFoundException("APK Signing Block entry #" + entryCount
                        + " size out of range: " + len + ", available: " + entries.remaining());
            }
            idValues.put(entries.getInt(), getByteBuffer(entries, len - 4));
            entries.position(nextEntryPos);
        }
        return idValues;
    }

    private static ByteBuffer getByteBuffer(ByteBuffer buffer, int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size: " + size);
        }
        int limit = buffer.limit();
        int position = buffer.position();
        int newLimit = position + size;
        if (newLimit < position || newLimit > limit) {
            throw new java.nio.BufferUnderflowException();
        }
        buffer.limit(newLimit);
        try {
            ByteBuffer slice = buffer.slice();
            slice.order(buffer.order());
            buffer.position(newLimit);
            return slice;
        } finally {
            buffer.limit(limit);
        }
    }

    public static long getCommentLength(FileChannel channel) throws IOException {
        long fileSize = channel.size();
        if (fileSize < ZIP_EOCD_REC_MIN_SIZE) {
            throw new IOException(
                    "APK too small for ZIP End of Central Directory (EOCD) record");
        }
        long maxCommentLength = Math.min(fileSize - ZIP_EOCD_REC_MIN_SIZE, UINT16_MAX_VALUE);
        long eocdOffset = fileSize - ZIP_EOCD_REC_MIN_SIZE;
        for (int i = 0; i <= maxCommentLength; i++) {
            long current = eocdOffset - i;
            ByteBuffer sig = ByteBuffer.allocate(4);
            channel.position(current);
            channel.read(sig);
            sig.order(ByteOrder.LITTLE_ENDIAN);
            if (sig.getInt(0) == ZIP_EOCD_REC_SIG) {
                ByteBuffer commentLength = ByteBuffer.allocate(2);
                channel.position(current + ZIP_EOCD_COMMENT_LENGTH_FIELD_OFFSET);
                channel.read(commentLength);
                commentLength.order(ByteOrder.LITTLE_ENDIAN);
                short length = commentLength.getShort(0);
                if (length == i) {
                    return length;
                }
            }
        }
        throw new IOException("ZIP End of Central Directory (EOCD) record not found");
    }

    private static ByteBuffer sliceFromTo(ByteBuffer buffer, int start, int end) {
        if (start < 0) {
            throw new IllegalArgumentException("start: " + start);
        }
        if (end < start) {
            throw new IllegalArgumentException("end < start: " + end + " < " + start);
        }
        int capacity = buffer.capacity();
        if (end > capacity) {
            throw new IllegalArgumentException("end > capacity: " + end + " > " + capacity);
        }
        int originalLimit = buffer.limit();
        int originalPosition = buffer.position();
        try {
            buffer.position(0);
            buffer.limit(end);
            buffer.position(start);
            ByteBuffer slice = buffer.slice();
            slice.order(buffer.order());
            return slice;
        } finally {
            buffer.position(0);
            buffer.limit(originalLimit);
            buffer.position(originalPosition);
        }
    }
}