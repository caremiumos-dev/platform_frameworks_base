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

package android.net;

import static android.system.OsConstants.AF_INET;
import static android.system.OsConstants.AF_INET6;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.os.PersistableBundle;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Class that provides utilities for collecting network connectivity diagnostics information.
 * Connectivity information is made available through triggerable diagnostics tools and by listening
 * to System validations. Some diagnostics information may be permissions-restricted.
 *
 * <p>ConnectivityDiagnosticsManager is intended for use by applications offering network
 * connectivity on a user device. These tools will provide several mechanisms for these applications
 * to be alerted to network conditions as well as diagnose potential network issues themselves.
 *
 * <p>The primary responsibilities of this class are to:
 *
 * <ul>
 *   <li>Perform traceroute diagnostics to given IP addresses
 *   <li>Allow permissioned applications to register and unregister callbacks for network event
 *       notifications
 *   <li>Invoke callbacks for network event notifications, including:
 *       <ul>
 *         <li>Network validations
 *         <li>Data stalls
 *         <li>Connectivity reports from applications
 *       </ul>
 * </ul>
 */
public class ConnectivityDiagnosticsManager {
    private static final int MIN_IPV4_PACKET_SIZE = 20;
    private static final int IPV6_PACKET_SIZE = 40;
    private static final int ICMP_ECHO_REQUEST_SIZE = 8;
    private static final int ICMP6_ECHO_REQUEST_SIZE = 8;

    private static final int TRACEROUTE_IPV4_DEFAULT_PACKET_SIZE = 60;
    private static final int TRACEROUTE_IPV6_DEFAULT_PACKET_SIZE = 80;

    public static final int DETECTION_METHOD_DNS_EVENTS = 1;
    public static final int DETECTION_METHOD_TCP_METRICS = 2;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(
            prefix = {"DETECTION_METHOD_"},
            value = {DETECTION_METHOD_DNS_EVENTS, DETECTION_METHOD_TCP_METRICS})
    public @interface DetectionMethod {}

    /** @hide */
    public ConnectivityDiagnosticsManager() {}

    /**
     * Base class for Route Diagnostics callbacks.
     *
     * <p>Used to provide information about a route diagnostics request to a specific IP address.
     * Must be extended by applications wanting route diagnostics information.
     */
    public abstract static class RouteDiagnosticsCallback {
        /** ProbeResponse represents the network's response to a single probe request. */
        public static class ProbeResponse {
            public static final double TIMEOUT_RTT = -1;

            /** The InetAddress for this ProbeResponse. */
            @NonNull public final InetAddress address;

            /**
             * The TTL (time to live) used for the probe request corresponding to this
             * ProbeResponse.
             */
            public final int ttl;

            /** The RTT (round trip time) in milliseconds for this ProbeResponse. */
            public final double rtt;

            /**
             * Constructor for ProbeResponse.
             *
             * <p>Apps should obtain instances through {@link
             * RouteDiagnosticsCallback#onRouteDiagnosticsComplete} instead of instantiating their
             * own instances (unless for testing purposes).
             *
             * @param address The InetAddress that this probe response came from
             * @param ttl The TTL used for the probe request corresponding to this response
             * @param rtt The RTT for this probe in milliseconds
             */
            public ProbeResponse(@NonNull InetAddress address, int ttl, double rtt) {
                this.address = address;
                this.ttl = ttl;
                this.rtt = rtt;
            }

            /** @hide */
            public ProbeResponse(int ttl) {
                this(null, ttl, TIMEOUT_RTT);
            }
        }

        /**
         * Called when route diagnostics have been completed to the specified host.
         *
         * <p>The network specified may not be active any more when this method is invoked.
         *
         * @param network The Network on which the route diagnostics were collected
         * @param host The InetAddress to which route diagnostics were collected
         * @param probeResponses The ProbeResponses representing the result of the route
         *     diagnostics. ProbeResponses may not be arranged in order of TTL.
         */
        public void onRouteDiagnosticsComplete(
                @NonNull Network network,
                @NonNull InetAddress host,
                @NonNull List<ProbeResponse> probeResponses) {}

