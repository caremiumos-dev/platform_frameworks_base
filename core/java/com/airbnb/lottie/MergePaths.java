package com.airbnb.lottie;

import org.json.JSONObject;

public class MergePaths {
    private final MergePaths.MergePathsMode mode;
    private final String name;

    static class Factory {
        private Factory() {
        }

        static MergePaths newInstance(JSONObject jSONObject) {
            return new MergePaths(jSONObject.optString("nm"), MergePaths.MergePathsMode.forId(jSONObject.optInt("mm", 1)), null);
        }
    }

    enum MergePathsMode {
        Merge,
        Add,
        Subtract,
        Intersect,
        ExcludeIntersections;

        public static MergePaths.MergePathsMode forId(int i) {
            switch (i) {
                case 1:
                    return Merge;
                case 2:
                    return Add;
                case 3:
                    return Subtract;
                case 4:
                    return Intersect;
                case 5:
                    return ExcludeIntersections;
                default:
                    return Merge;
            }
        }

        public static MergePaths.MergePathsMode[] valuesCustom() {
            return values();
        }
    }

    private MergePaths(String str, MergePaths.MergePathsMode mergePathsMode) {
        this.name = str;
        this.mode = mergePathsMode;
    }

     MergePaths(String str, MergePaths.MergePathsMode mergePathsMode, MergePaths mergePaths) {
        this(str, mergePathsMode);
    }

    MergePaths.MergePathsMode getMode() {
        return this.mode;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return "MergePaths{mode=" + this.mode + '}';
    }
}
