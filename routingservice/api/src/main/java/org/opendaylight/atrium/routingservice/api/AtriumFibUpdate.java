/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.routingservice.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Represents a change to the Forwarding Information Base (FIB).
 */
public class AtriumFibUpdate {

    /**
     * Specifies the type of the FIB update.
     */
    public enum Type {
        /**
         * The update contains a new or updated FIB entry for a prefix.
         */
        UPDATE,

        /**
         * The update signals that a prefix should be removed from the FIB.
         */
        DELETE
    }

    private final Type type;
    private final AtriumFibEntry entry;

    /**
     * Creates a new FIB update.
     *
     * @param type type of the update
     * @param entry FIB entry describing the update
     */
    public AtriumFibUpdate(Type type, AtriumFibEntry entry) {
        this.type = type;
        this.entry = entry;
    }

    /**
     * Returns the type of the update.
     *
     * @return update type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the FIB entry which contains update information.
     *
     * @return the FIB entry
     */
    public AtriumFibEntry entry() {
        return entry;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AtriumFibUpdate)) {
            return false;
        }

        AtriumFibUpdate that = (AtriumFibUpdate) o;

        return Objects.equals(this.type, that.type) &&
                Objects.equals(this.entry, that.entry);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, entry);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type)
                .add("entry", entry)
                .toString();
    }
}
