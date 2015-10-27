package org.opendaylight.atrium.bgprouter.api;

public interface IBgpRouter {
    
    /*
     * Start BGP Router service 
     */
    public void start();
    
    /*
     * Stop BGP Router Service
     */
    public void stop();
}
