/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.opendaylight.atrium.hostservice.api.AddressUpdateEvent;
import org.opendaylight.atrium.hostservice.api.AddressUpdateListener;
import org.opendaylight.atrium.hostservice.api.Host;
import org.opendaylight.atrium.hostservice.api.HostEvent;
import org.opendaylight.atrium.hostservice.api.HostListener;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.hostservice.api.HostUpdatesListener;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.atrium.util.AtriumIpAddress;
import org.opendaylight.atrium.util.AtriumMacAddress;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HostServiceImpl
		implements HostService, AutoCloseable, BindingAwareProvider, HostUpdatesListener, AddressUpdateListener {

	private static final Logger LOG = LoggerFactory.getLogger(HostServiceImpl.class);
	Set<HostListener> listeners = new CopyOnWriteArraySet<>();
	private DataBroker dataService;
	private RoutingConfigService configService;
	private PacketProcessingService packetService;
	private NotificationProviderService notificationService;
	private AddressTracker addressTracker;
	private ConcurrentHashMap<HostId, Host> hostStore;
	private HostMonitor monitor;
	private AddressObserver addressObserver;

	public void setServices(DataBroker dataService, RoutingConfigService configService,
			PacketProcessingService packetService, NotificationProviderService notificationService) {
		this.dataService = dataService;
		this.configService = configService;
		this.packetService = packetService;
		this.notificationService = notificationService;
	}

	@Override
	public void close() throws Exception {
		LOG.info("Stopping HostService");
		stop();
	}

	@Override
	public void start() {
		LOG.info("Starting host service");
		checkNotNull(dataService, "DataBroker is null");
		monitor = new HostMonitor(this, this, dataService, configService, packetService);
		monitor.start();
		addressObserver = new AddressObserver(monitor, notificationService);
		addressObserver.registerAsNotificationListener();
		LOG.info("Host service started");
	}

	@Override
	public void stop() {
		if (monitor != null) {
			monitor.shutdown();
		}
		if (hostStore != null) {
			hostStore.clear();
		}
		if (listeners != null) {
			listeners.clear();
		}
	}

	@Override
	public void onSessionInitiated(ProviderContext session) {
		LOG.debug("Host service session starterd");
		hostStore = new ConcurrentHashMap<>();
	}

	@Override
	public void startMonitoringIp(AtriumIpAddress nhIp) {
		LOG.debug("Adding MAC monitoring for :" + nhIp);
		monitor.addMonitoringFor(nhIp);
	}

	@Override
	public Set<Host> getHostsByIp(AtriumIpAddress ip) {
		return null;

	}

	@Override
	public ConnectorAddress getAddressByIp(AtriumIpAddress ip) {
		return addressTracker.getAddress(ip);
	}

	/**
	 * Returns the host with the specified identifier.
	 *
	 * @param hostId
	 *            host identifier
	 * @return host or null if one with the given identifier is not known
	 */
	public Host getHost(HostId hostId) {
		checkNotNull(hostId, "HostId is null");
		checkNotNull(hostStore, "Hoststore not initialized");
		return hostStore.get(hostId);
	}

	@Override
	public void addListener(HostListener listener) {
		checkNotNull(listener, "Listener cannot be null");
		listeners.add(listener);
	}

	@Override
	public void removeListener(HostListener listener) {
		checkNotNull(listener, "Listener cannot be null");
		if (!listeners.remove(listener)) {
			LOG.warn("Listener {} not registered", listener);
		}
	}

	public synchronized void sendHostAddEvent(HostEvent hostEvent) {
		for (HostListener listener : listeners) {
			listener.hostEventUpdate(hostEvent);
		}
	}

	@Override
	public void deleteHost(HostId hostId) {
		checkNotNull(hostStore, "HostStore empty");
		checkNotNull(hostId, "HostId empty");
		synchronized (hostStore) {
			if (hostStore.isEmpty()) {
				return;
			}
			Host delHost = null;
			if (hostStore.containsKey(hostId)) {
				delHost = hostStore.remove(hostId);
			}
			if (delHost != null) {
				HostEvent hostEvent = new HostEvent(HostEvent.Type.HOST_REMOVED, delHost);
				for (HostListener listener : listeners) {
					listener.hostEventUpdate(hostEvent);
				}
			}
		}
	}

	@Override
	public void addHost(HostId hostId, Host host) {
		checkNotNull(hostStore);
		checkNotNull(hostId);
		checkNotNull(host);
		synchronized (hostStore) {
			if (!hostStore.containsKey(hostId)) {
				LOG.debug("Hoststore does not contain key :" + hostId + " .Adding.");
				hostStore.put(hostId, host);
			}
		}
	}

	@Override
	public AtriumMacAddress getMacAddressByIp(AtriumIpAddress ip) {
		Host host = getHost(new HostId(ip.toString()));
		if (host == null) {
			return null;
		}
		List<ConnectorAddress> addresses = host.getHostNode().getConnectorAddress();
		for (ConnectorAddress address : addresses) {
			Ipv4Address ipv4Address = address.getIp().getIpv4Address();
			Ipv6Address ipv6Address = address.getIp().getIpv6Address();
			AtriumIpAddress atriumIp = null;
			if (ipv4Address != null) {
				atriumIp = AtriumIpAddress.valueOf(ipv4Address.getValue());
			} else if (ipv6Address != null) {
				atriumIp = AtriumIpAddress.valueOf(ipv6Address.getValue());
			}

			if (atriumIp != null && atriumIp.equals(ip)) {
				return AtriumMacAddress.valueOf(address.getMac().getValue());
			}
		}
		return null;
	}

	public void updateAddress(AddressUpdateEvent event) {
		checkNotNull(event);
		checkNotNull(event.getAddress());
		addressTracker.addAddress(AtriumIpAddress.valueOf(event.getAddress().getIp().toString()), event.getAddress());
	}

	class AddressTracker {
		private ConcurrentHashMap<AtriumIpAddress, ConnectorAddress> addressStore;

		public AddressTracker() {
			addressStore = new ConcurrentHashMap<>();
		}

		public void addAddress(AtriumIpAddress ip, ConnectorAddress address) {

			synchronized (addressStore) {
				addressStore.put(ip, address);
			}
		}

		public void updateAddress(AtriumIpAddress ip, ConnectorAddress address) {
			synchronized (addressStore) {
				addressStore.replace(ip, address);
			}
		}

		public void deleteAddress(AtriumIpAddress ip) {
			synchronized (addressStore) {
				addressStore.remove(ip);
			}
		}

		public ConnectorAddress getAddress(AtriumIpAddress ip) {
			return addressStore.get(ip);
		}

	}

}
