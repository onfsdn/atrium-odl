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

/**
 * Representation of a VLAN ID.
 */
public class AtriumVlanId {

    private final short value;

    // Based on convention used elsewhere? Check and change if needed
    public static final short UNTAGGED = (short) 0xffff;

    // In a traffic selector, this means that a VLAN ID must be present, but
    // can have any value. We use the same value as OpenFlow, but this is not
    // required.
    public static final short ANY_VALUE = (short) 0x1000;

    public static final AtriumVlanId NONE = AtriumVlanId.vlanId(UNTAGGED);
    public static final AtriumVlanId ANY = AtriumVlanId.vlanId(ANY_VALUE);

    // A VLAN ID is actually 12 bits of a VLAN tag.
    public static final short MAX_VLAN = 4095;

    protected AtriumVlanId() {
        this.value = UNTAGGED;
    }

    protected AtriumVlanId(short value) {
        this.value = value;
    }

    public static AtriumVlanId vlanId() {
        return new AtriumVlanId(UNTAGGED);
    }

    public static AtriumVlanId vlanId(short value) {
        if (value == UNTAGGED) {
            return new AtriumVlanId();
        }

        if (value == ANY_VALUE) {
            return new AtriumVlanId(ANY_VALUE);
        }

        if (value > MAX_VLAN) {
            throw new IllegalArgumentException(
                    "value exceeds allowed maximum VLAN ID value (4095)");
        }
        return new AtriumVlanId(value);
    }

    public short toShort() {
        return this.value;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AtriumVlanId) {

            AtriumVlanId other = (AtriumVlanId) obj;

             if (this.value == other.value) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return this.value;
    }

    @Override
    public String toString() {
        if (this.value == ANY_VALUE) {
            return "Any";
        }
        return String.valueOf(this.value);
    }
}