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
 * JNI side of XTCLogService's XTCUtilsService:
 *   - checkVersionDowngradeNative(String)
 *   - getSecurityKeyFromXtcInfo(int, String)
 *   - setSecurityKeyToXtcInfo(String)
 *
 * The device has two backends:
 *
 *  1. A Qualcomm TrustZone app reached through QSEECom, entirely recovered
 *     from the binary:
 *        - app path/name : "/firmware/image" + "smplap32"
 *        - request size  : 0x400
 *        - command ids   : read type 0->0x42, 1->0x43, 2->0x40, 3->0x47
 *        - request struct (528 bytes): { int32 cmd @0; uint8 flags @4;
 *          char data[515] @5; int32 field @520; int32 len @524; }
 *        - response buffer: 576 bytes, length field at offset 0x204
 *        - send via QSEECom_send_cmd(handle, req, reqLen, req, 576) using the
 *          handle's shared ionBuf; helper = qsc_issue_send_cmd_req_rsp.
 *     libQSEEComAPI.so is a NEEDED entry of the device libandroid_servers but
 *     is absent from the system image, so it is resolved with dlopen() at
 *     runtime; when it (or the TA) is unavailable we fall through exactly like
 *     the device does when the TEE call fails.
 *
 *  2. A flash fallback in the "xtcinfo" partition at offset 0x800 holding
 *     three 520-byte records { int32 length; int32 crc; char data[512]; },
 *     validated with android::calculateCRC() = CRC-16/CCITT-FALSE
 *     (poly 0x1021, init 0xFFFF, truncated to 16 bits). This path is
 *     reproduced byte-exactly.
 *
 * Condemnation: the anti-rollback check and the "security key" store are part
 * of the vendor's device-control scheme. Reproduced for compatibility only.
 */

#define LOG_TAG "XTCLogService"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include <utils/misc.h>

#include <cutils/properties.h>

#include <dlfcn.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <string.h>
#include <unistd.h>

#include <utils/Log.h>

