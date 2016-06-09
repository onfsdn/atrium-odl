package org.opendaylight.didm.vendor.extreme;

/*
 * Copyright (c) 2016 Extreme Networks.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Created by llam on 3/15/2016.
 * */

import org.opendaylight.didm.util.DriverUtil;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.AddFlowInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.FlowTableRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.FlowRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.Collection;

public class ExtremeDriverUtil extends DriverUtil {

    public static void install_flows(SalFlowService flowService, Collection<Flow> flows, InstanceIdentifier<Node> node) {
        install_flows(flowService, flows, node, "1000", 0xFFFFFFFFL);
    }

    public static void install_flows(SalFlowService flowService, Collection<Flow> flows, InstanceIdentifier<Node> node, String flowId, long bufferId) {
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
            TableKey flowTableKey = new TableKey(flow.getTableId());
            InstanceIdentifier<Table> tableID = node.builder().augmentation(FlowCapableNode.class).child(Table.class, flowTableKey).build();

            FlowKey flow_key = new FlowKey(new FlowId(flowId));
            InstanceIdentifier<Flow> flowID = tableID.child(Flow.class, flow_key);

            AddFlowInputBuilder inputBuilder = new AddFlowInputBuilder()
                    .setBufferId(Long.valueOf(bufferId))
                    .setPriority(flow.getPriority())
                    .setFlowTable(new FlowTableRef(tableID))
                    .setFlowRef(new FlowRef(flowID)).setNode(new NodeRef(node))
                    .setMatch(flow.getMatch()).setTableId(flow.getTableId())
                    .setHardTimeout(flow.getHardTimeout())
                    .setInstructions(flow.getInstructions());

            flowService.addFlow(inputBuilder.build());
        }
    }
}