/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.api;

import java.util.Collection;

import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumMacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;

/**
 * Provides a way of interacting with the RIB management component.
 */
public interface RoutingService {
    

    /**
     * Stops the routing service.
     */
    public void start();

    /**
     * Stops the routing service.
     */
    public void stop();

    /**
     * Gets all IPv4 routes
     *
     * @return the IPv4 routes
     */
    public Collection<RouteEntry> getRoutes4();


    /**
     * Adds FIB listener.
     *
     * @param fibListener listener to send FIB updates to
     */
    public void addFibListener(FibListener fibListener);

    
    
}
