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

package com.android.server.policy.hall;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.utils.HallUtil;

import java.io.File;

/**
 * Plays the open/close sounds for the hall event.
 *
 * @hide
 */
public class HallSound extends HallAction {
    private static final String CLOSE_SUFFIX = ".close";
    private static final String TAG = "Sys" + HallSound.class.getSimpleName();

    private AudioManager audioManager;
    private int flipCloseId;
    private int flipOpenId;
    private SoundPool flipSoundPool;
    private AudioManager.OnAudioFocusChangeListener mAudioListener;
    private int playingId;
    private int soundIndex;
    private String[] soundList;
    private String soundPath;

    public HallSound(Context context) {
        super(context);
        this.mAudioListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        stopSound();
                        break;
                    default:
                        break;
                }
            }
        };
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private boolean isPrebuiltSound() {
        if (!validIndex(this.soundIndex)) {
            return false;
        }
        return this.soundList[this.soundIndex].equals(this.soundPath)
                || TextUtils.isEmpty(this.soundPath);
    }

    private boolean needPlaySound() {
        if (validIndex(this.soundIndex)) {
            return true;
        }
        return fileCanRead(this.soundPath);
    }

    private void reloadSound(String path) {
        if (!fileCanRead(path)) {
            Log.w(TAG, "file not exist or can not read path:" + path);
            return;
        }
        this.soundPath = path;
        putSetting(HallUtil.SOUND_PROP, this.soundPath);
        if (this.flipOpenId > 0 || this.flipCloseId > 0) {
            this.flipSoundPool.unload(this.flipOpenId);
            this.flipSoundPool.unload(this.flipCloseId);
        }
        File openFile = new File(this.soundPath, "open.aac");
        File closeFile = new File(this.soundPath, "close.aac");
        this.flipOpenId = this.flipSoundPool.load(openFile.getPath(), 1);
        this.flipCloseId = this.flipSoundPool.load(closeFile.getPath(), 1);
        Log.d(TAG, "load sound path:" + path);
    }

    private void setHallSound(int index) {
        if (validIndex(index) && this.soundIndex != index) {
            this.soundIndex = index;
            if (!DISABLE_VALUE.equals(this.soundList[this.soundIndex])) {
                reloadSound(this.soundList[this.soundIndex]);
                return;
            }
            this.soundPath = DISABLE_VALUE;
            putSetting(HallUtil.SOUND_PROP, this.soundPath);
            return;
        }
        if (index != DISABLE_INDEX) {
            Log.d(TAG, "do not load sound value:" + index);
            return;
        }
        this.soundIndex = index;
        this.soundPath = DISABLE_VALUE;
        putSetting(HallUtil.SOUND_PROP, this.soundPath);
        Log.d(TAG, "disable sound");
    }

    private boolean validIndex(int index) {
        return index >= 0 && index < this.soundList.length;
    }

    public void abandonAudioFocus() {
        int result = this.audioManager.abandonAudioFocus(this.mAudioListener);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d(TAG, "abandonAudioFocus failed:" + result);
        }
    }

    @Override
    public void init() {
        this.soundIndex = SystemProperties.getInt(HallUtil.SOUND_PROP,
                HallDeviceManager.instance().getDefaultSoundIndex());
        this.soundPath = getSetting(HallUtil.SOUND_PROP);
        this.flipSoundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
        this.soundList = HallDeviceManager.instance().getSoundList(this.context);
        if (this.soundPath != null) {
            File file = new File(this.soundPath);
            if (file.exists() && file.isFile() && validIndex(this.soundIndex)) {
                Log.d(TAG, "old path:" + this.soundPath);
                this.soundPath = this.soundList[this.soundIndex];
                putSetting(HallUtil.SOUND_PROP, this.soundPath);
                Log.d(TAG, "new path:" + this.soundPath);
            }
        }
        if (needPlaySound()) {
            reloadSound(isPrebuiltSound() ? this.soundList[this.soundIndex] : this.soundPath);
        }
    }

    public void playFlipSound(boolean open) {
        if (needPlaySound()) {
            stopSound();
            this.workHandler.removeMessages(MSG_ABANDON_FOCUS);
            int result = this.audioManager.requestAudioFocus(this.mAudioListener,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                Log.d(TAG, "request focus failed:" + result);
                return;
            }
            this.playingId = this.flipSoundPool.play(open ? this.flipOpenId : this.flipCloseId,
                    1.0f, 1.0f, 1, 0, 1.0f);
            if (this.playingId == 0) {
                Log.d(TAG, "play sound failed");
            }
            this.workHandler.sendEmptyMessageDelayed(MSG_ABANDON_FOCUS, 4000L);
        }
    }

    public void setHallSound(String value) {
        try {
            setHallSound(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            if (!DISABLE_VALUE.equals(value)) {
                reloadSound(value);
                return;
            }
            this.soundIndex = DISABLE_INDEX;
            this.soundPath = value;
            putSetting(HallUtil.SOUND_PROP, value);
        }
    }

    public void stopOpenSound() {
        if (this.playingId == 0 || !HallUtil.hallOpen()) {
            return;
        }
        this.flipSoundPool.stop(this.playingId);
    }

    public void stopSound() {
        if (this.playingId != 0) {
            this.flipSoundPool.stop(this.playingId);
        }
    }
}