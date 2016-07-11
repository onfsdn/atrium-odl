/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.util.AtriumMacAddress;
import org.opendaylight.atrium.util.AtriumIp4Prefix;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.routingservice.api.AtriumFibEntry;
import org.opendaylight.atrium.routingservice.api.AtriumFibUpdate;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;

/**
 * This class tests whether the Fib entries are properly updated
 */
@RunWith(MockitoJUnitRunner.class)
public class FibDataModelWriterTest {
	@Mock
	private AtriumFibUpdate atriumFibUpdate;
	@Mock
	private DataBroker dataBroker;
	
	/**
	 * Sets up the details for fib entry
	 */
	@Before
	public void init() {
		AtriumFibEntry fibEntry = mock(AtriumFibEntry.class);
		AtriumIpAddress ipAddress = AtriumIpAddress.valueOf("192.168.10.1");
		AtriumMacAddress mac = AtriumMacAddress.valueOf("aa:bb:cc:dd:ee:f0");
		AtriumIpPrefix prefix = AtriumIp4Prefix.valueOf("1.1.1.0/24");
		
		when(atriumFibUpdate.entry()).thenReturn(fibEntry);
		when(fibEntry.nextHopIp()).thenReturn(ipAddress);
		when(fibEntry.nextHopMac()).thenReturn(mac);
		when(fibEntry.prefix()).thenReturn(prefix);
	}
	
	/**
	 * Tests updating the fib entry
	 */
	@Test
	public void testUpdateFib() {
		ReadWriteTransaction readWriteTx = mock(ReadWriteTransaction.class);
		
		when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTx);
		when(atriumFibUpdate.type()).thenReturn(AtriumFibUpdate.Type.UPDATE).
			thenReturn(AtriumFibUpdate.Type.DELETE);
		
		FibDataModelWriter.updateFib(atriumFibUpdate, dataBroker);
		FibDataModelWriter.updateFib(atriumFibUpdate, dataBroker);
		
		verify(readWriteTx, times(2)).submit();
	}
	
	/**
	 * Tests deleting the fib entry
	 */
	@Test
	public void testDeleteFib() {
		ReadWriteTransaction readWriteTx = mock(ReadWriteTransaction.class);
		
		when(dataBroker.newReadWriteTransaction()).thenReturn(readWriteTx);
		when(atriumFibUpdate.type()).thenReturn(AtriumFibUpdate.Type.DELETE);
		
		FibDataModelWriter.deleteFib(atriumFibUpdate, dataBroker);
		
		verify(readWriteTx, times(1)).submit();
	}

}
