/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpPeers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpPeersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakersBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeakerBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeakerKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddressesKey;

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

	public static BgpSpeakers getBgpSpeakers() {

		if (rootJsonNode == null) {
			return null;
		}

		JsonNode bgpSpeakersJsonNode = rootJsonNode.path("bgpSpeakers");
		JsonNode bgpSpeakerJsonNode = bgpSpeakersJsonNode.path("bgpSpeaker");
		Iterator<JsonNode> bgpSpeakerNodeIterator = bgpSpeakerJsonNode.elements();

		BgpSpeakersBuilder bgpSpeakersBuilder = new BgpSpeakersBuilder();
		List<BgpSpeaker> bgpSpeakerList = new ArrayList<BgpSpeaker>();

		while (bgpSpeakerNodeIterator.hasNext()) {
			BgpSpeakerBuilder bgpSpeakerBuilder = new BgpSpeakerBuilder();
			JsonNode speakerElementNode = bgpSpeakerNodeIterator.next();

			long attachmentPort = speakerElementNode.path("attachmentPort").asLong();
			MacAddress macAddress = new MacAddress(speakerElementNode.path("macAddress").asText());
			String speakerName = speakerElementNode.path("speakerName").asText();
			String asNumber = speakerElementNode.path("asNumber").asText();
			NodeId attachmentDpId = new NodeId(speakerElementNode.path("attachmentDpId").asText());
			List<InterfaceAddresses> interfaceAddressesList = new ArrayList<InterfaceAddresses>();
			BgpSpeakerKey bgpSpeakerKey = new BgpSpeakerKey(macAddress);

			Iterator<JsonNode> interfaceAddressesIterator = speakerElementNode.path("interfaceAddresses").elements();
			while (interfaceAddressesIterator.hasNext()) {
				InterfaceAddressesBuilder interfaceAddressesBuilder = new InterfaceAddressesBuilder();
				JsonNode interfaceElementNode = interfaceAddressesIterator.next();

				IpAddress ipAddress = new IpAddress(new Ipv4Address(interfaceElementNode.path("ipAddress").asText()));
				NodeConnectorId of_port_id = new NodeConnectorId(interfaceElementNode.path("of-port-id").asText());

				InterfaceAddressesKey addressesKey = new InterfaceAddressesKey(of_port_id);

				interfaceAddressesBuilder.setIpAddress(ipAddress);
				interfaceAddressesBuilder.setOfPortId(of_port_id);
				interfaceAddressesBuilder.setKey(addressesKey);
				interfaceAddressesList.add(interfaceAddressesBuilder.build());
			}

			bgpSpeakerBuilder.setAttachmentPort(attachmentPort);
			bgpSpeakerBuilder.setMacAddress(macAddress);
			bgpSpeakerBuilder.setSpeakerName(speakerName);
			bgpSpeakerBuilder.setAsNumber(asNumber);
			bgpSpeakerBuilder.setAttachmentDpId(attachmentDpId);
			bgpSpeakerBuilder.setInterfaceAddresses(interfaceAddressesList);
			bgpSpeakerBuilder.setKey(bgpSpeakerKey);

			bgpSpeakerList.add(bgpSpeakerBuilder.build());
		}
		bgpSpeakersBuilder.setBgpSpeaker(bgpSpeakerList);
		return bgpSpeakersBuilder.build();
	}

	public static BgpPeers getBgpPeer() {

		if (rootJsonNode == null) {
			return null;
		}

		JsonNode bgpPeersJsonNode = rootJsonNode.path("bgpPeers");
		JsonNode bgpPeerJsonNode = bgpPeersJsonNode.path("bgpPeer");
		Iterator<JsonNode> bgpPeerNodeIterator = bgpPeerJsonNode.elements();

		BgpPeersBuilder bgpPeersBuilder = new BgpPeersBuilder();
		List<BgpPeer> bgpPeerList = new ArrayList<BgpPeer>();

		while (bgpPeerNodeIterator.hasNext()) {
			BgpPeerBuilder bgpPeerBuilder = new BgpPeerBuilder();
			JsonNode peerElementNode = bgpPeerNodeIterator.next();

			IpAddress peerAddr = new IpAddress(new Ipv4Address(peerElementNode.path("peerAddr").asText()));
			NodeId peerDpId = new NodeId(peerElementNode.path("peerDpId").asText());
			Long peerPort = peerElementNode.path("peerPort").asLong();
			String remoteAs = peerElementNode.path("remoteAs").asText();
			BgpPeerKey peerKey = new BgpPeerKey(peerAddr);

			bgpPeerBuilder.setPeerAddr(peerAddr);
			bgpPeerBuilder.setPeerDpId(peerDpId);
			bgpPeerBuilder.setPeerPort(peerPort);
			bgpPeerBuilder.setRemoteAs(remoteAs);
			bgpPeerBuilder.setKey(peerKey);

			bgpPeerList.add(bgpPeerBuilder.build());
		}
		bgpPeersBuilder.setBgpPeer(bgpPeerList);
		return bgpPeersBuilder.build();
	}

}
