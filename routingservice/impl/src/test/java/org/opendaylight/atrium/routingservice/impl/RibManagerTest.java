/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.impl;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.hostservice.api.Host;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.routingservice.api.FibListener;
import org.opendaylight.atrium.routingservice.api.RouteEntry;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification.ModificationType;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.Attributes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddressBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;

/**
 * This class tests adding a route, updating a route, deleting a route, and
 * adding a route whose next hop is the local BGP speaker. The HostService
 * answers requests synchronously.
 */
@RunWith(MockitoJUnitRunner.class)
public class RibManagerTest {

	@Mock
	private DataBroker dataBroker;
	@Mock
	private RibReference ribReference;
	@Mock
	private HostService hostService;
	@Mock
	private RoutingConfigService routingConfigService;
	@Mock
	private FibListener fibListener;

	private RibManager ribManager;

	@Before
	public void setUp() throws Exception {
		setupHostService();
		ribManager = new RibManager(dataBroker, ribReference, hostService, routingConfigService);
		ribManager.onSessionInitiated(mock(ProviderContext.class));
		ribManager.addFibListener(fibListener);
	}

	/**
	 * Sets up the host service with details of some hosts.
	 */
	private void setupHostService() {
		IpAddress host1Address = new IpAddress(Ipv4Address.getDefaultInstance("192.168.10.1"));
		long now = new Date().getTime();
		ConnectorAddress ipv4Address = new ConnectorAddressBuilder().setLastSeen(now).setFirstSeen(now)
				.setMac(new MacAddress("aa:bb:cc:dd:ee:ff")).setIp(host1Address).build();
		NodeConnector nc1 = new NodeConnectorBuilder().setKey(new NodeConnectorKey(new NodeConnectorId("1"))).build();
		Host host1 = new Host(ipv4Address, nc1);
		when(hostService.getHost(new HostId("192.168.10.1"))).thenReturn(host1);

		IpAddress host2Address = new IpAddress(Ipv6Address.getDefaultInstance("2000::1"));
		long time = new Date().getTime();
		ConnectorAddress ipv6Address = new ConnectorAddressBuilder().setLastSeen(time).setFirstSeen(time)
				.setMac(new MacAddress("aa:bb:cc:dd:ee:00")).setIp(host2Address).build();
		NodeConnector nc2 = new NodeConnectorBuilder().setKey(new NodeConnectorKey(new NodeConnectorId("2"))).build();
		Host host2 = new Host(ipv6Address, nc2);
		when(hostService.getHost(new HostId("2000::1"))).thenReturn(host2);
	}
	
	public DataTreeModification getRouteUpdate(String prefix, String nextHopIp, ModificationType operation) {
		DataTreeModification routeUpdate = mock(DataTreeModification.class);
		
		Ipv4Prefix ipv4Prefix = new Ipv4Prefix(prefix);
		Ipv4Address ipv4Address = new Ipv4Address(nextHopIp);
		
		DataObjectModification root = mock(DataObjectModification.class);
		InstanceIdentifier instanceIdentifier = mock(InstanceIdentifier.class);
		DataTreeIdentifier treeIdentifier = new DataTreeIdentifier(LogicalDatastoreType.CONFIGURATION,
				instanceIdentifier);

		DataObject dataObject = mock(DataObject.class);
		Ipv4Route route = mock(Ipv4Route.class);
		Attributes attributes = mock(Attributes.class);
		Ipv4NextHopCase nhc = mock(Ipv4NextHopCase.class);
		Ipv4NextHop ipv4NextHop = mock(Ipv4NextHop.class);

		when(routeUpdate.getRootNode()).thenReturn(root);
		when(root.getModificationType()).thenReturn(operation);
		when(root.getDataAfter()).thenReturn(route);
		when(route.getAttributes()).thenReturn(attributes);
		when(attributes.getCNextHop()).thenReturn(nhc);
		when(nhc.getIpv4NextHop()).thenReturn(ipv4NextHop);
		when(ipv4NextHop.getGlobal()).thenReturn(ipv4Address);
		when(route.getPrefix()).thenReturn(ipv4Prefix);
		when(routeUpdate.getRootPath()).thenReturn(treeIdentifier);
		
		return routeUpdate;
	}

