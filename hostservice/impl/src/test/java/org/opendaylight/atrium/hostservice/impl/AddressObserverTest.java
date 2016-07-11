/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Matchers.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.sal.binding.api.NotificationService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.KnownEtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.Ipv6PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv6.rev140528.ipv6.packet.received.packet.chain.packet.Ipv6Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.class)
public class AddressObserverTest {
	@Mock
	private HostMonitor hostMonitor;
	@Mock
	private NotificationService notificationService;
	private AddressObserver addressObserver;

	@Before
	public void init() {
		addressObserver = new AddressObserver(hostMonitor, notificationService);
	}

	@Test
	public void testOnArpPacketReceived() {
		ArpPacketReceived packetReceived = mock(ArpPacketReceived.class);
		List<PacketChain> packetChains = new ArrayList<PacketChain>();

		PacketChain packetChain1 = mock(PacketChain.class);
		PacketChain packetChain2 = mock(PacketChain.class);
		PacketChain packetChain3 = mock(PacketChain.class);

		packetChains.add(packetChain1);
		packetChains.add(packetChain2);
		packetChains.add(packetChain3);

		RawPacket rawPacket = mock(RawPacket.class);
		EthernetPacket ethernetPacket = mock(EthernetPacket.class);
		ArpPacket arpPacket = mock(ArpPacket.class);

		List<Header8021q> headers = mock(ArrayList.class);
		Header8021q header = mock(Header8021q.class);
		headers.add(0, header);
		VlanId vlanId = VlanId.getDefaultInstance("4001");
		MacAddress srcMac = MacAddress.getDefaultInstance("aa:bb:cc:dd:ee:ff");
		InstanceIdentifier<NodeConnector> ncRefInstance = mock(InstanceIdentifier.class);
		NodeConnectorRef ncRef = new NodeConnectorRef(ncRefInstance);

		when(packetReceived.getPacketChain()).thenReturn(packetChains);
		when(packetChain1.getPacket()).thenReturn(rawPacket);
		when(packetChain2.getPacket()).thenReturn(ethernetPacket);
		when(packetChain3.getPacket()).thenReturn(arpPacket);
		when(arpPacket.getProtocolType()).thenReturn(KnownEtherType.Ipv4);
		when(arpPacket.getSourceProtocolAddress()).thenReturn("192.168.20.1");
		when(ethernetPacket.getEthertype()).thenReturn(KnownEtherType.VlanTagged);
		when(ethernetPacket.getHeader8021q()).thenReturn(headers);
		when(headers.get(anyInt())).thenReturn(header);
		when(header.getVlan()).thenReturn(vlanId);
		when(ethernetPacket.getSourceMac()).thenReturn(srcMac);
		when(rawPacket.getIngress()).thenReturn(ncRef);

		addressObserver.onArpPacketReceived(packetReceived);

		verify(hostMonitor, times(1)).packetReceived(any(ConnectorAddress.class), any(InstanceIdentifier.class));
	}

	@Test
	public void testOnIpv4PacketReceived() {
		Ipv4PacketReceived packetReceived = mock(Ipv4PacketReceived.class);
		List<PacketChain> packetChains = new ArrayList<PacketChain>();

		PacketChain packetChain1 = mock(PacketChain.class);
		PacketChain packetChain2 = mock(PacketChain.class);
		PacketChain packetChain3 = mock(PacketChain.class);

		packetChains.add(packetChain1);
		packetChains.add(packetChain2);
		packetChains.add(packetChain3);

		RawPacket rawPacket = mock(RawPacket.class);
		EthernetPacket ethernetPacket = mock(EthernetPacket.class);
		Ipv4Packet ipv4Packet = mock(Ipv4Packet.class);
		Ipv4Address ipv4Address = Ipv4Address.getDefaultInstance("192.168.10.1");

		List<Header8021q> headers = mock(ArrayList.class);
		Header8021q header = mock(Header8021q.class);
		headers.add(0, header);
		VlanId vlanId = VlanId.getDefaultInstance("2001");
		MacAddress srcMac = MacAddress.getDefaultInstance("aa:bb:cc:dd:ee:ff");
		InstanceIdentifier<NodeConnector> ncRefInstance = mock(InstanceIdentifier.class);

		NodeConnectorRef ncRef = new NodeConnectorRef(ncRefInstance);

		when(packetReceived.getPacketChain()).thenReturn(packetChains);
		when(packetChain1.getPacket()).thenReturn(rawPacket);
		when(packetChain2.getPacket()).thenReturn(ethernetPacket);
		when(packetChain3.getPacket()).thenReturn(ipv4Packet);
		when(ipv4Packet.getSourceIpv4()).thenReturn(ipv4Address);
		when(ethernetPacket.getEthertype()).thenReturn(KnownEtherType.VlanTagged);
		when(ethernetPacket.getHeader8021q()).thenReturn(headers);
		when(headers.get(anyInt())).thenReturn(header);
		when(header.getVlan()).thenReturn(vlanId);
		when(ethernetPacket.getSourceMac()).thenReturn(srcMac);
		when(rawPacket.getIngress()).thenReturn(ncRef);

		addressObserver.onIpv4PacketReceived(packetReceived);

		verify(hostMonitor, times(1)).packetReceived(any(ConnectorAddress.class), any(InstanceIdentifier.class));
	}

	@Test
	public void testOnIpv6PacketReceived() {
		Ipv6PacketReceived packetReceived = mock(Ipv6PacketReceived.class);
		List<PacketChain> packetChains = new ArrayList<PacketChain>();

		PacketChain packetChain1 = mock(PacketChain.class);
		PacketChain packetChain2 = mock(PacketChain.class);
		PacketChain packetChain3 = mock(PacketChain.class);

		packetChains.add(packetChain1);
		packetChains.add(packetChain2);
		packetChains.add(packetChain3);

		RawPacket rawPacket = mock(RawPacket.class);
		EthernetPacket ethernetPacket = mock(EthernetPacket.class);
		Ipv6Packet ipv6Packet = mock(Ipv6Packet.class);
		Ipv6Address ipv6Address = Ipv6Address.getDefaultInstance("2000::1");
		MacAddress srcMac = MacAddress.getDefaultInstance("aa:bb:cc:dd:ee:f0");

		List<Header8021q> headers = mock(ArrayList.class);
		Header8021q header = mock(Header8021q.class);
		headers.add(0, header);
		VlanId vlanId = VlanId.getDefaultInstance("3001");
		InstanceIdentifier<NodeConnector> ncRefInstance = mock(InstanceIdentifier.class);
		NodeConnectorRef ncRef = new NodeConnectorRef(ncRefInstance);

		when(packetReceived.getPacketChain()).thenReturn(packetChains);
		when(packetChain1.getPacket()).thenReturn(rawPacket);
		when(packetChain2.getPacket()).thenReturn(ethernetPacket);
		when(packetChain3.getPacket()).thenReturn(ipv6Packet);
		when(ipv6Packet.getSourceIpv6()).thenReturn(ipv6Address);
		when(ethernetPacket.getEthertype()).thenReturn(KnownEtherType.VlanTagged);
		when(ethernetPacket.getHeader8021q()).thenReturn(headers);
		when(headers.get(anyInt())).thenReturn(header);
		when(header.getVlan()).thenReturn(vlanId);
		when(ethernetPacket.getSourceMac()).thenReturn(srcMac);
		when(rawPacket.getIngress()).thenReturn(ncRef);

		addressObserver.onIpv6PacketReceived(packetReceived);

		verify(hostMonitor, times(1)).packetReceived(any(ConnectorAddress.class), any(InstanceIdentifier.class));
	}
}
