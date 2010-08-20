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

package org.mbte.groovypp.compiler.Issues

public class Issue45Test extends GroovyShellTestCase {
    void testInner () {
        shell.evaluate """
import groovypp.concurrent.*
import org.codehaus.groovy.util.AbstractConcurrentMap
import org.codehaus.groovy.util.AbstractConcurrentMapBase

@Typed class A {
    def m = new Map ()

    private class Map<K,V> extends AbstractConcurrentMap<K,V> implements Iterable<AtomicMapEntry<K,V>> {
        Map() { super(null) }

        protected AbstractConcurrentMapBase.Segment createSegment(Object segmentInfo, int cap) {}

        Iterator iterator () {}
    }
}

new A ()
"""
    }

    void testMe () {
        shell.evaluate """
            @Typed package p

            import groovypp.concurrent.*

            def m = new AtomicIntegerMap<String> ()
            m ['10'].incrementAndGet ()
        """
    }
}