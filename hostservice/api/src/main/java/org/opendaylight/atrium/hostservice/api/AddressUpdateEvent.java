/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;

public class AddressUpdateEvent {
    ConnectorAddress address;
    public AddressUpdateEvent(ConnectorAddress address) {
        this.address = address;
    }
    
    public ConnectorAddress getAddress() {
        return address;
    }
}

