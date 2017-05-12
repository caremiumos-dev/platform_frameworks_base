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

package android.net;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.SOCK_DGRAM;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.system.Os;
import android.test.AndroidTestCase;
import com.android.server.IpSecService;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.junit.Assert;

public class IpSecManagerTest extends AndroidTestCase {

    private static final String TAG = IpSecManagerTest.class.getSimpleName();

    private static final byte[] CRYPT_KEY =
            new byte[] {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F
            };
    private static final byte[] AUTH_KEY =
            new byte[] {
                0x7A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F,
                0x7A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F
            };

    private static final int TEST_UDP_ENCAP_PORT = 34567;
    private static final int DROID_SPI = 0xD1201D;

    private static final String IPV4_LOOPBACK = "127.0.0.1";
    private static final String IPV6_LOOPBACK = "::1";

    private static final InetAddress GOOGLE_DNS_4;
    private static final IpSecUdpEncapResponse UDP_ENCAP_RESP;

    static {
        try {
            // Google Public DNS Addresses;
            GOOGLE_DNS_4 = InetAddress.getByName("8.8.8.8");
        } catch (UnknownHostException e) {
            throw new RuntimeException("Could not resolve DNS Addresses", e);
        }
    }
    static {
        try {
            FileDescriptor sockFd = Os.socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);
            UDP_ENCAP_RESP =
                    new IpSecUdpEncapResponse(
                            IpSecManager.Status.OK, 0x1, TEST_UDP_ENCAP_PORT, sockFd);
        } catch (Exception e) {
            throw new RuntimeException("Could not resolve DNS Addresses", e);
        }
    }

    private static final IpSecSpiResponse SPI_RESP = new IpSecSpiResponse(
                                    IpSecManager.Status.OK, 0x1, DROID_SPI);
    private static final IpSecTransformResponse TSF_RESP = new IpSecTransformResponse(
                                    IpSecManager.Status.OK, 0x1);

    private ConnectivityManager mConnectivityManager;
    private IpSecManager mIpSecManager;
    private InetAddress mLocalAddr = null;
    private IpSecService mMockIpSecService = null;
    private Context mMockContext = null;

    protected void setUp() throws Exception {
        super.setUp();

        mMockContext = mock(Context.class);

        mMockIpSecService = mock(IpSecService.class);
        when(mMockIpSecService.reserveSecurityParameterIndex(
                        anyInt(), anyString(), anyInt(), anyObject()))
                .thenReturn(SPI_RESP);

        when(mMockIpSecService.openUdpEncapsulationSocket(eq(TEST_UDP_ENCAP_PORT), anyObject()))
                    .thenReturn(UDP_ENCAP_RESP);
        mLocalAddr = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
        mIpSecManager = new IpSecManager(mMockIpSecService);
    }

    /*
     * Allocate a specific SPI
     * Close SPIs
     */
    public void testAllocSpi() throws Exception {
        IpSecManager.SecurityParameterIndex randomSpi = null, droidSpi = null;
        randomSpi = mIpSecManager.reserveSecurityParameterIndex(IpSecTransform.DIRECTION_OUT, GOOGLE_DNS_4);

        assertNotEquals(IpSecManager.INVALID_SECURITY_PARAMETER_INDEX, randomSpi.getSpi());
        droidSpi =
                mIpSecManager.reserveSecurityParameterIndex(
                        IpSecTransform.DIRECTION_IN, GOOGLE_DNS_4, DROID_SPI);
        assertTrue(
                "Failed to allocate specified SPI, " + DROID_SPI, droidSpi.getSpi() == DROID_SPI);

        randomSpi.close();
        droidSpi.close();
    }

    /*
     * Throws resource unavailable exception
     */
    public void testAllocSpiResUnavaiableExeption() throws Exception {
        IpSecSpiResponse spiResp =
                    new IpSecSpiResponse(IpSecManager.Status.RESOURCE_UNAVAILABLE, 0x0, 0x0);
        when(mMockIpSecService.reserveSecurityParameterIndex(
                        anyInt(), anyString(), anyInt(), anyObject()))
                .thenReturn(spiResp);

        try {
            IpSecManager.SecurityParameterIndex retSpi =
                    mIpSecManager.reserveSecurityParameterIndex(IpSecTransform.DIRECTION_OUT, GOOGLE_DNS_4);
            Assert.fail("SPI " + retSpi.getSpi() + " allocated by error");
        } catch (Exception e) {
            assertTrue(e instanceof IpSecManager.ResourceUnavailableException);
        }
    }

    /*
     * Throws spi unavailable exception
     */
    public void testAllocSpiSpiUnavaiableExeption() throws Exception {
        IpSecSpiResponse spiResp =
                    new IpSecSpiResponse(IpSecManager.Status.SPI_UNAVAILABLE, 0x0, 0x0);
        when(mMockIpSecService.reserveSecurityParameterIndex(
                        anyInt(), anyString(), anyInt(), anyObject()))
                .thenReturn(spiResp);

        try {
            IpSecManager.SecurityParameterIndex retSpi =
                    mIpSecManager.reserveSecurityParameterIndex(IpSecTransform.DIRECTION_OUT, GOOGLE_DNS_4);
            Assert.fail("SPI " + retSpi.getSpi() + " allocated by error");
        } catch (Exception e) {
            assertTrue(e instanceof IpSecManager.ResourceUnavailableException);
        }
    }

    public void testBuildEncapsulationSocket() throws Exception {

        when(mMockIpSecService.openUdpEncapsulationSocket(eq(0), anyObject()))
                .thenReturn(UDP_ENCAP_RESP);

        IpSecManager.UdpEncapsulationSocket encapSocket =
                mIpSecManager.openUdpEncapsulationSocket(TEST_UDP_ENCAP_PORT);
        assertTrue(encapSocket.getSocket() != null);
        assertTrue(encapSocket.getPort() == TEST_UDP_ENCAP_PORT);

        encapSocket = mIpSecManager.openUdpEncapsulationSocket();
        assertTrue(encapSocket.getSocket() != null);
        assertTrue(encapSocket.getPort() > 0);
    }

    void setUpTestIpSecTransformBuilder() {
        try {
            mConnectivityManager = mock(ConnectivityManager.class);
            when(mMockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mConnectivityManager);
            when(mMockIpSecService.createTransportModeTransform(anyObject(), anyObject()))
                    .thenReturn(TSF_RESP);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot set up testUdpEncapsulation correctly " + e.getMessage());
        }
    }

    public void testIpSecTransformBuilder() throws Exception {

        setUpTestIpSecTransformBuilder();

        assertTrue(mIpSecManager != null);
        assertTrue(TSF_RESP != null);

        IpSecManager.UdpEncapsulationSocket encapSocket = mIpSecManager.openUdpEncapsulationSocket(34567);

        IpSecManager.SecurityParameterIndex outSpi =
                mIpSecManager.reserveSecurityParameterIndex(IpSecTransform.DIRECTION_OUT, mLocalAddr);

        IpSecManager.SecurityParameterIndex inSpi =
                mIpSecManager.reserveSecurityParameterIndex(
                        IpSecTransform.DIRECTION_IN, mLocalAddr, DROID_SPI);

        IpSecTransform.Builder builder = mock(IpSecTransform.Builder.class);
        builder = new IpSecTransform.Builder(mMockContext);

        IpSecConfig ipSecConfig =
                builder.setSpi(IpSecTransform.DIRECTION_OUT, outSpi)
                        .setEncryption(
                                IpSecTransform.DIRECTION_OUT,
                                new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, CRYPT_KEY))
                        .setAuthentication(
                                IpSecTransform.DIRECTION_OUT,
                                new IpSecAlgorithm(
                                        IpSecAlgorithm.AUTH_HMAC_SHA256,
                                        AUTH_KEY,
                                        AUTH_KEY.length * 8))
                        .setSpi(IpSecTransform.DIRECTION_IN, inSpi)
                        .setEncryption(
                                IpSecTransform.DIRECTION_IN,
                                new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, CRYPT_KEY))
                        .setAuthentication(
                                IpSecTransform.DIRECTION_IN,
                                new IpSecAlgorithm(
                                        IpSecAlgorithm.AUTH_HMAC_SHA256,
                                        AUTH_KEY,
                                        CRYPT_KEY.length * 8))
                        .setIpv4Encapsulation(encapSocket, encapSocket.getPort())
                        .getIpSecConfig();
        assertTrue(ipSecConfig != null);

        assertTrue(encapSocket.getPort() == ipSecConfig.getEncapRemotePort());
        assertArrayEquals(ipSecConfig.getAuthentication(
                IpSecTransform.DIRECTION_IN).getKey(), AUTH_KEY);
        assertArrayEquals(ipSecConfig.getEncryption(
                IpSecTransform.DIRECTION_IN).getKey(), CRYPT_KEY);
        assertTrue(ipSecConfig.getEncryption(IpSecTransform.DIRECTION_IN).getName() == "cbc(aes)");
        assertTrue(
                ipSecConfig.getAuthentication(IpSecTransform.DIRECTION_IN).getName()
                        == "hmac(sha256)");
        assertTrue(ipSecConfig.getEncryption(IpSecTransform.DIRECTION_OUT).getName() == "cbc(aes)");
        assertTrue(
                ipSecConfig.getAuthentication(IpSecTransform.DIRECTION_OUT).getName()
                        == "hmac(sha256)");
    }
}
