/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;

import org.opendaylight.atrium.atriumutil.ActionData;
import org.opendaylight.atrium.atriumutil.ActionUtils;
import org.opendaylight.atrium.atriumutil.AtriumConstants;
import org.opendaylight.atrium.atriumutil.AtriumUtils;
import org.opendaylight.atrium.routingservice.api.AtriumFibEntry;
import org.opendaylight.atrium.routingservice.api.AtriumFibUpdate;
import org.opendaylight.atrium.routingservice.api.FibListener;
import org.opendaylight.atrium.routingservice.api.RoutingService;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.util.AtriumInterface;
import org.opendaylight.atrium.util.AtriumInterfaceIpAddress;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.util.AtriumVlanId;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.FilterInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.ForwardInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.NextInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.Objective.Operation;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.AtriumFlowObjectiveService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.filter.input.FilterObjectiveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.ForwardingObjective.Flag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.ForwardingObjectiveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.forwarding.objective.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.next.input.NextObjective.Type;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.next.input.NextObjectiveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.next.input.next.objective.TrafficTreatment;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.next.input.next.objective.TrafficTreatmentBuilder;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

// TODO: Auto-generated Javadoc
/**
 * Main class in the BGP Application. 1. Starts all the required services 2.
 * Wait for FIB updates from routing service (BGP Speaker) 3. Initiates flow
 * installation by invoking Flow objectives API
 *
 */
public class Bgprouter implements BindingAwareConsumer, AutoCloseable {

	private static final Logger LOG = LoggerFactory.getLogger(Bgprouter.class);

	// IPv4 ethernet type used in flow match (TODO: AtriumConstants)
	public static final EthernetType IPV4_ETH_TYPE = new EthernetTypeBuilder()
			.setType(new EtherType(AtriumConstants.IPv4)).build();
	public static final EthernetType ARP_ETH_TYPE = new EthernetTypeBuilder()
			.setType(new EtherType(AtriumConstants.ARP)).build();

	// Constants for setting the prioirty in flows
	private static final int PRIORITY_OFFSET = 100;
	private static final int PRIORITY_MULTIPLIER = 5;

	// Reference count for how many times a next hop is used by a route
	private final Multiset<AtriumIpAddress> nextHopsCount = ConcurrentHashMultiset.create();

	// Mapping from prefix to its current next hop
	private final Map<AtriumIpPrefix, AtriumIpAddress> prefixToNextHop = Maps.newHashMap();

	// Mapping from next hop IP to next hop object containing group info
	private final Map<AtriumIpAddress, Integer> nextHops = Maps.newHashMap();

	// Stores FIB updates that are waiting for groups to be set up
	private final Multimap<AtriumNextHopGroupKey, AtriumFibEntry> pendingUpdates = HashMultimap.create();

	// Device id of data-plane switch - should be learned from config
	private NodeId deviceId;

	// Device id of control-plane switch (OVS) connected to BGP Speaker - should
	// be learned from config
	private NodeId ctrlDeviceId;

	// Responsible for handling BGP traffic (encapsulated within OF messages)
	// between the data-plane switch and the Quagga VM using a control plane
	// OVS.
	private TunnellingConnectivityManager connectivityManager;

	// Listens for device add/delete events
	private DeviceListener deviceListener;

	// Reads the bgp router static configuration and writes into data store.
	private RoutingConfigService configService;

	// BGP Speaker service which listens for I-BGP updates
	private RoutingService routingService;

	// Flow objectives implementation which invokes the right driver for pushing
	// flows to switches.
	private AtriumFlowObjectiveService flowObjectivesService;

	private ListenerRegistration<DataChangeListener> listenerRegistration;

	// For tunneling the BGP packets between CP and DP switches
	private PacketProcessingService packetService;

	// MD-SAL databroker for accessing data store
	private DataBroker dataBroker;

	// NextObj nextId generator
	NextIdGenerator nxtGenerator = new NextIdGenerator();

	/*
	 * (non-Javadoc)
	 *
	 * @see org.opendaylight.controller.sal.binding.api.BindingAwareProvider#
	 * onSessionInitiated
	 * (org.opendaylight.controller.sal.binding.api.BindingAwareBroker
	 * .ProviderContext)
	 */

