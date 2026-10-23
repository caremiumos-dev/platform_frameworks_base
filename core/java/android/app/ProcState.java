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

package android.app;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

/**
 * Persisted per-process usage statistics gathered by the XTC performance
 * subsystem (top PSS samples).
 *
 * @hide
 */
public class ProcState {
    private static final String TAG = "xtcperformance-ProcState";

    private static final String ATTR_USERID = "userid";
    private static final String ATTR_PKG = "pkgName";
    private static final String ATTR_PROC = "procName";
    private static final String ATTR_TOP_PSS = "topPss";
    private static final String ATTR_TOP_PSS_SAMPLE_COUNT = "topPssCount";

    public String packageName;
    public String processName;
    public long avgTopPss;
    public long topPssSampleCount;
    public Integer mUserId;

    public ProcState() {
    }

    public ProcState(String name, Integer userId) {
        this.packageName = name;
        this.processName = name;
        this.avgTopPss = 0L;
        this.topPssSampleCount = 0L;
        this.mUserId = userId;
    }

    public ProcState(String packageName, String processName, long avgTopPss,
            long topPssSampleCount, int userId) {
        this.packageName = packageName;
        this.processName = processName;
        this.avgTopPss = avgTopPss;
        this.topPssSampleCount = topPssSampleCount;
        this.mUserId = userId;
    }

    public static ProcState restoreFromFile(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        ProcState state = new ProcState();
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
            String name = parser.getAttributeName(i);
            String value = parser.getAttributeValue(i);
            if (ATTR_USERID.equals(name)) {
                state.mUserId = Integer.valueOf(value);
            } else if (ATTR_PKG.equals(name)) {
                state.packageName = value;
            } else if (ATTR_PROC.equals(name)) {
                state.processName = value;
            } else if (ATTR_TOP_PSS.equals(name)) {
                state.avgTopPss = Long.valueOf(value);
            } else if (ATTR_TOP_PSS_SAMPLE_COUNT.equals(name)) {
                state.topPssSampleCount = Long.valueOf(value);
            } else {
                Log.e(TAG, "error attr name....:" + name);
            }
        }
        return state;
    }

    @Override
    public String toString() {
        return "UID:" + this.mUserId + "|pkg:" + this.packageName + "|proc:" + this.processName
                + "|pss:" + this.avgTopPss + "|count:" + this.topPssSampleCount;
    }

    public void writeToFile(XmlSerializer serializer) throws XmlPullParserException, IOException {
        serializer.attribute(null, ATTR_USERID, String.valueOf(this.mUserId));
        serializer.attribute(null, ATTR_PKG, this.packageName);
        serializer.attribute(null, ATTR_PROC, this.processName);
        serializer.attribute(null, ATTR_TOP_PSS, String.valueOf(this.avgTopPss));
        serializer.attribute(null, ATTR_TOP_PSS_SAMPLE_COUNT,
                String.valueOf(this.topPssSampleCount));
    }
}