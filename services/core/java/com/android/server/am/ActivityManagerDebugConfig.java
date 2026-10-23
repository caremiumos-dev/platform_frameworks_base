/*
 * Copyright (C) 2014 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.server.am;

/**
 * Common class for the various debug {@link android.util.Log} output configuration in the activity
 * manager package.
 */
class ActivityManagerDebugConfig {

    // All output logs in the activity manager package use the {@link #TAG_AM} string for tagging
    // their log output. This makes it easy to identify the origin of the log message when sifting
    // through a large amount of log output from multiple sources. However, it also makes trying
    // to figure-out the origin of a log message while debugging the activity manager a little
    // painful. By setting this constant to true, log messages from the activity manager package
    // will be tagged with their class names instead fot the generic tag.
    static final boolean TAG_WITH_CLASS_NAME = false;

    // While debugging it is sometimes useful to have the category name of the log appended to the
    // base log tag to make sifting through logs with the same base tag easier. By setting this
    // constant to true, the category name of the log point will be appended to the log tag.
    static final boolean APPEND_CATEGORY_NAME = false;

    // Default log tag for the activity manager package.
    static final String TAG_AM = "ActivityManager";

    // Enable all debug log categories.
    // XTC 差异：这些开关由 `dumpsys activity -d` 在运行时动态修改，因此不能再是 final 常量。
    static boolean DEBUG_ALL = false;

    // Enable all debug log categories for activities.
    static final boolean DEBUG_ALL_ACTIVITIES;

    // Available log categories in the activity manager package.
    static boolean DEBUG_ADD_REMOVE = false;
    static boolean DEBUG_ANR = false;
    static boolean DEBUG_APP = false;
    static boolean DEBUG_BACKGROUND_CHECK = false;
    static boolean DEBUG_BACKUP = false;
    static boolean DEBUG_BROADCAST = false;
    static boolean DEBUG_BROADCAST_BACKGROUND = false;
    static boolean DEBUG_BROADCAST_LIGHT = false;
    static boolean DEBUG_CLEANUP = false;
    static boolean DEBUG_CONFIGURATION = false;
    static boolean DEBUG_CONTAINERS = false;
    static boolean DEBUG_FOCUS = false;
    static boolean DEBUG_IDLE = false;
    static boolean DEBUG_IMMERSIVE = false;
    static boolean DEBUG_LOCKSCREEN = false;
    static boolean DEBUG_LOCKTASK = false;
    static boolean DEBUG_LRU = false;
    static boolean DEBUG_MU = false;
    static boolean DEBUG_NETWORK = false;
    static boolean DEBUG_OOM_ADJ = false;
    static boolean DEBUG_OOM_ADJ_REASON = false;
    static boolean DEBUG_PAUSE = false;
    static boolean DEBUG_POWER = false;
    static boolean DEBUG_POWER_QUICK = false;
    static boolean DEBUG_PROCESS_OBSERVERS = false;
    static boolean DEBUG_PROCESSES = false;
    static boolean DEBUG_PROVIDER = false;
    static boolean DEBUG_PSS = false;
    static boolean DEBUG_RECENTS = false;
    static boolean DEBUG_RELEASE = false;
    static boolean DEBUG_RESULTS = false;
    static boolean DEBUG_SAVED_STATE = false;
    static boolean DEBUG_SCREENSHOTS = false;
    static boolean DEBUG_SERVICE = false;
    static boolean DEBUG_FOREGROUND_SERVICE = false;
    static boolean DEBUG_SERVICE_EXECUTING = false;
    static boolean DEBUG_STACK = false;
    static boolean DEBUG_STATES = false;
    static boolean DEBUG_SWITCH = false;
    static boolean DEBUG_TASKS = false;
    static boolean DEBUG_THUMBNAILS = false;
    static boolean DEBUG_TRANSITION = false;
    // XTC 差异：ActivityMetricsLogger 的过渡日志开关。
    static boolean DEBUG_METRICS = false;
    static boolean DEBUG_UID_OBSERVERS = false;
    static boolean DEBUG_URI_PERMISSION = false;
    static boolean DEBUG_USER_LEAVING = false;
    static boolean DEBUG_VISIBILITY = false;
    static boolean DEBUG_USAGE_STATS = false;
    static boolean DEBUG_PERMISSIONS_REVIEW = false;
    static boolean DEBUG_WHITELISTS = false;

