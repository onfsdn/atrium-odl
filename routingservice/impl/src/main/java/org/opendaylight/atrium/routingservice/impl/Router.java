/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.opendaylight.atrium.routingservice.bgp.api.RouteEntry.createBinaryString;

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

import org.opendaylight.atrium.atriumutil.IpAddress;
import org.opendaylight.atrium.atriumutil.IpPrefix;
import org.opendaylight.atrium.atriumutil.AtriumMacAddress;
import org.opendaylight.atrium.hostservice.api.Host;
import org.opendaylight.atrium.hostservice.api.HostEvent;
import org.opendaylight.atrium.hostservice.api.HostListener;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.routingservice.api.AtriumFibEntry;
import org.opendaylight.atrium.routingservice.api.FibListener;
import org.opendaylight.atrium.routingservice.api.AtriumFibUpdate;
import org.opendaylight.atrium.routingservice.api.RoutingService;
import org.opendaylight.atrium.routingservice.bgp.api.BgpService;
import org.opendaylight.atrium.routingservice.bgp.api.RouteEntry;
import org.opendaylight.atrium.routingservice.bgp.api.RouteListener;
import org.opendaylight.atrium.routingservice.bgp.api.RouteUpdate;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
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

/**
 * The BGP Speaker implementation which handles I-BGP updates and updates FIB
 */
// import
// org.opendaylight.atrium.routingservice.config.api.RoutingConfigManager;
public class Router implements BindingAwareProvider, AutoCloseable, RoutingService {

	/** The rib table4. */
	private InvertedRadixTree<RouteEntry> ribTable4;

	/** The rib table6. */
	private InvertedRadixTree<RouteEntry> ribTable6;

	/** The route updates queue. */
	// Stores all incoming route updates in a queue.
	private final BlockingQueue<Collection<RouteUpdate>> routeUpdatesQueue = new LinkedBlockingQueue<>();

	/** The routing config service. */
	// Routing Configuration Utility
	private RoutingConfigService routingConfigService;

	/** The bgp service. */
	// Bgp protocol handling utility
	private BgpService bgpService;

	/** The host service. */
	// Host tracking utility used to track nexthop hosts
	private HostService hostService;

	// Next-hop IP address to route entry mapping for next hops pending MAC
	/** The routes waiting on arp. */
	// resolution
	private SetMultimap<IpAddress, RouteEntry> routesWaitingOnArp;

	/** The ip2 mac. */
	// The IPv4 address to MAC address mapping
	private final Map<IpAddress, AtriumMacAddress> ip2Mac = new ConcurrentHashMap<>();

	/** The bgp updates executor. */
	// Single threaded Executor which processes route updates from BGP Session
	private ExecutorService bgpUpdatesExecutor;

	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(Router.class);

