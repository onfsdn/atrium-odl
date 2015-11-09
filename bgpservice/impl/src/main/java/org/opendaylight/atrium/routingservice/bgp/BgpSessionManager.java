package org.opendaylight.atrium.routingservice.bgp;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static org.opendaylight.atrium.atriumutil.Tools.groupedThreads;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.opendaylight.atrium.routingservice.bgp.api.BgpService;
import org.opendaylight.atrium.routingservice.bgp.api.RouteListener;
import org.opendaylight.atrium.atriumutil.Ip4Address;
import org.opendaylight.atrium.atriumutil.Ip4Prefix;
import org.opendaylight.atrium.atriumutil.Ip6Prefix;
import org.opendaylight.atrium.atriumutil.IpPrefix;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import static com.google.common.base.Preconditions.checkNotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BgpSessionManager implements BgpService, BindingAwareProvider,
        AutoCloseable {

    boolean isShutdown = true;
    private Channel serverChannel; // Listener for incoming BGP connections
    private ServerBootstrap serverBootstrap;
    private ChannelGroup allChannels = new DefaultChannelGroup();
    private ConcurrentMap<SocketAddress, BgpSession> bgpSessions = new ConcurrentHashMap<>();
    private Ip4Address myBgpId; // Same BGP ID for all peers

    private BgpRouteSelector bgpRouteSelector = new BgpRouteSelector(this);
    private ConcurrentMap<Ip4Prefix, BgpRouteEntry> bgpRoutes4 = new ConcurrentHashMap<>();
    private ConcurrentMap<Ip6Prefix, BgpRouteEntry> bgpRoutes6 = new ConcurrentHashMap<>();

    private RouteListener routeListener;

    private static final int DEFAULT_BGP_PORT = 2000;
    private int bgpPort;

    private static final Logger log = LoggerFactory
            .getLogger(BgpSessionManager.class);

    @Override
    public void close() throws Exception {
        log.info("BgpSessionManager stopped");

    }

    @Override
    public void onSessionInitiated(ProviderContext arg0) {
        log.info("BgpSessionManager started");
    }

    @Override
    public void start(RouteListener routeListener) {
        log.info("BGP Session Manager starting.");
        isShutdown = false;
        
        readComponentConfiguration();
        
        this.routeListener = checkNotNull(routeListener);
        ChannelFactory channelFactory = new NioServerSocketChannelFactory(
                newCachedThreadPool(groupedThreads("odl/bgp", "sm-boss-%d")),
                newCachedThreadPool(groupedThreads("odl/bgp", "sm-worker-%d")));
        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                // Allocate a new session per connection
                BgpSession bgpSessionHandler = new BgpSession(
                        BgpSessionManager.this);
                BgpFrameDecoder bgpFrameDecoder = new BgpFrameDecoder(
                        bgpSessionHandler);

                // Setup the processing pipeline
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("BgpFrameDecoder", bgpFrameDecoder);
                pipeline.addLast("BgpSession", bgpSessionHandler);
                return pipeline;
            }
        };
        InetSocketAddress listenAddress = new InetSocketAddress(bgpPort);

        serverBootstrap = new ServerBootstrap(channelFactory);
        // serverBootstrap.setOptions("reuseAddr", true);
        serverBootstrap.setOption("child.keepAlive", true);
        serverBootstrap.setOption("child.tcpNoDelay", true);
        serverBootstrap.setPipelineFactory(pipelineFactory);
        try {
            serverChannel = serverBootstrap.bind(listenAddress);
            allChannels.add(serverChannel);
            log.info("BgpSessionManager started");
        } catch (ChannelException e) {
            log.info("Exception binding to BGP port {}: ",
                    listenAddress.getPort(), e);
        }
    }

    @Override
    public void stop() {
        isShutdown = true;
        allChannels.close().awaitUninterruptibly();
        serverBootstrap.releaseExternalResources();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context
     *            the component context
     */
    private void readComponentConfiguration() {

        if (bgpPort == 0) {
            bgpPort = DEFAULT_BGP_PORT;
        } 
        log.info("BGP port is set to {}", bgpPort);
    }

    /**
     * Checks whether the BGP Session Manager is shutdown.
     *
     * @return true if the BGP Session Manager is shutdown, otherwise false
     */
    boolean isShutdown() {
        return this.isShutdown;
    }

    /**
     * Gets the route listener.
     *
     * @return the route listener to use
     */
    RouteListener getRouteListener() {
        return routeListener;
    }

    /**
     * Gets the BGP sessions.
     *
     * @return the BGP sessions
     */
    public Collection<BgpSession> getBgpSessions() {
        return bgpSessions.values();
    }

    /**
     * Gets the selected IPv4 BGP routes among all BGP sessions.
     *
     * @return the selected IPv4 BGP routes among all BGP sessions
     */
    public Collection<BgpRouteEntry> getBgpRoutes4() {
        return bgpRoutes4.values();
    }

    /**
     * Gets the selected IPv6 BGP routes among all BGP sessions.
     *
     * @return the selected IPv6 BGP routes among all BGP sessions
     */
    public Collection<BgpRouteEntry> getBgpRoutes6() {
        return bgpRoutes6.values();
    }

    /**
     * Finds a BGP route for a prefix. The prefix can be either IPv4 or IPv6.
     *
     * @param prefix
     *            the prefix to use
     * @return the BGP route if found, otherwise null
     */
    BgpRouteEntry findBgpRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            return bgpRoutes4.get(prefix.getIp4Prefix()); // IPv4
        }
        return bgpRoutes6.get(prefix.getIp6Prefix()); // IPv6
    }

    /**
     * Adds a BGP route. The route can be either IPv4 or IPv6.
     *
     * @param bgpRouteEntry
     *            the BGP route entry to use
     */
    void addBgpRoute(BgpRouteEntry bgpRouteEntry) {
        if (bgpRouteEntry.isIp4()) {
            bgpRoutes4.put(bgpRouteEntry.prefix().getIp4Prefix(), // IPv4
                    bgpRouteEntry);
        } else {
            bgpRoutes6.put(bgpRouteEntry.prefix().getIp6Prefix(), // IPv6
                    bgpRouteEntry);
        }
    }

    /**
     * Removes a BGP route for a prefix. The prefix can be either IPv4 or IPv6.
     *
     * @param prefix
     *            the prefix to use
     * @return true if the route was found and removed, otherwise false
     */
    boolean removeBgpRoute(IpPrefix prefix) {
        if (prefix.isIp4()) {
            return (bgpRoutes4.remove(prefix.getIp4Prefix()) != null); // IPv4
        }
        return (bgpRoutes6.remove(prefix.getIp6Prefix()) != null); // IPv6
    }

    /**
     * Adds the channel for a BGP session.
     *
     * @param channel
     *            the channel to add
     */
    void addSessionChannel(Channel channel) {
        allChannels.add(channel);
    }

    /**
     * Removes the channel for a BGP session.
     *
     * @param channel
     *            the channel to remove
     */
    void removeSessionChannel(Channel channel) {
        allChannels.remove(channel);
    }

    /**
     * Processes the connection from a BGP peer.
     *
     * @param bgpSession
     *            the BGP session for the peer
     * @return true if the connection can be established, otherwise false
     */
    boolean peerConnected(BgpSession bgpSession) {

        // Test whether there is already a session from the same remote
        if (bgpSessions.get(bgpSession.remoteInfo().address()) != null) {
            return false; // Duplicate BGP session
        }

        bgpSessions.put(bgpSession.remoteInfo().address(), bgpSession);

        //
        // If the first connection, set my BGP ID to the local address
        // of the socket.
        //
        if (bgpSession.localInfo().address() instanceof InetSocketAddress) {
            InetAddress inetAddr = ((InetSocketAddress) bgpSession.localInfo()
                    .address()).getAddress();
            Ip4Address ip4Address = Ip4Address.valueOf(inetAddr.getAddress());

            updateMyBgpId(ip4Address);
        }
        return true;
    }

    /**
     * Processes the disconnection from a BGP peer.
     *
     * @param bgpSession
     *            the BGP session for the peer
     */
    void peerDisconnected(BgpSession bgpSession) {
        bgpSessions.remove(bgpSession.remoteInfo().address());
    }

    /**
     * Conditionally updates the local BGP ID if it wasn't set already.
     * <p/>
     * NOTE: A BGP instance should use same BGP ID across all BGP sessions.
     *
     * @param ip4Address
     *            the IPv4 address to use as BGP ID
     */
    private synchronized void updateMyBgpId(Ip4Address ip4Address) {
        if (myBgpId == null) {
            myBgpId = ip4Address;
            log.info("BGP: My BGP ID is {}", myBgpId);
        }
    }

    /**
     * Gets the local BGP Identifier as an IPv4 address.
     *
     * @return the local BGP Identifier as an IPv4 address
     */
    Ip4Address getMyBgpId() {
        return myBgpId;
    }

    /**
     * Gets the BGP Route Selector.
     *
     * @return the BGP Route Selector
     */
    BgpRouteSelector getBgpRouteSelector() {
        return bgpRouteSelector;
    }

    @Override
    public void setBgpPort(int port) {
        // TODO Auto-generated method stub
        bgpPort = port;
    }

}
