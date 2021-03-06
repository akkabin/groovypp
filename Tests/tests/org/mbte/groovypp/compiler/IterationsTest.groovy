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





package org.mbte.groovypp.compiler

public class IterationsTest extends GroovyShellTestCase {
    void testSimple () {
        def res = shell.evaluate("""
            @Typed
            u () {
                [0,1,2,3,4,5].findAll {
                   it % 2 == 1
                }
            }
            u ()
        """)
        assertEquals ([1,3,5], res)
    }

    void testFoldLeft () {
        def res = shell.evaluate("""
            @Typed
            u () {
                [0,1,2,3,4,5].foldLeft(0) { el, sum ->
                   el + sum
                }
            }
            u ()
        """)
        assertEquals (15, res)
    }

    void testFoldRight () {
        def res = shell.evaluate("""
            @Typed
            u () {
                [0,1,2,3,4,5].foldRight([]) { e, List l -> l << e; l }
            }
            u ()
        """)
        assertEquals ([5,4,3,2,1,0], res)
    }

    void testFoldRightTailRecursive () {
        shell.evaluate("""
            @Typed package p
            (0..<100000).foldRight([]) { e, List l -> l }
        """)
    }

    void testFunctions () {
        def res = shell.evaluate("""
@Trait
abstract class Function0<R> {
    abstract def call ()

    public <R1> Function0<R1> addThen (Function1<R,R1> g) {
        { -> g.call(call()) }
    }
}

@Trait
abstract class Function1<T,R> {
    abstract def call (T param)

    public <R1> Function1<T,R1> addThen (Function1<R,R1> g) {
        { arg -> g.call(call(arg)) }
    }

    public <T1> Function1<T1,R> composeWith (Function1<T1,T> g) {
        { arg -> call(g.call(arg)) }
    }

    R getAt (T arg) {
        call(arg)
    }
}

@Trait
@Typed
abstract class Function2<T1,T2,R> {
    abstract def call (T1 param1, T2 param2)

    public <R1> Function2<T1,T2,R1> addThen (Function1<R,R1> g) {
        { arg1, arg2 -> g.call(call(arg1, arg2)) }
    }

    Function1<T2,R> curry (T1 arg1) {
        { arg2 -> call(arg1, arg2) }
    }

    Function1<T2,R> getAt (T1 arg1) {
        curry arg1
    }
}

@Typed u (Function2<Integer,Integer,Integer> op) {
    op.curry(10)[5].toString ()
}

@Typed v () {
   u { Integer a, Integer b ->
      a + b
   }
}

v ()

""")
        assertEquals ("15", res)
    }
}