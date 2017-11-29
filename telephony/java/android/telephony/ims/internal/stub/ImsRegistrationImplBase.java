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

package android.telephony.ims.internal.stub;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.telephony.ims.internal.aidl.IImsRegistration;
import android.telephony.ims.internal.aidl.IImsRegistrationCallback;
import android.util.Log;

import com.android.ims.ImsReasonInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The class that controls IMS registration for this ImsService and notifies the framework when
 * the IMS registration for this ImsService has changed status.
 * @hide
 */

public class ImsRegistrationImplBase {

    private static final String LOG_TAG = "ImsRegistrationImplBase";

    // Defines the underlying radio technology type that we have registered for IMS over.
    @IntDef(flag = true,
            value = {
                    REGISTRATION_TECH_NONE,
                    REGISTRATION_TECH_LTE,
                    REGISTRATION_TECH_IWLAN
            })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ImsRegistrationTech {}
    /**
     * No registration technology specified, used when we are not registered.
     */
    public static final int REGISTRATION_TECH_NONE = -1;
    /**
     * IMS is registered to IMS via LTE.
     */
    public static final int REGISTRATION_TECH_LTE = 0;
    /**
     * IMS is registered to IMS via IWLAN.
     */
    public static final int REGISTRATION_TECH_IWLAN = 1;

    // Registration states, used to notify new ImsRegistrationImplBase#Callbacks of the current
    // state.
    private static final int REGISTRATION_STATE_DISCONNECTED = 0;
    private static final int REGISTRATION_STATE_PROCESSING = 1;
    private static final int REGISTRATION_STATE_CONNECTED = 2;
    private static final int REGISTRATION_STATE_SUSPENDED = 3;
    private static final int REGISTRATION_STATE_RESUMED = 4;



    /**
     * Callback class for receiving Registration callback events.
     */
    public static class Callback extends IImsRegistrationCallback.Stub {

        /**
         * Notifies the application when the device is connected to the IMS network.
         *
         * @param imsRadioTech the radio access technology. Valid values are defined in
         * {@link ImsRegistrationTech}.
         */
        @Override
        public void onRegistrationConnected(@ImsRegistrationTech int imsRadioTech) {

        }

        /**
         * Notifies the application when the device is trying to connect the IMS network.
         *
         * @param imsRadioTech the radio access technology. Valid values are defined in
         * {@link ImsRegistrationTech}.
         */
        @Override
        public void onRegistrationProcessing(@ImsRegistrationTech int imsRadioTech) {

        }

        /**
         * Notifies the application when the device is disconnected from the IMS network.
         *
         * @param imsRadioTech the radio access technology. Valid values are defined in
         * {@link ImsRegistrationTech}.
         *
         * @param info the {@link ImsReasonInfo} associated with why registration was disconnected.
         */
        @Override
        public void onRegistrationDisconnected(@ImsRegistrationTech int imsRadioTech,
                ImsReasonInfo info) {

        }

        /**
         * Notifies the application when its suspended IMS connection is resumed,
         * meaning the connection now allows throughput.
         * @param imsRadioTech Valid values are defined in {@link ImsRegistrationTech}.
         */
        @Override
        public void onRegistrationResumed(@ImsRegistrationTech int imsRadioTech) {

        }

        /**
         * Notifies the application when its current IMS connection is suspended,
         * meaning there is no data throughput.
         *
         * @param imsRadioTech Valid values are defined in {@link ImsRegistrationTech}.
         */
        @Override
        public void onRegistrationSuspended(@ImsRegistrationTech int imsRadioTech) {

        }

        /**
         * Notifies the application when the registration change triggered by
         * {@link #changeSupportedImsFeatures} has failed.
         *
         * @param info the {@link ImsReasonInfo} explaining why it failed.
         */
        @Override
        public void onChangeImsFeaturesFailed(ImsReasonInfo info) {

        }

        /**
         * Notifies the application when the IMS registration for this ImsService has changed. This
         * can happen when the ImsService needs to register for different IMS features than what
         * were originally returned by {@link #onQuerySupportedImsFeatures}
         *
         * @param info the new registration configuration that this ImsService supports.
         */
        @Override
        public void changeSupportedImsFeatures(ImsRegistrationConfiguration info) {

        }
    }

    private final IImsRegistration mBinder = new IImsRegistration.Stub() {

        @Override
        public ImsRegistrationConfiguration querySupportedImsFeatures() throws RemoteException {
            return querySupportedImsFeaturesInternal();
        }

        @Override
        public @ImsRegistrationTech int getRegistrationTechnology() throws RemoteException {
            return getConnectionType();
        }

        @Override
        public void registrationFeaturesChanged(ImsRegistrationConfiguration info) throws
                RemoteException {
            registrationFeaturesChangedInternal(info);
        }

        @Override
        public void addRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
            ImsRegistrationImplBase.this.addRegistrationCallback(c);
        }

