/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.atrium.routingservice.config.api.LocalIpPrefixEntry;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.util.AtriumInterface;
import org.opendaylight.atrium.util.AtriumInterfaceIpAddress;
import org.opendaylight.atrium.util.AtriumIp4Address;
import org.opendaylight.atrium.util.AtriumIp4Prefix;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.util.AtriumVlanId;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpPeers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeakerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.addresses.Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

public class RoutingConfigServiceImpl implements RoutingConfigService, BindingAwareProvider, AutoCloseable {

	private final Logger log = LoggerFactory.getLogger(getClass());
	private static final String DEFAULT_CONFIG_FILE = "./configuration/initial/sdnip.json";

	private Map<MacAddress, BgpSpeaker> bgpSpeakerMap = new ConcurrentHashMap<>();
	private Map<IpAddress, BgpPeer> bgpPeerMap = new ConcurrentHashMap<>();

	private InvertedRadixTree<LocalIpPrefixEntry> localPrefixTable4 = new ConcurrentInvertedRadixTree<>(
			new DefaultByteArrayNodeFactory());
	private InvertedRadixTree<LocalIpPrefixEntry> localPrefixTable6 = new ConcurrentInvertedRadixTree<>(
			new DefaultByteArrayNodeFactory());

	private DataBroker dataBroker;

	private static final ScheduledExecutorService EXECUTORSERVICE = MoreExecutors
			.listeningDecorator(Executors.newScheduledThreadPool(1));

	public RoutingConfigServiceImpl(DataBroker dataBroker) {
		this.dataBroker = dataBroker;
	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		log.info("RoutingserviceConfiguration Session Initiated");
		readConfiguration();
	}

	private void readConfiguration() {
		log.info("reading configuration");

		URL configFileUrl = null;
		try {
			configFileUrl = new File(DEFAULT_CONFIG_FILE).toURI().toURL();
		} catch (Exception ex) {
			log.error("Error reading configuration file " + DEFAULT_CONFIG_FILE);
			return;
		}
		if (configFileUrl == null) {
			return;
		}

		boolean isSuccess = ConfigReader.initialize(configFileUrl);
		if (isSuccess) {
			BgpSpeakers bgpSpeakers = ConfigReader.getBgpSpeakers();
			BgpPeers bgpPeers = ConfigReader.getBgpPeer();
			ConfigWriter.writeBgpConfigData(dataBroker, bgpSpeakers, bgpPeers);

			for (BgpSpeaker bgpSpeaker : bgpSpeakers.getBgpSpeaker()) {
				bgpSpeakerMap.put(bgpSpeaker.getMacAddress(), bgpSpeaker);
			}

			for (BgpPeer bgpPeer : bgpPeers.getBgpPeer()) {
				bgpPeerMap.put(bgpPeer.getPeerAddr(), bgpPeer);
			}

		} else {
			log.error("Error reading configuration file " + DEFAULT_CONFIG_FILE);
		}
	}

	@Override
	public boolean isIpAddressLocal(IpAddress ipAddress) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isIpPrefixLocal(AtriumIpPrefix ipPrefix) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void close() throws Exception {
		log.info("RoutingserviceProvider Closed");
	}

