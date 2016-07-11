/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.After;
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
import org.opendaylight.atrium.bgprouter.impl.Bgprouter;
import org.opendaylight.atrium.bgprouter.impl.TunnellingConnectivityManager;
import org.opendaylight.atrium.routingservice.api.RoutingService;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.FilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.ForwardInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.AtriumFlowObjectiveService;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;

/**
 * This class tests adding a control and data switch.
 */
@RunWith(MockitoJUnitRunner.class)
public class BgprouterTest {

	@Mock
	private TunnellingConnectivityManager connectivityManager;
	@Mock
	private DataBroker dataBroker;
	@Mock
	private RoutingConfigService routingConfigService;
	@Mock
	private RoutingService routingService;
	@Mock
	private PacketProcessingService packetService;
	@Mock
	private AtriumFlowObjectiveService flowObjectives;

	private Bgprouter bgpRouter;

	/**
	 * Instantiates bgp router
	 */
	private void setupBgpRouter() throws InterruptedException, ExecutionException {
		ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
		Optional<Nodes> nodes = Optional.of(mock(Nodes.class));
		CheckedFuture<Optional<Nodes>, ReadFailedException> checkedNodes = mock(CheckedFuture.class);

		when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);
		when(readOnlyTransaction.read(any(LogicalDatastoreType.class),
				(InstanceIdentifier<Nodes>) any(InstanceIdentifier.class))).thenReturn(checkedNodes);
		when(checkedNodes.get()).thenReturn(nodes);

		bgpRouter = new Bgprouter(connectivityManager, dataBroker, routingConfigService, routingService, packetService,
				flowObjectives);
	}

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
		when(bgp.getAttachmentDpId()).thenReturn(NodeId.getDefaultInstance("00:00:00:00:00:00:00:bb"));

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
	 * Starts the bgp router
	 */
	@Before
	public void init() throws InterruptedException, ExecutionException {
		setupBgpRouter();
		bgpRouter.onSessionInitialized(mock(ConsumerContext.class));
		setupBgpSpeakers();
		bgpRouter.start();
	}

	/**
	 * Tests adding a control plane switch
	 */
	@Test
	public void testProcessControlSwitchAdd() {
		NodeId dpnId = NodeId.getDefaultInstance("20");
		bgpRouter.processNodeAdd(dpnId);

		verify(flowObjectives, atLeast(1)).forward(any(ForwardInput.class));
	}

	/**
	 * Tests adding a data plane switch
	 */
	@Test
	public void testProcessDeviceAdd() {
		NodeId dpnId = NodeId.getDefaultInstance("openflow:20");
		bgpRouter.processNodeAdd(dpnId);

		verify(flowObjectives, times(1)).filter(any(FilterInput.class));
		verify(flowObjectives, times(1)).forward(any(ForwardInput.class));
	}

	/**
	 * Stops the bgp router
	 */
	@After
	public void destroy() throws Exception {
		bgpRouter.close();
	}

}
