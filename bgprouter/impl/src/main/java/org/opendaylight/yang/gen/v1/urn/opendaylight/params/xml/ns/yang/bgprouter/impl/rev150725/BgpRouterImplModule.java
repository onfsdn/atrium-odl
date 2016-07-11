/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgprouter.impl.rev150725;

import org.opendaylight.atrium.bgprouter.impl.Bgprouter;
import org.opendaylight.atrium.bgprouter.impl.TunnellingConnectivityManager;
import org.opendaylight.atrium.routingservice.api.RoutingService;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.AtriumFlowObjectiveService;

/**
 * The Class BgpRouterImplModule.
 */
public class BgpRouterImplModule extends
org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgprouter.impl.rev150725.AbstractBgpRouterImplModule {

    /**
     * Instantiates a new bgp router impl module.
     *
     * @param identifier the identifier
     * @param dependencyResolver the dependency resolver
     */
    public BgpRouterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    /**
     * Instantiates a new bgp router impl module.
     *
     * @param identifier the identifier
     * @param dependencyResolver the dependency resolver
     * @param oldModule the old module
     * @param oldInstance the old instance
     */
    public BgpRouterImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgprouter.impl.rev150725.BgpRouterImplModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    /* (non-Javadoc)
     * @see org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgprouter.impl.rev150725.AbstractBgpRouterImplModule#customValidation()
     */
    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    /* (non-Javadoc)
     * @see org.opendaylight.controller.config.spi.AbstractModule#createInstance()
     */
    @Override
    public java.lang.AutoCloseable createInstance() {
        // DataBroker initialization
        DataBroker dataService = getDataBrokerDependency();
        RpcProviderRegistry rpcService = getRpcRegistryDependency();


        // PacketProcessingService initialization for sending packet_outs
        PacketProcessingService packetProcessingService = rpcService
                .<PacketProcessingService> getRpcService(PacketProcessingService.class);


        // FlowObjective Service
        AtriumFlowObjectiveService flowObjectivesService = rpcService
                .<AtriumFlowObjectiveService> getRpcService(AtriumFlowObjectiveService.class);

        // RoutingConfigService initialization
        RoutingConfigService routingConfigService = this.getRoutingconfigDependency();
        RoutingService routingService = getRoutingserviceDependency();

        TunnellingConnectivityManager connectivityManager = new TunnellingConnectivityManager(dataService,
                routingConfigService, packetProcessingService, flowObjectivesService);

        // BGPRouter instantiation
        Bgprouter bgpRouter = new Bgprouter(connectivityManager, dataService, routingConfigService, routingService,
                packetProcessingService, flowObjectivesService);

        getNotificationServiceDependency().registerNotificationListener(connectivityManager);
        getBrokerDependency().registerConsumer(bgpRouter);

        // Start BGPRouter
        bgpRouter.start();

        return bgpRouter;
    }

}
