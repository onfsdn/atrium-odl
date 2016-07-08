/*
 * Copyright (c) 2016 Extreme Networks.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.vendor.extreme;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.extreme.rev150211.ExtremeDeviceType;
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

public class OpenFlowDeviceDriver implements OpenflowFeatureService, DataChangeListener, AutoCloseable
{
	protected static final Logger LOG = LoggerFactory.getLogger(OpenFlowDeviceDriver.class);

	protected static final InstanceIdentifier<DeviceType> PATH         = InstanceIdentifier.builder(Nodes.class).child(Node.class).augmentation(DeviceType.class).build();
	protected static final Class<? extends DeviceTypeBase> DEVICE_TYPE = ExtremeDeviceType.class;

	protected final Map<InstanceIdentifier<?>, BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService>> rpcRegistrations = new HashMap<>();

	protected RpcProviderRegistry rpcRegistry  = null;
	protected SalFlowService salFlowService    = null;
	protected  SalGroupService salGroupService = null;

	protected ListenerRegistration<DataChangeListener> dataChangeListenerRegistration = null;

	protected static final long IPv4 = 2048;
	protected static final long ARP  = 2054;

	protected static final short FILTER_TABLE = 0;
	protected static final short FIB_TABLE    = 1;

	protected static final int DEFAULT_PRIORITY = 0x8000;
	protected static final int HIGHEST_PRIORITY = 0xFFFF;

	protected static final int DEFAULT_GROUP_ID = 0;

	public OpenFlowDeviceDriver(DataBroker dataBroker, RpcProviderRegistry rpcRegistry) {
		LOG.debug("OpenFlowDeviceDriver");

		this.rpcRegistry = Preconditions.checkNotNull(rpcRegistry);

		salFlowService = rpcRegistry.getRpcService(SalFlowService.class);
		salGroupService = rpcRegistry.getRpcService(SalGroupService.class);

		if ((salFlowService != null) && (salGroupService != null))
			LOG.info("Extreme driver correctly obtained the salflow and salgroup service.");

		dataChangeListenerRegistration = dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, PATH, this, AsyncDataBroker.DataChangeScope.BASE);
	}

	public Future<RpcResult<AdjustFlowOutput>> adjustFlow(AdjustFlowInput input) {
		LOG.debug("adjustFlow -> " + input);

		List<Flow> adjustedFlows = ImmutableList.of(new FlowBuilder(input.getFlow()).build());
		AdjustFlowOutput output = new AdjustFlowOutputBuilder().setFlow(adjustedFlows).build();
		return Futures.immediateFuture(RpcResultBuilder.success(output).build());
	}

	public Future<RpcResult<Void>> filter(FilterInput input) {
		LOG.debug("filter -> " + input);

		processFilterObjective(input.getFilterObjective(), (InstanceIdentifier<Node>) input.getNode().getValue());
		RpcResultBuilder<Void> result = RpcResultBuilder.success();
		return Futures.immediateFuture(result.build());
	}

	public Future<RpcResult<Void>> forward(ForwardInput input) {
		LOG.debug("forward -> " + input);

		Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> flows = processFwdObjective(input.getForwardingObjective());
		switch (input.getForwardingObjective().getOperation()) {
			case Add:
				LOG.info("forward add flow -> " + flows.toString());
				ExtremeDriverUtil.install_flows(salFlowService, flows, (InstanceIdentifier<Node>) input.getNode().getValue());
				break;
			case Remove:
				LOG.info("forward remove flow -> " + flows.toString());
				ExtremeDriverUtil.deleteFlows(salFlowService, flows, (InstanceIdentifier<Node>) input.getNode().getValue());
				break;
			default:
				LOG.warn("forward unsupported operation -> ", input.getForwardingObjective().getOperation());
				break;
		}

		RpcResultBuilder<Void> result = RpcResultBuilder.success();
		return Futures.immediateFuture(result.build());
	}

	public Future<RpcResult<Void>> next(NextInput input) {
		LOG.debug("next -> " + input);

		switch (input.getNextObjective().getType()) {
			case Simple:
				LOG.info("next simple objective -> " + input.getNextObjective());
				processSimpleNextObjective(input.getNextObjective(), (InstanceIdentifier<Node>) input.getNode().getValue());
				break;
			case Hashed:
				LOG.info("next hashed objective -> " + input.getNextObjective());
				break;
			case Failover:
				LOG.info("next failover objective -> " + input.getNextObjective());
				break;
			default:
				LOG.warn("next unsupported objective -> " + input.getNextObjective());
				break;
		}

		RpcResultBuilder<Void> result = RpcResultBuilder.success();
		return Futures.immediateFuture(result.build());
	}

	public void close() throws Exception {
		LOG.debug("close");

		if (dataChangeListenerRegistration != null) {
			dataChangeListenerRegistration.close();
			dataChangeListenerRegistration = null;
		}

		for (Map.Entry<InstanceIdentifier<?>, BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService>> entry : rpcRegistrations.entrySet())
			entry.getValue().close();

		rpcRegistrations.clear();
	}

	public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		LOG.debug("onDataChanged -> " + change);

		Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();
		if (createdData != null) {
			for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : createdData.entrySet()) {
				DeviceType deviceType = (DeviceType) entry.getValue();
				if (isExtreme(deviceType.getDeviceType()))
					registerRpcService(entry.getKey().firstIdentifierOf(Node.class));
			}
		}

		Set<InstanceIdentifier<?>> removedPaths = change.getRemovedPaths();
		if ((removedPaths != null) && !removedPaths.isEmpty()) {
			for (InstanceIdentifier<?> removedPath : removedPaths) {
				DeviceType deviceType = (DeviceType) change.getOriginalData().get(removedPath);
				if (isExtreme(deviceType.getDeviceType()))
					closeRpcRegistration(removedPath.firstIdentifierOf(Node.class));
			}
		}
	}

	protected static boolean isExtreme(Class<? extends DeviceTypeBase> deviceType) {
		return deviceType == DEVICE_TYPE;
	}

	protected void registerRpcService(InstanceIdentifier<Node> path) {
		LOG.debug("registerRpcService -> " + path);

		if (!rpcRegistrations.containsKey(path)) {
			try {
				BindingAwareBroker.RoutedRpcRegistration<OpenflowFeatureService> registration = rpcRegistry.addRoutedRpcImplementation(OpenflowFeatureService.class, this);
				registration.registerPath(NodeContext.class, path);
				rpcRegistrations.put(path, registration);
			} catch (IllegalStateException ise) {
				LOG.error("registerRpcService, failed to register RPC -> " + path, ise);
			}
		}
	}

	protected void closeRpcRegistration(InstanceIdentifier<Node> path) {
		LOG.debug("closeRpcRegistration -> " + path);

		if (rpcRegistrations.containsKey(path))
			rpcRegistrations.remove(path).close();
	}

	protected Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> processFwdObjective(ForwardingObjective fwd) {
		LOG.debug("processFwdObjective -> " + fwd);

		switch (fwd.getFlag()) {
			case Specific:
				LOG.info("processFwdObjective specific flag -> " + fwd);
				return processSpecific(fwd);
			case Versatile:
				LOG.info("processFwdObjective versatile flag -> " + fwd);
				return processVersatile(fwd);
			default:
				LOG.warn("processFwdObjective unsupported forwarding flag -> " + fwd);
				break;
		}
		return Collections.emptyList();
	}

	protected Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> processSpecific(
			ForwardingObjective fwd) {
		LOG.debug("processSpecific -> " + fwd);

		try {
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			Match match = fwd.getMatch();
			List<Action> action = fwd.getAction();
			Long etherType = match.getEthernetMatch().getEthernetType().getType().getValue();

			if (etherType != IPv4) {
				LOG.info("processSpecific unsupported ether type -> " + etherType);
				return Collections.emptySet();
			}

			Ipv4Match ipv4match = (Ipv4Match)match.getLayer3Match();

			flowBuilder.setTableId(FIB_TABLE).setPriority(fwd.getPriority());
			MatchBuilder matchBuilder = new MatchBuilder();

			EthernetMatchBuilder ethernetBuilder = new EthernetMatchBuilder().setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(IPv4))).build());

			Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
			ipv4MatchBuilder.setIpv4Destination(new Ipv4Prefix(ipv4match.getIpv4Destination().getValue()));

			matchBuilder.setEthernetMatch(ethernetBuilder.build()).setLayer3Match((Layer3Match) ipv4MatchBuilder.build());

			java.util.List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();

			Integer nextId = fwd.getNextId();
			if(nextId == null)
				nextId = new Integer(DEFAULT_GROUP_ID);

			GroupActionBuilder actionBuilder_t = new GroupActionBuilder().setGroupId(nextId.longValue());
			ActionBuilder actionBuilder = new ActionBuilder();
			actionBuilder.setOrder(0).setKey(new ActionKey(0));
			actionBuilder.setAction(new GroupActionCaseBuilder().setGroupAction(actionBuilder_t.build()).build());
			actions.add(actionBuilder.build());

			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
			WriteActions writeActions = new WriteActionsBuilder().setAction(actions).build();
			Instruction instruction = new InstructionBuilder().setOrder(0).setInstruction(new WriteActionsCaseBuilder().setWriteActions(writeActions).build()).build();

			flowBuilder.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build());
			flowBuilder.setMatch(matchBuilder.build());

			Integer timeout = fwd.getTimeout() == null ? 0 : fwd.getTimeout();

			flowBuilder.setHardTimeout(timeout);

			flowBuilder.setTableId(FIB_TABLE);
			return Collections.singleton(flowBuilder.build());
		} catch (Throwable t) {
			LOG.error("processSpecific unexpected error: ", t);
			return Collections.emptySet();
		}
	}

	protected Collection<org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow> processVersatile(
			ForwardingObjective fwd) {
		LOG.debug("processVersatile -> " + fwd);

		try {
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			Match match = fwd.getMatch();
			EthernetMatch ethernetMatch = match.getEthernetMatch();
			IpMatch ipMatch = match.getIpMatch();
			Layer4Match layer4Match = match.getLayer4Match();
			Long etherType = match.getEthernetMatch().getEthernetType().getType().getValue();

			MatchBuilder matchBuilder = new MatchBuilder();

			if (etherType == ARP)
				matchBuilder.setEthernetMatch(ethernetMatch);
			else if (etherType == IPv4)
				matchBuilder.setEthernetMatch(ethernetMatch).setIpMatch(ipMatch).setLayer4Match(layer4Match);
			else {
				LOG.warn("processVersatile unsupported ether type -> " + etherType);
				return Collections.emptySet();
			}

			List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = fwd.getAction();
			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
			Instruction instruction = new InstructionBuilder().setOrder(0).setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build()).build();

			flowBuilder.setTableId(FILTER_TABLE).setPriority(fwd.getPriority());
			flowBuilder.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build());
			flowBuilder.setMatch(matchBuilder.build());

			Integer timeout = fwd.getTimeout() == null ? 0 : fwd.getTimeout();
			flowBuilder.setHardTimeout(timeout);

			return Collections.singleton(flowBuilder.build());
		} catch (Throwable t) {
			LOG.error("processVersatile unexpected error: ", t);
			return Collections.emptySet();
		}
	}

	protected void processSimpleNextObjective(NextObjective nxtObject, InstanceIdentifier<Node> nodeID) {
		LOG.debug("processSimpleNextObjective -> " + nxtObject);

		List<TrafficTreatment> tts = nxtObject.getTrafficTreatment();

		if (tts.size() != 1) {
			LOG.warn("processSimpleNextObjective, next objective has " + tts.size() + " treatments");
			return;
		}

		TrafficTreatment tt = tts.get(0);
		ExtremeDriverUtil.addGroup(salGroupService, tt.getAction(), nodeID, nxtObject.getNextId());
	}

	protected org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow createGotoFilterRule(FilterObjective filterObj) {
		LOG.debug("createGotoFilterRule -> " + filterObj);

		try {
			EthernetDestination dl_dst = filterObj.getEthernetMatch().getEthernetDestination();
			VlanId vlanID = filterObj.getVlanMatch().getVlanId();
			NodeConnectorId inPort = filterObj.getInPort();
			Long etherType = filterObj.getEthernetMatch().getEthernetType().getType().getValue();

			if (etherType != IPv4) {
				LOG.warn("createGotoFilterRule unsupported ether type -> " + etherType);
				return null;
			}

			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
			ethMatchBuilder.setEthernetDestination(dl_dst).setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(IPv4))).build());

			VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
			vlanMatchBuilder.setVlanId(new VlanIdBuilder().setVlanId(vlanID.getVlanId()).setVlanIdPresent(true).build());

			MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethMatchBuilder.build()).setVlanMatch(vlanMatchBuilder.build()).setInPort(inPort);

			java.util.List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();

			ActionBuilder actionBuilder = new ActionBuilder();
			actionBuilder.setOrder(0).setKey(new ActionKey(0));
			actionBuilder.setAction(new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build());
			actions.add(actionBuilder.build());

			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();
			GoToTableBuilder gotoInst = new GoToTableBuilder().setTableId(FIB_TABLE);

			Instruction instruction = new InstructionBuilder().setOrder(0)
					.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build()).build();

			Instruction instructionGoto = new InstructionBuilder().setOrder(1).setInstruction(new GoToTableCaseBuilder().setGoToTable(gotoInst.build()).build()).build();

			flowBuilder.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction, instructionGoto)).build());
			flowBuilder.setMatch(matchBuilder.build()).setTableId(FILTER_TABLE).setHardTimeout(Integer.valueOf(0)).setPriority(Integer.valueOf(DEFAULT_PRIORITY));

			return flowBuilder.build();
		} catch (Throwable t) {
			LOG.error("createGotoFilterRule unexpected error: ", t);
			return null;
		}
	}

	protected org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow createPuntFilterRule(FilterObjective filterObj) {
		LOG.debug("createPuntFilterRule -> " + filterObj);

		try {
			EthernetDestination dl_dst = filterObj.getEthernetMatch().getEthernetDestination();
			VlanId vlanID = filterObj.getVlanMatch().getVlanId();
			NodeConnectorId inPort = filterObj.getInPort();
			Ipv4Prefix ipDst = ((Ipv4Match) filterObj.getLayer3Match()).getIpv4Destination();
			Long etherType = filterObj.getEthernetMatch().getEthernetType().getType().getValue();

			if (etherType != IPv4) {
				LOG.warn("createPuntFilterRule unsupported ether type -> " + etherType);
				return null;
			}

			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder flowBuilder = new org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder();

			EthernetMatchBuilder ethMatchBuilder = new EthernetMatchBuilder();
			ethMatchBuilder.setEthernetDestination(dl_dst).setEthernetType(new EthernetTypeBuilder().setType(new EtherType(Long.valueOf(IPv4))).build());

			VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
			vlanMatchBuilder.setVlanId(new VlanIdBuilder().setVlanId(vlanID.getVlanId()).setVlanIdPresent(true).build());

			Ipv4Match ipv4Match = new Ipv4MatchBuilder().setIpv4Destination(ipDst).build();
			MatchBuilder matchBuilder = new MatchBuilder().setEthernetMatch(ethMatchBuilder.build()).setVlanMatch(vlanMatchBuilder.build()).setInPort(inPort).setLayer3Match(ipv4Match);

			java.util.List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actions = new ArrayList<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action>();

			OutputActionBuilder outputAction = new OutputActionBuilder()
					.setOutputNodeConnector(new Uri(OutputPortValues.CONTROLLER.toString()))
					.setMaxLength(Integer.valueOf(0xffff));

			ActionBuilder actionBuilder = new ActionBuilder();
			actionBuilder.setOrder(0).setKey(new ActionKey(0));
			actionBuilder.setAction(new OutputActionCaseBuilder().setOutputAction(outputAction.build()).build());

			actions.add(actionBuilder.build());

			ApplyActions applyActions = new ApplyActionsBuilder().setAction(actions).build();

			Instruction instruction = new InstructionBuilder().setOrder(0).setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build()).build();
			flowBuilder.setInstructions(new InstructionsBuilder().setInstruction(ImmutableList.of(instruction)).build());
			flowBuilder.setMatch(matchBuilder.build()).setTableId(FILTER_TABLE).setHardTimeout(Integer.valueOf(0)).setPriority(Integer.valueOf(HIGHEST_PRIORITY));

			return flowBuilder.build();
		}  catch (Throwable t) {
			LOG.error("createPuntFilterRule unexpected error: ", t);
			return null;
		}
	}

	protected void processFilterObjective(FilterObjective filterObj, InstanceIdentifier<Node> nodeID) {
		LOG.debug("processFilterObjective -> " + filterObj);

		try {

			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flowNxtTable = createGotoFilterRule(filterObj);
			org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow flowPunt = createPuntFilterRule(filterObj);

			if ((flowPunt != null) && (flowNxtTable != null)) {
				ExtremeDriverUtil.install_flows(this.salFlowService, Collections.singletonList(flowNxtTable), nodeID);
				ExtremeDriverUtil.install_flows(this.salFlowService, Collections.singletonList(flowPunt), nodeID);
			}

		}  catch (Throwable t) {
			LOG.error("processFilterObjective unexpected error: ", t);
		}
	}
}
