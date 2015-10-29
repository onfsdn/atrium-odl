/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.atriumutil;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;

public class ActionData implements Serializable{

    private static final long serialVersionUID = 1L;
    private final ActionUtils m_actionType;
    private String[] m_asActionValues = null;
    private BigInteger [] m_aBigIntValues;
    private int m_actionKey = 0;

    public ActionData(ActionData action) {
        super();
        m_actionType = action.m_actionType;
        m_actionKey = action.m_actionKey;
        m_asActionValues = Arrays.copyOf(action.m_asActionValues, action.m_asActionValues.length);
    }

    public ActionData(ActionUtils actionType, String[] asActionValues) {
        m_actionType = actionType;
        m_actionKey = 0;
        m_asActionValues = asActionValues;
    }

    public ActionData(ActionUtils actionType, BigInteger[] aBigIntValues) {
        m_actionType = actionType;
        m_actionKey = 0;
        m_aBigIntValues = aBigIntValues;
    }

    public void setActionKey(int key) {
        m_actionKey = key;
    }

    public int getActionKey() {
        return m_actionKey;
    }

    public Action buildAction() {
        return m_actionType.buildAction(this);
    }

    public ActionUtils getActionType() {
        return m_actionType;
    }

    public String[] getActionValues() {
        return m_asActionValues;
    }

    public BigInteger[] getBigActionValues() {
        return m_aBigIntValues;
    }
}