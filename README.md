# Atrium
Atrium is an integrated vertical stack for SDN deployment

Steps to Build & Run : 

Build : mvn clean install 
Run: 
- Go to distribution-karaf/target/assembly/bin 
- Start Karaf by ./karaf clean 
- Install features:  
   feature:install odl-l2switch-all,  
   feature:install odl-atrium-all 
