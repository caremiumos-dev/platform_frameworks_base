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

import android.net.Uri;
import android.provider.Settings;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * SEEMP (security-enhanced event logging) helper used by the vendor to log
 * privacy-sensitive API usage through the native {@code seemp_println_native}.
 *
 * <p>WARNING / TRUST-MODEL CONDEMNATION: this silently reports calls into
 * settings, telephony, location, camera and sensor APIs to a vendor-owned
 * native logging channel. It is an opaque, device-side surveillance hook with
 * no user visibility or consent; it is reproduced only for device
 * compatibility.
 *
 * @hide
 */
public final class SeempLog {
    public static final int SEEMP_API_android_provider_Settings__get_ACCELEROMETER_ROTATION_ = 96;
    public static final int SEEMP_API_android_provider_Settings__get_ADB_ENABLED_ = 98;
    public static final int SEEMP_API_android_provider_Settings__get_AIRPLANE_MODE_ON_ = 101;
    public static final int SEEMP_API_android_provider_Settings__get_AIRPLANE_MODE_RADIOS_ = 102;
    public static final int SEEMP_API_android_provider_Settings__get_ALARM_ALERT_ = 103;
    public static final int SEEMP_API_android_provider_Settings__get_ALWAYS_FINISH_ACTIVITIES_ = 105;
    public static final int SEEMP_API_android_provider_Settings__get_ANDROID_ID_ = 7;
    public static final int SEEMP_API_android_provider_Settings__get_ANIMATOR_DURATION_SCALE_ = 107;
    public static final int SEEMP_API_android_provider_Settings__get_APPEND_FOR_LAST_AUDIBLE_ = 171;
    public static final int SEEMP_API_android_provider_Settings__get_AUTO_TIME_ = 123;
    public static final int SEEMP_API_android_provider_Settings__get_AUTO_TIME_ZONE_ = 124;
    public static final int SEEMP_API_android_provider_Settings__get_BLUETOOTH_DISCOVERABILITY_ = 127;
    public static final int SEEMP_API_android_provider_Settings__get_BLUETOOTH_DISCOVERABILITY_TIMEOUT_ = 128;
    public static final int SEEMP_API_android_provider_Settings__get_BLUETOOTH_ON_ = 129;
    public static final int SEEMP_API_android_provider_Settings__get_DATA_ROAMING_ = 142;
    public static final int SEEMP_API_android_provider_Settings__get_DATE_FORMAT_ = 125;
    public static final int SEEMP_API_android_provider_Settings__get_DEBUG_APP_ = 99;
    public static final int SEEMP_API_android_provider_Settings__get_DEVICE_PROVISIONED_ = 130;
    public static final int SEEMP_API_android_provider_Settings__get_DIM_SCREEN_ = 115;
    public static final int SEEMP_API_android_provider_Settings__get_DTMF_TONE_WHEN_DIALING_ = 132;
    public static final int SEEMP_API_android_provider_Settings__get_END_BUTTON_BEHAVIOR_ = 133;
    public static final int SEEMP_API_android_provider_Settings__get_FONT_SCALE_ = 109;
    public static final int SEEMP_API_android_provider_Settings__get_HAPTIC_FEEDBACK_ENABLED_ = 159;
    public static final int SEEMP_API_android_provider_Settings__get_HTTP_PROXY_ = 143;
    public static final int SEEMP_API_android_provider_Settings__get_INSTALL_NON_MARKET_APPS_ = 136;
    public static final int SEEMP_API_android_provider_Settings__get_LOCATION_PROVIDERS_ALLOWED_ = 137;
    public static final int SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_ENABLED_ = 138;
    public static final int SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED_ = 139;
    public static final int SEEMP_API_android_provider_Settings__get_LOCK_PATTERN_VISIBLE_ = 140;
    public static final int SEEMP_API_android_provider_Settings__get_LOGGING_ID_ = 106;
    public static final int SEEMP_API_android_provider_Settings__get_MODE_RINGER_ = 135;
    public static final int SEEMP_API_android_provider_Settings__get_MODE_RINGER_STREAMS_AFFECTED_ = 168;
    public static final int SEEMP_API_android_provider_Settings__get_MUTE_STREAMS_AFFECTED_ = 169;
    public static final int SEEMP_API_android_provider_Settings__get_NETWORK_PREFERENCE_ = 141;
    public static final int SEEMP_API_android_provider_Settings__get_NEXT_ALARM_FORMATTED_ = 104;
    public static final int SEEMP_API_android_provider_Settings__get_NOTIFICATION_SOUND_ = 170;
    public static final int SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_ENABLED_ = 144;
    public static final int SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_LAST_UPDATE_ = 145;
    public static final int SEEMP_API_android_provider_Settings__get_PARENTAL_CONTROL_REDIRECT_URL_ = 146;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_BLUETOOTH_ = 147;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_CELL_ = 148;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_NFC_ = 149;
    public static final int SEEMP_API_android_provider_Settings__get_RADIO_WIFI_ = 150;
    public static final int SEEMP_API_android_provider_Settings__get_RINGTONE_ = 134;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_ = 110;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_ = 111;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_AUTOMATIC_ = 112;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_BRIGHTNESS_MODE_MANUAL_ = 113;
    public static final int SEEMP_API_android_provider_Settings__get_SCREEN_OFF_TIMEOUT_ = 114;
    public static final int SEEMP_API_android_provider_Settings__get_SETTINGS_CLASSNAME_ = 152;
    public static final int SEEMP_API_android_provider_Settings__get_SETUP_WIZARD_HAS_RUN_ = 131;
    public static final int SEEMP_API_android_provider_Settings__get_SHOW_GTALK_SERVICE_STATUS_ = 121;
    public static final int SEEMP_API_android_provider_Settings__get_SHOW_PROCESSES_ = 119;
    public static final int SEEMP_API_android_provider_Settings__get_SHOW_WEB_SUGGESTIONS_ = 120;
    public static final int SEEMP_API_android_provider_Settings__get_SOUND_EFFECTS_ENABLED_ = 167;
    public static final int SEEMP_API_android_provider_Settings__get_STAY_ON_WHILE_PLUGGED_IN_ = 117;
    public static final int SEEMP_API_android_provider_Settings__get_SYS_PROP_SETTING_VERSION_ = 151;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_AUTO_CAPS_ = 153;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_AUTO_PUNCTUATE_ = 154;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_AUTO_REPLACE_ = 155;
    public static final int SEEMP_API_android_provider_Settings__get_TEXT_SHOW_PASSWORD_ = 156;
    public static final int SEEMP_API_android_provider_Settings__get_TIME_12_24_ = 126;
    public static final int SEEMP_API_android_provider_Settings__get_TRANSITION_ANIMATION_SCALE_ = 116;
    public static final int SEEMP_API_android_provider_Settings__get_USB_MASS_STORAGE_ENABLED_ = 157;
    public static final int SEEMP_API_android_provider_Settings__get_USER_ROTATION_ = 97;
    public static final int SEEMP_API_android_provider_Settings__get_USE_GOOGLE_MAIL_ = 122;
    public static final int SEEMP_API_android_provider_Settings__get_VIBRATE_ON_ = 158;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_ALARM_ = 160;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_BLUETOOTH_SCO_ = 161;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_MUSIC_ = 162;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_NOTIFICATION_ = 163;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_RING_ = 164;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_SYSTEM_ = 165;
    public static final int SEEMP_API_android_provider_Settings__get_VOLUME_VOICE_ = 166;
    public static final int SEEMP_API_android_provider_Settings__get_WAIT_FOR_DEBUGGER_ = 100;
    public static final int SEEMP_API_android_provider_Settings__get_WALLPAPER_ACTIVITY_ = 118;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_MAX_DHCP_RETRY_COUNT_ = 172;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS_ = 173;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON_ = 174;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY_ = 175;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_NUM_OPEN_NETWORKS_KEPT_ = 176;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_ON_ = 177;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_ = 178;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_DEFAULT_ = 179;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_NEVER_ = 180;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED_ = 181;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_DNS1_ = 182;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_DNS2_ = 183;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_GATEWAY_ = 184;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_IP_ = 185;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_STATIC_NETMASK_ = 186;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_USE_STATIC_IP_ = 187;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE_ = 188;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_AP_COUNT_ = 189;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS_ = 190;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED_ = 191;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS_ = 192;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT_ = 193;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_MAX_AP_CHECKS_ = 194;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_ON_ = 195;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_COUNT_ = 196;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_DELAY_MS_ = 197;
    public static final int SEEMP_API_android_provider_Settings__get_WIFI_WATCHDOG_PING_TIMEOUT_MS_ = 198;
    public static final int SEEMP_API_android_provider_Settings__get_WINDOW_ANIMATION_SCALE_ = 108;
    public static final int SEEMP_API_android_provider_Settings__put_ACCELEROMETER_ROTATION_ = 199;
    public static final int SEEMP_API_android_provider_Settings__put_ADB_ENABLED_ = 201;
    public static final int SEEMP_API_android_provider_Settings__put_AIRPLANE_MODE_ON_ = 204;
    public static final int SEEMP_API_android_provider_Settings__put_AIRPLANE_MODE_RADIOS_ = 205;
    public static final int SEEMP_API_android_provider_Settings__put_ALARM_ALERT_ = 206;
    public static final int SEEMP_API_android_provider_Settings__put_ALWAYS_FINISH_ACTIVITIES_ = 208;
    public static final int SEEMP_API_android_provider_Settings__put_ANDROID_ID_ = 209;
    public static final int SEEMP_API_android_provider_Settings__put_ANIMATOR_DURATION_SCALE_ = 211;
    public static final int SEEMP_API_android_provider_Settings__put_APPEND_FOR_LAST_AUDIBLE_ = 275;
    public static final int SEEMP_API_android_provider_Settings__put_AUTO_TIME_ = 227;
    public static final int SEEMP_API_android_provider_Settings__put_AUTO_TIME_ZONE_ = 228;
    public static final int SEEMP_API_android_provider_Settings__put_BLUETOOTH_DISCOVERABILITY_ = 231;
    public static final int SEEMP_API_android_provider_Settings__put_BLUETOOTH_DISCOVERABILITY_TIMEOUT_ = 232;
    public static final int SEEMP_API_android_provider_Settings__put_BLUETOOTH_ON_ = 233;
    public static final int SEEMP_API_android_provider_Settings__put_DATA_ROAMING_ = 246;
    public static final int SEEMP_API_android_provider_Settings__put_DATE_FORMAT_ = 229;
    public static final int SEEMP_API_android_provider_Settings__put_DEBUG_APP_ = 202;
    public static final int SEEMP_API_android_provider_Settings__put_DEVICE_PROVISIONED_ = 234;
    public static final int SEEMP_API_android_provider_Settings__put_DIM_SCREEN_ = 219;
    public static final int SEEMP_API_android_provider_Settings__put_DTMF_TONE_WHEN_DIALING_ = 236;
    public static final int SEEMP_API_android_provider_Settings__put_END_BUTTON_BEHAVIOR_ = 237;
    public static final int SEEMP_API_android_provider_Settings__put_FONT_SCALE_ = 213;
    public static final int SEEMP_API_android_provider_Settings__put_HAPTIC_FEEDBACK_ENABLED_ = 263;
    public static final int SEEMP_API_android_provider_Settings__put_HTTP_PROXY_ = 247;
    public static final int SEEMP_API_android_provider_Settings__put_INSTALL_NON_MARKET_APPS_ = 240;
    public static final int SEEMP_API_android_provider_Settings__put_LOCATION_PROVIDERS_ALLOWED_ = 241;
    public static final int SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_ENABLED_ = 242;
    public static final int SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_TACTILE_FEEDBACK_ENABLED_ = 243;
    public static final int SEEMP_API_android_provider_Settings__put_LOCK_PATTERN_VISIBLE_ = 244;
    public static final int SEEMP_API_android_provider_Settings__put_LOGGING_ID_ = 210;
    public static final int SEEMP_API_android_provider_Settings__put_MODE_RINGER_ = 239;
    public static final int SEEMP_API_android_provider_Settings__put_MODE_RINGER_STREAMS_AFFECTED_ = 272;
    public static final int SEEMP_API_android_provider_Settings__put_MUTE_STREAMS_AFFECTED_ = 273;
    public static final int SEEMP_API_android_provider_Settings__put_NETWORK_PREFERENCE_ = 245;
    public static final int SEEMP_API_android_provider_Settings__put_NEXT_ALARM_FORMATTED_ = 207;
    public static final int SEEMP_API_android_provider_Settings__put_NOTIFICATION_SOUND_ = 274;
    public static final int SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_ENABLED_ = 248;
    public static final int SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_LAST_UPDATE_ = 249;
    public static final int SEEMP_API_android_provider_Settings__put_PARENTAL_CONTROL_REDIRECT_URL_ = 250;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_BLUETOOTH_ = 251;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_CELL_ = 252;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_NFC_ = 253;
    public static final int SEEMP_API_android_provider_Settings__put_RADIO_WIFI_ = 254;
    public static final int SEEMP_API_android_provider_Settings__put_RINGTONE_ = 238;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_ = 214;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_ = 215;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_AUTOMATIC_ = 216;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_BRIGHTNESS_MODE_MANUAL_ = 217;
    public static final int SEEMP_API_android_provider_Settings__put_SCREEN_OFF_TIMEOUT_ = 218;
    public static final int SEEMP_API_android_provider_Settings__put_SETTINGS_CLASSNAME_ = 256;
    public static final int SEEMP_API_android_provider_Settings__put_SETUP_WIZARD_HAS_RUN_ = 235;
    public static final int SEEMP_API_android_provider_Settings__put_SHOW_GTALK_SERVICE_STATUS_ = 225;
    public static final int SEEMP_API_android_provider_Settings__put_SHOW_PROCESSES_ = 223;
    public static final int SEEMP_API_android_provider_Settings__put_SHOW_WEB_SUGGESTIONS_ = 224;
    public static final int SEEMP_API_android_provider_Settings__put_SOUND_EFFECTS_ENABLED_ = 271;
    public static final int SEEMP_API_android_provider_Settings__put_STAY_ON_WHILE_PLUGGED_IN_ = 221;
    public static final int SEEMP_API_android_provider_Settings__put_SYS_PROP_SETTING_VERSION_ = 255;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_AUTO_CAPS_ = 257;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_AUTO_PUNCTUATE_ = 258;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_AUTO_REPLACE_ = 259;
    public static final int SEEMP_API_android_provider_Settings__put_TEXT_SHOW_PASSWORD_ = 260;
    public static final int SEEMP_API_android_provider_Settings__put_TIME_12_24_ = 230;
    public static final int SEEMP_API_android_provider_Settings__put_TRANSITION_ANIMATION_SCALE_ = 220;
    public static final int SEEMP_API_android_provider_Settings__put_USB_MASS_STORAGE_ENABLED_ = 261;
    public static final int SEEMP_API_android_provider_Settings__put_USER_ROTATION_ = 200;
    public static final int SEEMP_API_android_provider_Settings__put_USE_GOOGLE_MAIL_ = 226;
    public static final int SEEMP_API_android_provider_Settings__put_VIBRATE_ON_ = 262;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_ALARM_ = 264;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_BLUETOOTH_SCO_ = 265;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_MUSIC_ = 266;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_NOTIFICATION_ = 267;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_RING_ = 268;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_SYSTEM_ = 269;
    public static final int SEEMP_API_android_provider_Settings__put_VOLUME_VOICE_ = 270;
    public static final int SEEMP_API_android_provider_Settings__put_WAIT_FOR_DEBUGGER_ = 203;
    public static final int SEEMP_API_android_provider_Settings__put_WALLPAPER_ACTIVITY_ = 222;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_MAX_DHCP_RETRY_COUNT_ = 276;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_MOBILE_DATA_TRANSITION_WAKELOCK_TIMEOUT_MS_ = 277;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_NETWORKS_AVAILABLE_NOTIFICATION_ON_ = 278;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_NETWORKS_AVAILABLE_REPEAT_DELAY_ = 279;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_NUM_OPEN_NETWORKS_KEPT_ = 280;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_ON_ = 281;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_ = 282;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_DEFAULT_ = 283;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_NEVER_ = 284;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_SLEEP_POLICY_NEVER_WHILE_PLUGGED_ = 285;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_DNS1_ = 286;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_DNS2_ = 287;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_GATEWAY_ = 288;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_IP_ = 289;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_STATIC_NETMASK_ = 290;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_USE_STATIC_IP_ = 291;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_ACCEPTABLE_PACKET_LOSS_PERCENTAGE_ = 292;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_AP_COUNT_ = 293;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_DELAY_MS_ = 294;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_ENABLED_ = 295;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_BACKGROUND_CHECK_TIMEOUT_MS_ = 296;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_INITIAL_IGNORED_PING_COUNT_ = 297;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_MAX_AP_CHECKS_ = 298;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_ON_ = 299;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_COUNT_ = 300;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_DELAY_MS_ = 301;
    public static final int SEEMP_API_android_provider_Settings__put_WIFI_WATCHDOG_PING_TIMEOUT_MS_ = 302;
    public static final int SEEMP_API_android_provider_Settings__put_WINDOW_ANIMATION_SCALE_ = 212;

