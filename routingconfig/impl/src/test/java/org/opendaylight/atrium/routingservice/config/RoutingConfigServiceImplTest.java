/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.config;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;

@RunWith(MockitoJUnitRunner.class)
public class RoutingConfigServiceImplTest {

	@Mock
	private DataBroker dataBroker;

	private RoutingConfigServiceImpl routingConfigService;

	@Before
	public void init() {
		routingConfigService = new RoutingConfigServiceImpl(dataBroker);
		WriteTransaction writeTx = mock(WriteTransaction.class);
		when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTx);
		routingConfigService.onSessionInitiated(mock(ProviderContext.class));
	}
	
	@Test
	public void testBgpSpeakers() {
		List<BgpSpeaker> bgpSpeakers = routingConfigService.getBgpSpeakerFromMap();
		assertNotNull(bgpSpeakers);
		assertEquals(1, bgpSpeakers.size());
	}
	
	@Test
	public void testBgpPeers() {
		List<BgpPeer> bgpPeers = routingConfigService.getBgpPeerFromMap();
		assertNotNull(bgpPeers);
		assertEquals(2, bgpPeers.size());
	}
}
