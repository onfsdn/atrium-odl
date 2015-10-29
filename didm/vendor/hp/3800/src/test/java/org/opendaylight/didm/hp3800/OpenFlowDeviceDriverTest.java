/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.didm.hp3800;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.input.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;


public class OpenFlowDeviceDriverTest {
	
	private DataBroker dataBroker= mock(DataBroker.class);
	private RpcProviderRegistry rpcRegistry=mock(RpcProviderRegistry.class);
	private OpenFlowDeviceDriver instance = new OpenFlowDeviceDriver(dataBroker, rpcRegistry);

	@Test
	public void adjustFlowTest() throws Exception {

		AdjustFlowInput adjustFlowInput = mock(AdjustFlowInput.class);
		Flow flow = mock(Flow.class);
		when(adjustFlowInput.getFlow()).thenReturn(flow);
		instance.adjustFlow(adjustFlowInput);
		assert (instance.adjustFlow(adjustFlowInput) instanceof Future);

	}
	 
    @Test
    public void closeTest() throws Exception {
      
        @SuppressWarnings("unchecked")
        ListenerRegistration<DataChangeListener> registration = mock(ListenerRegistration.class);

        when(dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataChangeListener.class), any(AsyncDataBroker.DataChangeScope.class))).thenReturn(registration);
        instance = new OpenFlowDeviceDriver(dataBroker, rpcRegistry);
           
        verify(dataBroker).registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class).child(Node.class).augmentation(DeviceType.class).build(), instance, AsyncDataBroker.DataChangeScope.BASE);

        instance.close();
        verify(registration).close();
    }


    /*
     * This is a minimum test. This test does not cover all the paths.
     * TODO: Need to add more test for this method.
     */
    @Test
    public void testOnDataChange() throws Exception{
   
    	
    	AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent = mock(AsyncDataChangeEvent.class);
    	Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<InstanceIdentifier<?>, DataObject>();
    	
		InstanceIdentifier<Node> id =  mock(InstanceIdentifier.class);
		
    	
    	DeviceType deviceType = mock(DeviceType.class);
    	createdData.put(id, deviceType);
    	
    	
    	when(asyncDataChangeEvent.getCreatedData()).thenReturn(createdData);
    	
    	instance.onDataChanged(asyncDataChangeEvent);
    	
    }
	
}
