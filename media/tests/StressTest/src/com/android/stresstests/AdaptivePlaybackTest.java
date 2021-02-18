/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.mediastresstest;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.util.Pair;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class AdaptivePlaybackTest extends CodecDecoderTestBase {
    private static final String LOG_TAG = AdaptivePlaybackTest.class.getSimpleName();

    private final long mSeed = 0x12b9b0a1;  // random seed
    private final int mMaxIterations = 3;
    private final String mMime;
    private final String mMidFile;
    private final String mHighFile;
    private final String mLowFile;
    private final long mMidCRC;
    private final long mHighCRC;
    private final long mLowCRC;

    ArrayList<Long> mListCRC;

    public AdaptivePlaybackTest(String mime, String midFile, long midCRC, String highFile,
                                long highCRC, String lowFile, long lowCRC) {
        super(mime, midFile);
        mMime = mime;
        mMidFile = midFile;
        mHighFile = highFile;
        mLowFile = lowFile;
        mMidCRC = midCRC;
        mHighCRC = highCRC;
        mLowCRC = lowCRC;
    }

    @Rule
    public ActivityTestRule<CodecTestActivity> mActivityRule =
            new ActivityTestRule<>(CodecTestActivity.class);

    @Parameterized.Parameters(name = "{index}({0})")
    public static Collection<Object[]> input() {
        List<Object[]> inputList = new ArrayList<>();
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_HEVC,
                "bbb_1280x720_768kbps_30fps_hevc.mp4", 0L,
                "bbb_1920x1080_3mbps_30fps_hevc.mp4", 0L,
                "bbb_1280x720_768kbps_30fps_hevc.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_HEVC,
                "bbb_hevc_640x360_1600kbps_30fps.mp4", 0L,
                "bbb_hevc_176x144_176kbps_60fps.mp4", 0L,
                "bbb_1280x720_768kbps_30fps_hevc.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_MPEG2,
                "bbb_640x360_1mbps_30fps_mpeg2.mp4", 0L,
                "bbb_1920x1080_5mbps_30fps_mpeg2.mp4", 0L,
                "bbb_640x360_1mbps_30fps_mpeg2.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_MPEG2,
                "bbb_cif_768kbps_30fps_mpeg2_stereo_48kHz_192kbps_mp3.mp4", 0L,
                "bbb_384x216_768kbps_30fps_mpeg2_2b.mp4", 0L,
                "bbb_cif_768kbps_30fps_mpeg2_stereo_48kHz_192kbps_mp3.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_MPEG4,
                "bbb_128x96_64kbps_12fps_mpeg4.mp4", 0L,
                "bbb_176x144_192kbps_15fps_mpeg4.mp4", 0L,
                "bbb_128x96_64kbps_12fps_mpeg4.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_H263,
                "bbb_176x144_128kbps_15fps_h263.3gp", 0L,
                "bbb_176x144_192kbps_10fps_h263.3gp", 0L,
                "bbb_176x144_128kbps_15fps_h263.3gp", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_VP8,
                "bbb_1280x720_1mbps_30fps_vp8.webm", 0L,
                "bbb_1920x1080_3mbps_30fps_vp8.webm", 0L,
                "bbb_1280x720_1mbps_30fps_vp8.webm", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_VP8,
                "bbb_vp8_176x144_240kbps_60fps.webm", 0L,
                "bbb_vp8_640x360_2mbps_30fps.webm", 0L,
                "bbb_1280x720_1mbps_30fps_vp8.webm", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_VP9,
                "bbb_1280x720_1mbps_30fps_vp9.webm", 0L,
                "bbb_1920x1080_3mbps_30fps_vp9.webm", 0L,
                "bbb_1280x720_1mbps_30fps_vp9.webm", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_VP9,
                "bbb_vp9_176x144_285kbps_60fps.webm", 0L,
                "bbb_vp9_640x360_1600kbps_30fps.webm", 0L,
                "bbb_1280x720_1mbps_30fps_vp9.webm", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_AV1,
                "bbb_1280x720_768kbps_30fps_av1.mp4", 0L,
                "bbb_1920x1080_2mbps_30fps_av1.mp4", 0L,
                "bbb_1280x720_768kbps_30fps_av1.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_AVC,
                "bbb_avc_176x144_300kbps_60fps.mp4", 0L,
                "bbb_avc_640x360_768kbps_30fps.mp4", 0L,
                "bbb_1280x720_1mbps_30fps_avc.mp4", 0L});
        inputList.add(new Object[]{MediaFormat.MIMETYPE_VIDEO_AVC,
                "bbb_1280x720_1mbps_30fps_avc.mp4", 0L,
                "bbb_1920x1080_3mbps_30fps_avc.mp4", 0L,
                "bbb_1280x720_1mbps_30fps_avc.mp4", 0L});
        return inputList;
    }

    public ArrayList<String> getCodecsListAdaptive() throws IOException {
        ArrayList<MediaFormat> formats = new ArrayList<>();
        formats.add(setUpSource(mLowFile));
        mExtractor.release();
        formats.add(setUpSource(mHighFile));
        mExtractor.release();
        formats.add(setUpSource(mMidFile));
        mExtractor.release();
        return selectCodecs(mMime, formats,
                new String[]{MediaCodecInfo.CodecCapabilities.FEATURE_AdaptivePlayback}, false);
    }

    @Override
    void enqueueInput(int bufferIndex) {
        if (mExtractor.getSampleSize() < 0) {
            mSawInputEOS = true;
        } else {
            ByteBuffer inputBuffer = mCodec.getInputBuffer(bufferIndex);
            mExtractor.readSampleData(inputBuffer, 0);
            int size = (int) mExtractor.getSampleSize();
            long pts = mExtractor.getSampleTime();
            int extractorFlags = mExtractor.getSampleFlags();
            int codecFlags = 0;
            if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                codecFlags |= MediaCodec.BUFFER_FLAG_KEY_FRAME;
            }
            if ((extractorFlags & MediaExtractor.SAMPLE_FLAG_PARTIAL_FRAME) != 0) {
                codecFlags |= MediaCodec.BUFFER_FLAG_PARTIAL_FRAME;
            }
            if (!mExtractor.advance()) {
                //codecFlags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                mSawInputEOS = true;
            }
            if (ENABLE_LOGS) {
                Log.v(LOG_TAG, "input: id: " + bufferIndex + " size: " + size + " pts: " + pts +
                        " flags: " + codecFlags);
            }
            mCodec.queueInputBuffer(bufferIndex, 0, size, pts, codecFlags);
            if (size > 0 && (codecFlags & (MediaCodec.BUFFER_FLAG_CODEC_CONFIG |
                    MediaCodec.BUFFER_FLAG_PARTIAL_FRAME)) == 0) {
                mOutputBuff.saveInPTS(pts);
                mInputCount++;
            }
        }
    }

    @Override
    void doWork(int frameLimit) throws InterruptedException, IOException {
        if (mIsCodecInAsyncMode) {
            // dequeue output after inputEOS is expected to be done in waitForAllOutputs()
            while (!mAsyncHandle.hasSeenError() && !mSawInputEOS) {
                Pair<Integer, MediaCodec.BufferInfo> element = mAsyncHandle.getWork();
                if (element != null) {
                    int bufferID = element.first;
                    MediaCodec.BufferInfo info = element.second;
                    if (info != null) {
                        // <id, info> corresponds to output callback. Handle it accordingly
                        dequeueOutput(bufferID, info);
                    } else {
                        // <id, null> corresponds to input callback. Handle it accordingly
                        enqueueInput(bufferID);
                    }
                }
                // verification of checksum
                if (mSignalledOutFormatChanged) {
                    assertEquals(String.format("%s checksum mismatch", mTestFile),
                            mListCRC.remove(0),
                            mOutputBuff.getCheckSumImage(), 0L);
                    mSignalledOutFormatChanged = false;
                    mOutputBuff.reset();
                }
            }
        } else {
            MediaCodec.BufferInfo outInfo = new MediaCodec.BufferInfo();
            // dequeue output after inputEOS is expected to be done in waitForAllOutputs()
            while (!mSawInputEOS) {
                int outputBufferId = mCodec.dequeueOutputBuffer(outInfo, Q_DEQ_TIMEOUT_US);
                if (outputBufferId >= 0) {
                    dequeueOutput(outputBufferId, outInfo);
                } else if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    mOutFormat = mCodec.getOutputFormat();
                    mSignalledOutFormatChanged = true;
                }
                int inputBufferId = mCodec.dequeueInputBuffer(Q_DEQ_TIMEOUT_US);
                if (inputBufferId != -1) {
                    enqueueInput(inputBufferId);
                }
                if (mSignalledOutFormatChanged) {
                    assertEquals(String.format("%s checksum mismatch", mTestFile),
                            mListCRC.remove(0),
                            mOutputBuff.getCheckSumImage(), 0L);
                    mSignalledOutFormatChanged = false;
                    mOutputBuff.reset();
                }
            }
        }
        mSawInputEOS = false;
    }

    @LargeTest
    @Test(timeout = mMaxIterations * CodecTestBase.PER_TEST_TIMEOUT_SMALL_TEST_MS)
    public void testAdaptivePlayback() throws IOException, InterruptedException {
        final int mode = MediaExtractor.SEEK_TO_CLOSEST_SYNC;
        ArrayList<String> listOfDecoders = getCodecsListAdaptive();
        Assume.assumeTrue("no codecs supporting adaptive playback for mime" + mMime,
                !listOfDecoders.isEmpty());

        String[] testFileList = new String[]{mLowFile, mHighFile, mMidFile};

        Random rand = new Random(mSeed);
        setUpSurface(mActivityRule.getActivity());
        for (String decoder : listOfDecoders) {
            mListCRC = new ArrayList<>(Arrays.asList(new Long[]{mLowCRC, mHighCRC, mMidCRC}));
            // for format change from default init to first clip, need to find alternative
            mListCRC.add(0, 0L);
            mOutputBuff = new OutputManager();
            mCodec = MediaCodec.createByCodecName(decoder);
            String log = "";
            MediaFormat format = setUpSource(mLowFile);
            boolean isAsync = ((rand.nextInt() & 1) == 0); // TODO: loop through?
            mIsCodecInAsyncMode = isAsync;
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            configureCodec(format, isAsync, false, false);
            mCodec.start();
            // Enqueue inputs, switch file
            for (String file : testFileList) {
                log = String.format("codec: %s, file: %s, mode: %s:: ",
                        decoder, file, (isAsync ? "async" : "sync"));
                format = setUpSource(file);
                mCsdBuffers.clear();
                for (int i = 0; ; i++) {
                    String csdKey = "csd-" + i;
                    if (format.containsKey(csdKey)) {
                        mCsdBuffers.add(format.getByteBuffer(csdKey));
                    } else break;
                }
                mSaveToMem = false;
                mOutputBuff = new OutputManager();
                // Ready surface
                CodecTestActivity activity = mActivityRule.getActivity();
                activity.mSurfaceLock.lock();
                activity.setSurfaceStatus(true);
                activity.setScreenParams(getWidth(format), getHeight(format), true);
                activity.mSurfaceLock.unlock();

                mExtractor.seekTo(0, mode);
                queueCodecConfig();
                doWork(Integer.MAX_VALUE);
                assertTrue(log + " unexpected error", !mAsyncHandle.hasSeenError());
            }
            // queuing EOS
            super.queueEOS();
            waitForAllOutputs();
            // verify
            assertEquals(String.format("%s checksum mismatch", mTestFile),
                    mListCRC.remove(0),
                    mOutputBuff.getCheckSumImage(), 0L);

            mCodec.reset();
            assertTrue(log + " unexpected error", !mAsyncHandle.hasSeenError());
            mExtractor.release();
            mCodec.release();
            mOutputBuff = null;
        }
        tearDownSurface();
    }
}
