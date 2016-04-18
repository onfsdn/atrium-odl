/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.opendaylight.atrium.atriumutil.AtriumUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.Addresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.AddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.addresses.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.addresses.AddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.addresses.AddressKey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ConfigReader {
	private static JsonNode rootJsonNode;

	public static boolean initialize(URL configFileUrl) {
		byte[] jsonData = null;
		if (configFileUrl != null) {
			try {
				InputStream input = configFileUrl.openStream();
				jsonData = IOUtils.toByteArray(input);
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			rootJsonNode = objectMapper.readTree(jsonData);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	public static Addresses getArpAddresses() {

		if (rootJsonNode == null) {
			return null;
		}

		JsonNode addressesJsonNode = rootJsonNode.path("addresses");
		Iterator<JsonNode> addressesNodeIterator = addressesJsonNode.elements();

		AddressesBuilder addressesBuilder = new AddressesBuilder();
		List<Address> addressList = new ArrayList<Address>();

		while (addressesNodeIterator.hasNext()) {
			AddressBuilder addressBuilder = new AddressBuilder();
			JsonNode addressElementNode = addressesNodeIterator.next();

			String hexDpid = addressElementNode.path("dpid").asText();
			String dpid = AtriumUtils.hexDpidStringToOpenFlowDpid(hexDpid);
			NodeConnectorId port = new NodeConnectorId(addressElementNode.path("port").asText());

			MacAddress mac = new MacAddress(addressElementNode.path("mac").asText());

			JsonNode ipsJsonNode = addressElementNode.path("ips");
			Iterator<JsonNode> ipsJsonNodeIterator = ipsJsonNode.elements();

			IpAddress ips = null;
			if (ipsJsonNodeIterator.hasNext()) {
				JsonNode ipJsonNode = ipsJsonNodeIterator.next();
				ips = new IpAddress(new Ipv4Address(ipJsonNode.asText().split("/")[0]));
			}

			int vlan = (int) addressElementNode.path("vlan").asLong();
			if (vlan == 0) {
				vlan = 1;
			}

			AddressKey addressKey = new AddressKey(ips);
			addressBuilder.setDpid(dpid);
			addressBuilder.setOfPortId(port);
			addressBuilder.setMac(mac);
			addressBuilder.setIpAddress(ips);
			addressBuilder.setVlan(new Integer(vlan));
			addressBuilder.setKey(addressKey);

			addressList.add(addressBuilder.build());
		}
		addressesBuilder.setAddress(addressList);
		return addressesBuilder.build();
	}
}
