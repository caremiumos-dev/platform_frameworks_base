package com.android.server;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.IPPROTO_UDP;
import static android.system.OsConstants.SOCK_DGRAM;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.INetd;
import android.net.IpSecAlgorithm;
import android.net.IpSecConfig;
import android.net.IpSecManager;
import android.net.IpSecSpiResponse;
import android.net.IpSecTransform;
import android.net.IpSecTransformResponse;
import android.net.IpSecUdpEncapResponse;
import android.os.Binder;
import android.os.RemoteException;
import android.system.ErrnoException;
import android.system.Os;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link IpSecService}. */
@RunWith(JUnit4.class)
public class IpSecServiceTest {

    private static final int DROID_SPI = 0xD1201D;
    private static final int TEST_UDP_ENCAP_INVALID_PORT = 100;
    private static final int TEST_UDP_ENCAP_PORT = 34567;

    private static final String IPV4_LOOPBACK = "127.0.0.1";
    private static final String IPV4_ADDR = "192.168.0.2";

    private static final byte[] CRYPT_KEY = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F
    };
    private static final byte[] AUTH_KEY = {
        0x7A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F,
        0x7A, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x7F
    };

    static final IpSecTransformResponse TSF_RESP =
        new IpSecTransformResponse(IpSecManager.Status.OK, 0x1);
    IpSecUdpEncapResponse mIpSecUdpEncapResp = null;
    private InetAddress mLocalAddr = null;

    Context mMockContext = mock(Context.class);
    INetd mMockNetd = mock(INetd.class);
    IpSecService mMockIpSecService = mock(IpSecService.class);
    ConnectivityManager mCM = mock(ConnectivityManager.class);
    IpSecManager mISM = null;

    @Before
    public void setUp()
            throws IOException, UnknownHostException, InterruptedException, RemoteException,
                    ErrnoException {
        mLocalAddr = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
    }

    @Test
    public void testIpSecServiceCreate() throws InterruptedException {
        IpSecService ipSecSrv = IpSecService.create(mMockContext);
        assertTrue(ipSecSrv != null);
    }

    @Test
    public void testIpSecServiceReserveSpi() throws Exception {
        when(mMockNetd.ipSecAllocateSpi(
                        anyInt(),
                        eq(IpSecTransform.DIRECTION_OUT),
                        anyString(),
                        eq(IPV4_LOOPBACK),
                        eq(DROID_SPI)))
                .thenReturn(DROID_SPI);
        when(mMockNetd.ipSecAllocateSpi(
                        anyInt(),
                        eq(IpSecTransform.DIRECTION_OUT),
                        anyString(),
                        eq(IPV4_ADDR),
                        eq(DROID_SPI)))
                .thenReturn(0);

        IpSecService ipSecSrv = IpSecService.create(mMockContext);
        ipSecSrv.setINetd(mMockNetd);

        IpSecSpiResponse spiResp =
                ipSecSrv.reserveSecurityParameterIndex(
                        IpSecTransform.DIRECTION_OUT, IPV4_LOOPBACK, DROID_SPI, new Binder());
        assertTrue(spiResp.status == IpSecManager.Status.OK);
        assertTrue(spiResp.spi == DROID_SPI);

        spiResp =
                ipSecSrv.reserveSecurityParameterIndex(
                        IpSecTransform.DIRECTION_OUT, IPV4_ADDR, DROID_SPI, new Binder());
        assertTrue(spiResp.status == IpSecManager.Status.OK);
        assertTrue(spiResp.spi == 0);
    }

    @Test
    public void testOpenUdpEncapsulationSocket()
            throws Exception {
        IpSecService ipSecSrv = IpSecService.create(mMockContext);
        ipSecSrv.setINetd(mMockNetd);

        // Get an available port and store in localport
        ServerSocket s = new ServerSocket(0);
        int localport = s.getLocalPort();
        s.close();

        IpSecUdpEncapResponse udpEncapResp =
                ipSecSrv.openUdpEncapsulationSocket(localport, new Binder());
        assertTrue(udpEncapResp != null);
        assertTrue(udpEncapResp.port == localport);

        udpEncapResp = ipSecSrv.openUdpEncapsulationSocket(0, new Binder());
        assertTrue(udpEncapResp != null);
        assertTrue(udpEncapResp.status == IpSecManager.Status.OK);
    }

    @Test
    public void testOpenUdpEncapsulationSocketPortRange() throws Exception {
        IpSecService ipSecSrv = IpSecService.create(mMockContext);
        ipSecSrv.setINetd(mMockNetd);

        try {
            IpSecUdpEncapResponse udpEncapResp =
                    ipSecSrv.openUdpEncapsulationSocket(TEST_UDP_ENCAP_INVALID_PORT, new Binder());
            Assert.fail("Invalid UDP encap socket open");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }

    @Test
    public void testOpenUdpEncapsulationSocketTwice() throws Exception {
        IpSecService ipSecSrv = IpSecService.create(mMockContext);
        ipSecSrv.setINetd(mMockNetd);

        // Get an available port and store in localport
        ServerSocket s = new ServerSocket(0);
        int localport = s.getLocalPort();
        s.close();

        IpSecUdpEncapResponse udpEncapResp =
                ipSecSrv.openUdpEncapsulationSocket(localport, new Binder());
        assertTrue(udpEncapResp != null);
        assertTrue(udpEncapResp.port == localport);

        udpEncapResp = ipSecSrv.openUdpEncapsulationSocket(localport, new Binder());
        assertTrue(udpEncapResp.status == IpSecManager.Status.RESOURCE_UNAVAILABLE);
    }

    void setUpTestCreateTransportModeTransform() throws Exception {
        when(mMockContext.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(mCM);

        // For building a mocking transform
        when(mMockIpSecService.createTransportModeTransform(anyObject(), anyObject()))
                .thenReturn(TSF_RESP);

        // Mocking the netd to add SA
        when(mMockNetd.ipSecAddSecurityAssociation(
                        anyInt(),
                        eq(0),
                        anyInt(),
                        eq(""),
                        eq(""),
                        anyLong(),
                        eq(DROID_SPI),
                        anyString(),
                        eq(AUTH_KEY),
                        anyInt(),
                        anyString(),
                        eq(CRYPT_KEY),
                        anyInt(),
                        anyInt(),
                        anyInt(),
                        anyInt()))
                .thenReturn(DROID_SPI);
        when(mMockNetd.ipSecAllocateSpi(anyInt(), anyInt(), anyString(), anyString(), anyInt()))
                .thenReturn(DROID_SPI);
        when(mMockNetd.ipSecAllocateSpi(
                        anyInt(), anyInt(), anyString(), anyString(), eq(DROID_SPI)))
                .thenReturn(DROID_SPI);
    }

    @Test
    public void testCreateTransportModeTransform()
            throws Exception {
        setUpTestCreateTransportModeTransform();

        IpSecService ipSecSrv = IpSecService.create(mMockContext);
        ipSecSrv.setINetd(mMockNetd);
        mISM = new IpSecManager(ipSecSrv);

        // Allocate and add SPI records in the IpSecService
        IpSecManager.SecurityParameterIndex outSpi =
                mISM.reserveSecurityParameterIndex(IpSecTransform.DIRECTION_OUT, mLocalAddr);
        IpSecManager.SecurityParameterIndex inSpi =
                mISM.reserveSecurityParameterIndex(IpSecTransform.DIRECTION_IN, mLocalAddr, DROID_SPI);

        IpSecConfig ipSecConfig =
                new IpSecTransform.Builder(mMockContext)
                        .setSpi(IpSecTransform.DIRECTION_OUT, outSpi)
                        .setSpi(IpSecTransform.DIRECTION_IN, inSpi)
                        .setEncryption(
                                IpSecTransform.DIRECTION_OUT,
                                new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, CRYPT_KEY))
                        .setAuthentication(
                                IpSecTransform.DIRECTION_OUT,
                                new IpSecAlgorithm(
                                        IpSecAlgorithm.AUTH_HMAC_SHA256,
                                        AUTH_KEY,
                                        AUTH_KEY.length * 8))
                        .setEncryption(
                                IpSecTransform.DIRECTION_IN,
                                new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, CRYPT_KEY))
                        .setAuthentication(
                                IpSecTransform.DIRECTION_IN,
                                new IpSecAlgorithm(
                                        IpSecAlgorithm.AUTH_HMAC_SHA256,
                                        AUTH_KEY,
                                        CRYPT_KEY.length * 8))
                        .getIpSecConfig();

        IpSecTransformResponse createTransformResp =
                ipSecSrv.createTransportModeTransform(ipSecConfig, new Binder());
        assertTrue(createTransformResp.status == IpSecManager.Status.OK);
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
