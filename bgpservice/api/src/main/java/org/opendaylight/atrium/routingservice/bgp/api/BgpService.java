package org.opendaylight.atrium.routingservice.bgp.api;

/**
 * Provides a way of interacting with the BGP protocol component.
 */
public interface BgpService {

    /**
     * Starts the BGP service.
     *
     * @param routeListener listener to send route updates to
     */
    void start(RouteListener routeListener);

    /**
     * Stops the BGP service.
     */
    void stop();
    
    /**
     * Set the port number for BGP sessions 
     * 
     * @param port
     */
    void setBgpPort(int port);
}