        @Override
        public void removeRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
            ImsRegistrationImplBase.this.removeRegistrationCallback(c);
        }
    };

    private final Object mLock = new Object();
    // Locked on mLock
    private ImsRegistrationConfiguration mConfig;
    private final RemoteCallbackList<IImsRegistrationCallback> mCallbacks
            = new RemoteCallbackList<>();
    // Locked on mLock
    private @ImsRegistrationTech
    int mConnectionType = REGISTRATION_TECH_NONE;
    // Locked on mLock
    private int mRegistrationState = REGISTRATION_STATE_DISCONNECTED;
    // Locked on mLock
    private ImsReasonInfo mLastDisconnectCause;

    private ImsRegistrationConfiguration querySupportedImsFeaturesInternal() {
        synchronized (mLock) {
            return onQuerySupportedImsFeatures();
        }
    }

    private void registrationFeaturesChangedInternal(ImsRegistrationConfiguration c)
            throws RemoteException {
        if (c == null) {
            throw new RemoteException("Invalid Configuration.");
        }
        synchronized (mLock) {
            if(mConfig == null || !mConfig.equals(c)) {
                mConfig = c;
                onSupportedImsFeaturesChanged(c);
            }
        }
    }

    public final IImsRegistration getBinder() {
        return mBinder;
    }

    private void addRegistrationCallback(IImsRegistrationCallback c) throws RemoteException {
        mCallbacks.register(c);
        updateNewCallbackWithState(c);
    }

    private void removeRegistrationCallback(IImsRegistrationCallback c) {
        mCallbacks.unregister(c);
    }

    /**
     * @return a {@link ImsRegistrationConfiguration} containing the supported IMS features of this
     * ImsService that should be registered. This, possibly along with other features that this
     * ImsService does not support, will be returned by the framework in
     * {@link #onSupportedImsFeaturesChanged}.
     */
    public @NonNull ImsRegistrationConfiguration onQuerySupportedImsFeatures() {
        // Base implementation
        return new ImsRegistrationConfiguration((int[]) null);
    }

    /**
     * Called when the framework requests that this ImsService register for the features requested
     * in the provided {@link ImsRegistrationConfiguration}. This can contain features in
     * {@link ImsRegistrationConfiguration#getRegistrationFeatures()} that this ImsService did not
     * provide to the framework when {@link #onQuerySupportedImsFeatures} was called. This is
     * because the framework wishes to support single IMS registration and needs this ImsService to
     * register for those external features as well.
     *
     * This should be used to configure the features used for IMS Registration on the attached
     * network. And should also be used to trigger initial IMS registration when the ImsService is
     * first started.
     *
     * @param config Contains the IMS registration configuration.
     */
    public void onSupportedImsFeaturesChanged(ImsRegistrationConfiguration config) {
        // Base implementation
    }

    /**
     * @return The current {@link ImsRegistrationConfiguration} used for registration or
     * {@code null} if the framework has not triggered registration yet.
     */
    public final ImsRegistrationConfiguration getRegistrationConfiguration() {
        synchronized (mLock) {
            return mConfig;
        }
    }

    /**
     * Notify the framework that the device is connected to the IMS network.
     *
     * @param imsRadioTech the radio access technology. Valid values are defined in
     * {@link ImsRegistrationTech}.
     */
    public final void registrationConnected(@ImsRegistrationTech int imsRadioTech) {
        updateToState(imsRadioTech, REGISTRATION_STATE_CONNECTED);
        mCallbacks.broadcast((c) -> {
            try {
                c.onRegistrationConnected(imsRadioTech);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "onRegistrationConnected() - Skipping " +
                        "callback.");
            }
        });
    }

    /**
     * Notify the framework that the device is trying to connect the IMS network.
     *
     * @param imsRadioTech the radio access technology. Valid values are defined in
     * {@link ImsRegistrationTech}.
     */
    public final void registrationProcessing(@ImsRegistrationTech int imsRadioTech) {
        updateToState(imsRadioTech, REGISTRATION_STATE_PROCESSING);
        mCallbacks.broadcast((c) -> {
            try {
                c.onRegistrationProcessing(imsRadioTech);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "onRegistrationProcessing() - Skipping " +
                        "callback.");
            }
        });
    }

    /**
     * Notify the framework that the device is disconnected from the IMS network.
     *
     * @param imsRadioTech the radio access technology. Valid values are defined in
     * {@link ImsRegistrationTech}.
     * @param info the {@link ImsReasonInfo} associated with why registration was disconnected.
     */
    public final void registrationDisconnected(@ImsRegistrationTech int imsRadioTech,
            ImsReasonInfo info) {
        updateToDisconnectedState(info);
        mCallbacks.broadcast((c) -> {
            try {
                c.onRegistrationDisconnected(imsRadioTech, info);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "onRegistrationDisconnected() - Skipping " +
                        "callback.");
            }
        });
    }

    /**
     * Notify the framework that the previously suspended IMS connection is resumed, meaning the
     * connection now allows throughput.
     */
    public final void registrationResumed(@ImsRegistrationTech int imsRadioTech) {
        updateToState(imsRadioTech, REGISTRATION_STATE_RESUMED);
        mCallbacks.broadcast((c) -> {
            try {
                c.onRegistrationResumed(imsRadioTech);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "onRegistrationResumed() - Skipping " +
                        "callback.");
            }
        });
    }

    /**
     * Notify the framework that its current IMS connection is suspended, meaning there is no data
     * throughput.
     */
    public final void registrationSuspended(@ImsRegistrationTech int imsRadioTech) {
        updateToState(imsRadioTech, REGISTRATION_STATE_SUSPENDED);
        mCallbacks.broadcast((c) -> {
            try {
                c.onRegistrationSuspended(imsRadioTech);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "onRegistrationSuspended() - Skipping " +
                        "callback.");
            }
        });
    }

    /**
     * Notify the framework that the IMS feature change triggered by
     * {@link #onSupportedImsFeaturesChanged} has failed.
     * @param info the {@link ImsReasonInfo} explaining why it failed.
     */
    public final void changingSupportedImsFeaturesFailed(ImsReasonInfo info) {
        mCallbacks.broadcast((c) -> {
            try {
                c.onChangeImsFeaturesFailed(info);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "changingSupportedImsFeaturesFailed() - Skipping " +
                        "callback.");
            }
        });
    }

    /**
     * Notify the framework that supported IMS features for this ImsService has changed. This
     * can happen when the ImsService needs to register for new IMS features.
     *
     * @param config the new IMS Feature configuration that this ImsService supports.
     */
    public final void supportedImsFeaturesChanged(ImsRegistrationConfiguration config) {
        mCallbacks.broadcast((c) -> {
            try {
                c.changeSupportedImsFeatures(config);
            } catch (RemoteException e) {
                Log.w(LOG_TAG, e + " " + "supportedImsFeaturesChanged() - Skipping " +
                        "callback.");
            }
        });
    }

    private void updateToState(@ImsRegistrationTech int connType, int newState) {
        synchronized (mLock) {
            mConnectionType = connType;
            mRegistrationState = newState;
            mLastDisconnectCause = null;
        }
    }

    private void updateToDisconnectedState(ImsReasonInfo info) {
        synchronized (mLock) {
            updateToState(REGISTRATION_TECH_NONE, REGISTRATION_STATE_DISCONNECTED);
            if (info != null) {
                mLastDisconnectCause = info;
            } else {
                Log.w(LOG_TAG, "updateToDisconnectedState: no ImsReasonInfo provided.");
                mLastDisconnectCause = new ImsReasonInfo();
            }
        }
    }

    private @ImsRegistrationTech int getConnectionType() {
        synchronized (mLock) {
            return mConnectionType;
        }
    }

    /**
     * @param c the newly registered callback that will be updated with the current registration
     *         state.
     */
    private void updateNewCallbackWithState(IImsRegistrationCallback c) throws RemoteException {
        int state;
        ImsReasonInfo disconnectInfo;
        synchronized (mLock) {
            state = mRegistrationState;
            disconnectInfo = mLastDisconnectCause;
        }
        switch (state) {
            case REGISTRATION_STATE_DISCONNECTED: {
                c.onRegistrationDisconnected(getConnectionType(), disconnectInfo);
                break;
            }
            case REGISTRATION_STATE_PROCESSING: {
                c.onRegistrationProcessing(getConnectionType());
                break;
            }
            case REGISTRATION_STATE_CONNECTED: {
                c.onRegistrationConnected(getConnectionType());
                break;
            }
            case REGISTRATION_STATE_SUSPENDED: {
                c.onRegistrationSuspended(getConnectionType());
                break;
            }
            case REGISTRATION_STATE_RESUMED: {
                c.onRegistrationResumed(getConnectionType());
                break;
            }
        }
    }
}
