package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpservice.impl.rev150725;

import org.opendaylight.atrium.routingservice.bgp.BgpSessionManager;
import org.opendaylight.atrium.routingservice.bgp.api.BgpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BgpServiceImplModule
        extends
        org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpservice.impl.rev150725.AbstractBgpServiceImplModule {

    private static final Logger LOG = LoggerFactory.getLogger(BgpServiceImplModule.class);
    
    public BgpServiceImplModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public BgpServiceImplModule(
            org.opendaylight.controller.config.api.ModuleIdentifier identifier,
            org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
            org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpservice.impl.rev150725.BgpServiceImplModule oldModule,
            java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
       
	LOG.info("Initializing BgpSessionManager");
        BgpSessionManager bgpService = new BgpSessionManager();
        Long lBgpPort = getBgpPort();
        if(lBgpPort != null) {
             int iBgpPort = lBgpPort.intValue();
             bgpService.setBgpPort(iBgpPort);
        }

         // Start Session Manager 
        getBrokerDependency().registerProvider(bgpService);

        return bgpService;

    }

}
