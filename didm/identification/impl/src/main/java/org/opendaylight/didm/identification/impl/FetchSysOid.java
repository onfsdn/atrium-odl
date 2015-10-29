/*
 * Copyright (c) 2015 Hewlett-Packard Development Company, L.P. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.didm.identification.impl;

import com.google.common.base.Preconditions;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpGetType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.SnmpService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.snmp.rev140922.snmp.get.output.Results;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FetchSysOid {

    private static final Logger LOG = LoggerFactory.getLogger(FetchSysOid.class);
    private static final String SYS_OID = "1.3.6.1.2.1.1.2.0";

    private final RpcProviderRegistry rpcProviderRegistry;

    public FetchSysOid(RpcProviderRegistry rpcProviderRegistry) {
        this.rpcProviderRegistry = Preconditions.checkNotNull(rpcProviderRegistry);
    }

    public String fetch(String ipStr) {
        // get a reference to the sendSnmpQuery RPC
        SnmpService rpcService = rpcProviderRegistry.getRpcService(SnmpService.class);

        Ipv4Address ip = new Ipv4Address(ipStr);

        // create the input object with community, ip address, oid, and query type
        SnmpGetInputBuilder input = new SnmpGetInputBuilder();
        input.setCommunity("public"); // TODO: Use AAA KeyManager when available.
        input.setIpAddress(ip);
        input.setOid(SYS_OID);
        input.setGetType(SnmpGetType.GET);

        // call the RPC
        try {
            Future<RpcResult<SnmpGetOutput>> resultFuture = rpcService.snmpGet(input.build());

            RpcResult<SnmpGetOutput> result = resultFuture.get();
            if (result.isSuccessful()) {
                SnmpGetOutput output = result.getResult();
                List<Results> snmpResults = output.getResults();
                if (snmpResults.size() == 1) {
                    return snmpResults.get(0).getValue();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("error while attempting to get snmp mib object: " + e);
        }
        return null;
    }
}
