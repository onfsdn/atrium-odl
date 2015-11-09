/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;

public class HostId {
    private int hostId;

    public HostId(String hostId) {
        this.hostId = hostId.hashCode();
        
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        HostId other = (HostId) o;

        if (this.hostId == other.getValue()) {
            return true;
        } else {
            return false;
        }
    }

    public int getValue() {
        return hostId;
    }
    
    public String toString() {
        return Integer.toString(this.hostId);
    }

    @Override
    public int hashCode() {
	return hostId;
    }

}
