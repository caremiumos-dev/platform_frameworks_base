package android.nfc.cardemulation;

import android.content.ComponentName;

/**
 * Service used to expose seac-specific functionality to the system.
 *
 * @see android.nfc.cardemulation.SeSettingsService
 * @hide
 */
interface ISeSettingsService {

    /** @see android.nfc.cardemulation.SeSettingsService#setSeacActive */
    void setSeacActive(in ComponentName service, boolean foreground);
}
