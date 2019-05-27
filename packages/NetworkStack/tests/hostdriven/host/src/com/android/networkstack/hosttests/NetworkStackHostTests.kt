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
 * limitations under the License
 */

package com.android.networkstack.hosttests

import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.fail

import org.junit.Assume.assumeFalse

import com.android.tests.util.ModuleTestUtils
import com.android.tradefed.device.ITestDevice
import com.android.tradefed.testtype.DeviceJUnit4ClassRunner
import com.android.tradefed.testtype.junit4.BaseHostJUnit4Test
import com.android.tradefed.util.AaptParser
import com.android.tradefed.util.CommandResult
import com.android.tradefed.util.CommandStatus

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DELAY_MS = 1_000L
private const val APP_APK = "NetworkStack.apk"
private const val NETWORKSTACK_TIMEOUT_MS = 5 * 60000

@RunWith(DeviceJUnit4ClassRunner::class)
class NetworkStackHostTests : BaseHostJUnit4Test() {

    private val mUtils = ModuleTestUtils(this)
    private val mModuleApk = mUtils.getTestFile(APP_APK)
    private val mDevice by lazy { getDevice() }

    private lateinit var mPackageName: String
    private var mDisableRootOnTeardown = false

    @Before
    fun setUp() {
        assertNotNull(mDevice)
        val parserInfo = AaptParser.parse(mModuleApk)
        assertNotNull(parserInfo)
        mPackageName = parserInfo.getPackageName()
        // Get root for framework-only restart. Will fall back to device restart if not available.
        mDisableRootOnTeardown = !mDevice.isAdbRoot() && mDevice.enableAdbRoot()
    }

    @After
    fun tearDown() {
        if (mDisableRootOnTeardown) {
            mDevice.disableAdbRoot()
        }
    }

    @Test
    fun testInstallAndRollback() {
        val initialUpdateTime = getLastUpdateTime()
        waitForNetworkStackRegistered()
        val error = mDevice.installPackage(mModuleApk, false, "--staged", "--enable-rollback")
        // Test can't run on builds not (yet) supporting staged installs. Skip test on such builds.
        assumeFalse(error != null && error.contains("Unknown option --staged"))
        assertNull(error, "Error installing module package: $error")
        try {
            mUtils.waitForStagedSessionReady()
            applyUpdateAndCheckNetworkStackRegistered()
            assertNotEquals(initialUpdateTime, getLastUpdateTime(), "Update time did not change")
        } finally {
            assertCommandSucceeds("pm rollback-app $mPackageName")
            mUtils.waitForStagedSessionReady()
            applyUpdateAndCheckNetworkStackRegistered()
        }
    }

    /**
     * Apply updates that are already staged for install on the device.
     *
     * <p>This can be done by rebooting the whole device, or just rebooting the framework if the
     * device allows root access.
     */
    private fun applyUpdateAndCheckNetworkStackRegistered() {
        if (mDevice.isAdbRoot()) {
            // Restart the framework to apply staged APK update
            val oldPid = getNetworkStackPid()
            oldPid ?: fail("networkstack process not found")
            val cmd = "am restart"
            val amResult = mDevice.executeShellV2Command(cmd)
            // am restart does not return a success code as the connection is lost: check stdout
            assertTrue(amResult.getStdout() != null &&
                    amResult.getStdout().trim().startsWith("Restart the system"),
                    "Could not restart the framework: " + makeErrorMessage(cmd, amResult))
            waitForNetworkStackRestart(oldPid)
        } else {
            // Without root access, need to reboot the whole device
            mDevice.reboot()
        }
        waitForNetworkStackRegistered()
    }

    private fun getLastUpdateTime(): String {
        return waitForDumpsys(mDevice, "package $mPackageName", "lastUpdateTime=")
    }

    private fun waitForNetworkStackRestart(oldPid: String) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() < start + NETWORKSTACK_TIMEOUT_MS) {
            val newPid = getNetworkStackPid()
            if (newPid != null && newPid != oldPid) {
                return
            }

            Thread.sleep(TEST_DELAY_MS)
        }

        fail("Timeout waiting for networkstack to restart")
    }

    /**
     * Returns the NetworkStack PID, or null if it is not running.
     */
    private fun getNetworkStackPid(): String? {
        return mDevice.getProcessPid(mPackageName)
    }

    /**
     * Wait for the NetworkStack to be registered in the system server as a system service. This is
     * the case if dumpsys network_stack succeeds.
     */
    private fun waitForNetworkStackRegistered() {
        waitForDumpsys(mDevice, "network_stack version", "NetworkStackConnector")
    }

    private fun assertCommandSucceeds(command: String): CommandResult {
        val result = mDevice.executeShellV2Command(command)
        assertEquals(CommandStatus.SUCCESS, result.getStatus(), makeErrorMessage(command, result))
        return result
    }
}

/**
 * Wait for a dumpsys command to return successfully with a line containing the specified prefix.
 * @param dumpsys The x in the "dumpsys x" command to run
 * @param expectedPrefix Prefix that one of the lines of the output must have
 */
private fun waitForDumpsys(device: ITestDevice, dumpsys: String, expectedPrefix: String): String {
    val start = System.currentTimeMillis()
    while (System.currentTimeMillis() < start + NETWORKSTACK_TIMEOUT_MS) {
        val cmdResult = device.executeShellV2Command("dumpsys $dumpsys | grep '$expectedPrefix'")
        if (cmdResult.getStatus() == CommandStatus.SUCCESS) {
            val out = cmdResult.getStdout().trim()
            if (out.startsWith(expectedPrefix)) {
                return out
            }
        }

        Thread.sleep(TEST_DELAY_MS)
    }

    fail("Timeout waiting for dumpsys $dumpsys")
}

private fun makeErrorMessage(command: String, result: CommandResult): String {
    return "$command failed; stderr: ${result.getStderr()}; stdout: ${result.getStdout()}; " +
            "status code: ${result.getExitCode()}"
}
