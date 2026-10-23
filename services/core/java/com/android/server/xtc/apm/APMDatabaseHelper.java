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

package com.android.server.xtc.apm;

/**
 * Schema and column names for the XTC application performance monitor database.
 *
 * @hide
 */
public class APMDatabaseHelper {
    public static final String APM_DATA_TABLE = "apm_data";
    public static final String COLUMN_ID = "_id";
    public static final String CONFIGURATION_TABLE = "apm_configuration";

    public static final String CREATE_APM_DATA_TABLE =
            "CREATE TABLE IF NOT EXISTS apm_data (timestamp INTEGER, year INTEGER, month INTEGER, "
                    + "day INTEGER, hour INTEGER, minute INTEGER, second INTEGER, package_name TEXT, "
                    + "type TEXT, json_data MEDIUMTEXT, uploaded INTEGER DEFAULT 0,_id INTEGER "
                    + "PRIMARY KEY AUTOINCREMENT);";
    public static final String CREATE_CONFIGURATION_TABLE =
            "CREATE TABLE IF NOT EXISTS apm_configuration (package_name TEXT, "
                    + "ui_monitor_enable INTEGER NOT NULL DEFAULT 1, evil_method_tracer_enable "
                    + "INTEGER NOT NULL DEFAULT 1, frame_tracer_enable INTEGER NOT NULL DEFAULT 1, "
                    + "startup_tracer_enable INTEGER NOT NULL DEFAULT 1, battery_monitor_enable "
                    + "INTEGER NOT NULL DEFAULT 1, method_cost_time_threshold INTEGER, "
                    + "drop_frame_threshold INTEGER, activity_launch_time_threshold INTEGER, "
                    + "sql_monitor_enable INTEGER NOT NULL DEFAULT 1, "
                    + "main_thread_sql_cost_time_threshold INTEGER, _id INTEGER PRIMARY KEY "
                    + "AUTOINCREMENT);";

    public static final String DB_NAME = "apm.db";
    public static final int DB_VERSION = 1;

    public static final String COLUMN_PKG_NAME = "package_name";
    public static final String COLUMN_UI_MONITOR_ENABLE = "ui_monitor_enable";
    public static final String COLUMN_EVIL_METHOD_TRACER_ENABLE = "evil_method_tracer_enable";
    public static final String COLUMN_FRAME_TRACER_ENABLE = "frame_tracer_enable";
    public static final String COLUMN_STARTUP_TRACER_ENABLE = "startup_tracer_enable";
    public static final String COLUMN_BATTERY_MONITOR_ENABLE = "battery_monitor_enable";
    public static final String COLUMN_METHOD_COST_TIME_THRESHOLD = "method_cost_time_threshold";
    public static final String COLUMN_DROP_FRAME_THRESHOLD = "drop_frame_threshold";
    public static final String COLUMN_ACTIVITY_LAUNCH_TIME_THRESHOLD =
            "activity_launch_time_threshold";
    public static final String COLUMN_SQL_MONITOR_ENABLE = "sql_monitor_enable";
    public static final String COLUMN_MAIN_THREAD_SQL_COST_TIME_THRESHOLD =
            "main_thread_sql_cost_time_threshold";
    public static final String[] CONFIGURATION_TABLE_COLUMNS = {
            COLUMN_PKG_NAME, COLUMN_UI_MONITOR_ENABLE, COLUMN_EVIL_METHOD_TRACER_ENABLE,
            COLUMN_FRAME_TRACER_ENABLE, COLUMN_STARTUP_TRACER_ENABLE,
            COLUMN_BATTERY_MONITOR_ENABLE, COLUMN_METHOD_COST_TIME_THRESHOLD,
            COLUMN_DROP_FRAME_THRESHOLD, COLUMN_ACTIVITY_LAUNCH_TIME_THRESHOLD,
            COLUMN_SQL_MONITOR_ENABLE, COLUMN_MAIN_THREAD_SQL_COST_TIME_THRESHOLD};

    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_YEAR = "year";
    public static final String COLUMN_MONTH = "month";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_HOUR = "hour";
    public static final String COLUMN_MINUTE = "minute";
    public static final String COLUMN_SECOND = "second";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_JSON_DATA = "json_data";
    public static final String COLUMN_UPLOADED = "uploaded";
    public static final String[] APM_DATA_TABLE_COLUMNS = {
            COLUMN_TIMESTAMP, COLUMN_YEAR, COLUMN_MONTH, COLUMN_DAY, COLUMN_HOUR, COLUMN_MINUTE,
            COLUMN_SECOND, COLUMN_PKG_NAME, COLUMN_TYPE, COLUMN_JSON_DATA, COLUMN_UPLOADED};

    public class APMDataTableIndex {
        public static final int COLUMN_TIMESTAMP = 0;
        public static final int COLUMN_YEAR = 1;
        public static final int COLUMN_MONTH = 2;
        public static final int COLUMN_DAY = 3;
        public static final int COLUMN_HOUR = 4;
        public static final int COLUMN_MINUTE = 5;
        public static final int COLUMN_SECOND = 6;
        public static final int COLUMN_PKG_NAME = 7;
        public static final int COLUMN_TYPE = 8;
        public static final int COLUMN_JSON_DATA = 9;
        public static final int COLUMN_UPLOADED = 10;
    }

    public class ConfigurationTableIndex {
        public static final int COLUMN_PKG_NAME = 0;
        public static final int COLUMN_UI_MONITOR_ENABLE = 1;
        public static final int COLUMN_EVIL_METHOD_TRACER_ENABLE = 2;
        public static final int COLUMN_FRAME_TRACER_ENABLE = 3;
        public static final int COLUMN_STARTUP_TRACER_ENABLE = 4;
        public static final int COLUMN_BATTERY_MONITOR_ENABLE = 5;
        public static final int COLUMN_METHOD_COST_TIME_THRESHOLD = 6;
        public static final int COLUMN_DROP_FRAME_THRESHOLD = 7;
        public static final int COLUMN_ACTIVITY_LAUNCH_TIME_THRESHOLD = 8;
        public static final int COLUMN_SQL_MONITOR_ENABLE = 9;
        public static final int COLUMN_MAIN_THREAD_SQL_COST_TIME_THRESHOLD = 10;
    }
}