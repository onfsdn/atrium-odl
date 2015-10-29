package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.impl.rev150725;

import org.opendaylight.atrium.routingservice.bgp.api.BgpService;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.routingservice.impl.Router;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        BgpService bgpService = getBgpserviceDependency();
        HostService hostService = getHostserviceDependency();
        Router router = new Router();
        router.setServices(routingConfigService, bgpService,hostService);

        getBrokerDependency().registerProvider(router);

        return router;
    }

}
