/*
 * Copyright 2018 The Android Open Source Project
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

package android.telephony.data;

import android.annotation.CallSuper;
import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.Rlog;
import android.telephony.data.ApnSetting.ApnType;
import android.util.SparseArray;

import com.android.internal.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class of the qualified networks service. Services that extend QualifiedNetworksService must
 * register the service in their AndroidManifest to be detected by the framework. They must be
 * protected by the permission "android.permission.BIND_TELEPHONY_QUALIFIED_NETWORKS_SERVICE".
 * The qualified networks service definition in the manifest must follow the following format:
 * ...
 * <service android:name=".xxxQualifiedNetworksService"
 *     android:permission="android.permission.BIND_TELEPHONY_QUALIFIED_NETWORKS_SERVICE" >
 *     <intent-filter>
 *         <action android:name="android.telephony.data.QualifiedNetworksService" />
 *     </intent-filter>
 * </service>
 * @hide
 */
@SystemApi
public abstract class QualifiedNetworksService extends Service {
    private static final String TAG = QualifiedNetworksService.class.getSimpleName();

    public static final String QUALIFIED_NETWORKS_SERVICE_INTERFACE =
            "android.telephony.data.QualifiedNetworksService";

    private static final int QNS_CREATE_NETWORK_AVAILABILITY_UPDATER                = 1;
    private static final int QNS_REMOVE_NETWORK_AVAILABILITY_UPDATER                = 2;
    private static final int QNS_REMOVE_ALL_NETWORK_AVAILABILITY_UPDATERS           = 3;
    private static final int QNS_REGISTER_QUALIFIED_NETWORKS_CHANGED                = 4;
    private static final int QNS_UNREGISTER_QUALIFIED_NETWORKS_CHANGED              = 5;
    private static final int QNS_UPDATE_QUALIFIED_NETWORKS                          = 6;

    private final HandlerThread mHandlerThread;

    private final QualifiedNetworksServiceHandler mHandler;

    private final SparseArray<NetworkAvailabilityUpdater> mUpdaters = new SparseArray<>();

    /** @hide */
    @VisibleForTesting
    public final IQualifiedNetworksServiceWrapper mBinder = new IQualifiedNetworksServiceWrapper();

    /**
     * The abstract class of the network availability updater implementation. The vendor qualified
     * network service must extend this class to report the available networks for data
     * connection setup. Note that each instance of network availability updater is associated with
     * one physical SIM slot.
     */
    public class NetworkAvailabilityUpdater {

        private final int mSlotId;

        private final List<IQualifiedNetworksServiceCallback> mQualifiedNetworksChangedCallbacks =
                new ArrayList<>();

        private SparseArray<int[]> mQualifiedNetworksList = new SparseArray<>();

        /**
         * Constructor
         * @param slotId SIM slot id the network availability updater associated with.
         */
        public NetworkAvailabilityUpdater(int slotId) {
            mSlotId = slotId;
        }

        /**
         * @return SIM slot id the network availability updater associated with.
         */
        public final int getSlotId() {
            return mSlotId;
        }

        private void registerForQualifiedNetworksChanged(
                IQualifiedNetworksServiceCallback callback) {
            synchronized (mQualifiedNetworksChangedCallbacks) {
                mQualifiedNetworksChangedCallbacks.add(callback);

                // Force sending the qualified networks upon registered.
                for (int i = 0; i < mQualifiedNetworksList.size(); i++) {
                    try {
                        callback.onQualifiedNetworksChanged(mQualifiedNetworksList.keyAt(i),
                                mQualifiedNetworksList.valueAt(i));
                    } catch (RemoteException e) {
                        loge("Failed to call onQualifiedNetworksChanged. " + e);
                    }
                }
            }
        }

        private void unregisterForQualifiedNetworksChanged(
                IQualifiedNetworksServiceCallback callback) {
            synchronized (mQualifiedNetworksChangedCallbacks) {
                mQualifiedNetworksChangedCallbacks.remove(callback);
            }
        }

        /**
         * Update the qualified networks list. Network availability updater must invoke this method
         * whenever the qualified networks changes.
         *
         * @param apnType APN type of the qualified networks
         * @param qualifiedNetworks List of the available networks with the given APN type
         */
        public final void updateQualifiedNetworks(@ApnType int apnType, int[] qualifiedNetworks) {
            mHandler.obtainMessage(QNS_UPDATE_QUALIFIED_NETWORKS, mSlotId, apnType,
                    qualifiedNetworks).sendToTarget();
        }

        private void onUpdateQualifiedNetworks(@ApnType int apnType, int[] qualifiedNetworks) {
            synchronized (mQualifiedNetworksChangedCallbacks) {
                mQualifiedNetworksList.put(apnType, qualifiedNetworks);
                for (IQualifiedNetworksServiceCallback callback
                        : mQualifiedNetworksChangedCallbacks) {
                    try {
                        callback.onQualifiedNetworksChanged(apnType, qualifiedNetworks);
                    } catch (RemoteException e) {
                        loge("Failed to call onQualifiedNetworksChanged. " + e);
                    }
                }
            }
        }

        /**
         * Called when the instance of qualified networks service is destroyed (e.g. got unbind or
         * binder died).
         */
        @CallSuper
        protected void onDestroy() {
            mQualifiedNetworksChangedCallbacks.clear();
        }
    }

    private static final class QualifiedNetworksChangedIndication {
        @ApnType
        public final int apnType;

