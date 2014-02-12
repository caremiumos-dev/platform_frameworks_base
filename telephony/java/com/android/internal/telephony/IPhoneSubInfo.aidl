/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.telephony;

/**
 * Interface used to retrieve various phone-related subscriber information.
 *
 */
interface IPhoneSubInfo {

    /**
     * Retrieves the unique device ID, e.g., IMEI for GSM phones.
     */
    String getDeviceId();

    /**
     * Retrieves the unique device ID of a subscription for the device, e.g., IMEI
     * for GSM phones.
     */
    String getDeviceIdUsingSub(int subscription);

    /**
     * Retrieves the software version number for the device, e.g., IMEI/SV
     * for GSM phones.
     */
    String getDeviceSvn();

    /**
     * Retrieves the unique sbuscriber ID, e.g., IMSI for GSM phones.
     */
    String getSubscriberId();

    /**
     * Retrieves the unique subscriber ID of a given subscription, e.g., IMSI for GSM phones.
     */
    String getSubscriberIdUsingSub(int subscription);

    /**
     * Retrieves the Group Identifier Level1 for GSM phones.
     */
    String getGroupIdLevel1();

    /**
     * Retrieves the Group Identifier Level1 for GSM phones of a subscription.
     */
    String getGroupIdLevel1UsingSub(int subscription);

    /**
     * Retrieves the serial number of the ICC, if applicable.
     */
    String getIccSerialNumber();

    /**
     * Retrieves the serial number of a given subscription.
     */
    String getIccSerialNumberUsingSub(int subscription);

    /**
     * Retrieves the phone number string for line 1.
     */
    String getLine1Number();

    /**
     * Retrieves the phone number string for line 1 of a subcription.
     */
    String getLine1NumberUsingSub(int subscription);


    /**
     * Retrieves the alpha identifier for line 1.
     */
    String getLine1AlphaTag();

    /**
     * Retrieves the alpha identifier for line 1 of a subscription.
     */
    String getLine1AlphaTagUsingSub(int subscription);


    /**
     * Retrieves MSISDN Number.
     */
    String getMsisdn();

    /**
     * Retrieves the Msisdn of a subscription.
     */
    String getMsisdnUsingSub(int subscription);

    /**
     * Retrieves the voice mail number.
     */
    String getVoiceMailNumber();

    /**
     * Retrieves the voice mail number of a given subscription.
     */
    String getVoiceMailNumberUsingSub(int subscription);

    /**
     * Retrieves the complete voice mail number.
     */
    String getCompleteVoiceMailNumber();

    /**
     * Retrieves the complete voice mail number for particular subscription
     */
    String getCompleteVoiceMailNumberUsingSub(int subscription);

    /**
     * Retrieves the alpha identifier associated with the voice mail number.
     */
    String getVoiceMailAlphaTag();

    /**
     * Retrieves the alpha identifier associated with the voice mail number
     * of a subscription.
     */
    String getVoiceMailAlphaTagUsingSub(int subscription);

    /**
     * Returns the IMS private user identity (IMPI) that was loaded from the ISIM.
     * @return the IMPI, or null if not present or not loaded
     */
    String getIsimImpi();

    /**
     * Returns the IMS home network domain name that was loaded from the ISIM.
     * @return the IMS domain name, or null if not present or not loaded
     */
    String getIsimDomain();

    /**
     * Returns the IMS public user identities (IMPU) that were loaded from the ISIM.
     * @return an array of IMPU strings, with one IMPU per string, or null if
     *      not present or not loaded
     */
    String[] getIsimImpu();
}
