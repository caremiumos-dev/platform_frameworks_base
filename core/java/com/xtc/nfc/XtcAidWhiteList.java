package com.xtc.nfc;

import android.content.Context;
import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class XtcAidWhiteList {
    public static final String KEY_CRS = "crs";
    public static final String KEY_ISD = "isd";
    public static final String KEY_SNB = "snowball";

    private static final String TAG = "XtcAidWhiteList";

    private static final Map<String, String> aids = new ConcurrentHashMap<String, String>();

    static {
        aids.put(KEY_SNB, "A0000003B0414D53442053425400");
        aids.put(KEY_ISD, "A000000151000000");
        aids.put(KEY_CRS, "A00000015143525300");
    }

    public String getAidByName(Context context, String name) {
        if (context == null || name == null) {
            Log.d(TAG, "getAidByName context:" + context + " ,name:" + name);
            return null;
        }
        XtcNfcSecure.enforceUserPerm(context.getPackageName());
        return aids.get(name);
    }
}