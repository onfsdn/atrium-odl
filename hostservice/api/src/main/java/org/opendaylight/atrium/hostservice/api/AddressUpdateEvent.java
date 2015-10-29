/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;

public class AddressUpdateEvent {
    Addresses address;
    public AddressUpdateEvent(Addresses address) {
        this.address = address;
    }
    
    public Addresses getAddress() {
        return address;
    }
}

