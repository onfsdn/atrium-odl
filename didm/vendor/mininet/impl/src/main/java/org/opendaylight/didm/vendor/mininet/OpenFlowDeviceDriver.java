/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.vendor.mininet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import org.opendaylight.didm.util.DriverUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.SalFlowService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.WriteActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.write.actions._case.WriteActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.service.rev130918.SalGroupService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeContext;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestination;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.AdjustFlowOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.FilterInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.ForwardInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.NextInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.adjust.flow.output.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.filter.input.FilterObjective;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.forward.input.ForwardingObjective;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.forward.input.forwarding.objective.Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.next.input.NextObjective;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.next.input.next.objective.TrafficTreatment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.mininet.rev150211.MininetDeviceType;
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
 * The mininet OF driver does the following:
 *
 * 1. listen for node added/removed in inventory (future: filtered by device
 * type) 2. when a mininet node is added, register the routed RPCs (other driver
 * types may register as DCLs for a feature such as vlan) 3. when a mininet node
 * is removed, close the RPC registration (and/or DCLs for other driver types)
 */
public class OpenFlowDeviceDriver implements OpenflowFeatureService, DataChangeListener, AutoCloseable {
	private static final Logger LOG = LoggerFactory.getLogger(OpenFlowDeviceDriver.class);
	private static final InstanceIdentifier<DeviceType> PATH = InstanceIdentifier.builder(Nodes.class).child(Node.class)
			.augmentation(DeviceType.class).build();
	private static final Class<? extends DeviceTypeBase> DEVICE_TYPE = MininetDeviceType.class;

	private final Map<InstanceIdentifier<?>, BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService>> rpcRegistrations = new HashMap<>();
	private final RpcProviderRegistry rpcRegistry;
	private final SalFlowService salFlowService;
	private final SalGroupService salGroupService;

	private ListenerRegistration<DataChangeListener> dataChangeListenerRegistration;

	public static final long IPv4 = 2048;
	public static final long ARP = 2054;
	protected static final short FILTER_TABLE = 0;
	protected static final short FIB_TABLE = 1;

	private static final int DEFAULT_PRIORITY = 0x8000;
	private static final int HIGHEST_PRIORITY = 0xffff;

