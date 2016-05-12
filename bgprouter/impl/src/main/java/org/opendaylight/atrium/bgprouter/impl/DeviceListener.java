/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.bgprouter.impl;

import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

public class DeviceListener implements DataChangeListener {
	private static final Logger LOG = LoggerFactory.getLogger(DeviceListener.class);
	private ListenerRegistration<DataChangeListener> listenerRegistration;
	private Bgprouter bgpRouter;
	private DataBroker dataBroker;

	public DeviceListener(final DataBroker dataBroker, Bgprouter router) {
		this.dataBroker = dataBroker;
		registerListener(dataBroker);
		bgpRouter = router;
	}

	private void registerListener(final DataBroker db) {
		try {
			listenerRegistration = db.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, getWildCardPath(),
					DeviceListener.this, DataChangeScope.ONE);
		} catch (final Exception e) {
			LOG.error("FibNodeConnectorListener: DataChange listener registration fail!", e);
			throw new IllegalStateException("FibNodeConnectorListener: registration Listener failed.", e);
		}
	}

	private InstanceIdentifier<FlowCapableNode> getWildCardPath() {
		return InstanceIdentifier.create(Nodes.class).child(Node.class).augmentation(FlowCapableNode.class);
	}

	@Override
	public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> change) {
		Map<InstanceIdentifier<?>, DataObject> createdData = change.getCreatedData();

		for (Map.Entry<InstanceIdentifier<?>, DataObject> entrySet : createdData.entrySet()) {

			InstanceIdentifier<?> iiD = entrySet.getKey();
			final DataObject dataObject = entrySet.getValue();

			if (dataObject instanceof FlowCapableNode) {
				final InstanceIdentifier<Node> path = iiD.firstIdentifierOf(Node.class);

				ReadOnlyTransaction readOnlyTransaction = dataBroker.newReadOnlyTransaction();
				final CheckedFuture<Optional<Node>, ReadFailedException> readFuture = readOnlyTransaction
						.read(LogicalDatastoreType.OPERATIONAL, path);
				Futures.addCallback(readFuture, new FutureCallback<Optional<Node>>() {
					@Override
					public void onSuccess(Optional<Node> result) {
						if (result.isPresent()) {
							bgpRouter.processNodeAdd(result.get().getId());
							LOG.info("Node discovered and passed to processNodeAdd : " + result.get().getId());
						} else {
							LOG.info("Read succeeded, node doesn't exist: {}", path);
						}
					}

					@Override
					public void onFailure(Throwable t) {
						LOG.info("Failed to read Node: {}", path, t);
					}
				});
			}
		}
	}
}
