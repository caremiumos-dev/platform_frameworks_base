package android.telecom;

import android.text.TextUtils;
import android.util.Base64;

/**
 * XTC PII "encryption" helper.
 *
 * <p><b>Note.</b> Despite the name, this is only Base64 encoding, not
 * encryption; it provides no confidentiality. Reproduced for source fidelity.
 *
 * @hide
 */
public class XtcSecurePii {
    public static String decrypt(String str) {
        return TextUtils.isEmpty(str) ? "" : new String(Base64.decode(str, 0));
    }

    public static String encrypt(String str) {
        return TextUtils.isEmpty(str) ? ""
                : Base64.encodeToString(str.getBytes(), 0).replaceAll("[\\s*\t\n\r]", "");
    }
}