/*
 * Copyright 2018 The Android Open Source Project
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

package android.telephony.data;

import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.AccessNetworkConstants.AccessNetworkType;
import android.telephony.data.ApnSetting.ApnType;

import java.util.Arrays;

/**
 * Description of a list of qualified networks for a particular APN type.
 *
 * @hide
 */
@SystemApi
public final class QualifiedNetworks implements Parcelable {

    private final int mApnType;

    private final int[] mNetworks;

    /** @hide */
    public QualifiedNetworks(@ApnType int apnType, int[] availableNetworks) {
        mApnType = apnType;
        mNetworks = availableNetworks;
    }

    /** @hide */
    public QualifiedNetworks(Parcel source) {
        mApnType = source.readInt();
        mNetworks = source.createIntArray();
    }

    /**
     * @return {@link ApnType} of the qualified networks.
     */
    public @ApnType int getApnType() {
        return mApnType;
    }

    /**
     * @return List of available networks in the preferred order. Each network is a
     * {@link AccessNetworkType}.
     */
    public int[] getNetworks() {
        return mNetworks;
    }

    /** @hide */
    @Override
    public int describeContents() {
        return 0;
    }

    /** @hide */
    @Override
    public String toString() {
        return "QualifiedNetworks:[apn=" + mApnType + ", networks:" + Arrays.toString(mNetworks)
                + "]";
    }

    /** @hide */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || !(o instanceof QualifiedNetworks)) {
            return false;
        }

        QualifiedNetworks other = (QualifiedNetworks) o;
        return this.mApnType == other.mApnType
                && Arrays.equals(this.mNetworks, other.mNetworks);
    }

    /** @hide */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mApnType);
        dest.writeIntArray(mNetworks);
    }

    /** @hide */
    public static final Parcelable.Creator<QualifiedNetworks> CREATOR =
            new Parcelable.Creator<QualifiedNetworks>() {
        @Override
        public QualifiedNetworks createFromParcel(Parcel source) {
            return new QualifiedNetworks(source);
        }

        @Override
        public QualifiedNetworks[] newArray(int size) {
            return new QualifiedNetworks[size];
        }
    };
}
