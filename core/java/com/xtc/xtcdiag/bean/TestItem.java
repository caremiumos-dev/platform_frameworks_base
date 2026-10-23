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

package com.xtc.xtcdiag.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A single self-test item requested by the PC-side diag tool.
 *
 * @hide
 */
public class TestItem implements Parcelable {
    public static final Parcelable.Creator<TestItem> CREATOR = new Parcelable.Creator<TestItem>() {
        @Override
        public TestItem createFromParcel(Parcel in) {
            return new TestItem(in);
        }

        @Override
        public TestItem[] newArray(int size) {
            return new TestItem[size];
        }
    };

    private int cmdCode;
    private String desc;
    private String handleSuggest;

    public TestItem() {
    }

    protected TestItem(Parcel in) {
        cmdCode = in.readInt();
        desc = in.readString();
        handleSuggest = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(cmdCode);
        out.writeString(desc);
        out.writeString(handleSuggest);
    }

    public int getCmdCode() {
        return cmdCode;
    }

    public void setCmdCode(int cmdCode) {
        this.cmdCode = cmdCode;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getHandleSuggest() {
        return handleSuggest;
    }

    public void setHandleSuggest(String handleSuggest) {
        this.handleSuggest = handleSuggest;
    }
}