/*
 * Copyright (c) 2016 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.atrium.routingservice.api.RouteEntry.createBinaryString;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.atrium.hostservice.api.Host;
import org.opendaylight.atrium.hostservice.api.HostEvent;
import org.opendaylight.atrium.hostservice.api.HostListener;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.routingservice.api.AtriumFibEntry;
import org.opendaylight.atrium.routingservice.api.AtriumFibUpdate;
import org.opendaylight.atrium.routingservice.api.FibListener;
import org.opendaylight.atrium.routingservice.api.RouteEntry;
import org.opendaylight.atrium.routingservice.api.RoutingService;
import org.opendaylight.atrium.routingservice.api.RouteUpdate;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.atrium.util.AtriumMacAddress;
import org.opendaylight.controller.md.sal.binding.api.BindingTransactionChain;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener;
import org.opendaylight.controller.md.sal.binding.api.DataTreeChangeService;
import org.opendaylight.controller.md.sal.binding.api.DataTreeIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChain;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainListener;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.protocol.bgp.rib.RibReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.googlecode.concurrenttrees.common.KeyValuePair;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;

import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.AsNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.Ipv4Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.inet.rev150305.ipv4.routes.ipv4.routes.Ipv4Route;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.AsPath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.message.rev130919.path.attributes.attributes.as.path.Segments;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.bgp.rib.rib.LocRib;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.Tables;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.TablesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.rib.rev130925.rib.tables.Routes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.Ipv4AddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.SubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.UnicastSubsequentAddressFamily;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.CNextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.Ipv4NextHopCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgp.types.rev130919.next.hop.c.next.hop.ipv4.next.hop._case.Ipv4NextHop;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostNode;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;

/**
 * Collects the RIB routes through bgpcep application, requires configuration as
 * given in atrium-bgp-config.xml in utils. Generates FIB entry and pass it on
 * to FIBListeners (for example BGPRouter application which translates them to
 * FlowObjectives)
 *
 * @param <T>
 */
