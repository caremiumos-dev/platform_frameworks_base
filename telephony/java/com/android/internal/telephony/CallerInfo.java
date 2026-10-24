/*
 * Copyright (C) 2006 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.Country;
import android.location.CountryDetector;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.provider.ContactsContract.RawContacts;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import android.telephony.PhoneNumberUtils;
import android.telephony.Rlog;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.telecom.XtcSecurePii;
import android.util.SeempLog;

import com.android.i18n.phonenumbers.geocoding.PhoneNumberOfflineGeocoder;
import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber.PhoneNumber;
import android.telephony.SubscriptionManager;

import java.util.Locale;


/**
 * Looks up caller information for the given phone number.
 *
 * {@hide}
 */
public class CallerInfo {
    private static final String TAG = "CallerInfo";
    private static final boolean VDBG = Rlog.isLoggable(TAG, Log.VERBOSE);

    public static final long USER_TYPE_CURRENT = 0;
    public static final long USER_TYPE_WORK = 1;

    /**
     * Please note that, any one of these member variables can be null,
     * and any accesses to them should be prepared to handle such a case.
     *
     * Also, it is implied that phoneNumber is more often populated than
     * name is, (think of calls being dialed/received using numbers where
     * names are not known to the device), so phoneNumber should serve as
     * a dependable fallback when name is unavailable.
     *
     * One other detail here is that this CallerInfo object reflects
     * information found on a connection, it is an OUTPUT that serves
     * mainly to display information to the user.  In no way is this object
     * used as input to make a connection, so we can choose to display
     * whatever human-readable text makes sense to the user for a
     * connection.  This is especially relevant for the phone number field,
     * since it is the one field that is most likely exposed to the user.
     *
     * As an example:
     *   1. User dials "911"
     *   2. Device recognizes that this is an emergency number
     *   3. We use the "Emergency Number" string instead of "911" in the
     *     phoneNumber field.
     *
     * What we're really doing here is treating phoneNumber as an essential
     * field here, NOT name.  We're NOT always guaranteed to have a name
     * for a connection, but the number should be displayable.
     */
    // XTC: 联系人扩展字段（来自厂商联系人库/通话记录）。
    public static String callTransName;
    public String callAutoCall;
    public String callCountryCode;
    public String callInShortNumWhiteList;
    public String callMobileID;
    public String callParentReview;
    public String callPhotoPath;
    public String callType;

    public String name;
    public String phoneNumber;
    public String normalizedNumber;
    public String geoDescription;

    public String cnapName;
    public int numberPresentation;
    public int namePresentation;
    public boolean contactExists;

    public String phoneLabel;
    /* Split up the phoneLabel into number type and label name */
    public int    numberType;
    public String numberLabel;

    public int photoResource;

    // Contact ID, which will be 0 if a contact comes from the corp CP2.
    public long contactIdOrZero;
    public boolean needUpdate;
    public Uri contactRefUri;
    public String lookupKey;

    public long userType;

    /**
     * Contact display photo URI.  If a contact has no display photo but a thumbnail, it'll be
     * the thumbnail URI instead.
     */
    public Uri contactDisplayPhotoUri;

    // fields to hold individual contact preference data,
    // including the send to voicemail flag and the ringtone
    // uri reference.
    public Uri contactRingtoneUri;
    public boolean shouldSendToVoicemail;

    /**
     * Drawable representing the caller image.  This is essentially
     * a cache for the image data tied into the connection /
     * callerinfo object.
     *
     * This might be a high resolution picture which is more suitable
     * for full-screen image view than for smaller icons used in some
     * kinds of notifications.
     *
     * The {@link #isCachedPhotoCurrent} flag indicates if the image
     * data needs to be reloaded.
     */
    public Drawable cachedPhoto;
    /**
     * Bitmap representing the caller image which has possibly lower
     * resolution than {@link #cachedPhoto} and thus more suitable for
     * icons (like notification icons).
     *
     * In usual cases this is just down-scaled image of {@link #cachedPhoto}.
     * If the down-scaling fails, this will just become null.
     *
     * The {@link #isCachedPhotoCurrent} flag indicates if the image
     * data needs to be reloaded.
     */
    public Bitmap cachedPhotoIcon;
    /**
     * Boolean which indicates if {@link #cachedPhoto} and
     * {@link #cachedPhotoIcon} is fresh enough. If it is false,
     * those images aren't pointing to valid objects.
     */
    public boolean isCachedPhotoCurrent;

