/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.mockito.Mockito.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.util.AtriumInterface;
import org.opendaylight.atrium.util.AtriumInterfaceIpAddress;
import org.opendaylight.atrium.util.AtriumIp4Prefix;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.util.AtriumVlanId;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.ForwardInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.AtriumFlowObjectiveService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

import io.netty.util.concurrent.Future;

/**
 * This class tests if a notification is sent on the addition of a new switch
 * and if the ipv4 packets are transmitted.
 */
@RunWith(MockitoJUnitRunner.class)
public class TunnellingConnectivityManagerTest {

	@Mock
	private DataBroker dataBroker;
	@Mock
	private RoutingConfigService routingConfigService;
	@Mock
	private PacketProcessingService packetService;
	@Mock
	private AtriumFlowObjectiveService flowObjectives;

	private TunnellingConnectivityManager connectivityManager;

	/**
	 * Sets up the details of bgp speakers
	 */
	private void setupBgpSpeakers() {
		BgpSpeakers bgps = mock(BgpSpeakers.class);
		BgpSpeaker bgp = mock(BgpSpeaker.class);
		InterfaceAddresses intfAddress = mock(InterfaceAddresses.class);

		NodeConnectorId ncId = new NodeConnectorId("openflow:20:17");
		List<InterfaceAddresses> interfaceAddresses = new ArrayList<InterfaceAddresses>();
		interfaceAddresses.add(intfAddress);

		when(bgp.getInterfaceAddresses()).thenReturn(interfaceAddresses);
		when(intfAddress.getOfPortId()).thenReturn(ncId);
		when(intfAddress.getIpAddress()).thenReturn(new IpAddress(Ipv4Address.getDefaultInstance("192.168.10.1")));
		when(bgp.getAttachmentDpId()).thenReturn(NodeId.getDefaultInstance("20"));
		when(bgp.getAttachmentPort()).thenReturn(Long.valueOf(17));

		List<BgpSpeaker> bgpSpeakers = new ArrayList<BgpSpeaker>();
		bgpSpeakers.add(bgp);

		NodeConnector connectPoint = mock(NodeConnector.class);

		when(connectPoint.getId()).thenReturn(ncId);

		Set<AtriumInterfaceIpAddress> ipAddresses = new HashSet<AtriumInterfaceIpAddress>();

		AtriumIpAddress ipAddress = AtriumIpAddress.valueOf("192.168.10.1");
		AtriumIpPrefix prefix = AtriumIp4Prefix.valueOf("1.1.1.0/24");
		AtriumInterfaceIpAddress intfIp = new AtriumInterfaceIpAddress(ipAddress, prefix);
		ipAddresses.add(intfIp);

		MacAddress mac = new MacAddress("aa:bb:cc:dd:ee:0f");
		AtriumVlanId vlanId = mock(AtriumVlanId.class);

		AtriumInterface intf = new AtriumInterface(connectPoint, ipAddresses, mac, vlanId);

		Set<AtriumInterface> interfaces = new HashSet<AtriumInterface>();
		interfaces.add(intf);

		when(routingConfigService.getBgpSpeakers()).thenReturn(bgps);
		when(bgps.getBgpSpeaker()).thenReturn(bgpSpeakers);
		when(routingConfigService.getInterfaces()).thenReturn(interfaces);
	}

	/**
	 * Instantiates the Tunnelling Connectivity Manager
	 */
	@Before
	public void init() {
		setupBgpSpeakers();
		connectivityManager = new TunnellingConnectivityManager(dataBroker, routingConfigService, packetService,
				flowObjectives);
	}

	/**
	 * Tests sending notification on switch addition
	 */
	@Test
	public void testNotifySwitchAvailable() {
		connectivityManager.notifySwitchAvailable();
		verify(flowObjectives, times(2)).forward(any(ForwardInput.class));
	}

	/**
	 * Tests if the ipv4 packets received are transmitted to the switch
	 */
	@Test
	public void testOnIpv4PacketReceived() throws InterruptedException, ExecutionException {
		Ipv4PacketReceived packetReceived = mock(Ipv4PacketReceived.class);
		List<PacketChain> packetChains = new ArrayList<PacketChain>();

		PacketChain packetChain1 = mock(PacketChain.class);
		PacketChain packetChain2 = mock(PacketChain.class);
		PacketChain packetChain3 = mock(PacketChain.class);

		RawPacket rawPacket = mock(RawPacket.class);
		EthernetPacket ethernetPacket = mock(EthernetPacket.class);
		Ipv4Packet ipv4Packet = mock(Ipv4Packet.class);

		packetChains.add(packetChain1);
		packetChains.add(packetChain2);
		packetChains.add(packetChain3);

		List<Header8021q> headers = mock(ArrayList.class);
		byte[] payload = new byte[100];

		for (byte index = 1; payload.length <= index; index++) {
			payload[index] = index;
		}

		NodeConnectorRef ncRef = mock(NodeConnectorRef.class);
		ReadOnlyTransaction readTx = mock(ReadOnlyTransaction.class);
		CheckedFuture<Optional<NodeConnector>, ReadFailedException> checkedNodes = mock(CheckedFuture.class);
		Optional<NodeConnector> nc = mock(Optional.class);
		NodeConnector nodeConnector = mock(NodeConnector.class);

		NodeConnectorId ncId = new NodeConnectorId("openflow:20:17");
		Ipv4Address destIpAddress = new Ipv4Address("192.168.10.1");
		BgpPeer bgpPeer = mock(BgpPeer.class);

		Future<RpcResult<Void>> future = mock(Future.class);

		when(packetReceived.getPacketChain()).thenReturn(packetChains);
		when(packetChain1.getPacket()).thenReturn(rawPacket);
		when(packetChain2.getPacket()).thenReturn(ethernetPacket);
		when(packetChain3.getPacket()).thenReturn(ipv4Packet);

		when(ipv4Packet.getProtocol()).thenReturn(KnownIpProtocols.Icmp);
		when(ethernetPacket.getHeader8021q()).thenReturn(headers);
		when(packetReceived.getPayload()).thenReturn(payload);
		when(rawPacket.getIngress()).thenReturn(ncRef);

		when(dataBroker.newReadOnlyTransaction()).thenReturn(readTx);
		when(readTx.read(any(LogicalDatastoreType.class),
				(InstanceIdentifier<NodeConnector>) any(InstanceIdentifier.class))).thenReturn(checkedNodes);
		when(checkedNodes.get()).thenReturn(nc);
		when(nc.isPresent()).thenReturn(Boolean.valueOf(true));
		when(nc.get()).thenReturn(nodeConnector);
		when(nodeConnector.getId()).thenReturn(ncId);
		when(ipv4Packet.getDestinationIpv4()).thenReturn(destIpAddress);

		when(routingConfigService.getBgpPeerByIpAddress(any(IpAddress.class))).thenReturn(bgpPeer);
		when(bgpPeer.getPeerDpId()).thenReturn(NodeId.getDefaultInstance("21"));
		when(bgpPeer.getPeerPort()).thenReturn(Long.valueOf(18));
		when(packetService.transmitPacket(any(TransmitPacketInput.class))).thenReturn(future);

		connectivityManager.onIpv4PacketReceived(packetReceived);

		verify(packetService, times(1)).transmitPacket(any(TransmitPacketInput.class));
	}
}
