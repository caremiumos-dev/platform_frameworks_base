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

    private static final int QNS_CREATE_QUALIFIED_NETWORKS_PROVIDER                 = 1;
    private static final int QNS_REMOVE_QUALIFIED_NETWORKS_PROVIDER                 = 2;
    private static final int QNS_REMOVE_ALL_QUALIFIED_NETWORKS_PROVIDERS            = 3;
    private static final int QNS_REQUEST_REGISTER_QUALIFIED_NETWORKS_CHANGED        = 4;
    private static final int QNS_REQUEST_UNREGISTER_QUALIFIED_NETWORKS_CHANGED      = 5;
    private static final int QNS_UPDATE_QUALIFIED_NETWORKS                          = 6;
    private static final int QNS_INDICATION_QUALIFIED_NETWORKS_CHANGED              = 7;

    private final HandlerThread mHandlerThread;

    private final QualifiedNetworksServiceHandler mHandler;

    private final SparseArray<QualifiedNetworksProvider> mProviderMap = new SparseArray<>();

    /** @hide */
    @VisibleForTesting
    public final IQualifiedNetworksServiceWrapper mBinder = new IQualifiedNetworksServiceWrapper();

    /**
     * The abstract class of the actual qualified networks service implementation. The qualified
     * networks provider must extend this class to report the available networks for data
     * connection setup. Note that each instance of qualified networks provider is associated with
     * one physical SIM slot.
     */
    public class QualifiedNetworksProvider {

        private final int mSlotId;

        private final List<IQualifiedNetworksServiceCallback> mQualifiedNetworksChangedCallbacks =
                new ArrayList<>();

        private List<QualifiedNetworks> mQualifiedNetworksList = new ArrayList<>();

        /**
         * Constructor
         * @param slotId SIM slot id the qualified networks provider associated with.
         */
        public QualifiedNetworksProvider(int slotId) {
            mSlotId = slotId;
        }

        /**
         * @return SIM slot id the qualified networks provider associated with.
         */
        public final int getSlotId() {
            return mSlotId;
        }

        private void registerForQualifiedNetworksChanged(
                IQualifiedNetworksServiceCallback callback) {
            synchronized (mQualifiedNetworksChangedCallbacks) {
                mQualifiedNetworksChangedCallbacks.add(callback);
                mHandler.obtainMessage(QNS_INDICATION_QUALIFIED_NETWORKS_CHANGED, mSlotId, 0,
                        new QualifiedNetworksChangedIndication(mQualifiedNetworksList, callback))
                        .sendToTarget();
            }
        }

        private void unregisterForQualifiedNetworksChanged(
                IQualifiedNetworksServiceCallback callback) {
            synchronized (mQualifiedNetworksChangedCallbacks) {
                mQualifiedNetworksChangedCallbacks.remove(callback);
            }
        }

        /**
         * Update the qualified networks list. Qualified networks provider must invoke this method
         * whenever the qualified networks changes.
         *
         * @param qualifiedNetworksList List of all the qualified networks
         */
        public final void updateQualifiedNetworks(List<QualifiedNetworks> qualifiedNetworksList) {
            ArrayList<QualifiedNetworks> networksList = new ArrayList<>(qualifiedNetworksList);
            mHandler.obtainMessage(QNS_UPDATE_QUALIFIED_NETWORKS, mSlotId, 0, networksList)
                    .sendToTarget();
        }

        private void onUpdateQualifiedNetworks(List<QualifiedNetworks> qualifiedNetworksList) {
            if (qualifiedNetworksList != null
                    && !qualifiedNetworksList.equals(mQualifiedNetworksList)) {
                synchronized (mQualifiedNetworksChangedCallbacks) {
                    mQualifiedNetworksList = qualifiedNetworksList;
                    for (IQualifiedNetworksServiceCallback callback
                            : mQualifiedNetworksChangedCallbacks) {
                        try {
                            callback.onQualifiedNetworksChanged(mQualifiedNetworksList);
                        } catch (RemoteException e) {
                            loge("Failed to call onQualifiedNetworksChanged. " + e);
                        }
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
        public final List<QualifiedNetworks> qualifiedNetworksList;
        public final IQualifiedNetworksServiceCallback callback;
        QualifiedNetworksChangedIndication(List<QualifiedNetworks> qualifiedNetworksList,
                                      IQualifiedNetworksServiceCallback callback) {
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
            QualifiedNetworksProvider serviceProvider = mProviderMap.get(slotId);

            switch (message.what) {
                case QNS_CREATE_QUALIFIED_NETWORKS_PROVIDER:
                    serviceProvider = createQualifiedNetworksProvider(message.arg1);
                    if (serviceProvider != null) {
                        mProviderMap.put(slotId, serviceProvider);
                    }
                    break;
                case QNS_REMOVE_QUALIFIED_NETWORKS_PROVIDER:
                    if (serviceProvider != null) {
                        serviceProvider.onDestroy();
                        mProviderMap.remove(slotId);
                    }
                    break;
                case QNS_REMOVE_ALL_QUALIFIED_NETWORKS_PROVIDERS:
                    for (int i = 0; i < mProviderMap.size(); i++) {
                        serviceProvider = mProviderMap.get(i);
                        if (serviceProvider != null) {
                            serviceProvider.onDestroy();
                        }
                    }
                    mProviderMap.clear();
                    break;
                case QNS_REQUEST_REGISTER_QUALIFIED_NETWORKS_CHANGED:
                    if (serviceProvider == null) break;
                    serviceProvider.registerForQualifiedNetworksChanged(
                            (IQualifiedNetworksServiceCallback) message.obj);
                    break;
                case QNS_REQUEST_UNREGISTER_QUALIFIED_NETWORKS_CHANGED:
                    if (serviceProvider == null) break;
                    callback = (IQualifiedNetworksServiceCallback) message.obj;
                    serviceProvider.unregisterForQualifiedNetworksChanged(callback);
                    break;
                case QNS_UPDATE_QUALIFIED_NETWORKS:
                    if (serviceProvider == null) break;
                    serviceProvider.onUpdateQualifiedNetworks(
                            (List<QualifiedNetworks>) message.obj);
                    break;
                case QNS_INDICATION_QUALIFIED_NETWORKS_CHANGED:
                    if (serviceProvider == null) break;
                    QualifiedNetworksChangedIndication indication =
                            (QualifiedNetworksChangedIndication) message.obj;
                    try {
                        indication.callback.onQualifiedNetworksChanged(
                                indication.qualifiedNetworksList);
                    } catch (RemoteException e) {
                        loge("Failed to call onQualifiedNetworksChanged. " + e);
                    }
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
     * Create the instance of {@link QualifiedNetworksProvider}. Qualified networks provider must
     * override this method to facilitate the creation of {@link QualifiedNetworksProvider}
     * instances. The system will call this method after binding the qualified networks service for
     * each active SIM slot id.
     *
     * @param slotId SIM slot id the qualified networks service associated with.
     * @return Qualified networks service instance
     */
    public abstract QualifiedNetworksProvider createQualifiedNetworksProvider(int slotId);

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
        mHandler.obtainMessage(QNS_REMOVE_ALL_QUALIFIED_NETWORKS_PROVIDERS).sendToTarget();
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
        public void createQualifiedNetworksProvider(int slotId) {
            mHandler.obtainMessage(QNS_CREATE_QUALIFIED_NETWORKS_PROVIDER, slotId, 0)
                    .sendToTarget();
        }

        @Override
        public void removeQualifiedNetworksProvider(int slotId) {
            mHandler.obtainMessage(QNS_REMOVE_QUALIFIED_NETWORKS_PROVIDER, slotId, 0)
                    .sendToTarget();
        }

        @Override
        public void registerForQualifiedNetworksChanged(
                int slotId, IQualifiedNetworksServiceCallback callback) {
            if (callback == null) {
                loge("registerForQualifiedNetworksChanged: callback is null");
                return;
            }
            mHandler.obtainMessage(QNS_REQUEST_REGISTER_QUALIFIED_NETWORKS_CHANGED, slotId,
                    0, callback).sendToTarget();
        }

        @Override
        public void unregisterForQualifiedNetworksChanged(
                int slotId, IQualifiedNetworksServiceCallback callback) {
            if (callback == null) {
                loge("unregisterForQualifiedNetworksChanged: callback is null");
                return;
            }
            mHandler.obtainMessage(QNS_REQUEST_UNREGISTER_QUALIFIED_NETWORKS_CHANGED, slotId,
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
