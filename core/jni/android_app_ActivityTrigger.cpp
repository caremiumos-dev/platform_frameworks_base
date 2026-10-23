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
 * The vendor "ActivityTrigger" shim is a thin bridge between the framework
 * activity lifecycle and a vendor performance library. The library path is
 * configured through the system property ro.vendor.at_library; when present
 * (and not an absolute path) it is dlopen'ed and a fixed set of symbols is
 * resolved:
 *
 *   activity_trigger_start(const char* name, int* flags)
 *   activity_trigger_resume(const char* name)
 *   activity_trigger_pause(const char* name)
 *   activity_trigger_stop(const char* name)
 *   activity_trigger_init(void)
 *   activity_trigger_misc(int, const char* name, int, int, float* out)
 *   activity_trigger_deinit(void)          (resolved lazily on finalize)
 *
 * When the property is unset, the library fails to load, or any symbol is
 * missing, all entry points become no-ops. This keeps the AOSP build usable
 * on devices without the vendor library while preserving the exact ABI the
 * rewritten Java class expects.
 */

#define LOG_TAG "ActivityTrigger"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include <utils/misc.h>

#include "core_jni_helpers.h"

#include <cutils/properties.h>
#include <dlfcn.h>
#include <string.h>
#include <utils/Log.h>

namespace android {

typedef void (*at_start_fn)(const char*, int*);
typedef void (*at_name_fn)(const char*);
typedef void (*at_void_fn)(void);
typedef void (*at_misc_fn)(int, const char*, int, int, float*);

struct ActivityTriggerState {
    void* handle;
    at_start_fn start;
    at_name_fn resume;
    at_name_fn pause;
    at_name_fn stop;
    at_void_fn init;
    at_void_fn deinit;
    at_misc_fn misc;
};

static ActivityTriggerState gAt = {};

static void clearState() {
    memset(&gAt, 0, sizeof(gAt));
}

static jint native_at_startActivity(JNIEnv* env, jobject, jstring nameObj, jint flags) {
    if (nameObj == NULL || gAt.start == NULL) {
        return flags;
    }
    const char* name = env->GetStringUTFChars(nameObj, NULL);
    if (name != NULL) {
        gAt.start(name, &flags);
        env->ReleaseStringUTFChars(nameObj, name);
    }
    return flags;
}

static void native_at_resumeActivity(JNIEnv* env, jobject, jstring nameObj) {
    if (nameObj == NULL || gAt.resume == NULL) {
        return;
    }
    const char* name = env->GetStringUTFChars(nameObj, NULL);
    if (name != NULL) {
        gAt.resume(name);
        env->ReleaseStringUTFChars(nameObj, name);
    }
}

static void native_at_pauseActivity(JNIEnv* env, jobject, jstring nameObj) {
    if (nameObj == NULL || gAt.pause == NULL) {
        return;
    }
    const char* name = env->GetStringUTFChars(nameObj, NULL);
    if (name != NULL) {
        gAt.pause(name);
        env->ReleaseStringUTFChars(nameObj, name);
    }
}

static void native_at_stopActivity(JNIEnv* env, jobject, jstring nameObj) {
    if (nameObj == NULL || gAt.stop == NULL) {
        return;
    }
    const char* name = env->GetStringUTFChars(nameObj, NULL);
    if (name != NULL) {
        gAt.stop(name);
        env->ReleaseStringUTFChars(nameObj, name);
    }
}

static jfloat native_at_miscActivity(JNIEnv* env, jobject, jint a, jstring nameObj,
        jint b, jint c) {
    jfloat out = -1.0f;
    if (nameObj == NULL || gAt.misc == NULL) {
        return out;
    }
    const char* name = env->GetStringUTFChars(nameObj, NULL);
    if (name != NULL) {
        gAt.misc(a, name, b, c, &out);
        env->ReleaseStringUTFChars(nameObj, name);
    }
    return out;
}

static void native_at_deinit(JNIEnv* env, jobject) {
    void* handle = gAt.handle;
    if (handle == NULL) {
        return;
    }
    at_void_fn deinit = (at_void_fn) dlsym(handle, "activity_trigger_deinit");
    clearState();
    if (deinit != NULL) {
        deinit();
    }
    dlclose(handle);
}

static const JNINativeMethod gActivityTriggerMethods[] = {
    { "native_at_startActivity",  "(Ljava/lang/String;I)I",  (void*) native_at_startActivity },
    { "native_at_resumeActivity", "(Ljava/lang/String;)V",   (void*) native_at_resumeActivity },
    { "native_at_pauseActivity",  "(Ljava/lang/String;)V",   (void*) native_at_pauseActivity },
    { "native_at_stopActivity",   "(Ljava/lang/String;)V",   (void*) native_at_stopActivity },
    { "native_at_miscActivity",   "(ILjava/lang/String;II)F",(void*) native_at_miscActivity },
    { "native_at_deinit",         "()V",                     (void*) native_at_deinit },
};

int register_com_android_internal_app_ActivityTrigger(JNIEnv* env) {
    char value[PROPERTY_VALUE_MAX] = {};
    int len = property_get("ro.vendor.at_library", value, "");
    if (len >= 1 && strchr(value, '/') == NULL) {
        void* handle = dlopen(value, RTLD_NOW);
        if (handle != NULL) {
            void* start = dlsym(handle, "activity_trigger_start");
            void* resume = dlsym(handle, "activity_trigger_resume");
            void* pause = dlsym(handle, "activity_trigger_pause");
            void* stop = dlsym(handle, "activity_trigger_stop");
            void* init = dlsym(handle, "activity_trigger_init");
            void* misc = dlsym(handle, "activity_trigger_misc");
            if (start != NULL && resume != NULL && pause != NULL && stop != NULL
                    && init != NULL && misc != NULL) {
                gAt.handle = handle;
                gAt.start = (at_start_fn) start;
                gAt.resume = (at_name_fn) resume;
                gAt.pause = (at_name_fn) pause;
                gAt.stop = (at_name_fn) stop;
                gAt.init = (at_void_fn) init;
                gAt.misc = (at_misc_fn) misc;
                gAt.init();
            } else {
                clearState();
                dlclose(handle);
            }
        }
    }
    return RegisterMethodsOrDie(env, "com/android/internal/app/ActivityTrigger",
            gActivityTriggerMethods, NELEM(gActivityTriggerMethods));
}

};  // namespace android
