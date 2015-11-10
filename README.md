# Atrium
Atrium is an integrated vertical stack for SDN deployment

Steps to Build & Run :
Build : mvn clean install -DskipTests=true

Run:
Go to distribution-karaf/target/assembly/bin
Start Karaf by ./karaf clean

Install features:
-----------------
feature:install odl-l2switch-all,
feature:install odl-didm-all,
feature:install odl-atrium-all

Setting up test environment 
---------------------------
1) Copy Atrium Release A VM from https://dl.orangedox.com/TfyGqd73qtcm3lhuaZ/Atrium_2015_A.ova 
2) Start the VM and login as admin/bgprouter 
3) Stop ONOS service using command 'onos-service localhost stop'
4) Copy the distribution zip/tar file generated after building the source code from distribution-karaf folder to the VM 
5) Unzip the distribution package and run ODL as described above. 
6) Setup the topology (in a new shell) by running 'sudo router-test.py' 


