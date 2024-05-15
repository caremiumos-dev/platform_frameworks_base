/*
 * Copyright 2024, The Android Open Source Project
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

#include "android_media_Streams.h"
#include "android_runtime/AndroidRuntime.h"
#include "jni.h"

#include <media/AudioCapabilities.h>
#include <media/CodecCapabilities.h>
#include <media/stagefright/foundation/ADebug.h>
#include <media/stagefright/foundation/AMessage.h>
#include <nativehelper/JNIHelp.h>
#include <utils/Log.h>

using namespace android;

// ----------------------------------------------------------------------------

struct fields_t {
    jfieldID context;
};
static fields_t fields;

static std::shared_ptr<AudioCapabilities> getAudioCapabilities(JNIEnv *env, jobject thiz) {
    AudioCapabilities* const p = (AudioCapabilities*)env->GetLongField(thiz, fields.context);
    return std::shared_ptr<AudioCapabilities>(p);
}

static std::shared_ptr<CodecCapabilities> getCodecCapabilities(JNIEnv *env, jobject thiz) {
    CodecCapabilities* const p = (CodecCapabilities*)env->GetLongField(thiz, fields.context);
    return std::shared_ptr<CodecCapabilities>(p);
}

jobject constructJavaAudioCapabilitiesFromNative(
        JNIEnv *env, std::shared_ptr<AudioCapabilities> audioCaps) {
    if (audioCaps == nullptr) {
        return NULL;
    }
    
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

jobject constructJavaCodecCapabilitiesFromNative(
        JNIEnv *env, std::shared_ptr<CodecCapabilities> codecCaps) {
    if (codecCaps == nullptr) {
        return NULL;
    }

    // Construct defaultFormat
    sp<AMessage> defaultFormat = codecCaps->getDefaultFormat();

    jobject defaultFormatObj = NULL;
    if (ConvertMessageToMap(env, defaultFormat, &defaultFormatObj)) {
        return NULL;
    }

    // Construct Java ProfileLevelArray
    std::vector<ProfileLevel> profileLevels = codecCaps->getProfileLevels();

    jclass profileLevelClazz =
        env->FindClass("android/media/MediaCodecInfo$CodecProfileLevel");
    CHECK(profileLevelClazz != NULL);

    jobjectArray profileLevelArray =
            env->NewObjectArray(profileLevels.size(), profileLevelClazz, NULL);

    jfieldID profileField =
            env->GetFieldID(profileLevelClazz, "profile", "I");
    jfieldID levelField =
            env->GetFieldID(profileLevelClazz, "level", "I");

    for (size_t i = 0; i < profileLevels.size(); ++i) {
        const ProfileLevel &src = profileLevels.at(i);

        jobject profileLevelObj = env->AllocObject(profileLevelClazz);

        env->SetIntField(profileLevelObj, profileField, src.mProfile);
        env->SetIntField(profileLevelObj, levelField, src.mLevel);

        env->SetObjectArrayElement(profileLevelArray, i, profileLevelObj);

        env->DeleteLocalRef(profileLevelObj);
        profileLevelObj = NULL;
    }

    // Construct ColorFormatArray
    std::vector<uint32_t> colorFormats = codecCaps->getColorFormats();

    jintArray colorFormatsArray = env->NewIntArray(colorFormats.size());
    for (size_t i = 0; i < colorFormats.size(); ++i) {
        jint val = colorFormats.at(i);
        env->SetIntArrayRegion(colorFormatsArray, i, 1, &val);
    }

    // Construct and set AudioCapabilities
    jobject jAudioCaps = NULL;
    std::shared_ptr<AudioCapabilities> audioCaps = codecCaps->getAudioCapabilities();
    jAudioCaps = constructJavaAudioCapabilitiesFromNative(audioCaps);

    // Set VideoCapabilities
    jobject jVideoCaps = NULL;
    if (codecCaps->isVideo()) {
        std::shared_ptr<VideoCapabilities> videoCaps = codecCaps->getVideoCapabilities();

        jclass videoCapsClazz =
            env->FindClass("android/media/MediaCodecInfo$VideoCapabilities");
        CHECK(videoCapsClazz != NULL);
        jmethodID videoCapsConstructID = env->GetMethodID(videoCapsClazz, "<init>", "()V");
        jVideoCaps = env->NewObject(videoCapsClazz, videoCapsConstructID);

        env->SetLongField(jVideoCaps, fields.context, (jlong)videoCaps.get());
    }

    // Set EncoderCapabilities
    jobject jEncoderCaps = NULL;
    if (codecCaps->isEncoder()) {
        std::shared_ptr<EncoderCapabilities> encoderCaps = codecCaps->getEncoderCapabilities();

        jclass encoderCapsClazz =
            env->FindClass("android/media/MediaCodecInfo$EncoderCapabilities");
        CHECK(encoderCapsClazz != NULL);
        jmethodID encoderCapsConstructID = env->GetMethodID(encoderCapsClazz, "<init>", "()V");
        jEncoderCaps = env->NewObject(encoderCapsClazz, encoderCapsConstructID);

        env->SetLongField(jEncoderCaps, fields.context, (jlong)encoderCaps.get());
    }

    // Construct CodecCapabilities
    jclass capsClazz =
        env->FindClass("android/media/MediaCodecInfo$CodecCapabilities");
    CHECK(capsClazz != NULL);

    jmethodID capsConstructID = env->GetMethodID(capsClazz, "<init>",
                "([Landroid/media/MediaCodecInfo$CodecProfileLevel;[I"
                "Ljava/util/Map;Landroid/media/MediaCodecInfo$AudioCapabilities"
                "Landroid/media/MediaCodecInfo$VideoCapabilities"
                "Landroid/media/MediaCodecInfo$EncoderCapabilities;)V");

    jobject jCodecCaps = env->NewObject(capsClazz, capsConstructID,
            profileLevelArray, colorFormatsArray, defaultFormatObj,
            jAudioCaps, jVideoCaps, jEncoderCaps);


    env->DeleteLocalRef(defaultFormatObj);
    defaultFormatObj = NULL;

    env->DeleteLocalRef(profileLevelArray);
    profileLevelArray = NULL;

    env->DeleteLocalRef(colorFormatsArray);
    colorFormatsArray = NULL;

    env->DeleteLocalRef(jAudioCaps);
    jAudioCaps = NULL;

    env->DeleteLocalRef(jVideoCaps);
    jVideoCaps = NULL;

    env->DeleteLocalRef(jEncoderCaps);
    jEncoderCaps = NULL;

    env->SetLongField(jCodecCaps, fields.context, (jlong)codecCaps.get());

    return jCodecCaps;
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

// CodecCapabilities

static jint android_media_CodecCapabilities_getMaxSupportedInstances(JNIEnv *env, jobject thiz) {
    std::shared_ptr<CodecCapabilities> codecCaps = getCodecCapabilities(env, thiz);
    if (codecCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return 0;
    }

    int maxSupportedInstances = codecCaps->getMaxSupportedInstances();
    return maxSupportedInstances;
}

static jstring android_media_CodecCapabilities_getMimeType(JNIEnv *env, jobject thiz) {
    std::shared_ptr<CodecCapabilities> codecCaps = getCodecCapabilities(env, thiz);
    if (codecCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return NULL;
    }

    std::string mediaType = codecCaps->getMediaType();
    return env->NewStringUTF(mediaType.c_str());
}

static jboolean android_media_CodecCapabilities_isFeatureRequired(
        JNIEnv *env, jobject thiz, jstring name) {
    std::shared_ptr<CodecCapabilities> codecCaps = getCodecCapabilities(env, thiz);
    if (codecCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }

    if (name == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return -ENOENT;
    }

    const char *nameStr = env->GetStringUTFChars(name, NULL);
    if (nameStr == NULL) {
        // Out of memory exception already pending.
        return -ENOENT;
    }

    bool isFeatureRequired = codecCaps->isFeatureRequired(std::string(nameStr));

    env->ReleaseStringUTFChars(name, nameStr);

    return isFeatureRequired;
}

static jboolean android_media_CodecCapabilities_isFeatureSupported(
        JNIEnv *env, jobject thiz, jstring name) {
    std::shared_ptr<CodecCapabilities> codecCaps = getCodecCapabilities(env, thiz);
    if (codecCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }

    if (name == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return -ENOENT;
    }

    const char *nameStr = env->GetStringUTFChars(name, NULL);
    if (nameStr == NULL) {
        // Out of memory exception already pending.
        return -ENOENT;
    }

    bool isFeatureSupported = codecCaps->isFeatureSupported(std::string(nameStr));

    env->ReleaseStringUTFChars(name, nameStr);

    return isFeatureSupported;
}

static jboolean android_media_CodecCapabilities_isFormatSupported(JNIEnv *env, jobject thiz,
        jobjectArray keys, jobjectArray values) {
    std::shared_ptr<CodecCapabilities> codecCaps = getCodecCapabilities(env, thiz);
    if (codecCaps == NULL) {
        jniThrowException(env, "java/lang/IllegalStateException", NULL);
        return false;
    }

    sp<AMessage> format;
    status_t err = ConvertKeyValueArraysToMessage(env, keys, values, &format);
    if (err != OK) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return -ENOENT;;
    }

    return codecCaps->isFormatSupported(format);
}

static jobject android_media_CodecCapabilities_CreateFromProfileLevel(JNIEnv *env,
        jobject /* thiz */, jstring mediaType, jint profile, jint level) {
    if (mediaType == NULL) {
        jniThrowException(env, "java/lang/IllegalArgumentException", NULL);
        return NULL;
    }

    const char *mediaTypeStr = env->GetStringUTFChars(mediaType, NULL);
    if (mediaTypeStr == NULL) {
        return NULL;
    }

    std::shared_ptr<CodecCapabilities> codecCaps = CodecCapabilities::CreateFromProfileLevel(
            mediaTypeStr, profile, level);

    jobject jCodecCaps = constructJavaCodecCapabilitiesFromNative(env, codecCaps);

    return jCodecCaps;
}

