/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.server.connectivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.INetworkPolicyManager;
import android.net.INetworkStatsService;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.INetworkManagementService;
import android.os.PersistableBundle;
import android.os.ResultReceiver;
import android.os.UserHandle;
import android.os.test.TestLooper;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.telephony.CarrierConfigManager;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;
import android.util.MutableBoolean;
import android.util.MutableInt;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class TetheringTest {
    private static final String[] PROVISIONING_APP_NAME = {"some", "app"};

    @Mock private Context mContext;
    @Mock private INetworkManagementService mNMService;
    @Mock private INetworkStatsService mStatsService;
    @Mock private INetworkPolicyManager mPolicyManager;
    @Mock private MockableSystemProperties mSystemProperties;
    @Mock private Resources mResources;
    @Mock private CarrierConfigManager mCarrierConfigManager;
    @Mock private TelephonyManager mMockTelephonyManager;
    @Mock private ConnectivityManager mMockConnectivityManager;

    // Like so many Android system APIs, these cannot be mocked because it is marked final.
    // We have to use the real versions.
    private final PersistableBundle mCarrierConfig = new PersistableBundle();
    private final TestLooper mLooper = new TestLooper();

    private final MutableInt mTetherServiceResult =
            new MutableInt(ConnectivityManager.TETHER_ERROR_NO_ERROR);
    private Tethering mTethering;

    @Before public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getStringArray(com.android.internal.R.array.config_tether_dhcp_range))
                .thenReturn(new String[0]);
        when(mResources.getStringArray(com.android.internal.R.array.config_tether_usb_regexs))
                .thenReturn(new String[0]);
        when(mResources.getStringArray(com.android.internal.R.array.config_tether_wifi_regexs))
                .thenReturn(new String[0]);
        when(mResources.getStringArray(com.android.internal.R.array.config_tether_bluetooth_regexs))
                .thenReturn(new String[0]);
        when(mResources.getIntArray(com.android.internal.R.array.config_tether_upstream_types))
                .thenReturn(new int[0]);
        when(mContext.getSystemService(Context.TELEPHONY_SERVICE))
                .thenReturn(mMockTelephonyManager);
        when(mContext.getSystemService(Context.CONNECTIVITY_SERVICE))
                .thenReturn(mMockConnectivityManager);
        setTetherServiceResult(ConnectivityManager.TETHER_ERROR_NO_ERROR);
        setupFakeTetherServiceResponses();
        mTethering = new Tethering(mContext, mNMService, mStatsService, mPolicyManager,
                                   mLooper.getLooper(), mSystemProperties);
    }

    private void setupFakeTetherServiceResponses() {
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                // TODO: Only verify services which match the right component name. This component
                // name is statically loaded from system resources in Tethering.
                Intent intent = (Intent) invocation.getArguments()[0];
                if (intent.getBooleanExtra(ConnectivityManager.EXTRA_RUN_PROVISION, false)) {
                    ResultReceiver receiver = (ResultReceiver) intent.getParcelableExtra(
                            ConnectivityManager.EXTRA_PROVISION_CALLBACK);
                    receiver.send(mTetherServiceResult.value, null);
                }

                return null;
            }
        }).when(mContext).startServiceAsUser(any(Intent.class), any(UserHandle.class));
    }

    private void setupForRequiredProvisioning() {
        // Produce some acceptable looking provision app setting if requested.
        when(mResources.getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app))
                .thenReturn(PROVISIONING_APP_NAME);
        // Don't disable tethering provisioning unless requested.
        when(mSystemProperties.getBoolean(eq(Tethering.DISABLE_PROVISIONING_SYSPROP_KEY),
                                          anyBoolean())).thenReturn(false);
        // Act like the CarrierConfigManager is present and ready unless told otherwise.
        when(mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE))
                .thenReturn(mCarrierConfigManager);
        when(mCarrierConfigManager.getConfig()).thenReturn(mCarrierConfig);
        mCarrierConfig.putBoolean(CarrierConfigManager.KEY_REQUIRE_ENTITLEMENT_CHECKS_BOOL, true);
    }

    private void assertCarrierAllowsTethering(int expectedResult) {
        final MutableBoolean hasHitCallback = new MutableBoolean(false);
        ResultReceiver receiver = new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                assertEquals(expectedResult, resultCode);
                hasHitCallback.value = true;
            }
        };

        mTethering.doesCarrierAllowTethering(ConnectivityManager.TETHERING_WIFI, receiver);
        assertTrue(hasHitCallback.value);
    }

    private void simulateAvailableCellNetwork() {
        CellInfoGsm gsmCellInfo = new CellInfoGsm();
        gsmCellInfo.setCellSignalStrength(new CellSignalStrengthGsm());
        gsmCellInfo.setRegistered(true);
        List<CellInfo> networkList = new ArrayList<>();
        networkList.add(gsmCellInfo);
        when(mMockTelephonyManager.getAllCellInfo()).thenReturn(networkList);
    }

    /**
     * Sets the next result sent by TetherService when started.
     * @param tetherServiceResult The result of the provisioining check.
     *         (ConnectivityManager.TETHER_ERROR_NO_ERROR, etc).
     */
    private void setTetherServiceResult(int tetherServiceResult) {
        mTetherServiceResult.value = tetherServiceResult;
    }

    @Test
    public void canRequireProvisioning() {
        setupForRequiredProvisioning();
        assertTrue(mTethering.isTetherProvisioningRequired());
    }

    @Test
    public void toleratesCarrierConfigManagerMissing() {
        setupForRequiredProvisioning();
        when(mContext.getSystemService(Context.CARRIER_CONFIG_SERVICE))
                .thenReturn(null);
        // Couldn't get the CarrierConfigManager, but still had a declared provisioning app.
        // We therefore still require provisioning.
        assertTrue(mTethering.isTetherProvisioningRequired());
    }

    @Test
    public void toleratesCarrierConfigMissing() {
        setupForRequiredProvisioning();
        when(mCarrierConfigManager.getConfig()).thenReturn(null);
        // We still have a provisioning app configured, so still require provisioning.
        assertTrue(mTethering.isTetherProvisioningRequired());
    }

    @Test
    public void provisioningNotRequiredWhenAppNotFound() {
        setupForRequiredProvisioning();
        when(mResources.getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app))
                .thenReturn(null);
        assertTrue(!mTethering.isTetherProvisioningRequired());
        when(mResources.getStringArray(
                com.android.internal.R.array.config_mobile_hotspot_provision_app))
                .thenReturn(new String[] {"malformedApp"});
        assertTrue(!mTethering.isTetherProvisioningRequired());
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningNotRequired() {
        when(mSystemProperties.getBoolean(eq(Tethering.DISABLE_PROVISIONING_SYSPROP_KEY),
                                          anyBoolean())).thenReturn(true);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_SUCCESS);
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningNoCellNetwork() {
        setupForRequiredProvisioning();
        when(mMockTelephonyManager.getAllCellInfo()).thenReturn(null);
        when(mMockTelephonyManager.getDataEnabled()).thenReturn(true);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_UNKNOWN);
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningCellDataDisabled() {
        setupForRequiredProvisioning();
        simulateAvailableCellNetwork();
        when(mMockTelephonyManager.getDataEnabled()).thenReturn(false);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_UNKNOWN);
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningDataNotConnected() {
        setupForRequiredProvisioning();
        simulateAvailableCellNetwork();
        when(mMockConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI))
                .thenReturn(null);
        when(mMockTelephonyManager.getDataEnabled()).thenReturn(true);
        when(mMockTelephonyManager.getDataState()).thenReturn(TelephonyManager.DATA_DISCONNECTED);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_UNKNOWN);
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningWifiConnectedAndDataNotConnected() {
        // When wifi is connected, dataState is always DATA_DISCONNECTED.
        setupForRequiredProvisioning();
        simulateAvailableCellNetwork();

        NetworkInfo wifiNetworkInfo = new NetworkInfo(ConnectivityManager.TYPE_WIFI, 0, "wifi", "");
        wifiNetworkInfo.setDetailedState(NetworkInfo.DetailedState.CONNECTED, "", "");
        when(mMockConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI))
                .thenReturn(wifiNetworkInfo);

        when(mMockTelephonyManager.getDataEnabled()).thenReturn(true);
        when(mMockTelephonyManager.getDataState()).thenReturn(TelephonyManager.DATA_DISCONNECTED);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_SUCCESS);
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningSuccessOnCellDatat() {
        setupForRequiredProvisioning();
        simulateAvailableCellNetwork();
        when(mMockConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI))
                .thenReturn(null);
        when(mMockTelephonyManager.getDataEnabled()).thenReturn(true);
        when(mMockTelephonyManager.getDataState()).thenReturn(TelephonyManager.DATA_CONNECTED);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_SUCCESS);
    }

    @Test
    public void doesCarrierAllowTetheringProvisioningProvisioningFailed() {
        setupForRequiredProvisioning();
        simulateAvailableCellNetwork();
        when(mMockConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI))
                .thenReturn(null);
        when(mMockTelephonyManager.getDataEnabled()).thenReturn(true);
        when(mMockTelephonyManager.getDataState()).thenReturn(TelephonyManager.DATA_CONNECTED);
        setTetherServiceResult(ConnectivityManager.TETHER_ERROR_PROVISION_FAILED);

        assertCarrierAllowsTethering(ConnectivityManager.TETHER_PROVISIONING_FAIL);
    }

}
