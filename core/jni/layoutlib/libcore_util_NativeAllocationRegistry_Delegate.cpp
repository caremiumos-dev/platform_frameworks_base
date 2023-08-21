/*
 * Copyright (C) 2021 The Android Open Source Project
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

#include <nativehelper/JNIHelp.h>
#include <nativehelper/jni_macros.h>

// This file is a modified version of
// libcore/luni/src/main/native/libcore_util_NativeAllocationRegistry.cpp

typedef void (*FreeFunction)(void*);

static void NativeAllocationRegistry_Delegate_nativeApplyFreeFunction(JNIEnv*, jclass,
                                                                      jlong freeFunction,
                                                                      jlong ptr) {
    void* nativePtr = reinterpret_cast<void*>(static_cast<uintptr_t>(ptr));
    FreeFunction nativeFreeFunction =
            reinterpret_cast<FreeFunction>(static_cast<uintptr_t>(freeFunction));
    nativeFreeFunction(nativePtr);
}

static JNINativeMethod gMethods[] = {
        NATIVE_METHOD(NativeAllocationRegistry_Delegate, nativeApplyFreeFunction, "(JJ)V"),
};

int register_libcore_util_NativeAllocationRegistry_Delegate(JNIEnv* env) {
    return jniRegisterNativeMethods(env, "libcore/util/NativeAllocationRegistry_Delegate", gMethods,
                                    NELEM(gMethods));
}