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
import android.os.Message;
import android.os.RemoteException;
import android.telecom.TelecomManager;
import android.telephony.ims.internal.ImsCallSessionListener;
import android.telephony.ims.internal.aidl.IImsCallSessionListener;
import android.telephony.ims.internal.aidl.IImsCapabilityCallback;
import android.telephony.ims.internal.aidl.IImsMmTelFeature;
import android.telephony.ims.internal.aidl.IImsMmTelListener;
import android.telephony.ims.stub.ImsEcbmImplBase;
import android.telephony.ims.stub.ImsMultiEndpointImplBase;
import android.telephony.ims.stub.ImsUtImplBase;

import com.android.ims.ImsCallProfile;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.ImsCallSession;
import com.android.internal.annotations.VisibleForTesting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Base implementation for Voice and SMS (IR-92) and Video (IR-94) IMS support.
 *
 * Any class wishing to use MmTelFeature should extend this class and implement all methods that the
 * service supports.
 * @hide
 */

public class MmTelFeature extends ImsFeature {

    private final IImsMmTelFeature mImsMMTelBinder = new IImsMmTelFeature.Stub() {

        @Override
        public void setListener(IImsMmTelListener l) throws RemoteException {
            synchronized (mLock) {
                MmTelFeature.this.setListener(l);
            }
        }

        @Override
        public boolean isConnected(int callSessionType, int callType)
                throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.isConnected(callSessionType, callType);
            }
        }

        @Override
        public boolean isOpened() throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.isOpened();
            }
        }

        @Override
        public int getFeatureStatus() throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.getFeatureState();
            }
        }


        @Override
        public ImsCallProfile createCallProfile(int callSessionType, int callType)
                throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.createCallProfile(callSessionType,  callType);
            }
        }

        @Override
        public IImsCallSession createCallSession(ImsCallProfile profile,
                IImsCallSessionListener listener) throws RemoteException {
            synchronized (mLock) {
                ImsCallSession s = MmTelFeature.this.createCallSession(profile,
                        new ImsCallSessionListener(listener));
                return s != null ? s.getSession() : null;
            }
        }

        @Override
        public IImsUt getUtInterface() throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.getUt();
            }
        }

        @Override
        public IImsEcbm getEcbmInterface() throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.getEcbm();
            }
        }

        @Override
        public void setUiTtyMode(int uiTtyMode, Message onCompleteMessage) throws RemoteException {
            synchronized (mLock) {
                MmTelFeature.this.setUiTtyMode(uiTtyMode, onCompleteMessage);
            }
        }

        @Override
        public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
            synchronized (mLock) {
                return MmTelFeature.this.getMultiEndpoint();
            }
        }

        @Override
        public void addCapabilityCallback(IImsCapabilityCallback c) {
            MmTelFeature.this.addCapabilityCallback(c);
        }

        @Override
        public void removeCapabilityCallback(IImsCapabilityCallback c) {
            MmTelFeature.this.removeCapabilityCallback(c);
        }

        @Override
        public void setCapabilities(long capabilities, IImsCapabilityCallback c)
                throws RemoteException {
            MmTelFeature.this.setCapabilities(new CapabilityConfiguration(capabilities), c);
        }

        @Override
        public void addCapability(long capability, IImsCapabilityCallback c)
                throws RemoteException {
            MmTelFeature.this.addCapability(capability, c);
        }

        @Override
        public void removeCapability(long capability, IImsCapabilityCallback c
        ) throws RemoteException {
            MmTelFeature.this.removeCapability(capability, c);
        }

        @Override
        public long queryCapabilities() throws RemoteException {
            return MmTelFeature.this.queryCapabilities().mCapabilities;
        }

        @Override
        public long queryCapabilityStatus() throws RemoteException {
            return MmTelFeature.this.queryCapabilityStatus().mCapabilities;
        }
    };

    /**
     * Contains the capabilities defined and supported by a MmTelFeature in the form of a Bitmask.
     * The capabilities that are used in MmTelFeature are defined by {@link MMTelCapability}.
     *
     * The capabilities of this MmTelFeature will be set by the framework and can be queried with
     * {@link #queryCapabilities()}.
     *
     * This MmTelFeature can then return the status of each of these capabilities (enabled or not)
     * by sending a {@link #notifyCapabilitiesStatusChanged} callback to the framework. The current
     * status can also be queried using {@link #queryCapabilityStatus()}.
     */
    public static class MmTelCapabilityConfiguration extends CapabilityConfiguration {

        @VisibleForTesting
        public MmTelCapabilityConfiguration() {
            super();
        }

        public MmTelCapabilityConfiguration(CapabilityConfiguration c) {
            mCapabilities = c.mCapabilities;
        }

        @IntDef(flag = true,
                value = {
                        CAPABILITY_TYPE_VOICE,
                        CAPABILITY_TYPE_VIDEO,
                        CAPABILITY_TYPE_UT
                })
        @Retention(RetentionPolicy.SOURCE)
        public @interface MMTelCapability {}

        /**
         * This MmTelFeature supports Voice calling (IR.92)
         */
        public static final int CAPABILITY_TYPE_VOICE = 1 << 0;
        /**
         * This MmTelFeature supports Video (IR.94)
         */
        public static final int CAPABILITY_TYPE_VIDEO = 1 << 1;
        /**
         * This MmTelFeature supports XCAP over Ut (IR.92)
         */
        public static final int CAPABILITY_TYPE_UT = 1 << 2;

        @Override
        public final void addCapabilities(@MMTelCapability long capabilities) {
            super.addCapabilities(capabilities);
        }

        @Override
        public final void removeCapabilities(@MMTelCapability long capability) {
            super.removeCapabilities(capability);
        }

        @Override
        public final boolean isCapable(@MMTelCapability long capabilities) {
            return super.isCapable(capabilities);
        }
    }

    public static class Listener extends IImsMmTelListener.Stub {

        @Override
        public final void onIncomingCall(IImsCallSession c) {
            onIncomingCall(new ImsCallSession(c));
        }

        public void onIncomingCall(ImsCallSession c) {
        }
    }

    // Lock for feature synchronization
    private final Object mLock = new Object();
    private IImsMmTelListener mListener;

    /**
     * @param listener A {@link Listener} used when the MmTelFeature receives an incoming call and
     *     notifies the framework.
     */
    private void setListener(IImsMmTelListener listener) {
        synchronized (mLock) {
            mListener = listener;
        }
    }

    /**
     * The current MmTelFeature capabilities defined by the framework. These are the capabilities
     * that the framework has defined are POSSIBLE for the MmTelFeature to have. The MmTelFeature
     * can then tell the framework when these capabilities are available using
     * {@link #notifyCapabilitiesStatusChanged}.
     * @return A copy of the current MmTelFeature capabilities.
     */
    @Override
    public final MmTelCapabilityConfiguration queryCapabilities() {
        return new MmTelCapabilityConfiguration(super.queryCapabilities());
    }

    /**
     * The current capability status that this MmTelFeature has defined is available. This
     * configuration will be used by the platform to figure out which capabilities are CURRENTLY
     * available to be used.
     *
     * Should be a subset of the capabilities that are set by the framework in
     * {@link #queryCapabilities()}.
     * @return A copy of the current MmTelFeature capability status.
     */
    @Override
    public final MmTelCapabilityConfiguration queryCapabilityStatus() {
        return new MmTelCapabilityConfiguration(super.queryCapabilityStatus());
    }

    @Override
    public final int onSetCapabilityValues(CapabilityConfiguration capabilities) {
        return onSetCapabilityValues(new MmTelCapabilityConfiguration(capabilities));
    }

    /**
     * Notify the framework that the status of the Capabilities has changed. Even though the
     * MmTelFeature capability may be enabled by the framework, the status may be disabled due to
     * the feature being unavailable from the network.
     * @param c The current capability status of the MmTelFeature. If a capability is disabled, then
     * the status of that capability is disabled. This can happen if the network does not currently
     * support the capability that is enabled. A capability that is disabled by the framework (via
     * {@link #queryCapabilities()}) should also show the status as disabled.
     */
    protected final void notifyCapabilitiesStatusChanged(MmTelCapabilityConfiguration c) {
        super.notifyCapabilitiesStatusChanged(c);
    }

    /**
     * Notify the framework of an incoming call.
     * @param c The {@link ImsCallSession} of the new incoming call.
     *
     * @throws RemoteException if the connection to the framework is not available. If this happens,
     *     the call should be no longer considered active and should be cleaned up.
     * */
    protected final void notifyIncomingCall(ImsCallSession c) throws RemoteException {
        synchronized (mLock) {
            if (mListener == null) {
                throw new IllegalStateException("Session is not available.");
            }
            mListener.onIncomingCall(c.getSession());
        }
    }

    /**
     * Notifies the ImsService when the user defined MMTel capabilities have changed.
     * @param c New Configuration
     * @return An integer code, defined by {@link ImsFeature.ImsCapabilityError}, that notifies
     * the framework whether or not the operation has succeeded.
     */
    public @ImsCapabilityError int onSetCapabilityValues(MmTelCapabilityConfiguration c) {
        // Base Implementation - Should be overridden
        return ImsFeature.CAPABILITY_ERROR_GENERIC;
    }

    /**
     * Checks if the IMS service has successfully registered to the IMS network with the specified
     * service & call type.
     *
     * @param callSessionType a service type that is specified in {@link ImsCallProfile}
     *        {@link ImsCallProfile#SERVICE_TYPE_NORMAL}
     *        {@link ImsCallProfile#SERVICE_TYPE_EMERGENCY}
     * @param callType a call type that is specified in {@link ImsCallProfile}
     *        {@link ImsCallProfile#CALL_TYPE_VOICE_N_VIDEO}
     *        {@link ImsCallProfile#CALL_TYPE_VOICE}
     *        {@link ImsCallProfile#CALL_TYPE_VT}
     *        {@link ImsCallProfile#CALL_TYPE_VS}
     * @return true if the specified service id is connected to the IMS network; false otherwise
     */
    public boolean isConnected(int callSessionType, int callType) {
        // Base Implementation - Should be overridden
        return false;
    }

    /**
     * Checks if the specified IMS service is opened.
     *
     * @return true if the specified service id is opened; false otherwise
     */
    boolean isOpened() {
        // Base Implementation - Should be overridden
        return false;
    }

    /**
     * Creates a {@link ImsCallProfile} from the service capabilities & IMS registration state.
     *
     * @param callSessionType a service type that is specified in {@link ImsCallProfile}
     *        {@link ImsCallProfile#SERVICE_TYPE_NONE}
     *        {@link ImsCallProfile#SERVICE_TYPE_NORMAL}
     *        {@link ImsCallProfile#SERVICE_TYPE_EMERGENCY}
     * @param callType a call type that is specified in {@link ImsCallProfile}
     *        {@link ImsCallProfile#CALL_TYPE_VOICE}
     *        {@link ImsCallProfile#CALL_TYPE_VT}
     *        {@link ImsCallProfile#CALL_TYPE_VT_TX}
     *        {@link ImsCallProfile#CALL_TYPE_VT_RX}
     *        {@link ImsCallProfile#CALL_TYPE_VT_NODIR}
     *        {@link ImsCallProfile#CALL_TYPE_VS}
     *        {@link ImsCallProfile#CALL_TYPE_VS_TX}
     *        {@link ImsCallProfile#CALL_TYPE_VS_RX}
     * @return a {@link ImsCallProfile} object
     */
    public ImsCallProfile createCallProfile(int callSessionType, int callType) {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * Creates an {@link ImsCallSession} with the specified call profile.
     * Use other methods, if applicable, instead of interacting with
     * {@link ImsCallSession} directly.
     *
     * @param profile a call profile to make the call
     * @param listener An implementation of IImsCallSessionListener.
     */
    public ImsCallSession createCallSession(ImsCallProfile profile,
            ImsCallSessionListener listener) {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * @return The Ut interface for the supplementary service configuration.
     */
    public ImsUtImplBase getUt() {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * @return The Emergency call-back mode interface for emergency VoLTE calls that support it.
     */
    public ImsEcbmImplBase getEcbm() {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * @return The Emergency call-back mode interface for emergency VoLTE calls that support it.
     */
    public ImsMultiEndpointImplBase getMultiEndpoint() {
        // Base Implementation - Should be overridden
        return null;
    }

    /**
     * Sets the current UI TTY mode for the MmTelFeature.
     * @param mode An integer containing the new UI TTY Mode, can consist of
     *         {@link TelecomManager#TTY_MODE_OFF},
     *         {@link TelecomManager#TTY_MODE_FULL},
     *         {@link TelecomManager#TTY_MODE_HCO},
     *         {@link TelecomManager#TTY_MODE_VCO}
     * @param onCompleteMessage A {@link Message} to be used when the mode has been set.
     */
    void setUiTtyMode(int mode, Message onCompleteMessage) {
        // Base Implementation - Should be overridden
    }

    /**{@inheritDoc}*/
    @Override
    public void onFeatureRemoved() {
        // Base Implementation - Should be overridden
    }

    /**{@inheritDoc}*/
    @Override
    public void onFeatureReady() {
        // Base Implementation - Should be overridden
    }

    /**
     * @hide
     */
    @Override
    public final IImsMmTelFeature getBinder() {
        return mImsMMTelBinder;
    }
}