    static {
        DEBUG_ALL_ACTIVITIES = DEBUG_ALL;
        DEBUG_ADD_REMOVE = DEBUG_ALL_ACTIVITIES;
        DEBUG_APP = DEBUG_ALL_ACTIVITIES;
        DEBUG_BACKGROUND_CHECK = DEBUG_ALL;
        DEBUG_BACKUP = DEBUG_ALL;
        DEBUG_BROADCAST = DEBUG_ALL;
        DEBUG_BROADCAST_BACKGROUND = DEBUG_BROADCAST;
        DEBUG_BROADCAST_LIGHT = DEBUG_BROADCAST;
        DEBUG_CLEANUP = DEBUG_ALL;
        DEBUG_CONFIGURATION = DEBUG_ALL;
        DEBUG_CONTAINERS = DEBUG_ALL_ACTIVITIES;
        DEBUG_IDLE = DEBUG_ALL_ACTIVITIES;
        DEBUG_IMMERSIVE = DEBUG_ALL;
        DEBUG_LOCKSCREEN = DEBUG_ALL;
        DEBUG_LOCKTASK = DEBUG_ALL;
        DEBUG_LRU = DEBUG_ALL;
        DEBUG_MU = DEBUG_ALL;
        DEBUG_NETWORK = DEBUG_ALL;
        DEBUG_OOM_ADJ = DEBUG_ALL;
        DEBUG_OOM_ADJ_REASON = DEBUG_ALL;
        DEBUG_PAUSE = DEBUG_ALL;
        DEBUG_POWER = DEBUG_ALL;
        DEBUG_POWER_QUICK = DEBUG_POWER;
        DEBUG_PROCESS_OBSERVERS = DEBUG_ALL;
        DEBUG_PROCESSES = DEBUG_ALL;
        DEBUG_PROVIDER = DEBUG_ALL;
        DEBUG_PSS = DEBUG_ALL;
        DEBUG_RECENTS = DEBUG_ALL;
        DEBUG_RELEASE = DEBUG_ALL_ACTIVITIES;
        DEBUG_RESULTS = DEBUG_ALL;
        DEBUG_SAVED_STATE = DEBUG_ALL_ACTIVITIES;
        DEBUG_SCREENSHOTS = DEBUG_ALL_ACTIVITIES;
        DEBUG_SERVICE = DEBUG_ALL;
        DEBUG_FOREGROUND_SERVICE = DEBUG_ALL;
        DEBUG_SERVICE_EXECUTING = DEBUG_ALL;
        DEBUG_STACK = DEBUG_ALL;
        DEBUG_STATES = DEBUG_ALL_ACTIVITIES;
        DEBUG_SWITCH = DEBUG_ALL;
        DEBUG_TASKS = DEBUG_ALL;
        DEBUG_THUMBNAILS = DEBUG_ALL;
        DEBUG_TRANSITION = DEBUG_ALL;
        DEBUG_UID_OBSERVERS = DEBUG_ALL;
        DEBUG_URI_PERMISSION = DEBUG_ALL;
        DEBUG_USER_LEAVING = DEBUG_ALL;
        DEBUG_VISIBILITY = DEBUG_ALL;
        DEBUG_USAGE_STATS = DEBUG_ALL;
        DEBUG_PERMISSIONS_REVIEW = DEBUG_ALL;
        DEBUG_WHITELISTS = DEBUG_ALL;
        DEBUG_METRICS = DEBUG_ALL;
    }