	public OpenFlowDeviceDriver(DataBroker dataBroker, RpcProviderRegistry rpcRegistry) {
		this.rpcRegistry = Preconditions.checkNotNull(rpcRegistry);

		// obtain the flow and group service.
		this.salFlowService = rpcRegistry.getRpcService(SalFlowService.class);
		this.salGroupService = rpcRegistry.getRpcService(SalGroupService.class);

		if (this.salFlowService != null && this.salGroupService != null) {
			LOG.info("Mininet driver correctly obtained the salflow and salgroup service.");
		}

		// register listener for Node, in future should be filtered by device
		// type
		// subscribe to be notified when a device-type augmentation is applied
		// to an inventory node
		dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, PATH,
				this, AsyncDataBroker.DataChangeScope.BASE);
	}

	@Override
	public Future<RpcResult<AdjustFlowOutput>> adjustFlow(AdjustFlowInput input) {
		LOG.error("Mininet adjustFlow");
		// Since mininet is s/w based it should support all capabilities, just
		// echo the input flow

		// TODO: should this be a deep copy?
		List<Flow> adjustedFlows = ImmutableList.of(new FlowBuilder(input.getFlow()).build());

		AdjustFlowOutput output = new AdjustFlowOutputBuilder().setFlow(adjustedFlows).build();
		return Futures.immediateFuture(RpcResultBuilder.success(output).build());
	}

	@Override
	public Future<RpcResult<Void>> filter(FilterInput input) {
		LOG.debug("filter input {}  noderef : {}", input);
		processFilterObjective(input.getFilterObjective(), (InstanceIdentifier<Node>) input.getNode().getValue());

		RpcResultBuilder<Void> result = RpcResultBuilder.success();
		return Futures.immediateFuture(result.build());
	}

	@Override
	public Future<RpcResult<Void>> forward(ForwardInput input) {
		// TODO Auto-generated method stub
		Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows = this
				.processFwdObjective(input.getForwardingObjective());

		switch (input.getForwardingObjective().getOperation()) {
		case Add:
			// Add operation install the flows.
			DriverUtil.install_flows(this.salFlowService, flows, (InstanceIdentifier<Node>) input.getNode().getValue());
			break;
		case Remove:
			// Delete operation delete the flows.
			DriverUtil.deleteFlows(this.salFlowService, flows, (InstanceIdentifier<Node>) input.getNode().getValue());
			break;

		default:
			// don't do anything.
			LOG.warn("Operation {} not supported", input.getForwardingObjective().getOperation());
			break;
		}

		RpcResultBuilder<Void> result = RpcResultBuilder.success();
		return Futures.immediateFuture(result.build());
	}

	@Override
	public Future<RpcResult<Void>> next(NextInput input) {

		switch (input.getNextObjective().getType()) {
		case Simple:
			processSimpleNextObjective(input.getNextObjective(), (InstanceIdentifier<Node>) input.getNode().getValue());
			break;
		case Hashed:
			break;
		case Failover:
			break;

		default:
			LOG.warn("Unknown next objective type {}", input.getNextObjective());
		}

		RpcResultBuilder<Void> result = RpcResultBuilder.success();
		return Futures.immediateFuture(result.build());
	}

	@Override
	public void close() throws Exception {
		if (dataChangeListenerRegistration != null) {
			dataChangeListenerRegistration.close();
			dataChangeListenerRegistration = null;
		}

		// remove any remaining RPC registrations
		for (Map.Entry<InstanceIdentifier<?>, BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService>> entry : rpcRegistrations
				.entrySet()) {
			entry.getValue().close();
		}
		rpcRegistrations.clear();
	}

	@Override
	public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		// NOTE: we're ignoring updates
		Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
		if (createdData != null) {
			for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : createdData.entrySet()) {
				DeviceType deviceType = (DeviceType) entry.getValue();
				if (isMininetDeviceType(deviceType.getDeviceType())) {
					registerRpcService(entry.getKey().firstIdentifierOf(Node.class));
				}
			}
		}

		// TODO: are RPCs automatically removed if the node is removed?
		Set<InstanceIdentifier<?>> removedPaths = change.getRemovedPaths();
		if ((removedPaths != null) && !removedPaths.isEmpty()) {
			for (InstanceIdentifier<?> removedPath : removedPaths) {
				DeviceType deviceType = (DeviceType) change.getOriginalData().get(removedPath);
				if (isMininetDeviceType(deviceType.getDeviceType())) {
					closeRpcRegistration(removedPath.firstIdentifierOf(Node.class));
				}
			}
		}
	}

	private static boolean isMininetDeviceType(Class<? extends DeviceTypeBase> deviceType) {
		return deviceType == DEVICE_TYPE;
	}

	private void registerRpcService(InstanceIdentifier<Node> path) {
		if (!rpcRegistrations.containsKey(path)) {
			try {
				BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService> registration = rpcRegistry
						.addRoutedRpcImplementation(OpenflowFeatureService.class, this);
				registration.registerPath(NodeContext.class, path);
				rpcRegistrations.put(path, registration);
			} catch (IllegalStateException e) {
				LOG.error("Failed to register RPC for node: {}", path, e);
			}
		}
	}

	private void closeRpcRegistration(InstanceIdentifier<Node> path) {
		if (rpcRegistrations.containsKey(path)) {
			rpcRegistrations.remove(path).close();
		}
	}

	/**
	 * This method translates the flow objective to the collection of flows.
	 *
	 * @param fwd
	 *            Forwarding objective.
	 * @return Collection of mapped flows.
	 */
	private Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> processFwdObjective(
			ForwardingObjective fwd) {
		switch (fwd.getFlag()) {
		case Specific:
			return processSpecific(fwd);
		case Versatile:
			return processVersatile(fwd);
		default:
			LOG.warn("Unknown forwarding floag {}", fwd.getFlag());
		}
		return Collections.emptyList();
	}

	private Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> processSpecific(
			ForwardingObjective fwd) {
		LOG.info("process specific : {}", fwd);

		try {
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			Match match = fwd.getMatch();
			List<Action> action = fwd.getAction();
			Long etherType = match.getEthernetMatch().getEthernetType().getType().getValue();
			LOG.info("layer 3 match : {}", match.getLayer3Match());

			if (etherType != IPv4) {
				LOG.info("match is not of type ipv4");
				return Collections.emptySet();
			}

			LOG.info(" match field : {}", match);
			Ipv4Match ipv4match = (Ipv4Match) match.getLayer3Match();

			flowBuilder.setTableId(FIB_TABLE).setPriority(fwd.getPriority());
			MatchBuilder matchBuilder = new MatchBuilder();

			EthernetMatchBuilder ethernetBuilder = new EthernetMatchBuilder()
					.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(IPv4))).build());

			Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
			ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(ipv4match.getIpv4Destination().getValue()));

			matchBuilder.setEthernetMatch(ethernetBuilder.build())
					.setLayer3Match((Layer3Match) ipv4MatchBuilder.build());

			// Action should be forward to controller.
			java.util.List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();

			GroupActionBuilder actionBuilder_t = new GroupActionBuilder().setGroupId(fwd.getNextId().longValue());
			// OutputActionBuilder actionBuilder_t = new
			// OutputActionBuilder().setOutputNodeConnector(new
			// Uri(OutputPortValues.FLOOD.toString()));

			ActionBuilder actionBuilder = new ActionBuilder();
			actionBuilder.setOrder(0).setKey(new ActionKey(0));

			actionBuilder.setAction(new GroupActionCaseBuilder().setGroupAction(actionBuilder_t.build()).build());
			// actionBuilder.setAction(new
			// OutputActionCaseBuilder().setOutputAction(actionBuilder_t.build()).build());

			actions.add(actionBuilder.build());

			// build instruction

			// TODO : Clarification in the use of instruction.
			// Two options are available write and apply instruction.

			// create applyActions
			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();

			// created the write instruction.
			WriteActions writeActions = new WriteActionsBuilder().setAction(actions).build();

			// add apply action to an Instruction.
			// Instruction instruction = new InstructionBuilder()
			// .setOrder(0)
			// .setInstruction(new ApplyActionsCaseBuilder()
			// .setApplyActions(applyActions)
			// .build())
			// .build();

			Instruction instruction = new InstructionBuilder().setOrder(0)
					.setInstruction(new WriteActionsCaseBuilder().setWriteActions(writeActions).build()).build();

			// set the flow instruction.
			flowBuilder
					.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build());
			flowBuilder.setMatch(matchBuilder.build());

			// set the timeout
			Integer timeout = fwd.getTimeout() == null ? 0 : fwd.getTimeout();

			flowBuilder.setHardTimeout(timeout);

			flowBuilder.setTableId(FIB_TABLE);
			return Collections.singleton(flowBuilder.build());
		} catch (NullPointerException ex) {
			LOG.warn("forwaridng objective is not supported : {}", fwd);
			return Collections.emptySet();
		}
	}

	private Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> processVersatile(
			ForwardingObjective fwd) {
		LOG.debug("process versatile {}", fwd);

		try {
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			Match match = fwd.getMatch();
			EthernetMatch ethernetMatch = match.getEthernetMatch();
			IpMatch ipMatch = match.getIpMatch();
			Layer4Match layer4Match = match.getLayer4Match();
			Long etherType = match.getEthernetMatch().getEthernetType().getType().getValue();

			MatchBuilder matchBuilder = new MatchBuilder();

			if (etherType == ARP) {
				matchBuilder.setEthernetMatch(ethernetMatch);
			}

			else if (etherType == IPv4) {
				matchBuilder.setEthernetMatch(ethernetMatch).setIpMatch(ipMatch).setLayer4Match(layer4Match);

			} else {
				LOG.warn("match not supported");
				return Collections.emptySet();
			}

			List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = fwd
					.getAction();
			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
			Instruction instruction = new InstructionBuilder().setOrder(0)
					.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build()).build();

			flowBuilder.setTableId(FILTER_TABLE).setPriority(fwd.getPriority());
			// set the flow instruction.
			flowBuilder
					.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build());
			flowBuilder.setMatch(matchBuilder.build());

			// set the timeout
			Integer timeout = fwd.getTimeout() == null ? 0 : fwd.getTimeout();

			flowBuilder.setHardTimeout(timeout);

			return Collections.singleton(flowBuilder.build());
		} catch (NullPointerException ex) {
			LOG.warn("forwaridng objective is not supported : {}", fwd);
			return Collections.emptySet();
		}
	}

	/**
	 * This method process the nxtObjective and create single group bucket
	 * (SIMPLE group type).
	 * 
	 * @param nxtObject
	 *            next Objective.
	 */
	private void processSimpleNextObjective(NextObjective nxtObject, InstanceIdentifier<Node> nodeID) {
		List<TrafficTreatment> tts = nxtObject.getTrafficTreatment();

		if (tts.size() != 1) {
			LOG.warn("Next Objective of type simple shoudl only have " + "a single traffic treatment.");
			return;
		}

		// we are sure that nxtObjective has only one
		// traffic treatment.
		TrafficTreatment tt = tts.get(0);
		DriverUtil.addGroup(this.salGroupService, tt.getAction(), nodeID, nxtObject.getNextId());
	}

	// private method to create the flow to forward the packet to
	// FILTER_TABLE -> FIB_TABLE
	private org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow createGotoFilterRule(
			FilterObjective filterObj) {

		try {
			// create flow rule to forward the packet to the FIB_TABLE.
			// match : in_port + vlan + dl_dst + ip
			// action : pop_vlan + goto: FIB_TABLE
			EthernetDestination dl_dst = filterObj.getEthernetMatch().getEthernetDestination();
			VlanId vlanID = filterObj.getVlanMatch().getVlanId();
			NodeConnectorId inPort = filterObj.getInPort();
			// IpVersion ipVersion = filterObj.getIpMatch().getIpProto();
			Long ethType = filterObj.getEthernetMatch().getEthernetType().getType().getValue();
			;

			if (ethType != IPv4) {
				LOG.warn("flow objective not supported");
				return null;
			}

			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			// create the ethernet match
			EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
			ethMatchBuilder.setEthernetDestination(dl_dst)
					.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(IPv4))).build());

			// create vlan match
			VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
			vlanMatchBuilder
					.setVlanId(new VlanIdBuilder().setVlanId(vlanID.getVlanId()).setVlanIdPresent(true).build());

			MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethMatchBuilder.build())
					.setVlanMatch(vlanMatchBuilder.build()).setInPort(inPort);
			// create the instruction to to punt the packet to next table.
			// action pop_vlan + goto: FIB_TABLE
			// create applyActions

			java.util.List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();

			// StripVlanActionBuilder stripVlanBuilder = new
			// StripVlanActionBuilder();
			// OutputActionBuilder actionBuilder_t = new
			// OutputActionBuilder().setOutputNodeConnector(new
			// Uri(OutputPortValues.FLOOD.toString()));

			ActionBuilder actionBuilder = new ActionBuilder();
			actionBuilder.setOrder(0).setKey(new ActionKey(0));

			actionBuilder.setAction(
					new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build());

			// actionBuilder.setAction(new
			// OutputActionCaseBuilder().setOutputAction(actionBuilder_t.build()).build());

			actions.add(actionBuilder.build());

			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
			GoToTableBuilder gotoInst = new GoToTableBuilder().setTableId(FIB_TABLE);

			// created the write instruction.
			// WriteActions writeActions = new
			// WriteActionsBuilder().setAction(actions).build();

			// add apply action to an Instruction.
			// Instruction instruction = new InstructionBuilder()
			// .setOrder(0)
			// .setInstruction(new ApplyActionsCaseBuilder()
			// .setApplyActions(applyActions)
			// .build())
			// .build();

			Instruction instruction = new InstructionBuilder().setOrder(0)
					.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build()).build();

			Instruction instructionGoto = new InstructionBuilder().setOrder(1)
					.setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoInst.build()).build()).build();

			// set the flow instruction.
			flowBuilder.setInstructions(
					new InstructionsBuilder().setInstruction(ImmutableList.of(instruction, instructionGoto)).build());
			flowBuilder.setMatch(matchBuilder.build()).setTableId(FILTER_TABLE).setHardTimeout(Integer.valueOf(0))
					.setPriority(Integer.valueOf(DEFAULT_PRIORITY));

			return flowBuilder.build();
		} catch (NullPointerException ex) {
			LOG.warn("filtering objective not supported : {}", filterObj);
			return null;
		}
	}

	// private method to create the flow to punt the bgp packet to the
	// controller.
	private org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow createPuntFilterRule(
			FilterObjective filterObj) {
		try {
			// if the packet are destined to bgp speaker ip address.
			// match : in_port + vlan + dl_dst + ip + ip_dst
			// action : output: ctrl
			EthernetDestination dl_dst = filterObj.getEthernetMatch().getEthernetDestination();
			VlanId vlanID = filterObj.getVlanMatch().getVlanId();
			NodeConnectorId inPort = filterObj.getInPort();
			Ipv4Prefix ipDst = ((Ipv4Match) filterObj.getLayer3Match()).getIpv4Destination();
			Long ethType = filterObj.getEthernetMatch().getEthernetType().getType().getValue();
			;

			if (ethType != IPv4) {
				LOG.warn("flow objective not supported");
				return null;
			}

			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			// create the ethernet match
			EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
			ethMatchBuilder.setEthernetDestination(dl_dst)
					.setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(IPv4))).build());

			// create vlan match
			VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
			vlanMatchBuilder
					.setVlanId(new VlanIdBuilder().setVlanId(vlanID.getVlanId()).setVlanIdPresent(true).build());

			// create IPv4_dst match
			Ipv4Match ipv4Match = new Ipv4MatchBuilder().setIpv4Destination(ipDst).build();

			MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethMatchBuilder.build())
					.setVlanMatch(vlanMatchBuilder.build()).setInPort(inPort).setLayer3Match(ipv4Match);

			// create the instruction to to punt the packet to next table.
			// action: output=ctrl

			java.util.List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();

			OutputActionBuilder outputAction = new OutputActionBuilder()
					.setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
					.setMaxLength(Integer.valueOf(0xffff));

			ActionBuilder actionBuilder = new ActionBuilder();
			actionBuilder.setOrder(0).setKey(new ActionKey(0));

			actionBuilder.setAction(new OutputActionCaseBuilder().setOutputAction(outputAction.build()).build());

			actions.add(actionBuilder.build());

			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();

			Instruction instruction = new InstructionBuilder().setOrder(0)
					.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build()).build();

			// set the flow instruction.
			flowBuilder
					.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build());
			flowBuilder.setMatch(matchBuilder.build()).setTableId(FILTER_TABLE).setHardTimeout(Integer.valueOf(0))
					.setPriority(Integer.valueOf(HIGHEST_PRIORITY));

			return flowBuilder.build();
		} catch (NullPointerException ex) {
			LOG.warn("filtering objective not supported : {}", filterObj);
			return null;
		}
	}

	/**
	 * This method process the filterObjective by converting the filter
	 * objective to corresponding flows.
	 * 
	 * @param filterObj
	 *            FilterObjective communicated from the application.
	 * @param nodeID
	 *            target Instance Identifier to apply the flow.
	 */
	private void processFilterObjective(FilterObjective filterObj, InstanceIdentifier<Node> nodeID) {

		try {

			// retrieve the rules for the ether_dst, vlanID and ipv4_dst
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flowNxtTable = createGotoFilterRule(
					filterObj);

			// create flow rule to generate the packet in message.
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flowPunt = createPuntFilterRule(
					filterObj);

			if (flowPunt != null && flowNxtTable != null) {
				DriverUtil.install_flows(this.salFlowService, Collections.singletonList(flowNxtTable), nodeID);
				DriverUtil.install_flows(this.salFlowService, Collections.singletonList(flowPunt), nodeID);
			}

		} catch (NullPointerException ex) {
			LOG.warn("filtering objective not supported : {}", filterObj);
		}

	}

}
