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

package com.android.server.vcn;

/**
 * SafemodeListener is a callback to be used for signalling when a VCN component has entered
 * Safemode.
 */
public interface SafemodeListener {
    /**
     * Called when a VCN component must signal that it has entered Safemode.
     *
     * <p>Upon receiving this signal, the recipient should update the corresponding VCN as needed.
     * This may include tearing down the component that initiated the signal, or simply marking the
     * VCN as inactive until updated {@link android.net.vcn.VcnConfig}s are provided.
     */
    void onEnteredSafemode();
}
