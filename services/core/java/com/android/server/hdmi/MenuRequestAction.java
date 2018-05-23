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
 * limitations under the License.
 */

package com.android.server.hdmi;

import android.hardware.hdmi.HdmiDeviceInfo;
import android.hardware.tv.cec.V1_0.SendMessageResult;
import android.util.Slog;
import android.content.Context;

import com.android.internal.util.Preconditions;

import java.util.Arrays;

/**
 * Feature action that transmits command of <Menu Request>.
 * A request from the TV for a device to show/remove a menu or to query
 * if device is currently showing a menu.
 */
final class MenuRequestAction extends HdmiCecFeatureAction {
    private static final String TAG = "MenuRequest";

    /**
    * Interface used to update menu state.
    */
    interface MenuStateCallback {
        /**
         * Called when menu audio mode is set.
         *
         * @param menuState specifies the state of a device menu
         */
        void updateMenuState(boolean menuState);
    }


    // State in which the action sent <Menu Request> and
    // is waiting for time out. If it receives <Feature Abort> within timeout.
    private static final int STATE_WAITING_TIMEOUT = 1;

    private final int mMenuRequestType;
    private final int mtargetAddress;
    private final MenuStateCallback mCallback;

    /**
     * @Constructor
     *
     * @param source {@link HdmiCecLocalDevice} instance
     * @param targetAddress logical address of the device to send the command to
     * @param menuRequestType: 0-Activate, 1-Deactivate, 2-Query
     */
    MenuRequestAction(HdmiCecLocalDevice source, int targetAddress,
            int menuRequestType, MenuStateCallback callback) {
        super(source);
        HdmiUtils.verifyAddressType(getSourceAddress(), HdmiDeviceInfo.DEVICE_TV);
        mtargetAddress = targetAddress;
        mMenuRequestType = menuRequestType;
        mCallback = Preconditions.checkNotNull(callback);
    }

    @Override
    boolean start() {
        sendMenuRequest();
        finish();
        return true;
    }

    private void sendMenuRequest() {
        HdmiCecMessage command =
                HdmiCecMessageBuilder.buildMenuRequest(getSourceAddress(),
                    mtargetAddress, mMenuRequestType);
        sendCommand(command, new HdmiControlService.SendMessageCallback() {
            @Override
            public void onSendCompleted(int error) {
                switch (error) {
                    case SendMessageResult.SUCCESS:
                    case SendMessageResult.BUSY:
                    case SendMessageResult.FAIL:
                        //Ignores it silently.
                        break;
                    case SendMessageResult.NACK:
                        HdmiLogger.debug("Failed to send <Menu Request>.");
                        finish();
                        break;
                }
            }
        });
    }

    @Override
    boolean processCommand(HdmiCecMessage cmd) {
        int opcode = cmd.getOpcode();
        int src = cmd.getSource();
        byte[] params = cmd.getParams();

        if (mtargetAddress != src) {
            return false;
        }
        if (opcode == Constants.MESSAGE_FEATURE_ABORT) {
            int originalOpcode = cmd.getParams()[0] & 0xFF;
            if (originalOpcode == Constants.MESSAGE_MENU_REQUEST) {
                HdmiLogger.debug("Feature aborted for <Menu Request>");
                mCallback.updateMenuState((Constants.MENU_STATE_DEACTIVATED != 0));
                finish();
                return true;
            }
        } else if (opcode == Constants.MESSAGE_MENU_STATUS) {
            HdmiLogger.debug("ProcessCommand: <Menu Status>");
            if (params[0] == Constants.MENU_STATE_ACTIVATED) {
                HdmiLogger.debug("MENU_STATE_ACTIVATED");
                mCallback.updateMenuState((Constants.MENU_STATE_ACTIVATED != 0));
                finish();
                return true;
            }
            HdmiLogger.debug("MENU_STATE_DEACTIVATED");
            mCallback.updateMenuState((Constants.MENU_STATE_DEACTIVATED != 0));
            HdmiLogger.debug("end ProcessCommand: <Menu Status>");
            finish();
            return true;
        }
        return false;
    }

    @Override
    void handleTimerEvent(int state) {
        if (mState != state || mState != STATE_WAITING_TIMEOUT) {
            return;
        }
        // Expire timeout for <Feature Abort>.
        finish();
    }
}
