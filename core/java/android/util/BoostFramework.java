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

package android.util;

import android.os.SystemProperties;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Vendor performance hint wrapper. It reflectively binds to the Qualcomm
 * {@code com.qualcomm.qti.Performance} class and is a no-op when unavailable.
 *
 * @hide
 */
public class BoostFramework {
    private static final String PERFORMANCE_CLASS = "com.qualcomm.qti.Performance";
    private static final String PERFORMANCE_JAR = "/system/framework/QPerformance.jar";
    private static final String TAG = "BoostFramework";

    public static final int VENDOR_HINT_ACTIVITY_BOOST = 4228;
    public static final int VENDOR_HINT_ANIM_BOOST = 4227;
    public static final int VENDOR_HINT_DRAG_BOOST = 4231;
    public static final int VENDOR_HINT_FIRST_DRAW = 4162;
    public static final int VENDOR_HINT_FIRST_LAUNCH_BOOST = 4225;
    public static final int VENDOR_HINT_MTP_BOOST = 4230;
    public static final int VENDOR_HINT_PACKAGE_INSTALL_BOOST = 4232;
    public static final int VENDOR_HINT_SCROLL_BOOST = 4224;
    public static final int VENDOR_HINT_SUBSEQ_LAUNCH_BOOST = 4226;
    public static final int VENDOR_HINT_TAP_EVENT = 4163;
    public static final int VENDOR_HINT_TOUCH_BOOST = 4229;

    private Object mPerf;
    private static int mIopv2 = SystemProperties.getInt("iop.enable_uxe", 0);
    private static boolean mIsLoaded = false;
    private static Class<?> mPerfClass = null;
    private static Method mAcquireFunc = null;
    private static Method mPerfHintFunc = null;
    private static Method mReleaseFunc = null;
    private static Method mReleaseHandlerFunc = null;
    private static Constructor<Class> mConstructor = null;
    private static Method mIOPStart = null;
    private static Method mIOPStop = null;
    private static Method mUXEngine_events = null;
    private static Method mUXEngine_trigger = null;

    public class Draw {
        public static final int EVENT_TYPE_V1 = 1;
    }

    public class Launch {
        public static final int BOOST_V1 = 1;
        public static final int BOOST_V2 = 2;
        public static final int BOOST_V3 = 3;
        public static final int TYPE_SERVICE_START = 100;
    }

    public class Scroll {
        public static final int HORIZONTAL = 2;
        public static final int PANEL_VIEW = 3;
        public static final int PREFILING = 4;
        public static final int VERTICAL = 1;
    }

    public BoostFramework() {
        this.mPerf = null;
        synchronized (BoostFramework.class) {
            if (!mIsLoaded) {
                try {
                    mPerfClass = Class.forName(PERFORMANCE_CLASS);
                    mAcquireFunc = mPerfClass.getMethod("perfLockAcquire", Integer.TYPE,
                            int[].class);
                    mPerfHintFunc = mPerfClass.getMethod("perfHint", Integer.TYPE, String.class,
                            Integer.TYPE, Integer.TYPE);
                    mReleaseFunc = mPerfClass.getMethod("perfLockRelease", new Class[0]);
                    mReleaseHandlerFunc = mPerfClass.getDeclaredMethod("perfLockReleaseHandler",
                            Integer.TYPE);
                    if (mIopv2 == 1) {
                        mIOPStart = mPerfClass.getDeclaredMethod("perfIOPrefetchStart",
                                Integer.TYPE, String.class, String.class);
                        mIOPStop = mPerfClass.getDeclaredMethod("perfIOPrefetchStop",
                                new Class[0]);
                        mUXEngine_events = mPerfClass.getDeclaredMethod("perfUXEngine_events",
                                Integer.TYPE, Integer.TYPE, String.class, Integer.TYPE);
                        mUXEngine_trigger = mPerfClass.getDeclaredMethod("perfUXEngine_trigger",
                                Integer.TYPE);
                    }
                    mIsLoaded = true;
                } catch (Exception e) {
                    Log.e(TAG, "BoostFramework() : Exception_1 = " + e);
                }
            }
        }
        try {
            if (mPerfClass != null) {
                this.mPerf = mPerfClass.newInstance();
            }
        } catch (Exception e2) {
            Log.e(TAG, "BoostFramework() : Exception_2 = " + e2);
        }
    }

    public int perfHint(int hint, String userData) {
        return perfHint(hint, userData, -1, -1);
    }

    public int perfHint(int hint, String userData, int userData1) {
        return perfHint(hint, userData, userData1, -1);
    }

    public int perfHint(int hint, String userData, int userData1, int userData2) {
        try {
            return ((Integer) mPerfHintFunc.invoke(this.mPerf, Integer.valueOf(hint), userData,
                    Integer.valueOf(userData1), Integer.valueOf(userData2))).intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public int perfIOPrefetchStart(int pid, String pkgName, String codePath) {
        try {
            if (mIopv2 == 0 || mIOPStart == null) {
                return -1;
            }
            return ((Integer) mIOPStart.invoke(this.mPerf, Integer.valueOf(pid), pkgName,
                    codePath)).intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public int perfIOPrefetchStop() {
        try {
            if (mIopv2 == 0 || mIOPStop == null) {
                return -1;
            }
            return ((Integer) mIOPStop.invoke(this.mPerf, new Object[0])).intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public int perfLockAcquire(int duration, int... list) {
        try {
            return ((Integer) mAcquireFunc.invoke(this.mPerf, Integer.valueOf(duration), list))
                    .intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public int perfLockRelease() {
        try {
            return ((Integer) mReleaseFunc.invoke(this.mPerf, new Object[0])).intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public int perfLockReleaseHandler(int handle) {
        try {
            return ((Integer) mReleaseHandlerFunc.invoke(this.mPerf, Integer.valueOf(handle)))
                    .intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public int perfUXEngine_events(int opcode, int pid, String pkgName, int value) {
        try {
            if (mIopv2 == 0 || mUXEngine_events == null) {
                return -1;
            }
            return ((Integer) mUXEngine_events.invoke(this.mPerf, Integer.valueOf(opcode),
                    Integer.valueOf(pid), pkgName, Integer.valueOf(value))).intValue();
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return -1;
        }
    }

    public String perfUXEngine_trigger(int opcode) {
        try {
            if (mIopv2 == 0 || mUXEngine_trigger == null) {
                return null;
            }
            return (String) mUXEngine_trigger.invoke(this.mPerf, Integer.valueOf(opcode));
        } catch (Exception e) {
            Log.e(TAG, "Exception " + e);
            return null;
        }
    }
}