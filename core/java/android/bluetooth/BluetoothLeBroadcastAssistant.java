/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.annotation.NonNull;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides the public APIs for the LE Audio Broadcast Assistant profile.
 *
 * <p>BluetoothLeBroadcastAssistant is a proxy onject for controlling the Broadcast Assistant
 * service via IPC. Use {@link BluetoothAdapter#getProfileProxy} to get the
 * BluetoothLeBroadcastAssistant proxy object.
 *
 * @hide
 */
public final class BluetoothLeBroadcastAssistant implements BluetoothProfile {
    private static final String TAG = "BluetoothLeBroadcastAssistant";
    private static final boolean DBG = true;

    /**
     * Create a new instance of an LE Audio Broadcast Assistant.
     *
     * @hide
     */
    /*package*/ BluetoothLeBroadcastAssistant(
            @NonNull Context context, @NonNull ServiceListener listener) {}

    /**
     * Connect to the Broadcast Audio Scan Service (BASS) server on the Scan Delegator.
     *
     * @param device BluetoothDevice representing the Scan Delegator
     * @hide
     */
    public int connect(@NonNull BluetoothDevice device) {
        log(TAG, "connect: ");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_CONNECT_FAILED;
    }

    /**
     * Disconnect from the Broadcast Audio Scan Service (BASS) server on the Scan Delegator.
     *
     * @param device BluetoothDevice representing the Scan Delegator
     * @hide
     */
    public int disconnect(@NonNull BluetoothDevice device) {
        log(TAG, "disconnect: ");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_DISCONNECT_FAILED;
    }

    /**
     * Register callbacks that will be invoked during scan offloading.
     *
     * @param sink BluetoothDevice representing the Scan Delegator
     * @param callback callbacks to be invoked
     * @hide
     */
    public int registerCallback(
            @NonNull BluetoothDevice sink, BluetoothLeBroadcastAssistantCallback callback) {
        log(TAG, "registerCallback: ");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_REGISTER_CALLBACK_FAILED;
    }

    /**
     * Unregister callbacks that are invoked during scan offloading.
     *
     * @param sink BluetoothDevice representing the Scan Delegator
     * @param callback callbacks to be unregistered
     * @hide
     */
    public int unregisterCallback(
            @NonNull BluetoothDevice sink, BluetoothLeBroadcastAssistantCallback callback) {
        log(TAG, "unregisterCallback: ");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_UNREGISTER_CALLBACK_FAILED;
    }

    /**
     * Search for LE Audio Broadcast Sources on behalf of a Scan Delegator.
     *
     * <p>Search results will be delivered to the application using {@link
     * BluetoothLeBroadcastAssistantCallback#onBluetoothLeBroadcastSourceFound}
     *
     * @param sink BluetoothDevice representing the Scan Delegator
     * @hide
     */
    public int searchforBroadcastSources(@NonNull BluetoothDevice sink) {
        log(TAG, "searchforBroadcastSources: ");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_START_SEARCH_FAILED;
    }

    /**
     * Stops an ongoing search for LE Audio Broadcast Sources.
     *
     * @param sink BluetoothDevice representing the Scan Delegator
     * @hide
     */
    public boolean stopSearchforBroadcastSources(@NonNull BluetoothDevice sink) {
        log(TAG, "stopSearchforBroadcastSources: ");
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_STOP_SEARCH_FAILED;
    }

    /**
     * Selects a Broadcast Source on behalf of a Scan Delegator.
     *
     * <p>This internally synchronizes with the Periodic Advertisements (PAs) from the provided
     * Broadcast Source. Upon synchronization, it will notify the Broadcast Assistant about the
     * channels that are available from the Broadcast Source.
     *
     * <p>The application should select the set of channels it wants to synchronize with and then
     * call {@link #addBroadcastSource} method to ask the Scan Delegator to synchronize with the
     * provided audio channels.
     *
     * <p>Result of selection of Broadcast source will be delivered through {@link
     * BluetoothLeBroadcastAssistantCallback#onBluetoothLeBroadcastSourceSelected}
     *
     * @param source BluetoothDevice representing the Broadcast Source to synchronize with
     * @param scanResult {@link #ScanResult} containing a Broadcast Source this is obtained from
     *     {@link BluetoothLeBroadcastAssistantCallback#onBluetoothLeBroadcastSourceFound}
     * @param isGroupOp set to true If Application wants to perform this operation for the whole
     *     coordinated set members
     * @hide
     */
    public int selectBroadcastSource(
            @NonNull BluetoothDevice source, @NonNull ScanResult scanResult, boolean isGroupOp) {
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_SELECT_SOURCE_FAILED;
    }

    /**
     * Asks a Scan Delegator to add the provided Broadcast Source.
     *
     * <p>Internally, this writes the provided Broadcast Source information to the Broadcast Audio
     * Scan Control Point of the Scan Delegator.
     *
     * <p>Upon addition of the Broadcast Source, {@link
     * BluetoothLeBroadcastAssistantCallback#onBluetoothLeBroadcastSourceAdded} will be invoked
     *
     * @param sink {@link #BluetoothDevice} representing the Broadcast Sink to which the Broadcast
     *     Source should be added
     * @param source Broadcast Source to be added to the Scan Delegator
     * @param isGroupOp set to true If Application wants to perform this operation for all
     *     coordinated set members, False otherwise
     * @return returns true if It is successfully initiated add Broadcast source operation false
     *     otherwise
     * @hide
     */
    public int addBroadcastSource(
            @NonNull BluetoothDevice sink,
            @NonNull BluetoothLeBroadcastSourceInfo source,
            boolean isGroupOp) {
        return BluetoothStatusCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_ADD_SOURCE_FAILED;
    }

    /**
     * Updates Broadcast Source information on a Scan Delegator.
     *
     * <p>After updating the Broadcast Source on the Scan Delegator, the callback {@link
     * BluetoothLeBroadcastAssistantCallback#onBluetoothLeBroadcastSourceUpdated} will be invoked.
     *
     * <p>In case of Group Operation, if there are no matching sources among any coordinated set
     * members, this operation will fail and the callback {@link
     * BluetoothLeBroadcastAssistantCallback#onBluetoothLeBroadcastSourceUpdated} will be invoked.
     *
     * @param sink {@link #BluetoothDevice} representing the Broadcast Sink to which the Broadcast
     *     Source should be updated
     * @param source Broadcast Source to be updated on the Scan Delegator
     * @param isGroupOp set to true if the application wants to perform this operation for all the
     *     coordinated set members, false otherwise
     * @hide
     */
    public int updateBroadcastSource(
            @NonNull BluetoothDevice sink,
            @NonNull BluetoothLeBroadcastSourceInfo source,
            boolean isGroupOp) {
        return BluetoothCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_UPDATE_SOURCE_FAILED;
    }

    /**
     * Removes the Broadcast Source Information from a Scan delegator.
     *
     * <p>Upon removal of Broadcast Source information from the Scan Delegator, the callback {@link
     * BluetoothLeBroadcastAssistantCallback#OnBluetoothLeBroadcastSourceRemoved} will be invoked.
     *
     * @param sink {@link #BluetoothDevice} representing the Broadcast Sink from which a Broadcast
     *     Source should be removed
     * @param sourceId source ID of the Broadcast Source which needs to be removed
     * @param isGroupOp true if an application wants to perform this operation for all the
     *     coordinated set members, false otherwise
     * @hide
     */
    public int removeBroadcastSource(
            @NonNull BluetoothDevice sink, int sourceId, boolean isGroupOp) {
        return BluetoothCodes.ERROR_LE_AUDIO_BROADCAST_ASSISTANT_REMOVE_SOURCE_FAILED;
    }

    /**
     * Get information about all the Broadcast Sources that a Scan Delegator knows about.
     *
     * @param sink {@link #BluetoothDevice} representing the Broadcast Sink from which to get all
     *     Broadcast Sources
     * @return returns the List of Broadcast Source Information {@link #BleBroadcastSourceInfo}
     *     stored in the Scan Delegator
     * @hide
     */
    public @NonNull List<BluetoothLeBroadcastSourceInfo> getAllBroadcastSources(
            @NonNull BluetoothDevice sink) {
        List<BluetoothLeBroadcastSourceInfo> sources = new ArrayList<>();
        return sources;
    }

    private static void log(@NonNull String msg) {
        if (DBG) {
            Log.d(TAG, msg);
        }
    }
}
