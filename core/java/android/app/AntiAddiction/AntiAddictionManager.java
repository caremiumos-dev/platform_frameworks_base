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

package android.app.AntiAddiction;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * Manager for the anti-addiction service (audio duration statistics).
 *
 * @hide
 */
public class AntiAddictionManager {
    private static final String TAG = "AntiAddictionManager";

    final Context mContext;
    private final IAntiAddictionManager mService;

    public AntiAddictionManager(Context context, IAntiAddictionManager service) {
        mContext = context;
        mService = service;
    }

    public void statisticalAudioDuration(int startTime, int duration) {
        try {
            mService.statisticalAudioDuration(startTime, duration);
        } catch (RemoteException e) {
            Log.e(TAG, "statisticalAudioDuration got an exception");
            e.printStackTrace();
        }
    }

    public void registerAddictionCallback(IAddictionCallback callback) {
        try {
            mService.registerAddictionCallback(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "registerAddictionCallback got an exception");
            e.printStackTrace();
        }
    }

    public void unregisterAddictionCallback(IAddictionCallback callback) {
        try {
            mService.unregisterAddictionCallback(callback);
        } catch (RemoteException e) {
            Log.e(TAG, "unregisterAddictionCallback got an exception");
            e.printStackTrace();
        }
    }
}