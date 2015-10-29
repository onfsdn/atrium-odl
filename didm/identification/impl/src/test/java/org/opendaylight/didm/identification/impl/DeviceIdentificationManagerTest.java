/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.identification.impl;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.UnknownDeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.snmp.get.output.ResultsBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DeviceIdentificationManagerTest {
    private static final String GOOD_HARDWARE = "good-hardware";
    private static final String GOOD_MANUFACTURER = "good-manufacturer";
    private static final String BAD_HARDWARE = "bad-hardware";
    private static final String BAD_MANUFACTURER = "bad-manufacturer";

    /**
     * Ensures that calling close on the mgr closes the DCL registration
     * @throws Exception
     */
    @Test
    public void closeTest() throws Exception {
        DataBroker dataBroker = mock(DataBroker.class);

        @SuppressWarnings("unchecked")
        ListenerRegistration<DataChangeListener> registration = mock(ListenerRegistration.class);

        when(dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataChangeListener.class), any(AsyncDataBroker.DataChangeScope.class))).thenReturn(registration);

        DeviceIdentificationManager dim = new DeviceIdentificationManager(dataBroker, mock(RpcProviderRegistry.class));
        verify(dataBroker).registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, InstanceIdentifier.builder(Nodes.class).child(Node.class).build(), dim, AsyncDataBroker.DataChangeScope.BASE);

        dim.close();
        verify(registration).close();
    }

    private class TestOfDeviceType extends DeviceTypeBase {}
    private class TestSnmpDeviceType extends DeviceTypeBase {}

    /**
     * Test identification of an OF device
     * @throws Exception
     */
    @Test
    public void ofDeviceTest() throws Exception {
        final Class<? extends DeviceTypeBase> expectedDeviceType = TestOfDeviceType.class;

        DeviceTypeInfo dti = new DeviceTypeInfoBuilder()
                .setDeviceType(expectedDeviceType)
                .setKey(new DeviceTypeInfoKey(expectedDeviceType))
                .setOpenflowHardware(Arrays.asList(GOOD_HARDWARE))
                .setOpenflowManufacturer(GOOD_MANUFACTURER)
                .setSysoid(ImmutableList.<String>of())
                .build();
        DeviceTypes deviceTypes = new DeviceTypesBuilder()
                .setDeviceTypeInfo(Arrays.asList(dti))
                .build();

        NodeId nodeId = new NodeId("of-node");
        NodeKey key = new NodeKey(nodeId);

        // OF extension - add FlowCapable aug
        FlowCapableNode flowCapableNode = new FlowCapableNodeBuilder()
                .setHardware(GOOD_HARDWARE)
                .setManufacturer(GOOD_MANUFACTURER)
                .setIpAddress(new IpAddress(Ipv4Address.getDefaultInstance("1.2.3.4")))
                .build();
        Node node = new NodeBuilder().setKey(key).setId(nodeId).addAugmentation(FlowCapableNode.class, flowCapableNode).build();

        testNewNode(deviceTypes, node, null, expectedDeviceType);
    }

    /**
     * Test identification of a SNMP device
     * @throws Exception
     */
    @Test
    public void snmpDeviceTest() throws Exception {
        final Class<? extends DeviceTypeBase> expectedDeviceType = TestSnmpDeviceType.class;
        final String sysOidValue = "test-value";

        DeviceTypeInfo dti = new DeviceTypeInfoBuilder()
                .setDeviceType(expectedDeviceType)
                .setKey(new DeviceTypeInfoKey(expectedDeviceType))
                .setOpenflowHardware(Arrays.asList(GOOD_HARDWARE))
                .setOpenflowManufacturer(GOOD_MANUFACTURER)
                .setSysoid(Arrays.asList(sysOidValue))
                .build();
        DeviceTypes deviceTypes = new DeviceTypesBuilder()
                .setDeviceTypeInfo(Arrays.asList(dti))
                .build();

        NodeId nodeId = new NodeId("snmp-node");
        NodeKey key = new NodeKey(nodeId);

        // OF extension - add FlowCapable aug
        FlowCapableNode flowCapableNode = new FlowCapableNodeBuilder()
                .setHardware(BAD_HARDWARE)
                .setManufacturer(BAD_MANUFACTURER)
                .setIpAddress(new IpAddress(Ipv4Address.getDefaultInstance("1.2.3.4")))
                .build();
        Node node = new NodeBuilder().setKey(key).setId(nodeId).addAugmentation(FlowCapableNode.class, flowCapableNode).build();

        SnmpService snmp = mock(SnmpService.class);
        when(snmp.snmpGet(any(SnmpGetInput.class))).thenReturn(Futures.immediateFuture(
                RpcResultBuilder.success(new SnmpGetOutputBuilder().setResults(Arrays.asList(new ResultsBuilder().setValue(sysOidValue).build())).build()).build()));

        testNewNode(deviceTypes, node, snmp, expectedDeviceType);
    }

    /**
     * Test default 'unknown' device type
     * @throws Exception
     */
    @Test
    public void unknownDeviceTest() throws Exception {
        final Class<? extends DeviceTypeBase> expectedDeviceType = UnknownDeviceType.class;

        NodeId nodeId = new NodeId("unknown-node");
        NodeKey key = new NodeKey(nodeId);

        Node node = new NodeBuilder().setKey(key).setId(nodeId).build();

        testNewNode(null, node, null, expectedDeviceType);
    }

    /**
     * Helper for testing new nodes
     *
     * @param inputDeviceTypes device type info to be made available to the mgr
     * @param inputNode node at keyedNodeId
     * @param snmp mock for SnmpService
     * @param expectedDeviceType expected device type value written to the node
     */
    private void testNewNode(DeviceTypes inputDeviceTypes, Node inputNode, SnmpService snmp, Class<? extends DeviceTypeBase> expectedDeviceType) {
        DataBroker dataBroker = mock(DataBroker.class);
        DeviceIdentificationManager dim = createDeviceIdentificationManager(dataBroker, snmp);

        // DIM will:

        // 1: read current device type info data
        ReadOnlyTransaction readOnlyTransaction = setDeviceTypeInfo(dataBroker, inputDeviceTypes);

        InstanceIdentifier<Node> keyedNodeId = InstanceIdentifier.builder(Nodes.class).child(Node.class, inputNode.getKey()).build();

        // HACK - the mgr will do a async read to give OFPlugin a chance to attach a FlowCapableNode augmentation
        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), eq((InstanceIdentifier)keyedNodeId))).thenReturn(Futures.immediateCheckedFuture(Optional.of(inputNode)));
        // END HACK

        // 2: act on a data change event
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent = createChangeEvent(inputNode, keyedNodeId);

        // 3: write the determined device type augmentation to the node
        WriteTransaction writeTransaction = createWriteTransaction(dataBroker);


        // Go! simulate a new node being added to the inventory
        dim.onDataChanged(changeEvent);

        // the 500ms timeout is due to the HACK since it runs on a separate thread.
        verify(writeTransaction, timeout(500)).merge(LogicalDatastoreType.OPERATIONAL, keyedNodeId.augmentation(DeviceType.class), new DeviceTypeBuilder().setDeviceType(expectedDeviceType).build());
        verify(writeTransaction).submit();
    }

    private DeviceIdentificationManager createDeviceIdentificationManager(DataBroker dataBroker, SnmpService snmp) {
        @SuppressWarnings("unchecked")
        ListenerRegistration<DataChangeListener> registration = mock(ListenerRegistration.class);

        RpcProviderRegistry rpcProviderRegistry = mock(RpcProviderRegistry.class);
        when(rpcProviderRegistry.getRpcService(SnmpService.class)).thenReturn(snmp);

        when(dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class), any(DataChangeListener.class), any(AsyncDataBroker.DataChangeScope.class))).thenReturn(registration);

        return new DeviceIdentificationManager(dataBroker, rpcProviderRegistry);
    }

    private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> createChangeEvent(Node inputNode, InstanceIdentifier<Node> keyedNodeId) {
        @SuppressWarnings("unchecked")
        AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changeEvent =  mock(AsyncDataChangeEvent.class);
        Map<InstanceIdentifier<?>, DataObject> createdData = new HashMap<>(1);

        createdData.put(keyedNodeId, inputNode);
        when(changeEvent.getCreatedData()).thenReturn(createdData);

        return changeEvent;
    }

    private ReadOnlyTransaction setDeviceTypeInfo(DataBroker dataBroker, DeviceTypes inputDeviceTypes) {
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        when(dataBroker.newReadOnlyTransaction()).thenReturn(readOnlyTransaction);

        when(readOnlyTransaction.read(any(LogicalDatastoreType.class), eq((InstanceIdentifier) InstanceIdentifier.builder(DeviceTypes.class).build())))
                .thenReturn(Futures.immediateCheckedFuture(Optional.fromNullable(inputDeviceTypes)));

        return readOnlyTransaction;
    }

    private WriteTransaction createWriteTransaction(DataBroker dataBroker) {
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        when(dataBroker.newWriteOnlyTransaction()).thenReturn(writeTransaction);
        when(writeTransaction.submit()).thenReturn(Futures.<Void, TransactionCommitFailedException>immediateCheckedFuture(null));

        return writeTransaction;
    }
}