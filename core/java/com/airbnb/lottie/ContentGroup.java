package com.airbnb.lottie;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ContentGroup implements DrawingContent, PathContent, BaseKeyframeAnimation.AnimationListener {
    private static final String TAG = ContentGroup.class.getSimpleName();
    private final LottieDrawable lottieDrawable;
    private final String name;

    @Nullable
    private List<PathContent> pathContents;

    @Nullable
    private TransformKeyframeAnimation transformAnimation;
    private final Matrix matrix = new Matrix();
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final List<Content> contents = new ArrayList();

    ContentGroup(LottieDrawable lottieDrawable, BaseLayer baseLayer, ShapeGroup shapeGroup) {
        this.name = shapeGroup.getName();
        this.lottieDrawable = lottieDrawable;
        List<Object> items = shapeGroup.getItems();
        if (items.isEmpty()) {
            return;
        }
        Object obj = items.get(items.size() - 1);
        if (obj instanceof AnimatableTransform) {
            this.transformAnimation = ((AnimatableTransform) obj).createAnimation();
            this.transformAnimation.addAnimationsToLayer(baseLayer);
            this.transformAnimation.addListener(this);
        }
        for (int i = 0; i < items.size(); i++) {
            Object obj2 = items.get(i);
            if (obj2 instanceof ShapeFill) {
                this.contents.add(new FillContent(lottieDrawable, baseLayer, (ShapeFill) obj2));
            } else if (obj2 instanceof GradientFill) {
                this.contents.add(new GradientFillContent(lottieDrawable, baseLayer, (GradientFill) obj2));
            } else if (obj2 instanceof ShapeStroke) {
                this.contents.add(new StrokeContent(lottieDrawable, baseLayer, (ShapeStroke) obj2));
            } else if (obj2 instanceof GradientStroke) {
                this.contents.add(new GradientStrokeContent(lottieDrawable, baseLayer, (GradientStroke) obj2));
            } else if (obj2 instanceof ShapeGroup) {
                this.contents.add(new ContentGroup(lottieDrawable, baseLayer, (ShapeGroup) obj2));
            } else if (obj2 instanceof RectangleShape) {
                this.contents.add(new RectangleContent(lottieDrawable, baseLayer, (RectangleShape) obj2));
            } else if (obj2 instanceof CircleShape) {
                this.contents.add(new EllipseContent(lottieDrawable, baseLayer, (CircleShape) obj2));
            } else if (obj2 instanceof ShapePath) {
                this.contents.add(new ShapeContent(lottieDrawable, baseLayer, (ShapePath) obj2));
            } else if (obj2 instanceof PolystarShape) {
                this.contents.add(new PolystarContent(lottieDrawable, baseLayer, (PolystarShape) obj2));
            } else if (obj2 instanceof ShapeTrimPath) {
                this.contents.add(new TrimPathContent(baseLayer, (ShapeTrimPath) obj2));
            } else if (obj2 instanceof MergePaths) {
                if (lottieDrawable.enableMergePathsForKitKatAndAbove()) {
                    this.contents.add(new MergePathsContent((MergePaths) obj2));
                } else {
                    Log.w(TAG, "Animation contains merge paths but they are disabled.");
                }
            }
        }
        ArrayList arrayList = new ArrayList();
        MergePathsContent mergePathsContent = null;
        for (int size = this.contents.size() - 1; size >= 0; size--) {
            Content content = this.contents.get(size);
            mergePathsContent = content instanceof MergePathsContent ? (MergePathsContent) content : mergePathsContent;
            if (mergePathsContent != null && content != mergePathsContent) {
                mergePathsContent.addContentIfNeeded(content);
                arrayList.add(content);
            }
        }
        Iterator<Content> it = this.contents.iterator();
        while (it.hasNext()) {
            if (arrayList.contains(it.next())) {
                it.remove();
            }
        }
    }

    @Override
    public void addColorFilter(@Nullable String str, @Nullable String str2, @Nullable ColorFilter colorFilter) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.contents.size()) {
                return;
            }
            Content content = this.contents.get(i2);
            if (content instanceof DrawingContent) {
                DrawingContent drawingContent = (DrawingContent) content;
                if (str2 == null || str2.equals(content.getName())) {
                    drawingContent.addColorFilter(str, null, colorFilter);
                } else {
                    drawingContent.addColorFilter(str, str2, colorFilter);
                }
            }
            i = i2 + 1;
        }
    }

    @Override
    public void draw(Canvas canvas, Matrix matrix, int i) {
        this.matrix.set(matrix);
        if (this.transformAnimation != null) {
            this.matrix.preConcat(this.transformAnimation.getMatrix());
            i = (int) ((((this.transformAnimation.getOpacity().getValue().intValue() / 100.0f) * i) / 255.0f) * 255.0f);
        }
        for (int size = this.contents.size() - 1; size >= 0; size--) {
            Content content = this.contents.get(size);
            if (content instanceof DrawingContent) {
                ((DrawingContent) content).draw(canvas, this.matrix, i);
            }
        }
    }

    @Override
    public void getBounds(RectF rectF, Matrix matrix) {
        this.matrix.set(matrix);
        if (this.transformAnimation != null) {
            this.matrix.preConcat(this.transformAnimation.getMatrix());
        }
        this.rect.set(0.0f, 0.0f, 0.0f, 0.0f);
        for (int size = this.contents.size() - 1; size >= 0; size--) {
            Content content = this.contents.get(size);
            if (content instanceof DrawingContent) {
                ((DrawingContent) content).getBounds(this.rect, this.matrix);
                if (rectF.isEmpty()) {
                    rectF.set(this.rect);
                } else {
                    rectF.set(Math.min(rectF.left, this.rect.left), Math.min(rectF.top, this.rect.top), Math.max(rectF.right, this.rect.right), Math.max(rectF.bottom, this.rect.bottom));
                }
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Path getPath() {
        this.matrix.reset();
        if (this.transformAnimation != null) {
            this.matrix.set(this.transformAnimation.getMatrix());
        }
        this.path.reset();
        for (int size = this.contents.size() - 1; size >= 0; size--) {
            Content content = this.contents.get(size);
            if (content instanceof PathContent) {
                this.path.addPath(((PathContent) content).getPath(), this.matrix);
            }
        }
        return this.path;
    }

    List<PathContent> getPathList() {
        if (this.pathContents == null) {
            this.pathContents = new ArrayList();
            int i = 0;
            while (true) {
                int i2 = i;
                if (i2 >= this.contents.size()) {
                    break;
                }
                Content content = this.contents.get(i2);
                if (content instanceof PathContent) {
                    this.pathContents.add((PathContent) content);
                }
                i = i2 + 1;
            }
        }
        return this.pathContents;
    }

    Matrix getTransformationMatrix() {
        if (this.transformAnimation != null) {
            return this.transformAnimation.getMatrix();
        }
        this.matrix.reset();
        return this.matrix;
    }

    @Override
    public void onValueChanged() {
        this.lottieDrawable.invalidateSelf();
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
        ArrayList arrayList = new ArrayList(list.size() + this.contents.size());
        arrayList.addAll(list);
        for (int size = this.contents.size() - 1; size >= 0; size--) {
            Content content = this.contents.get(size);
            content.setContents(arrayList, this.contents.subList(0, size));
            arrayList.add(content);
        }
    }
}
