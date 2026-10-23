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

package com.android.server.performance.policy.ram;

import android.util.Log;

import com.android.server.performance.PolicyConfig;

import java.io.PrintWriter;
import java.util.HashMap;

/**
 * Maps a memory "scene" to its policy action.
 *
 * @hide
 */
public class MemoryScene {
    public static final String TAG_NAME = "MemoryScene";

    public static final String SCENE_IDLE = "idle";
    public static final String SCENE_DEF = "default";
    public static final String SCENE_BIG_MEM = "BigMem";
    public static final String SCENE_EMERGENCY = "Emergency";

    public static final String POLICY_KILL = "kill";
    public static final String POLICY_QUICK_KILL = "quickkill";
    public static final String POLICY_RECLAIM = "reclaim";
    public static final String POLILCY_RECLAIM_PERSIST = "reclaimPersist";

    private static final String TAG = "xtcperformance-Ram";
    private static final String ATTR_NAME = "name";
    private static final String TAG_SCENE = "scene";
    private static final String TAG_ACTION = "action";

    HashMap<String, String> mSceneMap = new HashMap<>();

    public static MemoryScene loadFromConfig(PolicyConfig policyConfig) {
        MemoryScene scene = new MemoryScene();
        PolicyConfig.ConfigItem idle = policyConfig.getConfigItem(SCENE_IDLE);
        PolicyConfig.ConfigItem def = policyConfig.getConfigItem(SCENE_DEF);
        PolicyConfig.ConfigItem bigMem = policyConfig.getConfigItem(SCENE_BIG_MEM);
        PolicyConfig.ConfigItem emergency = policyConfig.getConfigItem(SCENE_EMERGENCY);
        if (idle != null && def != null && bigMem != null && emergency != null) {
            String idleAction = policyConfig.getConfigItem(SCENE_IDLE).getString(TAG_ACTION);
            String defAction = policyConfig.getConfigItem(SCENE_DEF).getString(TAG_ACTION);
            String bigMemAction = policyConfig.getConfigItem(SCENE_BIG_MEM).getString(TAG_ACTION);
            String emergencyAction = policyConfig.getConfigItem(SCENE_EMERGENCY)
                    .getString(TAG_ACTION);
            if (idleAction != null && defAction != null && bigMemAction != null
                    && emergencyAction != null) {
                scene.mSceneMap.put(SCENE_IDLE, idleAction);
                scene.mSceneMap.put(SCENE_DEF, defAction);
                scene.mSceneMap.put(SCENE_BIG_MEM, bigMemAction);
                scene.mSceneMap.put(SCENE_EMERGENCY, emergencyAction);
                return scene;
            }
        }
        return null;
    }

    public void dump() {
        if (this.mSceneMap != null) {
            for (String scene : this.mSceneMap.keySet()) {
                String policy = this.mSceneMap.get(scene);
                if (policy != null) {
                    Log.e(TAG, TAG_SCENE + scene + " --> policy:" + policy);
                }
            }
        }
    }

    public void dump(PrintWriter writer) {
        writer.println("Dump MemoryScene:");
        writer.println("----------------------------");
        if (this.mSceneMap != null) {
            for (String scene : this.mSceneMap.keySet()) {
                String policy = this.mSceneMap.get(scene);
                if (policy != null) {
                    writer.println(TAG_SCENE + scene + " --> policy:" + policy);
                }
            }
        }
        writer.println("----------------------------");
    }

    public String getPolicyForScene(String scene) {
        return this.mSceneMap.get(scene);
    }
}