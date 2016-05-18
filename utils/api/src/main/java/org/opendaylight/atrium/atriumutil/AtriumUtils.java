/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.atrium.atriumutil;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.opendaylight.atrium.util.AtriumInterfaceIpAddress;
import org.opendaylight.atrium.util.AtriumIpPrefix;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Layer3Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4Match;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv6MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

// TODO: Auto-generated Javadoc
/**
 * The Class AtriumUtils.
 */
public class AtriumUtils {
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(AtriumUtils.class);

	/**
	 * Gets the dpn from node connector id.
	 *
	 * @param portId
	 *            the port id
	 * @return the dpn from node connector id
	 */
	public static String getDpnFromNodeConnectorId(NodeConnectorId portId) {
		/*
		 * NodeConnectorId is of form 'openflow:dpnid:portnum'
		 */
		String[] split = portId.getValue().split(AtriumConstants.OF_URI_SEPARATOR);
		if (split == null || split.length <= 2) {
			return null;
		} else {
			return split[1];
		}
	}

	/**
	 * Gets the port no from node connector id.
	 *
	 * @param portId
	 *            the port id
	 * @return the port no from node connector id
	 */
	public static String getPortNoFromNodeConnectorId(NodeConnectorId portId) {
		/*
		 * NodeConnectorId is of form 'openflow:dpnid:portnum'
		 */
		String[] split = portId.getValue().split(AtriumConstants.OF_URI_SEPARATOR);
		return split[2];
	}

	/**
	 * Builds the dpn node id.
	 *
	 * @param dpnId
	 *            the dpn id
	 * @return the node id
	 */
	public static NodeId buildDpnNodeId(BigInteger dpnId) {
		return new NodeId(AtriumConstants.OF_URI_PREFIX + dpnId);
	}