public class RibManager<T extends Route> implements BindingAwareProvider, AutoCloseable, DataTreeChangeListener<T>,
		TransactionChainListener, RoutingService {

	private static final Logger LOG = LoggerFactory.getLogger(RibManager.class);

	boolean closed = false;

	// Reference to the localRIB in bgpcep
	private RibReference localRibRef = null;

	// Constants used in IID
	static final Class<? extends AddressFamily> AFI = Ipv4AddressFamily.class;
	static final Class<? extends SubsequentAddressFamily> SAFI = UnicastSubsequentAddressFamily.class;
	static final TablesKey KEY = new TablesKey(AFI, SAFI);

	// Single threaded Executor which processes route updates from BGP Session
	private ExecutorService bgpUpdatesExecutor;

	/** The rib table4. */
	private InvertedRadixTree<RouteEntry> ribTable4;

	// Used for creating the transaction 
	private final BindingTransactionChain chain;

	// Stores all incoming route updates in a queue.
	private final BlockingQueue<DataTreeModification<T>> routeUpdatesQueue = new LinkedBlockingQueue<>();

	// Listener for FIB updates
	private FibListener fibListener;

	// DataTreeChangeService for registering DataTreeChange Events
	DataTreeChangeService dataTreeChangeService;

	// Host tracking utility used to track nexthop hosts
	private HostService hostService;

	// The IPv4 address to MAC address mapping
	private final Map<AtriumIpAddress, AtriumMacAddress> ip2Mac = new ConcurrentHashMap<>();

	// Next-hop IP address to route entry mapping for next hops pending MAC
	// resolution
	private SetMultimap<AtriumIpAddress, RouteEntry> routesWaitingOnArp;

	// RoutingConfig Service to check if the IP Prefix is local
	RoutingConfigService routingConfigService;

	/**
	 * Constructor for RibManager
	 * 
	 * @param dataBroker
	 * @param ribReference
	 * @param hostService
	 * @param routingConfigService
	 */
	public RibManager(final DataBroker dataBroker, final RibReference ribReference, final HostService hostService,
			final RoutingConfigService routingConfigService) {
		localRibRef = ribReference;
		this.chain = dataBroker.createTransactionChain(this);
		this.hostService = hostService;
		this.routingConfigService = routingConfigService;
		dataTreeChangeService = (DataTreeChangeService) dataBroker;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.opendaylight.controller.sal.binding.api.BindingAwareProvider#
	 * onSessionInitiated
	 * (org.opendaylight.controller.sal.binding.api.BindingAwareBroker
	 * .ProviderContext)
	 */
	@Override
	public void onSessionInitiated(ProviderContext session) {
		LOG.info("Router Session Initiated");
		routesWaitingOnArp = Multimaps.synchronizedSetMultimap(HashMultimap.<AtriumIpAddress, RouteEntry> create());
		ribTable4 = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
		bgpUpdatesExecutor = Executors
				.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("atrium-bgp-updates-%d").build());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opendaylight.atrium.routingservice.api.RoutingService#start()
	 */
	public final void start() {

		bgpUpdatesExecutor.execute(new Runnable() {
			@Override
			public void run() {
				doUpdatesThread();
			}
		});

		// Starting host listener
		hostService.start();

		hostService.addListener(new InternalHostListener());

		final InstanceIdentifier<Tables> tablesId = this.localRibRef.getInstanceIdentifier().child(LocRib.class)
				.child(Tables.class, new TablesKey(AFI, SAFI));
		final DataTreeIdentifier<T> id = new DataTreeIdentifier<>(LogicalDatastoreType.OPERATIONAL,
				getRouteWildcard(tablesId));
		dataTreeChangeService.registerDataTreeChangeListener(id, this);

		LOG.info("Rib Manager Started");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.opendaylight.atrium.routingservice.api.RoutingService#stop()
	 */
	@Override
	public void stop() {
		// TODO Auto-generated method stub
		this.closed = true;
		// Stop host service
		hostService.stop();

		// Stop the thread(s)
		bgpUpdatesExecutor.shutdownNow();
		synchronized (this) {
			// Cleanup all local state
			ribTable4 = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
			routeUpdatesQueue.clear();
			routesWaitingOnArp.clear();
			ip2Mac.clear();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.AutoCloseable#close()
	 */
	@Override
	public void close() throws Exception {
		LOG.info("RoutingserviceProvider Closed");
		stop();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opendaylight.atrium.routingservice.api.RoutingService#getRoutes4()
	 */
	@Override
	public Collection<RouteEntry> getRoutes4() {
		Iterator<KeyValuePair<RouteEntry>> it = ribTable4.getKeyValuePairsForKeysStartingWith("").iterator();

		List<RouteEntry> routes = new LinkedList<>();

		while (it.hasNext()) {
			KeyValuePair<RouteEntry> entry = it.next();
			routes.add(entry.getValue());
		}
		return routes;
	}

	/**
	 * Used for constructing the RIB IID
	 * 
	 * @param tablesId
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected InstanceIdentifier<T> getRouteWildcard(final InstanceIdentifier<Tables> tablesId) {
		return tablesId.child((Class) Ipv4Routes.class).child(Ipv4Route.class);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.opendaylight.controller.md.sal.binding.api.DataTreeChangeListener#
	 * onDataTreeChanged(java.util.Collection)
	 */
	@Override
	public synchronized void onDataTreeChanged(Collection<DataTreeModification<T>> changes) {
		// TODO Auto-generated method stub
		if (this.closed) {
			LOG.trace("Transaction chain was already closed, skipping update.");
			return;
		}

		final ReadOnlyTransaction trans = this.chain.newReadOnlyTransaction();
		LOG.debug("Received data change {} event with transaction {}", changes, trans.getIdentifier());

		for (final DataTreeModification<T> change : changes) {
			try {
				routeChanged(change, trans);
			} catch (final RuntimeException e) {
				LOG.warn("Data change {} was not completely propagated to listener {}, aborting", change, this, e);
				// trans.cancel();
				return;
			}
		}

	}

	/**
	 * Processes adding a route entry.
	 * <p>
	 * The route entry is added to the radix tree. If there was an existing next
	 * hop for this prefix, but the next hop was different, then the old route
	 * entry is deleted.
	 *
	 * </p>
	 * <p>
	 * NOTE: Currently, we don't handle routes if the next hop is within the SDN
	 * domain.
	 * </p>
	 *
	 * @param routeEntry
	 *            the route entry to add
	 * @param withdrawPrefixes
	 *            the collection of accumulated prefixes whose intents will be
	 *            withdrawn
	 * @return the corresponding FIB entry change, or null
	 */
	private AtriumFibEntry processRouteAdd(RouteEntry routeEntry, Collection<AtriumIpPrefix> withdrawPrefixes) {
		LOG.info("Processing route add: {}", routeEntry);

		// Find the old next-hop if we are updating an old route entry
		AtriumIpAddress oldNextHop = null;
		RouteEntry oldRouteEntry = findRibRoute(routeEntry.prefix());
		if (oldRouteEntry != null) {
			oldNextHop = oldRouteEntry.nextHop();
		}

		// Add the new route to the RIB
		addRibRoute(routeEntry);

		if (oldNextHop != null) {
			if (oldNextHop.equals(routeEntry.nextHop())) {
				return null; // No change
			}
			//
			// Update an existing nexthop for the prefix.
			// We need to remove the old flows for this prefix from the
			// switches before the new flows are added.
			//
			withdrawPrefixes.add(oldRouteEntry.prefix());
		}

		if (isIpPrefixLocal(routeEntry.prefix())) {
			// Route originated by local SDN domain
			// We don't handle these here, reactive routing APP will handle
			// these
			LOG.debug("Own route {} to {}", routeEntry.prefix(), routeEntry.nextHop());
			return null;
		}

		AtriumMacAddress nextHopMacAddress = null;

		// Find the MAC address of next hop router for this route entry.
		// If the MAC address can not be found in ARP cache, then this prefix
		// will be put in routesWaitingOnArp queue.

		LOG.info("Sending request to host service for MAC resolution : {}", routeEntry.nextHop());
		// Monitor the IP address for updates of the MAC address
		hostService.startMonitoringIp(routeEntry.nextHop());

		LOG.info("Checking the ip2Mac table for : {}", routeEntry.nextHop());
		// Check if we know the MAC address of the next hop MacAddress
		nextHopMacAddress = ip2Mac.get(routeEntry.nextHop());

		if (nextHopMacAddress == null) {
			Host host = hostService.getHost(new HostId(routeEntry.nextHop().toString()));
			if (host != null) {
				HostNode hostNode = host.getHostNode();
				if (hostNode != null) {
					List<ConnectorAddress> addresses = hostNode.getConnectorAddress();
					org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress mac = addresses
							.get(0).getMac();
					checkNotNull(mac);
					nextHopMacAddress = AtriumMacAddress.valueOf(mac.getValue());
				}
			}
			if (nextHopMacAddress != null) {
				ip2Mac.put(routeEntry.nextHop(), nextHopMacAddress);
			}
		}
		if (nextHopMacAddress == null) {
			LOG.info("nextHopMacAddress not found in ip2Mac : {}", routeEntry.nextHop());
			routesWaitingOnArp.put(routeEntry.nextHop(), routeEntry);
			return null;
		}

		LOG.info("Creating FIB entry : " + routeEntry.prefix() + "," + routeEntry.nextHop() + "," + nextHopMacAddress);
		return new AtriumFibEntry(routeEntry.prefix(), routeEntry.nextHop(), nextHopMacAddress);
	}

	/*
	 * Processes the deletion of a route entry. <p> The prefix for the routing
	 * entry is removed from radix tree. If the operation is successful, the
	 * prefix is added to the collection of prefixes whose intents that will be
	 * withdrawn. </p>
	 *
	 * @param routeEntry the route entry to delete
	 *
	 * @param withdrawPrefixes the collection of accumulated prefixes whose
	 * intents will be withdrawn
	 */
	private void processRouteDelete(RouteEntry routeEntry, Collection<AtriumIpPrefix> withdrawPrefixes) {
		LOG.debug("Processing route delete: {}", routeEntry);
		boolean isRemoved = removeRibRoute(routeEntry.prefix());

		if (isRemoved) {
			withdrawPrefixes.add(routeEntry.prefix());
		}

		routesWaitingOnArp.remove(routeEntry.nextHop(), routeEntry);
	}

	/**
	 * Removes a route for a prefix from the RIB. The prefix can be either IPv4
	 * or IPv6.
	 *
	 * @param prefix
	 *            the prefix to use
	 * @return true if the route was found and removed, otherwise false
	 */
	boolean removeRibRoute(AtriumIpPrefix prefix) {
		if (prefix.isIp4()) {
			// IPv4
			return ribTable4.remove(createBinaryString(prefix));
		}
		return false;
	}

	/**
	 * Processes route updates.
	 *
	 * @param routeUpdate
	 *            the route updates to process
	 */
	public synchronized void processRouteUpdates(DataTreeModification<T> routeUpdate) {

		Collection<AtriumIpPrefix> withdrawPrefixes = new LinkedList<>();
		Collection<AtriumFibUpdate> fibUpdates = new LinkedList<>();
		Collection<AtriumFibUpdate> fibWithdraws = new LinkedList<>();

		LOG.info("Processing route update: {}", routeUpdate);
		final DataObjectModification<T> root = routeUpdate.getRootNode();

		switch (root.getModificationType()) {
		case SUBTREE_MODIFIED:
		case WRITE:
			LOG.debug("WRITE/SUBTREEMODIFIED: Updated Data for {} is - {}",
					routeUpdate.getRootPath().getRootIdentifier(), root.getDataAfter());

			Ipv4Route ipv4RouteAfter = (Ipv4Route) root.getDataAfter();
			AtriumIpAddress nextHopAfter = getNextHopFromIpv4Route(ipv4RouteAfter);
			AtriumIpPrefix ipPrefixAfter = getIpPrefixFromIpv4Route(ipv4RouteAfter);
			RouteEntry routeEntryAfter = new RouteEntry(ipPrefixAfter, nextHopAfter);

			AtriumFibEntry fib = processRouteAdd(routeEntryAfter, withdrawPrefixes);

			if (fib != null) {
				fibUpdates.add(new AtriumFibUpdate(AtriumFibUpdate.Type.UPDATE, fib));
			}
			break;
		case DELETE:
			LOG.debug("DELETE: Data before for {} is {}", routeUpdate.getRootPath().getRootIdentifier(),
					root.getDataBefore());
			Ipv4Route ipv4RouteUpdate = (Ipv4Route) root.getDataBefore();
			AtriumIpAddress nextHopIp = getNextHopFromIpv4Route(ipv4RouteUpdate);
			AtriumIpPrefix ipPrefix = getIpPrefixFromIpv4Route(ipv4RouteUpdate);
			if (nextHopIp != null && ipPrefix != null) {
				processRouteDelete(new RouteEntry(ipPrefix, nextHopIp), withdrawPrefixes);
			} else {
				LOG.warn("Issue with deleted route attributes");
			}

			break;
		default:
			LOG.error("Unknown update Type: {}", root.getModificationType());
			break;
		}

		withdrawPrefixes.forEach(p -> fibWithdraws
				.add(new AtriumFibUpdate(AtriumFibUpdate.Type.DELETE, new AtriumFibEntry(p, null, null))));

		if (!fibUpdates.isEmpty() || !fibWithdraws.isEmpty()) {
			// Send FIB Notification
			fibListener.update(fibUpdates, fibWithdraws);

			// TODO: Send a notification through md_sal or update fib in
			// data store
		}
	}

	/**
	 * Finds a route in the RIB for a prefix. The prefix can be either IPv4 or
	 * IPv6.
	 *
	 * @param prefix
	 *            the prefix to use
	 * @return the route if found, otherwise null
	 */
	RouteEntry findRibRoute(AtriumIpPrefix prefix) {
		String binaryString = createBinaryString(prefix);
		if (prefix.isIp4()) {
			// IPv4
			return ribTable4.getValueForExactKey(binaryString);
		}
		return null;
	}

	/**
	 * Checks if is ip prefix local.
	 *
	 * @param prefix
	 *            the prefix
	 * @return true, if is ip prefix local
	 */
	// TODO: copy the following method from RoutingConfigurationService
	private boolean isIpPrefixLocal(AtriumIpPrefix prefix) {
		return routingConfigService.isIpPrefixLocal(prefix);
	}

	/**
	 * Adds a route to the RIB. The route can be either IPv4 or IPv6.
	 *
	 * @param routeEntry
	 *            the route entry to use
	 */
	void addRibRoute(RouteEntry routeEntry) {
		if (routeEntry.isIp4()) {
			// IPv4
			ribTable4.put(createBinaryString(routeEntry.prefix()), routeEntry);
		}
	}

	/**
	 * Signals the Router that the MAC to IP mapping has potentially been
	 * updated. This has the effect of updating the MAC address for any
	 * installed prefixes if it has changed, as well as installing any pending
	 * prefixes that were waiting for MAC resolution.
	 *
	 * @param ipAddress
	 *            the IP address that an event was received for
	 * @param macAddress
	 *            the most recently known MAC address for the IP address
	 */
	private void updateMac(AtriumIpAddress ipAddress, AtriumMacAddress macAddress) {
		LOG.debug("Received updated MAC info: {} => {}", ipAddress, macAddress);

		// We synchronize on "this" to prevent changes to the Radix tree
		// while we're pushing intents. If the tree changes, the
		// tree and the intents could get out of sync.
		//
		synchronized (this) {
			Collection<AtriumFibUpdate> submitFibEntries = new LinkedList<>();

			Set<RouteEntry> routesToPush = routesWaitingOnArp.removeAll(ipAddress);

			for (RouteEntry routeEntry : routesToPush) {
				// These will always be adds
				RouteEntry foundRouteEntry = findRibRoute(routeEntry.prefix());
				if (foundRouteEntry != null && foundRouteEntry.nextHop().equals(routeEntry.nextHop())) {
					// We only push FIB updates if the prefix is still in the
					// radix tree and the next hop is the same as our entry.
					// The prefix could have been removed while we were waiting
					// for the ARP, or the next hop could have changed.
					submitFibEntries.add(new AtriumFibUpdate(AtriumFibUpdate.Type.UPDATE,
							new AtriumFibEntry(routeEntry.prefix(), ipAddress, macAddress)));
				} else {
					LOG.debug("{} has been revoked before the MAC was resolved", routeEntry);
				}
			}

			if (!submitFibEntries.isEmpty()) {

				fibListener.update(submitFibEntries, Collections.emptyList());

				// TODO: Send a notification through md_sal or update fib in
				// data store
			}

			ip2Mac.put(ipAddress, macAddress);
		}
	}

	/**
	 * The listener interface for receiving internalHost events. The class that
	 * is interested in processing a internalHost event implements this
	 * interface, and the object created with that class is registered with a
	 * component using the component's <code>addInternalHostListener
	 * <code> method. When the internalHost event occurs, that object's
	 * appropriate method is invoked.
	 *
	 * @see InternalHostEvent
	 */
	class InternalHostListener implements HostListener {

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.opendaylight.atrium.hostservice.api.HostListener#hostEventUpdate
		 * (org.opendaylight.atrium.hostservice.api.HostEvent)
		 */
		@Override
		public void hostEventUpdate(HostEvent event) {
			checkNotNull(event.getHost());
			LOG.info("Receved MAC/IP mapping for host ");
			switch (event.getType()) {
			case HOST_ADDED: {
				Host host = event.getHost();
				List<ConnectorAddress> addresses = host.getHostNode().getConnectorAddress();
				for (ConnectorAddress address : addresses) {
					AtriumMacAddress mac = AtriumMacAddress.valueOf(address.getMac().toString());
					Ipv4Address ipv4Address = address.getIp().getIpv4Address();
					Ipv6Address ipv6Address = address.getIp().getIpv6Address();
					if (ipv4Address != null) {
						updateMac(AtriumIpAddress.valueOf(ipv4Address.getValue()), mac);
					}
					if (ipv6Address != null) {
						updateMac(AtriumIpAddress.valueOf(ipv6Address.getValue()), mac);
					}
				}
				break;
			}
			case HOST_REMOVED: {
				Host host = event.getHost();
				List<ConnectorAddress> addresses = host.getHostNode().getConnectorAddress();
				for (ConnectorAddress address : addresses) {
					Ipv4Address ipv4Address = address.getIp().getIpv4Address();
					Ipv6Address ipv6Address = address.getIp().getIpv6Address();
					if (ipv4Address != null) {
						AtriumIpAddress ip = AtriumIpAddress.valueOf(ipv4Address.getValue());
						ip2Mac.remove(ip);
					}
					if (ipv6Address != null) {
						AtriumIpAddress ip = AtriumIpAddress.valueOf(ipv6Address.getValue());
						ip2Mac.remove(ip);
					}
				}
				break;
			}
			default:
				break;
			}
		}

	}

	/**
	 * Thread for handling route updates.
	 */
	private void doUpdatesThread() {
		boolean interrupted = false;
		try {
			while (!interrupted) {
				try {
					DataTreeModification<T> routeUpdates = routeUpdatesQueue.take();
					processRouteUpdates(routeUpdates);
				} catch (InterruptedException e) {
					LOG.error("Interrupted while taking from updates queue", e);
					interrupted = true;
				} catch (Exception e) {
					LOG.error("exception", e);
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Handles the route update event and initiates processing of routes. 
	 * @param change
	 * @param trans
	 */
	private void routeChanged(final DataTreeModification<T> change, final ReadOnlyTransaction trans) {
		// removeObject(trans, change.getRootPath().getRootIdentifier(),
		// root.getDataBefore());
		try {
			routeUpdatesQueue.put(change);
		} catch (InterruptedException e) {
			LOG.error("Interrupted while putting on routeUpdatesQueue", e);
			Thread.currentThread().interrupt();
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.opendaylight.atrium.routingservice.api.RoutingService#addFibListener
	 * (org.opendaylight.atrium.routingservice.api.FibListener)
	 */
	@Override
	public void addFibListener(FibListener fibListener) {
		// TODO Auto-generated method stub
		this.fibListener = fibListener;
	}

	@Override
	public void onTransactionChainFailed(TransactionChain<?, ?> chain, AsyncTransaction<?, ?> transaction,
			Throwable cause) {
		// TODO Auto-generated method stub
		LOG.error("RibManager for {} failed in transaction {}", localRibRef.getInstanceIdentifier(),
				transaction != null ? transaction.getIdentifier() : null, cause);
	}

	@Override
	public void onTransactionChainSuccessful(TransactionChain<?, ?> arg0) {
		LOG.info("Topology builder for {} shut down", localRibRef.getInstanceIdentifier());

	}

	private Long getASNumberFromASPath(AsPath asPath) {
		if (asPath == null) {
			return Long.valueOf(0);
		}
		List<Segments> segments = asPath.getSegments();
		for (Segments segment : segments) {
			List<AsNumber> asNumbers = segment.getAsSequence();
			for (AsNumber asNumber : asNumbers) {
				if (asNumber != null) {
					return asNumber.getValue();
				}
			}
		}
		return null;
	}

	private Ipv4Address getNextHopFromCNextHop(Ipv4NextHopCase ipv4CNextHop) {
		if (ipv4CNextHop == null) {
			return null;
		}
		Ipv4NextHop ipv4NextHop = ipv4CNextHop.getIpv4NextHop();
		if (ipv4NextHop != null) {
			return ipv4NextHop.getGlobal();
		} else {
			return null;
		}

	}

	private AtriumIpAddress getNextHopFromIpv4Route(Ipv4Route route) {
		if (route == null) {
			return null;
		}
		Ipv4NextHopCase nhc = (Ipv4NextHopCase) route.getAttributes().getCNextHop();
		Ipv4Address ipv4Address = getNextHopFromCNextHop(nhc);
		if (ipv4Address != null) {
			return AtriumIpAddress.valueOf(ipv4Address.getValue());
		}
		return null;

	}

	private AtriumIpPrefix getIpPrefixFromIpv4Route(Ipv4Route ipv4Route) {
		if (ipv4Route == null) {
			return null;
		}
		Ipv4Prefix prefix = ipv4Route.getPrefix();
		if (prefix != null) {
			return AtriumIpPrefix.valueOf(prefix.getValue());
		}
		return null;
	}

}
