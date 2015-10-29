/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.impl.rev150725;

import org.opendaylight.atrium.hostservice.arp.ArpHandler;
import org.opendaylight.atrium.hostservice.impl.HostServiceImpl;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

public class HostServiceImplModule extends
		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.impl.rev150725.AbstractHostServiceImplModule {
	public HostServiceImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public HostServiceImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.impl.rev150725.HostServiceImplModule oldModule,
			java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void customValidation() {
		// add custom validation form module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		HostServiceImpl hostServiceImpl = new HostServiceImpl();
		DataBroker dataService = this.getDataBrokerDependency();
		RoutingConfigService configService = this.getRoutingconfigDependency();
		RpcProviderRegistry rpcService = getRpcRegistryDependency();
                NotificationProviderService notificationService = getNotificationServiceDependency();

		PacketProcessingService packetProcessingService = rpcService
				.<PacketProcessingService> getRpcService(PacketProcessingService.class);
		hostServiceImpl.setServices(dataService, configService, packetProcessingService,notificationService);

		ArpHandler arpHandler = new ArpHandler(dataService, packetProcessingService,hostServiceImpl);
		arpHandler.readConfiguration();

		getBrokerDependency().registerProvider(hostServiceImpl);




		getNotificationServiceDependency().registerNotificationListener(arpHandler);

		return hostServiceImpl;
	}

}
