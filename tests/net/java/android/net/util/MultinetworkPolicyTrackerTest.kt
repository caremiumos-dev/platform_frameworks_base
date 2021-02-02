/*
 * Copyright (C) 2021 The Android Open Source Project
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

package android.net.util

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager.MULTIPATH_PREFERENCE_HANDOVER
import android.net.ConnectivityManager.MULTIPATH_PREFERENCE_PERFORMANCE
import android.net.ConnectivityManager.MULTIPATH_PREFERENCE_RELIABILITY
import android.net.util.MultinetworkPolicyTracker.ActiveDataSubscriptionIdChangedListener
import android.provider.Settings
import android.provider.Settings.Global.NETWORK_AVOID_BAD_WIFI
import android.provider.Settings.Global.NETWORK_METERED_MULTIPATH_PREFERENCE
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.test.mock.MockContentResolver
import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4
import com.android.internal.R
import com.android.internal.util.test.FakeSettingsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.argThat
import org.mockito.Mockito.any
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

/**
 * Tests for [MultinetworkPolicyTracker].
 *
 * Build, install and run with:
 * atest android.net.util.MultinetworkPolicyTrackerTest
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class MultinetworkPolicyTrackerTest {
    private val context = mock(Context::class.java)
    private val resources = mock(Resources::class.java)
    private val telephonyManager = mock(TelephonyManager::class.java)
    private val subscriptionManager = mock(SubscriptionManager::class.java)
    private val resolver = MockContentResolver().apply {
        addProvider(Settings.AUTHORITY, FakeSettingsProvider()) }

    // lateinit for this class under test, as they should be reset to a different instance for every
    // test but should always be initialized before use (or the test should crash).
    private lateinit var tracker: MultinetworkPolicyTracker

    @Before
    fun setUp() {
        doReturn(Context.TELEPHONY_SERVICE).`when`(context)
                .getSystemServiceName(TelephonyManager::class.java)
        doReturn(telephonyManager).`when`(context).getSystemService(Context.TELEPHONY_SERVICE)

        // Mock some stuff on SubscriptionManager for calling to getResourcesForSubId()
        doReturn(subscriptionManager).`when`(context)
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
        doReturn(null).`when`(subscriptionManager).getActiveSubscriptionInfo(anyInt())
        doReturn(resolver).`when`(context).contentResolver
        doReturn(resources).`when`(context).resources
        doReturn(0).`when`(resources).getInteger(R.integer.config_networkAvoidBadWifi)
        doReturn(context).`when`(context).createConfigurationContext(any())

        Settings.Global.putString(resolver, NETWORK_AVOID_BAD_WIFI, "1")
        tracker = MultinetworkPolicyTracker(context, null /* handler */)
        assertTrue(tracker.avoidBadWifi)
    }

    private fun assertMultipathPreference(preference: Int) {
        Settings.Global.putString(resolver, NETWORK_METERED_MULTIPATH_PREFERENCE,
                preference.toString())
        tracker.updateMeteredMultipathPreference()
        assertEquals(preference, tracker.meteredMultipathPreference)
    }

    @Test
    fun testUpdateMeteredMultipathPreference() {
        assertMultipathPreference(MULTIPATH_PREFERENCE_HANDOVER)
        assertMultipathPreference(MULTIPATH_PREFERENCE_RELIABILITY)
        assertMultipathPreference(MULTIPATH_PREFERENCE_PERFORMANCE)
    }

    @Test
    fun testUpdateAvoidBadWifi() {
        Settings.Global.putString(resolver, NETWORK_AVOID_BAD_WIFI, "0")
        assertTrue(tracker.updateAvoidBadWifi())
        assertFalse(tracker.avoidBadWifi)

        doReturn(1).`when`(resources).getInteger(R.integer.config_networkAvoidBadWifi)
        assertTrue(tracker.updateAvoidBadWifi())
        assertTrue(tracker.avoidBadWifi)
    }

    @Test
    fun testOnActiveDataSubscriptionIdChanged() {
        val testSubId = 1000
        val subscriptionInfo = SubscriptionInfo(testSubId, ""/* iccId */, 1/* iccId */,
                "TMO"/* displayName */, "TMO"/* carrierName */, 1/* nameSource */, 1/* iconTint */,
                "123"/* number */, 1/* roaming */, null/* icon */, "310"/* mcc */, "210"/* mnc */,
                ""/* countryIso */, false/* isEmbedded */, null/* nativeAccessRules */,
                "1"/* cardString */)
        doReturn(subscriptionInfo).`when`(subscriptionManager).getActiveSubscriptionInfo(testSubId)

        // Modify avoidBadWifi and meteredMultipathPreference settings value and local variables in
        // MultinetworkPolicyTracker should be also updated after subId changed.
        Settings.Global.putString(resolver, NETWORK_AVOID_BAD_WIFI, "0")
        Settings.Global.putString(resolver, NETWORK_METERED_MULTIPATH_PREFERENCE,
                MULTIPATH_PREFERENCE_PERFORMANCE.toString())

        val listenerCaptor = ArgumentCaptor.forClass(
                ActiveDataSubscriptionIdChangedListener::class.java)
        verify(telephonyManager, times(1))
                .registerPhoneStateListener(any(), listenerCaptor.capture())
        val listener = listenerCaptor.value
        listener.onActiveDataSubscriptionIdChanged(testSubId)

        // Check it get resource value with test sub id.
        verify(subscriptionManager, times(1)).getActiveSubscriptionInfo(testSubId)
        verify(context).createConfigurationContext(argThat { it.mcc == 310 && it.mnc == 210 })

        // Check if avoidBadWifi and meteredMultipathPreference values have been updated.
        assertFalse(tracker.avoidBadWifi)
        assertEquals(MULTIPATH_PREFERENCE_PERFORMANCE, tracker.meteredMultipathPreference)
    }
}
