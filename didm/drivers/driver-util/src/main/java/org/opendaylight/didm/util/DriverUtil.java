/*
 * Copyright (c) 2015 Criterion Networks.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.AddGroupInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class DriverUtil {

    /**
     * This utility method installs the flows to the switch.
     *
     * @param flowService
     *            reference of SalFlowService.
     * @param flows
     *            Collection of flows to be installed.
     * @param node
     *            Flows to be deleted from the node.
     */
    public static void install_flows(
            SalFlowService flowService,
            Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows,
            InstanceIdentifier<Node> node) {
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
            // create the input for the addFlow
            TableKey flowTableKey = new TableKey(flow.getTableId());
            InstanceIdentifier<Table> tableID = node.builder()
                    .augmentation(FlowCapableNode.class)
                    .child(Table.class, flowTableKey).build();

            // TODO : increment the flow id after each use. otherwise flow will
            // take same ID.
            FlowId flowid = new FlowId("1000");
            FlowKey flow_key = new FlowKey(flowid);
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flowID = tableID
                    .child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow.class,
                            flow_key);

            AddFlowInputBuilder inputBuilder = new AddFlowInputBuilder()
                    .setBufferId(Long.valueOf(0))
                    .setPriority(flow.getPriority())
                    .setFlowTable(new FlowTableRef(tableID))
                    .setFlowRef(new FlowRef(flowID)).setNode(new NodeRef(node))
                    .setMatch(flow.getMatch()).setTableId(flow.getTableId())
                    .setHardTimeout(flow.getHardTimeout())
                    .setBufferId(Long.valueOf(0))
                    .setInstructions(flow.getInstructions());

            flowService.addFlow(inputBuilder.build());
        }
    }

    /**
     * This utility method deletes the flows as per the match.
     *
     * @param flowService
     *            reference of SalFlowService.
     * @param flows
     *            Collection of flows to be deleted.
     * @param node
     *            Flows to be deleted from the node.
     */
    public static void deleteFlows(
            SalFlowService flowService,
            Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows,
            InstanceIdentifier<Node> node) {

        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
            RemoveFlowInputBuilder inputBuilder = new RemoveFlowInputBuilder()
                    .setMatch(flow.getMatch()).setTableId(flow.getTableId())
                    .setNode(new NodeRef(node));
            flowService.removeFlow(inputBuilder.build());

        }
    }

    /**
     * This utility method adds the group entry to the group table.
     *
     * @param salGrpService
     *            instance of sal group service.
     * @param actions
     *            List of action to be included in the group bucket.
     * @param nodeID
     *            node identifier to add the group entry.
     * @param grpID
     *            Group entry will be identified by the this ID.
     */
    public static void addGroup(SalGroupService salGrpService,
            List<Action> actions, InstanceIdentifier<Node> nodeID, Integer grpID) {

        AddGroupInputBuilder grpInputBuilder = new AddGroupInputBuilder();

        BucketBuilder buckerBuilder = new BucketBuilder();
        buckerBuilder.setAction(actions).setBucketId(
                new BucketId(grpID.longValue()));

        List<Bucket> bucketlist = new ArrayList<Bucket>();
        bucketlist.add(buckerBuilder.build());
        grpInputBuilder.setBuckets(
                new BucketsBuilder().setBucket(bucketlist)
                        .build()).setNode(new NodeRef(nodeID))
        .setGroupRef(new GroupRef(nodeID))
        .setGroupId(new GroupId(Long.valueOf(grpID.longValue())))
        .setGroupType(GroupTypes.GroupIndirect).setGroupName("Foo");

        AddGroupInput input = grpInputBuilder.build();
        salGrpService.addGroup(input);
    }
}