        /**
         * Called when a critical error is encountered while computing route diagnostics.
         *
         * <p>The network specified may not be active any more when this method is invoked.
         *
         * @param network The Network on which the route diagnostics were attempted
         * @param host The InetAddress to which route diagnostics were attempted
         * @param cause The Exception that caused the route diagnostics to fail. If an error is
         *     encountered while sending probes over the network, this will be an {@link
         *     IOException}. If the error is due to the host being unreachable, cause will be an
         *     {@link IcmpException} or {@link Icmpv6Exception} depending on whether host is an IPv4
         *     or IPv6 address, respectively. If an MTU (maximum transmission unit) is encountered
         *     while collecting diagnostics to an IPv6 host, cause will be an {@link
         *     Icmpv6PacketTooBigException}.
         */
        public void onError(
                @NonNull Network network, @NonNull InetAddress host, @NonNull Exception cause) {}
    }

    /**
     * Class used to configure the route diagnostics performed by the system.
     *
     * <p>The default configurations are:
     *
     * <ul>
     *   <li>Max TTL: 30
     *   <li>Probes per Hop: 3
     *   <li>Packet Size: 60 Bytes for IPv4, or 80 Bytes for IPv6
     *   <li>Single Probe Timeout Milliseconds: 5,000 ms
     *   <li>Concurrent Probes Sent: 16
     * </ul>
     *
     * <p>It is also possible to configure a timeout for the overall diagnostics procedure. If this
     * timeout expires before diagnostics collection is completed, {@link
     * RouteDiagnosticsCallback#onRouteDiagnosticsComplete} will be invoked with the available
     * results (or {@link RouteDiagnosticsCallback#onError} in the event of an error). By default,
     * no timeout is set.
     */
    public static class RouteDiagnosticsParams {
        private static final int DEFAULT_MAX_TTL = 30;
        private static final int DEFAULT_PROBES_PER_HOP = 3;
        private static final int DEFAULT_PACKET_SIZE_BYTES = 0;
        private static final int DEFAULT_PROBE_TIMEOUT_MILLIS = 5000;
        private static final int DEFAULT_DIAGNOSTICS_TIMEOUT_MILLIS = 0;
        private static final int DEFAULT_CONCURRENT_PROBES_LIMIT = 16;

        public final int maxTtl;
        public final int probesPerHop;
        public final int packetSizeBytes;
        public final int probeTimeoutMillis;
        public final int diagnosticsTimeoutMillis;
        public final int concurrentProbesLimit;

        private RouteDiagnosticsParams(
                int maxTtl,
                int probesPerHop,
                int packetSizeBytes,
                int probeTimeoutMillis,
                int diagnosticsTimeoutMillis,
                int concurrentProbesLimit) {
            this.maxTtl = maxTtl;
            this.probesPerHop = probesPerHop;
            this.packetSizeBytes = packetSizeBytes;
            this.probeTimeoutMillis = probeTimeoutMillis;
            this.diagnosticsTimeoutMillis = diagnosticsTimeoutMillis;
            this.concurrentProbesLimit = concurrentProbesLimit;
        }

        /**
         * Class to be used for configuring a RouteDiagnosticsParams instance.
         *
         * <p>The defaults are:
         *
         * <ul>
         *   <li>Max TTL: 30
         *   <li>Probes per Hop: 3
         *   <li>Packet Size: 60 Bytes for IPv4, or 80 Bytes for IPv6
         *   <li>Single Probe Timeout Milliseconds: 5,000 ms
         *   <li>Diagnostics Timeout Milliseconds: 0 ms (no timeout)
         *   <li>Concurrent Probes Limit: 16
         * </ul>
         */
        public static class Builder {
            private int mMaxTtl = DEFAULT_MAX_TTL;
            private int mProbesPerHop = DEFAULT_PROBES_PER_HOP;
            private int mPacketSizeBytes = DEFAULT_PACKET_SIZE_BYTES;
            private int mProbeTimeoutMillis = DEFAULT_PROBE_TIMEOUT_MILLIS;
            private int mDiagnosticsTimeoutMillis = DEFAULT_DIAGNOSTICS_TIMEOUT_MILLIS;
            private int mConcurrentProbesLimit = DEFAULT_CONCURRENT_PROBES_LIMIT;

            /** Constructs a new RouteDiagnosticsParams.Builder. */
            public Builder() {}

            /**
             * Sets the maximum TTL (inclusive).
             *
             * @param maxTtl The maximum TTL to be used
             * @return Builder this, to facilitate chaining
             * @throws IllegalArgumentException iff maxTtl < 1
             */
            @NonNull
            public Builder setMaxTtl(int maxTtl) {
                if (maxTtl < 1) {
                    throw new IllegalArgumentException("Max TTL must be at least 1");
                }
                mMaxTtl = maxTtl;
                return this;
            }

