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

package android.support.v4.util;

/**
 * Compatibility alias for {@link android.util.LongSparseArray} kept for the
 * bundled support library sources.
 *
 * @hide
 */
public class LongSparseArray<E> implements Cloneable {
    private android.util.LongSparseArray<E> mStorage;

    public LongSparseArray() {
        this.mStorage = new android.util.LongSparseArray<E>();
    }

    public LongSparseArray(int initialCapacity) {
        this.mStorage = new android.util.LongSparseArray<E>(initialCapacity);
    }

    @Override
    public LongSparseArray<E> clone() {
        LongSparseArray<E> copy = new LongSparseArray<E>();
        copy.mStorage = this.mStorage.clone();
        return copy;
    }

    public void put(long key, E value) {
        this.mStorage.put(key, value);
    }

    public void append(long key, E value) {
        this.mStorage.append(key, value);
    }

    public E get(long key) {
        return this.mStorage.get(key);
    }

    public E get(long key, E valueIfKeyNotFound) {
        return this.mStorage.get(key, valueIfKeyNotFound);
    }

    public void delete(long key) {
        this.mStorage.delete(key);
    }

    public void remove(long key) {
        this.mStorage.remove(key);
    }

    public void removeAt(int index) {
        this.mStorage.removeAt(index);
    }

    public int size() {
        return this.mStorage.size();
    }

    public long keyAt(int index) {
        return this.mStorage.keyAt(index);
    }

    public E valueAt(int index) {
        return this.mStorage.valueAt(index);
    }

    public int indexOfKey(long key) {
        return this.mStorage.indexOfKey(key);
    }

    public int indexOfValue(E value) {
        return this.mStorage.indexOfValue(value);
    }

    public void clear() {
        this.mStorage.clear();
    }
}