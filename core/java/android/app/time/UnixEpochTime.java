/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.app.time;

import android.annotation.ElapsedRealtimeLong;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;

import java.util.Objects;

/**
 * A Unix epoch time value with an associated reading from the elapsed realtime clock.
 * When representing a device's system clock time, the Unix epoch time can be obtained using {@link
 * System#currentTimeMillis()}. The Unix epoch time might also come from an external source
 * depending on usage.
 *
 * <p>The elapsed realtime clock can be obtained using methods like {@link
 * SystemClock#elapsedRealtime()} or {@link SystemClock#elapsedRealtimeClock()}.
 *
 * @hide
 */
@SystemApi
public final class UnixEpochTime implements Parcelable {
    @ElapsedRealtimeLong private final long mElapsedRealtimeTimeMillis;
    private final long mUnixEpochTimeMillis;

    public UnixEpochTime(@ElapsedRealtimeLong long elapsedRealtimeMillis,
            long unixEpochTimeMillis) {
        mElapsedRealtimeTimeMillis = elapsedRealtimeMillis;
        mUnixEpochTimeMillis = unixEpochTimeMillis;
    }

    /** Returns the elapsed realtime clock value. See {@link UnixEpochTime} for more information. */
    public @ElapsedRealtimeLong long getElapsedRealtimeMillis() {
        return mElapsedRealtimeTimeMillis;
    }

    /** Returns the unix epoch time value. See {@link UnixEpochTime} for more information. */
    @Nullable
    public long getUnixEpochTimeMillis() {
        return mUnixEpochTimeMillis;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UnixEpochTime that = (UnixEpochTime) o;
        return mElapsedRealtimeTimeMillis == that.mElapsedRealtimeTimeMillis
                && Objects.equals(mUnixEpochTimeMillis, that.mUnixEpochTimeMillis);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mElapsedRealtimeTimeMillis, mUnixEpochTimeMillis);
    }

    @Override
    public String toString() {
        return "UnixEpochTime{"
                + "mElapsedRealtimeTimeMillis=" + mElapsedRealtimeTimeMillis
                + ", mUnixEpochTimeMillis=" + mUnixEpochTimeMillis
                + '}';
    }

    public static final @NonNull Creator<UnixEpochTime> CREATOR =
            new ClassLoaderCreator<UnixEpochTime>() {

                @Override
                public UnixEpochTime createFromParcel(@NonNull Parcel source) {
                    return createFromParcel(source, null);
                }

                @Override
                public UnixEpochTime createFromParcel(
                        @NonNull Parcel source, @Nullable ClassLoader classLoader) {
                    long elapsedRealtimeMillis = source.readLong();
                    long unixEpochTimeMillis = source.readLong();
                    return new UnixEpochTime(elapsedRealtimeMillis, unixEpochTimeMillis);
                }

                @Override
                public UnixEpochTime[] newArray(int size) {
                    return new UnixEpochTime[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeLong(mElapsedRealtimeTimeMillis);
        dest.writeLong(mUnixEpochTimeMillis);
    }

    /**
     * Creates a new Unix epoch time value at {@code elapsedRealtimeTimeMillis} by adjusting this
     * Unix epoch time by the difference between the elapsed realtime value supplied and the one
     * associated with this instance.
     *
     * @hide
     */
    public UnixEpochTime at(@ElapsedRealtimeLong long elapsedRealtimeTimeMillis) {
        long adjustedUnixEpochTimeMillis =
                (elapsedRealtimeTimeMillis - mElapsedRealtimeTimeMillis) + mUnixEpochTimeMillis;
        return new UnixEpochTime(elapsedRealtimeTimeMillis, adjustedUnixEpochTimeMillis);
    }
}
