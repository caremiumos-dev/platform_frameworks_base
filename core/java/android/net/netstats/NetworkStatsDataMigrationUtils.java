/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net.netstats;

import static android.net.ConnectivityManager.TYPE_MOBILE;
import static android.net.NetworkStats.SET_ALL;
import static android.net.NetworkStats.TAG_NONE;
import static android.net.NetworkStats.UID_ALL;
import static android.net.NetworkStatsCollection.VERSION_UNIFIED_INIT;
import static android.net.NetworkStatsHistory.DataStreamUtils.readFullLongArray;
import static android.net.NetworkStatsHistory.DataStreamUtils.readVarLongArray;

import static com.android.internal.util.ArrayUtils.total;

import android.annotation.NonNull;
import android.net.Key;
import android.net.NetworkIdentity;
import android.net.NetworkIdentitySet;
import android.net.NetworkStatsCollection;
import android.net.NetworkStatsHistory;
import android.os.Environment;
import android.util.AtomicFile;

import libcore.io.IoUtils;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ProtocolException;

/**
 * Helper class to read old version of persistent network statistics, the implementation is
 * intended to be modified by OEM partners to accommodate their custom changes.
 * @hide
 */
// TODO: Expose as @SystemApi when ready.
public class NetworkStatsDataMigrationUtils {
    /**
     * File header magic number: "ANET". The definition is copied from NetworkStatsCollection,
     * but it is fine for OEM to re-define to their own value to adopt the legacy file reading
     * logic.
     */
    private static final int FILE_MAGIC = 0x414E4554;

    // Used by the NetworkStatsService to read files at /data/system/netstats_*.bin.
    public static @NonNull File getPlatformSystemDir() {
        return new File(Environment.getDataDirectory(), "system");
    }

    // Used by the NetworkStatsService to read files at /data/system/netstats/<tag>.<start>-<end>.
    public static @NonNull File getPlatformBaseDir() {
        File baseDir = new File(getPlatformSystemDir(), "netstats");
        baseDir.mkdirs();
        return baseDir;
    }

    /**
     * Helper function to read old version of NetworkStatsCollections that resided in the platform.
     */
    public static void readPlatformCollection(@NonNull NetworkStatsCollection collection,
            @NonNull DataInput in, int version)
            throws IOException {
        // For any object that is greater than or equal to VERSION_MAINLINED_STATS, the service
        // will not call into this class.
        switch (version) {
            case VERSION_UNIFIED_INIT: {
                // uid := size *(NetworkIdentitySet size *(uid set tag NetworkStatsHistory))
                final int identSize = in.readInt();
                for (int i = 0; i < identSize; i++) {
                    final NetworkIdentitySet ident = readPlatformNetworkIdentitySet(in);

                    final int size = in.readInt();
                    for (int j = 0; j < size; j++) {
                        final int uid = in.readInt();
                        final int set = in.readInt();
                        final int tag = in.readInt();

                        final NetworkStatsCollection.Key key = new NetworkStatsCollection.Key(ident,
                                uid, set, tag);
                        final NetworkStatsHistory history = readPlatformHistory(in);
                        collection.recordHistory(key, history);
                    }
                }
                break;
            }
            default: {
                throw new ProtocolException("unexpected version: " + version);
            }
        }
    }

    // Copied from NetworkStatsHistory#DataStreamUtils.
    private static long[] readFullLongArray(DataInput in) throws IOException {
        final int size = in.readInt();
        if (size < 0) throw new ProtocolException("negative array size");
        final long[] values = new long[size];
        for (int i = 0; i < values.length; i++) {
            values[i] = in.readLong();
        }
        return values;
    }

    // Copied from NetworkIdentitySet.
    private static String readOptionalString(DataInput in) throws IOException {
        if (in.readByte() != 0) {
            return in.readUTF();
        } else {
            return null;
        }
    }

