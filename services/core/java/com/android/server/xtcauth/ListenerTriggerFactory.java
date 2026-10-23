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

package com.android.server.xtcauth;

import android.os.Bundle;
import android.os.IBinder;

/**
 * Creates the listener trigger matching a controller id.
 *
 * @hide
 */
class ListenerTriggerFactory {
    ListenerTriggerFactory() {
    }

    public static ListenerTriggerBase createAsyncTrigger(int controllerID, int callingPid,
            IBinder listener, Bundle params, int defaultResult, IBinder callback) {
        switch (controllerID) {
            case PermissionController.ID:
                return new PermissionListenerTrigger(controllerID, callingPid, listener, params,
                        defaultResult, callback);
            default:
                throw new RuntimeException("Invalid controller ID" + controllerID);
        }
    }

    public static ListenerTriggerBase createTrigger(int controllerID, int callingPid,
            IBinder listener, Bundle params, int defaultResult) {
        switch (controllerID) {
            case PermissionController.ID:
                return new PermissionListenerTrigger(controllerID, callingPid, listener, params,
                        defaultResult);
            case 3:
                return new NotificationListenerTrigger(controllerID, callingPid, listener, params,
                        defaultResult);
            case 4:
            case 5:
                return new MessageInterceptListenerTrigger(controllerID, callingPid, listener,
                        params, defaultResult);
            case 6:
                return new CallInterceptionListenerTrigger(controllerID, callingPid, listener,
                        params, defaultResult);
            case 1:
            case 2:
            default:
                throw new RuntimeException("Invalid controller ID" + controllerID);
        }
    }
}