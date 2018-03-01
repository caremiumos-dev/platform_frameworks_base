/*
 * Copyright (C) 2018 The Android Open Source Project
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
package android.telephony.data;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.StringDef;
import android.content.ContentValues;
import android.database.Cursor;
import android.hardware.radio.V1_0.ApnTypes;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Telephony;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import java.lang.Integer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class representing an APN configuration.
 */
public class ApnSetting implements Parcelable {

    private static final String LOG_TAG = "ApnSetting";
    private static final boolean VDBG = false;

    private static final String APN_TYPE_CONSTANTS = "apn_type_constants";
    private static final String PROTOCOL_CONSTANTS = "protocol_constants";
    private static final String MVNO_TYPE_CONSTANTS = "mvno_type_constants";

    private static final Map<Object, Object> APN_TYPE_MAP;
    private static final Map<Object, Object> PROTOCOL_MAP;
    private static final Map<Object, Object> MVNO_TYPE_MAP;

    static {
        APN_TYPE_MAP = new ArrayMap<Object, Object>();
        APN_TYPE_MAP.put("*", 0);
        APN_TYPE_MAP.put("default", 1);
        APN_TYPE_MAP.put("mms", 2);
        APN_TYPE_MAP.put("supl", 3);
        APN_TYPE_MAP.put("dun", 4);
        APN_TYPE_MAP.put("hipri", 5);
        APN_TYPE_MAP.put("fota", 6);
        APN_TYPE_MAP.put("ims", 7);
        APN_TYPE_MAP.put("cbs", 8);
        APN_TYPE_MAP.put("ia", 9);
        APN_TYPE_MAP.put("emergency", 10);
        APN_TYPE_MAP.put(0, "*");
        APN_TYPE_MAP.put(1, "default");
        APN_TYPE_MAP.put(2, "mms");
        APN_TYPE_MAP.put(3, "supl");
        APN_TYPE_MAP.put(4, "dun");
        APN_TYPE_MAP.put(5, "hipri");
        APN_TYPE_MAP.put(6, "fota");
        APN_TYPE_MAP.put(7, "ims");
        APN_TYPE_MAP.put(8, "cbs");
        APN_TYPE_MAP.put(9, "ia");
        APN_TYPE_MAP.put(10, "emergency");

        PROTOCOL_MAP = new ArrayMap<Object, Object>();
        PROTOCOL_MAP.put("IP", 0);
        PROTOCOL_MAP.put("IPV6", 1);
        PROTOCOL_MAP.put("IPV4V6", 2);
        PROTOCOL_MAP.put("PPP", 3);
        PROTOCOL_MAP.put(0, "IP");
        PROTOCOL_MAP.put(1, "IPV6");
        PROTOCOL_MAP.put(2, "IPV4V6");
        PROTOCOL_MAP.put(3, "PPP");

        MVNO_TYPE_MAP = new ArrayMap<Object, Object>();
        MVNO_TYPE_MAP.put("spn", 0);
        MVNO_TYPE_MAP.put("imsi", 1);
        MVNO_TYPE_MAP.put("gid", 2);
        MVNO_TYPE_MAP.put("iccid", 3);
        MVNO_TYPE_MAP.put(0, "spn");
        MVNO_TYPE_MAP.put(1, "imsi");
        MVNO_TYPE_MAP.put(2, "gid");
        MVNO_TYPE_MAP.put(3, "iccid");
    }

    private final String mEntryName;
    private final String mApnName;
    private final InetAddress mProxy;
    private final int mPort;
    private final Uri mMmsc;
    private final InetAddress mMmsProxy;
    private final int mMmsPort;
    private final String mUser;
    private final String mPassword;
    private final int mAuthType;
    private final List<Integer> mTypes;
    private final int mTypesBitmap;
    private final int mId;
    private final String mOperatorNumeric;
    private final int mProtocol;
    private final int mRoamingProtocol;
    private final int mMtu;

    private final boolean mCarrierEnabled;

    private final int mNetworkTypeBitmask;

    private final int mProfileId;

    private final boolean mModemCognitive;
    private final int mMaxConns;
    private final int mWaitTime;
    private final int mMaxConnsTime;

    private final int mMvnoType;
    private final String mMvnoMatchData;

    private boolean mPermanentFailed = false;

    /**
     * Returns the types bitmap of the APN.
     *
     * @return types bitmap of the APN
     * @hide
     */
    public int getTypesBitmap() {
        return mTypesBitmap;
    }

    /**
     * Returns the MTU size of the mobile interface to which the APN connected.
     *
     * @return the MTU size of the APN
     * @hide
     */
    public int getMtu() {
        return mMtu;
    }

    /**
     * Returns the profile id to which the APN saved in modem.
     *
     * @return the profile id of the APN
     * @hide
     */
    public int getProfileId() {
        return mProfileId;
    }

    /**
     * Returns if the APN setting is to be set in modem.
     *
     * @return is the APN setting to be set in modem
     * @hide
     */
    public boolean getModemCognitive() {
        return mModemCognitive;
    }

    /**
     * Returns the max connections of this APN.
     *
     * @return the max connections of this APN
     * @hide
     */
    public int getMaxConns() {
        return mMaxConns;
    }

    /**
     * Returns the wait time for retry of the APN.
     *
     * @return the wait time for retry of the APN
     * @hide
     */
    public int getWaitTime() {
        return mWaitTime;
    }

    /**
     * Returns the time to limit max connection for the APN.
     *
     * @return the time to limit max connection for the APN
     * @hide
     */
    public int getMaxConnsTime() {
        return mMaxConnsTime;
    }

    /**
     * Returns the MVNO data. Examples:
     *   "spn": A MOBILE, BEN NL
     *   "imsi": 302720x94, 2060188
     *   "gid": 4E, 33
     *   "iccid": 898603 etc..
     *
     * @return the mvno match data
     * @hide
     */
    public String getMvnoMatchData() {
        return mMvnoMatchData;
    }

