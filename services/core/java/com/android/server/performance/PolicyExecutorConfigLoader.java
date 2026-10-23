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

package com.android.server.performance;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the {@code performance_config.xml} policy definitions.
 *
 * @hide
 */
public class PolicyExecutorConfigLoader {
    private static final String CONFIG_FILE = "/system/etc/performance_config.xml";

    private static void error(String message) {
        Log.e(PerformanceManagerDebugConfig.TAG, message);
    }

    public static List<PolicyItem> load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            error("config file not exist");
            return Collections.emptyList();
        }
        List<PolicyItem> items = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(reader);
            while (true) {
                int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                if (event == XmlPullParser.START_TAG && "policy".equals(parser.getName())) {
                    PolicyItem item = loadConfigFromXml(parser);
                    if (item != null) {
                        items.add(item);
                    }
                }
            }
            return items;
        } catch (Exception e) {
            error("Failed to load executors");
            return Collections.emptyList();
        }
    }

    private static PolicyItem loadConfigFromXml(XmlPullParser parser) {
        String policyName = null;
        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
            if ("name".equals(parser.getAttributeName(i))) {
                policyName = parser.getAttributeValue(i);
                break;
            }
        }
        if (policyName == null) {
            return null;
        }
        PolicyItem policyItem = new PolicyItem(policyName);
        while (true) {
            try {
                int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                String tag = parser.getName();
                if (event == XmlPullParser.START_TAG) {
                    if ("config".equals(tag)) {
                        String configName = "";
                        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
                            if ("name".equals(parser.getAttributeName(i))) {
                                configName = parser.getAttributeValue(i);
                                break;
                            }
                        }
                        PolicyConfig config = PolicyConfig.loadFromXml(configName, parser);
                        if (config != null) {
                            policyItem.addConfig(configName, config);
                        }
                    }
                } else if (event == XmlPullParser.END_TAG && "policy".equals(tag)) {
                    return policyItem;
                }
            } catch (Exception e) {
                error("Failed to load executors");
            }
        }
        return null;
    }
}