	/**
	 * Gets the nodes.
	 *
	 * @param dataBroker
	 *            the data broker
	 * @return the nodes
	 */
	public static List<Node> getNodes(DataBroker dataBroker) {
		Nodes nodes = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		InstanceIdentifier.InstanceIdentifierBuilder<Nodes> nodesInsIdBuilder = InstanceIdentifier
				.<Nodes> builder(Nodes.class);
		Optional<Nodes> nodesDataObjectOptional = null;
		try {
			nodesDataObjectOptional = readOnlyTransaction
					.read(LogicalDatastoreType.OPERATIONAL, nodesInsIdBuilder.build()).get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		if (nodesDataObjectOptional != null && nodesDataObjectOptional.isPresent()) {
			nodes = nodesDataObjectOptional.get();
		} else {

		}
		if (nodes != null) {
			return nodes.getNode();
		} else {
			return null;
		}
	}

	/**
	 * Checks if is node available.
	 *
	 * @param broker
	 *            the broker
	 * @param deviceId
	 *            the device id
	 * @return true, if is node available
	 */
	public static boolean isNodeAvailable(DataBroker broker, NodeId deviceId) {
		List<Node> nodes = AtriumUtils.getNodes(broker);
		if (nodes == null) {
			return false;
		}

		for (Node node : nodes) {
			if (node.getId().equals(deviceId)) {
				return true;
			}
		}

		return false;

	}

	/**
	 * Gets the dpn id from node name.
	 *
	 * @param nodeId
	 *            the node id
	 * @return the dpn id from node name
	 */
	public static BigInteger getDpnIdFromNodeName(NodeId nodeId) {
		return getDpnIdFromNodeName(nodeId.getValue());
	}

	/**
	 * Gets the dpn id from node name.
	 *
	 * @param sMdsalNodeName
	 *            the s mdsal node name
	 * @return the dpn id from node name
	 */
	public static BigInteger getDpnIdFromNodeName(String sMdsalNodeName) {
		String sDpId = sMdsalNodeName.substring(sMdsalNodeName.lastIndexOf(":") + 1);
		return new BigInteger(sDpId);
	}

	/**
	 * Gets the node connector.
	 *
	 * @param dataBroker
	 *            the data broker
	 * @param ncRef
	 *            the nc ref
	 * @return the node connector
	 */
	public static NodeConnector getNodeConnector(DataBroker dataBroker, NodeConnectorRef ncRef) {
		NodeConnector nc = null;
		ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
		try {
			Optional<NodeConnector> dataObjectOptional = readOnlyTransaction
					.read(LogicalDatastoreType.OPERATIONAL, (InstanceIdentifier<NodeConnector>) ncRef.getValue()).get();
			if (dataObjectOptional.isPresent())
				nc = dataObjectOptional.get();
		} catch (Exception e) {
			throw new RuntimeException("Error reading from configuration store, node connector : " + ncRef, e);
		}
		return nc;

	}

	/**
	 * Gets the node id from node connector id.
	 *
	 * @param ncId
	 *            the nc id
	 * @return the node id from node connector id
	 */
	public static NodeId getNodeIdFromNodeConnectorId(NodeConnectorId ncId) {
		if (ncId == null) {
			return null;
		}
		String nodeName = getDpnFromNodeConnectorId(ncId);
		BigInteger dpnId = getDpnIdFromNodeName(nodeName);
		return buildDpnNodeId(dpnId);
	}

	/**
	 * Gets the node conn ref.
	 *
	 * @param nodeId
	 *            the node id
	 * @param port
	 *            the port
	 * @return the node conn ref
	 */
	public static NodeConnectorRef getNodeConnRef(final NodeId nodeId, final Long port) {
		StringBuilder _stringBuilder = new StringBuilder(nodeId.getValue());
		StringBuilder _append = _stringBuilder.append(":");
		StringBuilder sBuild = _append.append(port);
		String _string = sBuild.toString();
		NodeConnectorId _nodeConnectorId = new NodeConnectorId(_string);
		NodeConnectorKey _nodeConnectorKey = new NodeConnectorKey(_nodeConnectorId);
		NodeConnectorKey nConKey = _nodeConnectorKey;
		InstanceIdentifierBuilder<Nodes> _builder = InstanceIdentifier.<Nodes> builder(Nodes.class);
		NodeId _nodeId = new NodeId(nodeId);
		NodeKey _nodeKey = new NodeKey(_nodeId);
		InstanceIdentifierBuilder<Node> _child = _builder.<Node, NodeKey> child(Node.class, _nodeKey);
		InstanceIdentifierBuilder<NodeConnector> _child_1 = _child
				.<NodeConnector, NodeConnectorKey> child(NodeConnector.class, nConKey);
		NodeConnectorRef _nodeConnectorRef = new NodeConnectorRef(_child_1.build());
		return _nodeConnectorRef;
	}

	/**
	 * Gets the ether match.
	 *
	 * @param etherType
	 *            the ether type
	 * @return the ether match
	 */
	public static EthernetMatch getEtherMatch(EthernetType etherType) {
		return new EthernetMatchBuilder().setEthernetType(etherType).build();
	}

	/**
	 * Gets the ether match.
	 *
	 * @param macAddress
	 *            the src/destination MAC
	 * @param isSrc
	 *            indicates source or destination MAC
	 * @return the ether match
	 */
	public static EthernetMatch getEtherMatch(MacAddress macAddress, boolean isSrc) {
		if (isSrc) {
			return new EthernetMatchBuilder()
					.setEthernetSource(new EthernetSourceBuilder().setAddress(macAddress).build()).build();
		} else {
			return new EthernetMatchBuilder()
					.setEthernetDestination(new EthernetDestinationBuilder().setAddress(macAddress).build()).build();
		}
	}

	public static EthernetMatch getEtherMatch(MacAddress macAddress, EthernetType etherType, boolean isSrc) {
		if (isSrc) {
			return new EthernetMatchBuilder().setEthernetType(etherType)
					.setEthernetSource(new EthernetSourceBuilder().setAddress(macAddress).build()).build();
		} else {
			return new EthernetMatchBuilder().setEthernetType(etherType)
					.setEthernetDestination(new EthernetDestinationBuilder().setAddress(macAddress).build()).build();
		}
	}

	public static VlanMatch getVlanMatch(int vlanId) {
		return new VlanMatchBuilder().setVlanId(new VlanIdBuilder().setVlanId(
				new org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId(Integer.valueOf(vlanId)))
				.setVlanIdPresent(true).build()).build();
	}

	public static Ipv4Match getL3Match(AtriumInterfaceIpAddress intfAddr, boolean isSrc) {

		if (isSrc) {
			return new Ipv4MatchBuilder()
					.setIpv4Source(new Ipv4Prefix(AtriumIpPrefix.valueOf(intfAddr.ipAddress(), 32).toString())).build();
		} else {
			return new Ipv4MatchBuilder()
					.setIpv4Destination(new Ipv4Prefix(AtriumIpPrefix.valueOf(intfAddr.ipAddress(), 32).toString())).build();
		}
	}

	public static IpMatch getTcpIpMatchType() {
		return new IpMatchBuilder().setIpProtocol(AtriumConstants.TCP.shortValue()).build();
	}
	
	public static IpMatch getIcmpIpMatchType() {
		return new IpMatchBuilder().setIpProtocol(AtriumConstants.ICMP.shortValue()).build();
	}

	public static TcpMatch getTcpMatch(int port, boolean isSrc) {
		if (isSrc) {
			return new TcpMatchBuilder().setTcpSourcePort(new PortNumber(Integer.valueOf(port))).build();
		} else {
			return new TcpMatchBuilder().setTcpDestinationPort(new PortNumber(Integer.valueOf(port))).build();
		}
	}

	/**
	 * Gets the ip match.
	 *
	 * @param ipProtoValue
	 *            the ip proto value
	 * @return the ip match
	 */
	public static IpMatch getIpMatch(short ipProtoValue) {
		return new IpMatchBuilder().setIpProtocol(ipProtoValue).build();
	}

	/**
	 * Creates the layer3 match.
	 *
	 * @param ipPrefix
	 *            the ip prefix
	 * @param isSrc
	 *            the is src
	 * @return the layer3 match
	 */
	public static Layer3Match createLayer3Match(AtriumIpPrefix ipPrefix, boolean isSrc) {
		if (ipPrefix.isIp4()) {
			if (isSrc) {
				return new Ipv4MatchBuilder().setIpv4Source(new Ipv4Prefix(ipPrefix.getIp4Prefix().toString())).build();
			} else {
				return new Ipv4MatchBuilder().setIpv4Destination(new Ipv4Prefix(ipPrefix.getIp4Prefix().toString()))
						.build();
			}
		} else {
			if (isSrc) {
				return new Ipv6MatchBuilder().setIpv6Source(new Ipv6Prefix(ipPrefix.getIp6Prefix().toString())).build();
			} else {
				return new Ipv6MatchBuilder().setIpv6Destination(new Ipv6Prefix(ipPrefix.getIp6Prefix().toString()))
						.build();
			}
		}
	}

	public static String hexDpidStringToOpenFlowDpid(String values) {
		long longValue = new BigInteger(values.replaceAll(":", ""), 16).longValue();
		String ofDpid = "openflow:" + longValue;
		return ofDpid;
	}

}
