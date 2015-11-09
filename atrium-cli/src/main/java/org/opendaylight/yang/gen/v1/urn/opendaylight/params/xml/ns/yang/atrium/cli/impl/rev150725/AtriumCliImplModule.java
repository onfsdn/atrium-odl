package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.atrium.cli.impl.rev150725;

import org.opendaylight.atrium.cli.AtriumCli;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;

public class AtriumCliImplModule extends
		org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.atrium.cli.impl.rev150725.AbstractAtriumCliImplModule {
	public AtriumCliImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
		super(identifier, dependencyResolver);
	}

	public AtriumCliImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier,
			org.opendaylight.controller.config.api.DependencyResolver dependencyResolver,
			org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.atrium.cli.impl.rev150725.AtriumCliImplModule oldModule,
			java.lang.AutoCloseable oldInstance) {
		super(identifier, dependencyResolver, oldModule, oldInstance);
	}

	@Override
	public void customValidation() {
		// add custom validation form module attributes here.
	}

	@Override
	public java.lang.AutoCloseable createInstance() {
		DataBroker dataBroker = getDataBrokerDependency();
		AtriumCli atriumCli = new AtriumCli(dataBroker);
		getBrokerDependency().registerConsumer(atriumCli);
		return atriumCli;
	}

}
