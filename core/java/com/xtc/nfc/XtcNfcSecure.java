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
 * limitations under the License.
 */

package com.xtc.nfc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Security helpers guarding the XTC NFC SDK from being used by third party
 * packages.
 *
 * @hide
 */
public class XtcNfcSecure {
    private static final List<String> nfcPackages = new CopyOnWriteArrayList<>();

    static {
        nfcPackages.add("com.xtc.nfc.nfcdemo");
        nfcPackages.add("com.xtc.cardpack");
        nfcPackages.add("com.xtc.selftest");
    }

    public static boolean check(String pkg) {
        return nfcPackages.contains(pkg);
    }

    public static void enforceUserPerm(String pkg) {
        if (!check(pkg)) {
            throw new SecurityException(
                    "only the xtc package can access the xtc-nfc-sdk APIs. your package: " + pkg);
        }
    }
}