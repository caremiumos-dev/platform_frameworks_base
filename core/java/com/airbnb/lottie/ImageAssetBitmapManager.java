package com.airbnb.lottie;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.Drawable.Callback;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

class ImageAssetBitmapManager {

    @Nullable
    private ImageAssetDelegate assetDelegate;
    private final Map<String, Bitmap> bitmaps = new HashMap();
    private final Context context;
    private final Map<String, LottieImageAsset> imageAssets;
    private String imagesFolder;

    ImageAssetBitmapManager(Callback callback, String str, ImageAssetDelegate imageAssetDelegate, Map<String, LottieImageAsset> map) {
        this.imagesFolder = str;
        if (!TextUtils.isEmpty(str) && this.imagesFolder.charAt(this.imagesFolder.length() - 1) != '/') {
            this.imagesFolder += '/';
        }
        if (callback instanceof View) {
            this.context = ((View) callback).getContext();
            this.imageAssets = map;
            setAssetDelegate(imageAssetDelegate);
        } else {
            Log.w("LOTTIE", "LottieDrawable must be inside of a view for images to work.");
            this.imageAssets = new HashMap();
            this.context = null;
        }
    }

    @Nullable
    Bitmap bitmapForId(String str) {
        long jUptimeMillis = SystemClock.uptimeMillis();
        Bitmap bitmapDecodeStream = this.bitmaps.get(str);
        if (bitmapDecodeStream == null) {
            LottieImageAsset lottieImageAsset = this.imageAssets.get(str);
            if (lottieImageAsset == null) {
                return null;
            }
            if (this.assetDelegate != null) {
                Bitmap bitmapFetchBitmap = this.assetDelegate.fetchBitmap(lottieImageAsset);
                if (bitmapFetchBitmap != null) {
                    this.bitmaps.put(str, bitmapFetchBitmap);
                }
                return bitmapFetchBitmap;
            }
            try {
                if (TextUtils.isEmpty(this.imagesFolder)) {
                    throw new IllegalStateException("You must set an images folder before loading an image. Set it with LottieComposition#setImagesFolder or LottieDrawable#setImagesFolder");
                }
                InputStream inputStreamOpen = this.context.getAssets().open(this.imagesFolder + lottieImageAsset.getFileName());
                Options options = new Options();
                options.inScaled = true;
                options.inDensity = 160;
                bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpen, null, options);
                this.bitmaps.put(str, bitmapDecodeStream);
            } catch (IOException e) {
                Log.w("LOTTIE", "Unable to open asset.", e);
                return null;
            }
        }
        long jUptimeMillis2 = SystemClock.uptimeMillis() - jUptimeMillis;
        if (jUptimeMillis2 > 10) {
            Log.w("Lottie", "load bitmap too slow " + str + " cost:" + jUptimeMillis2);
        }
        return bitmapDecodeStream;
    }

    boolean hasSameContext(Context context) {
        if (context == null && this.context == null) {
            return true;
        }
        if (context != null) {
            return this.context.equals(context);
        }
        return false;
    }

    void recycleBitmaps() {
        Iterator<Entry<String, Bitmap>> it = this.bitmaps.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().recycle();
            it.remove();
        }
    }

    void setAssetDelegate(@Nullable ImageAssetDelegate imageAssetDelegate) {
        this.assetDelegate = imageAssetDelegate;
    }

    @Nullable
    Bitmap updateBitmap(String str, @Nullable Bitmap bitmap) {
        return this.bitmaps.put(str, bitmap);
    }
}
