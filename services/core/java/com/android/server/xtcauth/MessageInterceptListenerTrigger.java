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

package com.android.server.xtcauth;

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

import com.xtc.auth.IMessageInterceptListener;

/**
 * Asks the management application whether an incoming SMS may be intercepted.
 *
 * @hide
 */
final class MessageInterceptListenerTrigger extends ListenerTriggerBase {
    public static final String TAG = "MessageInterceptListenerTrigger";

    public MessageInterceptListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult) {
        super(controllerID, callingPid, listener, params, defaultResult);
    }

    public MessageInterceptListenerTrigger(int controllerID, int callingPid, IBinder listener,
            Bundle params, int defaultResult, IBinder callback) {
        super(controllerID, callingPid, listener, params, defaultResult, callback);
    }

    @Override
    public int triggerListenerFunc() {
        Bundle params = getParams();
        IBinder func = getFunc();
        Intent intent = (Intent) params.getParcelable("intent");
        if (intent == null) {
            Log.d(TAG, "get intent null");
            return -1;
        }
        SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
        boolean allow = true;
        Log.d(TAG, "triggerListenerFunc() subId : " + params.getLong("subId"));
        if (messages == null) {
            Log.d(TAG, "[MoMS >> MoMA] onNewSMSCheck(), empty msgs");
        } else {
            Log.d(TAG, "[MoMS >> MoMA] onNewSMSCheck(), new pdu size : " + messages.length);
        }
        IMessageInterceptListener listener = IMessageInterceptListener.Stub.asInterface(func);
        if (listener != null) {
            try {
                allow = listener.onNewSMSCheck(params);
                Log.d(TAG, "onNewSMSCheck result=" + allow);
            } catch (android.os.RemoteException e) {
                Log.e(TAG, "onNewSMSCheck() failed!", e);
            }
        } else {
            Log.w(TAG, "Ignore null listener");
        }
        return allow ? 0 : -1;
    }

    @Override
    public void onTriggerListenerDone() {
    }

    @Override
    public void onTriggerListenerTimeout() {
    }
}