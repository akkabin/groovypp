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

public class CompareTest extends GroovyShellTestCase {

    void testAndOr() {
      def res = shell.evaluate("""
        @Typed
        def u () {
           Iterator<Integer> s = null
           s != null && s.hasNext() || (s = [1,2].iterator ()).hasNext ()
        }
        u ()
      """)

        assertTrue res
    }

  void testEqualZero() {
    def res = shell.evaluate("""
      @Typed
      def u () {
        def v = 0
        v == 0 && (v+1) != 0
      }
      u ()
    """)

      assertTrue res
  }

  void testMath() {
    shell.evaluate("""
      @Typed
      def u () {
        assert (10l == 10)
        assert (10 > 5G)
        assert !(10 < 5.0f)
        assert (10 >= 5)
        assert !(10d <= 5)
        assert (10l != 5d)
        assert (10g != 5d)
        assert (10  >  5d)
      }
      u ()
    """)
  }

  void testIntegerConstants() {
    shell.evaluate("""
      @Typed
      def u () {
          assert (10  >  5)
          assert (5  <  10)
          assert (4  >=  4)
          assert (-4  >=  -5)
          assert (4  >=  4)
          assert (4  !=  5)

          assert (4  <=  4)
          assert (-5  <=  -4)
          assert (4  <=  4)

          assert (5 == 5)
          assert (-5 == -5)


      }
      u ()
    """)
  }

  void testDoubleConstants() {
    shell.evaluate("""
      @Typed
      def u () {
          assert (10.0d  >  5.0d)
          assert (5.0d  <  10.0d)
          assert (4.0d  >=  4.0d)
          assert (-4.0d  >=  -5.0d)
          assert (4.0d  >=  4.0d)
          assert (4.0d  !=  5.0d)

          assert (4.0d  <=  4.0d)
          assert (-5.0d  <=  -4.0d)
          assert (4.0d  <=  4.0d)

          assert (5.0d == 5.0d)
          assert (-5.0d == -5.0d)

          assert (1.0d < Double.NaN)
          assert (1.0d < Double.POSITIVE_INFINITY)
          assert (1.0d > Double.NEGATIVE_INFINITY)
          assert (0.0d/0.0d == Double.NaN)
          assert (1.0d/0.0d == Double.POSITIVE_INFINITY)
          assert (-1.0d/0.0d == Double.NEGATIVE_INFINITY)
          assert (-1.0d/-0.0d == Double.POSITIVE_INFINITY)
      }
      u ()
    """)
  }

  void testMixedTypeConstants() {
    shell.evaluate("""
      @Typed
      def u () {
          assert (10L  >  5.0)
          assert (5.0  <  10L)
          assert (4L  >=  4d)
          assert (((byte)-4)  >=  -5L)
      }
      u ()
    """)
  }

  void testAutounboxing() {
    shell.evaluate("""
      @Typed
      def u () {
          assert (new Double(10)  >  new Double(5))
          assert (new Double(5)  <  new Integer(10))
          assert (new Double(10)  ==  new Double(10))
          assert (new Integer(10)  ==  new Double(10))
      }
      u ()
    """)
  }

  void testNegativeZeros() {
    shell.evaluate("""
      @Typed
      def u () {
        assert (0.0f != -(0.0f))
        assert 0.0f == -0.0

        assert 0.0d != -(0.0d)
        assert +0.0d == +0.0d
      }
      u ()
    """)
  }

  void testClassTypes() {
    shell.evaluate("""
      @Typed
      def u () {
        assert (Float.TYPE == float)
        assert (Double.TYPE == double)

        assert (1.2d.class == double)
      }
      u ()
    """)
  }
}