            /**
             * Sets the probes per hop.
             *
             * @param probesPerHop The number of probes to be sent for each TTL
             * @return Builder this, to facilitate chaining
             * @throws IllegalArgumentException iff probesPerHop < 1
             */
            @NonNull
            public Builder setProbesPerHop(int probesPerHop) {
                if (probesPerHop < 1) {
                    throw new IllegalArgumentException("Probes per Hop must be at least 1");
                }
                mProbesPerHop = probesPerHop;
                return this;
            }

            /**
             * Sets the packet size for each outgoing probe.
             *
             * <p>The minimum allowed packet size is dependent on the address type used in {@link
             * ConnectivityDiagnosticsManager#requestRouteDiagnostics}:
             *
             * <ul>
             *   <li>IPv4 address: the packet must be at least 28 Bytes
             *   <li>IPv6 address: the packet must be at least 48 Bytes
             * </ul>
             *
             * @param packetSizeBytes The packet size (in Bytes) to be used for each outgoing probe
             * @return Builder this, to facilitate chaining.
             */
            @NonNull
            public Builder setPacketSizeBytes(int packetSizeBytes) {
                mPacketSizeBytes = packetSizeBytes;
                return this;
            }

            /**
             * Sets the timeout to be used for each outgoing probe.
             *
             * <p>The timeout will be the maximum wait time allowed for a probe response. That is,
             * the network node's response message must be received by at most timeoutMillis ms
             * after the probe is sent.
             *
             * @param probeTimeoutMillis The timeout (in milliseconds) to be used for each outgoing
             *     probe
             * @return Builder this, to facilitate chaining
             * @throws IllegalArgumentException iff probeTimeoutMillis < 1
             */
            @NonNull
            public Builder setProbeTimeoutMillis(int probeTimeoutMillis) {
                if (probeTimeoutMillis < 1) {
                    throw new IllegalArgumentException(
                            "Timeout (in milliseconds) must be at least 1");
                }
                mProbeTimeoutMillis = probeTimeoutMillis;
                return this;
            }

            /**
             * Sets the timeout for route diagnostics collection.
             *
             * <p>If route diagnostics have not been fully collected after {@code
             * diagnosticsTimeoutMillis} ms have passed, {@link
             * RouteDiagnosticsCallback#onRouteDiagnosticsComplete} will be invoked with the
             * available results.
             *
             * <p>By default, no timeout is set. This is specified by setting a timeout of <b>0
             * ms</b>.
             *
             * @param diagnosticsTimeoutMillis The timeout (in milliseconds) to be used for route
             *     diagnostics collection
             * @return Builder this, to facilitate chaining
             * @throws IllegalArgumentException iff diagnosticsTimeoutMillis < 0
             */
            @NonNull
            public Builder setDiagnosticsTimeoutMillis(int diagnosticsTimeoutMillis) {
                if (diagnosticsTimeoutMillis < 0) {
                    throw new IllegalArgumentException(
                            "Timeout (in milliseconds) must be at least 0");
                }
                mDiagnosticsTimeoutMillis = diagnosticsTimeoutMillis;
                return this;
            }

            /**
             * Sets the limit for how many probes can be sent concurrently.
             *
             * <p>Care should be taken when deciding the limit for concurrent probes. Setting too
             * high of a limit may result in rate throttling in the network, resulting in a
             * higher-than-expected number of timeouts reported in {@link
             * RouteDiagnosticsCallback#onRouteDiagnosticsComplete}.
             *
             * @param concurrentProbesLimit The limit for how many probes can be sent concurrently
             * @return Builder this, to facilitate chaining
             * @throws IllegalArgumentException iff concurrentProbesLimit < 1
             */
            @NonNull
            public Builder setConcurrentProbesLimit(int concurrentProbesLimit) {
                if (concurrentProbesLimit < 1) {
                    throw new IllegalArgumentException("ConcurrentProbesLimit must be at least 1");
                }
                mConcurrentProbesLimit = concurrentProbesLimit;
                return this;
            }

            /**
             * Constructs and returns a RouteDiagnosticsParams with the configurations applied to
             * this Builder.
             *
             * @return the RouteDiagnosticsParams constructed by this Builder
             */
            @NonNull
            public RouteDiagnosticsParams build() {
                return new RouteDiagnosticsParams(
                        mMaxTtl,
                        mProbesPerHop,
                        mPacketSizeBytes,
                        mProbeTimeoutMillis,
                        mDiagnosticsTimeoutMillis,
                        mConcurrentProbesLimit);
            }
        }
    }

