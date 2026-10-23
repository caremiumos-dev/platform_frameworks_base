package com.airbnb.lottie;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.support.v4.util.LongSparseArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class CanvasPool {
    private final LongSparseArray<List<Bitmap>> availableBitmaps = new LongSparseArray<>();
    private final Map<BitmapCanvas, Bitmap> canvasBitmapMap = new HashMap();
    private final Map<Bitmap, BitmapCanvas> bitmapCanvasMap = new HashMap();

    CanvasPool() {
    }

    private int getKey(int i, int i2, Config config) {
        return ((i & 65535) << 17) | ((65535 & i2) << 1) | (config.ordinal() & 1);
    }

    private int getKey(Bitmap bitmap) {
        return getKey(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
    }

    BitmapCanvas acquire(int i, int i2, Config config) {
        BitmapCanvas bitmapCanvas;
        int key = getKey(i, i2, config);
        List<Bitmap> arrayList = this.availableBitmaps.get(key);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.availableBitmaps.put(key, arrayList);
        }
        if (arrayList.isEmpty()) {
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(i, i2, config);
            bitmapCanvas = new BitmapCanvas(bitmapCreateBitmap);
            this.canvasBitmapMap.put(bitmapCanvas, bitmapCreateBitmap);
            this.bitmapCanvasMap.put(bitmapCreateBitmap, bitmapCanvas);
        } else {
            bitmapCanvas = this.bitmapCanvasMap.get(arrayList.remove(0));
        }
        bitmapCanvas.getBitmap().eraseColor(0);
        return bitmapCanvas;
    }

    void recycleBitmaps() {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.availableBitmaps.size()) {
                break;
            }
            Iterator<Bitmap> it = this.availableBitmaps.valueAt(i2).iterator();
            while (it.hasNext()) {
                Bitmap next = it.next();
                BitmapCanvas bitmapCanvas = this.bitmapCanvasMap.get(next);
                this.bitmapCanvasMap.remove(next);
                this.canvasBitmapMap.remove(bitmapCanvas);
                next.recycle();
                it.remove();
            }
            i = i2 + 1;
        }
        if (!this.bitmapCanvasMap.isEmpty()) {
            throw new IllegalStateException("Not all canvases have been released!");
        }
    }

    void release(BitmapCanvas bitmapCanvas) {
        Bitmap bitmap = this.canvasBitmapMap.get(bitmapCanvas);
        List<Bitmap> list = this.availableBitmaps.get(getKey(bitmap));
        if (list.contains(bitmap)) {
            throw new IllegalStateException("Canvas already released.");
        }
        list.add(bitmap);
    }
}
