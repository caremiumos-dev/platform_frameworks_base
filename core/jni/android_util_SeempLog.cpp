/*
 * Copyright (C) 2018 The Android Open Source Project
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

/*
 * Reverse-engineered from the device libandroid_runtime.so.
 *
 * The vendor SEEMP privacy-event sink writes "<api> <message>" to the abstract
 * unix socket /dev/socket/seempdw. The device exposes the C entry point
 * __android_seemp_socket_write(int, const char*) from libandroid_runtime and
 * wires SeempLog.seemp_println_native() to it. The exact wire framing of the
 * device (a hookable function pointer receiving a {msg, api} struct) is not
 * reproduced because its consumer lives in a vendor-private daemon; the
 * observable behaviour - best-effort delivery of "api message" to the socket -
 * is preserved.
 *
 * Condemnation: this path is the plumbing behind the vendor's SEEMP telemetry,
 * which records privacy-sensitive API usage (location, contacts, sensors,
 * ...) and ships it off-device. It is reproduced only so the rewritten
 * framework keeps its ABI and does not crash.
 */

#define LOG_TAG "SeempLog"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include <utils/misc.h>

#include "core_jni_helpers.h"

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <unistd.h>

namespace android {

#define SEEMP_SOCKET_PATH "/dev/socket/seempdw"

extern "C" int __android_seemp_socket_write(int api, const char* msg) {
    if (msg == NULL) {
        msg = "";
    }
    // The device opens an AF_UNIX datagram socket (with SOCK_CLOEXEC) to
    // /dev/socket/seempdw and connects it, then writes a message whose first
    // four bytes are the little-endian api id followed by the UTF text.
    int fd = socket(AF_UNIX, SOCK_DGRAM | SOCK_CLOEXEC, 0);
    if (fd < 0) {
        fd = socket(AF_UNIX, SOCK_DGRAM, 0);
        if (fd < 0) {
            return -1;
        }
    }

    int ret = -1;
    struct sockaddr_un addr;
    memset(&addr, 0, sizeof(addr));
    addr.sun_family = AF_UNIX;
    strncpy(addr.sun_path, SEEMP_SOCKET_PATH, sizeof(addr.sun_path) - 1);
    if (connect(fd, (struct sockaddr*) &addr, sizeof(addr)) == 0) {
        size_t msgLen = strlen(msg);
        size_t total = sizeof(int32_t) + msgLen;
        char* buf = (char*) malloc(total);
        if (buf != NULL) {
            memcpy(buf, &api, sizeof(int32_t));
            memcpy(buf + sizeof(int32_t), msg, msgLen);
            ssize_t n = write(fd, buf, total);
            ret = (int) n;
            free(buf);
        }
    }
    close(fd);
    return ret;
}

static jint android_util_SeempLog_println_native(JNIEnv* env, jobject, jint api,
        jstring msgObj) {
    if (msgObj == NULL) {
        jniThrowNullPointerException(env, "seemp_println needs a message");
        return -1;
    }
    const char* msg = env->GetStringUTFChars(msgObj, NULL);
    if (msg == NULL) {
        return -1;
    }
    jint ret = (jint) __android_seemp_socket_write(api, msg);
    env->ReleaseStringUTFChars(msgObj, msg);
    return ret;
}

static const JNINativeMethod gSeempLogMethods[] = {
    { "seemp_println_native", "(ILjava/lang/String;)I",
            (void*) android_util_SeempLog_println_native },
};

int register_android_util_SeempLog(JNIEnv* env) {
    return RegisterMethodsOrDie(env, "android/util/SeempLog",
            gSeempLogMethods, NELEM(gSeempLogMethods));
}

};  // namespace android
