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

import android.media.MediaDataSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * In-memory {@link MediaDataSource} used to feed bundled hall videos to the
 * media player.
 *
 * @hide
 */
public class HallVideoData extends MediaDataSource {
    private int count;
    private final ByteArrayInputStream stream;

    public HallVideoData(byte[] data) {
        this.count = data.length;
        this.stream = new ByteArrayInputStream(data);
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
        this.count = -1;
    }

    @Override
    public long getSize() throws IOException {
        return this.count;
    }

    @Override
    public int readAt(long position, byte[] buffer, int offset, int size) throws IOException {
        this.stream.reset();
        this.stream.skip(position);
        return this.stream.read(buffer, offset, size);
    }
}