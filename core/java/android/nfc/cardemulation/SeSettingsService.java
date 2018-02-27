package android.nfc.cardemulation;

import android.annotation.CallSuper;
import android.app.Service;
import android.content.Intent;
import android.content.ComponentName;
import android.os.IBinder;

/**
 * A service that exposes seac-specific functionality to the system.
 * <p>
 * To extend this class, you must declare the service in your manifest file to require the
 * {@link android.Manifest.permission#BIND_SESETTINGS_SERVICES} permission and include an intent
 * filter with the {@link #SESETTINGS_SERVICE_INTERFACE}.
 * For example:
 * </p>
 *
 * <pre>{@code
 * <service android:name=".MySeSettingsService"
 *       android:label="@string/service_name"
 *       android:permission="android.permission.BIND_SESETTINGS_SERVICES">
 *  <intent-filter>
 *      <action android:name="android.nfc.cardemulation.SeSettingsService" />
 *  </intent-filter>
 * </service>
 * }</pre>
 */
public abstract class SeSettingsService extends Service {

    public static final String SESETTINGS_SERVICE_INTERFACE =
                                 "android.nfc.cardemulation.SeSettingsService";

    private final ISeSettingsService.Stub mStubWrapper;

    public SeSettingsService() {
        mStubWrapper = new ISeSettingsServiceWrapper();
    }

    /**
     * Override this method to set seac state.
     * <p>
     * This method will be called by nfc services to set seac state (ex: Uicc, Ese, Sd ).
     * </p>
     * @param service  default service.
     */
    public abstract void setSeacActive(ComponentName service, boolean foreground);

    /**
     * If overriding this method, call through to the super method for any unknown actions.
     * {@inheritDoc}
     */
    @Override
    @CallSuper
    public IBinder onBind(Intent intent) {
        return mStubWrapper;
    }

    /**
     * A wrapper around ISeSettingsService that forwards calls to implementations of
     * {@link SeSettingsService}.
     */
    private class ISeSettingsServiceWrapper extends ISeSettingsService.Stub {
        @Override
        public void setSeacActive(ComponentName service, boolean foreground) {
            SeSettingsService.this.setSeacActive(service, foreground);
        }
    }
}