    /**
     * Request diagnostic information be collected for a particular IP address or domain.
     *
     * <p>{@code requestRouteDiagnostics} is the Android API equivalent to the command line {@code
     * traceroute} tool.
     *
     * <p>Every call to {@link ConnectivityDiagnosticsManager#requestRouteDiagnostics} will result
     * in exactly one call to:
     *
     * <ul>
     *   <li>{@link RouteDiagnosticsCallback#onRouteDiagnosticsComplete}, or
     *   <li>{@link RouteDiagnosticsCallback#onError}
     * </ul>
     *
     * @param network The Network to be used for route diagnostics
     * @param host The InetAddress to be targeted for route diagnostics
     * @param routeDiagnosticsParams The configs to be used for collecting the route diagnostics
     * @param diagnosticsExecutor The Executor on which the route diagnostics will be computed
     * @param callbackExecutor The Executor on which the callback will be invoked
     * @param callback The RouteDiagnosticsCallback to be invoked
     * @throws IllegalArgumentException iff host is not an IPv4 address or an IPv6 address, or if
     *     the specified packet size is too small for the specified protocol. For IPv4, packet size
     *     must be at least 28 Bytes. For IPv6, packet size must be at least 48 Bytes
     */
    public void requestRouteDiagnostics(
            @NonNull Network network,
            @NonNull InetAddress host,
            @NonNull RouteDiagnosticsParams routeDiagnosticsParams,
            @NonNull Executor diagnosticsExecutor,
            @NonNull Executor callbackExecutor,
            @NonNull RouteDiagnosticsCallback callback) {
        int protocolFamily;
        int minPacketSize;
        int defaultPacketSize;
        if (host instanceof Inet4Address) {
            protocolFamily = AF_INET;
            minPacketSize = MIN_IPV4_PACKET_SIZE + ICMP_ECHO_REQUEST_SIZE;
            defaultPacketSize = TRACEROUTE_IPV4_DEFAULT_PACKET_SIZE;
        } else if (host instanceof Inet6Address) {
            protocolFamily = AF_INET6;
            minPacketSize = IPV6_PACKET_SIZE + ICMP6_ECHO_REQUEST_SIZE;
            defaultPacketSize = TRACEROUTE_IPV6_DEFAULT_PACKET_SIZE;
        } else {
            throw new IllegalArgumentException("host must be IPv4 or IPv6 address");
        }
        if (routeDiagnosticsParams.packetSizeBytes < minPacketSize) {
            throw new IllegalArgumentException(
                    "The packet size in the given RouteDiagnosticsParams is not large enough for"
                            + " the specified IP protocol");
        }

        // TODO(b/143189134): implement Route Diagnostics functionality
        throw new UnsupportedOperationException("requestRouteDiagnostics() not supported yet");
    }

    /** Class that includes connectivity information for a specific Network at a specific time. */
    public static class ConnectivityReport {
        /** The Network for which this ConnectivityReport applies */
        @NonNull public final Network network;

        /**
         * The timestamp for the report. The timestamp is taken from {@link
         * System#currentTimeMillis}.
         */
        public final long reportTimestamp;

        /** LinkProperties available on the Network at the reported timestamp */
        @NonNull public final LinkProperties linkProperties;

        /** NetworkCapabilities available on the Network at the reported timestamp */
        @NonNull public final NetworkCapabilities networkCapabilities;

        /** PersistableBundle that may contain additional info about the report */
        @NonNull public final PersistableBundle additionalInfo;

        /**
         * Constructor for ConnectivityReport.
         *
         * <p>Apps should obtain instances through {@link
         * ConnectivityDiagnosticsCallback#onConnectivityReport} instead of instantiating their own
         * instances (unless for testing purposes).
         *
         * @param network The Network for which this ConnectivityReport applies
         * @param reportTimestamp The timestamp for the report
         * @param linkProperties The LinkProperties available on network at reportTimestamp
         * @param networkCapabilities The NetworkCapabilities available on network at
         *     reportTimestamp
         * @param additionalInfo A PersistableBundle that may contain additional info about the
         *     report
         */
        public ConnectivityReport(
                @NonNull Network network,
                long reportTimestamp,
                @NonNull LinkProperties linkProperties,
                @NonNull NetworkCapabilities networkCapabilities,
                @NonNull PersistableBundle additionalInfo) {
            this.network = network;
            this.reportTimestamp = reportTimestamp;
            this.linkProperties = linkProperties;
            this.networkCapabilities = networkCapabilities;
            this.additionalInfo = additionalInfo;
        }
    }

