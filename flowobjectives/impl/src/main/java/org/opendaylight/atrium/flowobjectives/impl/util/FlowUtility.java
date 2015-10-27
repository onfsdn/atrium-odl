package org.opendaylight.atrium.flowobjectives.impl.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.FlowBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.collect.ImmutableList;

public class FlowUtility {

    public static Flow addDefaultFlow() {
        FlowBuilder flowBuilder = new FlowBuilder();

        // set the table id and the flowname
        flowBuilder.setTableId((short) (0)).setFlowName("default").setPriority(0);

        // match field leave it empty as flow is default rule.

        // Action should be forward to controller.
        java.util.List<Action> actions = new ArrayList<Action>();

        OutputActionBuilder actionBuilder_t = new OutputActionBuilder().setOutputNodeConnector(new Uri(OutputPortValues.FLOOD.toString()));

        ActionBuilder actionBuilder = new ActionBuilder();
        actionBuilder.setOrder(0).setKey(new ActionKey(0));
        actionBuilder.setAction(new OutputActionCaseBuilder().setOutputAction(actionBuilder_t.build()).build());

        actions.add(actionBuilder.build());

        // build instruction

        // create applyActions
        ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();

        // add apply action to an Instruction.
        Instruction instruction = new InstructionBuilder()
        .setOrder(0)
        .setInstruction(new ApplyActionsCaseBuilder()
        .setApplyActions(applyActions)
        .build())
        .build();

        // set the flow instruction.
        flowBuilder.setInstructions(new InstructionsBuilder()
        .setInstruction(ImmutableList.of(instruction))
        .build());

        //flowBuilder.setBufferId((long));
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        Flow flow = flowBuilder.build();
        return flow;

    }

    public static void install_flows(SalFlowService flowService, Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows, InstanceIdentifier<Node> node) {
        for (org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
            // create the input for the addFlow
            TableKey flowTableKey = new TableKey(flow.getTableId());
            InstanceIdentifier<Table> tableID = node.builder().augmentation(FlowCapableNode.class)
                    .child(Table.class, flowTableKey)
                    .build();

            // TODO : increment the flow id after each use. otherwise flow will take same ID.
            FlowId flowid = new FlowId("1000");
            FlowKey flow_key = new FlowKey(flowid);
            InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flowID = tableID.child(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow.class, flow_key);



            AddFlowInputBuilder inputBuilder = new AddFlowInputBuilder()
            .setBufferId(Long.valueOf(0))
            .setPriority(flow.getPriority())
            .setFlowTable(new FlowTableRef(tableID))
            .setFlowRef(new FlowRef(flowID))
            .setNode(new NodeRef(node))
            .setMatch(flow.getMatch())
            .setTableId(flow.getTableId())
            .setHardTimeout(flow.getHardTimeout())
            .setBufferId(Long.valueOf(0))
            .setInstructions(flow.getInstructions());

            flowService.addFlow(inputBuilder.build());
        }
    }

    /**
     * This utility method deletes the flows as per the match.
     * @param flowService reference of SalFlowService.
     * @param flows Collection of flows to be deleted.
     * @param node Flows to be deleted from the node.
     */
    public static void deleteFlows(SalFlowService flowService,
            Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows,
            InstanceIdentifier<Node> node) {

        for(org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flow : flows) {
            RemoveFlowInputBuilder inputBuilder = new RemoveFlowInputBuilder()
            .setMatch(flow.getMatch())
            .setTableId(flow.getTableId())
            .setNode(new NodeRef(node));
            flowService.removeFlow(inputBuilder.build());

        }
    }

    public static void addGroup(SalGroupService salGrpService,
            List<Action> actions,
            InstanceIdentifier<Node> nodeID,
            Integer grpID
            ) {

        AddGroupInputBuilder grpInputBuilder = new AddGroupInputBuilder();

        BucketBuilder buckerBuilder = new BucketBuilder();
        buckerBuilder.setAction(actions)
        .setBucketId(new BucketId(grpID.longValue()));

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
