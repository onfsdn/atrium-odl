package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.extreme.rev150211;

/*
 * Copyright (c) 2016 Extreme Networks.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Created by llam on 3/15/2016.
 */

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.didm.vendor.extreme.OpenFlowDeviceDriver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ExtremeModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.extreme.rev150211.AbstractExtremeModule {
    protected static final Logger LOG = LoggerFactory.getLogger(ExtremeModule.class);

    protected static final Class<? extends DeviceTypeBase> DEVICE_TYPE = ExtremeDeviceType.class;

    protected static final List<String> MANUFACTURERS = ImmutableList.of("Extreme Networks, Inc.");
    protected static final List<String> HARDWARE = ImmutableList.of("S1 Chassis",
                                                                    "S3 Chassis",
                                                                    "S8 Chassis",
                                                                    "SSA",
                                                                    "SSA Chassis",
                                                                    "SSA Chassis (0x15)");

    protected static final DeviceTypeInfo DEVICE_TYPE_INFO = new DeviceTypeInfoBuilder().setDeviceType(DEVICE_TYPE)
            .setOpenflowManufacturer(MANUFACTURERS)
            .setOpenflowHardware(HARDWARE).build();

    public ExtremeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public ExtremeModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.extreme.rev150211.ExtremeModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }
    
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("createInstance");

        final DataBroker dataBroker = getDataBrokerDependency();
        final InstanceIdentifier<DeviceTypeInfo> path = registerDeviceTypeInfo(dataBroker);
        final OpenFlowDeviceDriver ofDeviceDriver = new OpenFlowDeviceDriver(dataBroker, getRpcRegistryDependency());

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                LOG.debug("Closing");
                removeDeviceTypeInfo(dataBroker, path);
                ofDeviceDriver.close();
            }
        };
    }

    protected static InstanceIdentifier<DeviceTypeInfo> registerDeviceTypeInfo(DataBroker dataBroker) {
        LOG.debug("registerDeviceTypeInfo -> " + dataBroker);

        InstanceIdentifier<DeviceTypeInfo> path = createKeyedDeviceTypeInfoPath(DEVICE_TYPE);

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, DEVICE_TYPE_INFO, true);

        CheckedFuture<Void, TransactionCommitFailedException> future = writeTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            public void onSuccess(Void result) {
            	LOG.info("registerDeviceTypeInfo onSuccess");
            }

            public void onFailure(Throwable t) {
                LOG.error("registerDeviceTypeInfo onFailure -> " + t);
            }
        });

        return path;
    }

    protected static void removeDeviceTypeInfo(DataBroker dataBroker, InstanceIdentifier<DeviceTypeInfo> path) {
        LOG.debug("registerDeviceTypeInfo -> " + dataBroker);

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, path);

        CheckedFuture<Void, TransactionCommitFailedException> future = writeTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            public void onSuccess(Void result) {
                LOG.info("registerDeviceTypeInfo onSuccess");
            }

            public void onFailure(Throwable t) {
                LOG.error("registerDeviceTypeInfo onFailure -> " + t);
            }
        });
    }

    protected static InstanceIdentifier<DeviceTypeInfo> createKeyedDeviceTypeInfoPath(Class<? extends DeviceTypeBase> name) {
        Preconditions.checkNotNull(name);
        return InstanceIdentifier.builder(DeviceTypes.class).child(DeviceTypeInfo.class, new DeviceTypeInfoKey(name)).build();
    }
}
