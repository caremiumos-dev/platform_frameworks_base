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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Parcel;
import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Unit tests for {@link IpSecConfig}. */
@SmallTest
@RunWith(JUnit4.class)
public class IpSecConfigTest {

    private static final byte[] AEAD_KEY = {
        0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
        0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F,
        0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
        0x18, 0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F,
        0x73, 0x61, 0x6C, 0x74
    };
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

    private static final IpSecAlgorithm AUTH_ALGO =
            new IpSecAlgorithm(IpSecAlgorithm.AUTH_HMAC_SHA256, AUTH_KEY, AUTH_KEY.length * 4);
    private static final IpSecAlgorithm CRYPT_ALGO =
            new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, CRYPT_KEY);
    private static final IpSecAlgorithm AEAD_ALGO =
            new IpSecAlgorithm(IpSecAlgorithm.AUTH_CRYPT_AES_GCM, AEAD_KEY, 128);

    @Test
    public void testSetAuthenticationValidation() {
        // Validate that correct algorithm type succeeds
        IpSecConfig config = new IpSecConfig();
        config.setAuthentication(IpSecTransform.DIRECTION_IN, AUTH_ALGO);
        config.validateAlgorithmTypes();

        // Validate that incorrect algorithm types fails
        for (IpSecAlgorithm algo : new IpSecAlgorithm[] {CRYPT_ALGO, AEAD_ALGO}) {
            config = new IpSecConfig();
            try {
                config.setAuthentication(IpSecTransform.DIRECTION_IN, algo);
                config.validateAlgorithmTypes();
                fail("Did not throw exception on invalid algorithm type");
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void testSetEncryptionValidation() {
        // Validate that correct algorithm type succeeds
        IpSecConfig config = new IpSecConfig();
        config.setEncryption(IpSecTransform.DIRECTION_IN, CRYPT_ALGO);
        config.validateAlgorithmTypes();

        // Validate that incorrect algorithm types fails
        for (IpSecAlgorithm algo : new IpSecAlgorithm[] {AUTH_ALGO, AEAD_ALGO}) {
            config = new IpSecConfig();
            try {
                config.setEncryption(IpSecTransform.DIRECTION_IN, algo);
                config.validateAlgorithmTypes();
                fail("Did not throw exception on invalid algorithm type");
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void testSetAuthenticatedEncryptionValidation() {
        // Validate that correct algorithm type succeeds
        IpSecConfig config = new IpSecConfig();
        config.setAuthenticatedEncryption(IpSecTransform.DIRECTION_IN, AEAD_ALGO);
        config.validateAlgorithmTypes();

        // Validate that incorrect algorithm types fails
        for (IpSecAlgorithm algo : new IpSecAlgorithm[] {AUTH_ALGO, CRYPT_ALGO}) {
            config = new IpSecConfig();
            try {
                config.setAuthenticatedEncryption(IpSecTransform.DIRECTION_IN, algo);
                config.validateAlgorithmTypes();
                fail("Did not throw exception on invalid algorithm type");
            } catch (IllegalArgumentException expected) {
            }
        }
    }

    @Test
    public void testUnparcelInvalidAlgorithm() throws Exception {
        IpSecConfig config = new IpSecConfig();
        config.setEncryption(IpSecTransform.DIRECTION_IN, AEAD_ALGO);

        try {
            Parcel p = Parcel.obtain();
            config.writeToParcel(p, 0);
            p.setDataPosition(0);
            IpSecConfig.CREATOR.createFromParcel(p);
            fail("Did not throw exception on invalid algorithm type");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testDefaults() throws Exception {
        IpSecConfig c = new IpSecConfig();
        assertEquals(IpSecTransform.MODE_TRANSPORT, c.getMode());
        assertEquals("", c.getLocalAddress());
        assertEquals("", c.getRemoteAddress());
        assertNull(c.getNetwork());
        assertEquals(IpSecTransform.ENCAP_NONE, c.getEncapType());
        assertEquals(IpSecManager.INVALID_RESOURCE_ID, c.getEncapSocketResourceId());
        assertEquals(0, c.getEncapRemotePort());
        assertEquals(0, c.getNattKeepaliveInterval());
        for (int direction :
                new int[] {IpSecTransform.DIRECTION_OUT, IpSecTransform.DIRECTION_IN}) {
            assertNull(c.getEncryption(direction));
            assertNull(c.getAuthentication(direction));
            assertEquals(IpSecManager.INVALID_RESOURCE_ID, c.getSpiResourceId(direction));
        }
    }

    @Test
    public void testParcelUnparcel() throws Exception {
        assertParcelingIsLossless(new IpSecConfig());

        IpSecConfig c = new IpSecConfig();
        c.setMode(IpSecTransform.MODE_TUNNEL);
        c.setLocalAddress("0.0.0.0");
        c.setRemoteAddress("1.2.3.4");
        c.setEncapType(android.system.OsConstants.UDP_ENCAP_ESPINUDP);
        c.setEncapSocketResourceId(7);
        c.setEncapRemotePort(22);
        c.setNattKeepaliveInterval(42);
        c.setEncryption(
                IpSecTransform.DIRECTION_OUT,
                new IpSecAlgorithm(
                        IpSecAlgorithm.CRYPT_AES_CBC,
                        new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF}));
        c.setAuthentication(
                IpSecTransform.DIRECTION_OUT,
                new IpSecAlgorithm(
                        IpSecAlgorithm.AUTH_HMAC_SHA1,
                        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 0}));
        c.setSpiResourceId(IpSecTransform.DIRECTION_OUT, 1984);
        c.setEncryption(
                IpSecTransform.DIRECTION_IN,
                new IpSecAlgorithm(
                        IpSecAlgorithm.CRYPT_AES_CBC,
                        new byte[] {2, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF}));
        c.setAuthentication(
                IpSecTransform.DIRECTION_IN,
                new IpSecAlgorithm(
                        IpSecAlgorithm.AUTH_HMAC_SHA1,
                        new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 1}));
        c.setSpiResourceId(IpSecTransform.DIRECTION_IN, 99);
        assertParcelingIsLossless(c);
    }

    private void assertParcelingIsLossless(IpSecConfig ci) throws Exception {
        Parcel p = Parcel.obtain();
        ci.writeToParcel(p, 0);
        p.setDataPosition(0);
        IpSecConfig co = IpSecConfig.CREATOR.createFromParcel(p);
        assertTrue(IpSecConfig.equals(co, ci));
    }
}
