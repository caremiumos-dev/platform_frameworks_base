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

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

/**
 * A single policy configuration (a named {@code config} element) with its
 * configuration items.
 *
 * @hide
 */
public class PolicyConfig {
    private final LinkedHashMap<String, ConfigItem> mItems = new LinkedHashMap<>();
    private final String mName;

    public static class ConfigItem {
        private final String itemName;
        private final String itemValue;
        private final HashMap<String, String> mMap = new HashMap<>();
        private final HashMap<String, ArrayList<String>> mStringArrayMap = new HashMap<>();

        public ConfigItem(String name, String value) {
            this.itemName = name;
            this.itemValue = value;
        }

        public void add(String name, String value) {
            this.mMap.put(name, value);
        }

        public void addStringArray(String name, ArrayList<String> values) {
            this.mStringArrayMap.put(name, values);
        }

        public HashMap<String, String> getAllString() {
            return this.mMap;
        }

        public String getItemName() {
            return this.itemName;
        }

        public String getItemValue() {
            return this.itemValue;
        }

        public String getString(String name) {
            return this.mMap.get(name);
        }

        public ArrayList<String> getStringArray(String name) {
            return this.mStringArrayMap.get(name);
        }
    }

    public PolicyConfig(String name) {
        this.mName = name;
    }

    private void addConfigItem(ConfigItem item) {
        this.mItems.put(item.getItemValue(), item);
    }

    public static PolicyConfig loadFromXml(String name, XmlPullParser parser) {
        PolicyConfig config = new PolicyConfig(name);
        ArrayList<String> stringArray = null;
        ConfigItem item = null;
        while (true) {
            try {
                int event = parser.next();
                if (event == XmlPullParser.END_DOCUMENT) {
                    break;
                }
                String tag = parser.getName();
                if (event == XmlPullParser.START_TAG) {
                    if ("item".equals(tag)) {
                        int index = parser.getAttributeCount() - 1;
                        if (index >= 0) {
                            item = new ConfigItem(parser.getAttributeName(index),
                                    parser.getAttributeValue(index));
                        }
                    } else if ("string-array".equals(tag)) {
                        for (int i = parser.getAttributeCount() - 1; i >= 0; i--) {
                            String attrName = parser.getAttributeName(i);
                            String attrValue = parser.getAttributeValue(i);
                            if ("name".equals(attrName) && item != null) {
                                stringArray = new ArrayList<>();
                                item.addStringArray(attrValue, stringArray);
                                break;
                            }
                        }
                    } else if ("string".equals(tag)) {
                        if (stringArray != null) {
                            stringArray.add(parser.nextText());
                        }
                    } else if (item != null) {
                        item.add(tag, parser.nextText());
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    if ("item".equals(tag)) {
                        if (item != null) {
                            config.addConfigItem(item);
                        }
                    } else if ("config".equals(tag)) {
                        return config;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public HashMap<String, ConfigItem> getAllConfigItems() {
        return this.mItems;
    }

    public ConfigItem getConfigItem(String name) {
        return this.mItems.get(name);
    }

    public ConfigItem getDefaultItem() {
        return this.mItems.entrySet().iterator().next().getValue();
    }
}