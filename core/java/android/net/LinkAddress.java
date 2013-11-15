/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.os.Parcel;
import android.os.Parcelable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.UnknownHostException;

import static libcore.io.OsConstants.IFA_F_DADFAILED;
import static libcore.io.OsConstants.IFA_F_DEPRECATED;
import static libcore.io.OsConstants.IFA_F_PERMANENT;
import static libcore.io.OsConstants.IFA_F_TENTATIVE;

/**
 * Identifies an IP address on a network link.
 * @hide
 */
public class LinkAddress implements Parcelable {
    /**
     * IPv4 or IPv6 address.
     */
    private InetAddress address;

    /**
     * Prefix length.
     */
    private int prefixLength;

    /**
     * Address flags. A bitmask of IFA_F_* values.
     */
    private int flags;

    /**
     * Address scope. A value of 0 indicates global scope.
     */
    private int scope;

    /**
     * Returns true if an address is global scope and not deprecated.
     */
    public boolean isUsable() {
        return (scope == 0 &&
                (flags & (IFA_F_DADFAILED | IFA_F_DEPRECATED | IFA_F_TENTATIVE)) == 0L);
    }

    private void init(InetAddress address, int prefixLength, int flags, int scope) {
        if (address == null || prefixLength < 0 ||
                ((address instanceof Inet4Address) && prefixLength > 32) ||
                (prefixLength > 128)) {
            throw new IllegalArgumentException("Bad LinkAddress params " + address +
                    "/" + prefixLength);
        }
        this.address = address;
        this.prefixLength = prefixLength;
        this.flags = flags;
        this.scope = scope;
    }

    public LinkAddress(InetAddress address, int prefixLength, int flags, int scope) {
        init(address, prefixLength, flags, scope);
    }

    /**
     * Constructs a new {@code LinkAddress} from an {@code InetAddress} and a prefix length.
     * @param address The IP address.
     * @param prefixLength The prefix length.
     */
    public LinkAddress(InetAddress address, int prefixLength) {
        init(address, prefixLength, IFA_F_PERMANENT, 0);
    }

    /**
     * Constructs a new {@code LinkAddress} from an {@code InterfaceAddress}.
     * @param interfaceAddress The interface address.
     */
    public LinkAddress(InterfaceAddress interfaceAddress) {
        init(interfaceAddress.getAddress(),
             interfaceAddress.getNetworkPrefixLength(),
             IFA_F_PERMANENT, 0);
    }

    /**
     * Constructs a new {@code LinkAddress} from a string such as "192.0.2.5/24" or
     * "2001:db8::1/64" and the specified flags and scope.
     * @param string The string to parse.
     * @param flags The address flags.
     * @param scope The address scope.
     */
    public LinkAddress(String address, int flags, int scope) {
        InetAddress inetAddress = null;
        int prefixLength = -1;
        try {
            String [] pieces = address.split("/", 2);
            prefixLength = Integer.parseInt(pieces[1]);
            inetAddress = InetAddress.parseNumericAddress(pieces[0]);
        } catch (NullPointerException e) {            // Null string.
        } catch (ArrayIndexOutOfBoundsException e) {  // No prefix length.
        } catch (NumberFormatException e) {           // Non-numeric prefix.
        } catch (IllegalArgumentException e) {        // Invalid IP address.
        }

        if (inetAddress == null || prefixLength == -1) {
            throw new IllegalArgumentException("Bad LinkAddress params " + address);
        }

        init(inetAddress, prefixLength, flags, scope);
    }

    @Override
    public String toString() {
        return address.getHostAddress() + "/" + prefixLength;
    }

    /**
     * Compares this {@code LinkAddress} instance against the specified address
     * in {@code obj}. Two addresses are equal if their InetAddress and prefixLength
     * are equal.
     *
     * @param obj the object to be tested for equality.
     * @return {@code true} if both objects are equal, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LinkAddress)) {
            return false;
        }
        LinkAddress linkAddress = (LinkAddress) obj;
        return this.address.equals(linkAddress.address) &&
            this.prefixLength == linkAddress.prefixLength &&
            this.flags == linkAddress.flags &&
            this.scope == linkAddress.scope;
    }

    /**
     * Determines whether this {@code LinkAddress} and the provided {@code LinkAddress} represent
     * the same IP address.
     *
     * @param other the object to be tested for equality.
     * @return {@code true} if both objects have the same address and prefix length, {@code false}
     * otherwise.
     */
    public boolean isSameAddressAs(LinkAddress other) {
        return address.equals(other.address) && prefixLength == other.prefixLength;
    }

    /*
     * Returns a hashcode for this address.
     */
    @Override
    public int hashCode() {
        return address.hashCode() + 11 * prefixLength + 19 * flags + 43 * scope;
    }

    /**
     * Returns the InetAddress of this address.
     */
    public InetAddress getAddress() {
        return address;
    }

    /**
     * Returns the prefix length of this address.
     */
    public int getNetworkPrefixLength() {
        return prefixLength;
    }

    /**
     * Returns the flags of this address.
     */
    public int getFlags() {
        return flags;
    }

    /**
     * Returns the scope of this addresss.
     */
    public int getScope() {
        return scope;
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByteArray(address.getAddress());
        dest.writeInt(prefixLength);
        dest.writeInt(this.flags);
        dest.writeInt(scope);
    }

    /**
     * Implement the Parcelable interface.
     * @hide
     */
    public static final Creator<LinkAddress> CREATOR =
        new Creator<LinkAddress>() {
            public LinkAddress createFromParcel(Parcel in) {
                InetAddress address = null;
                try {
                    address = InetAddress.getByAddress(in.createByteArray());
                } catch (UnknownHostException e) {
                    // Nothing we can do here. When we call the constructor, we'll throw an
                    // IllegalArgumentException, because a LinkAddress can't have a null
                    // InetAddress.
                }
                int prefixLength = in.readInt();
                int flags = in.readInt();
                int scope = in.readInt();
                return new LinkAddress(address, prefixLength, flags, scope);
            }

            public LinkAddress[] newArray(int size) {
                return new LinkAddress[size];
            }
        };
}