    /** Class that includes information for a suspected data stall on a specific Network */
    public static class DataStallReport {
        /** The Network for which this DataStallReport applies */
        @NonNull public final Network network;

        /**
         * The timestamp for the report. The timestamp is taken from {@link
         * System#currentTimeMillis}.
         */
        public final long reportTimestamp;

        /** The detection method used to identify the suspected data stall */
        @DetectionMethod public final int detectionMethod;

        /** PersistableBundle that may contain additional information on the suspected data stall */
        @NonNull public final PersistableBundle stallDetails;

        /**
         * Constructor for DataStallReport.
         *
         * <p>Apps should obtain instances through {@link
         * ConnectivityDiagnosticsCallback#onDataStalled} instead of instantiating their own
         * instances (unless for testing purposes).
         *
         * @param network The Network for which this DataStallReport applies
         * @param reportTimestamp The timestamp for the report
         * @param detectionMethod The detection method used to identify this data stall
         * @param stallDetails A PersistableBundle that may contain additional info about the report
         */
        public DataStallReport(
                @NonNull Network network,
                long reportTimestamp,
                @DetectionMethod int detectionMethod,
                @NonNull PersistableBundle stallDetails) {
            this.network = network;
            this.reportTimestamp = reportTimestamp;
            this.detectionMethod = detectionMethod;
            this.stallDetails = stallDetails;
        }
    }

    /**
     * Abstract base class for Connectivity Diagnostics callbacks. Used for notifications about
     * network connectivity events. Must be extended by applications wanting notifications.
     */
    public abstract static class ConnectivityDiagnosticsCallback {
        /**
         * Called when the platform completes a data connectivity check. This will also be invoked
         * upon registration with the latest report.
         *
         * @param report The ConnectivityReport containing information about a connectivity check
         */
        void onConnectivityReport(ConnectivityReport report) {}

        /**
         * Called when the platform suspects a data stall on some Network.
         *
         * @param report The DataStallReport containing information about the suspected data stall
         */
        void onDataStalled(DataStallReport report) {}

        /**
         * Called when any app reports connectivity to the System.
         *
         * @param network The Network for which connectivity has been reported
         * @param hasConnectivity The connectivity reported to the System
         */
        void onNetworkConnectivityReported(Network network, boolean hasConnectivity) {}
    }

    /**
     * Registers a ConnectivityDiagnosticsCallback with the System.
     *
     * <p>Only apps that offer network connectivity to the user are allowed to register callbacks.
     * This includes:
     *
     * <ul>
     *   <li>Carrier apps with active subscriptions
     *   <li>Active VPNs
     *   <li>WiFi Suggesters
     * </ul>
     *
     * <p>Callbacks will be limited to receiving notifications for networks over which apps provide
     * connectivity.
     *
     * <p>If a registering app loses its relevant permissions, any callbacks it registered will
     * silently stop receiving callbacks.
     *
     * <p>Each register() call <b>MUST</b> use a unique ConnectivityDiagnosticsCallback instance. If
     * a single instance is registered with multiple NetworkRequests, an IllegalArgumentException
     * will be thrown.
     *
     * @param request The NetworkRequest that will be used to match with Networks for which
     *     callbacks will be fired
     * @param callback The ConnectivityDiagnosticsCallback that the caller wants registered with the
     *     System
     * @param e The Executor to be used for running the callback method invocations
     * @throws IllegalArgumentException if the same callback instance is registered with multiple
     *     NetworkRequests
     * @throws SecurityException if the caller does not have appropriate permissions to register a
     *     callback
     */
    public void registerConnectivityDiagnosticsCallback(
            @NonNull NetworkRequest request,
            @NonNull ConnectivityDiagnosticsCallback callback,
            @NonNull Executor e) {
        // TODO(b/143187964): implement ConnectivityDiagnostics functionality
        throw new UnsupportedOperationException("registerCallback() not supported yet");
    }

    /**
     * Unregisters a ConnectivityDiagnosticsCallback with the System.
     *
     * <p>If the given callback is not currently registered with the System, this operation will be
     * a no-op.
     *
     * @param callback The ConnectivityDiagnosticsCallback to be unregistered from the System.
     */
    public void unregisterConnectivityDiagnosticsCallback(
            @NonNull ConnectivityDiagnosticsCallback callback) {
        // TODO(b/143187964): implement ConnectivityDiagnostics functionality
        throw new UnsupportedOperationException("registerCallback() not supported yet");
    }
}
