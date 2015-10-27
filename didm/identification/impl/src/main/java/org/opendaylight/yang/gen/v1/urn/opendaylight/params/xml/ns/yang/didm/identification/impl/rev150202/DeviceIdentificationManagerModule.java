/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.impl.rev150202;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceIdentificationManagerModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.impl.rev150202.AbstractDeviceIdentificationManagerModule {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceIdentificationManagerModule.class);

    public DeviceIdentificationManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public DeviceIdentificationManagerModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, DeviceIdentificationManagerModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {}

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.trace("Creating DeviceIdentificationManager instance");
        return new org.opendaylight.didm.identification.impl.DeviceIdentificationManager(getDataBrokerDependency(), getRpcRegistryDependency());
    }

}
