/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.Addresses;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class ConfigWriter {

	public static void writeBgpConfigData(DataBroker dataBroker, Addresses arpAddresses) {
		WriteTransaction transaction = dataBroker.newWriteOnlyTransaction();
		transaction.put(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(Addresses.class).build(),
				arpAddresses);
		transaction.submit();
	}
}
