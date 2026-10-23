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

package com.xtc.xtcdiag.socket;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Describes a file transfer operation requested by the PC-side diag tool.
 *
 * @hide
 */
public class XtcFileOperate implements Parcelable {
    public static final Parcelable.Creator<XtcFileOperate> CREATOR =
            new Parcelable.Creator<XtcFileOperate>() {
                @Override
                public XtcFileOperate createFromParcel(Parcel in) {
                    return new XtcFileOperate(in);
                }

                @Override
                public XtcFileOperate[] newArray(int size) {
                    return new XtcFileOperate[size];
                }
            };

    private String destFilePath;
    private String fileName;
    private int operateCode;

    protected XtcFileOperate(Parcel in) {
        destFilePath = in.readString();
        fileName = in.readString();
        operateCode = in.readInt();
    }

    public XtcFileOperate(String destFilePath, String fileName, int operateCode) {
        this.destFilePath = destFilePath;
        this.fileName = fileName;
        this.operateCode = operateCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(destFilePath);
        out.writeString(fileName);
        out.writeInt(operateCode);
    }

    public String getDestFilePath() {
        return destFilePath;
    }

    public void setDestFilePath(String destFilePath) {
        this.destFilePath = destFilePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getOperateCode() {
        return operateCode;
    }

    public void setOperateCode(int operateCode) {
        this.operateCode = operateCode;
    }

    @Override
    public String toString() {
        return "XtcFileOperate{destFilePath='" + destFilePath + '\''
                + ", fileName='" + fileName + '\''
                + ", operateCode=" + operateCode + '}';
    }
}