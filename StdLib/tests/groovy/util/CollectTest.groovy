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

@Typed
public class CollectTest extends GroovyShellTestCase {

    void testIterator () {
        def res = [1,2,3].iterator().collect {
            it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testIteratorDefType () {
        def res = shell.evaluate("""
        @Typed u () {
            def res = [1,2,3].iterator().collect {
                it + 1
            }
            res
        }
        u ()
        """)
        assertEquals ([2,3,4], res)
    }

    void testIteratorIntType () {
        def res = shell.evaluate("""
        @Typed u () {
            def res = ((List<Integer>)(0..100000)).iterator().collect { int it ->
                println it
                it + 1
            }
            res
        }
        u ()
        """)
        assertEquals ((1..100001), res)
    }

    void testArrayIntType () {
        def res = shell.evaluate("""
        @Typed <T> T u (T [] o = null) {
            def res = ((Integer[])[1,2,3,4]).collect {
                println it
                it + 1
            }
            res
        }
        u ()
        """)
        assertEquals ((2..5), res)
    }

    void testIteratorWithState () {
        def res = [1,2,3,4].iterator().collect { int it ->
            @Field int state = 0
            state += it
        }
        assertEquals ([1,3,6,10], res)
    }

    void testCollection () {
        def res = [1,2,3].collect { int it ->
            it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testCollectionWithState () {
        def res = [1,2,3,4].collect { int it ->
            assert this instanceof Function1
            @Field int state = 0
            state += it
        }
        assertEquals ([1,3,6,10], res)
    }

    void testCollectionWithStateCompile () {
        shell.evaluate """
@Typed(debug=true)
def u () {
        def res = [1,2,3,4].collect {
            assert this instanceof Function1
            @Field int state = 0
            state += it
        }
//        assert [1,3,6,10] == res
}
u()
        """
    }

    void testArray () {
        def res = [1,2,3].collect { int it ->
            it + 1
        }
        assertEquals ([2,3,4], res)
    }

    void testArrayWithState () {
        def res = ((Integer[])[1,2,3,4]).collect { int it ->
            @Field int state = 0
            state += it
        }
        assertEquals ([1,3,6,10], res)
    }


    void testMapKeyValue () {
        def res = [a:20, b:40].collect { key, value ->
            [key.toUpperCase(),value + 1]
        }
        assertEquals ([["A", 21], ["B", 41]], res)
    }
}