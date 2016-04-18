package org.opendaylight.atrium;

import java.util.Collections;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.atriumutil.AtriumMacAddress;
import org.opendaylight.atrium.atriumutil.Ip4Address;
import org.opendaylight.atrium.atriumutil.Ip4Prefix;
import org.opendaylight.atrium.atriumutil.IpPrefix;
import org.opendaylight.atrium.hostservice.api.Host;
import org.opendaylight.atrium.hostservice.api.HostListener;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.routingservice.api.AtriumFibEntry;
import org.opendaylight.atrium.routingservice.api.AtriumFibUpdate;
import org.opendaylight.atrium.routingservice.api.FibListener;
import org.opendaylight.atrium.routingservice.bgp.api.BgpService;
import org.opendaylight.atrium.routingservice.bgp.api.RouteEntry;
import org.opendaylight.atrium.routingservice.bgp.api.RouteUpdate;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.routingservice.impl.Router;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddressBuilder;

import junit.framework.Assert;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;

@RunWith(MockitoJUnitRunner.class)
public class RouterTest {

	@Mock
	RoutingConfigService routingConfigSvc;

	@Mock
	BgpService bgpSvc;

	@Mock
	HostService hostService;

	@Mock
	FibListener fibListener;

	Router router;

	@Before
	public void setUp() throws Exception {

		setupHostService();
		router = new Router();
		router.onSessionInitiated(mock(ProviderContext.class));
		router.addFibListener(fibListener);
		router.setServices(routingConfigSvc, bgpSvc, hostService);
		router.start();
	}

	private void setupHostService() {

		IpAddress host1Address = new IpAddress(Ipv4Address.getDefaultInstance("192.168.10.1"));
		long now = new Date().getTime();
		ConnectorAddress address = new ConnectorAddressBuilder().setLastSeen(now).setFirstSeen(now)
				.setMac(new MacAddress("aa:bb:cc:dd:ee:ff")).setIp(host1Address).build();
		NodeConnector nc1 = new NodeConnectorBuilder().setKey(new NodeConnectorKey(new NodeConnectorId("1"))).build();
		Host host = new Host(address, nc1);
		when(hostService.getHost(new HostId("192.168.10.1"))).thenReturn(host);

	}
	/*
	 * @After public void tearDown() { router.stop(); }
	 */

	@Test
	public void testIpv4RouteAdd() {
		// Construct a route entry
		IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
		org.opendaylight.atrium.atriumutil.IpAddress nextHopIp = Ip4Address.valueOf("192.168.10.1");

		RouteEntry routeEntry = new RouteEntry(prefix, nextHopIp);

		/*
		 * fibListener.update(Collections.singletonList(new
		 * AtriumFibUpdate(AtriumFibUpdate.Type.UPDATE, new
		 * AtriumFibEntry(routeEntry.prefix(), routeEntry.nextHop(),
		 * AtriumMacAddress.valueOf("00:00:00:00:00:01")))),null);
		 */

		router.processRouteUpdates(Collections.singletonList(new RouteUpdate(RouteUpdate.Type.UPDATE, routeEntry)));

		verify(fibListener, atLeast(1)).update(anyObject(), anyObject());


	}

	@Test
	public void testRouteUpdate() {
		testIpv4RouteAdd();

		IpPrefix prefix = Ip4Prefix.valueOf("1.1.1.0/24");
		org.opendaylight.atrium.atriumutil.IpAddress nextHopIp = Ip4Address.valueOf("192.168.20.1");

		RouteEntry routeEntryUpdate = new RouteEntry(prefix, nextHopIp);
		RouteEntry routeEntryDelete = new RouteEntry(prefix, Ip4Address.valueOf("192.168.10.1"));
		
		AtriumFibEntry withdrawFibEntry = new AtriumFibEntry(routeEntryUpdate.prefix(), null, null);
		AtriumFibEntry updateFibEntry = new AtriumFibEntry(prefix, nextHopIp,
				AtriumMacAddress.valueOf("00:00:00:00:00:02"));
/*
		fibListener.update(Collections.singletonList(new AtriumFibUpdate(AtriumFibUpdate.Type.UPDATE, updateFibEntry)),
				Collections.singletonList(new AtriumFibUpdate(AtriumFibUpdate.Type.DELETE, withdrawFibEntry)));
*/		
		router.processRouteUpdates(Collections.singletonList(new RouteUpdate(RouteUpdate.Type.DELETE, routeEntryDelete)));
		router.processRouteUpdates(Collections.singletonList(new RouteUpdate(RouteUpdate.Type.UPDATE, routeEntryUpdate)));
		
		assertTrue(router.getRoutes4().contains(routeEntryUpdate));
		assertFalse(router.getRoutes4().contains(routeEntryDelete));
		
		verify(fibListener, atLeast(2)).update(anyObject(), anyObject());
	}
	
	
	

}
