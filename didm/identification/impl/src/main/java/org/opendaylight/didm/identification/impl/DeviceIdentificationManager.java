/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.identification.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.UnknownDeviceType;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;

// TODO: maintain closed state
public class DeviceIdentificationManager implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceIdentificationManager.class);
    private static final InstanceIdentifier<Node> NODE_IID = InstanceIdentifier.builder(Nodes.class).child(Node.class).build();
    private static final InstanceIdentifier<DeviceTypes> DEVICE_TYPES_IID = InstanceIdentifier.builder(DeviceTypes.class).build();
    private static final ScheduledExecutorService EXECUTORSERVICE = MoreExecutors.listeningDecorator(Executors.newScheduledThreadPool(1));
    private static final Class<? extends DeviceTypeBase> UNKNOWN_DEVICE_TYPE = UnknownDeviceType.class;

    private final DataBroker dataBroker;
    private final RpcProviderRegistry rpcProviderRegistry;
    private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

    public DeviceIdentificationManager(DataBroker dataBroker, RpcProviderRegistry rpcProviderRegistry) {
        this.dataBroker = Preconditions.checkNotNull(dataBroker);
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);

        dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, NODE_IID, this, AsyncDataBroker.DataChangeScope.BASE);
        if (dataChangeListenerRegistration == null) {
            LOG.error("Failed to register onDataChanged Listener");
        }
    }

    private List<DeviceTypeInfo> readDeviceTypeInfoFromMdsalDataStore() {
        ReadTransaction readTx = dataBroker.newReadOnlyTransaction();

        try {
            Optional<DeviceTypes> data = readTx.read(LogicalDatastoreType.CONFIGURATION, DEVICE_TYPES_IID).get();

            if (data.isPresent()) {
                DeviceTypes devTypes = data.get();
                return devTypes.getDeviceTypeInfo();
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Failed to read Device Info from data store", e);
        }
        return new ArrayList<>(0);
    }

    @Override
    public void close() throws Exception {
        if(dataChangeListenerRegistration != null) {
            LOG.error("closing onDataChanged listener registration");
            dataChangeListenerRegistration.close();
            dataChangeListenerRegistration = null;
        }
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
        handleDataCreated(change.getCreatedData());
    }

    private void handleDataCreated(Map<InstanceIdentifier<?>, DataObject> createdData) {
        Preconditions.checkNotNull(createdData);
        if(!createdData.isEmpty()) {
            // TODO: put this on a new thread, or make the algorithm below fully async
            for (Map.Entry<InstanceIdentifier<?>, DataObject> dataObjectEntry : createdData.entrySet()) {
                final InstanceIdentifier<Node> path = (InstanceIdentifier<Node>) dataObjectEntry.getKey();

                // sleep 250ms and then re-read the Node information to give OF a change to update with FlowCapable
                // TODO: Figure out why FlowCapableNode is attached later. Who adds the original Node?
                EXECUTORSERVICE.schedule(new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                        ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
                        final CheckedFuture<Optional<Node>, ReadFailedException> readFuture = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, path);
                        Futures.addCallback(readFuture, new FutureCallback<Optional<Node>>() {
                            @Override
                            public void onSuccess(Optional<Node> result) {
                                if (result.isPresent()) {
                                    identifyDevice(path, result.get());
                                } else {
                                    LOG.error("Read succeeded, node doesn't exist: {}", path);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LOG.error("Failed to read Node: {}", path, t);
                            }
                        });

                        return null;
                        }
                    }, 250, TimeUnit.MILLISECONDS);
            }
        }
    }
    
    private void checkOFMatch(final InstanceIdentifier<Node> path, Node node, FlowCapableNode flowCapableNode, List<DeviceTypeInfo> dtiInfoList ){
    	 if (flowCapableNode != null) {
             String hardware = flowCapableNode.getHardware();
             String manufacturer = flowCapableNode.getManufacturer();
             String serialNumber = flowCapableNode.getSerialNumber();
             String software = flowCapableNode.getSoftware();

             LOG.debug("Node '{}' is FlowCapable (\"{}\", \"{}\", \"{}\", \"{}\")",
                     node.getId().getValue(), hardware, manufacturer, serialNumber, software);

             // TODO: is there a more efficient way to do this?
             for(DeviceTypeInfo dti: dtiInfoList) {
                 // if the manufacturer matches and there is a h/w match
                 if (manufacturer != null && (manufacturer.equals(dti.getOpenflowManufacturer()))) {
                     List<String> hardwareValues = dti.getOpenflowHardware();
                     if(hardwareValues != null && hardwareValues.contains(hardware)) {
                             setDeviceType(dti.getDeviceType(), path);
                             return;
                     }
                 }
             }
         }

    }

    private void identifyDevice(final InstanceIdentifier<Node> path, Node node) {
        LOG.debug("Attempting to identify '{}'", node.getId().toString());

        // TODO: should we cache this and keep updated with DCL? Also, store this in a more efficient manner?
        List<DeviceTypeInfo> dtiInfoList = readDeviceTypeInfoFromMdsalDataStore();

        // 1) check for OF match
        FlowCapableNode flowCapableNode = node.getAugmentation(FlowCapableNode.class);
        checkOFMatch(path,node,flowCapableNode,dtiInfoList);
        
        /*
        // 2) check for sysOID match
        String ipStr = null;
        if(flowCapableNode != null) {
            IpAddress ip = flowCapableNode.getIpAddress();
            ipStr = ip.getIpv4Address().getValue();
        } else {
            // TODO: Write the code to get the IP address for a non-OF device.
            //       We still need to define how we get the IP address for a
            //       non-OF device
        }

        if(ipStr != null) {
            FetchSysOid fetchSysOid = new FetchSysOid(rpcProviderRegistry);
            String sysOid = fetchSysOid.fetch(ipStr);
            if (sysOid == null) {
                LOG.debug("SNMP sysOid could not be obtained for node '{}' @ {}", node.getId().getValue(), ipStr);
            } else {
                LOG.debug("Found SNMP sysOid '{}' for node '{}' @ {}", sysOid, node.getId().getValue(), ipStr);

                // TODO: is there a more efficient way to do this?
                for (DeviceTypeInfo dti : dtiInfoList) {
                    List<String> sysoidValues = dti.getSysoid();
                    if(sysoidValues != null && sysoidValues.contains(sysOid)){
                        setDeviceType(dti.getDeviceType(), path);
                        return;
                    }
                }
            }
        }

        // 3) default to unknown to trigger other devicetype DCLs to identify
        setDeviceType(UNKNOWN_DEVICE_TYPE, path);
        */
    }

    private void setDeviceType(Class<? extends DeviceTypeBase> deviceType, InstanceIdentifier<Node> path) {
        final InstanceIdentifier<DeviceType> deviceTypePath = path.augmentation(DeviceType.class);
        final WriteTransaction tx = dataBroker.newWriteOnlyTransaction();

        tx.merge(LogicalDatastoreType.OPERATIONAL, deviceTypePath, new DeviceTypeBuilder().setDeviceType(deviceType).build());

        LOG.debug("Setting node '{}' device type to '{}'", path, deviceType);
        CheckedFuture<Void, TransactionCommitFailedException> submitFuture = tx.submit();

        Futures.addCallback(submitFuture, new FutureCallback<Object>() {
            @Override
            public void onSuccess(Object result) {}

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to write DeviceType to: {}", deviceTypePath, t);
            }
        });
    }

}
