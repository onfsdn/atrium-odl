/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.hostservice.api;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.HostNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.address.node.connector.ConnectorAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.host.AttachmentPoints;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.host.AttachmentPointsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.hostservice.api.rev150725.host.AttachmentPointsKey;

/**
 * The Class Host.
 */
public class Host {

	/** The host id. */
	HostId hostId; // IP Address in the Atrium context
	HostNode hostNode;
	private List<AttachmentPointsBuilder> apbs;
	private HostNodeBuilder hostNodeBuilder;
	private NodeBuilder nodeBuilder;

	/**
	 * Hosttracker's prefix for nodes stored on MD-SAL.
	 */
	public static final String NODE_PREFIX = "host:";

	public static Host createHost(Node node) {
		HostNode hostNode = node.getAugmentation(HostNode.class);
		return new Host(hostNode.getId(), hostNode.getConnectorAddress(), hostNode.getAttachmentPoints());
	}

	private Host() {
		apbs = new ArrayList<>();
		hostNodeBuilder = new HostNodeBuilder();
	}

	public Host(HostId hId, List<ConnectorAddress> addrs, List<AttachmentPoints> aps) throws InvalidParameterException {
		this();
		hostNodeBuilder.getConnectorAddress();
		if (hId == null) {
			throw new InvalidParameterException("A host must have a HostId");
		}
		hostNodeBuilder.setId(hId);
		for (AttachmentPoints ap : aps) {
			apbs.add(new AttachmentPointsBuilder(ap));
		}
		nodeBuilder = createNodeBuilder(hostNodeBuilder, apbs);
	}

	public Host(ConnectorAddress addrs, NodeConnector nodeConnector) throws InvalidParameterException {
		this();
		List<ConnectorAddress> setAddrs = new ArrayList<>();
		if (addrs != null) {
			setAddrs.add(addrs);
		}
		hostNodeBuilder.setConnectorAddress(setAddrs);
		HostId hId = createHostId(addrs);
		if (hId == null) {
			throw new InvalidParameterException(
					"This host doesn't contain a valid MAC address to assign a valid HostId");
		}
		hostNodeBuilder.setId(hId);
		if (nodeConnector != null) {
			AttachmentPointsBuilder apb = createAPsfromNodeConnector(nodeConnector);
			apb.setActive(Boolean.TRUE);
			apbs.add(apb);
		}
		nodeBuilder = createNodeBuilder(hostNodeBuilder, apbs);
	}

	/**
	 * Creates a NodeBuilder based on the given HostNodeBuilder.
	 *
	 * @param hostNode
	 *            The HostNodeBuilder where the AttachmentPoints and Id are.
	 * @return A NodeBuilder with the same Id of HostNodeBuilder and a list of
	 *         TerminationPoint corresponding to each HostNodeBuilder's
	 *         AttachmentPoints.
	 */
	private NodeBuilder createNodeBuilder(HostNodeBuilder hostNode, List<AttachmentPointsBuilder> apbs) {
		List<TerminationPoint> tps = new ArrayList<>();
		for (AttachmentPointsBuilder atb : apbs) {
			TerminationPoint tp = createTerminationPoint(hostNode);
			tps.add(tp);
			atb.setCorrespondingTp(tp.getTpId());
		}
		NodeBuilder node = new NodeBuilder().setNodeId(createNodeId(hostNode)).setTerminationPoint(tps);
		node.setKey(new NodeKey(node.getNodeId()));

		return node;
	}

	/**
	 * Creates a HostId based on the MAC values present in Addresses, if MAC is
	 * null then returns null.
	 *
	 * @param addrs
	 *            Address containing a MAC address.
	 * @return A new HostId based on the MAC address present in addrs, null if
	 *         addrs is null or MAC is null.
	 */
	public static HostId createHostId(ConnectorAddress addrs) {
		if (addrs != null && addrs.getMac() != null) {
			return new HostId(addrs.getMac().getValue());
		} else {
			return null;
		}
	}

	/**
	 * Creates a NodeId based on the Id stored on the given HostNodeBuilder
	 * adding the NODE_PREFIX.
	 *
	 * @param host
	 *            HostNodeBuilder that contains an Id
	 * @return A new NodeId.
	 */
	private static NodeId createNodeId(HostNodeBuilder host) {
		return new NodeId(NODE_PREFIX + host.getId().getValue());
	}

	/**
	 * Creates a new TerminationPoint for this Host.
	 *
	 * @param hn
	 *            HostNodeBuilder containing an Id
	 * @param atb
	 *            AttachmentPointsBuilder containing a TpId
	 * @return A new TerminationPoint with an unique TpId
	 */
	private TerminationPoint createTerminationPoint(HostNodeBuilder hn) {
		TerminationPoint tp = new TerminationPointBuilder().setTpId(new TpId(NODE_PREFIX + hn.getId().getValue()))
				.build();
		return tp;
	}

	/**
	 * Returns this HostId
	 *
	 * @return this HostId.
	 */
	public synchronized HostId getId() {
		return hostNodeBuilder.getId();
	}

	public synchronized Node getNode() {
		List<AttachmentPoints> attachmentPoints = new ArrayList<>();
		for (AttachmentPointsBuilder apb : apbs) {
			attachmentPoints.add(apb.build());
		}
		hostNodeBuilder.setAttachmentPoints(attachmentPoints);
		return nodeBuilder.addAugmentation(HostNode.class, hostNodeBuilder.build()).build();
	}

	public synchronized HostNode getHostNode() {
		return hostNodeBuilder.build();
	}

	public static AttachmentPointsBuilder createAPsfromNodeConnector(NodeConnector nc) {
		TpId tpId = new TpId(nc.getId().getValue());
		return createAPsfromTP(tpId);
	}

	public static AttachmentPointsBuilder createAPsfromTP(TpId tpId) {
		AttachmentPointsBuilder at = new AttachmentPointsBuilder()//
				.setTpId(tpId)//
				.setKey(new AttachmentPointsKey(tpId));
		return at;
	}

}
