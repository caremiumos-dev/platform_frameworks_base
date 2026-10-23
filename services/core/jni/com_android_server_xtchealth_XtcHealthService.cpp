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

/*
 * Reverse-engineered from the device libandroid_servers.so.
 *
 * This is the JNI side of the vendor "BBD" (baseband/MCU health) bridge. The
 * device opens a fixed set of character devices and runs a reader thread that
 * forwards framed MCU messages back into Java through
 * XtcHealthService.sendBbdDataNative(int, byte[]):
 *
 *   /dev/MODULE_SPH   (keep-alive / health)
 *   /dev/MODULE_SMT
 *   /dev/MODULE_UTI   crash/self-test transport
 *   /dev/MODULE_FPD
 *
 * sendDataMcu() writes a caller supplied buffer to the UTI or FPD device,
 * choosing by the "package name" argument the framework passes ("MODULE_UTI"
 * vs "MODULE_FPD").
 *
 * The exact MCU frame layout is a vendor private protocol and is not
 * reproduced here; the reader forwards whatever bytes the driver provides and
 * the Java side performs the actual field parsing. This keeps the ABI and the
 * service contract intact on AOSP.
 *
 * Condemnation: this bridge is one end of the vendor's opaque over-the-air
 * "health"/diagnostic channel. It is reproduced only for behavioural
 * compatibility.
 */

#define LOG_TAG "XtcHealthService"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include <utils/misc.h>

#include <errno.h>
#include <fcntl.h>
#include <poll.h>
#include <pthread.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <unistd.h>

#include <utils/Log.h>

namespace android {

#define NELEM_HEALTH(x) (sizeof(x) / sizeof((x)[0]))

#define MODULE_SPH_PATH "/dev/MODULE_SPH"
#define MODULE_SMT_PATH "/dev/MODULE_SMT"
#define MODULE_UTI_PATH "/dev/MODULE_UTI"
#define MODULE_FPD_PATH "/dev/MODULE_FPD"

// Indices into gModuleFd, mirroring the device's global fd table
// (offset 4/8/12/16 -> SPH/SMT/UTI/FPD).
enum {
    FD_SPH = 0,
    FD_SMT = 1,
    FD_UTI = 2,
    FD_FPD = 3,
};

#define MODULE_SPH "MODULE_SPH"
#define MODULE_UTI "MODULE_UTI"
#define MODULE_FPD "MODULE_FPD"

static const char* const kModulePaths[] = {
    MODULE_SPH_PATH, MODULE_SMT_PATH, MODULE_UTI_PATH, MODULE_FPD_PATH,
};

// Type value forwarded to XtcHealthService.sendBbdDataNative(int, byte[]);
// the Java side treats 3 as the UTI module and 4 as the FPD module.
static const jint kModuleType[] = { 1, 2, 3, 4 };

static int gModuleFd[NELEM_HEALTH(kModulePaths)] = { -1, -1, -1, -1 };
static JavaVM* gVm = NULL;
static jobject gHost = NULL;
static jmethodID gSendBbdDataNative = NULL;
static pthread_t gReaderThread;
static volatile bool gReaderRunning = false;

static jboolean openModuleDevices() {
    bool ok = false;
    for (size_t i = 0; i < NELEM_HEALTH(kModulePaths); i++) {
        int fd = open(kModulePaths[i], O_RDWR);
        gModuleFd[i] = fd;
        if (fd >= 0) {
            ok = true;
        } else {
            ALOGW("open device %s failed, error=%d", kModulePaths[i], errno);
        }
    }
    return ok ? JNI_TRUE : JNI_FALSE;
}

static void closeModuleDevices() {
    for (size_t i = 0; i < NELEM_HEALTH(kModulePaths); i++) {
        if (gModuleFd[i] >= 0) {
            close(gModuleFd[i]);
            gModuleFd[i] = -1;
        }
    }
}

// Exact reproduction of the device reader: poll the four module fds (40 ms
// timeout), read up to 64 KiB from each ready fd and forward the raw bytes to
// XtcHealthService.sendBbdDataNative(type, data). The device passes type
// 1/2/3/4 for SPH/SMT/UTI/FPD; frame decoding happens in Java.
#define BBD_READ_MAX 0x10000

static void* xtcbbdReadMsgThread(void*) {
    JNIEnv* env = NULL;
    if (gVm == NULL || gVm->AttachCurrentThread(&env, NULL) != JNI_OK || env == NULL) {
        ALOGE("xtcbbd reader: AttachCurrentThread failed");
        return NULL;
    }

    uint8_t* buffer = (uint8_t*) malloc(BBD_READ_MAX);
    if (buffer == NULL) {
        ALOGE("xtcbbd reader: malloc %d fail", BBD_READ_MAX);
        gVm->DetachCurrentThread();
        return NULL;
    }

    const size_t count = NELEM_HEALTH(kModulePaths);
    struct pollfd fds[count];
    while (gReaderRunning) {
        for (size_t i = 0; i < count; i++) {
            fds[i].fd = gModuleFd[i];
            fds[i].events = POLLIN;
            fds[i].revents = 0;
        }
        int ret = poll(fds, count, 40);
        if (ret <= 0) {
            continue;
        }
        for (size_t i = 0; i < count; i++) {
            if ((fds[i].revents & POLLIN) == 0 || gModuleFd[i] < 0) {
                continue;
            }
            ssize_t n = read(gModuleFd[i], buffer, BBD_READ_MAX);
            if (n <= 0 || gSendBbdDataNative == NULL) {
                continue;
            }
            jbyteArray data = env->NewByteArray(n);
            if (data == NULL) {
                continue;
            }
            env->SetByteArrayRegion(data, 0, n, reinterpret_cast<const jbyte*>(buffer));
            env->CallVoidMethod(gHost, gSendBbdDataNative, kModuleType[i], data);
            env->DeleteLocalRef(data);
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
            }
        }
    }

