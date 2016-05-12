/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.cli;

import java.util.concurrent.ExecutionException;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.FibEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.fibentrygrouping.FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.fibentrygrouping.FibEntry.Type;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

@Command(scope = "atrium", name = "fib", description = "Display fib information")
public class FibCommand extends OsgiCommandSupport {

	@Override
	protected Object doExecute() throws Exception {

		DataBroker dataBroker = AtriumCli.getDataBroker();
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<FibEntries> instanceIdentifier = InstanceIdentifier.builder(FibEntries.class).build();

		FibEntries fibEntries = null;
		try {
			ListenableFuture<Optional<FibEntries>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, instanceIdentifier);
			Optional<FibEntries> oNT = lfONT.get();
			fibEntries = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}

		for (FibEntry fibEntry : fibEntries.getFibEntry()) {
			String prefix = fibEntry.getPrefix();
			Type type = fibEntry.getType();
			IpAddress ipAddress = fibEntry.getNextHopIp();
			MacAddress macAddress = fibEntry.getNextHopMac();

			System.out.println("Type : " + type + "\tNext Hop Ip : " + ipAddress.getIpv4Address().getValue()
					+ "\tPrefix : " + prefix + "\tNext Hop Mac : " + macAddress);
		}

		return null;
	}
}