    static final String POSTFIX_ADD_REMOVE = (APPEND_CATEGORY_NAME) ? "_AddRemove" : "";
    static final String POSTFIX_APP = (APPEND_CATEGORY_NAME) ? "_App" : "";
    static final String POSTFIX_BACKUP = (APPEND_CATEGORY_NAME) ? "_Backup" : "";
    static final String POSTFIX_BROADCAST = (APPEND_CATEGORY_NAME) ? "_Broadcast" : "";
    static final String POSTFIX_CLEANUP = (APPEND_CATEGORY_NAME) ? "_Cleanup" : "";
    static final String POSTFIX_CONFIGURATION = (APPEND_CATEGORY_NAME) ? "_Configuration" : "";
    static final String POSTFIX_CONTAINERS = (APPEND_CATEGORY_NAME) ? "_Containers" : "";
    static final String POSTFIX_FOCUS = (APPEND_CATEGORY_NAME) ? "_Focus" : "";
    static final String POSTFIX_IDLE = (APPEND_CATEGORY_NAME) ? "_Idle" : "";
    static final String POSTFIX_IMMERSIVE = (APPEND_CATEGORY_NAME) ? "_Immersive" : "";
    static final String POSTFIX_LOCKSCREEN = (APPEND_CATEGORY_NAME) ? "_LockScreen" : "";
    static final String POSTFIX_LOCKTASK = (APPEND_CATEGORY_NAME) ? "_LockTask" : "";
    static final String POSTFIX_LRU = (APPEND_CATEGORY_NAME) ? "_LRU" : "";
    static final String POSTFIX_MU = "_MU";
    static final String POSTFIX_NETWORK = "_Network";
    static final String POSTFIX_OOM_ADJ = (APPEND_CATEGORY_NAME) ? "_OomAdj" : "";
    static final String POSTFIX_PAUSE = (APPEND_CATEGORY_NAME) ? "_Pause" : "";
    static final String POSTFIX_POWER = (APPEND_CATEGORY_NAME) ? "_Power" : "";
    static final String POSTFIX_PROCESS_OBSERVERS = (APPEND_CATEGORY_NAME)
            ? "_ProcessObservers" : "";
    static final String POSTFIX_PROCESSES = (APPEND_CATEGORY_NAME) ? "_Processes" : "";
    static final String POSTFIX_PROVIDER = (APPEND_CATEGORY_NAME) ? "_Provider" : "";
    static final String POSTFIX_PSS = (APPEND_CATEGORY_NAME) ? "_Pss" : "";
    static final String POSTFIX_RECENTS = (APPEND_CATEGORY_NAME) ? "_Recents" : "";
    static final String POSTFIX_RELEASE = (APPEND_CATEGORY_NAME) ? "_Release" : "";
    static final String POSTFIX_RESULTS = (APPEND_CATEGORY_NAME) ? "_Results" : "";
    static final String POSTFIX_SAVED_STATE = (APPEND_CATEGORY_NAME) ? "_SavedState" : "";
    static final String POSTFIX_SCREENSHOTS = (APPEND_CATEGORY_NAME) ? "_Screenshots" : "";
    static final String POSTFIX_SERVICE = (APPEND_CATEGORY_NAME) ? "_Service" : "";
    static final String POSTFIX_SERVICE_EXECUTING =
            (APPEND_CATEGORY_NAME) ? "_ServiceExecuting" : "";
    static final String POSTFIX_STACK = (APPEND_CATEGORY_NAME) ? "_Stack" : "";
    static final String POSTFIX_STATES = (APPEND_CATEGORY_NAME) ? "_States" : "";
    static final String POSTFIX_SWITCH = (APPEND_CATEGORY_NAME) ? "_Switch" : "";
    static final String POSTFIX_TASKS = (APPEND_CATEGORY_NAME) ? "_Tasks" : "";
    static final String POSTFIX_THUMBNAILS = (APPEND_CATEGORY_NAME) ? "_Thumbnails" : "";
    static final String POSTFIX_TRANSITION = (APPEND_CATEGORY_NAME) ? "_Transition" : "";
    static final String POSTFIX_UID_OBSERVERS = (APPEND_CATEGORY_NAME)
            ? "_UidObservers" : "";
    static final String POSTFIX_URI_PERMISSION = (APPEND_CATEGORY_NAME) ? "_UriPermission" : "";
    static final String POSTFIX_USER_LEAVING = (APPEND_CATEGORY_NAME) ? "_UserLeaving" : "";
    static final String POSTFIX_VISIBILITY = (APPEND_CATEGORY_NAME) ? "_Visibility" : "";
    static final String POSTFIX_VISIBLE_BEHIND = (APPEND_CATEGORY_NAME) ? "_VisibleBehind" : "";

}
