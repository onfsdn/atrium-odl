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


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.opendaylight.atrium.util.AtriumIpAddress;

import com.google.common.base.MoreObjects;

/**
 * Identifier for a next hop group.
 */
public class AtriumNextHopGroupKey {

    private final AtriumIpAddress address;

    /**
     * Creates a new next hop group key.
     *
     * @param address next hop's IP address
     */
    public AtriumNextHopGroupKey(AtriumIpAddress address) {
        this.address = checkNotNull(address);
    }

    /**
     * Returns the next hop's IP address.
     *
     * @return next hop's IP address
     */
    public AtriumIpAddress address() {
        return address;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AtriumNextHopGroupKey)) {
            return false;
        }

        AtriumNextHopGroupKey that = (AtriumNextHopGroupKey) o;

        return Objects.equals(this.address, that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .toString();
    }
}