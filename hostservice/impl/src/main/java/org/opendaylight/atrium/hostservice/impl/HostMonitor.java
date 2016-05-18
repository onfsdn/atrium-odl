/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.opendaylight.atrium.hostservice.api.ArpMessageAddress;
import org.opendaylight.atrium.hostservice.api.Host;
import org.opendaylight.atrium.hostservice.api.HostEvent;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.hostservice.api.HostUpdatesListener;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumMacAddress;
import org.opendaylight.atrium.util.AtriumTimer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

public class HostMonitor implements TimerTask {
	private static final Logger LOG = LoggerFactory.getLogger(HostMonitor.class);
	private static final long DEFAULT_PROBE_RATE = 30000; // milliseconds
	private static final byte[] ZERO_MAC_ADDRESS = AtriumMacAddress.ZERO.toBytes();
	private long probeRate = DEFAULT_PROBE_RATE;

	private Timeout timeout;

	private Set<AtriumIpAddress> monitoredAddresses = Collections.newSetFromMap(new ConcurrentHashMap<>());;
	private ListenerRegistration<DataChangeListener> hostNodeListerRegistration;
	private HostUpdatesListener hostUpdatesListener;
	private HostService hostService;
	private static final String TOPOLOGY_NAME = "flow:1";
	private static final int CPUS = Runtime.getRuntime().availableProcessors();

	private DataBroker dataService;
	private String topologyId;
	private PacketProcessingService packetService;
	private ArpSender arpSender;
	private RoutingConfigService configService;

	ExecutorService exec = Executors.newFixedThreadPool(CPUS);

	public HostMonitor(HostUpdatesListener hostUpdatesListener, HostService hostService, DataBroker dataService,
			RoutingConfigService configService, PacketProcessingService packetService) {
		this.dataService = dataService;
		this.hostUpdatesListener = hostUpdatesListener;
		this.hostService = hostService;
		this.packetService = packetService;
		if (topologyId == null || topologyId.isEmpty()) {
			this.topologyId = TOPOLOGY_NAME;
		} else {
			this.topologyId = topologyId;
		}
		arpSender = new ArpSender(packetService);
	}

	/**
	 * Adds an IP address to be monitored by the host monitor. The monitor will
	 * periodically probe the host to detect changes.
	 *
	 * @param ip
	 *            IP address of the host to monitor
	 */
	void addMonitoringFor(AtriumIpAddress ip) {
		synchronized (monitoredAddresses) {
			monitoredAddresses.add(ip);
		}
	}

	/**
	 * Stops monitoring the given IP address.
	 *
	 * @param ip
	 *            IP address to stop monitoring on
	 */
	void stopMonitoring(AtriumIpAddress ip) {
		synchronized (monitoredAddresses) {
			monitoredAddresses.remove(ip);
		}
	}

	/**
	 * Starts the host monitor. Does nothing if the monitor is already running.
	 */
	void start() {
		LOG.info("Starting Host Monitor Service");
		synchronized (this) {
			if (timeout == null) {
				timeout = AtriumTimer.getTimer().newTimeout(this, 0, TimeUnit.MILLISECONDS);
			}
		}
		// registerAsDataChangeListener();
		LOG.info("Started Host Monitor Service");

	}

	/**
	 * Stops the host monitor.
	 */
	void shutdown() {
		synchronized (this) {
			if (timeout != null) {
				timeout.cancel();
				timeout = null;
			}
		}
	}

