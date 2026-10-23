package com.airbnb.lottie;

import android.content.res.Resources;
import org.json.JSONObject;

final class JsonCompositionLoader extends CompositionLoader<JSONObject> {
    private final OnCompositionLoadedListener loadedListener;
    private final Resources res;

    JsonCompositionLoader(Resources resources, OnCompositionLoadedListener onCompositionLoadedListener) {
        this.res = resources;
        this.loadedListener = onCompositionLoadedListener;
    }

    @Override
    public LottieComposition doInBackground(JSONObject... jSONObjectArr) {
        return LottieComposition.Factory.fromJsonSync(this.res, jSONObjectArr[0]);
    }

    @Override
    public void onPostExecute(LottieComposition lottieComposition) {
        this.loadedListener.onCompositionLoaded(lottieComposition);
    }
}
