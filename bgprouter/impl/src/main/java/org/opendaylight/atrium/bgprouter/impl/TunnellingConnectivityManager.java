/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import org.opendaylight.atrium.atriumutil.ActionData;
import org.opendaylight.atrium.atriumutil.ActionUtils;
import org.opendaylight.atrium.atriumutil.AtriumUtils;
import org.opendaylight.atrium.atriumutil.tcp.AtriumTCPDecodeException;
import org.opendaylight.atrium.atriumutil.tcp.AtriumTCPHeader;
import org.opendaylight.atrium.routingservice.config.api.RoutingConfigService;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.PacketChain;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.basepacket.rev140528.packet.chain.grp.packet.chain.packet.RawPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.fields.Header8021q;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ethernet.rev140528.ethernet.packet.received.packet.chain.packet.EthernetPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.Ipv4PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.KnownIpProtocols;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.ipv4.rev140528.ipv4.packet.received.packet.chain.packet.Ipv4Packet;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.BgpSpeakers;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgppeers.BgpPeer;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.BgpSpeaker;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddresses;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.bgpconfig.api.rev150725.bgpspeakers.bgpspeaker.InterfaceAddressesBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.ForwardInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.Objective.Operation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.AtriumFlowObjectiveService;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.openflow.rev150211.OpenflowFeatureService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.ForwardingObjective;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.ForwardingObjective.Flag;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.ForwardingObjectiveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.drivers.atrium.rev150211.forward.input.forwarding.objective.MatchBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;

import com.google.common.util.concurrent.JdkFutureAdapters;

/**
 * Manages connectivity between peers by tunnelling BGP traffic through OpenFlow
 * packet-ins and packet-outs.
 */
public class TunnellingConnectivityManager implements Ipv4PacketListener {

	// port where E-BGP updates are sent
	private static final short BGP_PORT = 179;

	// TCP offset
	private static final int TCP_OFFSET = 34;

	// Logger
	private final Logger LOG = getLogger(getClass());

	// BGP Speaker = DP switch acting as router
	private final BgpSpeaker bgpSpeaker;

	// Reads static router configuration from file and writes it into data store
	private final RoutingConfigService configService;

	// Sends packet-out with the help of openflow plugin
	private PacketProcessingService packetService;

	// Flow objectives to install flows by invoking the driver corresponding to
	// DP switch
	private AtriumFlowObjectiveService flowObjectivesService;

	// MD-SAL data broker to read/write data in data store
	private DataBroker dataBroker = null;

	public TunnellingConnectivityManager(DataBroker dataBroker, RoutingConfigService configService,
			PacketProcessingService packetService, AtriumFlowObjectiveService flowObjectives) {
		this.configService = configService;
		this.packetService = packetService;
		this.dataBroker = dataBroker;
		this.flowObjectivesService = flowObjectives;
		BgpSpeakers bgpSpeakers = null;
		bgpSpeakers = configService.getBgpSpeakers();

		if (bgpSpeakers == null) {
			throw new IllegalArgumentException("Must have at least one BGP speaker configured");
		}

		Optional<BgpSpeaker> bgpSpeaker = bgpSpeakers.getBgpSpeaker().stream().findAny();

		this.bgpSpeaker = bgpSpeaker.get();

	}

	public void start() {

		// packetService.addProcessor(processor, PacketProcessor.director(3));
	}

	public void stop() {
		// packetService.removeProcessor(processor);
	}

	/**
	 * Pushes the flow rules for forwarding BGP TCP packets to controller. It is
	 * called when switches are connected and available.
	 */
	public void notifySwitchAvailable() {

		checkNotNull(flowObjectivesService, "FlowObjectivesService in DIDM not initialized");

		checkNotNull(bgpSpeaker);
		NodeRef nodeRef = new NodeRef(InstanceIdentifier.builder(Nodes.class)
				.child(Node.class, new NodeKey(bgpSpeaker.getAttachmentDpId())).build());

		// Adding slight delay so that flow objective can identify the device
		// and register
		try {
			Thread.sleep(2000);
		} catch (Exception ex) {

		}

		// For BGP_PORT Source
		ForwardingObjective forwardingObjSrc = generateForwardingObjective(true);

		ForwardInputBuilder forwardInputBuilderSrc = new ForwardInputBuilder();
		forwardInputBuilderSrc.setNode(nodeRef);
		forwardInputBuilderSrc.setForwardingObjective(forwardingObjSrc);
		flowObjectivesService.forward(forwardInputBuilderSrc.build());

		// For BGP_PORT Dest
		ForwardingObjective forwardingObjDest = generateForwardingObjective(false);

		ForwardInputBuilder forwardInputBuilderDest = new ForwardInputBuilder();
		forwardInputBuilderDest.setNode(nodeRef);
		forwardInputBuilderDest.setForwardingObjective(forwardingObjDest);
		flowObjectivesService.forward(forwardInputBuilderDest.build());

		LOG.info("Punt to controller for BGP packets sent");

	}