	/**
	 * Instantiates a new bgp router.
	 *
	 * @param connectivityManager
	 *            the connectivity manager
	 * @param dataBroker
	 *            the data broker
	 * @param routingConfigService
	 *            the routing config service
	 * @param routingService
	 *            the routing service
	 * @param packetService
	 *            the packet service
	 */
	public Bgprouter(TunnellingConnectivityManager connectivityManager, DataBroker dataBroker,
			RoutingConfigService routingConfigService, RoutingService routingService,
			PacketProcessingService packetService, AtriumFlowObjectiveService flowObjectives) {
		this.connectivityManager = connectivityManager;
		this.dataBroker = dataBroker;
		this.routingService = routingService;
		this.configService = routingConfigService;
		this.packetService = packetService;
		this.flowObjectivesService = flowObjectives;

	}

	/**
	 * Call back function which gets invoked on feature module initialization
	 *
	 * @param ctxt
	 *
	 *
	 */
	@Override
	public void onSessionInitialized(ConsumerContext ctxt) {
		LOG.info("BgprouterProvider Session Initiated");
	}

	/**
	 * Start the BGP Router service
	 *
	 */
	public void start() {

		LOG.info("Starting BGP Router");

		getDeviceConfiguration(configService.getBgpSpeakers());

		deviceListener = new DeviceListener(dataBroker, this);

		routingService.addFibListener(new InternalFibListener());

		routingService.start();

		connectivityManager.start();

		// Initialize devices now if they are already connected
		if (AtriumUtils.isNodeAvailable(dataBroker, deviceId)) {
			LOG.info("Node: " + deviceId + " is discovered. Adding default flows");
			processIntfFilters(true, configService.getInterfaces());
		}

		if (AtriumUtils.isNodeAvailable(dataBroker, ctrlDeviceId)) {
			LOG.info("Node: " + ctrlDeviceId + " is discovered. Adding default flows");
			connectivityManager.notifySwitchAvailable();
		}

		LOG.info("BGP Router started");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		LOG.info("Stopping BGP Router Application");
		routingService.stop();
		connectivityManager.stop();
		if (listenerRegistration != null) {
			try {
				listenerRegistration.close();
			} catch (final Exception e) {
				LOG.error("Error when cleaning up DataChangeListener.", e);
			}
			listenerRegistration = null;
		}
		LOG.info("BgprouterProvider Closed");
	}

	/**
	 * Gets the device configuration.
	 *
	 * @param bgps
	 *            the BgpSpeakers
	 * @return none
	 */
	private void getDeviceConfiguration(BgpSpeakers bgps) {

		if (bgps == null) {
			LOG.error("BGP speakers configuration is missing");
			return;
		}

		List<BgpSpeaker> bgpSpeakers = bgps.getBgpSpeaker();
		if (bgpSpeakers == null || bgpSpeakers.isEmpty()) {
			LOG.error("BGP speakers configuration is missing");
			return;
		}

		Optional<BgpSpeaker> bgpSpeaker = bgps.getBgpSpeaker().stream().findAny();
		ctrlDeviceId = bgpSpeaker.get().getAttachmentDpId();

		Optional<InterfaceAddresses> intfAddress = bgpSpeaker.get().getInterfaceAddresses().stream().findAny();
		if (!intfAddress.isPresent()) {
			LOG.error("Could not find peer interface addresses in router configuration");
			return;
		}

		NodeConnectorId ncId = intfAddress.get().getOfPortId();
		if (ncId == null) {
			LOG.error("ncId is null");
			return;
		}

		String dpn = AtriumUtils.getDpnFromNodeConnectorId(ncId);
		if (dpn != null) {
			BigInteger dpnId = new BigInteger(dpn);
			deviceId = AtriumUtils.buildDpnNodeId(dpnId);
		}

		LOG.info("Router dpid: {}", deviceId);

		LOG.info("Control Plane OVS dpid: {}", ctrlDeviceId);
	}

	/**
	 * Received FIB updates after resolving NH MAC address and initiates a flow
	 * insertion process
	 *
	 * @param updates
	 *            the updates
	 */
	private void updateFibEntry(Collection<AtriumFibUpdate> updates) {
		Map<AtriumFibEntry, Integer> toInstall = new HashMap<>(updates.size());

		for (AtriumFibUpdate update : updates) {
			AtriumFibEntry entry = update.entry();

			addNextHop(entry);

			Integer nextId;
			synchronized (nextHops) {
				nextId = nextHops.get(entry.nextHopIp());
			}

			toInstall.put(update.entry(), nextId);
			FibDataModelWriter.updateFib(update, dataBroker);
		}

		installFlows(toInstall);
	}

