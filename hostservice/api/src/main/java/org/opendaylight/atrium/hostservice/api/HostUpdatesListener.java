/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;

/**
 * The listener interface for receiving hostUpdates events.
 * The class that is interested in processing a hostUpdates
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addHostUpdatesListener<code> method. When
 * the hostUpdates event occurs, that object's appropriate
 * method is invoked.
 *
 * @see HostUpdatesEvent
 */
public interface HostUpdatesListener {
    
    /**
     * Send Host added event to listeners waiting for NH resolution 
     *
     * @param event the event
     */

    public void sendHostAddEvent(HostEvent hostEvent);
    
    public void deleteHost(HostId hostId);
    
    public void addHost(HostId hostId, Host host);
}
