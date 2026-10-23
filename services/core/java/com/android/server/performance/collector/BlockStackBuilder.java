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

package com.android.server.performance.collector;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Builds and merges a call-tree of blocking ("caton") stack traces.
 *
 * @hide
 */
public class BlockStackBuilder {
    public static final String TAG = "BlockStackBuilder";
    public static final int MAX_LOG_LEVEL = 20;
    public static final String ROOT_LINE = "java.lang.reflect.Method.invoke(Native Method)";

    @Nullable
    public static BlockNode mainNode = null;

    public static void blockNodeList2StringList(@NonNull BlockNode node,
            @NonNull java.util.ArrayList<String> out) {
        StringBuilder builder = new StringBuilder();
        int level = node.getLevel();
        int count = node.getCount();
        int i = 0;
        BlockNode prev = node;
        while (prev != null && prev.getPrevNode() != null) {
            LinkedList<BlockNode> nextNodes = prev.getPrevNode().getNextNodes();
            int size = nextNodes.size();
            if (size <= 1) {
                builder.insert(0, "   ");
            } else if (i == 0) {
                builder.insert(0, "|__");
            } else if (nextNodes.get(size - 1).getStackLine().equals(prev.getStackLine())) {
                builder.insert(0, "   ");
            } else {
                builder.insert(0, "|  ");
            }
            prev = prev.getPrevNode();
            i++;
        }
        float percent = (count / prev.getCount()) * 100.0f;
        float rounded = Math.round(percent * 100.0f) / 100.0f;
        out.add(builder.toString() + "(L" + level + ", " + rounded + "%, " + node.getCount()
                + "次) " + node.getStackLine() + '\n');
        LinkedList<BlockNode> nextNodes = node.getNextNodes();
        if (nextNodes.isEmpty()) {
            return;
        }
        for (BlockNode child : nextNodes) {
            if (child.getLevel() <= MAX_LOG_LEVEL) {
                blockNodeList2StringList(child, out);
            }
        }
    }

    @NonNull
    public static synchronized BlockNode buildBlockNode(@NonNull List<String> lines) {
        Collections.reverse(lines);
        int size = lines.size();
        boolean foundRoot = false;
        BlockNode root = null;
        BlockNode current = null;
        for (int i = 0; i < size; i++) {
            String line = lines.get(i);
            if (!foundRoot) {
                if (!line.startsWith(ROOT_LINE)) {
                    continue;
                }
                foundRoot = true;
            }
            if (current == null) {
                root = new BlockNode(1, line, null);
                root.setLevel(1);
                current = root;
            } else {
                BlockNode child = new BlockNode(1, line, current);
                child.setLevel(current.getLevel() + 1);
                LinkedList<BlockNode> next = new LinkedList<>();
                next.add(child);
                current.setNextNodes(next);
                current = child;
            }
        }
        if (root == null) {
            throw new IllegalArgumentException("size is " + size);
        }
        return root;
    }

    @NonNull
    public static BlockNode merge(@Nullable BlockNode main, @NonNull BlockNode other) {
        if (main == null) {
            return other;
        }
        realMergeNode(main, other);
        return main;
    }

    private static void realMergeNode(BlockNode node, BlockNode other) {
        String otherLine = other.getStackLine();
        String line = node.getStackLine();
        LinkedList<BlockNode> next = node.getNextNodes();
        if (!otherLine.equals(line)) {
            node.getNextNodes().add(other);
            other.setPrevNode(node);
            return;
        }
        node.setCount(node.getCount() + 1);
        LinkedList<BlockNode> otherNext = other.getNextNodes();
        if (next.size() == 0 && otherNext.size() > 0) {
            BlockNode child = otherNext.get(0);
            child.setPrevNode(node);
            node.getNextNodes().add(child);
            Collections.sort(node.getNextNodes());
            return;
        }
        if (next.size() <= 0 || otherNext.size() <= 0) {
            return;
        }
        BlockNode otherFirst = otherNext.get(0);
        BlockNode matched = null;
        boolean found = false;
        for (BlockNode candidate : next) {
            if (candidate.getStackLine().equals(otherFirst.getStackLine())) {
                matched = candidate;
                found = true;
                break;
            }
        }
        if (found) {
            realMergeNode(matched, otherFirst);
            return;
        }
        node.getNextNodes().add(otherFirst);
        Collections.sort(node.getNextNodes());
        otherFirst.setPrevNode(node);
    }
}