        public final int[] qualifiedNetworksList;

        public final IQualifiedNetworksServiceCallback callback;

        QualifiedNetworksChangedIndication(@ApnType int apnType, int[] qualifiedNetworksList,
                                      IQualifiedNetworksServiceCallback callback) {
            this.apnType = apnType;
            this.qualifiedNetworksList = qualifiedNetworksList;
            this.callback = callback;
        }
    }

    private class QualifiedNetworksServiceHandler extends Handler {

        QualifiedNetworksServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            IQualifiedNetworksServiceCallback callback;
            final int slotId = message.arg1;
            NetworkAvailabilityUpdater updater = mUpdaters.get(slotId);

            switch (message.what) {
                case QNS_CREATE_NETWORK_AVAILABILITY_UPDATER:
                    updater = createNetworkAvailabilityUpdater(message.arg1);
                    if (updater != null) {
                        mUpdaters.put(slotId, updater);
                    }
                    break;
                case QNS_REMOVE_NETWORK_AVAILABILITY_UPDATER:
                    if (updater != null) {
                        updater.onDestroy();
                        mUpdaters.remove(slotId);
                    }
                    break;
                case QNS_REMOVE_ALL_NETWORK_AVAILABILITY_UPDATERS:
                    for (int i = 0; i < mUpdaters.size(); i++) {
                        updater = mUpdaters.get(i);
                        if (updater != null) {
                            updater.onDestroy();
                        }
                    }
                    mUpdaters.clear();
                    break;
                case QNS_REGISTER_QUALIFIED_NETWORKS_CHANGED:
                    if (updater == null) break;
                    updater.registerForQualifiedNetworksChanged(
                            (IQualifiedNetworksServiceCallback) message.obj);
                    break;
                case QNS_UNREGISTER_QUALIFIED_NETWORKS_CHANGED:
                    if (updater == null) break;
                    callback = (IQualifiedNetworksServiceCallback) message.obj;
                    updater.unregisterForQualifiedNetworksChanged(callback);
                    break;
                case QNS_UPDATE_QUALIFIED_NETWORKS:
                    if (updater == null) break;
                    updater.onUpdateQualifiedNetworks(message.arg2, (int[]) message.obj);
                    break;
            }
        }
    }

    /**
     * Default constructor.
     */
    public QualifiedNetworksService() {
        mHandlerThread = new HandlerThread(TAG);
        mHandlerThread.start();

        mHandler = new QualifiedNetworksServiceHandler(mHandlerThread.getLooper());
        log("Qualified networks service created");
    }

    /**
     * Create the instance of {@link NetworkAvailabilityUpdater}. Vendor qualified network service
     * must override this method to facilitate the creation of {@link NetworkAvailabilityUpdater}
     * instances. The system will call this method after binding the qualified networks service for
     * each active SIM slot id.
     *
     * @param slotId SIM slot id the qualified networks service associated with.
     * @return Qualified networks service instance
     */
    public abstract NetworkAvailabilityUpdater createNetworkAvailabilityUpdater(int slotId);

    /** @hide */
    @Override
    public IBinder onBind(Intent intent) {
        if (intent == null || !QUALIFIED_NETWORKS_SERVICE_INTERFACE.equals(intent.getAction())) {
            loge("Unexpected intent " + intent);
            return null;
        }
        return mBinder;
    }

    /** @hide */
    @Override
    public boolean onUnbind(Intent intent) {
        mHandler.obtainMessage(QNS_REMOVE_ALL_NETWORK_AVAILABILITY_UPDATERS).sendToTarget();
        return false;
    }

    /** @hide */
    @Override
    public void onDestroy() {
        mHandlerThread.quit();
    }

    /**
     * A wrapper around IQualifiedNetworksService that forwards calls to implementations of
     * {@link QualifiedNetworksService}.
     */
    private class IQualifiedNetworksServiceWrapper extends IQualifiedNetworksService.Stub {
        @Override
        public void createNetworkAvailabilityUpdater(int slotId) {
            mHandler.obtainMessage(QNS_CREATE_NETWORK_AVAILABILITY_UPDATER, slotId, 0)
                    .sendToTarget();
        }

        @Override
        public void removeNetworkAvailabilityUpdater(int slotId) {
            mHandler.obtainMessage(QNS_REMOVE_NETWORK_AVAILABILITY_UPDATER, slotId, 0)
                    .sendToTarget();
        }

        @Override
        public void registerForQualifiedNetworksChanged(
                int slotId, IQualifiedNetworksServiceCallback callback) {
            if (callback == null) {
                loge("registerForQualifiedNetworksChanged: callback is null");
                return;
            }
            mHandler.obtainMessage(QNS_REGISTER_QUALIFIED_NETWORKS_CHANGED, slotId,
                    0, callback).sendToTarget();
        }

        @Override
        public void unregisterForQualifiedNetworksChanged(
                int slotId, IQualifiedNetworksServiceCallback callback) {
            if (callback == null) {
                loge("unregisterForQualifiedNetworksChanged: callback is null");
                return;
            }
            mHandler.obtainMessage(QNS_UNREGISTER_QUALIFIED_NETWORKS_CHANGED, slotId,
                    0, callback).sendToTarget();
        }
    }

    private void log(String s) {
        Rlog.d(TAG, s);
    }

    private void loge(String s) {
        Rlog.e(TAG, s);
    }
}
