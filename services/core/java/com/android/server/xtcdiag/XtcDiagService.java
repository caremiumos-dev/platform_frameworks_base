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

package com.android.server.xtcdiag;

import android.content.Context;
import android.text.TextUtils;
import android.util.Slog;

import com.android.server.SystemService;

/**
 * System service hosting the XTC diag (socket diag) service.
 *
 * @hide
 */
public class XtcDiagService extends SystemService {
    private static final boolean DEBUG = true;
    private static final String TAG = "XtcDiagService";

    private final XtcDiagServiceImpl mImpl;

    public XtcDiagService(Context context) {
        super(context);
        logd("XtcDiagService init");
        mImpl = new XtcDiagServiceImpl(context);
    }

    private void logd(String message) {
        if (!TextUtils.isEmpty(message)) {
            Slog.d(TAG, message);
        }
    }

    @Override
    public void onStart() {
        logd("XtcDiagService onStart ");
        publishBinderService("xtcdiag", mImpl);
    }

    public void systemReady() {
        logd("XtcDiagService systemReady");
        mImpl.systemReady();
    }
}