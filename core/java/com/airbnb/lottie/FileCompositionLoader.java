package com.airbnb.lottie;

import android.content.res.Resources;
import java.io.InputStream;

final class FileCompositionLoader extends CompositionLoader<InputStream> {
    private final OnCompositionLoadedListener loadedListener;
    private final Resources res;

    FileCompositionLoader(Resources resources, OnCompositionLoadedListener onCompositionLoadedListener) {
        this.res = resources;
        this.loadedListener = onCompositionLoadedListener;
    }

    @Override
    public LottieComposition doInBackground(InputStream... inputStreamArr) {
        return LottieComposition.Factory.fromInputStream(this.res, inputStreamArr[0]);
    }

    @Override
    public void onPostExecute(LottieComposition lottieComposition) {
        this.loadedListener.onCompositionLoaded(lottieComposition);
    }
}
