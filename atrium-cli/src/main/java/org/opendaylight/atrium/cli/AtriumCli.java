/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.cli;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtriumCli implements BindingAwareConsumer, AutoCloseable {

	private static DataBroker dataBroker;
	private static final Logger LOG = LoggerFactory.getLogger(AtriumCli.class);

	public AtriumCli(DataBroker db) {
		dataBroker = db;
	}

	public static DataBroker getDataBroker() {
		return dataBroker;
	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSessionInitialized(ConsumerContext arg0) {
		LOG.info("Atrium Cli Started");
	}
}
