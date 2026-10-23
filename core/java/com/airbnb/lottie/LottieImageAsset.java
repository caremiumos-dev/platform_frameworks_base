package com.airbnb.lottie;

import org.json.JSONObject;

public class LottieImageAsset {
    private final String fileName;
    private final int height;
    private final String id;
    private final int width;

    static class Factory {
        private Factory() {
        }

        static LottieImageAsset newInstance(JSONObject jSONObject) {
            return new LottieImageAsset(jSONObject.optInt("w"), jSONObject.optInt("h"), jSONObject.optString("id"), jSONObject.optString("p"), null);
        }
    }

    private LottieImageAsset(int i, int i2, String str, String str2) {
        this.width = i;
        this.height = i2;
        this.id = str;
        this.fileName = str2;
    }

     LottieImageAsset(int i, int i2, String str, String str2, LottieImageAsset lottieImageAsset) {
        this(i, i2, str, str2);
    }

    public String getFileName() {
        return this.fileName;
    }

    public int getHeight() {
        return this.height;
    }

    public String getId() {
        return this.id;
    }

    public int getWidth() {
        return this.width;
    }
}
