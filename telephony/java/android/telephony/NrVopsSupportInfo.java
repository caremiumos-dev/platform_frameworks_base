/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.telephony;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.telephony.AccessNetworkConstants.AccessNetworkType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Class stores information related to NR network VoPS support
 * @hide
 */
@SystemApi
public final class NrVopsSupportInfo extends VopsSupportInfo {

    /**
     * Indicates information not available from modem.
     */
    public static final int NR_STATUS_NOT_AVAILABLE = -1;

    /**
     * Indicates network does not support the feature.
     */
    public static final int NR_STATUS_NOT_SUPPORTED = 0;

    /**
     * Indicates network supports vops over 3gpp access.
     */
    public static final int NR_STATUS_VOPS_3GPP_SUPPORTED = 1;

    /**
     * Indicates network supports vops over non 3gpp access
     */
    public static final int NR_STATUS_VOPS_NON_3GPP_SUPPORTED = 2;

    /**@hide*/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = {"NR_STATUS_"},
        value = {
            NR_STATUS_NOT_AVAILABLE,
            NR_STATUS_NOT_SUPPORTED,
            NR_STATUS_VOPS_3GPP_SUPPORTED,
            NR_STATUS_VOPS_NON_3GPP_SUPPORTED
        })
    public @interface NrVopsStatus {}

    /**
     * Indicates network supports emergency service or emergency service fallback
     * in NR connected to 5GCN only
     */
    public static final int NR_STATUS_5GCN_ONLY = 1;

    /**
     * Indicates network supports emergency service or emergency service fallback
     * in E-UTRA connected to 5GCN only
     */
    public static final int NR_STATUS_EUTRA_5GCN_ONLY = 2;

    /**
     * Indicates network supports emergency service or emergency service fallback
     * in NR connected to 5GCN and E-UTRA connected to 5GCN
     */
    public static final int NR_STATUS_NR_EUTRA_5GCN = 3;

    /**@hide*/
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
        prefix = {"NR_STATUS_"},
        value = {
            NR_STATUS_NOT_AVAILABLE,
            NR_STATUS_NOT_SUPPORTED,
            NR_STATUS_5GCN_ONLY,
            NR_STATUS_EUTRA_5GCN_ONLY,
            NR_STATUS_NR_EUTRA_5GCN
        })
    public @interface NrEmcStatus {}

    @NrVopsStatus
    private final int mVopsSupport;
    @NrEmcStatus
    private final int mEmcSupport;
    @NrEmcStatus
    private final int mEmfSupport;

    public NrVopsSupportInfo(@NrVopsStatus int vops, @NrEmcStatus int emc, @NrEmcStatus int emf) {
        mVopsSupport = vops;
        mEmcSupport = emc;
        mEmfSupport = emf;
    }

    /**
     * Provides the NR VoPS support capability as described in:
     * 3GPP 24.501 EPS network feature support -> IMS VoPS
     */
    @Override
    public @NrVopsStatus int getVopsSupport() {
        return mVopsSupport;
    }

    /**
     * Provides the NR Emergency bearer support capability as described in:
     * 3GPP 24.501 EPS network feature support -> EMC, and
     * 38.331 SIB1 : ims-EmergencySupport
     */
    @Override
    public @NrEmcStatus int getEmcSupport() {
        return mEmcSupport;
    }

    /**
     * Provides the NR emergency service support capability as described in:
     * 3GPP 24.501 EPS network feature support -> EMF
     */
    public @NrEmcStatus int getEmfSupport() {
        return mEmfSupport;
    }

    /**
     * Returns whether VoPS is supported by the network
     */
    @Override
    public boolean isVopsSupported() {
        return mVopsSupport == NR_STATUS_VOPS_3GPP_SUPPORTED
                || mVopsSupport == NR_STATUS_VOPS_NON_3GPP_SUPPORTED;
    }

    /**
     * Returns whether emergency service is supported by the network
     */
    @Override
    public boolean isEmcSupported() {
        return mEmcSupport == NR_STATUS_5GCN_ONLY
                || mEmcSupport == NR_STATUS_EUTRA_5GCN_ONLY
                || mEmcSupport == NR_STATUS_NR_EUTRA_5GCN;
    }

    /**
     * Returns whether emergency service fallback is supported by the network
     */
    public boolean isEmfSupported() {
        return mEmfSupport == NR_STATUS_5GCN_ONLY
                || mEmfSupport == NR_STATUS_EUTRA_5GCN_ONLY
                || mEmfSupport == NR_STATUS_NR_EUTRA_5GCN;
    }

    /**
     * Implement the Parcelable interface
     */
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel out, int flags) {
        super.writeToParcel(out, flags, AccessNetworkType.NGRAN);
        out.writeInt(mVopsSupport);
        out.writeInt(mEmcSupport);
        out.writeInt(mEmfSupport);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || !(o instanceof NrVopsSupportInfo)) {
            return false;
        }
        if (this == o) return true;
        NrVopsSupportInfo other = (NrVopsSupportInfo) o;
        return mVopsSupport == other.mVopsSupport
            && mEmcSupport == other.mEmcSupport
            && mEmfSupport == other.mEmfSupport;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mVopsSupport, mEmcSupport, mEmfSupport);
    }

    /**
     * @return string representation.
     */
    @NonNull
    @Override
    public String toString() {
        return ("NrVopsSupportInfo : "
                + " mVopsSupport = " + mVopsSupport
                + " mEmcSupport = " + mEmcSupport
                + " mEmfSupport = " + mEmfSupport);
    }

    public static final @android.annotation.NonNull Creator<NrVopsSupportInfo> CREATOR =
            new Creator<NrVopsSupportInfo>() {
        @Override
        public NrVopsSupportInfo createFromParcel(Parcel in) {
            // Skip the type info.
            in.readInt();
            return createFromParcelBody(in);
        }

        @Override
        public NrVopsSupportInfo[] newArray(int size) {
            return new NrVopsSupportInfo[size];
        }
    };

    /** @hide */
    protected static NrVopsSupportInfo createFromParcelBody(Parcel in) {
        return new NrVopsSupportInfo(in);
    }

    private NrVopsSupportInfo(Parcel in) {
        mVopsSupport = in.readInt();
        mEmcSupport = in.readInt();
        mEmfSupport = in.readInt();
    }
}
