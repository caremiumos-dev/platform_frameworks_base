/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.bluetooth;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents the Quality of Service (QoS) settings for a Bluetooth HID Device
 * application.
 *
 * The BluetoothHidDevice framework will update the L2CAP QoS settings for the
 * app during registration.
 *
 * {@see BluetoothHidDevice}
 *
 * {@hide}
 */
public final class BluetoothHidDeviceAppQosSettings implements Parcelable {

    public final int serviceType;
    public final int tokenRate;
    public final int tokenBucketSize;
    public final int peakBandwidth;
    public final int latency;
    public final int delayVariation;

    public static final int SERVICE_NO_TRAFFIC = 0x00;
    public static final int SERVICE_BEST_EFFORT = 0x01;
    public static final int SERVICE_GUARANTEED = 0x02;

    public static final int MAX = (int) 0xffffffff;

    public BluetoothHidDeviceAppQosSettings(int serviceType, int tokenRate, int tokenBucketSize,
            int peakBandwidth, int latency, int delayVariation) {
        this.serviceType = serviceType;
        this.tokenRate = tokenRate;
        this.tokenBucketSize = tokenBucketSize;
        this.peakBandwidth = peakBandwidth;
        this.latency = latency;
        this.delayVariation = delayVariation;
    }

    private BluetoothHidDeviceAppQosSettings(Builder builder) {
        this.serviceType = builder.mServiceType;
        this.tokenRate = builder.mTokenRate;
        this.tokenBucketSize = builder.mTokenBucketSize;
        this.peakBandwidth = builder.mPeakBandwidth;
        this.latency = builder.mLatency;
        this.delayVariation = builder.mDelayVariation;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BluetoothHidDeviceAppQosSettings) {
            BluetoothHidDeviceAppQosSettings qos = (BluetoothHidDeviceAppQosSettings) o;
            return false;
        }
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<BluetoothHidDeviceAppQosSettings> CREATOR =
            new Parcelable.Creator<BluetoothHidDeviceAppQosSettings>() {

                @Override
                public BluetoothHidDeviceAppQosSettings createFromParcel(Parcel in) {

                    return new BluetoothHidDeviceAppQosSettings(
                            in.readInt(),
                            in.readInt(),
                            in.readInt(),
                            in.readInt(),
                            in.readInt(),
                            in.readInt());
                }

                @Override
                public BluetoothHidDeviceAppQosSettings[] newArray(int size) {
                    return new BluetoothHidDeviceAppQosSettings[size];
                }
            };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(serviceType);
        out.writeInt(tokenRate);
        out.writeInt(tokenBucketSize);
        out.writeInt(peakBandwidth);
        out.writeInt(latency);
        out.writeInt(delayVariation);
    }

    /** @return an int array representation of this instance */
    public int[] toArray() {
        return new int[] {
                serviceType, tokenRate, tokenBucketSize, peakBandwidth, latency, delayVariation
        };
    }

    /**
     * A helper to build the BluetoothHidDeviceAppQosSettings object.
     */
    public static class Builder {
        // Required parameter
        private final int mServiceType;

        // Optional parameters - initialized to default values
        private int mTokenRate = 0;
        private int mTokenBucketSize = 0;
        private int mPeakBandwidth = 0;
        private int mLatency = 0;
        private int mDelayVariation = 0;

        public Builder(int serviceType) {
            this.mServiceType = serviceType;
        }

        /**
         * Set the token rate.
         * @param val token rate
         * @return BluetoothHidDeviceAppQosSettings Builder with specified token rate.
         */
        public Builder tokenRate(int val) {
            mTokenRate = val;
            return this;
        }

        /**
         * Set the bucket size.
         * @param val bucket size
         * @return BluetoothHidDeviceAppQosSettings Builder with specified bucket size.
         */
        public Builder tokenBucketSize(int val) {
            mTokenBucketSize = val;
            return this;
        }

        /**
         * Set the peak bandwidth.
         * @param val peak bandwidth
         * @return BluetoothHidDeviceAppQosSettings Builder with specified peak bandwidth.
         */
        public Builder peakBandwidth(int val) {
            mPeakBandwidth = val;
            return this;
        }
        /**
         * Set the latency.
         * @param val latency
         * @return BluetoothHidDeviceAppQosSettings Builder with specified latency.
         */
        public Builder latency(int val) {
            mLatency = val;
            return this;
        }

        /**
         * Set the delay variation.
         * @param val delay variation
         * @return BluetoothHidDeviceAppQosSettings Builder with specified delay variation.
         */
        public Builder delayVariation(int val) {
            mDelayVariation = val;
            return this;
        }

        /**
         * Build the BluetoothHidDeviceAppQosSettings object.
         * @return BluetoothHidDeviceAppQosSettings object with current settings.
         */
        public BluetoothHidDeviceAppQosSettings build() {
            return new BluetoothHidDeviceAppQosSettings(this);
        }
    }
}