	private ForwardingObjective generateForwardingObjective(boolean isSrc) {
		ForwardingObjectiveBuilder fwdObjBuilder = new ForwardingObjectiveBuilder();
		fwdObjBuilder.setOperation(Operation.Add);
		fwdObjBuilder.setFlag(Flag.Versatile);
		MatchBuilder matchBuilder = new MatchBuilder();

		// set Ethernet type - IPv4 
		EthernetMatch etherMatch =
		AtriumUtils.getEtherMatch(Bgprouter.IPV4_ETH_TYPE);
		matchBuilder.setEthernetMatch(etherMatch);

		// Ip type Match 
		IpMatch ipMatch = AtriumUtils.getTcpIpMatchType();
		matchBuilder.setIpMatch(ipMatch);

		// TCP Src/Dest
		if (isSrc) {
			TcpMatch tcpMatch = AtriumUtils.getTcpMatch(BGP_PORT, true);
			matchBuilder.setLayer4Match(tcpMatch);
		} else {
			TcpMatch tcpMatch = AtriumUtils.getTcpMatch(BGP_PORT, false);
			matchBuilder.setLayer4Match(tcpMatch);
		}

		// Action - punt to controller
		ActionData puntAction = new ActionData(ActionUtils.punt_to_controller, new String[] { null });

		fwdObjBuilder.setMatch(matchBuilder.build());
		List<Action> actions = new ArrayList<>();
		actions.add(puntAction.buildAction());
		fwdObjBuilder.setAction(actions);

		return fwdObjBuilder.build();
	}

	@Override
	public void onIpv4PacketReceived(Ipv4PacketReceived packetReceived) {
		if (packetReceived == null || packetReceived.getPacketChain() == null) {
			return;
		}
		RawPacket rawPacket = null;
		EthernetPacket ethernetPacket = null;
		Ipv4Packet ipv4Packet = null;
		for (PacketChain packetChain : packetReceived.getPacketChain()) {
			if (packetChain.getPacket() instanceof RawPacket) {
				rawPacket = (RawPacket) packetChain.getPacket();
			} else if (packetChain.getPacket() instanceof EthernetPacket) {
				ethernetPacket = (EthernetPacket) packetChain.getPacket();
			} else if (packetChain.getPacket() instanceof Ipv4Packet) {
				ipv4Packet = (Ipv4Packet) packetChain.getPacket();
			}
		}
		if (rawPacket == null || ethernetPacket == null || ipv4Packet == null) {
			LOG.info("Ipv4 somehting null");
			return;
		}

		// Currently this is handled only for IPv4. Need to check for ipv6 as
		// well
		List<Header8021q> list = ethernetPacket.getHeader8021q();
		int offset = TCP_OFFSET;
		if (list != null && !list.isEmpty()) {
			offset = TCP_OFFSET + 4;
		}

		byte[] payload = packetReceived.getPayload();
		try {
			AtriumTCPHeader header = AtriumTCPHeader.decodeTCPHeader(payload, offset);

			if (header.getSourcePort() == BGP_PORT || header.getDestinationPort() == BGP_PORT
					|| ipv4Packet.getProtocol() == KnownIpProtocols.Icmp) {
				/*
				 * TODO : Identify the egressNodeconnector of DP/CP
				 */

				NodeConnectorRef ingressNCRef = rawPacket.getIngress();
				NodeConnector ingressNC = null;
				ingressNC = AtriumUtils.getNodeConnector(dataBroker, ingressNCRef);
				if (ingressNC == null) {
					return;
				}
				NodeId ingressNodeId = AtriumUtils.getNodeIdFromNodeConnectorId(ingressNC.getId());
				String portNum = AtriumUtils.getPortNoFromNodeConnectorId(ingressNC.getId());
				Long ingressPort = Long.valueOf(portNum);

				Ipv4Address dstAddress = ipv4Packet.getDestinationIpv4();
				NodeConnectorRef egressNodeConnectorRef = null;
				if (ingressNodeId.equals(bgpSpeaker.getAttachmentDpId())
						&& ingressPort.equals(bgpSpeaker.getAttachmentPort())) {
					
					BgpPeer bgpPeer = configService.getBgpPeerByIpAddress(new IpAddress((dstAddress)));
					if(bgpPeer != null) {
						egressNodeConnectorRef = AtriumUtils.getNodeConnRef(bgpPeer.getPeerDpId(), bgpPeer.getPeerPort());
					}
					
				}

				for (InterfaceAddresses addr : bgpSpeaker.getInterfaceAddresses()) {
					InterfaceAddressesBuilder builder = new InterfaceAddressesBuilder(addr);
					if (builder.getIpAddress().equals(new IpAddress(dstAddress))) {
						egressNodeConnectorRef = AtriumUtils.getNodeConnRef(bgpSpeaker.getAttachmentDpId(),
								bgpSpeaker.getAttachmentPort());
					}
				}

				sendPacketOut(payload, egressNodeConnectorRef);
			}

		} catch (AtriumTCPDecodeException e) {
			e.printStackTrace();
		}

	}

	private void sendPacketOut(byte[] payload, NodeConnectorRef egress) {
		if (egress == null) {
			LOG.info("Egress is null");
			return;
		}

		InstanceIdentifier<Node> egressNodePath = getNodePath(egress.getValue());

		TransmitPacketInput input = new TransmitPacketInputBuilder() //
				.setPayload(payload) //
				.setNode(new NodeRef(egressNodePath)) //
				.setEgress(egress) //
				.build();

		Future<RpcResult<Void>> future = packetService.transmitPacket(input);
		JdkFutureAdapters.listenInPoolThread(future);
	}

	private InstanceIdentifier<Node> getNodePath(final InstanceIdentifier<?> nodeChild) {
		return nodeChild.firstIdentifierOf(Node.class);
	}

}
