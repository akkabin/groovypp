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

import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.handler.codec.http.HttpChunkAggregator
import org.jboss.netty.handler.codec.http.HttpRequestEncoder
import org.jboss.netty.handler.codec.http.HttpResponseDecoder
import org.jboss.netty.channel.*
import org.mbte.gretty.AbstractClient
import org.jboss.netty.handler.codec.http.HttpResponseStatus
import org.jboss.netty.handler.codec.http.DefaultHttpResponse
import org.mbte.gretty.httpserver.GrettyHttpResponse
import org.jboss.netty.handler.codec.http.HttpVersion

@Typed class AbstractHttpClient extends AbstractClient {
    AbstractHttpClient(SocketAddress remoteAddress, ChannelFactory factory = null) {
        super(remoteAddress, factory)
    }

    def ChannelPipeline getPipeline() {
        def pipeline = super.getPipeline()

        pipeline.removeFirst() // remove self

        pipeline.addLast("http.response.decoder", (HttpResponseDecoder)[
            createMessage: { initialLine ->
                def response = new GrettyHttpResponse(HttpVersion.valueOf(initialLine[0]), new HttpResponseStatus(Integer.valueOf(initialLine[1]), initialLine[2]))
                return response;
            }
        ])
        pipeline.addLast("http.response.aggregator", new HttpChunkAggregator(Integer.MAX_VALUE))
        pipeline.addLast("http.request.encoder", new HttpRequestEncoder())
        pipeline.addLast("http.application", this)

        pipeline
    }
}
