/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.api;

import static com.google.common.base.Preconditions.checkNotNull;

import org.opendaylight.atrium.routingservice.api.RouteUpdate.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;

public class RouteUpdate {
	
    private final Type type;                    // The route update type
    private final Ipv4Route routeEntry;        // The updated route entry
	
    /**
     * Specifies the type of a route update.
     * <p>
     * Route updates can either provide updated information for a route, or
     * withdraw a previously updated route.
     * </p>
     */
    public enum Type {
        /**
         * The update contains updated route information for a route.
         */
        UPDATE,
        /**
         * The update withdraws the route, meaning any previous information is
         * no longer valid.
         */
        DELETE
    }
    
    /**
     * Class constructor.
     *
     * @param type the type of the route update
     * @param routeEntry the route entry with the update
     */
    public RouteUpdate(Type type, Ipv4Route routeEntry) {
        this.type = type;
        this.routeEntry = checkNotNull(routeEntry);
    }
}
