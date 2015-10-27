/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;

import java.util.Set;

import org.opendaylight.atrium.atriumutil.AtriumMacAddress;
import org.opendaylight.atrium.atriumutil.IpAddress;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.address.tracker.rev140617.address.node.connector.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;

// TODO: Auto-generated Javadoc
/**
 * The Interface HostService.
 */
public interface HostService {
          
    /**
     * Start.
     */
    public void start();
    
    /**
     * Stop.
     */
    public void stop();
    
    /**
     * Start monitoring ip.
     *
     * @param nhIp the nh ip
     */
    public void startMonitoringIp(IpAddress nhIp);
    
    /**
     * Gets the hosts by ip.
     *
     * @param ip the ip
     * @return the hosts by ip
     */
    public Set<Host> getHostsByIp(IpAddress ip);
    
    /**
     * Returns the host with the specified identifier.
     *
     * @param hostId host identifier
     * @return host or null if one with the given identifier is not known
     */
    public Host getHost(HostId hostId);
    
    
    /**
     * Adds the listener.
     *
     * @param listener the listener
     */
    public void addListener(HostListener listener);
 
    /**
     * Removes the listener.
     *
     * @param listener the listener
     */
    public void removeListener(HostListener listener);
    
    /**
     * Sets the services.
     *
     * @param broker the new services
     * @param configService the config service
     * @param packetService the packet service
     */
    public void setServices(DataBroker broker, RoutingConfigService configService, 
				PacketProcessingService packetService,NotificationProviderService notifService);
    
    /**
     * Gets the address instead of Host based on ARP resolution 
     *
     * @param ip address to be resolved 
     * @return Address for the ip
     */
    public Addresses getAddressByIp(IpAddress ip);

    /**
     * Gets the MAC address instead of Host based on ARP resolution 
     *
     * @param ip address to be resolved 
     * @return MAC Address for the ip
     */
    public AtriumMacAddress getMacAddressByIp(IpAddress ip);
    
}
