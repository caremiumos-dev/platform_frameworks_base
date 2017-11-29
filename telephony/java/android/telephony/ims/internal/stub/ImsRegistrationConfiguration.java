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
 * limitations under the License
 */

package android.telephony.ims.internal.stub;

import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.ims.internal.feature.ImsFeature;
import android.util.ArraySet;

import java.util.Arrays;
import java.util.Set;

/**
 * Container class for IMS registration configuration. This class contains three data types:
 * 1) features that the ImsService supports, which are defined in {@link ImsFeature},
 * 2) features that the framework supports and the ImsService should register for, and
 * 2) Additional "carrier" features, which are nonstandard registration strings needed for
 * carrier specific features.
 * @hide
 */
public class ImsRegistrationConfiguration implements Parcelable {
    /**
     * Features that this ImsService supports.
     */
    private final Set<Integer> mFeatures;
    /**
     * Features that the platform supports for Registration purposes that this ImsService does not
     * support.
     */
    private final Set<Integer> mExternalFeatures;
    /**
     * A list of custom carrier feature strings that are carrier specific and are needed for
     * Registration.
     */
    private final Set<String> mCarrierFeatures;

    /**
     * Creates an ImsRegistrationConfiguration with the features
     */
    public static class Builder {
            ImsRegistrationConfiguration mConfig;
        public Builder() {
            mConfig = new ImsRegistrationConfiguration();
        }

        /**
         * @param feature A feature defined in {@link ImsFeature} that this service supports.
         * @return a {@link Builder} to continue constructing the ImsRegistrationConfiguration.
         */
        public Builder addFeature(int feature) {
            mConfig.addFeature(feature);
            return this;
        }
        /**
         * @param feature A feature defined in {@link ImsFeature} that this service supports.
         * @return a {@link Builder} to continue constructing the ImsRegistrationConfiguration.
         */
        public Builder addExternalFeature(int feature) {
            mConfig.addExternalFeature(feature);
            return this;
        }
        public Builder addCarrierSpecificFeature(String feature) {
            mConfig.addCarrierSpecificFeature(feature);
            return this;
        }
        public ImsRegistrationConfiguration build() {
            return mConfig;
        }
    }

    /**
     * Creates with all registration features empty. Use the {@link Builder} to add features to this
     * structure.
     */
    public ImsRegistrationConfiguration() {
        mFeatures = new ArraySet<>();
        mExternalFeatures = new ArraySet<>();
        mCarrierFeatures = new ArraySet<>();
    }

    /**
     * Configuration of the ImsService, which describes which features the ImsService supports
     * (for registration).
     * @param features an array of feature integers defined in {@link ImsFeature} that describe
     * which features this ImsService supports.
     */
    public ImsRegistrationConfiguration(int[] features) {
        mFeatures = new ArraySet<>();
        mExternalFeatures = new ArraySet<>();
        mCarrierFeatures = new ArraySet<>();

        if (features != null) {
            for (int i : features) {
                mFeatures.add(i);
            }
        }
    }

    /**
     * @return an int[] containing all of the features that this ImsService needs to register for.
     *
     * May include features that this ImsService does not support, but must still register for, in
     * order to support single IMS registration on a Carrier's network.
     */
    public int[] getRegistrationFeatures() {
        ArraySet<Integer> combinedFeatures = new ArraySet<>();
        combinedFeatures.addAll(mFeatures);
        combinedFeatures.addAll(mExternalFeatures);
        return combinedFeatures.stream().mapToInt(i->i).toArray();
    }

    /**
     * @return a String[] containing all of the feature tags that the ImsService should include in
     * registration that are not included in the standard feature registration.
     */
    public String[] getCarrierSpecificRegistrationFeatures() {
        return mCarrierFeatures.toArray(new String[mCarrierFeatures.size()]);
    }

    void addFeature(int feature) {
        mFeatures.add(feature);
    }

    void addCarrierSpecificFeature(String feature) {
        mCarrierFeatures.add(feature);
    }

    void addExternalFeature(int feature) {
        mExternalFeatures.add(feature);
    }

    protected ImsRegistrationConfiguration(Parcel in) {
        int[] features = in.createIntArray();
        if (features != null) {
            mFeatures = new ArraySet<>(features.length);
            for(Integer i : features) {
                mFeatures.add(i);
            }
        } else {
            mFeatures = new ArraySet<>();
        }

        int[] externalFeatures = in.createIntArray();
        if (externalFeatures != null) {
            mExternalFeatures = new ArraySet<>(externalFeatures.length);
            for(Integer i : externalFeatures) {
                mExternalFeatures.add(i);
            }
        } else {
            mExternalFeatures = new ArraySet<>();
        }

        String[] carrierFeatures = in.readStringArray();
        if (carrierFeatures != null) {
            mCarrierFeatures = new ArraySet<>(carrierFeatures.length);
            mCarrierFeatures.addAll(Arrays.asList(carrierFeatures));
        } else {
            mCarrierFeatures = new ArraySet<>();
        }
    }

    public static final Creator<ImsRegistrationConfiguration> CREATOR
            = new Creator<ImsRegistrationConfiguration>() {
        @Override
        public ImsRegistrationConfiguration createFromParcel(Parcel in) {
            return new ImsRegistrationConfiguration(in);
        }

        @Override
        public ImsRegistrationConfiguration[] newArray(int size) {
            return new ImsRegistrationConfiguration[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeIntArray(mFeatures.stream().mapToInt(i->i).toArray());
        dest.writeIntArray(mExternalFeatures.stream().mapToInt(i->i).toArray());
        dest.writeStringArray(mCarrierFeatures.toArray(new String[mCarrierFeatures.size()]));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImsRegistrationConfiguration)) return false;

        ImsRegistrationConfiguration that = (ImsRegistrationConfiguration) o;

        if (!mFeatures.equals(that.mFeatures)) return false;
        if (!mExternalFeatures.equals(that.mExternalFeatures)) return false;
        return mCarrierFeatures.equals(that.mCarrierFeatures);
    }

    @Override
    public int hashCode() {
        int result = mFeatures.hashCode();
        result = 31 * result + mExternalFeatures.hashCode();
        result = 31 * result + mCarrierFeatures.hashCode();
        return result;
    }
}
