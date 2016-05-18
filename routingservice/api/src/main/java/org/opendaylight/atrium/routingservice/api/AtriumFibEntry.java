/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.util.AtriumMacAddress;

/**
 * An entry in the Forwarding Information Base (FIB).
 */
public class AtriumFibEntry {

    private final AtriumIpPrefix prefix;
    private final AtriumIpAddress nextHopIp;
    private final AtriumMacAddress nextHopMac;

    /**
     * Creates a new FIB entry.
     *
     * @param prefix IP prefix of the FIB entry
     * @param nextHopIp IP address of the next hop
     * @param nextHopMac MAC address of the next hop
     */
    public AtriumFibEntry(AtriumIpPrefix prefix, AtriumIpAddress nextHopIp, AtriumMacAddress nextHopMac) {
        this.prefix = prefix;
        this.nextHopIp = nextHopIp;
        this.nextHopMac = nextHopMac;
    }

    /**
     * Returns the IP prefix of the FIB entry.
     *
     * @return the IP prefix
     */
    public AtriumIpPrefix prefix() {
        return prefix;
    }

    /**
     * Returns the IP address of the next hop.
     *
     * @return the IP address
     */
    public AtriumIpAddress nextHopIp() {
        return nextHopIp;
    }

    /**
     * Returns the MAC address of the next hop.
     *
     * @return the MAC address
     */
    public AtriumMacAddress nextHopMac() {
        return nextHopMac;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AtriumFibEntry)) {
            return false;
        }

        AtriumFibEntry that = (AtriumFibEntry) o;

        return Objects.equals(this.prefix, that.prefix) &&
                Objects.equals(this.nextHopIp, that.nextHopIp) &&
                Objects.equals(this.nextHopMac, that.nextHopMac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prefix, nextHopIp, nextHopMac);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("prefix", prefix)
                .add("nextHopIp", nextHopIp)
                .add("nextHopMac", nextHopMac)
                .toString();
    }
}
