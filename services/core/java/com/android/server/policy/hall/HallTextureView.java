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

import android.animation.Animator;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.IOException;

/**
 * Hall animation view that plays a bundled video through a {@link TextureView}.
 *
 * @hide
 */
public class HallTextureView extends HallAnimationView {
    private static final String TAG = HallTextureView.class.getSimpleName();

    private final String CLOSE_FILE = "close.mp4";
    private final String OPEN_FILE = "open.mp4";

    private final MediaPlayer.OnCompletionListener completionListener;
    private final MediaPlayer.OnErrorListener errorListener;
    MediaPlayer mediaPlayer;
    Animator.AnimatorListener notifyListener;
    private Surface savedSurface;
    private SurfaceTexture savedTexture;
    private final TextureView.SurfaceTextureListener textureListener;
    TextureView textureView;

    public HallTextureView(Context context) {
        this(context, null);
    }

    public HallTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HallTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.completionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                onAnimationEnd(null);
            }
        };
        this.errorListener = new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.w(TAG, "play error what:" + what + " extra:" + extra);
                return true;
            }
        };
        this.textureListener = new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
                if (savedTexture == null) {
                    savedTexture = texture;
                    savedSurface = new Surface(savedTexture);
                    mediaPlayer.setSurface(savedSurface);
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
                return false;
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture texture) {
                onAnimationUpdated();
            }
        };
        setBackgroundColor(0xff000000);
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.setOnErrorListener(this.errorListener);
        this.mediaPlayer.setOnCompletionListener(this.completionListener);
        this.mediaPlayer.setVolume(0.0f, 0.0f);
        this.textureView = new TextureView(context);
        this.textureView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        this.textureView.setSurfaceTextureListener(this.textureListener);
        addView(this.textureView);
    }

    @Override
    public void cancelAnimation() {
        if (this.mediaPlayer.isPlaying()) {
            this.mediaPlayer.pause();
            this.mediaPlayer.seekTo(0);
        }
    }

    @Override
    public void disableAnimator(boolean disable) {
    }

    @Override
    public long getDuration() {
        return this.mediaPlayer.getDuration();
    }

    @Override
    public boolean isAnimatorRunning() {
        return this.mediaPlayer.isPlaying();
    }

    @Override
    public void loadAnimation(String path) {
        this.animPath = path;
        setAnimation();
    }

    protected void onAnimationEnd(Animator animator) {
        if (this.notifyListener != null) {
            this.notifyListener.onAnimationEnd(animator);
        }
    }

    protected void onAnimationUpdated() {
    }

    @Override
    protected void onAttachedToWindow() {
        if (this.savedTexture != null) {
            this.textureView.setSurfaceTextureListener(this.textureListener);
            this.textureView.setSurfaceTexture(this.savedTexture);
            this.mediaPlayer.setSurface(this.savedSurface);
        }
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAnimation();
    }

    @Override
    public void playAnimation() {
        if (this.mediaPlayer.isPlaying()) {
            return;
        }
        this.mediaPlayer.start();
    }

    @Override
    public void requestHideAnimation() {
    }

    @Override
    public void setAnimation() {
        setVideoData(false);
    }

    @Override
    public void setAnimatorListener(Animator.AnimatorListener listener) {
        this.notifyListener = listener;
    }

    @Override
    public void setOnDrawListener(OnDrawListener listener) {
    }

    @Override
    public void setTitle(String title) {
    }

    void setVideoData(boolean open) {
        File file = new File(this.animPath, open ? OPEN_FILE : CLOSE_FILE);
        if (!file.exists()) {
            Log.d(TAG, "video not exist:" + file.getPath());
            return;
        }
        this.mediaPlayer.reset();
        try {
            this.mediaPlayer.setDataSource(file.getPath());
            this.mediaPlayer.prepare();
        } catch (IOException e) {
            Log.e(TAG, "prepare error", e);
        }
    }
}