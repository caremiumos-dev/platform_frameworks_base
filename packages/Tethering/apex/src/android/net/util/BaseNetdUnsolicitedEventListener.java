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
package android.net.util;

import android.net.INetdUnsolicitedEventListener;

/**
 * Base {@link INetdUnsolicitedEventListener} that provides no-op implementations which can be
 * overridden.
 */
public class BaseNetdUnsolicitedEventListener extends INetdUnsolicitedEventListener.Stub {

    @Override
    public void onInterfaceClassActivityChanged(boolean isActive, int timerLabel, long timestampNs,
            int uid) { }

    @Override
    public void onQuotaLimitReached(String alertName, String ifName) { }

    @Override
    public void onInterfaceDnsServerInfo(String ifName, long lifetimeS, String[] servers) { }

    @Override
    public void onInterfaceAddressUpdated(String addr, String ifName, int flags, int scope) { }

    @Override
    public void onInterfaceAddressRemoved(String addr, String ifName, int flags, int scope) { }

    @Override
    public void onInterfaceAdded(String ifName) { }

    @Override
    public void onInterfaceRemoved(String ifName) { }

    @Override
    public void onInterfaceChanged(String ifName, boolean up) { }

    @Override
    public void onInterfaceLinkStateChanged(String ifName, boolean up) { }

    @Override
    public void onRouteChanged(boolean updated, String route, String gateway, String ifName) { }

    @Override
    public void onStrictCleartextDetected(int uid, String hex) { }

    @Override
    public int getInterfaceVersion() {
        return INetdUnsolicitedEventListener.VERSION;
    }
}