	@Override
	public void run(Timeout arg0) throws Exception {

		LOG.info("**Timer triggered**");

		List<AtriumIpAddress> resolvedIps = new ArrayList<>();
		for (AtriumIpAddress ip : monitoredAddresses) {
			Host host = hostService.getHost(new HostId(ip.toString()));
			if (host == null) {
				LOG.info("Host not found.Sending ARP request for:" + ip);
				// TODO : check if we can resolve from config
				sendArpRequest(ip);
			} else {
				LOG.info("IP resolved:" + ip);
				HostEvent hostEvent = new HostEvent(HostEvent.Type.HOST_ADDED, host);
				resolvedIps.add(ip);
				hostUpdatesListener.sendHostAddEvent(hostEvent);
			}
		}

		if (resolvedIps != null && !resolvedIps.isEmpty()) {
			for (AtriumIpAddress resolvedIp : resolvedIps) {
				synchronized (monitoredAddresses) {
					monitoredAddresses.remove(resolvedIp);
				}
			}
			resolvedIps.clear();
		}
		LOG.info("setting timer again");
		this.timeout = AtriumTimer.getTimer().newTimeout(this, probeRate, TimeUnit.MILLISECONDS);
	}

	private void sendArpRequest(AtriumIpAddress ip) {
		checkNotNull(ip, "ipaddress for ARP flood is null");
		LOG.info("MAC not found sending ARP flood");
		List<BgpSpeaker> speakers = configService.getBgpSpeakers().getBgpSpeaker();

		for (BgpSpeaker bgpSpeaker : speakers) {
			org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress mac = bgpSpeaker
					.getMacAddress();

			List<InterfaceAddresses> addresses = bgpSpeaker.getInterfaceAddresses();
			for (InterfaceAddresses address : addresses) {
				InterfaceAddressesBuilder builder = new InterfaceAddressesBuilder(address);
				Ipv4Address ipAddr = builder.getIpAddress().getIpv4Address();
				ArpMessageAddress arpSenderAddress = new ArpMessageAddress(mac, ipAddr);
				NodeId nodeId = bgpSpeaker.getAttachmentDpId();
				InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeIid = InstanceIdentifier
						.builder(Nodes.class)
						.child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
								new NodeKey(nodeId))
						.build();

				arpSender.floodArp(arpSenderAddress, new Ipv4Address(ip.toString()), nodeIid);

			}
		}

	}

	// TODO: check if we need to directly monitor ARP requests and update host
	// list (just like in l2switch/hostracker)
	public void packetReceived(ConnectorAddress addrs, InstanceIdentifier<?> ii) {
		InstanceIdentifier<NodeConnector> iinc = ii.firstIdentifierOf(NodeConnector.class);
		InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> iin//
		= ii.firstIdentifierOf(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class);

		ListenableFuture<Optional<NodeConnector>> futureNodeConnector;
		ListenableFuture<Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>> futureNode;
		try (ReadOnlyTransaction readTx = dataService.newReadOnlyTransaction()) {
			futureNodeConnector = readTx.read(LogicalDatastoreType.OPERATIONAL, iinc);
			futureNode = readTx.read(LogicalDatastoreType.OPERATIONAL, iin);
			readTx.close();
		}
		Optional<NodeConnector> opNodeConnector = null;
		Optional<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> opNode = null;
		try {
			opNodeConnector = futureNodeConnector.get();
			opNode = futureNode.get();
		} catch (ExecutionException | InterruptedException ex) {
			LOG.warn(ex.getLocalizedMessage());
		}
		if (opNode != null && opNode.isPresent() && opNodeConnector != null && opNodeConnector.isPresent()) {
			processHost(opNode.get(), opNodeConnector.get(), addrs);
		}
	}

	private void processHost(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
			NodeConnector nodeConnector, ConnectorAddress addrs) {
		if (!isNodeConnectorInternal(nodeConnector)) {
			Host host = new Host(addrs, nodeConnector);
			addHost(addrs, nodeConnector);
		}
	}

	private boolean isNodeConnectorInternal(NodeConnector nodeConnector) {
		TpId tpId = new TpId(nodeConnector.getKey().getId().getValue());
		InstanceIdentifier<NetworkTopology> ntII = InstanceIdentifier.builder(NetworkTopology.class).build();
		ListenableFuture<Optional<NetworkTopology>> lfONT;
		try (ReadOnlyTransaction rot = dataService.newReadOnlyTransaction()) {
			lfONT = rot.read(LogicalDatastoreType.OPERATIONAL, ntII);
			rot.close();
		}
		Optional<NetworkTopology> oNT;
		try {
			oNT = lfONT.get();
		} catch (InterruptedException | ExecutionException ex) {
			LOG.warn(ex.getLocalizedMessage());
			return false;
		}
		if (oNT != null && oNT.isPresent()) {
			NetworkTopology networkTopo = oNT.get();
			for (Topology t : networkTopo.getTopology()) {
				if (t.getLink() != null) {
					for (Link l : t.getLink()) {
						if ((l.getSource().getSourceTp().equals(tpId)
								&& !l.getDestination().getDestTp().getValue().startsWith(Host.NODE_PREFIX))
								|| (l.getDestination().getDestTp().equals(tpId)
										&& !l.getSource().getSourceTp().getValue().startsWith(Host.NODE_PREFIX))) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	private void addHost(ConnectorAddress address, NodeConnector nodeConnector) {
		checkNotNull(address);
		checkNotNull(nodeConnector);
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address ipv4Address = address
				.getIp().getIpv4Address();
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address ipv6Address = address
				.getIp().getIpv6Address();
		HostId hostId = null;
		if (ipv4Address != null) {
			org.opendaylight.atrium.util.AtriumIpAddress ip = org.opendaylight.atrium.util.AtriumIpAddress
					.valueOf(ipv4Address.getValue());
			hostId = new HostId(ipv4Address.getValue());
			if (hostService.getHost(hostId) == null) {
				Host host = new Host(address, nodeConnector);
				hostUpdatesListener.addHost(hostId, host);
			}
		} else if (ipv6Address != null) {
			org.opendaylight.atrium.util.AtriumIpAddress ip = org.opendaylight.atrium.util.AtriumIpAddress
					.valueOf(ipv6Address.getValue());
			hostId = new HostId(ipv6Address.getValue());
			if (hostService.getHost(hostId) == null) {
				Host host = new Host(address, nodeConnector);
				hostUpdatesListener.addHost(hostId, host);
			}
		}

	}

	private void addHost(ConnectorAddress address, Host host) {
		checkNotNull(address);
		checkNotNull(host);
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address ipv4Address = address
				.getIp().getIpv4Address();
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address ipv6Address = address
				.getIp().getIpv6Address();
		HostId hostId = null;
		if (ipv4Address != null) {
			org.opendaylight.atrium.util.AtriumIpAddress ip = org.opendaylight.atrium.util.AtriumIpAddress
					.valueOf(ipv4Address.getValue());
			hostId = new HostId(ip.toString());
			if (hostService.getHost(hostId) == null) {
				hostUpdatesListener.addHost(hostId, host);
			}
		}

		if (ipv6Address != null) {
			org.opendaylight.atrium.util.AtriumIpAddress ip = org.opendaylight.atrium.util.AtriumIpAddress
					.valueOf(ipv6Address.getValue());
			hostId = new HostId(ipv6Address.getValue());
			if (hostService.getHost(hostId) == null) {
				hostUpdatesListener.addHost(hostId, host);
			}
		}
	}

	private void deleteHost(ConnectorAddress address) {
		checkNotNull(address);
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address ipv4Address = address
				.getIp().getIpv4Address();
		org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address ipv6Address = address
				.getIp().getIpv6Address();
		HostId hostId = null;
		if (ipv4Address != null) {
			org.opendaylight.atrium.util.AtriumIpAddress ip = org.opendaylight.atrium.util.AtriumIpAddress
					.valueOf(ipv4Address.getValue());
			hostId = new HostId(ip.toString());
			hostUpdatesListener.deleteHost(hostId);
		}
		if (ipv6Address != null) {
			org.opendaylight.atrium.util.AtriumIpAddress ip = org.opendaylight.atrium.util.AtriumIpAddress
					.valueOf(ipv6Address.getValue());
			hostId = new HostId(ip.toString());
			hostUpdatesListener.deleteHost(hostId);
		}
	}

}
