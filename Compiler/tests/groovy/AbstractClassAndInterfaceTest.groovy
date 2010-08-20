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

package groovy

import static groovy.util.test.CompileTestSupport.shouldCompile
import static groovy.util.test.CompileTestSupport.shouldNotCompile

public class AbstractClassAndInterfaceTest extends GroovyShellTestCase {
    void testInterface() {
        def res = shell.evaluate("""
      interface A {
          void methodOne(Object o)
          Object methodTwo()
      }

      @Typed
      class B implements A {
          void methodOne(Object o){}
          Object methodTwo(){
              methodOne(null)
              return new Object()
          }
      }

      def b = new B();
      return b.methodTwo()
    """)
        assertEquals(res.class, Object)
    }


    void testClassImplementingAnInterfaceButMissesMethod() {
        shouldNotCompile """
          interface A {
              void methodOne(Object o)
              Object methodTwo()
          }

          @Typed
          class B implements A {
              void methodOne(Object o){assert true}
          }

          def b = new B();
          return b.methodTwo()
          """

        shouldNotCompile """
          interface A {
              Object methodTwo()
          }
          interface B extends A{
              void methodOne(Object o)
          }

          @Typed
          class C implements A {
              void methodOne(Object o){assert true}
          }

          def b = new C();
          return b.methodTwo()
          """
    }

    void testClassImplementingNestedInterfaceShouldContainMethodsFromSuperInterfaces() {
        shouldNotCompile """
          interface A { def a() }
          interface B extends A { def b() }
          @Typed
          class BImpl implements B {
              def b(){ println 'foo' }
          }
          new BImpl().b()
          """
    }

    void testAbstractClass() {
        def shell = new GroovyShell()
        def text = """
        	abstract class A {
				abstract void methodOne(Object o)
				Object methodTwo(){
					assert true
					methodOne(null)
					return new Object()
				}
			}
            @Typed
			class B extends A {
				void methodOne(Object o){assert true}
			}

			def b = new B();
			return b.methodTwo()
			"""
        def retVal = shell.evaluate(text)
        assert retVal.class == Object
    }

    void testClassExtendingAnAbstractClassButMissesMethod() {
        shouldNotCompile """
            @Typed
        	abstract class A {
				abstract void methodOne(Object o)
				Object methodTwo(){
					assert true
					methodOne(null)
					return new Object()
				}
				abstract void MethodThree()
			}

            @Typed
			abstract class B extends A {
				void methodOne(Object o){assert true}
			}

            @Typed
			class C extends B{}

			def b = new C();
			return b.methodTwo()
			"""

        shouldNotCompile """

            @Typed
        	abstract class A {
				abstract void methodOne(Object o)
				Object methodTwo(){
					assert true
					methodOne(null)
					return new Object()
				}
				abstract void MethodThree()
			}

            @Typed
			class B extends A {
				void methodOne(Object o){assert true}
			}

			def b = new B();
			return b.methodTwo()
			"""
    }

    void testInterfaceAbstractClassCombination() {
        def shell = new GroovyShell()
        def text = """
			interface A {
				void methodOne()
			}

            @Typed
			abstract class B implements A{
				abstract void methodTwo()
			}

            @Typed
			class C extends B {
				void methodOne(){assert true}
				void methodTwo(){
				  methodOne()
				}
			}
			def c = new C()
			c.methodTwo()
			"""
        shell.evaluate(text)

        shouldNotCompile """
			interface A {
				void methodOne()
			}

            @Typed
			abstract class B implements A{
				abstract void methodTwo()
			}

            @Typed
			class C extends B {}
			def c = new c()
			c.methodTwo()
			"""
    }


    void testAccessToInterfaceField() {
        def shell = new GroovyShell()
        def text = """
			interface A {
				def foo=1
			}

            @Typed
            class B implements A {
              def foo(){foo}
            }
            assert new B().foo()==1
	   """
        shell.evaluate(text)
    }

    void testImplementsDuplicateInterface() {
        shouldCompile """
        interface I {}
        @Typed
        class C implements I {}

        10
        """
        shouldNotCompile """
        interface I {}
        @Typed
        class C implements I, I {}
        """
    }

    void testDefaultMethodParamsNotAllowedInInterface() {
        shouldNotCompile """
        interface Foo {
           def doit( String param = "Groovy", int o )
        }
        """
    }

    void testClassImplementsItselfCreatingACycle() {
//    shouldNotCompile """
//            package p1
//            @Typed
//            class XXX implements XXX {}
//        """
//
//    shouldNotCompile """
//            @Typed
//            class YYY implements YYY {}
//        """
    }
}