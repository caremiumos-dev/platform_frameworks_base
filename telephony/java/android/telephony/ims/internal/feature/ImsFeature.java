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
 * limitations under the License
 */

package android.telephony.ims.internal.feature;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.os.IInterface;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.SubscriptionManager;
import android.telephony.ims.internal.aidl.IImsCapabilityCallback;
import android.util.Log;

import com.android.ims.internal.IImsFeatureStatusCallback;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Base class for all IMS features that are supported by the framework.
 * @hide
 */
public abstract class ImsFeature {

    private static final String LOG_TAG = "ImsFeature";

    /**
     * Action to broadcast when ImsService is up.
     * Internal use only.
     * Only defined here separately compatibility purposes with the old ImsService.
     * @hide
     */
    public static final String ACTION_IMS_SERVICE_UP =
            "com.android.ims.IMS_SERVICE_UP";

    /**
     * Action to broadcast when ImsService is down.
     * Internal use only.
     * Only defined here separately for compatibility purposes with the old ImsService.
     * @hide
     */
    public static final String ACTION_IMS_SERVICE_DOWN =
            "com.android.ims.IMS_SERVICE_DOWN";

    /**
     * Part of the ACTION_IMS_SERVICE_UP or _DOWN intents.
     * A long value; the phone ID corresponding to the IMS service coming up or down.
     * Only defined here separately for compatibility purposes with the old ImsService.
     * @hide
     */
    public static final String EXTRA_PHONE_ID = "android:phone_id";

    // Invalid feature value
    public static final int INVALID = -1;
    // ImsFeatures that are defined in the Manifests. Ensure that these values match the previously
    // defined values in ImsServiceClass for compatibility purposes.
    public static final int EMERGENCY_MMTEL = 0;
    public static final int MMTEL = 1;
    public static final int RCS = 2;
    // Total number of features defined
    public static final int MAX = 3;

