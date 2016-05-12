/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.atrium.routingservice.api;

import java.util.Collection;

/**
 * A component that is able to process Forwarding Information Base (FIB) updates.
 */
public interface FibListener {

    /**
     * Signals the FIB component of changes to the FIB.
     *
     * @param updates FIB updates of the UDPATE type
     * @param withdraws FIB updates of the WITHDRAW type
     */
    void update(Collection<AtriumFibUpdate> updates, Collection<AtriumFibUpdate> withdraws);

}