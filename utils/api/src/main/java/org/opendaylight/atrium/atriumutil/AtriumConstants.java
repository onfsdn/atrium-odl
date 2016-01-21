/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.atriumutil;

public class AtriumConstants {
    public static final String OF_URI_PREFIX = "openflow:";
    public static final String OF_URI_SEPARATOR = ":";
    /**
     * ARP ethertype
     */
    public static final Long ARP = Long.valueOf(0x0806);
    /**
     * IPv4 ethertype
     */
    public static final Long IPv4 = Long.valueOf(0x0800);
    /**
     * IPv6 ethertype
     */
    public static final Long IPv6 = Long.valueOf(0x86DD);

    /**
     * 802_1Q ethertype
     */
    public static final Long ETH_802_1Q = Long.valueOf(0x8100);


    public static final Long TCP = Long.valueOf(6);

    public static final Long UDP = Long.valueOf(17);

    public static final Long SCTP = Long.valueOf(132);
    
    public static final Long ICMP= Long.valueOf(1);
    

}
