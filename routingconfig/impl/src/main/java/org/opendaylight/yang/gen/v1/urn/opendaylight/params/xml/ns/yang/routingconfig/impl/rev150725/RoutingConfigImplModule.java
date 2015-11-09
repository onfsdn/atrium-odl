/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingconfig.impl.rev150725;

import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.routingservice.config.RoutingConfigServiceImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public class RoutingConfigImplModule extends
		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingconfig.impl.rev150725.AbstractRoutingConfigImplModule {
	public RoutingConfigImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public RoutingConfigImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingconfig.impl.rev150725.RoutingConfigImplModule oldModule,
			java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void customValidation() {
		// add custom validation form module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		DataBroker dataBroker = getDataBrokerDependency();

		RoutingConfigServiceImpl routingConfig = new RoutingConfigServiceImpl(dataBroker);
		getBrokerDependency().registerProvider(routingConfig);
		return routingConfig;
	}
}