namespace android {

// ---------------------------------------------------------------------------
// QSEECom client (runtime resolved; see file header)
// ---------------------------------------------------------------------------

#define QSC_APP_PATH "/firmware/image"
#define QSC_APP_NAME "smplap32"
#define QSC_APP_SIZE 0x400
#define QSC_CMD_SIZE 528
#define QSC_RSP_SIZE 576
#define QSC_DATA_OFF 5
#define QSC_FIELD_OFF 520
#define QSC_LEN_OFF 524
#define QSC_RSP_LEN_OFF 0x204

struct QSEECom_handle {
    unsigned char* ionBuf;
    int ionFd;
    uint32_t sbuf_len;
};

typedef int (*qsee_start_fn)(QSEECom_handle**, const char*, const char*, uint32_t);
typedef int (*qsee_shutdown_fn)(QSEECom_handle**);
typedef int (*qsee_send_cmd_fn)(QSEECom_handle*, void*, uint32_t, void*, uint32_t);
typedef int (*qsee_get_app_info_fn)(QSEECom_handle*, void*);

static qsee_start_fn gQseeStart;
static qsee_shutdown_fn gQseeShutdown;
static qsee_send_cmd_fn gQseeSendCmd;
static qsee_get_app_info_fn gQseeGetAppInfo;
static bool gQseeLoaded = false;

static void loadQsee() {
    if (gQseeLoaded) {
        return;
    }
    gQseeLoaded = true;
    void* lib = dlopen("libQSEEComAPI.so", RTLD_NOW);
    if (lib == NULL) {
        ALOGW("libQSEEComAPI.so not available: %s", dlerror());
        return;
    }
    gQseeStart = (qsee_start_fn) dlsym(lib, "QSEECom_start_app");
    gQseeShutdown = (qsee_shutdown_fn) dlsym(lib, "QSEECom_shutdown_app");
    gQseeSendCmd = (qsee_send_cmd_fn) dlsym(lib, "QSEECom_send_cmd");
    gQseeGetAppInfo = (qsee_get_app_info_fn) dlsym(lib, "QSEECom_get_app_info");
}

// Exact reproduction of android::qsc_issue_send_cmd_req_rsp.
static int qsc_issue_send_cmd_req_rsp(QSEECom_handle* handle, void* cmd, void* rsp) {
    if (handle == NULL || handle->ionBuf == NULL || gQseeSendCmd == NULL) {
        ALOGE("qsc_issue_send_cmd_req_rsp: invalid handle");
        return -1;
    }
    unsigned char* req = handle->ionBuf;
    int32_t zero = 0;
    int reqLen;

    char appInfo = 0;
    if (gQseeGetAppInfo != NULL) {
        gQseeGetAppInfo(handle, &appInfo);
    }
    if (appInfo != 0) {
        memcpy(req + 0, (char*) cmd + 0, 4);
        memcpy(req + 8, (char*) cmd + 4, 1);
        memcpy(req + 12, &zero, 4);
        memcpy(req + 16, (char*) cmd + QSC_FIELD_OFF, 4);
        memcpy(req + 20, &zero, 4);
        memcpy(req + 24, (char*) cmd + QSC_LEN_OFF, 4);
        reqLen = 64;
    } else {
        int32_t len;
        memcpy(req + 0, (char*) cmd + 0, 4);
        memcpy(req + 4, (char*) cmd + 4, 1);
        memcpy(req + QSC_FIELD_OFF, (char*) cmd + QSC_FIELD_OFF, 4);
        memcpy(req + QSC_LEN_OFF, (char*) cmd + QSC_LEN_OFF, 4);
        memcpy(&len, (char*) cmd + QSC_LEN_OFF, 4);
        if (len > 0 && len <= (QSC_LEN_OFF - QSC_DATA_OFF)) {
            memcpy(req + QSC_DATA_OFF, (char*) cmd + QSC_DATA_OFF, len);
        }
        reqLen = QSC_RSP_SIZE;
    }

    int ret = gQseeSendCmd(handle, req, reqLen, req, QSC_RSP_SIZE);
    ALOGD("req len = %d bytes, rsp len = %d bytes, ret = %d", reqLen, QSC_RSP_SIZE, ret);
    if (rsp != NULL) {
        memcpy(rsp, req, QSC_RSP_SIZE);
    }
    return ret;
}

// Exact reproduction of android::qsc_run_read_keydata. Returns the number of
// key bytes written to out on success, -1 on hard failure, or -2 when the
// TrustZone backend is unavailable (the device's "try read from old" signal).
static int qsc_run_read_keydata(unsigned char* out, int type, char* name) {
    char cmd[QSC_CMD_SIZE];
    memset(cmd, 0, sizeof(cmd));

    int nameLen = 0;
    int cmdId;
    switch (type) {
        case 0: cmdId = 0x42; break;
        case 1: cmdId = 0x43; nameLen = (int) strlen(name); break;
        case 2: cmdId = 0x40; nameLen = (int) strlen(name); break;
        case 3: cmdId = 0x47; nameLen = (int) strlen(name); break;
        default:
            ALOGE("qsc_run_read_keydata not support type = %d", type);
            return -1;
    }
    ALOGD("Starting qsc_run_read_keydata type = %d", type);

    memcpy(cmd + 0, &cmdId, 4);
    if (nameLen > 0 && name != NULL) {
        memcpy(cmd + QSC_DATA_OFF, name, nameLen);
    }
    memcpy(cmd + QSC_LEN_OFF, &nameLen, 4);

    loadQsee();
    if (gQseeStart == NULL) {
        return -2;
    }

    QSEECom_handle* handle = NULL;
    if (gQseeStart(&handle, QSC_APP_PATH, QSC_APP_NAME, QSC_APP_SIZE) != 0
            || handle == NULL) {
        ALOGE("QSEECom_start_app fail");
        return -1;
    }

    char rsp[QSC_RSP_SIZE];
    memset(rsp, 0, sizeof(rsp));
    if (qsc_issue_send_cmd_req_rsp(handle, cmd, rsp) != 0) {
        ALOGE("qsc_issue_send_cmd_req_rsp fail");
        gQseeShutdown(&handle);
        return -1;
    }

    int outLen = 0;
    memcpy(&outLen, rsp + QSC_RSP_LEN_OFF, 4);
    if (out != NULL && outLen > 0 && outLen <= QSC_RSP_SIZE) {
        memcpy(out, rsp, outLen);
    } else {
        ALOGE("qsc_run_read_keydata read fail");
    }

    gQseeShutdown(&handle);
    return outLen;
}

// ---------------------------------------------------------------------------
// xtcinfo flash fallback (byte-exact)
// ---------------------------------------------------------------------------

#define XTCINFO_PATH "/dev/block/bootdevice/by-name/xtcinfo"
#define XTCINFO_TABLE_OFFSET 0x800
#define XTCINFO_RECORD_SIZE 520
#define XTCINFO_RECORD_COUNT 3
#define XTCINFO_DATA_MAX 512

struct XtcInfoRecord {
    int32_t length;
    int32_t crc;
    char data[XTCINFO_DATA_MAX];
};

static int32_t calculateCRC(const char* data, int length) {
    if (length < 1) {
        return 0xFFFF;
    }
    uint16_t crc = 0xFFFF;
    for (int i = 0; i < length; i++) {
        crc ^= (uint16_t) (((uint8_t) data[i]) << 8);
        for (int bit = 0; bit < 8; bit++) {
            if (crc & 0x8000) {
                crc = (uint16_t) ((crc << 1) ^ 0x1021);
            } else {
                crc = (uint16_t) (crc << 1);
            }
        }
    }
    return (int32_t) (crc & 0xFFFF);
}

static int readRecord(char* out, int outSize) {
    int fd = open(XTCINFO_PATH, O_RDONLY);
    if (fd < 0) {
        ALOGE("open %s failed, error=%d", XTCINFO_PATH, errno);
        return -1;
    }

    int result = -1;
    if (lseek(fd, XTCINFO_TABLE_OFFSET, SEEK_SET) == (off_t) XTCINFO_TABLE_OFFSET) {
        XtcInfoRecord record;
        for (int i = 0; i < XTCINFO_RECORD_COUNT; i++) {
            ssize_t n = read(fd, &record, sizeof(record));
            if (n != (ssize_t) sizeof(record)) {
                break;
            }
            if (record.length <= 0 || record.length > XTCINFO_DATA_MAX) {
                continue;
            }
            int32_t calc = calculateCRC(record.data, record.length);
            ALOGD("getSecurityKeyFromXtcInfo_partition saveCrc = %d, calcCrc = %d",
                    record.crc, calc);
            if (calc == record.crc) {
                int copy = record.length < outSize ? record.length : outSize;
                memcpy(out, record.data, copy);
                out[copy] = '\0';
                result = copy;
                break;
            }
        }
    }
    close(fd);
    return result;
}

static bool writeRecord(const char* key, int length) {
    if (key == NULL || length <= 0 || length > XTCINFO_DATA_MAX) {
        return false;
    }
    int fd = open(XTCINFO_PATH, O_RDWR);
    if (fd < 0) {
        ALOGE("open %s failed, error=%d", XTCINFO_PATH, errno);
        return false;
    }
    bool ok = false;
    if (lseek(fd, XTCINFO_TABLE_OFFSET, SEEK_SET) == (off_t) XTCINFO_TABLE_OFFSET) {
        XtcInfoRecord record;
        memset(&record, 0, sizeof(record));
        record.length = length;
        record.crc = calculateCRC(key, length);
        memcpy(record.data, key, length);
        ok = (write(fd, &record, sizeof(record)) == (ssize_t) sizeof(record));
    }
    close(fd);
    return ok;
}

// ---------------------------------------------------------------------------
// JNI entry points
// ---------------------------------------------------------------------------

static jint checkVersionDowngradeNative(JNIEnv* env, jclass, jstring versionObj) {
    if (versionObj == NULL) {
        return 0;
    }
    const char* version = env->GetStringUTFChars(versionObj, NULL);
    ALOGD("checkVersionDowngradeNative version = %s", version != NULL ? version : "(null)");

    jint result = 0;
    if (version != NULL) {
        char installed[PROPERTY_VALUE_MAX] = {};
        property_get("ro.build.version.incremental", installed, "0");
        int cmp = strcmp(version, installed);
        if (cmp > 0) {
            result = 1;
        } else if (cmp < 0) {
            result = -1;
        }
    }
    if (version != NULL) {
        env->ReleaseStringUTFChars(versionObj, version);
    }
    return result;
}

static jstring getSecurityKeyFromXtcInfo(JNIEnv* env, jclass, jint type, jstring nameObj) {
    const char* name = NULL;
    if (nameObj != NULL) {
        name = env->GetStringUTFChars(nameObj, NULL);
    }
    ALOGD("getSecurityKeyFromXtcInfo: %d", (int) type);

    char buffer[XTCINFO_DATA_MAX + 1];
    memset(buffer, 0, sizeof(buffer));

    int len = -1;
    if (name != NULL) {
        len = qsc_run_read_keydata((unsigned char*) buffer, (int) type, (char*) name);
    }
    if (len == -2) {
        // Device fallback: the TEE path reported "read from old"; read the
        // flash partition instead.
        ALOGD("retry getSecurityKeyFromXtcInfo");
        len = readRecord(buffer, XTCINFO_DATA_MAX);
    }

    if (name != NULL) {
        env->ReleaseStringUTFChars(nameObj, name);
    }

    if (len < 0) {
        return NULL;
    }
    return env->NewStringUTF(buffer);
}

static jboolean setSecurityKeyToXtcInfo(JNIEnv* env, jclass, jstring keyObj) {
    if (keyObj == NULL) {
        return JNI_FALSE;
    }
    const char* key = env->GetStringUTFChars(keyObj, NULL);
    if (key == NULL) {
        return JNI_FALSE;
    }
    ALOGD("setSecurityKeyToXtcInfo: %s", key);

    jboolean ok = writeRecord(key, (int) strlen(key)) ? JNI_TRUE : JNI_FALSE;
    env->ReleaseStringUTFChars(keyObj, key);
    return ok;
}

static const JNINativeMethod gXTCLogMethods[] = {
    { "checkVersionDowngradeNative", "(Ljava/lang/String;)I", (void*) checkVersionDowngradeNative },
    { "getSecurityKeyFromXtcInfo", "(ILjava/lang/String;)Ljava/lang/String;",
            (void*) getSecurityKeyFromXtcInfo },
    { "setSecurityKeyToXtcInfo", "(Ljava/lang/String;)Z", (void*) setSecurityKeyToXtcInfo },
};

int register_android_server_XTCLogService(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "com/android/server/xtclog/XTCLogService",
            gXTCLogMethods, NELEM(gXTCLogMethods));
}

};  // namespace android
