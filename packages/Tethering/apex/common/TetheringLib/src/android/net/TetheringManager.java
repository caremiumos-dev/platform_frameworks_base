/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static android.net.ConnectivityManager.TETHER_ERROR_NO_ERROR;

import android.annotation.SystemApi;
import android.content.Context;
import android.net.ConnectivityManager.OnTetheringEventCallback;
import android.os.Binder;
import android.os.ConditionVariable;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.Log;

import java.io.PrintWriter;
import java.util.StringJoiner;
import java.util.concurrent.Executor;

/**
 * Service used to communicate with the tethering, which is running in a separate module.
 *
 * @hide
 */
@SystemApi
public class TetheringManager {
    private static final String TAG = TetheringManager.class.getSimpleName();

    private static TetheringManager sInstance;

    private final ITetheringConnector mConnector;
    private final TetheringCallbackInternal mCallback;
    private final Context mContext;
    private final ArrayMap<OnTetheringEventCallback, ITetheringEventCallback>
            mTetheringEventCallbacks = new ArrayMap<>();

    private Network mTetherUpstream;
    private TetheringConfigurationParcel mTetheringConfiguration;
    private TetherStatesParcel mTetherStatesParcel;
    private volatile boolean mTetherRestricted;

    /**
     * @hide
     */
    @SystemApi
    public TetheringManager(Context context, IBinder service) throws RemoteException {
        mContext = context;
        mConnector = ITetheringConnector.Stub.asInterface(service);
        mCallback = new TetheringCallbackInternal();

        String pkgName = mContext.getOpPackageName();
        Log.i(TAG, "registerTetheringEventCallback:" + pkgName);
        mConnector.registerTetheringEventCallback(mCallback, pkgName);
    }

    private class TetheringCallbackInternal extends ITetheringEventCallback.Stub {
        private final ConditionVariable mWaitForCallback = new ConditionVariable(false);
        private final int EVENT_CALLBACK_TIMEOUT_MS = 60_000;

        @Override
        public void onCallbackCreated(Network network, TetheringConfigurationParcel config,
                TetherStatesParcel states, boolean isRestricted) {
            mTetherUpstream = network;
            mTetheringConfiguration = config;
            mTetherStatesParcel = states;
            mTetherRestricted = isRestricted;
            mWaitForCallback.open();
        }

        @Override
        public void onUpstreamChanged(Network network) {
            mTetherUpstream = network;
        }

        @Override
        public void onConfigurationChanged(TetheringConfigurationParcel config) {
            mTetheringConfiguration = config;
        }

        @Override
        public void onTetherStatesChanged(TetherStatesParcel states) {
            mTetherStatesParcel = states;
        }

        @Override
        public void onTetheringRestricted(boolean isRestricted) {
            mTetherRestricted = isRestricted;
        }

        boolean awaitCallbackCreation() {
            return mWaitForCallback.block(EVENT_CALLBACK_TIMEOUT_MS);
        }
    }

