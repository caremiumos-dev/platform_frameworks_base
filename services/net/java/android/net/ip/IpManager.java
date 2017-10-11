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

package android.net.ip;

import android.content.Context;
import android.net.INetd;
import android.net.LinkProperties;
import android.net.Network;
import android.net.StaticIpConfiguration;
import android.net.apf.ApfCapabilities;
import android.net.util.NetdService;
import android.os.INetworkManagementService;
import android.os.ServiceManager;
import android.net.apf.ApfCapabilities;

import com.android.internal.annotations.VisibleForTesting;


/*
 * TODO: Delete this altogether in favor of its renamed successor: IpClient.
 *
 * @hide
 */
public class IpManager extends IpClient {
    public static class ProvisioningConfiguration extends IpClient.ProvisioningConfiguration {
        public ProvisioningConfiguration(IpClient.ProvisioningConfiguration ipcConfig) {
            super(ipcConfig);
        }

        public static class Builder extends IpClient.ProvisioningConfiguration.Builder {
            @Override
            public Builder withoutIPv4() {
                return (Builder) super.withoutIPv4();
            }
            @Override
            public Builder withoutIPv6() {
                return (Builder) super.withoutIPv6();
            }
            @Override
            public Builder withoutIpReachabilityMonitor() {
                return (Builder) super.withoutIpReachabilityMonitor();
            }
            @Override
            public Builder withPreDhcpAction() {
                return (Builder) super.withPreDhcpAction();
            }
            @Override
            public Builder withPreDhcpAction(int dhcpActionTimeoutMs) {
                return (Builder) super.withPreDhcpAction(dhcpActionTimeoutMs);
            }
            // No Override; locally defined type.
            public Builder withInitialConfiguration(InitialConfiguration initialConfig) {
                return (Builder) super.withInitialConfiguration(
                        (IpClient.InitialConfiguration) initialConfig);
            }
            @Override
            public Builder withStaticConfiguration(StaticIpConfiguration staticConfig) {
                return (Builder) super.withStaticConfiguration(staticConfig);
            }
            @Override
            public Builder withApfCapabilities(ApfCapabilities apfCapabilities) {
                return (Builder) super.withApfCapabilities(apfCapabilities);
            }
            @Override
            public Builder withProvisioningTimeoutMs(int timeoutMs) {
                return (Builder) super.withProvisioningTimeoutMs(timeoutMs);
            }
            @Override
            public Builder withIPv6AddrGenModeEUI64() {
                return (Builder) super.withIPv6AddrGenModeEUI64();
            }
            @Override
            public Builder withIPv6AddrGenModeStablePrivacy() {
                return (Builder) super.withIPv6AddrGenModeStablePrivacy();
            }
            @Override
            public Builder withNetwork(Network network) {
                return (Builder) super.withNetwork(network);
            }
            @Override
            public Builder withDisplayName(String displayName) {
                return (Builder) super.withDisplayName(displayName);
            }
            @Override
            public ProvisioningConfiguration build() {
                return new ProvisioningConfiguration(super.build());
            }
        }
    }

    public static ProvisioningConfiguration.Builder buildProvisioningConfiguration() {
        return new ProvisioningConfiguration.Builder();
    }

    public static class InitialConfiguration extends IpClient.InitialConfiguration {
    }

    public static class Callback extends IpClient.Callback {
    }

    public static class WaitForProvisioningCallback extends Callback {
        private LinkProperties mCallbackLinkProperties;

        public LinkProperties waitForProvisioning() {
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {}
                return mCallbackLinkProperties;
            }
        }

        @Override
        public void onProvisioningSuccess(LinkProperties newLp) {
            synchronized (this) {
                mCallbackLinkProperties = newLp;
                notify();
            }
        }

        @Override
        public void onProvisioningFailure(LinkProperties newLp) {
            synchronized (this) {
                mCallbackLinkProperties = null;
                notify();
            }
        }
    }

    public IpManager(Context context, String ifName, Callback callback) {
        this(context, ifName, callback, INetworkManagementService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORKMANAGEMENT_SERVICE)),
                NetdService.getInstance());
    }

    public IpManager(Context context, String ifName, Callback callback,
            INetworkManagementService nwService) {
        this(context, ifName, callback, nwService, NetdService.getInstance());
    }

    @VisibleForTesting
    public IpManager(Context context, String ifName, Callback callback,
            INetworkManagementService nwService, INetd netd) {
        super(context, ifName, callback, nwService, netd);
    }

    public void startProvisioning(ProvisioningConfiguration req) {
        super.startProvisioning((IpClient.ProvisioningConfiguration) req);
    }
}
