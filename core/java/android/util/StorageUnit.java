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

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
public enum StorageUnit {

    KILOBYTES(DataUnit.KILOBYTES, "K", "KB"),

    MEGABYTES(DataUnit.MEGABYTES, "M", "MB"),

    GIGABYTES(DataUnit.GIGABYTES, "G", "GB"),

    KIBIBYTES(DataUnit.KIBIBYTES, "Ki", "KiB"),

    MEBIBYTES(DataUnit.MEBIBYTES, "Mi", "MiB"),

    GIBIBYTES(DataUnit.GIBIBYTES, "Gi", "GiB");

    private static final Map<String, StorageUnit> STRING_TO_ENUM;

    static {
        STRING_TO_ENUM = new HashMap<>();
        for (StorageUnit su : values()) {
            for (String name : su.mNames) {
                STRING_TO_ENUM.put(name.toUpperCase(), su);
            }
        }
    }

    private final DataUnit mUnit;
    private final String[] mNames;

    StorageUnit(DataUnit unit, String...names) {
        this.mUnit = unit;
        this.mNames = names;
    }

    public DataUnit getDataUnit() {
        return mUnit;
    }

    @Override
    public String toString() {
        return Arrays.toString(mNames);
    }

    /**
     * @return {@code StorageUnit} that has the given {@code name}. If the given
     * {@code name} does not match any {@code mNames}, null is returned.
     */
    @Nullable
    public static StorageUnit fromName(@NonNull String name) {
        return STRING_TO_ENUM.get(name.toUpperCase());
    }
}

