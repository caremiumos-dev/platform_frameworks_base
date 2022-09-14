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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Objects;

/**
 * A snapshot of the system clock state.
 *
 * <p>{@code mUnixEpochTime} contains a snapshot of the system clock time and elapsed realtime clock
 * time.
 *
 * <p>{@code mUserShouldConfirmTime} is {@code true} if the system has low confidence in the system
 * clock time.
 *
 * @hide
 */
@SystemApi
public final class ClockState implements Parcelable {

    public static final @NonNull Creator<ClockState> CREATOR =
            new Creator<ClockState>() {
                public ClockState createFromParcel(Parcel in) {
                    return ClockState.createFromParcel(in);
                }

                public ClockState[] newArray(int size) {
                    return new ClockState[size];
                }
            };

    @NonNull private final UnixEpochTime mUnixEpochTime;
    private boolean mUserShouldConfirmTime;

    /** @hide */
    public ClockState(@NonNull UnixEpochTime unixEpochTime, boolean userShouldConfirmId) {
        mUnixEpochTime = Objects.requireNonNull(unixEpochTime);
        mUserShouldConfirmTime = userShouldConfirmId;
    }

    private static ClockState createFromParcel(Parcel in) {
        UnixEpochTime unixEpochTime = in.readParcelable(null, UnixEpochTime.class);
        boolean userShouldConfirmId = in.readBoolean();
        return new ClockState(unixEpochTime, userShouldConfirmId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeParcelable(mUnixEpochTime, 0);
        dest.writeBoolean(mUserShouldConfirmTime);
    }

    public boolean getUserShouldConfirmTime() {
        return mUserShouldConfirmTime;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClockState that = (ClockState) o;
        return Objects.equals(mUnixEpochTime, that.mUnixEpochTime)
                && mUserShouldConfirmTime == that.mUserShouldConfirmTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mUnixEpochTime, mUserShouldConfirmTime);
    }

    @Override
    public String toString() {
        return "ClockState{"
                + "mUnixEpochTime=" + mUnixEpochTime
                + ", mUserShouldConfirmTime=" + mUserShouldConfirmTime
                + '}';
    }
}