    /**
     * This is copied from NetworkStatsHistory#NetworkStatsHistory(DataInput in). But it is fine
     * for OEM to re-write the logic to adopt the legacy file reading.
     */
    @NonNull
    private static NetworkStatsHistory readPlatformHistory(@NonNull DataInput in)
            throws IOException {
        final long bucketDuration;
        final long[] bucketStart;
        final long[] rxBytes;
        final long[] rxPackets;
        final long[] txBytes;
        final long[] txPackets;
        final long[] operations;
        final int bucketCount;
        final long totalBytes;
        long[] activeTime = new long[0];

        final int version = in.readInt();
        switch (version) {
            case NetworkStatsHistory.VERSION_INIT: {
                bucketDuration = in.readLong();
                bucketStart = readFullLongArray(in);
                rxBytes = readFullLongArray(in);
                rxPackets = new long[bucketStart.length];
                txBytes = readFullLongArray(in);
                txPackets = new long[bucketStart.length];
                operations = new long[bucketStart.length];
                bucketCount = bucketStart.length;
                totalBytes = total(rxBytes) + total(txBytes);
                break;
            }
            case NetworkStatsHistory.VERSION_ADD_PACKETS:
            case NetworkStatsHistory.VERSION_ADD_ACTIVE: {
                bucketDuration = in.readLong();
                bucketStart = readVarLongArray(in);
                activeTime = (version >= NetworkStatsHistory.VERSION_ADD_ACTIVE)
                        ? readVarLongArray(in)
                        : new long[bucketStart.length];
                rxBytes = readVarLongArray(in);
                rxPackets = readVarLongArray(in);
                txBytes = readVarLongArray(in);
                txPackets = readVarLongArray(in);
                operations = readVarLongArray(in);
                bucketCount = bucketStart.length;
                totalBytes = total(rxBytes) + total(txBytes);
                break;
            }
            default: {
                throw new ProtocolException("unexpected version: " + version);
            }
        }

        return new NetworkStatsHistory(bucketDuration, bucketStart, activeTime, rxBytes, rxPackets,
                txBytes, txPackets, operations, bucketCount, totalBytes);
    }

    @NonNull
    private static NetworkIdentitySet readPlatformNetworkIdentitySet(@NonNull DataInput in)
            throws IOException {
        final int version = in.readInt();
        final int size = in.readInt();
        final NetworkIdentitySet set = new NetworkIdentitySet();
        for (int i = 0; i < size; i++) {
            if (version <= NetworkIdentitySet.VERSION_INIT) {
                final int ignored = in.readInt();
            }
            final int type = in.readInt();
            final int subType = in.readInt();
            final String subscriberId = readOptionalString(in);
            final String networkId;
            if (version >= NetworkIdentitySet.VERSION_ADD_NETWORK_ID) {
                networkId = readOptionalString(in);
            } else {
                networkId = null;
            }
            final boolean roaming;
            if (version >= NetworkIdentitySet.VERSION_ADD_ROAMING) {
                roaming = in.readBoolean();
            } else {
                roaming = false;
            }

            final boolean metered;
            if (version >= NetworkIdentitySet.VERSION_ADD_METERED) {
                metered = in.readBoolean();
            } else {
                // If this is the old data and the type is mobile, treat it as metered. (Note that
                // if this is a mobile network, TYPE_MOBILE is the only possible type that could be
                // used.)
                metered = (type == TYPE_MOBILE);
            }

            final boolean defaultNetwork;
            if (version >= NetworkIdentitySet.VERSION_ADD_DEFAULT_NETWORK) {
                defaultNetwork = in.readBoolean();
            } else {
                defaultNetwork = true;
            }

            final int oemNetCapabilities;
            if (version >= NetworkIdentitySet.VERSION_ADD_OEM_MANAGED_NETWORK) {
                oemNetCapabilities = in.readInt();
            } else {
                oemNetCapabilities = NetworkIdentity.OEM_NONE;
            }

            set.add(new NetworkIdentity(type, subType, subscriberId, networkId, roaming, metered,
                    defaultNetwork, oemNetCapabilities));
        }
        return set;
    }

    /**
     * Read legacy network summary statistics file format into the collection,
     * See {@code NetworkStatsService#maybeUpgradeLegacyStatsLocked}.
     */
    public static void readLegacyNetwork(@NonNull NetworkStatsCollection collection,
            @NonNull File file)
            throws IOException {
        final AtomicFile inputFile = new AtomicFile(file);

        DataInputStream in = null;
        try {
            in = new DataInputStream(new BufferedInputStream(inputFile.openRead()));

            // verify file magic header intact
            final int magic = in.readInt();
            if (magic != FILE_MAGIC) {
                throw new ProtocolException("unexpected magic: " + magic);
            }

            final int version = in.readInt();
            switch (version) {
                case NetworkStatsCollection.VERSION_NETWORK_INIT: {
                    // network := size *(NetworkIdentitySet NetworkStatsHistory)
                    final int size = in.readInt();
                    for (int i = 0; i < size; i++) {
                        final NetworkIdentitySet ident = new NetworkIdentitySet(in);
                        final NetworkStatsHistory history = new NetworkStatsHistory(in);

                        final Key key = new Key(ident, UID_ALL, SET_ALL, TAG_NONE);
                        collection.recordHistory(key, history);
                    }
                    break;
                }
                default: {
                    throw new ProtocolException("unexpected version: " + version);
                }
            }
        } catch (FileNotFoundException | ProtocolException e) {
            // missing stats is okay, probably first boot
        } finally {
            IoUtils.closeQuietly(in);
        }
    }

    public void readLegacyUid(@NonNull NetworkStatsCollection collection, @NonNull File file) {

    }
}
