/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.gretty.httpclient

import org.jboss.netty.handler.codec.http.HttpResponse

import org.jboss.netty.channel.ChannelHandlerContext

import org.jboss.netty.channel.MessageEvent

import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrame
import org.mbte.gretty.httpserver.GrettyHttpRequest
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.*
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameEncoder
import org.jboss.netty.handler.codec.http.websocket.WebSocketFrameDecoder
import org.jboss.netty.channel.ChannelFuture
import org.jboss.netty.channel.ChannelFactory

@Typed class GrettyWebsocketClient extends AbstractHttpClient {
    private final String path

    GrettyWebsocketClient(SocketAddress remoteAddress, String path, ChannelFactory factory = null) {
        super(remoteAddress, factory)
        this.path = path
    }

    void onConnect() {
        GrettyHttpRequest req = [path]
        req.addHeader(UPGRADE, WEBSOCKET)
        req.addHeader(CONNECTION, UPGRADE)
        req.addHeader(HOST, UPGRADE)
        req.addHeader(ORIGIN, remoteAddress.toString())
        channel.write(req)
    }

    void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        def msg = e.message
        switch(msg) {
            case HttpResponse:
                    def pipeline = channel.pipeline
                    pipeline.remove("http.response.decoder")
                    pipeline.remove("http.request.encoder")
                    pipeline.addBefore("http.application", "websocket.decoder", new WebSocketFrameDecoder())
                    pipeline.addBefore("http.application", "websocket.encoder", new WebSocketFrameEncoder())
                    onWebSocketConnect ()
                break

            default:
                    onMessage(((WebSocketFrame)msg).textData)
                break
        }
    }

    protected void onWebSocketConnect() {}

    protected void onMessage(String text) {}

    void send(String textData) {
        channel?.write(new DefaultWebSocketFrame(textData))
    }

    void disconnect() {
        channel?.close()
    }
}