// ----------------------------------------------------------------------------

static const JNINativeMethod gAudioCapsMethods[] = {
    {"native_getMaxInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMaxInputChannelCount},
    {"native_getMinInputChannelCount", "()I", (void *)android_media_AudioCapabilities_getMinInputChannelCount},
    {"native_isSampleRateSupported", "(I)Z", (void *)android_media_AudioCapabilities_isSampleRateSupported}
};

static const JNINativeMethod gCodecCapsMethods[] = {
    { "native_getMaxSupportedInstances", "()I", (void *)android_media_CodecCapabilities_getMaxSupportedInstances },
    { "native_getMimeType", "()Ljava/lang/String", (void *)android_media_CodecCapabilities_getMimeType },
    { "native_isFeatureRequired", "(Ljava/lang/String)Z", (void *)android_media_CodecCapabilities_isFeatureRequired },
    { "native_isFeatureSupported", "(Ljava/lang/String)Z", (void *)android_media_CodecCapabilities_isFeatureSupported },
    { "native_isFormatSupported", "([Ljava/lang/String;[Ljava/lang/Object)Z", (void *)android_media_CodecCapabilities_isFormatSupported},
    { "native_CreateFromProfileLevel", "(Ljava/lang/String;I;I)Landroid/media/MediaCodecInfo$CodecCapabilities", (void *)android_media_CodecCapabilities_CreateFromProfileLevel},
};

int register_android_media_CodecCapabilities(JNIEnv *env) {
    int result = AndroidRuntime::registerNativeMethods(env, "android/media/MediaCodecInfo$AudioCapabilities",
            gAudioCapsMethods, NELEM(gAudioCapsMethods));
    if (result != JNI_OK) {
        return result;
    }
    result = AndroidRuntime::registerNativeMethods(env, "android/media/MediaCodecInfo$CodecCapabilities",
            gCodecCapsMethods, NELEM(gCodecCapsMethods));
    return result;
}
