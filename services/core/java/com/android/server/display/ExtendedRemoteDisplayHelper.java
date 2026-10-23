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

package com.android.server.display;

import android.content.Context;
import android.media.RemoteDisplay;
import android.os.Handler;
import android.util.Slog;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Reflective bridge to the Qualcomm extended remote display implementation, if
 * it is present on the device.
 *
 * @hide
 */
class ExtendedRemoteDisplayHelper {
    private static final String TAG = "ExtendedRemoteDisplayHelper";

    private static Class sExtRemoteDisplayClass;
    private static Method sExtRemoteDisplayListen;
    private static Method sExtRemoteDisplayDispose;

    static {
        try {
            sExtRemoteDisplayClass = Class.forName("com.qualcomm.wfd.ExtendedRemoteDisplay");
        } catch (Throwable t) {
            Slog.i(TAG, "ExtendedRemoteDisplay Not available.");
        }
        if (sExtRemoteDisplayClass != null) {
            Slog.i(TAG, "ExtendedRemoteDisplay Is available. Find Methods");
            try {
                sExtRemoteDisplayListen = sExtRemoteDisplayClass.getDeclaredMethod("listen",
                        String.class, RemoteDisplay.Listener.class, Handler.class, Context.class);
            } catch (Throwable t) {
                Slog.i(TAG, "ExtendedRemoteDisplay.listen Not available.");
            }
            try {
                sExtRemoteDisplayDispose = sExtRemoteDisplayClass.getDeclaredMethod("dispose");
            } catch (Throwable t) {
                Slog.i(TAG, "ExtendedRemoteDisplay.dispose Not available.");
            }
        }
    }

    ExtendedRemoteDisplayHelper() {
    }

    public static boolean isAvailable() {
        if (sExtRemoteDisplayClass == null || sExtRemoteDisplayDispose == null
                || sExtRemoteDisplayListen == null) {
            Slog.i(TAG, "ExtendedRemoteDisplay isAvailable() : Not Available.");
            return false;
        }
        Slog.i(TAG, "ExtendedRemoteDisplay isAvailable() : Available.");
        return true;
    }

    public static Object listen(String iface, RemoteDisplay.Listener listener, Handler handler,
            Context context) {
        Slog.i(TAG, "ExtendedRemoteDisplay.listen");
        if (sExtRemoteDisplayListen == null || sExtRemoteDisplayDispose == null) {
            return null;
        }
        try {
            return sExtRemoteDisplayListen.invoke(null, iface, listener, handler, context);
        } catch (IllegalAccessException e) {
            Slog.i(TAG, "ExtendedRemoteDisplay.listen -IllegalAccessException");
            e.printStackTrace();
            return null;
        } catch (InvocationTargetException e) {
            Slog.i(TAG, "ExtendedRemoteDisplay.listen - InvocationTargetException");
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(e);
        }
    }

    public static void dispose(Object display) {
        Slog.i(TAG, "ExtendedRemoteDisplay.dispose");
        try {
            sExtRemoteDisplayDispose.invoke(display);
        } catch (IllegalAccessException e) {
            Slog.i(TAG, "ExtendedRemoteDisplay.dispose-IllegalAccessException");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            Slog.i(TAG, "ExtendedRemoteDisplay.dispose - InvocationTargetException");
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(e);
        }
    }
}