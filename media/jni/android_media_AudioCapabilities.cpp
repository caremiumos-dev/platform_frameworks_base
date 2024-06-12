/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//#define LOG_NDEBUG 0
#define LOG_TAG "MediaCodec-JNI"

#include "android_runtime/AndroidRuntime.h"
#include "jni.h"

#include <media/AudioCapabilities.h>
#include <media/stagefright/foundation/ADebug.h>
#include <nativehelper/JNIHelp.h>

using namespace android;

struct fields_t {
    jfieldID context;
};
static fields_t fields;

static std::shared_ptr<AudioCapabilities> getAudioCapabilities(JNIEnv *env, jobject thiz) {
    AudioCapabilities* const p = (AudioCapabilities*)env->GetLongField(thiz, fields.context);
    return std::shared_ptr<AudioCapabilities>(p);
}

jobject constructJavaAudioCapabilitiesFromNative(
        JNIEnv *env, std::shared_ptr<AudioCapabilities> audioCaps) {
    // construct Java bitrateRange
    const Range<int>& bitrateRange = audioCaps->getBitrateRange();
    jclass rangeClazz = env->FindClass("android/util/Range");
    jmethodID rangeConstructID = env->GetMethodID(rangeClazz, "<init>", "(I;I)V");
    jobject jBitrateRange = env->NewObject(rangeClazz, rangeConstructID,
            bitrateRange.lower(), bitrateRange.upper());

    // construct Java sampleRates array
    const std::vector<int>& sampleRates = audioCaps->getSupportedSampleRates();
    jintArray jSampleRates = env->NewIntArray(sampleRates.size());
    for (size_t i = 0; i < sampleRates.size(); ++i) {
        jint val = sampleRates.at(i);
        env->SetIntArrayRegion(jSampleRates, i, 1, &val);
    }

    // construct Java sampleRateRanges
    const std::vector<Range<int>>& sampleRateRanges = audioCaps->getSupportedSampleRateRanges();
    jobjectArray jSampleRateRanges = env->NewObjectArray(sampleRateRanges.size(), rangeClazz, NULL);
    for (int i = 0; i < sampleRateRanges.size(); i++) {
        Range<int> range = sampleRateRanges.at(i);
        jobject jRange = env->NewObject(rangeClazz, rangeConstructID,
                range.lower(), range.upper());
        env->SetObjectArrayElement(jSampleRateRanges, i, jRange);
        env->DeleteLocalRef(jRange);
        jRange = NULL;
    }

    // construct Java inputChannelRanges
    const std::vector<Range<int>>& inputChannelRanges = audioCaps->getInputChannelCountRanges();
    jobjectArray jInputChannelRanges = env->NewObjectArray(inputChannelRanges.size(), rangeClazz, NULL);
    for (int i = 0; i < inputChannelRanges.size(); i++) {
        Range<int> range = inputChannelRanges.at(i);
        jobject jRange = env->NewObject(rangeClazz, rangeConstructID,
                range.lower(), range.upper());
        env->SetObjectArrayElement(jInputChannelRanges, i, jRange);
        env->DeleteLocalRef(jRange);
        jRange = NULL;
    }

    // construct Java AudioCapabilities
    jclass audioCapsClazz =
        env->FindClass("android/media/MediaCodecInfo$AudioCapabilities");
    CHECK(audioCapsClazz != NULL);
    jmethodID audioCapsConstructID = env->GetMethodID(audioCapsClazz, "<init>",
            "(Landroid/util/Range;"
            "[I;"
            "[Landroid/util/Range;"
            "[Landroid/util/Range;)V");
    jobject jAudioCaps = env->NewObject(audioCapsClazz, audioCapsConstructID, jBitrateRange, jSampleRates,
            jSampleRateRanges, jInputChannelRanges);

    env->SetLongField(jAudioCaps, fields.context, (jlong)audioCaps.get());

    return jAudioCaps;
}

// ----------------------------------------------------------------------------

static jint android_media_AudioCapabilities_getMaxInputChannelCount(JNIEnv *env, jobject thiz) {
    std::shared_ptr<AudioCapabilities> audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int maxInputChannelCount = audioCaps->getMaxInputChannelCount();
    return maxInputChannelCount;
}

static jint android_media_AudioCapabilities_getMinInputChannelCount(JNIEnv *env, jobject thiz) {
    std::shared_ptr<AudioCapabilities> audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int minInputChannelCount = audioCaps->getMinInputChannelCount();
    return minInputChannelCount;
}

static jboolean android_media_AudioCapabilities_isSampleRateSupported(JNIEnv *env, jobject thiz, int sampleRate) {
    std::shared_ptr<AudioCapabilities> audioCaps = getAudioCapabilities(env, thiz);
    if (audioCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    bool res = audioCaps->isSampleRateSupported(sampleRate);
    return res;
}

// ----------------------------------------------------------------------------

static const JNINativeMethod gAudioCapsMethods[] = {
    {"native_getMaxInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMaxInputChannelCount},
    {"native_getMinInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMinInputChannelCount},
    {"native_isSampleRateSupported", "(I)Z", (void *)android_media_AudioCapabilities_isSampleRateSupported}
};

int register_android_media_AudioCapabilities(JNIEnv *env) {
    return AndroidRuntime::registerNativeMethods(env, "android/media/MediaCodecInfo$AudioCapabilities",
            gAudioCapsMethods, NELEM(gAudioCapsMethods));
}