/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.atrium.atriumutil.AtriumUtils;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddressesKey;

/**
 * This class tests the parsing of JSON configuration files
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigReaderTest {
	private static final String DEFAULT_CONFIG_FILE = "../../utils/config/target/classes/sdnip.json";
	private JSONObject jsonObject;

	/**
	 * Initializes the JSON Parser
	 */
	@Before
	public void init() throws FileNotFoundException, IOException, ParseException {
		JSONParser jsonParser = new JSONParser();
		File configFile = new File(DEFAULT_CONFIG_FILE);
		String fileName = configFile.getAbsoluteFile().getCanonicalPath();
		FileReader fileReader = new FileReader(fileName);
		Object object = jsonParser.parse(fileReader);
		jsonObject = (JSONObject) object;
	}

	/**
	 * Tests whether the Config Reader is initialized
	 */
	@Test
	public void testInitialize() throws MalformedURLException, IOException {
		File configFile = new File(DEFAULT_CONFIG_FILE);
		URL configFileUrl = configFile.getCanonicalFile().toURI().toURL();
		assertEquals(true, ConfigReader.initialize(configFileUrl));
	}

	/**
	 * Tests whether the bgp speakers are read properly from the config file
	 */
	@Test
	public void testGetBgpSpeakers() throws FileNotFoundException, IOException, ParseException {
		testInitialize();

		BgpSpeakers actualBgpSpeakers = null;
		BgpSpeakers expectedBgpSpeakers = null;

		BgpSpeakerBuilder bgpSpeakerBuilder = new BgpSpeakerBuilder();
		BgpSpeakersBuilder bgpSpeakersBuilder = new BgpSpeakersBuilder();
		InterfaceAddressesBuilder intfAddressBuilder = new InterfaceAddressesBuilder();
		List<InterfaceAddresses> intfAddressesList = new ArrayList<InterfaceAddresses>();
		List<BgpSpeaker> bgpSpeakerList = new ArrayList<BgpSpeaker>();

		JSONArray bgpSpeakers = (JSONArray) jsonObject.get("bgpSpeakers");
		Iterator<JSONObject> bgpIterator = bgpSpeakers.iterator();

		while (bgpIterator.hasNext()) {
			JSONObject bgp = (JSONObject) bgpIterator.next();
			bgpSpeakerBuilder.setSpeakerName((String) bgp.get("name"));
			String attachmentDpid = AtriumUtils.hexDpidStringToOpenFlowDpid((String) bgp.get("attachmentDpid"));
			bgpSpeakerBuilder.setAttachmentDpId(NodeId.getDefaultInstance(attachmentDpid));
			String attachmentPort = (String) bgp.get("attachmentPort");
			bgpSpeakerBuilder.setAttachmentPort(Long.valueOf(attachmentPort));
			String macAddress = (String) bgp.get("macAddress");
			bgpSpeakerBuilder.setMacAddress(MacAddress.getDefaultInstance(macAddress));
			JSONArray intfList = (JSONArray) bgp.get("interfaceAddresses");
			Iterator<JSONObject> intfIterator = intfList.iterator();

			while (intfIterator.hasNext()) {
				JSONObject intfAddress = (JSONObject) intfIterator.next();
				String ipAddress = (String) intfAddress.get("ipAddress");
				intfAddressBuilder.setIpAddress(new IpAddress(Ipv4Address.getDefaultInstance(ipAddress)));
				String interfaceDpid = AtriumUtils
						.hexDpidStringToOpenFlowDpid((String) intfAddress.get("interfaceDpid"));
				String interfacePort = (String) intfAddress.get("interfacePort");
				NodeConnectorId ncId = NodeConnectorId.getDefaultInstance(interfaceDpid + ":" + interfacePort);
				intfAddressBuilder.setOfPortId(ncId);
				intfAddressBuilder.setKey(new InterfaceAddressesKey(ncId));
				intfAddressesList.add(intfAddressBuilder.build());
			}
			bgpSpeakerBuilder.setInterfaceAddresses(intfAddressesList);
			bgpSpeakerList.add(bgpSpeakerBuilder.build());
		}

		bgpSpeakersBuilder.setBgpSpeaker(bgpSpeakerList);

		expectedBgpSpeakers = bgpSpeakersBuilder.build();
		actualBgpSpeakers = ConfigReader.getBgpSpeakers();

		assertEquals(expectedBgpSpeakers, actualBgpSpeakers);
	}

	/**
	 * Tests whether the bgp peers are read properly from the config file
	 */
	@Test
	public void testGetBgpPeers() throws FileNotFoundException, IOException, ParseException {
		testInitialize();

		BgpPeers actualBgpPeers = null;
		BgpPeers expectedBgpPeers = null;

		List<BgpPeer> bgpPeerList = new ArrayList<BgpPeer>();
		BgpPeerBuilder bgpPeerBuilder = new BgpPeerBuilder();
		BgpPeersBuilder bgpPeersBuilder = new BgpPeersBuilder();

		JSONArray bgpPeers = (JSONArray) jsonObject.get("bgpPeers");
		Iterator<JSONObject> bgpIterator = bgpPeers.iterator();

		while (bgpIterator.hasNext()) {
			JSONObject bgpPeer = (JSONObject) bgpIterator.next();
			String dpId = AtriumUtils.hexDpidStringToOpenFlowDpid((String) bgpPeer.get("attachmentDpid"));
			bgpPeerBuilder.setPeerDpId(NodeId.getDefaultInstance(dpId));
			String attachmentPort = (String) bgpPeer.get("attachmentPort");
			bgpPeerBuilder.setPeerPort(Long.valueOf(attachmentPort));
			String ipAddress = (String) bgpPeer.get("ipAddress");
			IpAddress ip = new IpAddress(Ipv4Address.getDefaultInstance(ipAddress));
			bgpPeerBuilder.setPeerAddr(ip);
			bgpPeerBuilder.setKey(new BgpPeerKey(ip));
			bgpPeerList.add(bgpPeerBuilder.build());
		}

		bgpPeersBuilder.setBgpPeer(bgpPeerList);
		expectedBgpPeers = bgpPeersBuilder.build();

		actualBgpPeers = ConfigReader.getBgpPeer();

		assertEquals(expectedBgpPeers, actualBgpPeers);
	}
}
