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

public class TypeInferenceTest extends GroovyShellTestCase {

  void testAssert() {
    def res = shell.evaluate("""
@Typed
class A extends GroovyTestCase {
    def m () {
        def list = [] as List<Number>
        list.leftShift 1
        list << 2
        assertEquals ([1,2], list)

        if (list.size() == 2) {
            list = list [0]
            list++
            assertTrue (list instanceof Integer)
        }
        else {
            list = 239G
            assertTrue list instanceof BigDecimal
        }
        list instanceof Number
    }
}

new A().m ()
        """)
    assertTrue res == Boolean.TRUE
  }

    void testAssertBug() {
      def res = shell.evaluate("""
  @Typed
  class A extends GroovyTestCase {
      def m () {
          def list = [12]

          if (true) {
              list = list [0]
              assertTrue (list instanceof Integer)
          }
          else {
              list = 239G
              assertTrue list instanceof BigDecimal
          }
          list instanceof Number
      }
  }

  new A().m ()
          """)
      assertTrue res == Boolean.TRUE
    }

    void testList() {
      def res = shell.evaluate("""
      @Typed
      def m () {
          def list = [2] as List<Number>
          def u = list.get(0), w = list.getAt(0), v = list[0]
          [u++, w++, v++]
      }
      m ()
          """)
      assertEquals ([2,2,2], res)
    }

    void testArrayInference() {
      def res = shell.evaluate("""
      @Typed class Foo {
        def <T> T getFirst(T[] ts) { ts[0] }
        def bar() {
           int[] arr = new int[1]
           arr[0] = 0
           getFirst(arr) * 100
        }
      }
      new Foo().bar()
          """)
      assertEquals (0, res)
    }

    void testArrayInference1() {
      def res = shell.evaluate("""
      @Typed 
      class Foo {
        def <T> T[] getArray(T[] ts) { ts }
        def bar() {
           int[] arr = new int[1]
           arr[0] = 5
           arr = getArray(arr)
           arr[0]
        }
      }
      new Foo().bar()
          """)
      assertEquals (5, res)
    }

    void testVarargsInference() {
      def res = shell.evaluate("""
        @Typed
        def bar() {
          Arrays.asList("schwiitzi", "nati").get(1).charAt(0)
        }
        bar()
        """)
      assertEquals ('n', res)
    }


  void testCast() {
    def res = shell.evaluate("""
  @Typed
  def m (val) {
    (List)val
    ((List)val).size ()
  }

  m ([1,2,3])
      """)
    assertEquals 3, res
  }

  void testInference() {
    def res = shell.evaluate("""
@Typed
def m () {
   Collection<Object> x = [1, 2, 3]
   x.leftShift(4)
   x = x + 5

   def y
   if (x.size() == 5)
     y = x.size() + 1.0
   else
     y = 5G

   x.leftShift(y.doubleValue ())

   if (!x)
     y = [1] as Set
   else {
     y = [2]
   }

   x.leftShift(y.size ())

   def u = 0
   while (!(u == 10)) {
     u++
   }
   x.add(u)
   x.add "\$u \${((List)x).size()}"
   x
}

m ()
    """)
    assertEquals([1, 2, 3, 4, 5, 6.0d, 1, 10, "10 8"], res)
  }

  void testListWithGen() {
    def res = shell.evaluate ("""
        @Typed
         <T> List<List<T>> u (List<List<T>> list, T toAdd) {
          for (int i = 0; i != list.size (); ++i)
            list [i] << toAdd
          list
        }
        u ([[0], [1], [2]], -1)
    """)
    assertEquals ([[0, -1], [1,-1], [2,-1]], res)
  }

    void testGroupBy() {
      def res = shell.evaluate ("""
        interface Transform<K,V> {
           V transform (K key)
        }

        @Typed
        static <K,T> Map<K, List<T>> groupBy(Collection<T> self, Transform<T,K> transform) {
            def answer = (Map<K, List<T>>)[:]
            for (T element : self) {
                def value = transform.transform(element)
                def list = answer[value]
                if (list == null) {
                    list = new LinkedList<T> ()
                    answer[value] = list
                }
                list << element
            }
            answer
        }

        @Typed
        def test () {
            groupBy(["1", 3, "2", "4", 0]){
              it instanceof String
            }
        }

        test ()
      """)
      assertEquals (res[true], ["1", "2", "4"])
      assertEquals (res[false],[3, 0])
    }


    void testMapSyntax() {
      def res = shell.evaluate ("""
        @Typed class U {
            int i
            float f

            String toString () { "[i: \$i, f: \$f]" }

            boolean testSameI(U other) {
               i == other.i
            }
        }

        @Typed
        def test () {
            List<U> l = []
            def map = [:]
            U a = [i:12]
            a.testSameI ([f:3])
            def b = (U) [:]
            l << [i:0, f:12]
            l << [i:0] << a << b
            l.addAll ( [[i:1, f:12], [i:2, f:13]] )
            l
        }

        test ()
      """)
        println res
    }

    void testListSyntax() {
      def res = shell.evaluate ("""
        @Typed class U {
            int i
            float f

            U (int i, float f) {
               this.i = i
               this.f = f
            }

            String toString () { "[i: \$i, f: \$f]" }

            boolean testSameI(U other) {
               i == other.i
            }
        }

        @Typed
        def test () {
            List<U> l = []
            def map = [:]
            U a = [12, 10]
            a.testSameI ([3,10])
            def b = (U) [11,4]
            l << [0, 12]
            l << [0,11] << a << b
//            l.addAll ( [[1, 12], [2, 13]] )
            l
        }

        test ()
      """)
        println res
    }

    void testListMapping () {
        shell.evaluate """
          @Typed
  static <T, R> Iterator<R> flatMap(Iterator<Iterator<T>> self, Function1<T, R> op) {
    [
            curr: (Iterator<T>) null,
            hasNext: {
              (curr != null && curr.hasNext()) || (self.hasNext() && (curr = self.next()).hasNext())
            },
            next: { op.call(curr.next()) },
            remove: { throw new UnsupportedOperationException("remove() is not supported") }
    ]
  }

          @Typed
  static <T, R> Iterator<R> flatMap(Iterable<Iterable<T>> self, Function1<T, R> op) {
    flatMap(self.iterator().map {it.iterator()}, op)
  }
          @Typed
          def u () {
              def l = [[0,1,2], [3,4]]
              assert ["0", "1", "2", "3", "4"] == l.flatMap{it.toString()}.asList()
          }
          u ()
        """
    }

  void testPrimitiveVar () {
     shell.evaluate """
      @Typed package p

      def a = 0, b = false, c = 0.0d, d = 10L, e = (byte)12, g = '1234'
      assert a.class == int.class
      assert b.class == boolean.class
      assert c.class == double.class
      assert e.class == byte.class
      assert g.class == String
     """
  }

  void testLongStringVar () {
     shell.evaluate """
      @Typed package p

     def o = null
     if(o){
        o = 'lala'
     }
     else {
        o = new Long(10L)
     }
     println o
     """
  }

    void testClosureArrayType () {
        shell.evaluate """
@Typed package p

abstract class Module {
    abstract doSomething ()
}

def m(Module [] arr){
    for(a in arr)
        a.doSomething ()
}

m {
    println "lalala"
}

m {
    println "lalala"
}{
    println "ohohoho"
}
        """
    }
}
