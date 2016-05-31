/*
 * Copyright (c) 2016 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import static org.mockito.Mockito.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.hostservice.api.ArpMessageAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class ArpSenderTest {

	@Mock
	private PacketProcessingService packetProcessingService;
	
	private ArpSender arpSender;
	
	@Test
	public void testSendArp() {
		/*arpSender = new ArpSender(packetProcessingService);
		
		Ipv4Address srcIpv4Address = Ipv4Address.getDefaultInstance("192.168.10.101");
		Ipv4Address dstIpv4Address = Ipv4Address.getDefaultInstance("192.168.20.1");
		MacAddress macAddress = new MacAddress("aa:bb:cc:dd:ee:ff");
		ArpMessageAddress senderAddress = new ArpMessageAddress(macAddress, srcIpv4Address);
		
		InstanceIdentifier<NodeConnector> egressNc = mock(InstanceIdentifier.class);
		//InstanceIdentifier<NodeConnector> egressNc = InstanceIdentifier.builder(InstanceIdentifier<NodeConnector>)getClass();
		InstanceIdentifier<DataObject> instance = mock(InstanceIdentifier.class);
		
		when(egressNc.firstIdentifierOf(DataObject.class)).thenReturn(instance);
		
		arpSender.sendArp(senderAddress, dstIpv4Address, egressNc);*/
	}
}
