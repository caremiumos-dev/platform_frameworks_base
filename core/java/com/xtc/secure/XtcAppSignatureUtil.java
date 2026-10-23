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

package com.xtc.secure;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Binder;
import android.text.TextUtils;
import android.util.Log;

import com.xtc.xtcdiag.socket.XtcConvertUtil;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Signature whitelist used to recognise "XTC-signed" applications.
 *
 * <p>WARNING / TRUST-MODEL CONDEMNATION: this is a hard-coded allow-list of
 * certificate hashes plus a {@code Binder.getCallingUid() == 1000} gate. It
 * discriminates against third-party applications and centralises trust in a
 * fixed vendor key set, so an app is judged by who signed it rather than by
 * what it does. It is reproduced only for device-compatibility; it should not
 * be treated as a security boundary.
 *
 * @hide
 */
public class XtcAppSignatureUtil {
    private static final String TAG = "XtcAppSignatureUtil";

    private static final List<String> xtcAppSignatureList = new CopyOnWriteArrayList<>();

    static {
        xtcAppSignatureList.add("840F343A0EFC325BA0BF75DAC835E4D587033435");
        xtcAppSignatureList.add("050E6C37CB4A7732EDA30E77EFA1FA5E393D98D8");
        xtcAppSignatureList.add("4089ADF8B4D11E876064C9DF7478A7556EBD091A");
        xtcAppSignatureList.add("753FD1D8074D00B20A179ED8974541F2C7654F14");
        xtcAppSignatureList.add("3A8AC7AB02FB79CF0B136A1EEC8819CA5AA54403");
        xtcAppSignatureList.add("9D62E07E4AD5710141D96AFC35C174A8355DFAD2");
        xtcAppSignatureList.add("06A2FDC33830396B3BA5F31DA0D1BDEA40713C58");
        xtcAppSignatureList.add("3957EA760FCF15EEE1E04C0B920F4A6828E374B6");
        xtcAppSignatureList.add("BAA02A200759844BE1CD60C669FE8600B3DBB3D4");
        xtcAppSignatureList.add("BFE6252B263046A1D53C8A3DB49F5FAD408157C0");
        xtcAppSignatureList.add("29CC3DB9B75DC5DB51B4F911D6DA5C18FEC6FA5A");
        xtcAppSignatureList.add("ED0C2CC43B6A560EA6BC6B49C10AB6EB667F0BFE");
        xtcAppSignatureList.add("5D8B32BBF64C0555B86B5D71D77987F792ED0E4A");
        xtcAppSignatureList.add("3CFDA1CA54E50BE2437D218BF36370BB940739C9");
        xtcAppSignatureList.add("7C73269CF49142F3D4540C4869B832E3BD2C026F");
        xtcAppSignatureList.add("438CE5CF1038644CA0CFBCD341294992B59BC377");
        xtcAppSignatureList.add("C29A5A2CA235F326A4800335ECAFBF31ACE2D0A2");
        xtcAppSignatureList.add("33B28EAFDF43DEA4FF5DC3DBD9E5A523F67F6BB1");
        xtcAppSignatureList.add("BB987D06225A0F0E67E37AD5709AF8FE6795DF56");
        xtcAppSignatureList.add("5E77A5E4DF2C1399B3A5A8C03E26BD62B7F2738D");
        xtcAppSignatureList.add("8C65F7B3FD7D328E108780B1D2CE4BC80B1B0E27");
        xtcAppSignatureList.add("8B238C6AE07432E65758A2BF1BFAAA25A8644786");
        xtcAppSignatureList.add("0AC0DACC820AD87C6F4E82BEF09AECD9EAA13561");
        xtcAppSignatureList.add("457D4C2940FAA553ABCA3A1F3C02E3DBAE83598E");
        xtcAppSignatureList.add("7D521089907679AD9DD77DB795A874FDB20DF331");
        xtcAppSignatureList.add("E80AB32A9C4A439F883301AACEF5EE2BC33081BC");
        xtcAppSignatureList.add("C714A7271DF98588954D4627D28884B59CAFF95A");
        xtcAppSignatureList.add("944F56C67678D65C1BF946090CA0C448C54B9E10");
        xtcAppSignatureList.add("EB5C39108CF3213218004CE70210374854925AB7");
        xtcAppSignatureList.add("E5647869EF8C62AEBD9052CB44DD60CE3B44BA15");
        xtcAppSignatureList.add("B9B4F98254CFB5DDEC438ABCB3EDF429AF73CDA3");
        xtcAppSignatureList.add("51F23472CF85A449BD6027BB10C1B5265CEA2079");
        xtcAppSignatureList.add("68493115AF3E9FD568D87B06FED9E6B76C491BD9");
        xtcAppSignatureList.add("0A49C550B9004370F4DCA1A5A3ABAE6BA7D4A943");
        xtcAppSignatureList.add("6568FECDCD20CFCA42FE7BF2384670B553029415");
        xtcAppSignatureList.add("3B34B94C7A9123ECCC1698A8E394C0A068BE3526");
        xtcAppSignatureList.add("4282AC67524D3E83AF8F5D2253D32C40AFF4B2E2");
        xtcAppSignatureList.add("8480F28E62D1D844F4C7C5CE4432B0087E31E8DC");
        xtcAppSignatureList.add("13742EB512A17E7EAE7622B45224E0A9ED1144EE");
        xtcAppSignatureList.add("F1A54A3F024A8D1B74D1FF1F74D3BE66ED79312E");
        xtcAppSignatureList.add("00C9C3FC5844F5BA3215856B1A9775F40D10EA88");
        xtcAppSignatureList.add("DD66005EEE8DA757E18F358547AAD832BD2E6BD8");
        xtcAppSignatureList.add("77D332E6061CF1C8CBBD8E82E8670B9ACE522915");
        xtcAppSignatureList.add("62655D7E6D39A156FB16F37D68240CC35F3E1250");
        xtcAppSignatureList.add("44F8D5AE3EB6020635430BFE938F46687D61F258");
        xtcAppSignatureList.add("36FFB6412C1A62F1C5A50BD122823D74552032AA");
        xtcAppSignatureList.add("2AC21ED652C367C5FB32F3A9EEE25FC393E56D03");
        xtcAppSignatureList.add("3470685E3B173442F48BD7A46D9628AD3EC02A31");
        xtcAppSignatureList.add("067D7A9714DEED65708B9413CD56DE569E05BC2D");
        xtcAppSignatureList.add("85CF40DE47E67C48198B941B5678017B8E1A51D0");
        xtcAppSignatureList.add("366A8DAB0D3023743F3E4CA8DC1406ED658081E2");
        xtcAppSignatureList.add("4990F7804D3966F2D283F850BD0853392B3B8705");
        xtcAppSignatureList.add("500045746689475B1044344BD77CEA3F7C335493");
        xtcAppSignatureList.add("50782996FBF6134990D97A7790B8B95045993182");
        xtcAppSignatureList.add("8D930EBD2FD5E6875670E9CBCBD6A9A94ACB51D8");
        xtcAppSignatureList.add("A77D4C1B214EA10ACE5AAF1C22B1CA68E7834FE3");
        xtcAppSignatureList.add("1B14E9B33349A494A960040F0AEC9368FA696A71");
        xtcAppSignatureList.add("53509046A0479442892464DD007BE1D28798EFB8");
        xtcAppSignatureList.add("C9D8C0BEBE65C47495C1015257BC33326648666E");
        xtcAppSignatureList.add("4AC6768564EE4F7B0B476BCCC04C6EA174D76A7D");
        xtcAppSignatureList.add("5DE9B82E2AE534334E73CCF29D054DEEFC8EB309");
        xtcAppSignatureList.add("38226F5442CD1B9CFE2E40FFDC8B9A3D5EB1F75C");
        xtcAppSignatureList.add("C6CCDAD74295497BA27FBD854404E77655988A7B");
        xtcAppSignatureList.add("28C3B6F6AA8B2F098E37CAEE842EA40E0BF1FAF4");
        xtcAppSignatureList.add("4A247E0BD102298DB2F05C3BA72A004D0BC1C4D3");
        xtcAppSignatureList.add("C101697C545037183B29C2DDFC76366EE592FB6B");
        xtcAppSignatureList.add("25B6BBDD78393552499FC2FBEB8EB18861EAFAF6");
        xtcAppSignatureList.add("51222D848EC7F4A68AF50359B802A428E7B8A436");
        xtcAppSignatureList.add("3870B9CCDCEFA3D1D4A4528F3FF862C65D65335E");
        xtcAppSignatureList.add("75E267917583092B7978FD86AE0C9F6426AE76DD");
        xtcAppSignatureList.add("92B0A350F6824A3D540EA86203BED033CBDB5DE8");
        xtcAppSignatureList.add("85B074862D2FDB5FAE6185F8D81312620A9EFD8E");
        xtcAppSignatureList.add("F2BFB9F77A7E3931596D172F6321657FEFEC82D3");
        xtcAppSignatureList.add("60F2A055735E7A83200AE901000E1AB472C0A405");
        xtcAppSignatureList.add("9A78389DC8C119E3115B9313BF010FF85F1001C4");
        xtcAppSignatureList.add("5C425C84D08BCF497C7F33CE7B9EA646A249DF17");
        xtcAppSignatureList.add("BF60842FE83BAAE2BD167AA44A7EEC761B434560");
        xtcAppSignatureList.add("E358CB054F8A85B44E4FC9E3030DD02C98B2DF8D");
        xtcAppSignatureList.add("D06C48151487F7F177B5C2531088A0878FD670F9");
        xtcAppSignatureList.add("90129CA425C5C88AB843689605C82F0167BD3BBB");
        xtcAppSignatureList.add("FB402BBB910CC46B08E9FED49BB04FCA3C0146AF");
        xtcAppSignatureList.add("F4B5962FA8279B28868CA8A2226EC51216E906FD");
        xtcAppSignatureList.add("00CC2C9D60CD337597056B473320991C0EF3825A");
        xtcAppSignatureList.add("A0FEDD45DF5750B1058D3B498DECF46E2868DA12");
        xtcAppSignatureList.add("35D2D23C98CB8B9CFEFF20E5909E34206D906172");
        xtcAppSignatureList.add("88B4AA3569B61ECC078C9E6C0BFF65EF908A856E");
        xtcAppSignatureList.add("37C8D620580FD6564EE3D3B44A347C981F89449B");
        xtcAppSignatureList.add("AD8D9BA838B33246903AF25E6E0F73ACC794B2CD");
        xtcAppSignatureList.add("A0B5ED1DE32E38F9694C22A871F7983EC16E4EC7");
        xtcAppSignatureList.add("D172263990AF4ED0562AC8787DB8B53A57BC4F92");
        xtcAppSignatureList.add("508C771711FFA515B1183930EC67CA2C55011C1A");
        xtcAppSignatureList.add("468C62BC610B3DBA2BAFEABBCE2D7BDAF15B6B25");
        xtcAppSignatureList.add("DA4D44C712231058C9A21E7A6E7ADF9D8BB7B9CC");
        xtcAppSignatureList.add("68719EEDC046EF552588E8B61374BD3985C9631D");
        xtcAppSignatureList.add("3E6D765D89ECFDCCFEE484CA78144385FBB6466C");
        xtcAppSignatureList.add("510AC374E105DC0FE2DDB5688E7271BA6DD1E15A");
        xtcAppSignatureList.add("4D071786108840117B9D7AFA595B11CE9D1E18CE");
        xtcAppSignatureList.add("269CCA7614A6DB26F97B4305B92C758824053E17");
        xtcAppSignatureList.add("A62E812C3DEE55F0CE5F2C9AB6A5FA8ADE19A7FC");
        xtcAppSignatureList.add("3E6BD5F09CE6F728F0841E7EDCF0D30FF5648CD9");
        xtcAppSignatureList.add("496D0C5AB97813582969B42D497124B26583DDF7");
        xtcAppSignatureList.add("DD2CB093CA3E41A1E8E7BA67D607A1101B8BB0FD");
        xtcAppSignatureList.add("0D20F7696C1459332714261F1141F8B6D22C55DE");
        xtcAppSignatureList.add("704E6414EC5324F233F8C58437A8F5654FE47E6E");
        xtcAppSignatureList.add("788F2C23EBE90845F06CE821322AC55AC8DA5F77");
        xtcAppSignatureList.add("94B4F36D1A30C9D438A1C0896989C66BC80DF0FB");
        xtcAppSignatureList.add("55BF9088B5F5A72FE5419992929B89A165B8BA9F");
        xtcAppSignatureList.add("37FF1CB4BDB034505235717CD1A3349364F9FF40");
        xtcAppSignatureList.add("35661E40369605E8AFC942BC46ED7545602D28B8");
        xtcAppSignatureList.add("349E7B04C826D886694A4A524E267C2342A2704D");
        xtcAppSignatureList.add("E36F46CCEEC5C9FC2D705C6F0A66B585E6690C5F");
        xtcAppSignatureList.add("A8C21162AF92C82DE2D2C988B7919DE066C15E7C");
        xtcAppSignatureList.add("283C9ECBAFF350D7C6FDD15D73B90FF02DE1B5F5");
        xtcAppSignatureList.add("50F7429F8998FDC81316724544536B9AE5A9A884");
        xtcAppSignatureList.add("7AF111BEEA408B716A8F81A606BDD1A85354FD76");
        xtcAppSignatureList.add("49197C1B9D9CA4F3464DDFD8D66ABC294393179E");
        xtcAppSignatureList.add("851C10E02A59ACA366F820B1A170940F39A2EA02");
        xtcAppSignatureList.add("F677E2C3500AF7FF9FF11A3F4CB515CE56F54933");
    }

