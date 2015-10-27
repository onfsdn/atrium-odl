/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.didm.hp3800;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.FilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.ForwardInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.NextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.hp.rev150218.Hp3800DeviceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;


/**
 * The HP 3800 OF driver does the following:
 *
 * 1. listen for node added/removed in inventory (future: filtered by device type)
 * 2. when a HP 3800 node is added, register the routed RPCs (other driver types may register as DCLs for a feature such as vlan)
 * 3. when a HP 3800 node is removed, close the RPC registration (and/or DCLs for other driver types)
 */
public class OpenFlowDeviceDriver implements OpenflowFeatureService, DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OpenFlowDeviceDriver.class);
    private static final InstanceIdentifier<DeviceType> PATH = InstanceIdentifier.builder(Nodes.class).child(Node.class).augmentation(DeviceType.class).build();
    private static final Class<? extends DeviceTypeBase> DEVICE_TYPE = Hp3800DeviceType.class;

    private final Map<InstanceIdentifier<?>, BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService>> rpcRegistrations = new HashMap<>();
    private final RpcProviderRegistry rpcRegistry;

    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    public OpenFlowDeviceDriver(DataBroker dataBroker, RpcProviderRegistry rpcRegistry) {
        this.rpcRegistry = Preconditions.checkNotNull(rpcRegistry);

     // register listener for Node, in future should be filtered by device type
     // subscribe to be notified when a device-type augmentation is applied to an inventory node
     dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, PATH, this, AsyncDataBroker.DataChangeScope.BASE);
    }

    @Override
    public Future<RpcResult<AdjustFlowOutput>> adjustFlow(AdjustFlowInput input) {
        LOG.debug("HP 3800 adjustFlow");

        // TODO: should this be a deep copy?
        List<Flow> adjustedFlows = ImmutableList.of(new FlowBuilder(input.getFlow()).build());

        // TODO: finish this method, but for now just return the same flow that was receive
        AdjustFlowOutput output = new AdjustFlowOutputBuilder().setFlow(adjustedFlows).build();
        return Futures.immediateFuture(RpcResultBuilder.success(output).build());
    }

    @Override
    public void close() throws Exception {
        if(dataChangeListenerRegistration != null) {
            dataChangeListenerRegistration.close();
            dataChangeListenerRegistration = null;
        }

        // remove any remaining RPC registrations
        for (Map.Entry<InstanceIdentifier<?>, BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService>> entry : rpcRegistrations.entrySet()) {
            entry.getValue().close();
        }
        rpcRegistrations.clear();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        // NOTE: we're ignoring updates
        Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
        if(createdData != null) {
            for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : createdData.entrySet()) {
                DeviceType deviceType = (DeviceType)entry.getValue();
                if(isHP3800DeviceType(deviceType.getDeviceType())) {
                    registerRpcService(entry.getKey().firstIdentifierOf(Node.class));
                }
            }
        }

        // TODO: are RPCs automatically removed if the node is removed?
        Set<InstanceIdentifier<?>> removedPaths = change.getRemovedPaths();
        if((removedPaths != null) && !removedPaths.isEmpty()) {
            for (InstanceIdentifier<?> removedPath : removedPaths) {
                DeviceType deviceType = (DeviceType)change.getOriginalData().get(removedPath);
                if(isHP3800DeviceType(deviceType.getDeviceType())) {
                    closeRpcRegistration(removedPath.firstIdentifierOf(Node.class));
                }
            }
        }
    }

    private static boolean isHP3800DeviceType(Class<? extends DeviceTypeBase> deviceType) {
        return deviceType == DEVICE_TYPE;
    }

    private void registerRpcService(InstanceIdentifier<Node> path) {
        if(!rpcRegistrations.containsKey(path)) {
            try {
                BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService> registration = rpcRegistry.addRoutedRpcImplementation(OpenflowFeatureService.class, this);
                registration.registerPath(NodeContext.class, path);
                rpcRegistrations.put(path, registration);
            } catch (IllegalStateException e) {
                LOG.error("Failed to register RPC for node: {}", path, e);
            }
        }
    }

    private void closeRpcRegistration(InstanceIdentifier<Node> path) {
        if(rpcRegistrations.containsKey(path)) {
            rpcRegistrations.remove(path).close();
        }
    }

	@Override
	public Future<RpcResult<Void>> filter(FilterInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<Void>> forward(ForwardInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<Void>> next(NextInput input) {
		// TODO Auto-generated method stub
		return null;
	}

}
