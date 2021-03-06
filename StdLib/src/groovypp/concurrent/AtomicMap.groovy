/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Typed package groovypp.concurrent

import org.codehaus.groovy.util.AbstractConcurrentMap
import org.codehaus.groovy.util.AbstractConcurrentMapBase

abstract class AtomicMap<K,V extends AtomicMapEntry> implements Iterable<V> {

    private final Map<K,V> map = new Map ()

    protected static <K> int hash(K key) {
        def h = key.hashCode ()
        h += ~(h << 9)
        h ^=  (h >>> 14)
        h +=  (h << 4)
        h ^=  (h >>> 10)
        h
    }

    V getAt(K key) {
        def h = hash(key)
        map.segmentFor(h).getOrPut(key, h, null).value
    }

    void remove (K key) {
        def h = hash(key)
        map.segmentFor(h).remove(key, h)
    }

    Iterator<V> iterator () {
        map.iterator()
    }

    abstract V createEntry(K key, int hash)

    private class Map<K,V extends AtomicMapEntry> extends AbstractConcurrentMap<K,V> implements Iterable<V> {

        Map() { super(null) }

        protected AbstractConcurrentMapBase.Segment createSegment(Object segmentInfo, int cap) {
            return new Segment(cap)
        }

        Iterator<V> iterator () {
            segments.iterator().map { s -> ((Segment)s).iterator() }.flatten ()
        }

        private class Segment<K,V extends AtomicMapEntry> extends AbstractConcurrentMap.Segment<K,V> implements Iterable<V> {
            Segment(int cap) {
                super(cap);
            }

            protected AbstractConcurrentMap.Entry<K,V> createEntry(K key, int hash, V unused) {
                createEntry(key, hash)
            }

            Iterator<V> iterator () {
                new MyIterator<K> (table)
            }
        }
    }

    private static class MyIterator<K,V extends AtomicMapEntry> implements Iterator<V> {
        final Object [] table
        int index = 0, innerIndex = 0

        MyIterator (Object [] t) {
            this.@table = t
        }

        boolean hasNext() {
            while (index < table.length) {
                def o = table[index]
                if (!o) {
                    index++
                    continue
                }

                return true
            }

            false
        }

        AtomicMapEntry<K,V> next() {
            def o = table[index]
            if (o instanceof AbstractConcurrentMap.Entry) {
                index++
                o
            }
            else {
                def arr = (Object[])o
                def res = arr [innerIndex++]
                if (innerIndex == arr.length) {
                    innerIndex = 0
                    index++
                }
                res
            }
        }

        void remove() {
            throw new UnsupportedOperationException()
        }
    }
}