/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.atrium.routingservice.api.AtriumFibEntry;
import org.opendaylight.atrium.routingservice.api.AtriumFibUpdate;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.FibEntries;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.FibEntriesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.fibentrygrouping.FibEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.fibentrygrouping.FibEntry.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.fibentrygrouping.FibEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.routingservice.api.rev150725.fibentrygrouping.FibEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class FibDataModelWriter {

	static void updateFib(AtriumFibUpdate atriumFibUpdate, DataBroker dataBroker) {
		WriteTransaction writeTx = dataBroker.newReadWriteTransaction();

		Type fibUpdateType = null;
		if (atriumFibUpdate.type() == AtriumFibUpdate.Type.UPDATE) {
			fibUpdateType = Type.UPDATE;

		} else if (atriumFibUpdate.type() == AtriumFibUpdate.Type.DELETE) {
			fibUpdateType = Type.DELETE;
		}

		AtriumFibEntry atriumFibEntry = atriumFibUpdate.entry();
		IpAddress nextHopIp = new IpAddress(new Ipv4Address(atriumFibEntry.nextHopIp().getIp4Address().toString()));
		MacAddress nextHopMac = new MacAddress(atriumFibEntry.nextHopMac().toString());
		String ipPrefix = atriumFibEntry.prefix().getIp4Prefix().toString();

		FibEntriesBuilder fibEntriesBuilder = new FibEntriesBuilder();
		List<FibEntry> fibEntries = new ArrayList<>();

		FibEntryBuilder fibEntryBuilder = new FibEntryBuilder();
		fibEntryBuilder.setType(fibUpdateType);
		fibEntryBuilder.setNextHopIp(nextHopIp);
		fibEntryBuilder.setNextHopMac(nextHopMac);
		fibEntryBuilder.setPrefix(ipPrefix);
		fibEntries.add(fibEntryBuilder.build());

		fibEntriesBuilder.setFibEntry(fibEntries);

		writeTx.merge(LogicalDatastoreType.CONFIGURATION, InstanceIdentifier.builder(FibEntries.class).build(),
				fibEntriesBuilder.build(), true);
		writeTx.submit();
	}

	static void deleteFib(AtriumFibUpdate atriumFibUpdate, DataBroker dataBroker) {
		WriteTransaction writeTx = dataBroker.newReadWriteTransaction();

		AtriumFibEntry atriumFibEntry = atriumFibUpdate.entry();
		String ipPrefix = atriumFibEntry.prefix().getIp4Prefix().toString();

		FibEntryKey fibEntryKey = new FibEntryKey(ipPrefix);

		writeTx.delete(LogicalDatastoreType.CONFIGURATION,
				InstanceIdentifier.builder(FibEntries.class).child(FibEntry.class, fibEntryKey).build());
		writeTx.submit();
	}
}
