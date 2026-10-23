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

package com.android.server.os;

import android.text.TextUtils;
import android.util.Log;

import com.android.internal.os.IRegionalizationService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

/**
 * System service backing {@link com.android.internal.os.RegionalizationEnvironment}.
 * It provides file access for the carrier-package configuration.
 *
 * @hide
 */
public class RegionalizationService extends IRegionalizationService.Stub {
    private static final String TAG = "RegionalizationService";

    @Override
    public boolean checkFileExists(String path) {
        File file = new File(path);
        return file.exists();
    }

    @Override
    public void deleteFilesUnderDir(String dir, String extension, boolean deleteDir) {
        File file = new File(dir);
        if (!file.exists()) {
            return;
        }
        deleteFiles(file, extension, deleteDir);
    }

    private void deleteFiles(File file, String extension, boolean deleteDir) {
        if (!file.isDirectory()) {
            if (file.isFile() && (extension.isEmpty() || file.getName().endsWith(extension))) {
                file.delete();
            }
            return;
        }
        String[] children = file.list();
        if (children == null) {
            return;
        }
        for (String child : children) {
            deleteFiles(new File(file, child), extension, deleteDir);
        }
        if (deleteDir) {
            file.delete();
        }
    }

    @Override
    public ArrayList<String> readFile(String path, String regex) {
        File file = new File(path);
        if (!file.exists() || !file.canRead()) {
            return null;
        }
        ArrayList<String> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed == null) {
                    break;
                }
                if (TextUtils.isEmpty(regex)) {
                    result.add(trimmed);
                } else if (trimmed.matches(regex)) {
                    result.add(trimmed);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Read File error, caused by: " + e.getMessage());
        }
        return result;
    }

    @Override
    public boolean writeFile(String path, String content, boolean append) {
        File file = new File(path);
        if (!file.exists() || !file.canWrite()) {
            return false;
        }
        if (TextUtils.isEmpty(content)) {
            return false;
        }
        try (FileWriter writer = new FileWriter(file, append)) {
            writer.write(content);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}