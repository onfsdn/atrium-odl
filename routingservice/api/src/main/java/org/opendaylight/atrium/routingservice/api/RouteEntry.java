/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.api;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;

import com.google.common.base.MoreObjects;

/**
 * Represents a route entry for an IP prefix.
 */
public class RouteEntry {
    private final AtriumIpPrefix prefix;              // The IP prefix
    private final AtriumIpAddress nextHop;            // Next-hop IP address

    /**
     * Class constructor.
     *
     * @param prefix the IP prefix of the route
     * @param nextHop the next hop IP address for the route
     */
    public RouteEntry(AtriumIpPrefix prefix, AtriumIpAddress nextHop) {
        this.prefix = checkNotNull(prefix);
        this.nextHop = checkNotNull(nextHop);
    }

    /**
     * Returns the IP version of the route.
     *
     * @return the IP version of the route
     */
    public AtriumIpAddress.Version version() {
        return nextHop.version();
    }

    /**
     * Tests whether the IP version of this address is IPv4.
     *
     * @return true if the IP version of this address is IPv4, otherwise false.
     */
    public boolean isIp4() {
        return nextHop.isIp4();
    }

    /**
     * Tests whether the IP version of this address is IPv6.
     *
     * @return true if the IP version of this address is IPv6, otherwise false.
     */
    public boolean isIp6() {
        return nextHop.isIp6();
    }

    /**
     * Returns the IP prefix of the route.
     *
     * @return the IP prefix of the route
     */
    public AtriumIpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the next hop IP address for the route.
     *
     * @return the next hop IP address for the route
     */
    public AtriumIpAddress nextHop() {
        return nextHop;
    }

    /**
     * Creates the binary string representation of an IP prefix.
     * The prefix can be either IPv4 or IPv6.
     * The string length is equal to the prefix length.
     *
     * @param ipPrefix the IP prefix to use
     * @return the binary string representation
     */
    public static String createBinaryString(AtriumIpPrefix ipPrefix) {
        if (ipPrefix.prefixLength() == 0) {
            return "";
        }

        byte[] octets = ipPrefix.address().toOctets();
        StringBuilder result = new StringBuilder(ipPrefix.prefixLength());
        for (int i = 0; i < ipPrefix.prefixLength(); i++) {
            int byteOffset = i / Byte.SIZE;
            int bitOffset = i % Byte.SIZE;
            int mask = 1 << (Byte.SIZE - 1 - bitOffset);
            byte value = octets[byteOffset];
            boolean isSet = ((value & mask) != 0);
            result.append(isSet ? "1" : "0");
        }
        return result.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        //
        // NOTE: Subclasses are considered as change of identity, hence
        // equals() will return false if the class type doesn't match.
        //
        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        RouteEntry otherRoute = (RouteEntry) other;
        return Objects.equals(this.prefix, otherRoute.prefix) &&
            Objects.equals(this.nextHop, otherRoute.nextHop);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHop);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("prefix", prefix)
            .add("nextHop", nextHop)
            .toString();
    }
}
