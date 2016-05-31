/*
 * Copyright (c) 2016 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import static org.mockito.Mockito.*;

import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.hostservice.api.ArpMessageAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class ArpSenderTest {

	@Mock
	private PacketProcessingService packetProcessingService;

	private ArpSender arpSender;

	@Before
	public void initialize() {
		arpSender = new ArpSender(packetProcessingService);
	}

	@Test
	public void testFloodArp() {
		Ipv4Address srcIpv4Address = Ipv4Address.getDefaultInstance("192.168.10.1");
		Ipv4Address dstIpv4Address = Ipv4Address.getDefaultInstance("192.168.20.1");
		MacAddress macAddress = new MacAddress("aa:bb:cc:dd:ee:ff");
		ArpMessageAddress senderAddress = new ArpMessageAddress(macAddress, srcIpv4Address);
		InstanceIdentifier<Node> instanceId = InstanceIdentifier.builder(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId("node_001"))).toInstance();

		Future<RpcResult<Void>> futureTransmitPacketResult = mock(Future.class);

		when(packetProcessingService.transmitPacket(any(TransmitPacketInput.class)))
				.thenReturn(futureTransmitPacketResult);

		arpSender.floodArp(senderAddress, dstIpv4Address, instanceId);

		verify(packetProcessingService, times(1)).transmitPacket(any(TransmitPacketInput.class));
	}

	@Test
	public void testSendArpResponse() {
		Ipv4Address srcIpv4Address = Ipv4Address.getDefaultInstance("192.168.20.1");
		Ipv4Address dstIpv4Address = Ipv4Address.getDefaultInstance("192.168.10.1");
		MacAddress srcMacAddress = new MacAddress("aa:bb:cc:dd:ee:00");
		MacAddress dstMacAddress = new MacAddress("aa:bb:cc:dd:ee:ff");

		ArpMessageAddress senderAddress = new ArpMessageAddress(srcMacAddress, srcIpv4Address);
		ArpMessageAddress receiverAddress = new ArpMessageAddress(dstMacAddress, dstIpv4Address);

		InstanceIdentifier<Node> instanceId = InstanceIdentifier.builder(Nodes.class)
				.child(Node.class, new NodeKey(new NodeId("node_001"))).toInstance();
		NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(new NodeConnectorId("node_001:0xfffffffc"));
		InstanceIdentifier<NodeConnector> egressNc = instanceId.child(NodeConnector.class, nodeConnectorKey);

		Future<RpcResult<Void>> futureTransmitPacketResult = mock(Future.class);

		when(packetProcessingService.transmitPacket(any(TransmitPacketInput.class)))
				.thenReturn(futureTransmitPacketResult);

		arpSender.sendArpResponse(senderAddress, receiverAddress, egressNc, null);

		verify(packetProcessingService, times(1)).transmitPacket(any(TransmitPacketInput.class));
	}
}
