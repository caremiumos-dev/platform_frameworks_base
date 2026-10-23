/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.server;

import android.os.StrictMode;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;

import dalvik.system.SocketTagger;

import java.io.FileDescriptor;
import java.net.SocketException;

/**
 * Assigns tags to sockets for traffic stats.
 */
public final class NetworkManagementSocketTagger extends SocketTagger {
    private static final String TAG = "NetworkManagementSocketTagger";
    private static final boolean LOGD = false;

    /**
     * {@link SystemProperties} key that indicates if {@code qtaguid} bandwidth
     * controls have been enabled.
     */
    // TODO: remove when always enabled, or once socket tagging silently fails.
    public static final String PROP_QTAGUID_ENABLED = "net.qtaguid_enabled";

    // XTC bbk_traffic: process-wide socket counter tag applied to newly created fds.
    private static int sc_tag = 0;

    private static ThreadLocal<SocketTags> threadSocketTags = new ThreadLocal<SocketTags>() {
        @Override
        protected SocketTags initialValue() {
            return new SocketTags();
        }
    };

    public static void install() {
        SocketTagger.set(new NetworkManagementSocketTagger());
    }

    public static int setThreadSocketStatsTag(int tag) {
        final int old = threadSocketTags.get().statsTag;
        threadSocketTags.get().statsTag = tag;
        return old;
    }

    public static int getThreadSocketStatsTag() {
        return threadSocketTags.get().statsTag;
    }

    public static int setThreadSocketStatsUid(int uid) {
        final int old = threadSocketTags.get().statsUid;
        threadSocketTags.get().statsUid = uid;
        return old;
    }

    /**
     * XTC bbk_traffic: set the process-wide socket counter tag. When non-zero it
     * overrides the per-thread stats tag when tagging new socket fds.
     */
    public static void setSCTag(int tag) {
        Log.d(TAG, "bbk_traffic setSCTag for current app process, the socket tag is = " + tag);
        sc_tag = tag;
    }

    /**
     * XTC bbk_traffic: tag an already-open udp socket fd with {@link #sc_tag}.
     */
    public static void setUDPSocketTag(int fd) {
        Log.d(TAG, "bbk_traffic setUDPSocketTag for current app process, the socket FD is = " + fd);
        if (fd == 0 || sc_tag == 0) {
            return;
        }
        final int errno = native_tagSocketFdInt(fd, sc_tag, 1000);
        Log.d(TAG, "bbk_traffic setUDPSocketTag  fd = " + fd + ", tag = " + sc_tag
                + ", errno = " + errno);
        if (errno < 0) {
            Log.i(TAG, "setUDPSocketTag fd = " + fd + ", failed with errno" + errno);
        }
    }

    @Override
    public void tag(FileDescriptor fd) throws SocketException {
        final SocketTags options = threadSocketTags.get();
        if (LOGD) {
            Log.d(TAG, "tagSocket(" + fd.getInt$() + ") with statsTag=0x"
                    + Integer.toHexString(options.statsTag) + ", statsUid=" + options.statsUid);
        }
        if (options.statsTag == -1 && StrictMode.vmUntaggedSocketEnabled()) {
            StrictMode.onUntaggedSocket();
        }
        // TODO: skip tagging when options would be no-op
        tagSocketFd(fd, options.statsTag, options.statsUid);
    }

    private void tagSocketFd(FileDescriptor fd, int tag, int uid) {
        Log.d(TAG, "bbk_traffic tagSocketFd  fd.tag = " + tag + ",uid = " + uid
                + ",sc_tag = " + sc_tag);
        if ((tag == -1 && uid == -1 && sc_tag == 0)
                || !SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false)) {
            return;
        }

        if (sc_tag != 0) {
            tag = sc_tag;
        }
        final int errno = native_tagSocketFd(fd, tag, uid);
        Log.d(TAG, "bbk_traffic tagSocketFd  fd.getInt = " + fd.getInt$() + "  uid = " + uid
                + ", tag = " + tag + ", sc_tag = " + sc_tag);
        if (errno < 0) {
            Log.i(TAG, "tagSocketFd(" + fd.getInt$() + ", "
                  + tag + ", " +
                  + uid + ") failed with errno" + errno);
        }
    }

    @Override
    public void untag(FileDescriptor fd) throws SocketException {
        if (LOGD) {
            Log.i(TAG, "untagSocket(" + fd.getInt$() + ")");
        }
        unTagSocketFd(fd);
    }

    private void unTagSocketFd(FileDescriptor fd) {
        final SocketTags options = threadSocketTags.get();
        if (options.statsTag == -1 && options.statsUid == -1) return;

        if (SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false)) {
            final int errno = native_untagSocketFd(fd);
            Log.d(TAG, "bbk_traffic unTagSocketFd  fd.getInt = " + fd.getInt$()
                    + "  uid = " + options.statsUid + ", tag = " + options.statsTag);
            if (errno < 0) {
                Log.w(TAG, "untagSocket(" + fd.getInt$() + ") failed with errno " + errno);
            }
        }
    }

    /**
     * XTC bbk_traffic: tag a specific socket fd with an explicit tag/uid.
     */
    public void setSocketFdTag(FileDescriptor fd, int tag, int uid) throws SocketException {
        tagSocketFd(fd, tag, uid);
    }

    public static class SocketTags {
        public int statsTag = -1;
        public int statsUid = -1;
    }

    public static void setKernelCounterSet(int uid, int counterSet) {
        if (SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false)) {
            final int errno = native_setCounterSet(counterSet, uid);
            if (errno < 0) {
                Log.w(TAG, "setKernelCountSet(" + uid + ", " + counterSet + ") failed with errno "
                        + errno);
            }
        }
    }

    public static void resetKernelUidStats(int uid) {
        if (SystemProperties.getBoolean(PROP_QTAGUID_ENABLED, false)) {
            int errno = native_deleteTagData(0, uid);
            if (errno < 0) {
                Slog.w(TAG, "problem clearing counters for uid " + uid + " : errno " + errno);
            }
        }
    }

    /**
     * Convert {@code /proc/} tag format to {@link Integer}. Assumes incoming
     * format like {@code 0x7fffffff00000000}.
     */
    public static int kernelToTag(String string) {
        int length = string.length();
        if (length > 10) {
            return Long.decode(string.substring(0, length - 8)).intValue();
        } else {
            return 0;
        }
    }

    private static native int native_tagSocketFd(FileDescriptor fd, int tag, int uid);
    private static native int native_tagSocketFdInt(int fd, int tag, int uid);
    private static native int native_untagSocketFd(FileDescriptor fd);
    private static native int native_setCounterSet(int uid, int counterSetNum);
    private static native int native_deleteTagData(int tag, int uid);
}
