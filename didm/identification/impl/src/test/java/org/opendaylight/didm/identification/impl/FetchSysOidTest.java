/**
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.identification.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import org.junit.Test;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.snmp.get.output.Results;
import org.opendaylight.yangtools.yang.common.RpcResult;

public class FetchSysOidTest {
    /**
     * Ensure that when RPC call fails, the fetch method returns null
     * @throws Exception
     */
	
	@Test
	public void testNullFetch() throws Exception{
		RpcProviderRegistry testRegistry = mock(RpcProviderRegistry.class);
		FetchSysOid instance = new FetchSysOid(testRegistry);
		SnmpService snmpService = mock(SnmpService.class);
		
		@SuppressWarnings("unchecked")
		RpcResult<SnmpGetOutput> rpcResult = mock(RpcResult.class);
		
		String ip = new String("15.10.11.12");
		@SuppressWarnings("unchecked")
		Future<RpcResult<SnmpGetOutput>> future = mock(Future.class);
		
		when(testRegistry.getRpcService(SnmpService.class)).thenReturn(snmpService);
		when(snmpService.snmpGet(any(SnmpGetInput.class))).thenReturn(future);
		when(future.get()).thenReturn(rpcResult);
		when(rpcResult.isSuccessful()).thenReturn(false);
		
		String out = instance.fetch(ip);
		assertEquals(out,null);
	}
	/**
     * Ensure that when RPC for SNMP returns true, the method returns the IP string.
     * @throws Exception
     */
	
	@Test
	public void testNotNullFetch() throws Exception{
		RpcProviderRegistry testRegistry = mock(RpcProviderRegistry.class);
		FetchSysOid instance = new FetchSysOid(testRegistry);
		SnmpService snmpService = mock(SnmpService.class);
		@SuppressWarnings("unchecked")
		RpcResult<SnmpGetOutput> rpcResult = mock(RpcResult.class);
		
		SnmpGetOutput output = mock(SnmpGetOutput.class);
		Results result = mock(Results.class);
		List<Results> listResults = new LinkedList<Results>();
		listResults.add(result);
		
		String ip = new String("15.10.11.12");
		@SuppressWarnings("unchecked")
		Future<RpcResult<SnmpGetOutput>> future = mock(Future.class);
		
		when(testRegistry.getRpcService(SnmpService.class)).thenReturn(snmpService);
		when(snmpService.snmpGet(any(SnmpGetInput.class))).thenReturn(future);
		when(future.get()).thenReturn(rpcResult);
		when(rpcResult.isSuccessful()).thenReturn(true);
		when(rpcResult.getResult()).thenReturn(output);
		when(output.getResults()).thenReturn(listResults);
		when(result.getValue()).thenReturn("test IP");
		
		String out = instance.fetch(ip);
		assertEquals(out,"test IP");
	}
}