    private static final Map<String, Integer> value_to_get_map = new HashMap<>(198);
    private static final Map<String, Integer> value_to_put_map = new HashMap<>(198);

    static {
        value_to_get_map.put(Settings.System.NOTIFICATION_SOUND, 170);
        value_to_get_map.put(Settings.System.DTMF_TONE_WHEN_DIALING, 132);
        value_to_get_map.put("lock_pattern_autolock", 138);
        value_to_get_map.put("wifi_max_dhcp_retry_count", 172);
        value_to_get_map.put("auto_time", 123);
        value_to_get_map.put(Settings.System.SETUP_WIZARD_HAS_RUN, 131);
        value_to_get_map.put("wifi_watchdog_background_check_timeout_ms", 192);
        value_to_get_map.put("location_providers_allowed", 137);
        value_to_get_map.put(Settings.System.ALARM_ALERT, 103);
        value_to_get_map.put(Settings.System.VIBRATE_ON, 158);
        value_to_get_map.put("usb_mass_storage_enabled", 157);
        value_to_get_map.put("wifi_watchdog_ping_delay_ms", 197);
        value_to_get_map.put(Settings.System.FONT_SCALE, 109);
        value_to_get_map.put("wifi_watchdog_ap_count", 189);
        value_to_get_map.put("always_finish_activities", 105);
        value_to_get_map.put(Settings.System.ACCELEROMETER_ROTATION, 96);
        value_to_get_map.put("wifi_watchdog_ping_timeout_ms", 198);
        value_to_get_map.put(Settings.System.VOLUME_NOTIFICATION, 163);
        value_to_get_map.put("airplane_mode_on", 101);
        value_to_get_map.put("wifi_watchdog_background_check_delay_ms", 190);
        value_to_get_map.put(Settings.System.WIFI_STATIC_IP, 185);
        value_to_get_map.put("bluetooth", 147);
        value_to_get_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT, 128);
        value_to_get_map.put(Settings.System.VOLUME_RING, 164);
        value_to_get_map.put(Settings.System.MODE_RINGER_STREAMS_AFFECTED, 168);
        value_to_get_map.put(Settings.System.VOLUME_SYSTEM, 165);
        value_to_get_map.put(Settings.System.SCREEN_OFF_TIMEOUT, 114);
        value_to_get_map.put("wifi", 150);
        value_to_get_map.put("auto_time_zone", 124);
        value_to_get_map.put(Settings.System.TEXT_AUTO_CAPS, 153);
        value_to_get_map.put(Settings.System.WALLPAPER_ACTIVITY, 118);
        value_to_get_map.put("animator_duration_scale", 107);
        value_to_get_map.put("wifi_num_open_networks_kept", 176);
        value_to_get_map.put("lock_pattern_visible_pattern", 140);
        value_to_get_map.put(Settings.System.VOLUME_VOICE, 166);
        value_to_get_map.put("debug_app", 99);
        value_to_get_map.put("wifi_on", 177);
        value_to_get_map.put(Settings.System.TEXT_SHOW_PASSWORD, 156);
        value_to_get_map.put("wifi_networks_available_repeat_delay", 175);
        value_to_get_map.put("wifi_sleep_policy", 178);
        value_to_get_map.put(Settings.System.VOLUME_MUSIC, 162);
        value_to_get_map.put("parental_control_last_update", 145);
        value_to_get_map.put("device_provisioned", 130);
        value_to_get_map.put("http_proxy", 143);
        value_to_get_map.put("android_id", 7);
        value_to_get_map.put("wifi_watchdog_max_ap_checks", 194);
        value_to_get_map.put(Settings.System.END_BUTTON_BEHAVIOR, 133);
        value_to_get_map.put(Settings.System.NEXT_ALARM_FORMATTED, 104);
        value_to_get_map.put("cell", 148);
        value_to_get_map.put("parental_control_enabled", 144);
        value_to_get_map.put("bluetooth_on", 129);
        value_to_get_map.put("window_animation_scale", 108);
        value_to_get_map.put("wifi_watchdog_background_check_enabled", 191);
        value_to_get_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY, 127);
        value_to_get_map.put(Settings.System.WIFI_STATIC_DNS1, 182);
        value_to_get_map.put(Settings.System.WIFI_STATIC_DNS2, 183);
        value_to_get_map.put(Settings.System.HAPTIC_FEEDBACK_ENABLED, 159);
        value_to_get_map.put(Settings.System.SHOW_WEB_SUGGESTIONS, 120);
        value_to_get_map.put("parental_control_redirect_url", 146);
        value_to_get_map.put(Settings.System.DATE_FORMAT, 125);
        value_to_get_map.put("nfc", 149);
        value_to_get_map.put("airplane_mode_radios", 102);
        value_to_get_map.put("lock_pattern_tactile_feedback_enabled", 139);
        value_to_get_map.put(Settings.System.TIME_12_24, 126);
        value_to_get_map.put("wifi_watchdog_initial_ignored_ping_count", 193);
        value_to_get_map.put(Settings.System.VOLUME_BLUETOOTH_SCO, 161);
        value_to_get_map.put(Settings.System.USER_ROTATION, 97);
        value_to_get_map.put(Settings.System.WIFI_STATIC_GATEWAY, 184);
        value_to_get_map.put("stay_on_while_plugged_in", 117);
        value_to_get_map.put(Settings.System.SOUND_EFFECTS_ENABLED, 167);
        value_to_get_map.put("wifi_watchdog_ping_count", 196);
        value_to_get_map.put("data_roaming", 142);
        value_to_get_map.put("settings_classname", 152);
        value_to_get_map.put("transition_animation_scale", 116);
        value_to_get_map.put("wait_for_debugger", 100);
        value_to_get_map.put("install_non_market_apps", 136);
        value_to_get_map.put("adb_enabled", 98);
        value_to_get_map.put(Settings.System.WIFI_USE_STATIC_IP, 187);
        value_to_get_map.put(Settings.System.DIM_SCREEN, 115);
        value_to_get_map.put(Settings.System.VOLUME_ALARM, 160);
        value_to_get_map.put("wifi_watchdog_on", 195);
        value_to_get_map.put(Settings.System.WIFI_STATIC_NETMASK, 186);
        value_to_get_map.put("network_preference", 141);
        value_to_get_map.put("show_processes", 119);
        value_to_get_map.put(Settings.System.TEXT_AUTO_REPLACE, 155);
        value_to_get_map.put("wifi_networks_available_notification_on", 174);
        value_to_get_map.put(Settings.System.APPEND_FOR_LAST_AUDIBLE, 171);
        value_to_get_map.put(Settings.System.SHOW_GTALK_SERVICE_STATUS, 121);
        value_to_get_map.put(Settings.System.SCREEN_BRIGHTNESS, 110);
        value_to_get_map.put("use_google_mail", 122);
        value_to_get_map.put(Settings.System.RINGTONE, 134);
        value_to_get_map.put("logging_id", 106);
        value_to_get_map.put("mode_ringer", 135);
        value_to_get_map.put(Settings.System.MUTE_STREAMS_AFFECTED, 169);
        value_to_get_map.put("wifi_watchdog_acceptable_packet_loss_percentage", 188);
        value_to_get_map.put(Settings.System.TEXT_AUTO_PUNCTUATE, 154);
        value_to_get_map.put("wifi_mobile_data_transition_wakelock_timeout_ms", 173);
        value_to_get_map.put(Settings.System.SCREEN_BRIGHTNESS_MODE, 111);

        value_to_put_map.put(Settings.System.NOTIFICATION_SOUND, 274);
        value_to_put_map.put(Settings.System.DTMF_TONE_WHEN_DIALING, 236);
        value_to_put_map.put("lock_pattern_autolock", 242);
        value_to_put_map.put("wifi_max_dhcp_retry_count", 276);
        value_to_put_map.put("auto_time", 227);
        value_to_put_map.put(Settings.System.SETUP_WIZARD_HAS_RUN, 235);
        value_to_put_map.put("wifi_watchdog_background_check_timeout_ms", 296);
        value_to_put_map.put("location_providers_allowed", 241);
        value_to_put_map.put(Settings.System.ALARM_ALERT, 206);
        value_to_put_map.put(Settings.System.VIBRATE_ON, 262);
        value_to_put_map.put("usb_mass_storage_enabled", 261);
        value_to_put_map.put("wifi_watchdog_ping_delay_ms", 301);
        value_to_put_map.put(Settings.System.FONT_SCALE, 213);
        value_to_put_map.put("wifi_watchdog_ap_count", 293);
        value_to_put_map.put("always_finish_activities", 208);
        value_to_put_map.put(Settings.System.ACCELEROMETER_ROTATION, 199);
        value_to_put_map.put("wifi_watchdog_ping_timeout_ms", 302);
        value_to_put_map.put(Settings.System.VOLUME_NOTIFICATION, 267);
        value_to_put_map.put("airplane_mode_on", 204);
        value_to_put_map.put("wifi_watchdog_background_check_delay_ms", 294);
        value_to_put_map.put(Settings.System.WIFI_STATIC_IP, 289);
        value_to_put_map.put("bluetooth", 251);
        value_to_put_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY_TIMEOUT, 232);
        value_to_put_map.put(Settings.System.VOLUME_RING, 268);
        value_to_put_map.put(Settings.System.MODE_RINGER_STREAMS_AFFECTED, 272);
        value_to_put_map.put(Settings.System.VOLUME_SYSTEM, 269);
        value_to_put_map.put(Settings.System.SCREEN_OFF_TIMEOUT, 218);
        value_to_put_map.put("wifi", 254);
        value_to_put_map.put("auto_time_zone", 228);
        value_to_put_map.put(Settings.System.TEXT_AUTO_CAPS, 257);
        value_to_put_map.put(Settings.System.WALLPAPER_ACTIVITY, 222);
        value_to_put_map.put("animator_duration_scale", 211);
        value_to_put_map.put("wifi_num_open_networks_kept", 280);
        value_to_put_map.put("lock_pattern_visible_pattern", 244);
        value_to_put_map.put(Settings.System.VOLUME_VOICE, 270);
        value_to_put_map.put("debug_app", 202);
        value_to_put_map.put("wifi_on", 281);
        value_to_put_map.put(Settings.System.TEXT_SHOW_PASSWORD, 260);
        value_to_put_map.put("wifi_networks_available_repeat_delay", 279);
        value_to_put_map.put("wifi_sleep_policy", 282);
        value_to_put_map.put(Settings.System.VOLUME_MUSIC, 266);
        value_to_put_map.put("parental_control_last_update", 249);
        value_to_put_map.put("device_provisioned", 234);
        value_to_put_map.put("http_proxy", 247);
        value_to_put_map.put("android_id", 209);
        value_to_put_map.put("wifi_watchdog_max_ap_checks", 298);
        value_to_put_map.put(Settings.System.END_BUTTON_BEHAVIOR, 237);
        value_to_put_map.put(Settings.System.NEXT_ALARM_FORMATTED, 207);
        value_to_put_map.put("cell", 252);
        value_to_put_map.put("parental_control_enabled", 248);
        value_to_put_map.put("bluetooth_on", 233);
        value_to_put_map.put("window_animation_scale", 212);
        value_to_put_map.put("wifi_watchdog_background_check_enabled", 295);
        value_to_put_map.put(Settings.System.BLUETOOTH_DISCOVERABILITY, 231);
        value_to_put_map.put(Settings.System.WIFI_STATIC_DNS1, 286);
        value_to_put_map.put(Settings.System.WIFI_STATIC_DNS2, 287);
        value_to_put_map.put(Settings.System.HAPTIC_FEEDBACK_ENABLED, 263);
        value_to_put_map.put(Settings.System.SHOW_WEB_SUGGESTIONS, 224);
        value_to_put_map.put("parental_control_redirect_url", 250);
        value_to_put_map.put(Settings.System.DATE_FORMAT, 229);
        value_to_put_map.put("nfc", 253);
        value_to_put_map.put("airplane_mode_radios", 205);
        value_to_put_map.put("lock_pattern_tactile_feedback_enabled", 243);
        value_to_put_map.put(Settings.System.TIME_12_24, 230);
        value_to_put_map.put("wifi_watchdog_initial_ignored_ping_count", 297);
        value_to_put_map.put(Settings.System.VOLUME_BLUETOOTH_SCO, 265);
        value_to_put_map.put(Settings.System.USER_ROTATION, 200);
        value_to_put_map.put(Settings.System.WIFI_STATIC_GATEWAY, 288);
        value_to_put_map.put("stay_on_while_plugged_in", 221);
        value_to_put_map.put(Settings.System.SOUND_EFFECTS_ENABLED, 271);
        value_to_put_map.put("wifi_watchdog_ping_count", 300);
        value_to_put_map.put("data_roaming", 246);
        value_to_put_map.put("settings_classname", 256);
        value_to_put_map.put("transition_animation_scale", 220);
        value_to_put_map.put("wait_for_debugger", 203);
        value_to_put_map.put("install_non_market_apps", 240);
        value_to_put_map.put("adb_enabled", 201);
        value_to_put_map.put(Settings.System.WIFI_USE_STATIC_IP, 291);
        value_to_put_map.put(Settings.System.DIM_SCREEN, 219);
        value_to_put_map.put(Settings.System.VOLUME_ALARM, 264);
        value_to_put_map.put("wifi_watchdog_on", 299);
        value_to_put_map.put(Settings.System.WIFI_STATIC_NETMASK, 290);
        value_to_put_map.put("network_preference", 245);
        value_to_put_map.put("show_processes", 223);
        value_to_put_map.put(Settings.System.TEXT_AUTO_REPLACE, 259);
        value_to_put_map.put("wifi_networks_available_notification_on", 278);
        value_to_put_map.put(Settings.System.APPEND_FOR_LAST_AUDIBLE, 275);
        value_to_put_map.put(Settings.System.SHOW_GTALK_SERVICE_STATUS, 225);
        value_to_put_map.put(Settings.System.SCREEN_BRIGHTNESS, 214);
        value_to_put_map.put("use_google_mail", 226);
        value_to_put_map.put(Settings.System.RINGTONE, 238);
        value_to_put_map.put("logging_id", 210);
        value_to_put_map.put("mode_ringer", 239);
        value_to_put_map.put(Settings.System.MUTE_STREAMS_AFFECTED, 273);
        value_to_put_map.put("wifi_watchdog_acceptable_packet_loss_percentage", 292);
        value_to_put_map.put(Settings.System.TEXT_AUTO_PUNCTUATE, 258);
        value_to_put_map.put("wifi_mobile_data_transition_wakelock_timeout_ms", 277);
        value_to_put_map.put(Settings.System.SCREEN_BRIGHTNESS_MODE, 215);
    }

    private SeempLog() {
    }

    public static int getSeempGetApiIdFromValue(String value) {
        Integer id = value_to_get_map.get(value);
        return id == null ? -1 : id;
    }

    public static int getSeempPutApiIdFromValue(String value) {
        Integer id = value_to_put_map.get(value);
        return id == null ? -1 : id;
    }

    public static int record(int api) {
        return seemp_println_native(api, "");
    }

    public static int record_sensor(int api, android.hardware.Sensor sensor) {
        return sensor != null
                ? seemp_println_native(api, "sensor=" + sensor.getType())
                : seemp_println_native(api, "sensor=-1");
    }

    public static int record_sensor_rate(int api, android.hardware.Sensor sensor, int rate) {
        return sensor != null
                ? seemp_println_native(api, "sensor=" + sensor.getType() + ",rate=" + rate)
                : seemp_println_native(api, "sensor=-1,rate=" + rate);
    }

    public static int record_str(int api, String value) {
        return value != null ? seemp_println_native(api, value) : seemp_println_native(api, "");
    }

    public static int record_uri(int api, Uri uri) {
        return uri != null
                ? seemp_println_native(api, "uri, " + uri.toString())
                : seemp_println_native(api, "uri, null");
    }

    public static int record_vg_layout(int api, ViewGroup.LayoutParams params) {
        try {
            android.view.WindowManager.LayoutParams lp =
                    (android.view.WindowManager.LayoutParams) params;
            return lp != null
                    ? seemp_println_native(api,
                            "window_type=" + lp.type + ",window_flag=" + lp.flags)
                    : seemp_println_native(api, "");
        } catch (ClassCastException e) {
            return seemp_println_native(api, "");
        }
    }

    public static native int seemp_println_native(int api, String msg);
}