    /**
     * Attempt to tether the named interface.  This will setup a dhcp server
     * on the interface, forward and NAT IP v4 packets and forward DNS requests
     * to the best active upstream network interface.  Note that if no upstream
     * IP network interface is available, dhcp will still run and traffic will be
     * allowed between the tethered devices and this device, though upstream net
     * access will of course fail until an upstream network interface becomes
     * active. Note: return value do not have any meaning. It is better to use
     * #getTetherableIfaces() to ensure corresponding interface is available for
     * tethering before calling #tether().
     *
     * TODO: Deprecate this API. The only usages should be in PanService and Wifi P2P which
     * need direct access.
     * @hide
     */
    @SystemApi
    public int tether(String iface, String callerPkg) throws RemoteException {
        mConnector.tether(iface, callerPkg);
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Stop tethering the named interface.
     *
     */
    public int untether(String iface, String callerPkg) throws RemoteException {
        mConnector.untether(iface, callerPkg);
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Attempt to both alter the mode of USB and Tethering of USB. WARNING: New client should not
     * use this API anymore. All clients should use #startTethering or #stopTethering which
     * encapsulate proper entitlement logic. Using this API while entitlement check is needed, usb
     * tethering would not have upstream if entitlement check is not lunched.
     *
     * @hide
     */
    @SystemApi
    public int setUsbTethering(boolean enable, String callerPkg) throws RemoteException {
        mConnector.setUsbTethering(enable, callerPkg);
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Starts tethering and runs tether provisioning for the given type if needed. If provisioning
     * fail, tethering would be closed automatically.
     *
     * @hide
     */
    @SystemApi
    public void startTethering(int type, ResultReceiver receiver, boolean showProvisioningUi,
            String callerPkg) throws RemoteException {
        mConnector.startTethering(type, receiver, showProvisioningUi, callerPkg);
    }

    /**
     * Stops tethering for the given type. Also cancels any provisioning rechecks for that type if
     * applicable.
     *
     * @hide
     */
    @SystemApi
    public void stopTethering(int type, String callerPkg) throws RemoteException {
        mConnector.stopTethering(type, callerPkg);
    }

    /**
     * Get the latest value of the tethering entitlement check.
     *
     * Note: Allow privileged apps who have TETHER_PRIVILEGED permission to access. If it turns
     * out some such apps are observed to abuse this API, change to per-UID limits on this API
     * if it's really needed.
     *
     * @hide
     */
    @SystemApi
    public void getLatestTetheringEntitlementResult(int type, ResultReceiver receiver,
            boolean showEntitlementUi, String callerPkg) throws RemoteException {
        mConnector.getLatestTetheringEntitlementResult(type, receiver, showEntitlementUi, callerPkg);
    }

    /**
     * Register tethering event callback.
     *
     * @hide
     */
    @SystemApi
    public void registerTetheringEventCallback(Executor executor,
            OnTetheringEventCallback callback, String callerPkg) throws RemoteException {
        synchronized (mTetheringEventCallbacks) {
            if (!mTetheringEventCallbacks.containsKey(callback)) {
                throw new IllegalArgumentException("callback was already registered.");
            }
            ITetheringEventCallback remoteCallback = new ITetheringEventCallback.Stub() {
                @Override
                public void onUpstreamChanged(Network network) throws RemoteException {
                    executor.execute(() -> {
                        callback.onUpstreamChanged(network);
                    });
                }

                @Override
                public void onCallbackCreated(Network network, TetheringConfigurationParcel config,
                        TetherStatesParcel states, boolean isRestricted) { }

                @Override
                public void onConfigurationChanged(TetheringConfigurationParcel config) { }

                @Override
                public void onTetherStatesChanged(TetherStatesParcel states) { }

                @Override
                public void onTetheringRestricted(boolean isRestricted) { }
            };
            mConnector.registerTetheringEventCallback(remoteCallback, callerPkg);
            mTetheringEventCallbacks.put(callback, remoteCallback);
        }
    }

    /**
     * Unregister tethering event callback.
     *
     * @hide
     */
    @SystemApi
    public void unregisterTetheringEventCallback(OnTetheringEventCallback callback, String callerPkg)
            throws RemoteException {
        synchronized (mTetheringEventCallbacks) {
            ITetheringEventCallback remoteCallback = mTetheringEventCallbacks.remove(callback);
            if (remoteCallback == null) {
                throw new IllegalArgumentException("callback was not registered.");
            }
            mConnector.unregisterTetheringEventCallback(remoteCallback, callerPkg);
        }
    }

    /**
     * Get a more detailed error code after a Tethering or Untethering
     * request asynchronously failed.
     *
     * @hide
     */
    @SystemApi
    public int getLastTetherError(String iface) {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        if (mTetherStatesParcel == null) return TETHER_ERROR_NO_ERROR;

        int i = 0;
        for (String errored : mTetherStatesParcel.erroredIfaceList) {
            if (iface.equals(errored)) return mTetherStatesParcel.lastErrorList[i];

            i++;
        }
        return TETHER_ERROR_NO_ERROR;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * USB network interfaces.  If USB tethering is not supported by the
     * device, this list should be empty.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetherableUsbRegexs() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        return mTetheringConfiguration.tetherableUsbRegexs;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Wifi network interfaces.  If Wifi tethering is not supported by the
     * device, this list should be empty.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetherableWifiRegexs() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        return mTetheringConfiguration.tetherableWifiRegexs;
    }

    /**
     * Get the list of regular expressions that define any tetherable
     * Bluetooth network interfaces.  If Bluetooth tethering is not supported by the
     * device, this list should be empty.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetherableBluetoothRegexs() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        return mTetheringConfiguration.tetherableBluetoothRegexs;
    }

    /**
     * Get the set of tetherable, available interfaces.  This list is limited by
     * device configuration and current interface existence.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetherableIfaces() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        if (mTetherStatesParcel == null) return new String[0];

        return mTetherStatesParcel.availableList;
    }

    /**
     * Get the set of tethered interfaces.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetheredIfaces() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        if (mTetherStatesParcel == null) return new String[0];

        return mTetherStatesParcel.tetheredList;
    }

    /**
     * Get the set of interface names which attempted to tether but
     * failed.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetheringErroredIfaces() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        if (mTetherStatesParcel == null) return new String[0];

        return mTetherStatesParcel.erroredIfaceList;
    }

    /**
     * Get the set of tethered dhcp ranges.
     *
     * @hide
     */
    @SystemApi
    public String[] getTetheredDhcpRanges() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        return mTetheringConfiguration.legacyDhcpRanges;
    }

    /**
     * Check if the device allows for tethering.
     *
     * @hide
     */
    @SystemApi
    public boolean hasTetherableConfiguration() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }

        final boolean hasDownstreamConfiguration =
                (mTetheringConfiguration.tetherableUsbRegexs.length != 0)
                || (mTetheringConfiguration.tetherableWifiRegexs.length != 0)
                || (mTetheringConfiguration.tetherableBluetoothRegexs.length != 0);
        final boolean hasUpstreamConfiguration =
                (mTetheringConfiguration.preferredUpstreamIfaceTypes.length != 0)
                || mTetheringConfiguration.chooseUpstreamAutomatically;

        return hasDownstreamConfiguration && hasUpstreamConfiguration;
    }


    /**
     * Check if the device allows for tethering.
     *
     * @hide
     */
    @SystemApi
    public boolean isTetheringSupported() {
        if (!mCallback.awaitCallbackCreation()) {
            throw new NullPointerException("callback was not ready yet");
        }
        // Keep the same as TetheringService.java
        int defaultVal =
                SystemProperties.get("ro.tether.denied").equals("true") ? 0 : 1;
        boolean tetherSupported = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.TETHER_SUPPORTED, defaultVal) != 0;

        return tetherSupported && !mTetherRestricted;
    }
}
