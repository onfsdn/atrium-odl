/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.atrium.hostservice.arp;

import java.net.URL;
import java.util.List;
import java.io.File;
import java.util.concurrent.ExecutionException;

import org.opendaylight.atrium.hostservice.api.ArpMessageAddress;
import org.opendaylight.atrium.hostservice.api.HostService;
import org.opendaylight.atrium.hostservice.impl.ArpSender;
import org.opendaylight.atrium.hostservice.impl.ConfigReader;
import org.opendaylight.atrium.hostservice.impl.ConfigWriter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.ArpPacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.arp.rev140528.arp.packet.received.packet.chain.packet.ArpPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.addresses.Address;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ArpHandler implements ArpPacketListener {

	private DataBroker dataBroker;
	private ArpSender arpSender;
	private HostService hostService;

	private static final String DEFAULT_CONFIG_FILE = "./configuration/initial/addresses.json";

	private final Logger log = LoggerFactory.getLogger(getClass());

	public ArpHandler(DataBroker dataBroker, PacketProcessingService packetProcessingService, HostService hostService) {
		this.dataBroker = dataBroker;
		this.arpSender = new ArpSender(packetProcessingService);
		this.hostService = hostService;
	}

	public void readConfiguration() {
		log.info("reading configuration");
		
        URL configFileUrl=null;
        try {
                configFileUrl = new File(DEFAULT_CONFIG_FILE).toURI().toURL();
        } catch (Exception ex) {
                log.error("Error reading configuration file " + DEFAULT_CONFIG_FILE);
                return;
        }
        if(configFileUrl == null) {
                return;
        }

		boolean isSuccess = ConfigReader.initialize(configFileUrl);
		if (isSuccess) {
			Addresses arpAddresses = ConfigReader.getArpAddresses();
			ConfigWriter.writeBgpConfigData(dataBroker, arpAddresses);

		} else {
			log.error("Error reading configuration file " + DEFAULT_CONFIG_FILE);
		}
	}

	@Override
	public void onArpPacketReceived(ArpPacketReceived packetReceived) {

		if (packetReceived == null || packetReceived.getPacketChain() == null) {
			return;
		}

		RawPacket rawPacket = null;
		EthernetPacket ethernetPacket = null;
		ArpPacket arpPacket = null;
		for (PacketChain packetChain : packetReceived.getPacketChain()) {
			if (packetChain.getPacket() instanceof RawPacket) {
				rawPacket = (RawPacket) packetChain.getPacket();
			} else if (packetChain.getPacket() instanceof EthernetPacket) {
				ethernetPacket = (EthernetPacket) packetChain.getPacket();
			} else if (packetChain.getPacket() instanceof ArpPacket) {
				arpPacket = (ArpPacket) packetChain.getPacket();
			}
		}
		if (rawPacket == null || ethernetPacket == null || arpPacket == null) {
			return;
		}

		// TODO: Need to handle Ipv6 address ?
		MacAddress sourceMac = ethernetPacket.getSourceMac();
		Ipv4Address sourceIp = new Ipv4Address(arpPacket.getSourceProtocolAddress());
		Ipv4Address destIp = new Ipv4Address(arpPacket.getDestinationProtocolAddress());
		MacAddress destMac = null;

		List<Header8021q> list = ethernetPacket.getHeader8021q();
		Header8021q vlan = null;

		if (list == null || list.isEmpty()) {
			log.debug("Untagged packet observed");
		} else {
			vlan = list.get(0);
			log.debug("Vlan tag for packet is " + vlan);
		}

		Address address = getMatchingAddress(new IpAddress(destIp));

		if (address != null) {
			destMac = address.getMac();

			ArpMessageAddress senderArpMessageAddress = new ArpMessageAddress(destMac, destIp);
			ArpMessageAddress receiverArpMessageAddress = new ArpMessageAddress(sourceMac, sourceIp);
			InstanceIdentifier<NodeConnector> egressNc = (InstanceIdentifier<NodeConnector>) rawPacket.getIngress()
					.getValue();
			arpSender.sendArpResponse(senderArpMessageAddress, receiverArpMessageAddress, egressNc, vlan);

		} else {
			// TODO
			// LOG.info("Address not found in configuration.. Cant resolve
			// arp...");
			if (hostService != null) {
				org.opendaylight.atrium.util.AtriumIpAddress atriumIp = null;
				if (destIp != null) {
					atriumIp = org.opendaylight.atrium.util.AtriumIpAddress.valueOf(destIp.getValue());
				}
				org.opendaylight.atrium.util.AtriumMacAddress mac = hostService.getMacAddressByIp(atriumIp);
				if (mac != null) {
					MacAddress arpMac = new MacAddress(mac.toString());
					ArpMessageAddress senderArpMessageAddress = new ArpMessageAddress(arpMac, destIp);
					ArpMessageAddress receiverArpMessageAddress = new ArpMessageAddress(sourceMac, sourceIp);
					InstanceIdentifier<NodeConnector> egressNc = (InstanceIdentifier<NodeConnector>) rawPacket
							.getIngress().getValue();
					arpSender.sendArpResponse(senderArpMessageAddress, receiverArpMessageAddress, egressNc, vlan);
				}
			}
		}
	}

	private Address getMatchingAddress(IpAddress destIp) {
		Addresses addresses = null;

		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier<Addresses> addressesIdentifier = InstanceIdentifier.builder(Addresses.class).build();
		try {
			addresses = readOnlyTransaction.read(LogicalDatastoreType.CONFIGURATION, addressesIdentifier).get().get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		for (Address address : addresses.getAddress()) {
			IpAddress ipAddress = address.getIpAddress();

			if (ipAddress.getIpv4Address().getValue().equals(destIp.getIpv4Address().getValue())) {
				return address;
			}
		}

		return null;
	}
}
