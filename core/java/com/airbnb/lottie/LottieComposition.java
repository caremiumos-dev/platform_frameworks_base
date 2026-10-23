package com.airbnb.lottie;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LottieComposition {
    private final Rect bounds;
    private final float dpScale;
    private final long endFrame;
    private final int frameRate;
    private final Map<String, LottieImageAsset> images;
    private final LongSparseArray<Layer> layerMap;
    private final List<Layer> layers;
    private final Map<String, List<Layer>> precomps;
    private final long startFrame;

    public static class Factory {
        private Factory() {
        }

        private static void addLayer(List<Layer> list, LongSparseArray<Layer> longSparseArray, Layer layer) {
            list.add(layer);
            longSparseArray.put(layer.getId(), layer);
        }

        public static Cancellable fromAssetFileName(Context context, String str, OnCompositionLoadedListener onCompositionLoadedListener) {
            try {
                return fromInputStream(context, context.getAssets().open(str), onCompositionLoadedListener);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to find file " + str, e);
            }
        }

        public static LottieComposition fromFileSync(Context context, String str) {
            try {
                return fromInputStream(context.getResources(), context.getAssets().open(str));
            } catch (IOException e) {
                throw new IllegalStateException("Unable to find file " + str, e);
            }
        }

        public static Cancellable fromInputStream(Context context, InputStream inputStream, OnCompositionLoadedListener onCompositionLoadedListener) {
            FileCompositionLoader fileCompositionLoader = new FileCompositionLoader(context.getResources(), onCompositionLoadedListener);
            fileCompositionLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new InputStream[]{inputStream});
            return fileCompositionLoader;
        }

        @Nullable
        static LottieComposition fromInputStream(Resources resources, InputStream inputStream) {
            try {
                byte[] bArr = new byte[inputStream.available()];
                inputStream.read(bArr);
                return fromJsonSync(resources, new JSONObject(new String(bArr, "UTF-8")));
            } catch (IOException e) {
                Log.e("LOTTIE", "Failed to load composition.", new IllegalStateException("Unable to find file.", e));
                return null;
            } catch (JSONException e2) {
                Log.e("LOTTIE", "Failed to load composition.", new IllegalStateException("Unable to load JSON.", e2));
                return null;
            } finally {
                Utils.closeQuietly(inputStream);
            }
        }

        public static Cancellable fromJson(Resources resources, JSONObject jSONObject, OnCompositionLoadedListener onCompositionLoadedListener) {
            JsonCompositionLoader jsonCompositionLoader = new JsonCompositionLoader(resources, onCompositionLoadedListener);
            jsonCompositionLoader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new JSONObject[]{jSONObject});
            return jsonCompositionLoader;
        }

        static LottieComposition fromJsonSync(Resources resources, JSONObject jSONObject) {
            LottieComposition lottieComposition = null;
            float f = resources.getDisplayMetrics().density;
            int iOptInt = jSONObject.optInt("w", -1);
            int iOptInt2 = jSONObject.optInt("h", -1);
            LottieComposition lottieComposition2 = new LottieComposition((iOptInt == -1 || iOptInt2 == -1) ? null : new Rect(0, 0, (int) (iOptInt * f), (int) (iOptInt2 * f)), jSONObject.optLong("ip", 0L), jSONObject.optLong("op", 0L), jSONObject.optInt("fr", 0), f, lottieComposition);
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("assets");
            parseImages(jSONArrayOptJSONArray, lottieComposition2);
            parsePrecomps(jSONArrayOptJSONArray, lottieComposition2);
            parseLayers(jSONObject, lottieComposition2);
            return lottieComposition2;
        }

        private static void parseImages(@Nullable JSONArray jSONArray, LottieComposition lottieComposition) {
            if (jSONArray == null) {
                return;
            }
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                if (jSONObjectOptJSONObject.has("p")) {
                    LottieImageAsset lottieImageAssetNewInstance = LottieImageAsset.Factory.newInstance(jSONObjectOptJSONObject);
                    lottieComposition.images.put(lottieImageAssetNewInstance.getId(), lottieImageAssetNewInstance);
                }
            }
        }

        private static void parseLayers(JSONObject jSONObject, LottieComposition lottieComposition) {
            JSONArray jSONArrayOptJSONArray = jSONObject.optJSONArray("layers");
            if (jSONArrayOptJSONArray == null) {
                return;
            }
            int length = jSONArrayOptJSONArray.length();
            for (int i = 0; i < length; i++) {
                addLayer(lottieComposition.layers, lottieComposition.layerMap, Layer.Factory.newInstance(jSONArrayOptJSONArray.optJSONObject(i), lottieComposition));
            }
        }

        private static void parsePrecomps(@Nullable JSONArray jSONArray, LottieComposition lottieComposition) {
            if (jSONArray == null) {
                return;
            }
            int length = jSONArray.length();
            for (int i = 0; i < length; i++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i);
                JSONArray jSONArrayOptJSONArray = jSONObjectOptJSONObject.optJSONArray("layers");
                if (jSONArrayOptJSONArray != null) {
                    ArrayList arrayList = new ArrayList(jSONArrayOptJSONArray.length());
                    LongSparseArray longSparseArray = new LongSparseArray();
                    for (int i2 = 0; i2 < jSONArrayOptJSONArray.length(); i2++) {
                        Layer layerNewInstance = Layer.Factory.newInstance(jSONArrayOptJSONArray.optJSONObject(i2), lottieComposition);
                        longSparseArray.put(layerNewInstance.getId(), layerNewInstance);
                        arrayList.add(layerNewInstance);
                    }
                    lottieComposition.precomps.put(jSONObjectOptJSONObject.optString("id"), arrayList);
                }
            }
        }
    }

    private LottieComposition(Rect rect, long j, long j2, int i, float f) {
        this.precomps = new HashMap();
        this.images = new HashMap();
        this.layerMap = new LongSparseArray<>();
        this.layers = new ArrayList();
        this.bounds = rect;
        this.startFrame = j;
        this.endFrame = j2;
        this.frameRate = i;
        this.dpScale = f;
    }

     LottieComposition(Rect rect, long j, long j2, int i, float f, LottieComposition lottieComposition) {
        this(rect, j, j2, i, f);
    }

    public Rect getBounds() {
        return this.bounds;
    }

    public float getDpScale() {
        return this.dpScale;
    }

    public long getDuration() {
        return (long) (((this.endFrame - this.startFrame) / this.frameRate) * 1000.0f);
    }

    float getDurationFrames() {
        return (getDuration() * this.frameRate) / 1000.0f;
    }

    long getEndFrame() {
        return this.endFrame;
    }

    Map<String, LottieImageAsset> getImages() {
        return this.images;
    }

    List<Layer> getLayers() {
        return this.layers;
    }

    @Nullable
    List<Layer> getPrecomps(String str) {
        return this.precomps.get(str);
    }

    public boolean hasImages() {
        return !this.images.isEmpty();
    }

    Layer layerModelForId(long j) {
        return this.layerMap.get(j);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("LottieComposition:\n");
        Iterator<Layer> it = this.layers.iterator();
        while (it.hasNext()) {
            sb.append(((Layer) it.next()).toString("\t"));
        }
        return sb.toString();
    }
}
