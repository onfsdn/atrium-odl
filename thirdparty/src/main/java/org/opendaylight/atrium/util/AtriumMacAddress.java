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

import java.util.Arrays;

/**
 * The class representing MAC address.
 */
public class AtriumMacAddress {

    public static final AtriumMacAddress ZERO = valueOf("00:00:00:00:00:00");
    public static final AtriumMacAddress BROADCAST = valueOf("ff:ff:ff:ff:ff:ff");

    private static final byte[] LL = new byte[]{
            0x01, (byte) 0x80, (byte) 0xc2, 0x00, 0x00,
            0x00, 0x0e, 0x03
    };

    public static final int MAC_ADDRESS_LENGTH = 6;
    private byte[] address = new byte[AtriumMacAddress.MAC_ADDRESS_LENGTH];

    public AtriumMacAddress(final byte[] address) {
        this.address = Arrays.copyOf(address, AtriumMacAddress.MAC_ADDRESS_LENGTH);
    }

    /**
     * Returns a MAC address instance representing the value of the specified
     * {@code String}.
     *
     * @param address the String representation of the MAC Address to be parsed.
     * @return a MAC Address instance representing the value of the specified
     * {@code String}.
     * @throws IllegalArgumentException if the string cannot be parsed as a MAC address.
     */
    public static AtriumMacAddress valueOf(final String address) {
        final String[] elements = address.split(":");
        if (elements.length != AtriumMacAddress.MAC_ADDRESS_LENGTH) {
            throw new IllegalArgumentException(
                    "Specified MAC Address must contain 12 hex digits"
                            + " separated pairwise by :'s.");
        }

        final byte[] addressInBytes = new byte[AtriumMacAddress.MAC_ADDRESS_LENGTH];
        for (int i = 0; i < AtriumMacAddress.MAC_ADDRESS_LENGTH; i++) {
            final String element = elements[i];
            addressInBytes[i] = (byte) Integer.parseInt(element, 16);
        }

        return new AtriumMacAddress(addressInBytes);
    }

    /**
     * Returns a MAC address instance representing the specified {@code byte}
     * array.
     *
     * @param address the byte array to be parsed.
     * @return a MAC address instance representing the specified {@code byte}
     * array.
     * @throws IllegalArgumentException if the byte array cannot be parsed as a MAC address.
     */
    public static AtriumMacAddress valueOf(final byte[] address) {
        if (address.length != AtriumMacAddress.MAC_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("the length is not "
                                                       + AtriumMacAddress.MAC_ADDRESS_LENGTH);
        }

        return new AtriumMacAddress(address);
    }

    /**
     * Returns a MAC address instance representing the specified {@code long}
     * value. The lower 48 bits of the long value are used to parse as a MAC
     * address.
     *
     * @param address the long value to be parsed. The lower 48 bits are used for a
     *                MAC address.
     * @return a MAC address instance representing the specified {@code long}
     * value.
     * @throws IllegalArgumentException if the long value cannot be parsed as a MAC address.
     */
    public static AtriumMacAddress valueOf(final long address) {
        final byte[] addressInBytes = new byte[]{
                (byte) (address >> 40 & 0xff), (byte) (address >> 32 & 0xff),
                (byte) (address >> 24 & 0xff), (byte) (address >> 16 & 0xff),
                (byte) (address >> 8 & 0xff), (byte) (address >> 0 & 0xff)};

        return new AtriumMacAddress(addressInBytes);
    }

    /**
     * Returns the length of the {@code MACAddress}.
     *
     * @return the length of the {@code MACAddress}.
     */
    public int length() {
        return this.address.length;
    }

    /**
     * Returns the value of the {@code MACAddress} as a {@code byte} array.
     *
     * @return the numeric value represented by this object after conversion to
     * type {@code byte} array.
     */
    public byte[] toBytes() {
        return Arrays.copyOf(this.address, this.address.length);
    }

    /**
     * Returns the value of the {@code MACAddress} as a {@code long}.
     *
     * @return the numeric value represented by this object after conversion to
     * type {@code long}.
     */
    public long toLong() {
        long mac = 0;
        for (int i = 0; i < 6; i++) {
            final long t = (this.address[i] & 0xffL) << (5 - i) * 8;
            mac |= t;
        }
        return mac;
    }

    /**
     * Returns {@code true} if the MAC address is the broadcast address.
     *
     * @return {@code true} if the MAC address is the broadcast address.
     */
    public boolean isBroadcast() {
        for (final byte b : this.address) {
            if (b != -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the MAC address is the multicast address.
     *
     * @return {@code true} if the MAC address is the multicast address.
     */
    public boolean isMulticast() {
        if (this.isBroadcast()) {
            return false;
        }
        return (this.address[0] & 0x01) != 0;
    }

    /**
     * Returns true if this MAC address is link local.
     *
     * @return true if link local
     */
    public boolean isLinkLocal() {
        return LL[0] == address[0] && LL[1] == address[1] && LL[2] == address[2] &&
                LL[3] == address[3] && LL[4] == address[4] &&
                (LL[5] == address[5] || LL[6] == address[5] || LL[7] == address[5]);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof AtriumMacAddress)) {
            return false;
        }

        final AtriumMacAddress other = (AtriumMacAddress) o;
        return Arrays.equals(this.address, other.address);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toLong());
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : this.address) {
            if (builder.length() > 0) {
                builder.append(":");
            }
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }

    /**
     * @return MAC address in string representation without colons (useful for
     * radix tree storage)
     */
    public String toStringNoColon() {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : this.address) {
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }
}
