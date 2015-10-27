/*
 * Copyright (c) 2015 Wipro Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.atrium.atriumutil;

import java.net.InetAddress;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.OutputPortValues;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

public enum ActionUtils {
	
	output {
        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            String port = actionValues[0];
            int maxLength = 0;

            if (actionValues.length == 2) {
                maxLength = Integer.valueOf(actionValues[1]);
            }

            return new ActionBuilder().setAction(
                    new OutputActionCaseBuilder().setOutputAction(
                            new OutputActionBuilder().setMaxLength(Integer.valueOf(maxLength))
                                            .setOutputNodeConnector(new Uri(port)).build()).build())
                    .setKey(new ActionKey(actionInfo.getActionKey())).build();
        }
    },
	
    pop_vlan {
        @Override
        public Action buildAction(ActionData actionInfo) {
            return new ActionBuilder().setAction(
                    new PopVlanActionCaseBuilder().setPopVlanAction(new PopVlanActionBuilder().build()).build())
                    .setKey(new ActionKey(actionInfo.getActionKey())).build();
        }
    },

    push_vlan {
        @Override
        public Action buildAction(ActionData actionInfo) {
            return new ActionBuilder().setAction(
                    new PushVlanActionCaseBuilder().setPushVlanAction(
                            new PushVlanActionBuilder().setEthernetType(
                                    Integer.valueOf(AtriumConstants.ETH_802_1Q.intValue())).build()).build())
                                    .setKey(new ActionKey(actionInfo.getActionKey())).build();
        }
    },


    set_field_vlan_vid {
        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            int vlanId = Integer.valueOf(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setVlanMatch(
                                    new VlanMatchBuilder().setVlanId(
                                            new VlanIdBuilder().setVlanId(new VlanId(vlanId))
                                            .setVlanIdPresent(true).build()).build()).build()).build())
                                            .setKey(new ActionKey(actionInfo.getActionKey())).build();
        }
    },


    set_field_eth_dest {

        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            MacAddress mac = new MacAddress(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setEthernetMatch(
                                    new EthernetMatchBuilder().setEthernetDestination(
                                            new EthernetDestinationBuilder().setAddress(mac).build()).build())
                                            .build()).build()).setKey(new ActionKey(actionInfo.getActionKey())).build();

        }

    },

    punt_to_controller {
        @Override
        public Action buildAction(ActionData actionInfo) {
            ActionBuilder ab = new ActionBuilder();
            OutputActionBuilder output = new OutputActionBuilder();
            output.setMaxLength(0xffff);
            Uri value = new Uri(OutputPortValues.CONTROLLER.toString());
            output.setOutputNodeConnector(value);
            ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
            ab.setKey(new ActionKey(actionInfo.getActionKey()));
            return ab.build();
        }

    },
    set_destination_port_field {

        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer portNumber = new Integer(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer4Match(
                                    new UdpMatchBuilder().setUdpDestinationPort(
                                            new PortNumber(portNumber)).build())
                                            .build()).build()).setKey(new ActionKey(actionInfo.getActionKey())).build();

        }

    },
    set_source_port_field {

        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            Integer portNumber = new Integer(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer4Match(
                                    new UdpMatchBuilder().setUdpSourcePort(
                                            new PortNumber(portNumber)).build())
                                            .build()).build()).setKey(new ActionKey(actionInfo.getActionKey())).build();

        }

    },
    set_source_ip {

        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            InetAddress sourceIp = null;
            try{
                sourceIp = InetAddress.getByName(actionValues[0]);
            } catch (Exception e){
                e.printStackTrace();
            }
            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer3Match(
                                    new Ipv4MatchBuilder().setIpv4Source(
                                            new Ipv4Prefix(sourceIp.getHostAddress())).build()).
                                            build()).build()).setKey(new ActionKey(actionInfo.getActionKey())).build();

        }

    },
    set_destination_ip {

        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            InetAddress sourceIp = null;
            try{
                sourceIp = InetAddress.getByName(actionValues[0]);
            } catch (Exception e){
                e.printStackTrace();
            }
            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setLayer3Match(
                                    new Ipv4MatchBuilder().setIpv4Destination(
                                            new Ipv4Prefix(sourceIp.getHostAddress())).build()).
                                            build()).build()).setKey(new ActionKey(actionInfo.getActionKey())).build();

        }

    },
    set_field_eth_src {

        @Override
        public Action buildAction(ActionData actionInfo) {
            String[] actionValues = actionInfo.getActionValues();
            MacAddress mac = new MacAddress(actionValues[0]);

            return new ActionBuilder().setAction(
                    new SetFieldCaseBuilder().setSetField(
                            new SetFieldBuilder().setEthernetMatch(
                                    new EthernetMatchBuilder().setEthernetSource(
                                            new EthernetSourceBuilder().setAddress(mac).build()).build())
                                            .build()).build()).setKey(new ActionKey(actionInfo.getActionKey())).build();

        }
    },
    drop_action {

        @Override
        public Action buildAction(ActionData actionInfo) {
            DropActionBuilder dab = new DropActionBuilder();
            DropAction dropAction = dab.build();
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(new DropActionCaseBuilder().setDropAction(dropAction).build());
            return ab.build();
        }
    };

    private static final int RADIX_HEX = 16;
    public abstract Action buildAction(ActionData actionInfo);
}
