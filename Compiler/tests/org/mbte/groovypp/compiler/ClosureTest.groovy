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



package org.mbte.groovypp.compiler

public class ClosureTest extends GroovyShellTestCase {

  void testAssignable() {
    def res = shell.evaluate("""
      interface I<T> {
        T calc ()
      }

      @Typed
      def u () {
        I r = {
            11
        }

        def c = (I){
            12
        }

        def u = { 13 } as I

        [r.calc (), c.calc (), u.calc()]
      }

      u ()
  """)
    assertEquals([11, 12, 13], res)
  }

  void testListAsArray() {
    def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        @Typed
        def u () {
          def x = (I[])[ {->10}, {->9} ]

          [x[0].calc(), x[1].calc () ]
        }

        u ()
    """)
    assertEquals([10, 9], res)
  }

  void testArgsCoerce() {
    def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        def v ( I a, I b, I c) {
           a.calc () + b.calc () + c.calc ()
        }

        @Typed
        def u (int add) {
            v ( {10}, {11+add}, {12} )
        }

        u (10)
    """)
    assertEquals(43, res)
  }

  void testMap() {
    def res = shell.evaluate("""
        interface I<T> {
          T calc ()
        }

        class ListOfI extends LinkedList<I> {}

        @Typed
        def u () {
          def     x = [{14}] as List<I>
          def     y = (List<I>)[{15}]
          ListOfI z = [{16}]

          [x[0].calc (), y[0].calc(), z[0].calc () ]
        }

        u ()
    """)
    assertEquals([14, 15, 16], res)
  }

  void testSeveralClosuresCall1() {
    def res = shell.evaluate("""
    @Typed class C {
      static <S,R> R apply (List<S> self, Function1<S,R> mutator, Function1<R,S> extractor) {
        mutator(self[0])
      }
      static foo() {
        def l = [1,2]
        apply l, { new Pair(it, it) }, {it.first.byteValue()}
      }
    }
    C.foo()
    """)
    assertEquals 1, res.first
  }

  void testSeveralClosuresCall2() {
    def res = shell.evaluate("""
    @Typed class C {
      static <S,R> R apply (List<S> self, Function1<R,S> extractor, Function1<S,R> mutator) {
        mutator(self[0])
      }
      static foo() {
        def l = [1,2]
        apply l, {it.first.byteValue()}, { new Pair(it, it) }
      }
    }
    C.foo()
    """)
    assertEquals 1, res.first
  }
}