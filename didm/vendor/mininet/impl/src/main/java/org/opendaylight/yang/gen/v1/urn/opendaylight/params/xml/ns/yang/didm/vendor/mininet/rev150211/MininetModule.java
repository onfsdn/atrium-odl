package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.mininet.rev150211;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.didm.vendor.mininet.OpenFlowDeviceDriver;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.DeviceTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.devicetypes.rev150202.device.types.DeviceTypeInfoKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.identification.rev150202.DeviceTypeBase;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * The Mininet config subsystem module manages:
 *   1) the mininet device type info
 *   2) the mininet OF driver
 */
public class MininetModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.mininet.rev150211.AbstractMininetModule {
    private static final Logger LOG = LoggerFactory.getLogger(MininetModule.class);

    private static final Class<? extends DeviceTypeBase> DEVICE_TYPE = MininetDeviceType.class;
    //private static final String MANUFACTURER = "Nicira, Inc.";
    private static final List<String> HARDWARE = ImmutableList.of("Open vSwitch", "NS1132", "NS1248", "NS2128");
    private static final List<String> MANUFACTURERS = ImmutableList.of("Nicira, Inc.", "NoviFlow Inc");
    private static final DeviceTypeInfo DEVICE_TYPE_INFO = new DeviceTypeInfoBuilder().setDeviceType(DEVICE_TYPE)
            .setOpenflowManufacturer(MANUFACTURERS)
            .setOpenflowHardware(HARDWARE).build();

    public MininetModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public MininetModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.didm.vendor.mininet.rev150211.MininetModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }
    
    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.debug("Registering Mininet DeviceTypeInfo");
        final DataBroker dataBroker = getDataBrokerDependency();
        final InstanceIdentifier<DeviceTypeInfo> path = registerDeviceTypeInfo(dataBroker);
        final OpenFlowDeviceDriver ofDeviceDriver = new OpenFlowDeviceDriver(dataBroker, getRpcRegistryDependency());

        return new AutoCloseable() {
            @Override
            public void close() throws Exception {
                LOG.debug("Closing");
                removeDeviceTypeInfo(dataBroker, path);

                // close drivers
                ofDeviceDriver.close();
            }
        };
    }

    private static InstanceIdentifier<DeviceTypeInfo> registerDeviceTypeInfo(DataBroker dataBroker) {
        InstanceIdentifier<DeviceTypeInfo> path = createKeyedDeviceTypeInfoPath(DEVICE_TYPE);

        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.merge(LogicalDatastoreType.CONFIGURATION, path, DEVICE_TYPE_INFO, true);

        CheckedFuture<Void, TransactionCommitFailedException> future = writeTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            	LOG.info("### Initialized the DeviceTypeInfo ### ");
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("### Failed to write DeviceTypeInfo ### ", t);
            }
        });

        return path;
    }

    private static void removeDeviceTypeInfo(DataBroker dataBroker, InstanceIdentifier<DeviceTypeInfo> path) {
        WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
        writeTx.delete(LogicalDatastoreType.CONFIGURATION, path);

        CheckedFuture<Void, TransactionCommitFailedException> future = writeTx.submit();
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
            }

            @Override
            public void onFailure(Throwable t) {
                LOG.error("Failed to delete DeviceTypeInfo", t);
            }
        });
    }

    private static InstanceIdentifier<DeviceTypeInfo> createKeyedDeviceTypeInfoPath(Class<? extends DeviceTypeBase> name) {
        Preconditions.checkNotNull(name);
        return InstanceIdentifier.builder(DeviceTypes.class).child(DeviceTypeInfo.class, new DeviceTypeInfoKey(name)).build();
    }

}