    // Integer values defining the state of the ImsFeature at any time.
    @IntDef(flag = true,
            value = {
                    STATE_NOT_AVAILABLE,
                    STATE_INITIALIZING,
                    STATE_READY,
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsState {}
    public static final int STATE_NOT_AVAILABLE = 0;
    public static final int STATE_INITIALIZING = 1;
    public static final int STATE_READY = 2;

    // Integer values defining the result codes that should be returned from
    // {@link onSetCapabilityValues} when the framework tries to set a feature's capability.
    @IntDef(flag = true,
            value = {
                    CAPABILITY_ERROR_GENERIC,
                    CAPABILITY_SUCCESS
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsCapabilityError {}
    public static final int CAPABILITY_ERROR_GENERIC = -1;
    public static final int CAPABILITY_SUCCESS = 0;


    public static class CapabilityCallback extends IImsCapabilityCallback.Stub {
        @Override
        public final void onQueryCapabilities(long config) throws RemoteException {
            onQueryCapabilities(new CapabilityConfiguration(config));
        }

        @Override
        public final void onCapabilitiesChanged(long config) throws RemoteException {
            onCapabilitiesChanged(new CapabilityConfiguration(config));
        }

        @Override
        public final void onCapabilitiesStatusChanged(long config) throws RemoteException {
            onCapabilitiesStatusChanged(new CapabilityConfiguration(config));
        }

        @Override
        public void onSetCapabilitiesError(int result) throws RemoteException {
        }

        public void onQueryCapabilities(CapabilityConfiguration config) {
        }

        public void onCapabilitiesChanged(CapabilityConfiguration config) {
        }

        public void onCapabilitiesStatusChanged(CapabilityConfiguration config) {
        }
    }

    /**
     * Contains the capabilities defined and supported by an ImsFeature in the form of a bit mask.
     */
    public static class CapabilityConfiguration {
        protected long mCapabilities = 0;

        public CapabilityConfiguration() {
        }

        protected CapabilityConfiguration(long capabilities) {
            mCapabilities = capabilities;
        }

        /**
         * @param capabilities Capabilities to be added to the configuration in the form of a
         *         bit mask.
         */
        public void addCapabilities(long capabilities) {
            mCapabilities |= capabilities;
        }

        /**
         * @param capabilities Capabilities to be removed to the configuration in the form of a
         *         bit mask.
         */
        public void removeCapabilities(long capabilities) {
            mCapabilities &= ~capabilities;
        }

        /**
         * @return true if all of the capabilities specified are capable.
         */
        public boolean isCapable(long capabilities) {
            return (mCapabilities & capabilities) == capabilities;
        }
        public CapabilityConfiguration clone() {
            return new CapabilityConfiguration(mCapabilities);
        }

        public long getMask() {
            return mCapabilities;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CapabilityConfiguration)) return false;

            CapabilityConfiguration that = (CapabilityConfiguration) o;

            return mCapabilities == that.mCapabilities;
        }

        @Override
        public int hashCode() {
            return (int) (mCapabilities ^ (mCapabilities >>> 32));
        }
    }

    private final Set<IImsFeatureStatusCallback> mStatusCallbacks = Collections.newSetFromMap(
            new WeakHashMap<IImsFeatureStatusCallback, Boolean>());
    private @ImsState int mState = STATE_NOT_AVAILABLE;
    private int mSlotId = SubscriptionManager.INVALID_SIM_SLOT_INDEX;
    private Context mContext;
    private final Object mLock = new Object();
    private final RemoteCallbackList<IImsCapabilityCallback> mCapabilityCallbacks
            = new RemoteCallbackList<>();
    private CapabilityConfiguration mCapabilities = new CapabilityConfiguration();
    private CapabilityConfiguration mCapabilityStatus = new CapabilityConfiguration();

    public final void initialize(Context context, int slotId) {
        mContext = context;
        mSlotId = slotId;
    }

    public final int getFeatureState() {
        synchronized (mLock) {
            return mState;
        }
    }

    protected final void setFeatureState(@ImsState int state) {
        synchronized (mLock) {
            if (mState != state) {
                mState = state;
                notifyFeatureState(state);
            }
        }
    }

    public final void addImsFeatureStatusCallback(@NonNull IImsFeatureStatusCallback c) {
        try {
            // If we have just connected, send queued status.
            c.notifyImsFeatureStatus(getFeatureState());
            // Add the callback if the callback completes successfully without a RemoteException.
            synchronized (mLock) {
                mStatusCallbacks.add(c);
            }
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "Couldn't notify feature state: " + e.getMessage());
        }
    }

    public final void removeImsFeatureStatusCallback(@NonNull IImsFeatureStatusCallback c) {
        synchronized (mLock) {
            mStatusCallbacks.remove(c);
        }
    }

    /**
     * Internal method called by ImsFeature when setFeatureState has changed.
     * @param state
     */
    private void notifyFeatureState(@ImsState int state) {
        synchronized (mLock) {
            for (Iterator<IImsFeatureStatusCallback> iter = mStatusCallbacks.iterator();
                 iter.hasNext(); ) {
                IImsFeatureStatusCallback callback = iter.next();
                try {
                    Log.i(LOG_TAG, "notifying ImsFeatureState=" + state);
                    callback.notifyImsFeatureStatus(state);
                } catch (RemoteException e) {
                    // remove if the callback is no longer alive.
                    iter.remove();
                    Log.w(LOG_TAG, "Couldn't notify feature state: " + e.getMessage());
                }
            }
        }
        sendImsServiceIntent(state);
    }

    /**
     * Provide backwards compatibility using deprecated service UP/DOWN intents.
     */
    private void sendImsServiceIntent(@ImsState int state) {
        if(mContext == null || mSlotId == SubscriptionManager.INVALID_SIM_SLOT_INDEX) {
            return;
        }
        Intent intent;
        switch (state) {
            case ImsFeature.STATE_NOT_AVAILABLE:
            case ImsFeature.STATE_INITIALIZING:
                intent = new Intent(ACTION_IMS_SERVICE_DOWN);
                break;
            case ImsFeature.STATE_READY:
                intent = new Intent(ACTION_IMS_SERVICE_UP);
                break;
            default:
                intent = new Intent(ACTION_IMS_SERVICE_DOWN);
        }
        intent.putExtra(EXTRA_PHONE_ID, mSlotId);
        mContext.sendBroadcast(intent);
    }


    public final void addCapabilityCallback(IImsCapabilityCallback c) {
        mCapabilityCallbacks.register(c);
    }

    public final void removeCapabilityCallback(IImsCapabilityCallback c) {
        mCapabilityCallbacks.unregister(c);
    }

    @VisibleForTesting
    public CapabilityConfiguration queryCapabilities() {
        synchronized (mLock) {
            return mCapabilities.clone();
        }
    }

    @VisibleForTesting
    public CapabilityConfiguration queryCapabilityStatus() {
        synchronized (mLock) {
            return mCapabilityStatus.clone();
        }
    }

    /**
     * Sets the user defined capabilities of this feature.
     *
     * @param capabilities {@link CapabilityConfiguration} containing the new capabilities of this
     *         feature
     * @param c {@link IImsCapabilityCallback} calls
     *         {@link IImsCapabilityCallback#onSetCapabilitiesError(int)} if there was an error
     *         setting the new capabilities.
     *
     */
    @VisibleForTesting
    public final void setCapabilities(CapabilityConfiguration capabilities,
            IImsCapabilityCallback c) throws RemoteException {
        if (capabilities == null) {
            throw new IllegalArgumentException(
                    "ImsFeature#setCapabilities called with invalid params.");
        }
        int result;
        synchronized (mLock) {
            result = onSetCapabilityValues(capabilities);
            if(result == CAPABILITY_SUCCESS) {
                mCapabilities = capabilities;
            }
        }
        if(result == CAPABILITY_SUCCESS) {
            notifyCapabilitiesChanged(capabilities);
        } else {
            try {
                if (c != null) {
                    c.onSetCapabilitiesError(result);
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "setCapabilities - error: " + e.getMessage());
            }
        }
    }

    void addCapability(long capability, IImsCapabilityCallback callback) throws RemoteException {
        // Clone current capabilities
        CapabilityConfiguration c = queryCapabilities();
        // add new capability to current capabilities
        c.addCapabilities(capability);
        // notify implementer of new capabilities and try to set.
        setCapabilities(c, callback);
    }

    void removeCapability(long capability, IImsCapabilityCallback callback) throws RemoteException {
        // Clone current capabilities
        CapabilityConfiguration c = queryCapabilities();
        // remove capability from current capabilities
        c.removeCapabilities(capability);
        // notify implementer of new capabilities and try to set.
        setCapabilities(c, callback);
    }

    public @ImsCapabilityError int onSetCapabilityValues(CapabilityConfiguration capabilities) {
        return CAPABILITY_ERROR_GENERIC;
    }

    public final void notifyCapabilitiesChanged(CapabilityConfiguration c) {
        synchronized (mLock) {
            mCapabilityStatus = c.clone();
        }
        int count = mCapabilityCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < count; i++) {
                try {
                    mCapabilityCallbacks.getBroadcastItem(i).onCapabilitiesChanged(
                            c.mCapabilities);
                } catch (RemoteException e) {
                    Log.w(LOG_TAG, e + " " + "notifyCapabilitiesChanged() - Skipping " +
                            "callback.");
                }
            }
        } finally {
            mCapabilityCallbacks.finishBroadcast();
        }
    }

    public final void notifyCapabilitiesStatusChanged(CapabilityConfiguration c) {
        synchronized (mLock) {
            mCapabilityStatus = c.clone();
        }
        int count = mCapabilityCallbacks.beginBroadcast();
        try {
            for (int i = 0; i < count; i++) {
                try {
                    mCapabilityCallbacks.getBroadcastItem(i).onCapabilitiesStatusChanged(
                            c.mCapabilities);
                } catch (RemoteException e) {
                    Log.w(LOG_TAG, e + " " + "notifyCapabilitiesStatusChanged() - Skipping " +
                            "callback.");
                }
            }
        } finally {
            mCapabilityCallbacks.finishBroadcast();
        }
    }

    /**
     * Called when the feature is being removed and must be cleaned up.
     */
    public abstract void onFeatureRemoved();

    /**
     * Called when the feature has been initialized by the framework and is ready.
     */
    public abstract void onFeatureReady();

    /**
     * @return Binder instance
     */
    public abstract IInterface getBinder();
}
