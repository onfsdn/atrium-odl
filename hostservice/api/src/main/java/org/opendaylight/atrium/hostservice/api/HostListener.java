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
 * The listener interface for receiving host events.
 * The class that is interested in processing a host
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addHostListener<code> method. When
 * the host event occurs, that object's appropriate
 * method is invoked.
 *
 * @see HostEvent
 */
public interface HostListener  {

    /**
     * Host event update.
     *
     * @param event the event
     */
    public void hostEventUpdate(HostEvent event);
    
}
