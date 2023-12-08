/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.server.vcn.routeselection;

import static com.android.server.VcnManagementService.LOCAL_LOG;
import static com.android.server.vcn.util.PersistableBundleUtils.PersistableBundleWrapper;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.vcn.VcnUnderlyingNetworkTemplate;
import android.os.ParcelUuid;

import com.android.internal.util.IndentingPrintWriter;
import com.android.server.vcn.TelephonySubscriptionTracker.TelephonySubscriptionSnapshot;
import com.android.server.vcn.VcnContext;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * UnderlyingNetworkEvaluator evaluates the quality and priority class of a network candidate for
 * route selection.
 *
 * @hide
 */
class UnderlyingNetworkEvaluator {
    private static final String TAG = UnderlyingNetworkEvaluator.class.getSimpleName();

    @NonNull private final VcnContext mVcnContext;
    @NonNull private final UnderlyingNetworkRecord.Builder mNetworkRecordBuilder;
    @NonNull private final List<VcnUnderlyingNetworkTemplate> mUnderlyingNetworkTemplates;
    @NonNull private final ParcelUuid mSubscriptionGroup;
    @NonNull private final TelephonySubscriptionSnapshot mLastSnapshot;
    @Nullable private final PersistableBundleWrapper mCarrierConfig;

    @Nullable private UnderlyingNetworkRecord mCurrentRecord;

    private boolean mIsSelected;
    private int mPriorityClass = NetworkPriorityClassifier.PRIORITY_INVALID;

    UnderlyingNetworkEvaluator(
            @NonNull VcnContext vcnContext,
            @NonNull Network network,
            @NonNull List<VcnUnderlyingNetworkTemplate> underlyingNetworkTemplates,
            @NonNull ParcelUuid subscriptionGroup,
            @NonNull TelephonySubscriptionSnapshot lastSnapshot,
            @Nullable PersistableBundleWrapper carrierConfig) {
        mVcnContext = vcnContext;
        mUnderlyingNetworkTemplates = underlyingNetworkTemplates;
        mSubscriptionGroup = subscriptionGroup;
        mLastSnapshot = lastSnapshot;
        mCarrierConfig = carrierConfig;

        mNetworkRecordBuilder = new UnderlyingNetworkRecord.Builder(network);
        mCurrentRecord = null;
        mIsSelected = false;

        updatePriorityClass();

        logInfo("Constructed");
    }

    private void updatePriorityClass() {
        if (mNetworkRecordBuilder.isValid()) {
            mPriorityClass =
                    NetworkPriorityClassifier.calculatePriorityClass(
                            mVcnContext,
                            mNetworkRecordBuilder.build(),
                            mUnderlyingNetworkTemplates,
                            mSubscriptionGroup,
                            mLastSnapshot,
                            mCurrentRecord,
                            mCarrierConfig);
        } else {
            mPriorityClass = NetworkPriorityClassifier.PRIORITY_INVALID;
        }
    }

    static Comparator<UnderlyingNetworkEvaluator> getComparator() {
        return (left, right) -> {
            final int leftIndex = left.mPriorityClass;
            final int rightIndex = right.mPriorityClass;

            // In the case of networks in the same priority class, prioritize based on other
            // criteria (eg. actively selected network, link metrics, etc)
            if (leftIndex == rightIndex) {
                // TODO: Improve the strategy of network selection when both UnderlyingNetworkRecord
                // fall into the same priority class.
                if (left.mIsSelected) {
                    return -1;
                }
                if (right.mIsSelected) {
                    return 1;
                }
            }
            return Integer.compare(leftIndex, rightIndex);
        };
    }

    void setNetworkCapabilities(@NonNull NetworkCapabilities nc) {
        mNetworkRecordBuilder.setNetworkCapabilities(nc);
        updatePriorityClass();
    }

    void setLinkProperties(@NonNull LinkProperties lp) {
        mNetworkRecordBuilder.setLinkProperties(lp);
        updatePriorityClass();
    }

    void setIsBlocked(boolean isBlocked) {
        mNetworkRecordBuilder.setIsBlocked(isBlocked);
        updatePriorityClass();
    }

    void setSelectedNetwork(@Nullable UnderlyingNetworkRecord currentlySelected) {
        if (currentlySelected == null) {
            mIsSelected = false;
        } else {
            mIsSelected =
                    Objects.equals(currentlySelected.network, mNetworkRecordBuilder.getNetwork());
        }

        updatePriorityClass();
    }

    boolean isValid() {
        return mNetworkRecordBuilder.isValid();
    }

    Network getNetwork() {
        return mNetworkRecordBuilder.getNetwork();
    }

    UnderlyingNetworkRecord getNetworkRecord() {
        return mNetworkRecordBuilder.build();
    }

    int getPriorityClass() {
        return mPriorityClass;
    }

    void dump(IndentingPrintWriter pw) {
        pw.println("UnderlyingNetworkEvaluator:");
        pw.increaseIndent();

        if (mNetworkRecordBuilder.isValid()) {
            getNetworkRecord().dump(pw);
        } else {
            pw.println("mNetwork: " + mNetworkRecordBuilder.getNetwork());
        }

        pw.println("mIsSelected: " + mIsSelected);
        pw.println("mPriorityClass: " + mPriorityClass);

        pw.decreaseIndent();
    }

    private static String getLogPrefix() {
        return "[Network " + mNetworkRecordBuilder.getNetwork() + "] ";
    }

    private void logInfo(String msg) {
        Slog.i(TAG, getLogPrefix() + msg);
        LOCAL_LOG.log("[INFO ] " + TAG + getLogPrefix() + msg);
    }
}