	/** The fib listener. */
	// Listener for FIB updates
	private FibListener fibListener;

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
		ribTable4 = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
		ribTable6 = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());

		routesWaitingOnArp = Multimaps.synchronizedSetMultimap(HashMultimap.<IpAddress, RouteEntry> create());

		bgpUpdatesExecutor = Executors
				.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("atrium-bgp-updates-%d").build());

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.opendaylight.atrium.routingservice.api.RoutingService#setServices
	 * (org.opendaylight.atrium.routingservice.config.api.RoutingConfigService,
	 * org.opendaylight.atrium.routingservice.bgp.api.BgpService,
	 * org.opendaylight.atrium.hostservice.api.HostService)
	 */
	@Override
	public void setServices(RoutingConfigService routingConfigSvc, BgpService bgpSvc, HostService hostService) {
		this.routingConfigService = routingConfigSvc;
		this.bgpService = bgpSvc;
		this.hostService = hostService;
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
	 * @see org.opendaylight.atrium.routingservice.api.RoutingService#start()
	 */
	@Override
	public void start() {

		// Starting bgp session manager
		bgpService.start(new InternalRouteListener());

		bgpUpdatesExecutor.execute(new Runnable() {
			@Override
			public void run() {
				doUpdatesThread();
			}
		});

		// Starting host listener
		hostService.start();

		hostService.addListener(new InternalHostListener());
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.opendaylight.atrium.routingservice.api.RoutingService#stop()
	 */
	@Override
	public void stop() {
		// Stop bgp service
		bgpService.stop();

		// Stop host service
		hostService.stop();

		// Stop the thread(s)
		bgpUpdatesExecutor.shutdownNow();
		synchronized (this) {
			// Cleanup all local state
			ribTable4 = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
			ribTable6 = new ConcurrentInvertedRadixTree<>(new DefaultByteArrayNodeFactory());
			routeUpdatesQueue.clear();
			routesWaitingOnArp.clear();
			ip2Mac.clear();
		}
	}

	/**
	 * Gets all IPv4 routes from the RIB.
	 *
	 * @return all IPv4 routes from the RIB
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
	 * Gets all IPv6 routes from the RIB.
	 *
	 * @return all IPv6 routes from the RIB
	 */
	@Override
	public Collection<RouteEntry> getRoutes6() {
		Iterator<KeyValuePair<RouteEntry>> it = ribTable6.getKeyValuePairsForKeysStartingWith("").iterator();

		List<RouteEntry> routes = new LinkedList<>();

		while (it.hasNext()) {
			KeyValuePair<RouteEntry> entry = it.next();
			routes.add(entry.getValue());
		}
		return routes;
	}

	/**
	 * Finds a route in the RIB for a prefix. The prefix can be either IPv4 or
	 * IPv6.
	 *
	 * @param prefix
	 *            the prefix to use
	 * @return the route if found, otherwise null
	 */
	RouteEntry findRibRoute(IpPrefix prefix) {
		String binaryString = createBinaryString(prefix);
		if (prefix.isIp4()) {
			// IPv4
			return ribTable4.getValueForExactKey(binaryString);
		}
		// IPv6
		return ribTable6.getValueForExactKey(binaryString);
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
		} else {
			// IPv6
			ribTable6.put(createBinaryString(routeEntry.prefix()), routeEntry);
		}
	}

	/**
	 * Removes a route for a prefix from the RIB. The prefix can be either IPv4
	 * or IPv6.
	 *
	 * @param prefix
	 *            the prefix to use
	 * @return true if the route was found and removed, otherwise false
	 */
	boolean removeRibRoute(IpPrefix prefix) {
		if (prefix.isIp4()) {
			// IPv4
			return ribTable4.remove(createBinaryString(prefix));
		}
		// IPv6
		return ribTable6.remove(createBinaryString(prefix));
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
	private void updateMac(IpAddress ipAddress, AtriumMacAddress macAddress) {
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
	 * Process route delete.
	 *
	 * @param routeEntry
	 *            the route entry
	 * @param withdrawPrefixes
	 *            the withdraw prefixes
	 */
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
	private void processRouteDelete(RouteEntry routeEntry, Collection<IpPrefix> withdrawPrefixes) {
		LOG.debug("Processing route delete: {}", routeEntry);
		boolean isRemoved = removeRibRoute(routeEntry.prefix());

		if (isRemoved) {
			//
			// Only withdraw intents if an entry was actually removed from the
			// tree. If no entry was removed, the <prefix, nexthop> wasn't
			// there so it's probably already been removed and we don't
			// need to do anything.
			//
			withdrawPrefixes.add(routeEntry.prefix());
		}

		routesWaitingOnArp.remove(routeEntry.nextHop(), routeEntry);
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
	private AtriumFibEntry processRouteAdd(RouteEntry routeEntry, Collection<IpPrefix> withdrawPrefixes) {
		LOG.info("Processing route add: {}", routeEntry);

		// Find the old next-hop if we are updating an old route entry
		IpAddress oldNextHop = null;
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
			withdrawPrefixes.add(routeEntry.prefix());
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
		// will be put in routesWaitingOnArp queue. Otherwise, generate
		// a new route intent.

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

	/**
	 * Processes route updates.
	 *
	 * @param routeUpdates
	 *            the route updates to process
	 */
	public void processRouteUpdates(Collection<RouteUpdate> routeUpdates) {
		synchronized (this) {
			Collection<IpPrefix> withdrawPrefixes = new LinkedList<>();
			Collection<AtriumFibUpdate> fibUpdates = new LinkedList<>();
			Collection<AtriumFibUpdate> fibWithdraws = new LinkedList<>();

			for (RouteUpdate update : routeUpdates) {
				LOG.info("Processing route update: {}", update);
				switch (update.type()) {
				case UPDATE:
					LOG.info("Route update: Creating RIB entry");
					AtriumFibEntry fib = processRouteAdd(update.routeEntry(), withdrawPrefixes);
					if (fib != null) {
						fibUpdates.add(new AtriumFibUpdate(AtriumFibUpdate.Type.UPDATE, fib));
					}

					break;
				case DELETE:
					LOG.info("Route delete: Calling processRouteDelete");
					processRouteDelete(update.routeEntry(), withdrawPrefixes);

					break;
				default:
					LOG.error("Unknown update Type: {}", update.type());
					break;
				}
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
						updateMac(IpAddress.valueOf(ipv4Address.getValue()), mac);
					}
					if (ipv6Address != null) {
						updateMac(IpAddress.valueOf(ipv6Address.getValue()), mac);
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
						IpAddress ip = IpAddress.valueOf(ipv4Address.getValue());
						ip2Mac.remove(ip);
					}
					if (ipv6Address != null) {
						IpAddress ip = IpAddress.valueOf(ipv6Address.getValue());
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
	 * Entry point for route updates.
	 *
	 * @param routeUpdates
	 *            collection of route updates to process
	 */
	private void update(Collection<RouteUpdate> routeUpdates) {
		try {
			routeUpdatesQueue.put(routeUpdates);
		} catch (InterruptedException e) {
			LOG.error("Interrupted while putting on routeUpdatesQueue", e);
			Thread.currentThread().interrupt();
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
					Collection<RouteUpdate> routeUpdates = routeUpdatesQueue.take();
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
	 * Listener for route events.
	 */
	private class InternalRouteListener implements RouteListener {

		/*
		 * (non-Javadoc)
		 *
		 * @see
		 * org.opendaylight.atrium.routingservice.bgp.api.RouteListener#update
		 * (java.util.Collection)
		 */
		@Override
		public void update(Collection<RouteUpdate> routeUpdates) {
			LOG.info("Received route update: ");
			Router.this.update(routeUpdates);
		}
	}

	/**
	 * Checks if is ip prefix local.
	 *
	 * @param prefix
	 *            the prefix
	 * @return true, if is ip prefix local
	 */
	// TODO: copy the following method from RoutingConfigurationService
	private boolean isIpPrefixLocal(IpPrefix prefix) {
		return routingConfigService.isIpPrefixLocal(prefix);
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

}
