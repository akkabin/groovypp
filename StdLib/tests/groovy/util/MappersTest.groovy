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



package groovy.util

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import groovy.xml.MarkupBuilder

@Typed
class MappersTest extends GroovyShellTestCase {
    void testPrimitive () {
        def toTransform = /*(int[])*/[0, 1, 2]         //todo: restore array
        /*assertTrue (toTransform instanceof int[])*/
        def res = toTransform.map { it.toString () }
        assertEquals (["0", "1", "2"], res )
    }

    void testCollection () {
        def toTransform = [0, 1, 2]
        def res = toTransform.map { it.toString () }
      assertEquals (["0", "1", "2"], res )
    }

    void testArray () {
        def toTransform = (Integer[])[0, 1, 2]
        def res = toTransform.map { int v -> v.toString() }
        assertEquals (["0", "1", "2"], res )
    }

    void testUnaryNegate () {
        assertEquals (["0", "-1", "-2"], [0,1,2].map { (-it).toString() } )
    }
//
    void testIterator () {
        def iter = [0, 1, 2].iterator().map { it.toString() }
        def res = []
        while (iter.hasNext()) {
            res << iter.next ().toUpperCase()
        }
        assertEquals (["0", "1", "2"], res )
    }

  void testProduct () {
        assertEquals((List<Pair>)[[0,2], [0,3], [1,2], [1,3]], (0..1).product(2..3).asList())
    }

    void testZip () {
      def range = 0..2
      def zipped = range.iterator().zip(range.map {it + 1}.iterator())
      def list = []
      while (zipped.hasNext()) list << zipped.next()
      assertEquals((List<Pair>) [[0, 1], [1, 2], [2, 3]], list)
    }

    @Typed
    void testFlatMap () {
      def l = [[0,1,2], [3,4]]
      assertEquals(["0", "1", "2", "3", "4"], l.flatMap{it.toString()}.asList())
    }

    void testFlatten () {
      def l = [[0,1,2], [3,4]]
      assertEquals([0, 1, 2, 3, 4], l.flatten().asList())
    }

    void testMapConcurrently () {
        ExecutorService pool = Executors.newFixedThreadPool(10)
        def res = (0..100000).iterator ().mapConcurrently (pool, true, 50) {
//            println "1st pass ${Thread.currentThread().id} $it"
            it + 1
        }.toList ()
        assertEquals ((1..100001), res)

        res = (0..100000).iterator ().mapConcurrently (pool, false, 50) {
//            println "2nd pass ${Thread.currentThread().id} $it"
            it + 1
        }.toList ()
        res.sort()
        assertEquals ((1..100001), res)
    }

    @Typed(TypePolicy.MIXED)
    void testConcurrentlyMixed () {
        ExecutorService pool = Executors.newFixedThreadPool(10)
        new MarkupBuilder ().root {
            (0..10000).iterator ().mapConcurrently (pool, true, 50) {
                it + 1
            }.each {
                number([value:it])
            }
        }
    }
}