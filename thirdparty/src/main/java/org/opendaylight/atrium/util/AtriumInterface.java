/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opendaylight.atrium.util;

import java.util.Objects;
import java.util.Set;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

/**
 * An Interface maps network configuration information (such as addresses and
 * vlans) to a port in the network.
 */
public class AtriumInterface {
	private final NodeConnector connectPoint;
	private final Set<AtriumInterfaceIpAddress> ipAddresses;
	private final MacAddress macAddress;
	private final AtriumVlanId vlan;
	// private final NodeId nodeId;

	/**
	 * Creates new Interface with the provided configuration.
	 *
	 * @param connectPoint
	 *            the connect point this interface maps to
	 * @param ipAddresses
	 *            Set of IP addresses
	 * @param macAddress
	 *            MAC address
	 * @param vlan
	 *            VLAN ID
	 */

	public AtriumInterface(NodeConnector connectPoint, Set<AtriumInterfaceIpAddress> ipAddresses, MacAddress macAddress,
			AtriumVlanId vlan) {
		this.connectPoint = connectPoint;
		this.ipAddresses = Sets.newHashSet(ipAddresses);
		this.macAddress = macAddress;
		this.vlan = vlan;
	}

	/**
	 * Retrieves the connection point that this interface maps to.
	 *
	 * @return the connection point
	 */
	public NodeConnector connectPoint() {
		return connectPoint;
	}

	/**
	 * Retrieves the set of IP addresses that are assigned to the interface.
	 *
	 * @return the set of interface IP addresses
	 */
	public Set<AtriumInterfaceIpAddress> ipAddresses() {
		return ipAddresses;
	}

	/**
	 * Retrieves the MAC address that is assigned to the interface.
	 *
	 * @return the MAC address
	 */
	public MacAddress mac() {
		return macAddress;
	}

	/**
	 * Retrieves the VLAN ID that is assigned to the interface.
	 *
	 * @return the VLAN ID
	 */
	public AtriumVlanId vlan() {
		return vlan;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof AtriumInterface)) {
			return false;
		}

		AtriumInterface otherInterface = (AtriumInterface) other;

		return Objects.equals(connectPoint, otherInterface.connectPoint)
				&& Objects.equals(ipAddresses, otherInterface.ipAddresses)
				&& Objects.equals(macAddress, otherInterface.macAddress) && Objects.equals(vlan, otherInterface.vlan);
	}

	@Override
	public int hashCode() {
		return Objects.hash(connectPoint, ipAddresses, macAddress, vlan);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(getClass()).add("connectPoint", connectPoint).add("ipAddresses", ipAddresses)
				.add("macAddress", macAddress).add("vlan", vlan).toString();
	}
}
