# Atrium
Atrium is an integrated vertical stack for SDN deployment

Steps to Build & Run :

Please ensure that you follow instructions in following link
https://wiki.opendaylight.org/view/GettingStarted:Development_Environment_Setup

Build : mvn clean install -DskipTests=true

Run:
Go to distribution-karaf/target/assembly/bin
Start Karaf by ./karaf clean


Install features:
-----------------
Please follow the correct order as given below

feature:install odl-atrium-all

If dlux UI + RESTCONF web interface is required, please add following features as well in the order given 

feature:install odl-restconf odl-mdsal-apidocs odl-dlux-core

Setting up test environment 
---------------------------
1) Copy Atrium Release A VM from https://dl.orangedox.com/TfyGqd73qtcm3lhuaZ/Atrium_2015_A.ova 

2) Start the VM and login as admin/bgprouter 

3) Stop ONOS service using command 'onos-service localhost stop'

4) Copy the distribution zip/tar file generated after building the source code from distribution-karaf folder to the VM 

5) Unzip the distribution package and run ODL as described above. 

6) Start Karaf : Go to <odl distribution folder>/bin , and run ./karaf clean

7) Install features as suggested above 

8) Verify the configurations (sdnip.json and addresses.json) in <distribution folder>/configuration/initial

9) Setup the topology (in a new shell) by running 'sudo router-test.py' 

Note: Latest atrium-odl is integrated with bgppcep application in ODL for receiving RIB updates. This configuration is currently placed in atrium-odl/utils/config/src/main/resources/atrium-bgp-config.xml. Here the details of the Quagga BGP Speaker needs to be updated. The existing config file is fine tuned for the test setup mentioned below and will work without any manual instrumentation if the test setup given below is used. This file will be loaded by default to ODL runtime distribution folder etc/opendaylight/karaf directory. 
