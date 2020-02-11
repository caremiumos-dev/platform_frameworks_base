/*
 * Copyright (C) 2020 The Android Open Source Project
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
package com.android.server.connectivity.tethering;

import static android.net.TetheringManager.TETHERING_BLUETOOTH;
import static android.net.TetheringManager.TETHERING_INVALID;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.util.ArrayMap;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.IndentingPrintWriter;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

/**
 * Tethering downstream IP addresses may conflict with network assigned addresses.
 * This coordinator is responsible for recording all of network assigned addresses and dispatched
 * free address to downstream interfaces.
 *
 * This class should be accessed on the same tethering internal thread. Access from other threads
 * is not advised.
 * @hide
 */
public class PrivateAddressCoordinator {
    private static final int DEFAULT_PREFIX_LENGTH = 24;
    private static final int MAX_SUBNET_NUM = 256;
    private static final int SUBNET_MASK = 0xff;
    // reserved for bluetooth tethering.
    private static final int BLUETOOTH_RESVERED = 50;

    public static final String BLUETOOTH_PREFIX = "192.168.50.0/24";

    private final ArrayMap<String, IpPrefix> mPotentialConflictPrefixesMap;
    private final ArrayMap<Integer, IpPrefix> mDownstreamPrefixMap;
    // IANA has reserved the following three blocks of the IP address space for private intranets:
    // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
    // Tethering use 192.168.0.0/16 that has 256 contiguous class C network numbers.
    private final IpPrefix mTetheringPrefix = new IpPrefix("192.168.0.0/16");
    private IpPrefix mLastConflictPrefix;

    public PrivateAddressCoordinator() {
        mPotentialConflictPrefixesMap = new ArrayMap();
        mDownstreamPrefixMap = new ArrayMap();
    }

    /**
     * Record IpPrefix which may be possible to conflict with tethering downstream, kicking out
     * conflict downstream and its downstream type. If no conflict, TETHERING_INVALID would be
     * return.
     */
    public int updateUpstreamRecord(final LinkProperties lp) {
        if (!lp.hasIpv4Address()) {
            removeUpstreamRecord(lp.getInterfaceName());
            return TETHERING_INVALID;
        }

        final List<LinkAddress> linkAddresses = lp.getLinkAddresses();
        for (LinkAddress address : linkAddresses) {
            // Typically a link only have one IPv4 address.
            InetAddress ipv4IP = address.getAddress();
            if (!(ipv4IP instanceof Inet4Address)) continue;
            // Do not care about the prefix which is not in 192.168.*.*/24.
            if (!mTetheringPrefix.contains(ipv4IP)) {
                removeUpstreamRecord(lp.getInterfaceName());
                return TETHERING_INVALID;
            }

            final IpPrefix newPrefix = new IpPrefix(ipv4IP, address.getPrefixLength());
            mPotentialConflictPrefixesMap.put(lp.getInterfaceName(), newPrefix);

            final int downstreamType = getConflictDownstreamType(newPrefix);
            if (downstreamType != TETHERING_INVALID) {
                mLastConflictPrefix = removeDownstreamRecord(downstreamType);
            }
            return downstreamType;
        }

        return TETHERING_INVALID;
    }

    /** Remove IpPrefix records corresponding to input interface name. */
    public void removeUpstreamRecord(final String interfaceName) {
        mPotentialConflictPrefixesMap.remove(interfaceName);
    }

