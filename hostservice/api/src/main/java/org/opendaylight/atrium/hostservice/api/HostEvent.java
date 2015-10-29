/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;


// TODO: Auto-generated Javadoc
/**
 * The Class HostEvent.
 */
public class HostEvent {
    
    /** The type. */
    private Type type;
    
    /** The host. */
    private Host host;
    
    /**
     * Type of host events.
     */
    public enum Type {
        /**
         * Signifies that a new host has been detected.
         */
        HOST_ADDED,

        /**
         * Signifies that a host has been removed.
         */
        HOST_REMOVED,

        /**
         * Signifies that host data changed, e.g. IP address
         */
        HOST_UPDATED,

        /**
         * Signifies that a host location has changed.
         */
        HOST_MOVED
    }
    
    /**
     * Creates an event of a given type and for the specified host and the
     * current time.
     *
     * @param type host event type
     * @param host event host subject
     */
    public HostEvent(Type type, Host host) {
        this.type = type;
        this.host = host;
    }
    
    /**
     * Gets the type.
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }
    
    /**
     * Gets the host.
     *
     * @return the host
     */
    public Host getHost() {
        return host;
    }

    
}