    /**
     * Indicates this APN setting is permanently failed and cannot be
     * retried by the retry manager anymore.
     *
     * @return if this APN setting is permanently failed
     * @hide
     */
    public boolean getPermanentFailed() {
        return mPermanentFailed;
    }

    /**
     * Sets if this APN setting is permanently failed.
     *
     * @param permanentFailed if this APN setting is permanently failed
     * @hide
     */
    public void setPermanentFailed(boolean permanentFailed) {
        mPermanentFailed = permanentFailed;
    }

    /**
     * Returns the entry name of the APN.
     *
     * @return the entry name for the APN
     */
    public String getEntryName() {
        return mEntryName;
    }

    /**
     * Returns the name of the APN.
     *
     * @return APN name
     */
    public String getApnName() {
        return mApnName;
    }

    /**
     * Returns the proxy address of the APN.
     *
     * @return proxy address.
     */
    public InetAddress getProxy() {
        return mProxy;
    }

    /**
     * Returns the proxy port of the APN.
     *
     * @return proxy port
     */
    public int getPort() {
        return mPort;
    }
    /**
     * Returns the MMSC Uri of the APN.
     *
     * @return MMSC Uri.
     */
    public Uri getMmsc() {
        return mMmsc;
    }

    /**
     * Returns the MMS proxy address of the APN.
     *
     * @return MMS proxy address.
     */
    public InetAddress getMmsProxyAddress() {
        return mMmsProxy;
    }

    /**
     * Returns the MMS proxy port of the APN.
     *
     * @return MMS proxy port
     */
    public int getMmsProxyPort() {
        return mMmsPort;
    }

    /**
     * Returns the APN username of the APN.
     *
     * @return APN username
     */
    public String getUser() {
        return mUser;
    }

    /**
     * Returns the APN password of the APN.
     *
     * @return APN password
     */
    public String getPassword() {
        return mPassword;
    }

