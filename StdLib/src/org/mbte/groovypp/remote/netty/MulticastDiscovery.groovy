package org.mbte.groovypp.remote.netty;

import java.net.InetAddress
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import groovy.remote.ClusterNode
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.handler.codec.serialization.ObjectEncoder
import org.jboss.netty.handler.codec.serialization.ObjectDecoder
import java.util.concurrent.Executors
import groovy.util.concurrent.SupervisedChannel

@Typed class MulticastDiscovery extends BroadcastThread.Receiver {
    NioClientSocketChannelFactory clientFactory

    ClusterNode clusterNode

    MulticastDiscovery () {
        messageTransform = { byte [] buf ->
            def msg = InetDiscoveryInfo.fromBytes(buf)
            if (msg) {
                if (msg.clusterId > clusterNode.id) {
                    synchronized(clients) {
                        if(!clients.containsKey(msg.clusterId)) {
                            clusterNode.communicationEvents << new ClusterNode.CommunicationEvent.TryingConnect(uuid:msg.clusterId, address:msg.serverAddress)
                            def client = new NettyClient()
                            client.address = msg.serverAddress
                            client.remoteId = msg.clusterId
                            clients.put(msg.clusterId, client)
                            startupChild(client)
                        }
                    }
                }
            }
        }
    }

    public void doStartup() {
        super.doStartup()

        if (!clusterNode) {
            if(owner instanceof ClusterNode)
                clusterNode = (ClusterNode)owner
            else
                throw new IllegalStateException("MulticastDiscovery requires clusterNode")
        }
        multicastGroup = clusterNode.multicastGroup
        multicastPort  = clusterNode.multicastPort

        clientFactory = [Executors.newCachedThreadPool(),Executors.newCachedThreadPool()]
    }

    public void doShutdown() {
        clientFactory.releaseExternalResources()
    }

    private HashMap<UUID,NettyClient> clients = [:]

    class NettyClient extends SupervisedChannel {
        NettyConnection connection
        SocketAddress address
        UUID remoteId

        void doStartup() {
            ClientBootstrap bootstrap = [clientFactory]
            SimpleChannelHandlerEx handler = [
                createConnection: { ctx ->
                    new NettyClientConnection(channel: ctx.channel, clusterNode:clusterNode)
                }
            ]

            bootstrap.pipeline.addLast("object.encoder", new ObjectEncoder())
            bootstrap.pipeline.addLast("object.decoder", new ObjectDecoder())
            bootstrap.pipeline.addLast("handler", handler);

            bootstrap.setOption("tcpNoDelay", true);
            bootstrap.setOption("keepAlive", true);

            bootstrap.connect(address)
        }

        void doShutdown () {
            connection?.channel?.close()
        }

        void onDisconnect() {
            synchronized(clients) {
                clients.remove(remoteId)
            }
            connection = null
            shutdown()
        }

        private static class NettyClientConnection extends NettyConnection {
            NettyClient nettyClient

            public void onConnect() {
                super.onConnect()
                if (nettyClient)
                    nettyClient.connection = this
            }

            public void onDisconnect() {
                nettyClient?.onDisconnect()
                nettyClient = null
                super.onDisconnect();
            }
        }
    }
}