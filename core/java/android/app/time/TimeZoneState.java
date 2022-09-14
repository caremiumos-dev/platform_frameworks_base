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
 * A snapshot of the system's time zone state.
 *
 * <p>{@code id} contains the system's time zone ID setting, e.g. "America/Los_Angeles". This
 * will usually agree with {@code TimeZone.getDefault().getID()} but it can be {@code null} or empty
 * in rare cases.
 *
 * <p>{@code userShouldConfirmId} is {@code true} if the system has low confidence in the current
 * time zone.
 *
 * @hide
 */
@SystemApi
public final class TimeZoneState implements Parcelable {

    public static final @NonNull Creator<TimeZoneState> CREATOR =
            new Creator<TimeZoneState>() {
                public TimeZoneState createFromParcel(Parcel in) {
                    return TimeZoneState.createFromParcel(in);
                }

                public TimeZoneState[] newArray(int size) {
                    return new TimeZoneState[size];
                }
            };

    @Nullable private final String mId;
    private boolean mUserShouldConfirmId;

    /** @hide */
    public TimeZoneState(@Nullable String id, boolean userShouldConfirmId) {
        mId = id;
        mUserShouldConfirmId = userShouldConfirmId;
    }

    private static TimeZoneState createFromParcel(Parcel in) {
        String zoneId = in.readString();
        boolean userShouldConfirmId = in.readBoolean();
        return new TimeZoneState(zoneId, userShouldConfirmId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeBoolean(mUserShouldConfirmId);
    }

    @NonNull
    public String getId() {
        return mId;
    }

    public boolean getUserShouldConfirmId() {
        return mUserShouldConfirmId;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeZoneState that = (TimeZoneState) o;
        return Objects.equals(mId, that.mId)
                && mUserShouldConfirmId == that.mUserShouldConfirmId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mUserShouldConfirmId);
    }

    @Override
    public String toString() {
        return "TimeZoneState{"
                + "mZoneId=" + mId
                + ", mUserShouldConfirmId=" + mUserShouldConfirmId
                + '}';
    }
}