    private boolean mIsEmergency;
    private boolean mIsVoiceMail;

    public CallerInfo() {
        // TODO: Move all the basic initialization here?
        mIsEmergency = false;
        mIsVoiceMail = false;
        userType = USER_TYPE_CURRENT;
    }

    /**
     * getCallerInfo given a Cursor.
     * @param context the context used to retrieve string constants
     * @param contactRef the URI to attach to this CallerInfo object
     * @param cursor the first object in the cursor is used to build the CallerInfo object.
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied.
     */
    public static CallerInfo getCallerInfo(Context context, Uri contactRef, Cursor cursor) {
        SeempLog.record_uri(12, contactRef);
        CallerInfo info = new CallerInfo();
        info.photoResource = 0;
        info.phoneLabel = null;
        info.numberType = 0;
        info.numberLabel = null;
        info.cachedPhoto = null;
        info.isCachedPhotoCurrent = false;
        info.contactExists = false;
        info.userType = USER_TYPE_CURRENT;

        if (VDBG) Rlog.v(TAG, "getCallerInfo() based on cursor...");

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // TODO: photo_id is always available but not taken
                // care of here. Maybe we should store it in the
                // CallerInfo object as well.

                int columnIndex;

                // Look for the name
                columnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
                if (columnIndex != -1) {
                    info.name = cursor.getString(columnIndex);
                }

                // Look for the number
                columnIndex = cursor.getColumnIndex(PhoneLookup.NUMBER);
                if (columnIndex != -1) {
                    info.phoneNumber = cursor.getString(columnIndex);
                }

                // Look for the normalized number
                columnIndex = cursor.getColumnIndex(PhoneLookup.NORMALIZED_NUMBER);
                if (columnIndex != -1) {
                    info.normalizedNumber = cursor.getString(columnIndex);
                }

                // Look for the label/type combo
                columnIndex = cursor.getColumnIndex(PhoneLookup.LABEL);
                if (columnIndex != -1) {
                    int typeColumnIndex = cursor.getColumnIndex(PhoneLookup.TYPE);
                    if (typeColumnIndex != -1) {
                        info.numberType = cursor.getInt(typeColumnIndex);
                        info.numberLabel = cursor.getString(columnIndex);
                        info.phoneLabel = Phone.getDisplayLabel(context,
                                info.numberType, info.numberLabel)
                                .toString();
                    }
                }

                // Look for the person_id.
                columnIndex = getColumnIndexForPersonId(contactRef, cursor);
                if (columnIndex != -1) {
                    final long contactId = cursor.getLong(columnIndex);
                    if (contactId != 0 && !Contacts.isEnterpriseContactId(contactId)) {
                        info.contactIdOrZero = contactId;
                        if (VDBG) {
                            Rlog.v(TAG, "==> got info.contactIdOrZero: " + info.contactIdOrZero);
                        }
                    }
                    if (Contacts.isEnterpriseContactId(contactId)) {
                        info.userType = USER_TYPE_WORK;
                    }
                } else {
                    // No valid columnIndex, so we can't look up person_id.
                    Rlog.w(TAG, "Couldn't find contact_id column for " + contactRef);
                    // Watch out: this means that anything that depends on
                    // person_id will be broken (like contact photo lookups in
                    // the in-call UI, for example.)
                }

                // Contact lookupKey
                columnIndex = cursor.getColumnIndex(PhoneLookup.LOOKUP_KEY);
                if (columnIndex != -1) {
                    info.lookupKey = cursor.getString(columnIndex);
                }

                // Display photo URI.
                columnIndex = cursor.getColumnIndex(PhoneLookup.PHOTO_URI);
                if ((columnIndex != -1) && (cursor.getString(columnIndex) != null)) {
                    info.contactDisplayPhotoUri = Uri.parse(cursor.getString(columnIndex));
                } else {
                    info.contactDisplayPhotoUri = null;
                }

                // look for the custom ringtone, create from the string stored
                // in the database.
                // An empty string ("") in the database indicates a silent ringtone,
                // and we set contactRingtoneUri = Uri.EMPTY, so that no ringtone will be played.
                // {null} in the database indicates the default ringtone,
                // and we set contactRingtoneUri = null, so that default ringtone will be played.
                columnIndex = cursor.getColumnIndex(PhoneLookup.CUSTOM_RINGTONE);
                if ((columnIndex != -1) && (cursor.getString(columnIndex) != null)) {
                    if (TextUtils.isEmpty(cursor.getString(columnIndex))) {
                        info.contactRingtoneUri = Uri.EMPTY;
                    } else {
                        info.contactRingtoneUri = Uri.parse(cursor.getString(columnIndex));
                    }
                } else {
                    info.contactRingtoneUri = null;
                }

                // look for the send to voicemail flag, set it to true only
                // under certain circumstances.
                columnIndex = cursor.getColumnIndex(PhoneLookup.SEND_TO_VOICEMAIL);
                info.shouldSendToVoicemail = (columnIndex != -1) &&
                        ((cursor.getInt(columnIndex)) == 1);

                // XTC: 厂商联系人库扩展列（AUTO_CALL/MOBILE_ID/TYPE/PHOTO_PATH/
                // COUNTRY_CODE/shortDialNumber/parentReview）。
                parseXtcColumns(info, cursor);
                info.contactExists = true;
            }
            cursor.close();
            cursor = null;
        }

        info.needUpdate = false;
        info.name = normalize(info.name);
        info.contactRefUri = contactRef;

        return info;
    }

    private static void parseXtcColumns(CallerInfo info, Cursor cursor) {
        int columnIndex = cursor.getColumnIndex("AUTO_CALL");
        if (columnIndex != -1) {
            info.callAutoCall = cursor.getString(columnIndex);
        }
        columnIndex = cursor.getColumnIndex("MOBILE_ID");
        if (columnIndex != -1) {
            info.callMobileID = cursor.getString(columnIndex);
        }
        columnIndex = cursor.getColumnIndex("TYPE");
        if (columnIndex != -1) {
            info.callType = cursor.getString(columnIndex);
        }
        columnIndex = cursor.getColumnIndex("PHOTO_PATH");
        if (columnIndex != -1) {
            info.callPhotoPath = cursor.getString(columnIndex);
        }
        columnIndex = cursor.getColumnIndex("COUNTRY_CODE");
        if (columnIndex != -1) {
            info.callCountryCode = cursor.getString(columnIndex);
        }
        columnIndex = cursor.getColumnIndex("shortDialNumber");
        if (columnIndex != -1) {
            info.callInShortNumWhiteList = cursor.getString(columnIndex);
        }
        columnIndex = cursor.getColumnIndex("parentReview");
        if (columnIndex != -1) {
            info.callParentReview = cursor.getString(columnIndex);
        }
        Log.e(TAG, "bbklhj getCallerInfo from xtcContactDB info :  " + info
                + ",callParentReview = " + info.callParentReview);
    }

    /**
     * XTC: 把游标里的所有匹配行解析成 CallerInfo 列表（callTransName 用于过滤）。
     */
    private static List<CallerInfo> cursor2CallerInfo(Context context, Cursor cursor, Uri uri) {
        List<CallerInfo> infos = new ArrayList<>();
        if (cursor == null) {
            return infos;
        }
        Log.d(TAG, "cursor2CallerInfo start->");
        while (cursor.moveToNext()) {
            CallerInfo info = new CallerInfo();
            info.photoResource = 0;
            info.phoneLabel = null;
            info.numberType = 0;
            info.numberLabel = null;
            info.cachedPhoto = null;
            info.isCachedPhotoCurrent = false;
            info.contactExists = false;

            int columnIndex = cursor.getColumnIndex("NAME");
            if (columnIndex == -1) {
                columnIndex = cursor.getColumnIndex(PhoneLookup.DISPLAY_NAME);
            }
            if (columnIndex != -1) {
                info.name = cursor.getString(columnIndex);
                Log.d(TAG, "bbklhj   getCallerInfo from xtcContactDB name ="
                        + XtcSecurePii.encrypt(info.name));
            }
            columnIndex = cursor.getColumnIndex("NUMBER");
            if (columnIndex == -1) {
                columnIndex = cursor.getColumnIndex(PhoneLookup.NUMBER);
            }
            if (columnIndex != -1) {
                info.phoneNumber = cursor.getString(columnIndex);
                Log.d(TAG, "bbklhj   getCallerInfo from xtcContactDB info.phoneNumber ="
                        + XtcSecurePii.encrypt(info.phoneNumber));
            }
            columnIndex = cursor.getColumnIndex("normalized_number");
            if (columnIndex == -1) {
                columnIndex = cursor.getColumnIndex(PhoneLookup.NORMALIZED_NUMBER);
            }
            if (columnIndex != -1) {
                info.normalizedNumber = cursor.getString(columnIndex);
            }
            parseXtcColumns(info, cursor);

            int labelIndex = cursor.getColumnIndex("label");
            int typeIndex = cursor.getColumnIndex("type");
            if (labelIndex != -1 && typeIndex != -1) {
                info.numberType = cursor.getInt(typeIndex);
                info.numberLabel = cursor.getString(labelIndex);
                info.phoneLabel = Phone.getDisplayLabel(context, info.numberType,
                        info.numberLabel).toString();
            }

            int personIdIndex = getColumnIndexForPersonId(uri, cursor);
            if (personIdIndex != -1) {
                long contactId = cursor.getLong(personIdIndex);
                if (contactId != 0 && !Contacts.isEnterpriseContactId(contactId)) {
                    info.contactIdOrZero = contactId;
                }
                if (Contacts.isEnterpriseContactId(contactId)) {
                    info.userType = USER_TYPE_WORK;
                }
            }

            int lookupIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY);
            if (lookupIndex != -1) {
                info.lookupKey = cursor.getString(lookupIndex);
            }

            int photoIndex = cursor.getColumnIndex(PhoneLookup.PHOTO_URI);
            if (photoIndex != -1 && cursor.getString(photoIndex) != null) {
                info.contactDisplayPhotoUri = Uri.parse(cursor.getString(photoIndex));
            } else {
                info.contactDisplayPhotoUri = null;
            }

            int ringtoneIndex = cursor.getColumnIndex(PhoneLookup.CUSTOM_RINGTONE);
            if (ringtoneIndex != -1 && cursor.getString(ringtoneIndex) != null) {
                if (TextUtils.isEmpty(cursor.getString(ringtoneIndex))) {
                    info.contactRingtoneUri = Uri.EMPTY;
                } else {
                    info.contactRingtoneUri = Uri.parse(cursor.getString(ringtoneIndex));
                }
            } else {
                info.contactRingtoneUri = null;
            }

            int voicemailIndex = cursor.getColumnIndex(PhoneLookup.SEND_TO_VOICEMAIL);
            info.shouldSendToVoicemail = (voicemailIndex != -1)
                    && (cursor.getInt(voicemailIndex) == 1);
            info.contactExists = true;
            info.needUpdate = false;
            info.name = normalize(info.name);
            info.contactRefUri = uri;

            Log.d(TAG, "callTransName:" + callTransName);
            if (callTransName == null || info.name == null
                    || info.name.contains(callTransName)) {
                infos.add(info);
            } else {
                Log.d(TAG, "callTransName is not matching");
            }
        }
        Log.d(TAG, "cursor2CallerInfo end <-- callerInfo.Size:" + infos.size());
        cursor.close();
        return infos;
    }

    /**
     * XTC: 多个联系人匹配同一号码时，优先取号码完全相等的，其次取后缀匹配的。
     */
    public static CallerInfo getCallerInfo(Context context, String number, Uri contactRef,
            Cursor cursor) {
        SeempLog.record_uri(12, contactRef);
        List<CallerInfo> infos = cursor2CallerInfo(context, cursor, contactRef);
        Log.d(TAG, "framework getCallerInfo number: " + XtcSecurePii.encrypt(number)
                + " ,callerInfos=" + infos);
        CallerInfo info = new CallerInfo();
        info.phoneNumber = number;
        if (infos.isEmpty() || TextUtils.isEmpty(number)) {
            return info;
        }
        String firstNumber = infos.get(0).phoneNumber;
        info.callInShortNumWhiteList = infos.get(0).callInShortNumWhiteList;
        if (TextUtils.isEmpty(firstNumber)) {
            return info;
        }
        if (infos.size() == 1) {
            return (firstNumber.endsWith(number) || number.endsWith(firstNumber))
                    ? infos.get(0) : info;
        }
        LinkedHashMap<String, CallerInfo> byNumber = new LinkedHashMap<>();
        for (CallerInfo ci : infos) {
            Log.d(TAG, "do getCallerInfo info: " + ci);
            if (!TextUtils.isEmpty(ci.phoneNumber)) {
                byNumber.put(ci.phoneNumber, ci);
            } else if (!TextUtils.isEmpty(ci.callInShortNumWhiteList)) {
                byNumber.put(ci.callInShortNumWhiteList, ci);
            }
        }
        if (byNumber.isEmpty()) {
            return info;
        }
        if (byNumber.containsKey(number)) {
            return byNumber.get(number);
        }
        for (String key : byNumber.keySet()) {
            CallerInfo ci = byNumber.get(key);
            if (!TextUtils.isEmpty(ci.callInShortNumWhiteList)) {
                info.callInShortNumWhiteList = ci.callInShortNumWhiteList;
            }
            if (key.equals(number)) {
                return ci;
            }
            if (key.endsWith(number) || number.endsWith(key)) {
                return ci;
            }
        }
        return info;
    }

    /**
     * XTC: 按号码查询联系人（返回多行匹配后的结果）。
     */
    public static CallerInfo getCallerInfo(Context context, String number, Uri contactRef) {
        ContentResolver cr = CallerInfoAsyncQuery.getCurrentProfileContentResolver(context);
        if (cr == null) {
            return null;
        }
        try {
            return getCallerInfo(context, number, contactRef,
                    cr.query(contactRef, null, null, null, null));
        } catch (RuntimeException re) {
            Rlog.e(TAG, "Error getting caller info.", re);
            return null;
        }
    }

    private static int getListEqualLength(ArrayList<String> first, ArrayList<String> second) {
        if (first == null || second == null || first.isEmpty() || second.isEmpty()) {
            return 0;
        }
        if (first.size() < second.size()) {
            ArrayList<String> tmp = first;
            first = second;
            second = tmp;
        }
        int equal = 0;
        for (int i = 0; i < first.size(); i++) {
            if (i >= second.size()) {
                return second.size();
            }
            if (!first.get(i).equals(second.get(i))) {
                break;
            }
            equal++;
        }
        return equal;
    }

    /**
     * XTC: 设置通话转接/翻译名过滤（只保留名字包含该串的联系人）。
     */
    public static void setTransName(String name) {
        callTransName = name;
    }

    /**
     * getCallerInfo given a URI, look up in the call-log database
     * for the uri unique key.
     * @param context the context used to get the ContentResolver
     * @param contactRef the URI used to lookup caller id
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied.
     */
    public static CallerInfo getCallerInfo(Context context, Uri contactRef) {
        CallerInfo info = null;
        ContentResolver cr = CallerInfoAsyncQuery.getCurrentProfileContentResolver(context);
        if (cr != null) {
            try {
                info = getCallerInfo(context, contactRef,
                        cr.query(contactRef, null, null, null, null));
            } catch (RuntimeException re) {
                Rlog.e(TAG, "Error getting caller info.", re);
            }
        }
        return info;
    }

    /**
     * getCallerInfo given a phone number, look up in the call-log database
     * for the matching caller id info.
     * @param context the context used to get the ContentResolver
     * @param number the phone number used to lookup caller id
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied. If
     * a matching number is not found, then a generic caller info is returned,
     * with all relevant fields empty or null.
     */
    public static CallerInfo getCallerInfo(Context context, String number) {
        if (VDBG) Rlog.v(TAG, "getCallerInfo() based on number...");

        int subId = SubscriptionManager.getDefaultSubscriptionId();
        return getCallerInfo(context, number, subId);
    }

    /**
     * getCallerInfo given a phone number and subscription, look up in the call-log database
     * for the matching caller id info.
     * @param context the context used to get the ContentResolver
     * @param number the phone number used to lookup caller id
     * @param subId the subscription for checking for if voice mail number or not
     * @return the CallerInfo which contains the caller id for the given
     * number. The returned CallerInfo is null if no number is supplied. If
     * a matching number is not found, then a generic caller info is returned,
     * with all relevant fields empty or null.
     */
    public static CallerInfo getCallerInfo(Context context, String number, int subId) {
        SeempLog.record_str(12, "number=" + number + ",subId=" + subId);
        if (TextUtils.isEmpty(number)) {
            return null;
        }

        // Change the callerInfo number ONLY if it is an emergency number
        // or if it is the voicemail number.  If it is either, take a
        // shortcut and skip the query.
        if (PhoneNumberUtils.isLocalEmergencyNumber(context, number)) {
            return new CallerInfo().markAsEmergency(context);
        } else if (PhoneNumberUtils.isVoiceMailNumber(subId, number)) {
            return new CallerInfo().markAsVoiceMail();
        }

        Uri contactUri = Uri.withAppendedPath(PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI,
                Uri.encode(number));

        CallerInfo info = getCallerInfo(context, contactUri);
        info = doSecondaryLookupIfNecessary(context, number, info);

        // if no query results were returned with a viable number,
        // fill in the original number value we used to query with.
        if (TextUtils.isEmpty(info.phoneNumber)) {
            info.phoneNumber = number;
        }

        return info;
    }

    /**
     * Performs another lookup if previous lookup fails and it's a SIP call
     * and the peer's username is all numeric. Look up the username as it
     * could be a PSTN number in the contact database.
     *
     * @param context the query context
     * @param number the original phone number, could be a SIP URI
     * @param previousResult the result of previous lookup
     * @return previousResult if it's not the case
     */
    static CallerInfo doSecondaryLookupIfNecessary(Context context,
            String number, CallerInfo previousResult) {
        if (!previousResult.contactExists
                && PhoneNumberUtils.isUriNumber(number)) {
            String username = PhoneNumberUtils.getUsernameFromUriNumber(number);
            if (PhoneNumberUtils.isGlobalPhoneNumber(username)) {
                previousResult = getCallerInfo(context,
                        Uri.withAppendedPath(PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI,
                                Uri.encode(username)));
            }
        }
        return previousResult;
    }

    // Accessors

    /**
     * @return true if the caller info is an emergency number.
     */
    public boolean isEmergencyNumber() {
        return mIsEmergency;
    }

    /**
     * @return true if the caller info is a voicemail number.
     */
    public boolean isVoiceMailNumber() {
        return mIsVoiceMail;
    }

    /**
     * Mark this CallerInfo as an emergency call.
     * @param context To lookup the localized 'Emergency Number' string.
     * @return this instance.
     */
    // TODO: Note we're setting the phone number here (refer to
    // javadoc comments at the top of CallerInfo class) to a localized
    // string 'Emergency Number'. This is pretty bad because we are
    // making UI work here instead of just packaging the data. We
    // should set the phone number to the dialed number and name to
    // 'Emergency Number' and let the UI make the decision about what
    // should be displayed.
    /* package */ CallerInfo markAsEmergency(Context context) {
        phoneNumber = context.getString(
            com.android.internal.R.string.emergency_call_dialog_number_for_display);
        photoResource = com.android.internal.R.drawable.picture_emergency;
        mIsEmergency = true;
        return this;
    }


    /**
     * Mark this CallerInfo as a voicemail call. The voicemail label
     * is obtained from the telephony manager. Caller must hold the
     * READ_PHONE_STATE permission otherwise the phoneNumber will be
     * set to null.
     * @return this instance.
     */
    // TODO: As in the emergency number handling, we end up writing a
    // string in the phone number field.
    /* package */ CallerInfo markAsVoiceMail() {

        int subId = SubscriptionManager.getDefaultSubscriptionId();
        return markAsVoiceMail(subId);

    }

    /* package */ CallerInfo markAsVoiceMail(int subId) {
        mIsVoiceMail = true;

        try {
            String voiceMailLabel = TelephonyManager.getDefault().getVoiceMailAlphaTag(subId);

            phoneNumber = voiceMailLabel;
        } catch (SecurityException se) {
            // Should never happen: if this process does not have
            // permission to retrieve VM tag, it should not have
            // permission to retrieve VM number and would not call
            // this method.
            // Leave phoneNumber untouched.
            Rlog.e(TAG, "Cannot access VoiceMail.", se);
        }
        // TODO: There is no voicemail picture?
        // FIXME: FIND ANOTHER ICON
        // photoResource = android.R.drawable.badge_voicemail;
        return this;
    }

    private static String normalize(String s) {
        if (s == null || s.length() > 0) {
            return s;
        } else {
            return null;
        }
    }

    /**
     * Returns the column index to use to find the "person_id" field in
     * the specified cursor, based on the contact URI that was originally
     * queried.
     *
     * This is a helper function for the getCallerInfo() method that takes
     * a Cursor.  Looking up the person_id is nontrivial (compared to all
     * the other CallerInfo fields) since the column we need to use
     * depends on what query we originally ran.
     *
     * Watch out: be sure to not do any database access in this method, since
     * it's run from the UI thread (see comments below for more info.)
     *
     * @return the columnIndex to use (with cursor.getLong()) to get the
     * person_id, or -1 if we couldn't figure out what colum to use.
     *
     * TODO: Add a unittest for this method.  (This is a little tricky to
     * test, since we'll need a live contacts database to test against,
     * preloaded with at least some phone numbers and SIP addresses.  And
     * we'll probably have to hardcode the column indexes we expect, so
     * the test might break whenever the contacts schema changes.  But we
     * can at least make sure we handle all the URI patterns we claim to,
     * and that the mime types match what we expect...)
     */
    private static int getColumnIndexForPersonId(Uri contactRef, Cursor cursor) {
        // TODO: This is pretty ugly now, see bug 2269240 for
        // more details. The column to use depends upon the type of URL:
        // - content://com.android.contacts/data/phones ==> use the "contact_id" column
        // - content://com.android.contacts/phone_lookup ==> use the "_ID" column
        // - content://com.android.contacts/data ==> use the "contact_id" column
        // If it's none of the above, we leave columnIndex=-1 which means
        // that the person_id field will be left unset.
        //
        // The logic here *used* to be based on the mime type of contactRef
        // (for example Phone.CONTENT_ITEM_TYPE would tell us to use the
        // RawContacts.CONTACT_ID column).  But looking up the mime type requires
        // a call to context.getContentResolver().getType(contactRef), which
        // isn't safe to do from the UI thread since it can cause an ANR if
        // the contacts provider is slow or blocked (like during a sync.)
        //
        // So instead, figure out the column to use for person_id by just
        // looking at the URI itself.

        if (VDBG) Rlog.v(TAG, "- getColumnIndexForPersonId: contactRef URI = '"
                        + contactRef + "'...");
        // Warning: Do not enable the following logging (due to ANR risk.)
        // if (VDBG) Rlog.v(TAG, "- MIME type: "
        //                 + context.getContentResolver().getType(contactRef));

        String url = contactRef.toString();
        String columnName = null;
        if (url.startsWith("content://com.android.contacts/data/phones")) {
            // Direct lookup in the Phone table.
            // MIME type: Phone.CONTENT_ITEM_TYPE (= "vnd.android.cursor.item/phone_v2")
            if (VDBG) Rlog.v(TAG, "'data/phones' URI; using RawContacts.CONTACT_ID");
            columnName = RawContacts.CONTACT_ID;
        } else if (url.startsWith("content://com.android.contacts/data")) {
            // Direct lookup in the Data table.
            // MIME type: Data.CONTENT_TYPE (= "vnd.android.cursor.dir/data")
            if (VDBG) Rlog.v(TAG, "'data' URI; using Data.CONTACT_ID");
            // (Note Data.CONTACT_ID and RawContacts.CONTACT_ID are equivalent.)
            columnName = Data.CONTACT_ID;
        } else if (url.startsWith("content://com.android.contacts/phone_lookup")) {
            // Lookup in the PhoneLookup table, which provides "fuzzy matching"
            // for phone numbers.
            // MIME type: PhoneLookup.CONTENT_TYPE (= "vnd.android.cursor.dir/phone_lookup")
            if (VDBG) Rlog.v(TAG, "'phone_lookup' URI; using PhoneLookup._ID");
            columnName = PhoneLookup._ID;
        } else {
            Rlog.w(TAG, "Unexpected prefix for contactRef '" + url + "'");
        }
        int columnIndex = (columnName != null) ? cursor.getColumnIndex(columnName) : -1;
        if (VDBG) Rlog.v(TAG, "==> Using column '" + columnName
                        + "' (columnIndex = " + columnIndex + ") for person_id lookup...");
        return columnIndex;
    }

    /**
     * Updates this CallerInfo's geoDescription field, based on the raw
     * phone number in the phoneNumber field.
     *
     * (Note that the various getCallerInfo() methods do *not* set the
     * geoDescription automatically; you need to call this method
     * explicitly to get it.)
     *
     * @param context the context used to look up the current locale / country
     * @param fallbackNumber if this CallerInfo's phoneNumber field is empty,
     *        this specifies a fallback number to use instead.
     */
    public void updateGeoDescription(Context context, String fallbackNumber) {
        String number = TextUtils.isEmpty(phoneNumber) ? fallbackNumber : phoneNumber;
        geoDescription = getGeoDescription(context, number);
    }

    /**
     * @return a geographical description string for the specified number.
     * @see com.android.i18n.phonenumbers.PhoneNumberOfflineGeocoder
     */
    public static String getGeoDescription(Context context, String number) {
        if (VDBG) Rlog.v(TAG, "getGeoDescription('" + number + "')...");

        if (TextUtils.isEmpty(number)) {
            return null;
        }

        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneNumberOfflineGeocoder geocoder = PhoneNumberOfflineGeocoder.getInstance();

        Locale locale = context.getResources().getConfiguration().locale;
        String countryIso = getCurrentCountryIso(context, locale);
        PhoneNumber pn = null;
        try {
            if (VDBG) Rlog.v(TAG, "parsing '" + number
                            + "' for countryIso '" + countryIso + "'...");
            pn = util.parse(number, countryIso);
            if (VDBG) Rlog.v(TAG, "- parsed number: " + pn);
        } catch (NumberParseException e) {
            Rlog.w(TAG, "getGeoDescription: NumberParseException for incoming number '"
                    + Rlog.pii(TAG, number) + "'");
        }

        if (pn != null) {
            String description = geocoder.getDescriptionForNumber(pn, locale);
            if (VDBG) Rlog.v(TAG, "- got description: '" + description + "'");
            return description;
        } else {
            return null;
        }
    }

    /**
     * @return The ISO 3166-1 two letters country code of the country the user
     *         is in.
     */
    private static String getCurrentCountryIso(Context context, Locale locale) {
        String countryIso = null;
        CountryDetector detector = (CountryDetector) context.getSystemService(
                Context.COUNTRY_DETECTOR);
        if (detector != null) {
            Country country = detector.detectCountry();
            if (country != null) {
                countryIso = country.getCountryIso();
            } else {
                Rlog.e(TAG, "CountryDetector.detectCountry() returned null.");
            }
        }
        if (countryIso == null) {
            countryIso = locale.getCountry();
            Rlog.w(TAG, "No CountryDetector; falling back to countryIso based on locale: "
                    + countryIso);
        }
        return countryIso;
    }

    protected static String getCurrentCountryIso(Context context) {
        return getCurrentCountryIso(context, Locale.getDefault());
    }

    /**
     * @return a string debug representation of this instance.
     */
    @Override
    public String toString() {
        // XTC: 号码/姓名等 PII 一律脱敏后输出，另附厂商联系人扩展字段。
        return new StringBuilder(512)
                .append(super.toString()).append(" { ")
                .append("\nname: ").append(XtcSecurePii.encrypt(name))
                .append("\nphoneNumber: ").append(XtcSecurePii.encrypt(phoneNumber))
                .append("\ncallAutoCall: ").append(callAutoCall)
                .append("\ncallMobileID: ").append(callMobileID)
                .append("\ncallType: ").append(callType)
                .append("\ncallPhotoPath: ").append(callPhotoPath)
                .append("\ncallCountryCode: ").append(callCountryCode)
                .append("\nnormalizedNumber: ").append(XtcSecurePii.encrypt(normalizedNumber))
                .append("\ngeoDescription: ").append(geoDescription)
                .append("\ncnapName: ").append(XtcSecurePii.encrypt(cnapName))
                .append("\nnumberPresentation: ").append(numberPresentation)
                .append("\nnamePresentation: ").append(namePresentation)
                .append("\ncontactExits: ").append(contactExists)
                .append("\nphoneLabel: ").append(phoneLabel)
                .append("\nnumberType: ").append(numberType)
                .append("\nnumberLabel: ").append(numberLabel)
                .append("\nphotoResource: ").append(photoResource)
                .append("\ncontactIdOrZero: ").append(contactIdOrZero)
                .append("\nneedUpdate: ").append(needUpdate)
                .append("\ncontactRingtoneUri: ").append(contactRingtoneUri)
                .append("\ncontactDisplayPhotoUri: ").append(contactDisplayPhotoUri)
                .append("\nshouldSendToVoicemail: ").append(shouldSendToVoicemail)
                .append("\ncachedPhoto: ").append(cachedPhoto)
                .append("\nisCachedPhotoCurrent: ").append(isCachedPhotoCurrent)
                .append("\nemergency: ").append(mIsEmergency)
                .append("\nvoicemail ").append(mIsVoiceMail)
                .append("\ncontactExists ").append(contactExists)
                .append("\nuserType ").append(userType)
                .append("\ncallAutoCall ").append(callAutoCall)
                .append("\ncallCountryCode ").append(callCountryCode)
                .append("\ncallInShortNumWhiteList ").append(callInShortNumWhiteList)
                .append(" }").toString();
    }
}
