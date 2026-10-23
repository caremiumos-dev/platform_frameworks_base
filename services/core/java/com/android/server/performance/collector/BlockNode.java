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

import java.util.LinkedList;

/**
 * A node in the blocking ("caton") call-tree.
 *
 * @hide
 */
public class BlockNode implements Comparable<BlockNode> {
    private int count;
    private int level = -1;
    private LinkedList<BlockNode> nextNodes = new LinkedList<>();
    private BlockNode prevNode;
    private String stackLine;

    public BlockNode(int count, String stackLine, BlockNode prevNode) {
        this.prevNode = null;
        this.count = count;
        this.stackLine = stackLine;
        this.prevNode = prevNode;
    }

    @Override
    public int compareTo(@NonNull BlockNode other) {
        return Integer.compare(other.count, getCount());
    }

    public int getCount() {
        return this.count;
    }

    public int getLevel() {
        return this.level;
    }

    @NonNull
    public LinkedList<BlockNode> getNextNodes() {
        return this.nextNodes;
    }

    public BlockNode getPrevNode() {
        return this.prevNode;
    }

    public String getStackLine() {
        return this.stackLine;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setNextNodes(@NonNull LinkedList<BlockNode> nextNodes) {
        this.nextNodes = nextNodes;
    }

    public void setPrevNode(BlockNode prevNode) {
        this.prevNode = prevNode;
    }

    public void setStackLine(String stackLine) {
        this.stackLine = stackLine;
    }

    @Override
    public String toString() {
        return "BlockNode{level=" + this.level + ", count=" + this.count + ", stackLine='"
                + this.stackLine + "', nextSize="
                + (this.nextNodes == null ? 0 : this.nextNodes.size())
                + ", prev=" + this.prevNode.getStackLine() + ", next=" + this.nextNodes + '}';
    }
}