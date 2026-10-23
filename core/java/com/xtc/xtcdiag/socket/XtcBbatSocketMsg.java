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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Builds the binary diag command packages sent to the BBAT/diag baseband tool.
 *
 * @hide
 */
public class XtcBbatSocketMsg {
    public ByteBuffer buildDiagCmdPkg(byte pkgTag, int seqNum, byte type, byte subType,
            String cmd) {
        if (TextUtils.isEmpty(cmd)) {
            return null;
        }
        int length = cmd.getBytes().length + 10;
        ByteBuffer buffer = ByteBuffer.allocate(length);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(pkgTag);
        buffer.putInt(seqNum);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) (length - 2));
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put(type);
        buffer.put(subType);
        buffer.put(cmd.getBytes());
        buffer.put(pkgTag);
        return buffer;
    }
}