    /** Request downstream prefix. */
    public IpPrefix requestDownstreamPrefix(final int interfaceType) {
        final int index = mDownstreamPrefixMap.indexOfKey(interfaceType);
        if (index >= 0) {
            return mDownstreamPrefixMap.valueAt(index);
        }

        // TODO: support random prefix for bluetooth after b/148078390 is fixed.
        if (interfaceType == TETHERING_BLUETOOTH) {
            final IpPrefix prefix = new IpPrefix(BLUETOOTH_PREFIX);
            updateDownstreamRecord(interfaceType, prefix);
            return prefix;
        }

        // Prefix would be 192.168.[subNet].0/24.
        final byte[] bytes = {(byte) 0xc0, (byte) 0xa8, 0, 0};
        int random = getRandomSubNetId();
        for (int i = 0; i < MAX_SUBNET_NUM; i++) {
            final int subNet = (random + i) & SUBNET_MASK;
            if (subNet == BLUETOOTH_RESVERED) continue;

            bytes[2] = (byte) subNet;
            InetAddress addr;
            try {
                addr = InetAddress.getByAddress(bytes);
            } catch (UnknownHostException e) {
                throw new IllegalStateException("Invalid address, shouldn't happen.", e);
            }

            final IpPrefix prefix = new IpPrefix(addr, DEFAULT_PREFIX_LENGTH);
            // Do not choose the prefix which is recorded as conflict last time.
            if (prefix.equals(mLastConflictPrefix)) continue;
            // Check whether this prefix is inused.
            if (getDownstreamType(prefix) != TETHERING_INVALID) continue;
            // Check whether this prefix is conflict with any upstream network.
            if (!isConflictWithUpstream(prefix)) {
                updateDownstreamRecord(interfaceType, prefix);
                return prefix;
            }
        }

        // No available prefix.
        return null;
    }

    /** Get random subnet value. Return value is in 0 ~ 255. */
    @VisibleForTesting
    public int getRandomSubNetId() {
        return ((new Random()).nextInt()) & SUBNET_MASK; // subNet is in 0 ~ 255.
    }

    private void updateDownstreamRecord(final int interfaceType, final IpPrefix prefix) {
        mDownstreamPrefixMap.put(interfaceType, prefix);
    }

    /** Remove donwstream record for interfaceTYpe. */
    public IpPrefix removeDownstreamRecord(final int interfaceType) {
        return mDownstreamPrefixMap.remove(interfaceType);
    }

    /** Clear all prefix records. */
    public void clearAllRecords() {
        mPotentialConflictPrefixesMap.clear();
        mDownstreamPrefixMap.clear();
    }

    private boolean isConflictWithUpstream(final IpPrefix source) {
        for (int i = 0; i < mPotentialConflictPrefixesMap.size(); i++) {
            final IpPrefix target = mPotentialConflictPrefixesMap.valueAt(i);
            if (isConflictPrefix(source, target)) return true;
        }
        return false;
    }

    private int getConflictDownstreamType(final IpPrefix source) {
        for (int i = 0; i < mDownstreamPrefixMap.size(); i++) {
            final IpPrefix target = mDownstreamPrefixMap.valueAt(i);
            if (isConflictPrefix(source, target)) return mDownstreamPrefixMap.keyAt(i);
        }
        return TETHERING_INVALID;
    }

    private boolean isConflictPrefix(final IpPrefix source, final IpPrefix target) {
        if (target.getPrefixLength() < source.getPrefixLength()) {
            return target.contains(source.getAddress());
        }

        return source.contains(target.getAddress());
    }

    private int getDownstreamType(final IpPrefix source) {
        final int index = mDownstreamPrefixMap.indexOfValue(source);

        if (index < 0) return TETHERING_INVALID;

        return mDownstreamPrefixMap.keyAt(index);
    }

    void dump(final IndentingPrintWriter pw) {
        pw.println("mPotentialConflictPrefixesMap:");
        for (int i = 0; i < mPotentialConflictPrefixesMap.size(); i++) {
            pw.println(mPotentialConflictPrefixesMap.keyAt(i) + " - "
                    + mPotentialConflictPrefixesMap.valueAt(i));
        }
        pw.println("mDownstreamPrefixMap:");
        for (int i = 0; i < mDownstreamPrefixMap.size(); i++) {
            pw.println(mDownstreamPrefixMap.keyAt(i) + " - " + mDownstreamPrefixMap.valueAt(i));
        }
    }
}
