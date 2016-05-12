/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.impl.rev150725;


import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.routingservice.impl.RibManager;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeService;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.Rib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.RibKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yangtools.yang.binding.KeyedInstanceIdentifier;

public class RoutingserviceImplModule
        extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.impl.rev150725.AbstractRoutingserviceImplModule {
    private static final Logger LOG = LoggerFactory
            .getLogger(RoutingserviceImplModule.class);

    public RoutingserviceImplModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public RoutingserviceImplModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.impl.rev150725.RoutingserviceImplModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Initializing Routingservice");
        RoutingConfigService routingConfigService =  getRoutingconfigDependency();
        HostService hostService = getHostserviceDependency();
        RibReference ribReference = getLocalRibDependency();
        KeyedInstanceIdentifier<Rib,RibKey> ribIID = ribReference.getInstanceIdentifier();
        DataBroker broker = getDataBrokerDependency();
        
        
        //Router router = new Router();
        //router.setServices(routingConfigService, bgpService,hostService);

        RibManager<Route> ribManager = new RibManager<Route> (broker,ribReference,hostService,routingConfigService); 
        //ribManager.start();
        
        
        
        getBrokerDependency().registerProvider(ribManager);

        return ribManager;
    }

}