    free(buffer);
    gVm->DetachCurrentThread();
    return NULL;
}

static jboolean init_bbd(JNIEnv* env, jclass, jobject host) {
    ALOGI("initializing..");
    env->GetJavaVM(&gVm);

    // The callback is declared on the instance; resolve it on the host
    // object's class so subclasses/overrides are honoured like the device.
    jclass hostClass = env->GetObjectClass(host);
    gSendBbdDataNative = env->GetMethodID(hostClass, "sendBbdDataNative", "(I[B)V");
    env->DeleteLocalRef(hostClass);
    if (gSendBbdDataNative == NULL) {
        ALOGE("sendBbdDataNative() not found");
        return JNI_FALSE;
    }
    if (gHost != NULL) {
        env->DeleteGlobalRef(gHost);
    }
    gHost = env->NewGlobalRef(host);

    if (!openModuleDevices()) {
        return JNI_FALSE;
    }

    gReaderRunning = true;
    if (pthread_create(&gReaderThread, NULL, xtcbbdReadMsgThread, NULL) != 0) {
        gReaderRunning = false;
        closeModuleDevices();
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static jint sendDataMcu(JNIEnv* env, jclass, jstring packageName, jint length,
        jbyteArray data) {
    if (packageName == NULL || data == NULL) {
        jniThrowNullPointerException(env, "sendDataMcu: null argument");
        return -1;
    }

    const char* pkg = env->GetStringUTFChars(packageName, NULL);
    jbyte* bytes = env->GetByteArrayElements(data, NULL);
    ALOGD("%s send data to mcu length=%d, dataBuf=%p", pkg, (int) length, bytes);

    // The device selects the device by exact package name and rejects
    // anything else.
    int index = -1;
    if (pkg != NULL) {
        if (strcmp(pkg, MODULE_SPH) == 0) {
            index = FD_SPH;
        } else if (strcmp(pkg, MODULE_UTI) == 0) {
            index = FD_UTI;
        } else if (strcmp(pkg, MODULE_FPD) == 0) {
            index = FD_FPD;
        }
    }

    jint result = -1;
    if (index < 0 || gModuleFd[index] < 0) {
        ALOGE("MODULE_FPD open file fail, error=%d", errno);
    } else if (bytes != NULL) {
        ssize_t written = write(gModuleFd[index], bytes, (size_t) length);
        if (written == length) {
            result = 1;
        } else {
            ALOGE("error writing to the file");
        }
    }

    if (bytes != NULL) {
        env->ReleaseByteArrayElements(data, bytes, 0);
    }
    if (pkg != NULL) {
        env->ReleaseStringUTFChars(packageName, pkg);
    }
    return result;
}

static const JNINativeMethod gXtcHealthMethods[] = {
    { "init_bbd", "(Lcom/android/server/xtchealth/XtcHealthService;)Z", (void*) init_bbd },
    { "sendDataMcu", "(Ljava/lang/String;I[B)I", (void*) sendDataMcu },
};

int register_android_server_XtcHealthService(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/android/server/xtchealth/XtcHealthService",
            gXtcHealthMethods, NELEM_HEALTH(gXtcHealthMethods));
}

};  // namespace android
