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

package android.util;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * A {@code DataUnit} represents data sizes at a given unit of granularity and provides utility
 * methods to convert across units.
 *
 * <p>Note that both SI units (powers of 10) and IEC units (powers of 2) are supported, and you'll
 * need to pick the correct one for your use-case. For example, Wikipedia defines a "kilobyte" as an
 * SI unit of 1000 bytes, and a "kibibyte" as an IEC unit of 1024 bytes.
 *
 * <p>This design is mirrored after {@link TimeUnit} and {@link ChronoUnit}.
 *
 * @hide
 */
public enum DataUnit {
    KILOBYTES("KB") {
        @Override
        public long toBytes(long v) {
            return v * 1_000;
        }
    },
    MEGABYTES("MB") {
        @Override
        public long toBytes(long v) {
            return v * 1_000_000;
        }
    },
    GIGABYTES("GB") {
        @Override
        public long toBytes(long v) {
            return v * 1_000_000_000;
        }
    },
    KIBIBYTES("KiB") {
        @Override
        public long toBytes(long v) {
            return v * 1_024;
        }
    },
    MEBIBYTES("MiB") {
        @Override
        public long toBytes(long v) {
            return v * 1_048_576;
        }
    },
    GIBIBYTES("GiB") {
        @Override
        public long toBytes(long v) {
            return v * 1_073_741_824;
        }
    };

    private final String mShortName;

    DataUnit(String shortName) {
        this.mShortName = shortName;
    }

    /**
     * Convert {@code v} to bytes.
     */
    public long toBytes(long v) {
        throw new AbstractMethodError();
    }

    public String getShortName() {
        return mShortName;
    }
}
