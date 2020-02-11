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
import static android.net.TetheringManager.TETHERING_USB;
import static android.net.TetheringManager.TETHERING_WIFI;

import static com.android.server.connectivity.tethering.PrivateAddressCoordinator.BLUETOOTH_PREFIX;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.net.InetAddresses;
import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.util.NetworkConstants;

import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public final class PrivateAddressCoordinatorTest {
    private static final String TEST_MOBILE_IFNAME = "test_rmnet_data0";
    private static final String TEST_WIFI_IFNAME = "test_wlan0";

    private PrivateAddressCoordinator mPrivateAddressCoordinator;
    private final IpPrefix mBluetoothPrefix = new IpPrefix(BLUETOOTH_PREFIX);

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        mPrivateAddressCoordinator = spy(new PrivateAddressCoordinator());
    }

    @After
    public void tearDown() throws Exception {
        mPrivateAddressCoordinator.clearAllRecords();
    }

    @Test
    public void testRequestDownstreamPrefix() throws Exception {
        final IpPrefix hotspotPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_WIFI);
        assertNotEquals(hotspotPrefix, mBluetoothPrefix);
        final IpPrefix testDupRequest = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_WIFI);
        assertEquals("Test duplicated request: ", hotspotPrefix, testDupRequest);
        final IpPrefix usbPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_USB);
        assertNotEquals(usbPrefix, mBluetoothPrefix);
        assertNotEquals(usbPrefix, hotspotPrefix);
        final IpPrefix btPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_BLUETOOTH);
        assertEquals("Test request bluetooth prefix: ", mBluetoothPrefix, btPrefix);
    }

    @Test
    public void testNoConflictDownstreamPrefix() throws Exception {
        final int fakeHotspotSubId = 43;
        final IpPrefix predefinedPrefix = new IpPrefix("192.168.43.0/24");
        when(mPrivateAddressCoordinator.getRandomSubNetId()).thenReturn(fakeHotspotSubId);
        final IpPrefix hotspotPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_WIFI);
        assertEquals("Test expected perfix: ", predefinedPrefix, hotspotPrefix);
        final IpPrefix usbPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_USB);
        assertNotEquals(usbPrefix, mBluetoothPrefix);
        assertNotEquals(predefinedPrefix, usbPrefix);
        final IpPrefix btPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_BLUETOOTH);
        assertEquals("Test request bluetooth prefix: ", mBluetoothPrefix, btPrefix);
        mPrivateAddressCoordinator.removeDownstreamRecord(TETHERING_WIFI);
        mPrivateAddressCoordinator.removeDownstreamRecord(TETHERING_USB);
        final IpPrefix allowUseFreePrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_USB);
        assertEquals("Test reselect available perfix: ", predefinedPrefix, allowUseFreePrefix);
    }

    private LinkProperties buildMobileLinkProperties(boolean withIPv4, boolean withIPv6,
            boolean isMobile) {
        final String testIface;
        final String testIpv4Address;
        if (isMobile) {
            testIface = TEST_MOBILE_IFNAME;
            testIpv4Address = "10.0.0.1";
        } else {
            testIface = TEST_WIFI_IFNAME;
            testIpv4Address = "192.168.43.42";
        }

        final LinkProperties prop = new LinkProperties();
        prop.setInterfaceName(testIface);

        if (withIPv4) {
            prop.addLinkAddress(
                    new LinkAddress(InetAddresses.parseNumericAddress(testIpv4Address),
                            NetworkConstants.IPV4_ADDR_BITS));
        }

        if (withIPv6) {
            prop.addLinkAddress(
                    new LinkAddress(InetAddresses.parseNumericAddress("2001:db8::"),
                            NetworkConstants.RFC7421_PREFIX_LENGTH));
        }
        return prop;
    }

    @Test
    public void testNoConflictUpstreamPrefix() throws Exception {
        final int fakeHotspotSubId = 43;
        final IpPrefix predefinedPrefix = new IpPrefix("192.168.43.0/24");
        // Force always get subNet 43 for conflict testing.
        when(mPrivateAddressCoordinator.getRandomSubNetId()).thenReturn(fakeHotspotSubId);
        // 1. Enable hotspot with prefix 192.168.43.0/24
        final IpPrefix hotspotPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_WIFI);
        assertEquals(predefinedPrefix, hotspotPrefix);
        // 2. Update v6 only mobile network, hotspot prefix should not be removed.
        int testConflict;
        final LinkProperties v6OnlyMobileProp = buildMobileLinkProperties(false, true, true);
        testConflict = mPrivateAddressCoordinator.updateUpstreamRecord(v6OnlyMobileProp);
        assertEquals(TETHERING_INVALID, testConflict);
        mPrivateAddressCoordinator.removeUpstreamRecord(TEST_MOBILE_IFNAME);
        // 3. Update v4 only mobile network, hotspot prefix should not be removed.
        final LinkProperties v4OnlyMobileProp = buildMobileLinkProperties(true, false, true);
        testConflict = mPrivateAddressCoordinator.updateUpstreamRecord(v6OnlyMobileProp);
        assertEquals(TETHERING_INVALID, testConflict);
        // 4. Update v4v6 mobile network, hotspot prefix should not be removed.
        final LinkProperties v4v6OnlyMobileProp = buildMobileLinkProperties(true, true, true);
        testConflict = mPrivateAddressCoordinator.updateUpstreamRecord(v4v6OnlyMobileProp);
        assertEquals(TETHERING_INVALID, testConflict);
        // 5. Update v6 only wifi network, hotspot prefix should not be removed.
        final LinkProperties v6OnlyWifiProp = buildMobileLinkProperties(false, true, false);
        testConflict = mPrivateAddressCoordinator.updateUpstreamRecord(v6OnlyWifiProp);
        assertEquals(TETHERING_INVALID, testConflict);
        mPrivateAddressCoordinator.removeUpstreamRecord(TEST_WIFI_IFNAME);
        // 6. Update v4 only wifi network, it conflict with hotspot prefix.
        final LinkProperties v4OnlyWifiProp = buildMobileLinkProperties(true, false, false);
        testConflict = mPrivateAddressCoordinator.updateUpstreamRecord(v4OnlyWifiProp);
        assertEquals(TETHERING_WIFI, testConflict);
        // 7. Usb tethering can be enabled and its prefix is different with conflict one.
        final IpPrefix usbPrefix = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_USB);
        assertNotEquals(predefinedPrefix, usbPrefix);
        // 8. After disable wifi, 192.168.43.0/24 is not conflict and but it still won't be choose.
        mPrivateAddressCoordinator.removeUpstreamRecord(TEST_WIFI_IFNAME);
        final IpPrefix hotspotPrefix2 = mPrivateAddressCoordinator.requestDownstreamPrefix(
                TETHERING_WIFI);
        assertNotEquals(predefinedPrefix, hotspotPrefix2);
    }
}