	@Override
	public BgpSpeakers getBgpSpeakers() {
		BgpSpeakers localBgpSpeakers = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<BgpSpeakers> bgpSpeakersBuilder = InstanceIdentifier.builder(BgpSpeakers.class).build();

		try {
			ListenableFuture<Optional<BgpSpeakers>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, bgpSpeakersBuilder);
			Optional<BgpSpeakers> oNT = lfONT.get();
			localBgpSpeakers = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}
		return localBgpSpeakers;
	}

	@Override
	public BgpPeers getBgpPeers() {
		BgpPeers localBgpPeers = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<BgpPeers> bgpPeersBuilder = InstanceIdentifier.builder(BgpPeers.class).build();

		try {
			ListenableFuture<Optional<BgpPeers>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, bgpPeersBuilder);
			Optional<BgpPeers> oNT = lfONT.get();
			localBgpPeers = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}
		return localBgpPeers;
	}

	@Override
	public BgpSpeaker getBgpSpeakerByMac(String mac) {
		BgpSpeaker bgpSpeaker = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		MacAddress macAddress = new MacAddress(mac);

		BgpSpeakerKey bgpSpeakerKey = new BgpSpeakerKey(macAddress);
		InstanceIdentifier<BgpSpeaker> bgpSpeakerBuilder = InstanceIdentifier.builder(BgpSpeakers.class)
				.child(BgpSpeaker.class, bgpSpeakerKey).build();

		try {
			ListenableFuture<Optional<BgpSpeaker>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, bgpSpeakerBuilder);
			Optional<BgpSpeaker> oNT = lfONT.get();
			bgpSpeaker = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}

		return bgpSpeaker;
	}

	@Override
	public BgpPeer getBgpPeerByIpAddress(IpAddress ip) {
		if (ip == null) {
			return null;
		}

		BgpPeer bgpPeer = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();

		// IpAddress ipAddress = new IpAddress(new Ipv4Address(ip));

		BgpPeerKey bgpPeerKey = new BgpPeerKey(ip);

		InstanceIdentifier<BgpPeer> bgpPeerBuilder = InstanceIdentifier.builder(BgpPeers.class)
				.child(BgpPeer.class, bgpPeerKey).build();

		try {
			ListenableFuture<Optional<BgpPeer>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, bgpPeerBuilder);
			Optional<BgpPeer> oNT = lfONT.get();
			bgpPeer = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}
		return bgpPeer;
	}

	@Override
	public AtriumInterface getInterface(NodeConnector connectPoint) {

		NodeConnectorId connectorId = connectPoint.getId();
		String splitArray[] = connectorId.getValue().split(":");

		String dpid = splitArray[0] + ":" + splitArray[1];
		String ofPortId = splitArray[2];

		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();

		InstanceIdentifier<Addresses> addressBuilder = InstanceIdentifier.builder(Addresses.class).build();

		Addresses addresses = null;

		try {
			ListenableFuture<Optional<Addresses>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, addressBuilder);
			Optional<Addresses> oNT = lfONT.get();
			addresses = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}

		for (Address address : addresses.getAddress()) {

			if (dpid.equals(address.getDpid()) && ofPortId.equals(address.getOfPortId().getValue())) {
				MacAddress mac = address.getMac();
				AtriumVlanId vlanId = AtriumVlanId.vlanId(address.getVlan().shortValue());

				IpAddress ipAddress = address.getIpAddress();
				AtriumIp4Address ip4Address = AtriumIp4Address.valueOf(ipAddress.getIpv4Address().getValue());

				// TODO
				// Include subnet in yang
				AtriumIp4Prefix ip4Prefix = AtriumIp4Prefix.valueOf(ip4Address.getIp4Address().toString() + "/24");
				AtriumInterfaceIpAddress interfaceIpAddress = new AtriumInterfaceIpAddress(ip4Address, ip4Prefix);
				Set<AtriumInterfaceIpAddress> interfaceIpAddressSet = new HashSet<>();
				interfaceIpAddressSet.add(interfaceIpAddress);

				AtriumInterface matchingInterface = new AtriumInterface(connectPoint, interfaceIpAddressSet, mac, vlanId);
				return matchingInterface;
			}
		}

		return null;
	}

	@Override
	public AtriumInterface getMatchingInterface(IpAddress ipAddress) {

		AtriumInterface matchingInterface = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();

		BgpPeerKey bgpPeerKey = new BgpPeerKey(ipAddress);
		InstanceIdentifier<BgpPeer> bgpPeersBuilder = InstanceIdentifier.builder(BgpPeers.class)
				.child(BgpPeer.class, bgpPeerKey).build();

		String peerDpId = null;
		String peerPort = null;
		try {
			ListenableFuture<Optional<BgpPeer>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, bgpPeersBuilder);
			Optional<BgpPeer> oNT = lfONT.get();
			BgpPeer bgpPeer = oNT.get();
			peerDpId = bgpPeer.getPeerDpId().getValue();
			peerPort = bgpPeer.getPeerPort().toString();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}

		Addresses addresses = null;
		InstanceIdentifier<Addresses> addressBuilder = InstanceIdentifier.builder(Addresses.class).build();
		try {
			ListenableFuture<Optional<Addresses>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, addressBuilder);
			Optional<Addresses> oNT = lfONT.get();
			addresses = oNT.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}

		for (Address address : addresses.getAddress()) {

			String addressDpId = address.getDpid();
			String addressPort = address.getOfPortId().getValue();

			if (peerDpId.equals(addressDpId) && peerPort.equals(addressPort)) {

				MacAddress mac = address.getMac();
				AtriumVlanId vlanId = AtriumVlanId.vlanId(address.getVlan().shortValue());

				AtriumIp4Address ip4Address = AtriumIp4Address.valueOf(address.getIpAddress().getIpv4Address().getValue());

				// TODO
				// Include subnet in yang
				AtriumIp4Prefix ip4Prefix = AtriumIp4Prefix.valueOf(ip4Address.getIp4Address().toString() + "/24");
				AtriumInterfaceIpAddress interfaceIpAddress = new AtriumInterfaceIpAddress(ip4Address, ip4Prefix);
				Set<AtriumInterfaceIpAddress> interfaceIpAddressSet = new HashSet<>();
				interfaceIpAddressSet.add(interfaceIpAddress);

				NodeId nodeId = new NodeId(address.getDpid());
				NodeConnectorId connectorId = new NodeConnectorId(
						address.getDpid() + ":" + address.getOfPortId().getValue());

				InstanceIdentifier<NodeConnector> instanceIdentifier = InstanceIdentifier.builder(Nodes.class)
						.child(Node.class, new NodeKey(nodeId))
						.child(NodeConnector.class, new NodeConnectorKey(connectorId)).build();

				NodeConnector nodeConnector = null;
				try {
					ListenableFuture<Optional<NodeConnector>> lfONT;
					lfONT = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
					Optional<NodeConnector> oNT = lfONT.get();
					nodeConnector = oNT.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} finally {
					readOnlyTransaction.close();
				}

				matchingInterface = new AtriumInterface(nodeConnector, interfaceIpAddressSet, mac, vlanId);
				return matchingInterface;
			}
		}
		return null;
	}

	@Override
	public Set<AtriumInterface> getInterfaces() {

		Set<AtriumInterface> interfaceSet = new HashSet<>();
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<Addresses> addressBuilder = InstanceIdentifier.builder(Addresses.class).build();
		Addresses addresses = null;

		// Cautious wait to ensure data is filled..
		try {
			Thread.sleep(250);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		try {
			ListenableFuture<Optional<Addresses>> lfONT;
			lfONT = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, addressBuilder);
			Optional<Addresses> oNT = lfONT.get();
			if (oNT.isPresent()) {
				addresses = oNT.get();
			} else {
				log.warn("Coudn't get addresses in data store..");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} finally {
			readOnlyTransaction.close();
		}

		for (Address address : addresses.getAddress()) {

			NodeId nodeId = new NodeId(address.getDpid());
			NodeConnectorId connectorId = new NodeConnectorId(
					address.getDpid() + ":" + address.getOfPortId().getValue());

			InstanceIdentifier<NodeConnector> instanceIdentifier = InstanceIdentifier.builder(Nodes.class)
					.child(Node.class, new NodeKey(nodeId))
					.child(NodeConnector.class, new NodeConnectorKey(connectorId)).build();

			NodeConnector nodeConnector = null;
			try {
				ListenableFuture<Optional<NodeConnector>> lfONT;
				lfONT = readOnlyTransaction.read(LogicalDatastoreType.OPERATIONAL, instanceIdentifier);
				Optional<NodeConnector> oNT = lfONT.get();
				if (oNT.isPresent()) {
					nodeConnector = oNT.get();
				} else {
					log.warn("Coudn't get Node Connector {} in data store", connectorId);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} finally {
				readOnlyTransaction.close();
			}

			MacAddress mac = address.getMac();
			AtriumVlanId vlanId = AtriumVlanId.vlanId(address.getVlan().shortValue());

			IpAddress ipAddress = address.getIpAddress();
			AtriumIp4Address ip4Address = AtriumIp4Address.valueOf(ipAddress.getIpv4Address().getValue());

			// TODO
			// Include subnet in yang
			AtriumIp4Prefix ip4Prefix = AtriumIp4Prefix.valueOf(ip4Address.getIp4Address().toString() + "/24");
			AtriumInterfaceIpAddress interfaceIpAddress = new AtriumInterfaceIpAddress(ip4Address, ip4Prefix);
			Set<AtriumInterfaceIpAddress> interfaceIpAddressSet = new HashSet<>();
			interfaceIpAddressSet.add(interfaceIpAddress);

			AtriumInterface matchingInterface = new AtriumInterface(nodeConnector, interfaceIpAddressSet, mac, vlanId);
			interfaceSet.add(matchingInterface);
		}
		return interfaceSet;
	}
	
	public List<BgpSpeaker> getBgpSpeakerFromMap() {
		List<BgpSpeaker> bgpSpeakers = null;
		
		if(bgpSpeakerMap != null) {
			bgpSpeakers = new ArrayList<BgpSpeaker>();
			Iterator<BgpSpeaker> iterator = bgpSpeakerMap.values().iterator();
			while(iterator.hasNext()) {
				bgpSpeakers.add(iterator.next());
			}
		}
		return bgpSpeakers;
	}
	
	public List<BgpPeer> getBgpPeerFromMap() {
		List<BgpPeer> bgpPeers = null;
		
		if(bgpPeerMap != null) {
			bgpPeers = new ArrayList<BgpPeer>();
			Iterator<BgpPeer> iterator = bgpPeerMap.values().iterator();
			while(iterator.hasNext()) {
				bgpPeers.add(iterator.next());
			}
		}
		return bgpPeers;
	}
}