	/**
	 * Install flows.
	 *
	 * @param entriesToInstall
	 *            the entries to install
	 */
	private void installFlows(Map<AtriumFibEntry, Integer> entriesToInstall) {

		checkNotNull(flowObjectivesService, "FlowObjectives Service not initialized");

		for (Map.Entry<AtriumFibEntry, Integer> entry : entriesToInstall.entrySet()) {
			AtriumFibEntry fibEntry = entry.getKey();
			Integer nextId = entry.getValue();

			ForwardingObjectiveBuilder forwardingObjBuilder = generateRibForwardingObj(fibEntry.prefix(), nextId);
			NodeRef nodeRef = new NodeRef(
					InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(deviceId)).build());

			if (forwardingObjBuilder != null) {
				ForwardInputBuilder inputBuilder = new ForwardInputBuilder();
				forwardingObjBuilder.setOperation(Operation.Add);
				inputBuilder.setNode(nodeRef);
				inputBuilder.setForwardingObjective(forwardingObjBuilder.build());
				LOG.info("Invoking forward objective in DIDM for fibEntry update");
				LOG.info("FIB Entry: " + forwardingObjBuilder.build());
				try {
					Future<RpcResult<Void>> result = flowObjectivesService.forward(inputBuilder.build());
					RpcResult<Void> rpcResult = result.get();
					if (rpcResult.isSuccessful()) {
						LOG.info("FibEntry update sent to flowObjective");
					} else {
						LOG.info("Failed to send FibEntry to flowObjective");
					}
				} catch (Exception ex) {
					LOG.info("", ex);
				}
			} else {
				continue;
			}
		}
	}

	/**
	 * Delete fib entry.
	 *
	 * @param withdraws
	 *            the withdraws
	 */
	private synchronized void deleteFibEntry(Collection<AtriumFibUpdate> withdraws) {

		for (AtriumFibUpdate update : withdraws) {
			AtriumFibEntry entry = update.entry();
			NodeRef nodeRef = new NodeRef(
					InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(deviceId)).build());

			ForwardingObjectiveBuilder forwardingObjBuilder = generateRibForwardingObj(entry.prefix(), null);
			if (forwardingObjBuilder != null) {
				ForwardInputBuilder inputBuilder = new ForwardInputBuilder();
				forwardingObjBuilder.setOperation(Operation.Remove);
				inputBuilder.setNode(nodeRef);
				inputBuilder.setForwardingObjective(forwardingObjBuilder.build());
				LOG.info("Invoking forward objective in DIDM for fibEntry delete");
				LOG.info("FIB Entry: " + forwardingObjBuilder.build());
				flowObjectivesService.forward(inputBuilder.build());
				FibDataModelWriter.deleteFib(update, dataBroker);

			} else {
				continue;
			}
		}

	}

	private ForwardingObjectiveBuilder generateRibForwardingObj(AtriumIpPrefix prefix, Integer nextId) {
		ForwardingObjectiveBuilder forwardingObjBuilder = new ForwardingObjectiveBuilder();
		MatchBuilder matchBuilder = new MatchBuilder();

		// set Ethernet type - IPv4
		EthernetMatch etherMatch = AtriumUtils.getEtherMatch(IPV4_ETH_TYPE);
		matchBuilder.setEthernetMatch(etherMatch);

		// set IP DST - prefix
		Layer3Match l3Match = AtriumUtils.createLayer3Match(prefix, false);
		matchBuilder.setLayer3Match(l3Match);

		forwardingObjBuilder.setMatch(matchBuilder.build());

		// set priority
		int priority = prefix.prefixLength() * PRIORITY_MULTIPLIER + PRIORITY_OFFSET;

		forwardingObjBuilder.setPriority(Integer.valueOf(priority));
		forwardingObjBuilder.setFlag(Flag.Specific);
		if (nextId != null) {
			forwardingObjBuilder.setNextId(nextId);
		}
		return forwardingObjBuilder;
	}

	/**
	 * Adds the next hop.
	 *
	 * @param entry
	 *            the entry
	 */
	private synchronized void addNextHop(AtriumFibEntry entry) {
		prefixToNextHop.put(entry.prefix(), entry.nextHopIp());
		// This check is not done currently as there is an issue wherein after
		// the router/ovs instance is restarted ODL is not installing nexthop
		// groups.
		// if (nextHopsCount.count(entry.nextHopIp()) == 0) {
		AtriumInterface egressIntf = configService.getMatchingInterface(
				org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddressBuilder
						.getDefaultInstance(entry.nextHopIp().toString()));

		if (egressIntf == null) {
			LOG.warn("no egress interface found for {}", entry);
			return;
		}

		AtriumNextHopGroupKey groupKey = new AtriumNextHopGroupKey(entry.nextHopIp());

		AtriumNextHop nextHop = new AtriumNextHop(entry.nextHopIp(), entry.nextHopMac(), groupKey);

		List<TrafficTreatment> treatment = getTrafficTreatmentForNextObj(egressIntf, nextHop);

		NextObjectiveBuilder nextObjBuilder = new NextObjectiveBuilder();

		nextObjBuilder.setOperation(Operation.Add);
		nextObjBuilder.setType(Type.Simple);
		nextObjBuilder.setTrafficTreatment(treatment);
		NodeRef nodeRef = new NodeRef(
				InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(deviceId)).build());

		int nextId = nxtGenerator.allocateNextId();

		nextObjBuilder.setNextId(Integer.valueOf(nextId));

		NextInputBuilder inputBuilder = new NextInputBuilder();
		inputBuilder.setNode(nodeRef);
		inputBuilder.setNextObjective(nextObjBuilder.build());
		flowObjectivesService.next(inputBuilder.build());

		nextHops.put(entry.nextHopIp(), nextId);
		
		// }
		// nextHopsCount.add(entry.nextHopIp());
	}

	private List<TrafficTreatment> getTrafficTreatmentForNextObj(AtriumInterface egressInterface, AtriumNextHop nextHop) {
		List<TrafficTreatment> treatment = new ArrayList<TrafficTreatment>();
		List<ActionData> actions = new ArrayList<ActionData>();

		MacAddress dstMac = new MacAddress(nextHop.mac().toString());
		MacAddress srcMac = new MacAddress(egressInterface.mac());
		String outPort = AtriumUtils.getPortNoFromNodeConnectorId(egressInterface.connectPoint().getId());

		// Set Eth Dest
		actions.add(new ActionData(ActionUtils.set_field_eth_dest, new String[] { dstMac.getValue() }));

		// Set Eth Src
		actions.add(new ActionData(ActionUtils.set_field_eth_src, new String[] { srcMac.getValue() }));

		// Push VLAN
		actions.add(new ActionData(ActionUtils.push_vlan, new String[] { null }));

		// Set VLAN ID
		actions.add(new ActionData(ActionUtils.set_field_vlan_vid,
				new String[] { String.valueOf(egressInterface.vlan()) }));

		// Set out port
		actions.add(new ActionData(ActionUtils.output, new String[] { outPort }));

		List<Action> actionList = new ArrayList<Action>();
		for (ActionData action : actions) {
			actionList.add(action.buildAction());
		}
		TrafficTreatment trtment = new TrafficTreatmentBuilder().setAction(actionList).build();
		treatment.add(trtment);
		return treatment;
	}

	/**
	 * The listener interface for receiving internalFib events. The class that
	 * is interested in processing a internalFib event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's <code>addInternalFibListener
	 * <code> method. When the internalFib event occurs, that object's
	 * appropriate method is invoked.
	 *
	 * @see InternalFibEvent
	 */
	private class InternalFibListener implements FibListener {

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.opendaylight.atrium.routingservice.api.FibListener#update(java
		 * .util.Collection, java.util.Collection)
		 */
		@Override
		public void update(Collection<AtriumFibUpdate> updates, Collection<AtriumFibUpdate> withdraws) {
			Bgprouter.this.deleteFibEntry(withdraws);
			Bgprouter.this.updateFibEntry(updates);
		}
	}

	/**
	 * Process intf filters.
	 *
	 * @param install
	 *            the install
	 * @param intfs
	 *            the intfs
	 */
	private void processIntfFilters(boolean install, Set<AtriumInterface> intfs) {
		LOG.info("Processing {} router interfaces", intfs.size());
		for (AtriumInterface intf : intfs) {
			NodeConnector connector = intf.connectPoint();
			if (connector == null) {
				continue;
			}
			String dpn = AtriumUtils.getDpnFromNodeConnectorId(connector.getId());
			BigInteger dpnId = new BigInteger(dpn);
			LOG.info("DpnId: " + deviceId);
			NodeId routerId = AtriumUtils.buildDpnNodeId(dpnId);
			LOG.info("RouterId: " + routerId);
			if (!routerId.equals(deviceId)) {
				// Ignore interfaces if they are not on the router switch
				continue;
			}

			NodeRef nodeRef = new NodeRef(
					InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(deviceId)).build());

			FilterObjectiveBuilder filterObjBuilder = gnerateFilterObjectiveBuilder(intf);

			FilterInputBuilder filterBuilder = new FilterInputBuilder();
			filterBuilder.setFilterObjective(filterObjBuilder.build());
			filterBuilder.setNode(nodeRef);
			LOG.info("Invoking filter objective with values: " + filterBuilder.build());
			flowObjectivesService.filter(filterBuilder.build());
		}

	}

	private FilterObjectiveBuilder gnerateFilterObjectiveBuilder(AtriumInterface intf) {

		checkNotNull(intf);
		Set<AtriumInterfaceIpAddress> ipAddresses = intf.ipAddresses();
		MacAddress ethDstMac = intf.mac();
		AtriumVlanId vlanId = intf.vlan();

		FilterObjectiveBuilder fobjBuilder = new FilterObjectiveBuilder();

		// Match ethernet destination
		EthernetMatch ethMatch = AtriumUtils.getEtherMatch(ethDstMac, IPV4_ETH_TYPE, false);

		// Match vlan
		VlanMatch vlanMatch = AtriumUtils.getVlanMatch(vlanId.toShort());

		fobjBuilder.setEthernetMatch(ethMatch);
		fobjBuilder.setVlanMatch(vlanMatch);
		fobjBuilder.setInPort(intf.connectPoint().getId());

		// Match IP Dst
		for (AtriumInterfaceIpAddress infAddr : ipAddresses) {
			Ipv4Match l3Match = AtriumUtils.getL3Match(infAddr, false);
			fobjBuilder.setLayer3Match(l3Match);
		}
		return fobjBuilder;
	}

	/**
	 * Callback for device add
	 *
	 * @param dpnId
	 */
	public void processNodeAdd(NodeId dpnId) {

		try {
			Thread.sleep(3000);
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}

		// s1
		if (dpnId.equals(ctrlDeviceId)) {
			connectivityManager.notifySwitchAvailable();
			addIcmpFlowToController(dpnId);
		}

		// router
		if (dpnId.equals(deviceId)) {
			processIntfFilters(true, configService.getInterfaces());
		}

		addArpFlowToController(dpnId);

	}

	public void addArpFlowToController(NodeId dpnId) {

		NodeRef nodeRef = new NodeRef(
				InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(dpnId)).build());

		ForwardingObjectiveBuilder fwdObjBuilder = new ForwardingObjectiveBuilder();
		fwdObjBuilder.setOperation(Operation.Add);
		fwdObjBuilder.setFlag(Flag.Versatile);
		MatchBuilder matchBuilder = new MatchBuilder();
		EthernetMatch etherMatch = AtriumUtils.getEtherMatch(Bgprouter.ARP_ETH_TYPE);
		matchBuilder.setEthernetMatch(etherMatch);

		ActionData puntAction = new ActionData(ActionUtils.punt_to_controller, new String[] { null });

		fwdObjBuilder.setMatch(matchBuilder.build());
		List<Action> actions = new ArrayList<>();
		actions.add(puntAction.buildAction());
		fwdObjBuilder.setAction(actions);

		ForwardInputBuilder forwardInputBuilderSrc = new ForwardInputBuilder();
		forwardInputBuilderSrc.setNode(nodeRef);
		forwardInputBuilderSrc.setForwardingObjective(fwdObjBuilder.build());
		flowObjectivesService.forward(forwardInputBuilderSrc.build());

	}

	public void addIcmpFlowToController(NodeId dpnId) {

		NodeRef nodeRef = new NodeRef(
				InstanceIdentifier.builder(Nodes.class).child(Node.class, new NodeKey(dpnId)).build());

		ForwardingObjectiveBuilder fwdObjBuilder = new ForwardingObjectiveBuilder();
		fwdObjBuilder.setOperation(Operation.Add);
		fwdObjBuilder.setFlag(Flag.Versatile);
		MatchBuilder matchBuilder = new MatchBuilder();

		// set Ethernet type - IPv4
		EthernetMatch etherMatch = AtriumUtils.getEtherMatch(Bgprouter.IPV4_ETH_TYPE);
		matchBuilder.setEthernetMatch(etherMatch);

		// Ip type Match
		IpMatch ipMatch = AtriumUtils.getIcmpIpMatchType();
		matchBuilder.setIpMatch(ipMatch);

		ActionData puntAction = new ActionData(ActionUtils.punt_to_controller, new String[] { null });

		fwdObjBuilder.setMatch(matchBuilder.build());
		List<Action> actions = new ArrayList<>();
		actions.add(puntAction.buildAction());
		fwdObjBuilder.setAction(actions);

		ForwardInputBuilder forwardInputBuilderSrc = new ForwardInputBuilder();
		forwardInputBuilderSrc.setNode(nodeRef);
		forwardInputBuilderSrc.setForwardingObjective(fwdObjBuilder.build());
		flowObjectivesService.forward(forwardInputBuilderSrc.build());

	}

	class NextIdGenerator {
		int counter = 1;

		public int allocateNextId() {
			return counter++;
		}
	}
}
