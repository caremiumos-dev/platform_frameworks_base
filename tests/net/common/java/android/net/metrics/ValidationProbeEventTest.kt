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

package android.net.metrics

import androidx.test.filters.SmallTest
import androidx.test.runner.AndroidJUnit4

import com.android.internal.util.ParcelableTestUtil
import com.android.internal.util.TestUtils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

import java.lang.reflect.Modifier

private const val FIRST_VALIDATION: Int = 1 shl 8
private const val REVALIDATION: Int = 2 shl 8

@RunWith(AndroidJUnit4::class)
@SmallTest
class ValidationProbeEventTest {
    fun hasType(type: Int, target: Int) = (type and target) == type

    @Test
    fun testBuilder() {
        var validationProbeEvent = ValidationProbeEvent.Builder()
                .setDurationMs(Long.MAX_VALUE)
                .setProbeType(ValidationProbeEvent.PROBE_DNS, true)
                .setReturnCode(ValidationProbeEvent.DNS_SUCCESS)
                .build()

        assertEquals(Long.MAX_VALUE, validationProbeEvent.durationMs)
        assertTrue(hasType(ValidationProbeEvent.PROBE_DNS, validationProbeEvent.probeType))
        assertTrue(hasType(FIRST_VALIDATION, validationProbeEvent.probeType))
        assertEquals(ValidationProbeEvent.DNS_SUCCESS, validationProbeEvent.returnCode)

        validationProbeEvent = ValidationProbeEvent.Builder()
                .setProbeType(ValidationProbeEvent.PROBE_DNS, false).build()

        assertTrue(hasType(REVALIDATION, validationProbeEvent.probeType))
    }

    @Test
    fun testParceling() {
        val validationProbeEvent = ValidationProbeEvent.Builder()
                .setDurationMs(Long.MAX_VALUE)
                .setProbeType(ValidationProbeEvent.PROBE_DNS, true)
                .setReturnCode(ValidationProbeEvent.DNS_SUCCESS)
                .build()

        ParcelableTestUtil.assertFieldCountEquals(3, ValidationProbeEvent::class.java)
        TestUtils.assertParcelingIsLossless(validationProbeEvent)
    }

    @Test
    fun testGetProbeName() {
        val probeFields = ValidationProbeEvent::class.java.declaredFields.filter {
            it.type == Int::class.javaPrimitiveType
              && Modifier.isPublic(it.modifiers) && Modifier.isStatic(it.modifiers)
              && it.name.contains("PROBE")
        }

        probeFields.forEach {
            val intValue = it.getInt(null) as Int
            val stringValue =  ValidationProbeEvent.getProbeName(intValue)
            assertEquals(it.name, stringValue)
        }

    }
}
