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

package com.xtc.xtcdiag.socket;

/**
 * Result of a single self-test item.
 *
 * @hide
 */
public enum ItemTestResult {
    TEST_NOT_SUPPORT(-1),
    TEST_FAIL(0),
    TEST_PASS(1),
    TEST_UPDATE_DATA(2),
    TEST_NEED_CONFIRM(3);

    private int mResult;

    ItemTestResult(int result) {
        this.mResult = result;
    }

    public int value() {
        return mResult;
    }
}