	/**
	 * Tests adding a IPv4 route entry.
	 */
	@Test
	public void testIpv4RouteAdd() {
		String nextHopIp = "192.168.10.1";
		String prefix = "1.1.1.0/24";

		AtriumIpPrefix ipv4Prefix = AtriumIpPrefix.valueOf(prefix);
		AtriumIpAddress ipv4Address = AtriumIpAddress.valueOf(nextHopIp);
		RouteEntry routeEntry = new RouteEntry(ipv4Prefix, ipv4Address);

		DataTreeModification routeUpdate = getRouteUpdate(prefix, nextHopIp, ModificationType.WRITE);

		ribManager.processRouteUpdates(routeUpdate);

		assertEquals(1, ribManager.getRoutes4().size());
		assertTrue(ribManager.getRoutes4().contains(routeEntry));
		verify(fibListener, times(1)).update(anyObject(), anyObject());
	}

	/**
	 * Tests updating a IPv4 route entry.
	 */
	@Test
	public void testIpv4RouteUpdate() {
		// Firstly add a route
		testIpv4RouteAdd();

		String nextHopIp = "192.168.20.1";
		String prefix = "1.1.1.0/24";

		AtriumIpPrefix ipv4Prefix = AtriumIpPrefix.valueOf(prefix);
		AtriumIpAddress ipv4Address = AtriumIpAddress.valueOf(nextHopIp);
		RouteEntry routeEntry = new RouteEntry(ipv4Prefix, ipv4Address);

		DataTreeModification routeUpdate = getRouteUpdate(prefix, nextHopIp, ModificationType.SUBTREE_MODIFIED);

		ribManager.processRouteUpdates(routeUpdate);

		assertEquals(1, ribManager.getRoutes4().size());
		assertTrue(ribManager.getRoutes4().contains(routeEntry));
		verify(fibListener, atLeast(1)).update(anyObject(), anyObject());
	}

	/**
	 * Tests deleting a IPv4 route entry.
	 */
	@Test
	public void testIpv4RouteDelete() {
		testIpv4RouteAdd();
		testIpv4RouteUpdate();

		String nextHopIp = "192.168.10.1";
		String prefix = "1.1.1.0/24";

		AtriumIpPrefix ipv4Prefix = AtriumIpPrefix.valueOf(prefix);
		AtriumIpAddress ipv4Address = AtriumIpAddress.valueOf(nextHopIp);
		RouteEntry routeEntry = new RouteEntry(ipv4Prefix, ipv4Address);

		DataTreeModification routeUpdate = getRouteUpdate(prefix, nextHopIp, ModificationType.DELETE);

		ribManager.processRouteUpdates(routeUpdate);

		assertFalse(ribManager.getRoutes4().contains(routeEntry));
		verify(fibListener, atLeast(1)).update(anyObject(), anyObject());
	}

	/**
	 * Tests adding a IPv4 route whose next hop is the local BGP speaker.
	 */
	@Test
	public void testIpv4LocalRouteAdd() {
		String nextHopIp = "192.168.30.1";
		String prefix = "1.1.1.0/24";

		AtriumIpPrefix ipv4Prefix = AtriumIpPrefix.valueOf(prefix);
		AtriumIpAddress ipv4Address = AtriumIpAddress.valueOf(nextHopIp);
		RouteEntry routeEntry = new RouteEntry(ipv4Prefix, ipv4Address);

		DataTreeModification routeUpdate = getRouteUpdate(prefix, nextHopIp, ModificationType.WRITE);
		
		ribManager.processRouteUpdates(routeUpdate);

		assertEquals(1, ribManager.getRoutes4().size());
		assertTrue(ribManager.getRoutes4().contains(routeEntry));
		verify(fibListener, never()).update(anyObject(), anyObject());
	}
}
