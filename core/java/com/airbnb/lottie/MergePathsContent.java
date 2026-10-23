package com.airbnb.lottie;

import android.annotation.TargetApi;
import android.graphics.Path;
import android.graphics.Path.Op;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

@TargetApi(19)
class MergePathsContent implements PathContent {

    private static int[] f15comairbnblottieMergePaths$MergePathsModeSwitchesValues = null;
    private final MergePaths mergePaths;
    private final String name;
    private final Path firstPath = new Path();
    private final Path remainderPath = new Path();
    private final Path path = new Path();
    private final List<PathContent> pathContents = new ArrayList();

    private static int[] m285getcomairbnblottieMergePaths$MergePathsModeSwitchesValues() {
        if (f15comairbnblottieMergePaths$MergePathsModeSwitchesValues != null) {
            return f15comairbnblottieMergePaths$MergePathsModeSwitchesValues;
        }
        int[] iArr = new int[MergePaths.MergePathsMode.valuesCustom().length];
        try {
            iArr[MergePaths.MergePathsMode.Add.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[MergePaths.MergePathsMode.ExcludeIntersections.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[MergePaths.MergePathsMode.Intersect.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[MergePaths.MergePathsMode.Merge.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[MergePaths.MergePathsMode.Subtract.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        f15comairbnblottieMergePaths$MergePathsModeSwitchesValues = iArr;
        return iArr;
    }

    MergePathsContent(MergePaths mergePaths) {
        if (Build.VERSION.SDK_INT < 19) {
            throw new IllegalStateException("Merge paths are not supported pre-KitKat.");
        }
        this.name = mergePaths.getName();
        this.mergePaths = mergePaths;
    }

    private void addPaths() {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.pathContents.size()) {
                return;
            }
            this.path.addPath(this.pathContents.get(i2).getPath());
            i = i2 + 1;
        }
    }

    @TargetApi(19)
    private void opFirstPathWithRest(Op op) {
        this.remainderPath.reset();
        this.firstPath.reset();
        int size = this.pathContents.size() - 1;
        while (true) {
            int i = size;
            if (i < 1) {
                break;
            }
            PathContent pathContent = this.pathContents.get(i);
            if (pathContent instanceof ContentGroup) {
                List<PathContent> pathList = ((ContentGroup) pathContent).getPathList();
                for (int size2 = pathList.size() - 1; size2 >= 0; size2--) {
                    Path path = pathList.get(size2).getPath();
                    path.transform(((ContentGroup) pathContent).getTransformationMatrix());
                    this.remainderPath.addPath(path);
                }
            } else {
                this.remainderPath.addPath(pathContent.getPath());
            }
            size = i - 1;
        }
        PathContent pathContent2 = this.pathContents.get(0);
        if (pathContent2 instanceof ContentGroup) {
            List<PathContent> pathList2 = ((ContentGroup) pathContent2).getPathList();
            for (int i2 = 0; i2 < pathList2.size(); i2++) {
                Path path2 = pathList2.get(i2).getPath();
                path2.transform(((ContentGroup) pathContent2).getTransformationMatrix());
                this.firstPath.addPath(path2);
            }
        } else {
            this.firstPath.set(pathContent2.getPath());
        }
        this.path.op(this.firstPath, this.remainderPath, op);
    }

    void addContentIfNeeded(Content content) {
        if (content instanceof PathContent) {
            this.pathContents.add((PathContent) content);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Path getPath() {
        this.path.reset();
        switch (m285getcomairbnblottieMergePaths$MergePathsModeSwitchesValues()[this.mergePaths.getMode().ordinal()]) {
            case 1:
                opFirstPathWithRest(Op.UNION);
                break;
            case 2:
                opFirstPathWithRest(Op.XOR);
                break;
            case 3:
                opFirstPathWithRest(Op.INTERSECT);
                break;
            case 4:
                addPaths();
                break;
            case 5:
                opFirstPathWithRest(Op.REVERSE_DIFFERENCE);
                break;
        }
        return this.path;
    }

    @Override
    public void setContents(List<Content> list, List<Content> list2) {
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= this.pathContents.size()) {
                return;
            }
            this.pathContents.get(i2).setContents(list, list2);
            i = i2 + 1;
        }
    }
}