    /** @hide */
    @IntDef({
            AUTH_TYPE_NONE,
            AUTH_TYPE_PAP,
            AUTH_TYPE_CHAP,
            AUTH_TYPE_PAP_OR_CHAP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AuthType {}

    /**
     * Returns the authentication type of the APN.
     *
     * Example of possible values: {@link #AUTH_TYPE_NONE}, {@link #AUTH_TYPE_PAP}.
     *
     * @return authentication type
     */
    @AuthType
    public int getAuthType() {
        return mAuthType;
    }

    /** @hide */
    @IntDef({
            TYPE_DEFAULT,
            TYPE_MMS,
            TYPE_SUPL,
            TYPE_DUN,
            TYPE_HIPRI,
            TYPE_FOTA,
            TYPE_IMS,
            TYPE_CBS,
            TYPE_IA,
            TYPE_EMERGENCY
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ApnType {}

    /**
     * Returns the list of APN types of the APN.
     *
     * Example of possible values: {@link #TYPE_DEFAULT}, {@link #TYPE_MMS}.
     *
     * @return the list of APN types
     */
    @ApnType
    public List<Integer> getTypes() {
        return mTypes;
    }

    /**
     * Returns the unique database id for this entry.
     *
     * @return the unique database id
     */
    public int getId() {
        return mId;
    }

    /**
     * Returns the numeric operator ID for the APN. Usually
     * {@link android.provider.Telephony.Carriers#MCC} +
     * {@link android.provider.Telephony.Carriers#MNC}.
     *
     * @return the numeric operator ID
     */
    public String getOperatorNumeric() {
        return mOperatorNumeric;
    }

    /** @hide */
    @IntDef({
            PROTOCOL_IP,
            PROTOCOL_IPV6,
            PROTOCOL_IPV4V6,
            PROTOCOL_PPP,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProtocolType {}

    /**
     * Returns the protocol to use to connect to this APN.
     *
     * One of the {@code PDP_type} values in TS 27.007 section 10.1.1.
     * Example of possible values: {@link #PROTOCOL_IP}, {@link #PROTOCOL_IPV6}.
     *
     * @return the protocol
     */
    @ProtocolType
    public int getProtocol() {
        return mProtocol;
    }

    /**
     * Returns the protocol to use to connect to this APN while the device is roaming.
     *
     * One of the {@code PDP_type} values in TS 27.007 section 10.1.1.
     * Example of possible values: {@link #PROTOCOL_IP}, {@link #PROTOCOL_IPV6}.
     *
     * @return the roaming protocol
     */
    @ProtocolType
    public int getRoamingProtocol() {
        return mRoamingProtocol;
    }

    /**
     * Returns the current status of APN.
     *
     * {@code true} : enabled APN.
     * {@code false} : disabled APN.
     *
     * @return the current status
     */
    public boolean isEnabled() {
        return mCarrierEnabled;
    }

    /**
     * Returns a bitmask describing the Radio Technologies(Network Types) which this APN may use.
     *
     * NetworkType bitmask is calculated from NETWORK_TYPE defined in {@link TelephonyManager}.
     *
     * Examples of Network Types include {@link TelephonyManager#NETWORK_TYPE_UNKNOWN},
     * {@link TelephonyManager#NETWORK_TYPE_GPRS}, {@link TelephonyManager#NETWORK_TYPE_EDGE}.
     *
     * @return a bitmask describing the Radio Technologies(Network Types)
     */
    public int getNetworkTypeBitmask() {
        return mNetworkTypeBitmask;
    }

    /** @hide */
    @IntDef({
            MVNO_TYPE_SPN,
            MVNO_TYPE_IMSI,
            MVNO_TYPE_GID,
            MVNO_TYPE_ICCID,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MvnoType {}

    /**
     * Returns the MVNO match type for this APN.
     *
     * Example of possible values: {@link #MVNO_TYPE_SPN}, {@link #MVNO_TYPE_IMSI}.
     *
     * @return the MVNO match type
     */
    @MvnoType
    public int getMvnoType() {
        return mMvnoType;
    }

    private ApnSetting(Builder builder) {
        this.mEntryName = builder.mEntryName;
        this.mApnName = builder.mApnName;
        this.mProxy = builder.mProxy;
        this.mPort = builder.mPort;
        this.mMmsc = builder.mMmsc;
        this.mMmsProxy = builder.mMmsProxy;
        this.mMmsPort = builder.mMmsPort;
        this.mUser = builder.mUser;
        this.mPassword = builder.mPassword;
        this.mAuthType = builder.mAuthType;
        this.mTypes = (builder.mTypes == null ? new ArrayList<Integer>() : builder.mTypes);
        this.mTypesBitmap = builder.mTypesBitmap;
        this.mId = builder.mId;
        this.mOperatorNumeric = builder.mOperatorNumeric;
        this.mProtocol = builder.mProtocol;
        this.mRoamingProtocol = builder.mRoamingProtocol;
        this.mMtu = builder.mMtu;
        this.mCarrierEnabled = builder.mCarrierEnabled;
        this.mNetworkTypeBitmask = builder.mNetworkTypeBitmask;
        this.mProfileId = builder.mProfileId;
        this.mModemCognitive = builder.mModemCognitive;
        this.mMaxConns = builder.mMaxConns;
        this.mWaitTime = builder.mWaitTime;
        this.mMaxConnsTime = builder.mMaxConnsTime;
        this.mMvnoType = builder.mMvnoType;
        this.mMvnoMatchData = builder.mMvnoMatchData;
    }

    /** @hide */
    public static ApnSetting makeApnSetting(int id, String operatorNumeric, String entryName,
            String apnName, InetAddress proxy, int port, Uri mmsc, InetAddress mmsProxy,
            int mmsPort, String user, String password, int authType, List<Integer> types,
            int protocol, int roamingProtocol, boolean carrierEnabled,
            int networkTypeBitmask, int profileId, boolean modemCognitive, int maxConns,
            int waitTime, int maxConnsTime, int mtu, int mvnoType, String mvnoMatchData) {
        return new Builder()
                .setId(id)
                .setOperatorNumeric(operatorNumeric)
                .setEntryName(entryName)
                .setApnName(apnName)
                .setProxy(proxy)
                .setPort(port)
                .setMmsc(mmsc)
                .setMmsProxyAddress(mmsProxy)
                .setMmsProxyPort(mmsPort)
                .setUser(user)
                .setPassword(password)
                .setAuthType(authType)
                .setTypes(types)
                .setProtocol(protocol)
                .setRoamingProtocol(roamingProtocol)
                .setCarrierEnabled(carrierEnabled)
                .setNetworkTypeBitmask(networkTypeBitmask)
                .setProfileId(profileId)
                .setModemCognitive(modemCognitive)
                .setMaxConns(maxConns)
                .setWaitTime(waitTime)
                .setMaxConnsTime(maxConnsTime)
                .setMtu(mtu)
                .setMvnoType(mvnoType)
                .setMvnoMatchData(mvnoMatchData)
                .build();
    }

    /** @hide */
    public static ApnSetting makeApnSetting(Cursor cursor) {
        final Integer[] types = parseTypes(
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.TYPE)));
        int networkTypeBitmask = cursor.getInt(
                cursor.getColumnIndexOrThrow(Telephony.Carriers.NETWORK_TYPE_BITMASK));
        if (networkTypeBitmask == 0) {
            final int bearerBitmask = cursor.getInt(cursor.getColumnIndexOrThrow(
                    Telephony.Carriers.BEARER_BITMASK));
            networkTypeBitmask =
                    ServiceState.convertBearerBitmaskToNetworkTypeBitmask(bearerBitmask);
        }

        return makeApnSetting(
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NUMERIC)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.APN)),
                inetAddressFromString(cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.PROXY))),
                portFromString(cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.PORT))),
                UriFromString(cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSC))),
                inetAddressFromString(cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPROXY))),
                portFromString(cursor.getString(
                        cursor.getColumnIndexOrThrow(Telephony.Carriers.MMSPORT))),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.USER)),
                cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PASSWORD)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.AUTH_TYPE)),
                Arrays.asList(types),
                transferStringConstants(
                    cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROTOCOL)),
                    PROTOCOL_CONSTANTS),
                transferStringConstants(
                    cursor.getString(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.ROAMING_PROTOCOL)),
                    PROTOCOL_CONSTANTS),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.CARRIER_ENABLED)) == 1,
                networkTypeBitmask,
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.PROFILE_ID)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.MODEM_COGNITIVE)) == 1,
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MAX_CONNS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.WAIT_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.MAX_CONNS_TIME)),
                cursor.getInt(cursor.getColumnIndexOrThrow(Telephony.Carriers.MTU)),
                transferStringConstants(
                    cursor.getString(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.MVNO_TYPE)),
                    MVNO_TYPE_CONSTANTS),
                cursor.getString(cursor.getColumnIndexOrThrow(
                        Telephony.Carriers.MVNO_MATCH_DATA)));
    }

    /** @hide */
    public static ApnSetting makeApnSetting(ApnSetting apn) {
        return makeApnSetting(apn.mId, apn.mOperatorNumeric, apn.mEntryName, apn.mApnName,
                apn.mProxy, apn.mPort, apn.mMmsc, apn.mMmsProxy, apn.mMmsPort, apn.mUser,
                apn.mPassword, apn.mAuthType, apn.mTypes, apn.mProtocol, apn.mRoamingProtocol,
                apn.mCarrierEnabled, apn.mNetworkTypeBitmask, apn.mProfileId,
                apn.mModemCognitive, apn.mMaxConns, apn.mWaitTime, apn.mMaxConnsTime, apn.mMtu,
                apn.mMvnoType, apn.mMvnoMatchData);
    }

    /** @hide */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[ApnSettingV4] ")
                .append(mEntryName)
                .append(", ").append(mId)
                .append(", ").append(mOperatorNumeric)
                .append(", ").append(mApnName)
                .append(", ").append(inetAddressToString(mProxy))
                .append(", ").append(UriToString(mMmsc))
                .append(", ").append(inetAddressToString(mMmsProxy))
                .append(", ").append(portToString(mMmsPort))
                .append(", ").append(portToString(mPort))
                .append(", ").append(mAuthType).append(", ");
        for (int i = 0; i < mTypes.size(); i++) {
            sb.append(mTypes.get(i));
            if (i < mTypes.size() - 1) {
                sb.append(" | ");
            }
        }
        sb.append(", ").append(mProtocol);
        sb.append(", ").append(mRoamingProtocol);
        sb.append(", ").append(mCarrierEnabled);
        sb.append(", ").append(mProfileId);
        sb.append(", ").append(mModemCognitive);
        sb.append(", ").append(mMaxConns);
        sb.append(", ").append(mWaitTime);
        sb.append(", ").append(mMaxConnsTime);
        sb.append(", ").append(mMtu);
        sb.append(", ").append(mMvnoType);
        sb.append(", ").append(mMvnoMatchData);
        sb.append(", ").append(mPermanentFailed);
        sb.append(", ").append(mNetworkTypeBitmask);
        return sb.toString();
    }

    /**
     * Returns true if there are MVNO params specified.
     * @hide
     */
    public boolean hasMvnoParams() {
        return (mMvnoType != -1) && !TextUtils.isEmpty(mMvnoMatchData);
    }

    /** @hide */
    public boolean canHandleType(@ApnType int type) {
        if (!mCarrierEnabled) {
            return false;
        }
        boolean wildcardable = true;
        if (TYPE_IA == type) {
            wildcardable = false;
        }
        for (int t : mTypes) {
            // DEFAULT handles all, and HIPRI is handled by DEFAULT
            if ((t == type) || (wildcardable && t == TYPE_ALL)
                    || ((t == TYPE_DEFAULT) && type == TYPE_HIPRI)) {
                return true;
            }
        }
        return false;
    }

    // check whether the types of two APN same (even only one type of each APN is same)
    private boolean typeSameAny(ApnSetting first, ApnSetting second) {
        if (VDBG) {
            StringBuilder apnType1 = new StringBuilder(first.mApnName + ": ");
            for (int index1 = 0; index1 < first.mTypes.size(); index1++) {
                apnType1.append(first.mTypes.get(index1));
                apnType1.append(",");
            }

            StringBuilder apnType2 = new StringBuilder(second.mApnName + ": ");
            for (int index1 = 0; index1 < second.mTypes.size(); index1++) {
                apnType2.append(second.mTypes.get(index1));
                apnType2.append(",");
            }
            Rlog.d(LOG_TAG, "APN1: is " + apnType1);
            Rlog.d(LOG_TAG, "APN2: is " + apnType2);
        }

        for (int index1 = 0; index1 < first.mTypes.size(); index1++) {
            for (int index2 = 0; index2 < second.mTypes.size(); index2++) {
                if (first.mTypes.get(index1).equals(ApnSetting.TYPE_ALL)
                        || second.mTypes.get(index2).equals(ApnSetting.TYPE_ALL)
                        || first.mTypes.get(index1).equals(second.mTypes.get(index2))) {
                    if (VDBG) Rlog.d(LOG_TAG, "typeSameAny: return true");
                    return true;
                }
            }
        }

        if (VDBG) Rlog.d(LOG_TAG, "typeSameAny: return false");
        return false;
    }

    // TODO - if we have this function we should also have hashCode.
    // Also should handle changes in type order and perhaps case-insensitivity
    /** @hide */
    public boolean equals(Object o) {
        if (o instanceof ApnSetting == false) {
            return false;
        }

        ApnSetting other = (ApnSetting) o;

        return mEntryName.equals(other.mEntryName)
                && Objects.equals(mId, other.mId)
                && Objects.equals(mOperatorNumeric, other.mOperatorNumeric)
                && Objects.equals(mApnName, other.mApnName)
                && Objects.equals(mProxy, other.mProxy)
                && Objects.equals(mMmsc, other.mMmsc)
                && Objects.equals(mMmsProxy, other.mMmsProxy)
                && Objects.equals(mMmsPort, other.mMmsPort)
                && Objects.equals(mPort,other.mPort)
                && Objects.equals(mUser, other.mUser)
                && Objects.equals(mPassword, other.mPassword)
                && Objects.equals(mAuthType, other.mAuthType)
                && Objects.equals(mTypes, other.mTypes)
                && Objects.equals(mTypesBitmap, other.mTypesBitmap)
                && Objects.equals(mProtocol, other.mProtocol)
                && Objects.equals(mRoamingProtocol, other.mRoamingProtocol)
                && Objects.equals(mCarrierEnabled, other.mCarrierEnabled)
                && Objects.equals(mProfileId, other.mProfileId)
                && Objects.equals(mModemCognitive, other.mModemCognitive)
                && Objects.equals(mMaxConns, other.mMaxConns)
                && Objects.equals(mWaitTime, other.mWaitTime)
                && Objects.equals(mMaxConnsTime, other.mMaxConnsTime)
                && Objects.equals(mMtu, other.mMtu)
                && Objects.equals(mMvnoType, other.mMvnoType)
                && Objects.equals(mMvnoMatchData, other.mMvnoMatchData)
                && Objects.equals(mNetworkTypeBitmask, other.mNetworkTypeBitmask);
    }

    /**
     * Compare two APN settings
     *
     * Note: This method does not compare 'mId', 'mNetworkTypeBitmask'. We only use this for
     * determining if tearing a data call is needed when conditions change. See
     * cleanUpConnectionsOnUpdatedApns in DcTracker.
     *
     * @param o the other object to compare
     * @param isDataRoaming True if the device is on data roaming
     * @return True if the two APN settings are same
     * @hide
     */
    public boolean equals(Object o, boolean isDataRoaming) {
        if (!(o instanceof ApnSetting)) {
            return false;
        }

        ApnSetting other = (ApnSetting) o;

        return mEntryName.equals(other.mEntryName)
                && Objects.equals(mOperatorNumeric, other.mOperatorNumeric)
                && Objects.equals(mApnName, other.mApnName)
                && Objects.equals(mProxy, other.mProxy)
                && Objects.equals(mMmsc, other.mMmsc)
                && Objects.equals(mMmsProxy, other.mMmsProxy)
                && Objects.equals(mMmsPort, other.mMmsPort)
                && Objects.equals(mPort, other.mPort)
                && Objects.equals(mUser, other.mUser)
                && Objects.equals(mPassword, other.mPassword)
                && Objects.equals(mAuthType, other.mAuthType)
                && Objects.equals(mTypes, other.mTypes)
                && Objects.equals(mTypesBitmap, other.mTypesBitmap)
                && (isDataRoaming || Objects.equals(mProtocol,other.mProtocol))
                && (!isDataRoaming || Objects.equals(mRoamingProtocol, other.mRoamingProtocol))
                && Objects.equals(mCarrierEnabled, other.mCarrierEnabled)
                && Objects.equals(mProfileId, other.mProfileId)
                && Objects.equals(mModemCognitive, other.mModemCognitive)
                && Objects.equals(mMaxConns, other.mMaxConns)
                && Objects.equals(mWaitTime, other.mWaitTime)
                && Objects.equals(mMaxConnsTime, other.mMaxConnsTime)
                && Objects.equals(mMtu, other.mMtu)
                && Objects.equals(mMvnoType, other.mMvnoType)
                && Objects.equals(mMvnoMatchData, other.mMvnoMatchData);
    }

    /**
     * Check if neither mention DUN and are substantially similar
     *
     * @param other The other APN settings to compare
     * @return True if two APN settings are similar
     * @hide
     */
    public boolean similar(ApnSetting other) {
        return (!this.canHandleType(TYPE_DUN)
                && !other.canHandleType(TYPE_DUN)
                && Objects.equals(this.mApnName, other.mApnName)
                && !typeSameAny(this, other)
                && xorEquals(this.mProxy, other.mProxy)
                && xorEqualsPort(this.mPort, other.mPort)
                && xorEquals(this.mProtocol, other.mProtocol)
                && xorEquals(this.mRoamingProtocol, other.mRoamingProtocol)
                && Objects.equals(this.mCarrierEnabled, other.mCarrierEnabled)
                && Objects.equals(this.mProfileId, other.mProfileId)
                && Objects.equals(this.mMvnoType, other.mMvnoType)
                && Objects.equals(this.mMvnoMatchData, other.mMvnoMatchData)
                && xorEquals(this.mMmsc, other.mMmsc)
                && xorEquals(this.mMmsProxy, other.mMmsProxy)
                && xorEqualsPort(this.mMmsPort, other.mMmsPort))
                && Objects.equals(this.mNetworkTypeBitmask, other.mNetworkTypeBitmask);
    }

    // Equal or one is not specified.
    private boolean xorEquals(String first, String second) {
        return (Objects.equals(first, second)
                || TextUtils.isEmpty(first)
                || TextUtils.isEmpty(second));
    }

    // Equal or one is not null.
    private boolean xorEquals(Object first, Object second) {
        return first == null || second == null || first.equals(second);
    }

    // Equal or one is not specified.
    private boolean xorEqualsPort(int first, int second) {
        return first == -1 || second == -1 || Objects.equals(first, second);
    }

    // Helper function to convert APN string into a 32-bit bitmask.
    private static int getApnBitmask(String apn) {
        switch (transferStringConstants(apn, APN_TYPE_CONSTANTS)) {
            case TYPE_DEFAULT: return ApnTypes.DEFAULT;
            case TYPE_MMS: return ApnTypes.MMS;
            case TYPE_SUPL: return ApnTypes.SUPL;
            case TYPE_DUN: return ApnTypes.DUN;
            case TYPE_HIPRI: return ApnTypes.HIPRI;
            case TYPE_FOTA: return ApnTypes.FOTA;
            case TYPE_IMS: return ApnTypes.IMS;
            case TYPE_CBS: return ApnTypes.CBS;
            case TYPE_IA: return ApnTypes.IA;
            case TYPE_EMERGENCY: return ApnTypes.EMERGENCY;
            case TYPE_ALL: return ApnTypes.ALL;
            default: return ApnTypes.NONE;
        }
    }

    private String deParseTypes(List<Integer> types) {
        if (types == null || types.size() == 0) {
            return null;
        }
        String[] strTypes = new String[types.size()];
        for (int i = 0; i < strTypes.length; i ++) {
            strTypes[i] = transferIntConstants(types.get(i), APN_TYPE_CONSTANTS);
        }
        return TextUtils.join(",", strTypes);
    }

    private String nullToEmpty(String stringValue) {
        return stringValue == null ? "" : stringValue;
    }

    /** @hide */
    // Called by DPM.
    public ContentValues toContentValues() {
        ContentValues apnValue = new ContentValues();
        apnValue.put(Telephony.Carriers.NUMERIC, nullToEmpty(mOperatorNumeric));
        apnValue.put(Telephony.Carriers.NAME, nullToEmpty(mEntryName));
        apnValue.put(Telephony.Carriers.APN, nullToEmpty(mApnName));
        apnValue.put(Telephony.Carriers.PROXY, mProxy == null ? "" : inetAddressToString(mProxy));
        apnValue.put(Telephony.Carriers.PORT, portToString(mPort));
        apnValue.put(Telephony.Carriers.MMSC, mMmsc == null ? "" : UriToString(mMmsc));
        apnValue.put(Telephony.Carriers.MMSPORT, portToString(mMmsPort));
        apnValue.put(Telephony.Carriers.MMSPROXY, mMmsProxy == null
                ? "" : inetAddressToString(mMmsProxy));
        apnValue.put(Telephony.Carriers.USER, nullToEmpty(mUser));
        apnValue.put(Telephony.Carriers.PASSWORD, nullToEmpty(mPassword));
        apnValue.put(Telephony.Carriers.AUTH_TYPE, mAuthType);
        String apnType = deParseTypes(mTypes);
        apnValue.put(Telephony.Carriers.TYPE, nullToEmpty(apnType));
        apnValue.put(Telephony.Carriers.PROTOCOL,
            transferIntConstants(mProtocol, PROTOCOL_CONSTANTS));
        apnValue.put(Telephony.Carriers.ROAMING_PROTOCOL,
            transferIntConstants(mRoamingProtocol, PROTOCOL_CONSTANTS));
        apnValue.put(Telephony.Carriers.CARRIER_ENABLED, mCarrierEnabled);
        apnValue.put(Telephony.Carriers.MVNO_TYPE,
            transferIntConstants(mMvnoType, MVNO_TYPE_CONSTANTS));
        apnValue.put(Telephony.Carriers.NETWORK_TYPE_BITMASK, mNetworkTypeBitmask);

        return apnValue;
    }

    /**
     * @param types comma delimited list of APN types
     * @return array of APN types
     * @hide
     */
    public static Integer[] parseTypes(String types) {
        String[] strResult;
        // If unset, set to DEFAULT.
        if (TextUtils.isEmpty(types)) {
            strResult = new String[1];
            strResult[0] = transferIntConstants(TYPE_ALL, APN_TYPE_CONSTANTS);
        } else {
            strResult = types.split(",");
        }
        Integer[] result = new Integer[strResult.length];
        for(int i = 0; i < result.length; i ++) {
            result[i] = transferStringConstants(strResult[i], APN_TYPE_CONSTANTS);
        }
        return result;
    }

    private static Uri UriFromString(String uri) {
        return TextUtils.isEmpty(uri) ? null : Uri.parse(uri);
    }

    private static String UriToString(Uri uri) {
        return uri == null ? "" : uri.toString();
    }

    private static InetAddress inetAddressFromString(String inetAddress) {
        if (TextUtils.isEmpty(inetAddress)) {
            return null;
        }
        try {
            return InetAddress.getByName(inetAddress);
        } catch (UnknownHostException e) {
            Log.e(LOG_TAG, "Can't parse InetAddress from string: unknown host.");
            return null;
        }
    }

    private static String inetAddressToString(InetAddress inetAddress) {
        if (inetAddress == null) {
            return null;
        }
        final String inetAddressString = inetAddress.toString();
        if (TextUtils.isEmpty(inetAddressString)) {
            return null;
        }
        final String hostName = inetAddressString.substring(0, inetAddressString.indexOf("/"));
        final String address = inetAddressString.substring(inetAddressString.indexOf("/") + 1);
        if (TextUtils.isEmpty(hostName) && TextUtils.isEmpty(address)) {
            return null;
        }
        return TextUtils.isEmpty(hostName) ? address : hostName;
    }

    private static int portFromString(String strPort) {
        int port = -1;
        if (!TextUtils.isEmpty(strPort)) {
            try {
                port = Integer.parseInt(strPort);
            } catch (NumberFormatException e) {
                Log.e(LOG_TAG, "Can't parse port from String");
            }
        }
        return port;
    }

    private static String portToString(int port) {
        return port == -1 ? "" : Integer.toString(port);
    }

    // Implement Parcelable.
    @Override
    /** @hide */
    public int describeContents() {
        return 0;
    }

    @Override
    /** @hide */
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mOperatorNumeric);
        dest.writeString(mEntryName);
        dest.writeString(mApnName);
        dest.writeValue(mProxy);
        dest.writeInt(mPort);
        dest.writeValue(mMmsc);
        dest.writeValue(mMmsProxy);
        dest.writeInt(mMmsPort);
        dest.writeString(mUser);
        dest.writeString(mPassword);
        dest.writeInt(mAuthType);
        dest.writeList(mTypes);
        dest.writeInt(mProtocol);
        dest.writeInt(mRoamingProtocol);
        dest.writeInt(mCarrierEnabled ? 1: 0);
        dest.writeInt(mMvnoType);
        dest.writeInt(mNetworkTypeBitmask);
    }

    private static ApnSetting readFromParcel(Parcel in) {
        final int id = in.readInt();
        final String operatorNumeric = in.readString();
        final String entryName = in.readString();
        final String apnName = in.readString();
        final InetAddress proxy = (InetAddress)in.readValue(InetAddress.class.getClassLoader());
        final int port = in.readInt();
        final Uri mmsc = (Uri)in.readValue(Uri.class.getClassLoader());
        final InetAddress mmsProxy = (InetAddress)in.readValue(InetAddress.class.getClassLoader());
        final int mmsPort = in.readInt();
        final String user = in.readString();
        final String password = in.readString();
        final int authType = in.readInt();
        final List<Integer> types = new ArrayList<>();
        in.readList(types, List.class.getClassLoader());
        final int protocol = in.readInt();
        final int roamingProtocol = in.readInt();
        final boolean carrierEnabled = in.readInt() > 0;
        final int mvnoType = in.readInt();
        final int networkTypeBitmask = in.readInt();

        return makeApnSetting(id, operatorNumeric, entryName, apnName,
                proxy, port, mmsc, mmsProxy, mmsPort, user, password, authType, types, protocol,
                roamingProtocol, carrierEnabled, networkTypeBitmask, 0, false,
                0, 0, 0, 0, mvnoType, null);
    }

    public static final Parcelable.Creator<ApnSetting> CREATOR =
            new Parcelable.Creator<ApnSetting>() {
                @Override
                public ApnSetting createFromParcel(Parcel in) {
                    return readFromParcel(in);
                }

                @Override
                public ApnSetting[] newArray(int size) {
                    return new ApnSetting[size];
                }
            };

    /** @hide */
    public static String transferIntConstants(int type, String category) {
        Object result;
        switch (category) {
            case APN_TYPE_CONSTANTS:
                result = APN_TYPE_MAP.get(type);
                break;
            case PROTOCOL_CONSTANTS:
                result = PROTOCOL_MAP.get(type);
                break;
            case MVNO_TYPE_CONSTANTS:
                result = MVNO_TYPE_MAP.get(type);
                break;
            default:
                result = null;
        }
        return result == null ? "" : String.valueOf(result);
    }

    /** @hide */
    public static int transferStringConstants(String type, String category) {
        Object result;
        switch (category) {
            case APN_TYPE_CONSTANTS:
                result = APN_TYPE_MAP.get(type);
                break;
            case PROTOCOL_CONSTANTS:
                result = PROTOCOL_MAP.get(type);
                break;
            case MVNO_TYPE_CONSTANTS:
                result = MVNO_TYPE_MAP.get(type);
                break;
            default:
                result = null;
        }
        return result == null ? -1 : (int)result;
    }

    /**
     * APN types for data connections.  These are usage categories for an APN
     * entry.  One APN entry may support multiple APN types, eg, a single APN
     * may service regular internet traffic ("default") as well as MMS-specific
     * connections.<br/>
     * ALL is a special type to indicate that this APN entry can
     * service all data connections.
     */
    public static final int TYPE_ALL = 0;
    /** APN type for default data traffic */
    public static final int TYPE_DEFAULT = 1;
    /** APN type for MMS traffic */
    public static final int TYPE_MMS = 2;
    /** APN type for SUPL assisted GPS */
    public static final int TYPE_SUPL = 3;
    /** APN type for DUN traffic */
    public static final int TYPE_DUN = 4;
    /** APN type for HiPri traffic */
    public static final int TYPE_HIPRI = 5;
    /** APN type for FOTA */
    public static final int TYPE_FOTA = 6;
    /** APN type for IMS */
    public static final int TYPE_IMS = 7;
    /** APN type for CBS */
    public static final int TYPE_CBS = 8;
    /** APN type for IA Initial Attach APN */
    public static final int TYPE_IA = 9;
    /** APN type for Emergency PDN. This is not an IA apn, but is used
     * for access to carrier services in an emergency call situation. */
    public static final int TYPE_EMERGENCY = 10;

    // Possible values for authentication types.
    /** No authentication type. */
    public static final int AUTH_TYPE_NONE = 0;
    /** Authentication type for PAP. */
    public static final int AUTH_TYPE_PAP = 1;
    /** Authentication type for CHAP. */
    public static final int AUTH_TYPE_CHAP = 2;
    /** Authentication type for PAP or CHAP. */
    public static final int AUTH_TYPE_PAP_OR_CHAP = 3;

    // Possible values for protocol.
    /** Protocol type for IP. */
    public static final int PROTOCOL_IP = 0;
    /** Protocol type for IPV6. */
    public static final int PROTOCOL_IPV6 = 1;
    /** Protocol type for IPV4V6. */
    public static final int PROTOCOL_IPV4V6 = 2;
    /** Protocol type for PPP. */
    public static final int PROTOCOL_PPP = 3;

    // Possible values for MVNO type.
    /** MVNO type for service provider name. */
    public static final int MVNO_TYPE_SPN = 0;
    /** MVNO type for IMSI. */
    public static final int MVNO_TYPE_IMSI = 1;
    /** MVNO type for group identifier level 1. */
    public static final int MVNO_TYPE_GID = 2;
    /** MVNO type for ICCID. */
    public static final int MVNO_TYPE_ICCID = 3;

    public static class Builder{
        private String mEntryName;
        private String mApnName;
        private InetAddress mProxy;
        private int mPort = -1;
        private Uri mMmsc;
        private InetAddress mMmsProxy;
        private int mMmsPort = -1;
        private String mUser;
        private String mPassword;
        private int mAuthType;
        private List<Integer> mTypes;
        private int mTypesBitmap;
        private int mId;
        private String mOperatorNumeric;
        private int mProtocol;
        private int mRoamingProtocol;
        private int mMtu;
        private int mNetworkTypeBitmask;
        private boolean mCarrierEnabled;
        private int mProfileId;
        private boolean mModemCognitive;
        private int mMaxConns;
        private int mWaitTime;
        private int mMaxConnsTime;
        private int mMvnoType;
        private String mMvnoMatchData;

        /**
         * Default constructor for Builder.
         */
        public Builder() {}

        /**
         * Sets the unique database id for this entry.
         *
         * @param id the unique database id to set for this entry
         */
        private Builder setId(int id) {
            this.mId = id;
            return this;
        }

        /**
         * Set the MTU size of the mobile interface to which the APN connected.
         *
         * @param mtu the MTU size to set for the APN
         * @hide
         */
        public Builder setMtu(int mtu) {
            this.mMtu = mtu;
            return this;
        }

        /**
         * Sets the profile id to which the APN saved in modem.
         *
         * @param profileId the profile id to set for the APN
         * @hide
         */
        public Builder setProfileId(int profileId) {
            this.mProfileId = profileId;
            return this;
        }

        /**
         * Sets if the APN setting is to be set in modem.
         *
         * @param modemCognitive if the APN setting is to be set in modem
         * @hide
         */
        public Builder setModemCognitive(boolean modemCognitive) {
            this.mModemCognitive = modemCognitive;
            return this;
        }

        /**
         * Sets the max connections of this APN.
         *
         * @param maxConns the max connections of this APN
         * @hide
         */
        public Builder setMaxConns(int maxConns) {
            this.mMaxConns = maxConns;
            return this;
        }

        /**
         * Sets the wait time for retry of the APN.
         *
         * @param waitTime the wait time for retry of the APN
         * @hide
         */
        public Builder setWaitTime(int waitTime) {
            this.mWaitTime = waitTime;
            return this;
        }

        /**
         * Sets the time to limit max connection for the APN.
         *
         * @param maxConnsTime the time to limit max connection for the APN
         * @hide
         */
        public Builder setMaxConnsTime(int maxConnsTime) {
            this.mMaxConnsTime = maxConnsTime;
            return this;
        }

        /**
         * Sets the MVNO match data for the APN.
         *
         * @param mvnoMatchData the MVNO match data for the APN
         * @hide
         */
        public Builder setMvnoMatchData(String mvnoMatchData) {
            this.mMvnoMatchData = mvnoMatchData;
            return this;
        }

        /**
         * Sets the entry name of the APN.
         *
         * @param entryName the entry name to set for the APN
         */
        public Builder setEntryName(String entryName) {
            this.mEntryName = entryName;
            return this;
        }

        /**
         * Sets the name of the APN.
         *
         * @param apnName the name to set for the APN
         */
        public Builder setApnName(String apnName) {
            this.mApnName = apnName;
            return this;
        }

        /**
         * Sets the proxy address of the APN.
         *
         * @param proxy the proxy address to set for the APN
         */
        public Builder setProxy(InetAddress proxy) {
            this.mProxy = proxy;
            return this;
        }

        /**
         * Sets the proxy port of the APN.
         *
         * @param port the proxy port to set for the APN
         */
        public Builder setPort(int port) {
            this.mPort = port;
            return this;
        }

        /**
         * Sets the MMSC Uri of the APN.
         *
         * @param mmsc the MMSC Uri to set for the APN
         */
        public Builder setMmsc(Uri mmsc) {
            this.mMmsc = mmsc;
            return this;
        }

        /**
         * Sets the MMS proxy address of the APN.
         *
         * @param mmsProxy the MMS proxy address to set for the APN
         */
        public Builder setMmsProxyAddress(InetAddress mmsProxy) {
            this.mMmsProxy = mmsProxy;
            return this;
        }

        /**
         * Sets the MMS proxy port of the APN.
         *
         * @param mmsPort the MMS proxy port to set for the APN
         */
        public Builder setMmsProxyPort(int mmsPort) {
            this.mMmsPort = mmsPort;
            return this;
        }

        /**
         * Sets the APN username of the APN.
         *
         * @param user the APN username to set for the APN
         */
        public Builder setUser(String user) {
            this.mUser = user;
            return this;
        }

        /**
         * Sets the APN password of the APN.
         *
         * @see android.provider.Telephony.Carriers#PASSWORD
         * @param password the APN password to set for the APN
         */
        public Builder setPassword(String password) {
            this.mPassword = password;
            return this;
        }

        /**
         * Sets the authentication type of the APN.
         *
         * Example of possible values: {@link #AUTH_TYPE_NONE}, {@link #AUTH_TYPE_PAP}.
         *
         * @param authType the authentication type to set for the APN
         */
        public Builder setAuthType(@AuthType int authType) {
            this.mAuthType = authType;
            return this;
        }

        /**
         * Sets the list of APN types of the APN.
         *
         * Example of possible values: {@link #TYPE_DEFAULT}, {@link #TYPE_MMS}.
         *
         * @param types the list of APN types to set for the APN
         */
        public Builder setTypes(@ApnType List<Integer> types) {
            this.mTypes = types;
            int apnBitmap = 0;
            for (int i = 0; i < mTypes.size(); i++) {
                final String strType = transferIntConstants(mTypes.get(i), APN_TYPE_CONSTANTS);
                apnBitmap |= getApnBitmask(strType);
            }
            this.mTypesBitmap = apnBitmap;
            return this;
        }

        /**
         * Set the numeric operator ID for the APN.
         *
         * @param operatorNumeric the numeric operator ID to set for this entry
         */
        public Builder setOperatorNumeric(String operatorNumeric) {
            this.mOperatorNumeric = operatorNumeric;
            return this;
        }

        /**
         * Sets the protocol to use to connect to this APN.
         *
         * One of the {@code PDP_type} values in TS 27.007 section 10.1.1.
         * Example of possible values: {@link #PROTOCOL_IP}, {@link #PROTOCOL_IPV6}.
         *
         * @param protocol the protocol to set to use to connect to this APN
         */
        public Builder setProtocol(@ProtocolType int protocol) {
            this.mProtocol = protocol;
            return this;
        }

        /**
         * Sets the protocol to use to connect to this APN when the device is roaming.
         *
         * One of the {@code PDP_type} values in TS 27.007 section 10.1.1.
         * Example of possible values: {@link #PROTOCOL_IP}, {@link #PROTOCOL_IPV6}.
         *
         * @param roamingProtocol the protocol to set to use to connect to this APN when roaming
         */
        public Builder setRoamingProtocol(int roamingProtocol) {
            this.mRoamingProtocol = roamingProtocol;
            return this;
        }

        /**
         * Sets the current status for this APN.
         *
         * @param carrierEnabled the current status to set for this APN
         */
        public Builder setCarrierEnabled(boolean carrierEnabled) {
            this.mCarrierEnabled = carrierEnabled;
            return this;
        }

        /**
         * Sets Radio Technology (Network Type) info for this APN.
         *
         * @param networkTypeBitmask the Radio Technology (Network Type) info
         */
        public Builder setNetworkTypeBitmask(int networkTypeBitmask) {
            this.mNetworkTypeBitmask = networkTypeBitmask;
            return this;
        }

        /**
         * Sets the MVNO match type for this APN.
         *
         * Example of possible values: {@link #MVNO_TYPE_SPN}, {@link #MVNO_TYPE_IMSI}.
         *
         * @param mvnoType the MVNO match type to set for this APN
         */
        public Builder setMvnoType(@MvnoType int mvnoType) {
            this.mMvnoType = mvnoType;
            return this;
        }

        public ApnSetting build() {
            return new ApnSetting(this);
        }
    }
}

