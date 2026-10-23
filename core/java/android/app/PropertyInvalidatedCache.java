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

package android.app;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.FastPrintWriter;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cache of values derived from a system property, invalidated when the property
 * value changes (a nonce is stored in the property).
 *
 * @param <Query> the query key type
 * @param <Result> the cached value type
 * @hide
 */
public abstract class PropertyInvalidatedCache<Query, Result> {
    private static final String TAG = "PropertyInvalidatedCache";
    private static final boolean DEBUG = false;
    private static final boolean VERIFY = false;
    private static final long NONCE_DISABLED = -1;
    private static final long NONCE_UNSET = 0;

    private final Object mLock = new Object();

    @GuardedBy("mLock")
    private final LinkedHashMap<Query, Result> mCache;

    @GuardedBy("mLock")
    private long mLastSeenNonce = NONCE_UNSET;

    @GuardedBy("mLock")
    private long mHits = 0;

    @GuardedBy("mLock")
    private long mMisses = 0;

    private final int mMaxEntries;
    private final String mPropertyName;
    private boolean mDisabled = false;
    private long mPropertyHandle;

    private static final Object sCorkLock = new Object();

    @GuardedBy("sCorkLock")
    private static final HashMap<String, Long> sInvalidates = new HashMap<>();

    @GuardedBy("sCorkLock")
    private static final HashMap<String, Integer> sCorks = new HashMap<>();

    @GuardedBy("sCorkLock")
    private static final WeakHashMap<PropertyInvalidatedCache, Void> sCaches =
            new WeakHashMap<>();

    private static boolean sEnabled = true;

    private static final class NoPreloadHolder {
        private static final AtomicLong sNextNonce = new AtomicLong(new Random().nextLong());

        private NoPreloadHolder() {
        }

        public static long next() {
            return sNextNonce.getAndIncrement();
        }
    }

