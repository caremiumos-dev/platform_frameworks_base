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

package com.xtc.secure.cipher;

import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * RSA sign/verify helpers for the XTC app install verification path.
 *
 * @hide
 */
public class RSAUtil {
    private static final String TAG = "RSAUtil";
    private static final String SIGN_ALGORITHMS = "SHA1WithRSA";

    private static final String PUBLIC_KEY_V1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiZsxeOGZPCOTNLOgxapEq45ub+XLZ1IGpE8/+zrFIQR2J+0dYjFU7lvVgJ5IZtj6LK4rIiO1nt4RFkitet26Nwl3rbqkx6bd/NTiJeDWVU0CqHzPQ1QkfX8AldUTx5Lx16lGv1FP9jF/vkGyiVVSDqSVoEt3pT+t3TONiGWlCh3D6/CiQL3DSdIOFKAn5zG3XfE/x1iuKmhaMsfk1crSdJpKyQTdLcIWu0A+2UYaUAN/rrPtnJ57H2PRLn7tyERVhjiqbxIM0DAq6g1SMwGrFkMdBR25aX+QPpw6Pb/pen1Di00ZYuC8tAWmVB/RfBaS+oCmekwxPrN6zFytAnslCwIDAQAB";

    public static String generateSHA256Signature(String data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getPublicKey(int keyVersion) {
        switch (keyVersion) {
            default:
                return PUBLIC_KEY_V1;
        }
    }

    public static String sign(String data, String privateKeyBase64, String charset) {
        try {
            PrivateKey privateKey = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
                    .generatePrivate(new PKCS8EncodedKeySpec(
                            Base64.decode(privateKeyBase64, Base64.DEFAULT)));
            Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
            signature.initSign(privateKey);
            signature.update(data.getBytes(charset));
            return Base64.encodeToString(signature.sign(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean verifySignature(String data, String signatureBase64,
            String publicKeyBase64, String charset) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_RSA)
                .generatePublic(new X509EncodedKeySpec(
                        Base64.decode(publicKeyBase64, Base64.DEFAULT)));
        Signature signature = Signature.getInstance(SIGN_ALGORITHMS);
        signature.initVerify(publicKey);
        signature.update(data.getBytes(charset));
        return signature.verify(Base64.decode(signatureBase64, Base64.DEFAULT));
    }
}