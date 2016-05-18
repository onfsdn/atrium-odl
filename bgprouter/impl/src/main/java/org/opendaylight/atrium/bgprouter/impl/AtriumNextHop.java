/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opendaylight.atrium.bgprouter.impl;

import java.util.Objects;

import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumMacAddress;

import com.google.common.base.MoreObjects;

/**
 * Represents a next hop for routing, whose MAC address has already been resolved.
 */
public class AtriumNextHop {

    private final AtriumIpAddress ip;
    private final AtriumMacAddress mac;
    private final AtriumNextHopGroupKey group;

    /**
     * Creates a new next hop.
     *
     * @param ip next hop's IP address
     * @param mac next hop's MAC address
     * @param group next hop's group
     */
    public AtriumNextHop(AtriumIpAddress ip, AtriumMacAddress mac, AtriumNextHopGroupKey group) {
        this.ip = ip;
        this.mac = mac;
        this.group = group;
    }

    /**
     * Returns the next hop's IP address.
     *
     * @return next hop's IP address
     */
    public AtriumIpAddress ip() {
        return ip;
    }

    /**
     * Returns the next hop's MAC address.
     *
     * @return next hop's MAC address
     */
    public AtriumMacAddress mac() {
        return mac;
    }

    /**
     * Returns the next hop group.
     *
     * @return group
     */
    public AtriumNextHopGroupKey group() {
        return group;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AtriumNextHop)) {
            return false;
        }

        AtriumNextHop that = (AtriumNextHop) o;

        return Objects.equals(this.ip, that.ip) &&
                Objects.equals(this.mac, that.mac) &&
                Objects.equals(this.group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip, mac, group);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ip", ip)
                .add("mac", mac)
                .add("group", group)
                .toString();
    }
}