    public PropertyInvalidatedCache(int maxEntries, String propertyName) {
        mPropertyName = propertyName;
        mMaxEntries = maxEntries;
        mCache = new LinkedHashMap<Query, Result>(2, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Query, Result> eldest) {
                return size() > maxEntries;
            }
        };
        synchronized (sCorkLock) {
            sCaches.put(this, null);
            sInvalidates.put(propertyName, 0L);
        }
    }

    public String cacheName() {
        return mPropertyName;
    }

    /**
     * Invalidates the cache of every {@link PropertyInvalidatedCache} that
     * shares the given property name.
     */
    public static void invalidateCache(String propertyName) {
        if (!sEnabled) {
            return;
        }
        synchronized (sCorkLock) {
            Integer corkCount = sCorks.get(propertyName);
            if (corkCount == null || corkCount <= 0) {
                invalidateCacheLocked(propertyName);
            }
        }
    }

    @GuardedBy("sCorkLock")
    private static void invalidateCacheLocked(String propertyName) {
        if (SystemProperties.getLong(propertyName, NONCE_UNSET) == NONCE_DISABLED) {
            return;
        }
        long nonce;
        do {
            nonce = NoPreloadHolder.next();
        } while (nonce == NONCE_UNSET || nonce == NONCE_DISABLED);
        SystemProperties.set(propertyName, Long.toString(nonce));
        sInvalidates.put(propertyName,
                sInvalidates.getOrDefault(propertyName, 0L) + 1);
    }

    public static void corkInvalidations(String propertyName) {
        synchronized (sCorkLock) {
            int corkCount = sCorks.getOrDefault(propertyName, 0);
            if (corkCount == 0) {
                long nonce = SystemProperties.getLong(propertyName, NONCE_UNSET);
                if (nonce != NONCE_UNSET && nonce != NONCE_DISABLED) {
                    SystemProperties.set(propertyName, Long.toString(NONCE_UNSET));
                }
            }
            sCorks.put(propertyName, corkCount + 1);
        }
    }

    public static void uncorkInvalidations(String propertyName) {
        synchronized (sCorkLock) {
            int corkCount = sCorks.getOrDefault(propertyName, 0);
            if (corkCount < 1) {
                throw new AssertionError("cork underflow: " + propertyName);
            }
            if (corkCount == 1) {
                sCorks.remove(propertyName);
                invalidateCacheLocked(propertyName);
            } else {
                sCorks.put(propertyName, corkCount - 1);
            }
        }
    }

    public static void disableForTestMode() {
        Log.d(TAG, "disabling all caches in the process");
        sEnabled = false;
    }

    public static void disableSystemWide(String propertyName) {
        if (sEnabled) {
            SystemProperties.set(propertyName, Long.toString(NONCE_DISABLED));
        }
    }

    public final void disableLocal() {
        synchronized (mLock) {
            mDisabled = true;
            mCache.clear();
        }
    }

    public final void disableSystemWide() {
        disableSystemWide(mPropertyName);
    }

    public final boolean isDisabledLocal() {
        return mDisabled || !sEnabled;
    }

    public final void invalidateCache() {
        invalidateCache(mPropertyName);
    }

    public final void clear() {
        synchronized (mLock) {
            mCache.clear();
        }
    }

    public Result query(Query query) {
        long currentNonce = isDisabledLocal() ? NONCE_DISABLED : getCurrentNonce();
        while (currentNonce != NONCE_DISABLED && currentNonce != NONCE_UNSET) {
            Result result;
            synchronized (mLock) {
                if (currentNonce == mLastSeenNonce) {
                    result = mCache.get(query);
                    if (result != null) {
                        mHits++;
                    }
                } else {
                    mCache.clear();
                    mLastSeenNonce = currentNonce;
                    result = null;
                }
            }
            if (result == null) {
                Result computed = recompute(query);
                synchronized (mLock) {
                    if (mLastSeenNonce == currentNonce && computed != null) {
                        mCache.put(query, computed);
                    }
                    mMisses++;
                }
                return maybeCheckConsistency(query, computed);
            }
            if (VERIFY && !debugCompareQueryResults(result, recompute(query))) {
                Log.wtf(TAG, "Computed value for " + cacheName() + " does not match cached value");
            }
            Result refreshed = refresh(result, query);
            if (refreshed == result) {
                return maybeCheckConsistency(query, result);
            }
            long newNonce = getCurrentNonce();
            if (currentNonce == newNonce) {
                synchronized (mLock) {
                    if (currentNonce == mLastSeenNonce) {
                        if (refreshed == null) {
                            mCache.remove(query);
                        } else {
                            mCache.put(query, refreshed);
                        }
                    }
                }
                return maybeCheckConsistency(query, refreshed);
            }
            currentNonce = newNonce;
        }
        return recompute(query);
    }

    private long getCurrentNonce() {
        return SystemProperties.getLong(mPropertyName, NONCE_UNSET);
    }

    public String queryToString(Query query) {
        return Objects.toString(query);
    }

    protected abstract Result recompute(Query query);

    protected Result refresh(Result result, Query query) {
        return result;
    }

    protected Result maybeCheckConsistency(Query query, Result result) {
        return result;
    }

    protected boolean debugCompareQueryResults(Result cachedResult, Result recomputedResult) {
        if (recomputedResult != null) {
            return Objects.equals(cachedResult, recomputedResult);
        }
        return true;
    }

    public static ArrayList<PropertyInvalidatedCache> getActiveCaches() {
        synchronized (sCorkLock) {
            return new ArrayList<>(sCaches.keySet());
        }
    }

    public static ArrayList<Map.Entry<String, Integer>> getActiveCorks() {
        synchronized (sCorkLock) {
            return new ArrayList<>(sCorks.entrySet());
        }
    }

    private void dumpContents(PrintWriter writer, String[] args) {
        long invalidates;
        synchronized (sCorkLock) {
            invalidates = sInvalidates.getOrDefault(mPropertyName, 0L);
        }
        synchronized (mLock) {
            writer.println(String.format("  Cache Property Name: %s", cacheName()));
            writer.println(String.format("    Hits: %d, Misses: %d, Invalidates: %d",
                    mHits, mMisses, invalidates));
            writer.println(String.format("    Last Observed Nonce: %d", mLastSeenNonce));
            writer.println(String.format("    Current Size: %d, Max Size: %d",
                    mCache.entrySet().size(), mMaxEntries));
            writer.println(String.format("    Enabled: %s", mDisabled ? "false" : "true"));
            Set<Map.Entry<Query, Result>> entries = mCache.entrySet();
            if (entries.isEmpty()) {
                writer.println("");
                return;
            }
            writer.println("");
            writer.println("    Contents:");
            for (Map.Entry<Query, Result> entry : entries) {
                writer.println(String.format("      Key: %s\n      Value: %s\n",
                        Objects.toString(entry.getKey()), Objects.toString(entry.getValue())));
            }
        }
    }

    public static void dumpCacheInfo(FileDescriptor fd, String[] args) {
        if (!sEnabled) {
            try (FileOutputStream fout = new FileOutputStream(fd);
                    FastPrintWriter writer = new FastPrintWriter(fout)) {
                writer.println("  Caching is disabled in this process.");
            } catch (IOException e) {
                Log.e(TAG, "Failed to dump PropertyInvalidatedCache instances");
            }
            return;
        }
        ArrayList<PropertyInvalidatedCache> activeCaches;
        synchronized (sCorkLock) {
            activeCaches = getActiveCaches();
            ArrayList<Map.Entry<String, Integer>> activeCorks = getActiveCorks();
            try (FileOutputStream fout = new FileOutputStream(fd);
                    FastPrintWriter writer = new FastPrintWriter(fout)) {
                if (activeCorks.size() > 0) {
                    writer.println("  Corking Status:");
                    for (int i = 0; i < activeCorks.size(); i++) {
                        Map.Entry<String, Integer> entry = activeCorks.get(i);
                        writer.println(String.format("    Property Name: %s Count: %d",
                                entry.getKey(), entry.getValue()));
                    }
                }
                for (int i = 0; i < activeCaches.size(); i++) {
                    activeCaches.get(i).dumpContents(writer, args);
                    writer.flush();
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to dump PropertyInvalidatedCache instances");
            }
        }
    }

    /**
     * Automatically corks invalidations and uncorks them after a delay.
     *
     * @hide
     */
    public static final class AutoCorker {
        public static final int DEFAULT_AUTO_CORK_DELAY_MS = 2000;

        private final Object mLock = new Object();
        private final String mPropertyName;
        private final int mAutoCorkDelayMs;

        @GuardedBy("mLock")
        private long mUncorkDeadlineMs = -1;

        @GuardedBy("mLock")
        private Handler mHandler;

        public AutoCorker(String propertyName) {
            this(propertyName, DEFAULT_AUTO_CORK_DELAY_MS);
        }

        public AutoCorker(String propertyName, int autoCorkDelayMs) {
            mPropertyName = propertyName;
            mAutoCorkDelayMs = autoCorkDelayMs;
        }

        public void autoCork() {
            if (Looper.getMainLooper() == null) {
                PropertyInvalidatedCache.invalidateCache(mPropertyName);
                return;
            }
            synchronized (mLock) {
                boolean previouslyCorked = mUncorkDeadlineMs >= 0;
                mUncorkDeadlineMs = SystemClock.uptimeMillis() + mAutoCorkDelayMs;
                if (!previouslyCorked) {
                    getHandlerLocked().sendEmptyMessageAtTime(0, mUncorkDeadlineMs);
                    PropertyInvalidatedCache.corkInvalidations(mPropertyName);
                }
            }
        }

        @GuardedBy("mLock")
        private Handler getHandlerLocked() {
            if (mHandler == null) {
                mHandler = new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        AutoCorker.this.handleMessage(msg);
                    }
                };
            }
            return mHandler;
        }

        private void handleMessage(Message msg) {
            synchronized (mLock) {
                if (mUncorkDeadlineMs < 0) {
                    return;
                }
                long nowMs = SystemClock.uptimeMillis();
                if (mUncorkDeadlineMs > nowMs) {
                    mUncorkDeadlineMs = nowMs + mAutoCorkDelayMs;
                    getHandlerLocked().sendEmptyMessageAtTime(0, mUncorkDeadlineMs);
                } else {
                    mUncorkDeadlineMs = -1;
                    PropertyInvalidatedCache.uncorkInvalidations(mPropertyName);
                }
            }
        }
    }
}