    private static boolean check(String signatureHex) {
        for (String item : xtcAppSignatureList) {
            if (item.equalsIgnoreCase(signatureHex)) {
                return true;
            }
        }
        return false;
    }

    private static boolean compareToList(Signature[] signatures) throws NoSuchAlgorithmException {
        if (signatures == null) {
            Log.w(TAG, "sigs is null.");
            return false;
        }
        if (Binder.getCallingUid() != 1000) {
            Log.w(TAG, "Only system caller allowed.");
            return false;
        }
        List<byte[]> shaList = getSignatureSHA(signatures);
        if (shaList == null || shaList.isEmpty()) {
            return false;
        }
        for (byte[] sha : shaList) {
            if (check(XtcConvertUtil.byte2Hex(sha))) {
                return true;
            }
        }
        return false;
    }

    public static Signature[] getApkSignatures(Context context, String apkPath) {
        if (context == null || TextUtils.isEmpty(apkPath)) {
            Log.w(TAG, "getApkSignatures context:" + context + ", apkPath:" + apkPath);
            return null;
        }
        if (!new File(apkPath).exists()) {
            Log.w(TAG, "getApkSignatures apk not exists:" + apkPath);
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "getApkSignatures pm is null.");
            return null;
        }
        PackageInfo info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_SIGNATURES);
        if (info != null) {
            return info.signatures;
        }
        Log.w(TAG, "getApkSignatures PackageInfo is null.");
        return null;
    }

    public static Signature[] getAppSignatures(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "getAppSignatures context:" + context + ", packageName:" + packageName);
            return null;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            Log.w(TAG, "getAppSignatures pm is null.");
            return null;
        }
        PackageInfo info;
        try {
            info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, "Cannot getPackageInfo by packageName:" + packageName + ", ex:" + e);
            info = null;
        }
        if (info != null) {
            return info.signatures;
        }
        Log.w(TAG, "getAppSignatures PackageInfo is null.");
        return null;
    }

    public static List<byte[]> getSignatureSHA(Signature[] signatures)
            throws NoSuchAlgorithmException {
        if (signatures == null || signatures.length == 0) {
            Log.w(TAG, "getSignatureSHA PackageInfo has no signatures.");
            return null;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "No SHA algorithm ex:" + e);
            digest = null;
        }
        if (digest == null) {
            Log.w(TAG, "getSignatureSHA SHA can not be computed");
            return null;
        }
        List<byte[]> result = new ArrayList<>();
        for (Signature signature : signatures) {
            result.add(digest.digest(signature.toByteArray()));
        }
        return result;
    }

    public static boolean verify(Context context, File apkFile) {
        if (context != null && apkFile != null && apkFile.exists()) {
            try {
                return compareToList(getApkSignatures(context, apkFile.getAbsolutePath()));
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "verify apk failed", e);
                return false;
            }
        }
        Log.w(TAG, "context:" + context + ", apkFile:" + apkFile);
        return false;
    }

    public static boolean verify(Context context, String packageName) {
        try {
            return compareToList(getAppSignatures(context, packageName));
        } catch (NoSuchAlgorithmException e) {
            Log.w(TAG, "verify package failed", e);
            return false;
        }
    }
}