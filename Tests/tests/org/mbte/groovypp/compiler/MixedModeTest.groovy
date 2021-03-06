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

public class MixedModeTest extends GroovyShellTestCase {

  void testMe() {
    def res = shell.evaluate("""
    import groovy.xml.*

    @Typed(TypePolicy.MIXED)
    class A {
        void m () {
            def writer = new StringWriter()
            def mb = new MarkupBuilder (writer);
            def i = 0
            mb."do" {
     //           a(i){
                    Integer j = i
                    while (!(j++ == 5)) {
                        b("b\$j")
                    }
    //            }
    //            c {
    //            }
            }
            writer.toString ()
        }
    }

    new A ().m ()
""")
  }

  void testConstructor () {
    def res = shell.evaluate ("""
      @Typed(TypePolicy.MIXED) package p

      class X {
         String val

         X (int i){ val = "int: \${i.toString()}" }
         X (String i){ val = "String: \${i.toString()}" }
      }

      Object o = 239
      new X(o).val
""")
    assert res == 'int: 239'
  }

  void testScript () {
    def res = shell.evaluate ("""
      @Typed(value=TypePolicy.MIXED) package p

      var = 239
      def clos = { var }
      this.properties = [val:clos()]
""")
    assert res == [val:239]
  }

    void testSequentially () {
        shell.evaluate """
          import java.util.concurrent.*
          import java.util.concurrent.atomic.*
          import groovy.xml.*


          @Typed(TypePolicy.MIXED)
          void u () {
              new MarkupBuilder ().numbers {
                  def divisors = { int n, Collection alreadyFound = [] ->
                      if (n > 3)
                          for(candidate in 2..<n)
                             if (n % candidate == 0)
                                return call (n / candidate, alreadyFound << candidate)

                      alreadyFound << n
                  }

                  (2..1500).iterator().mapConcurrently(Executors.newFixedThreadPool(10), 50) {
                     new Pair(it, divisors(it))
                  }.each { pair ->
                    if (pair.second.size () == 1)
                        number ([value: pair.first, prime:true ])
                    else {
                        number ([value: pair.first, prime:false]) {
                          divisor([value: pair])
                        }
                    }
                  }
              }
          }

          u ()
        """
    }

  void testConcurrently () {
      shell.evaluate """
        import java.util.concurrent.*
        import java.util.concurrent.atomic.*
        import groovy.xml.*

        static <T,R> Iterator<R> mapConcurrently (Iterator<T> self, Executor executor, int maxConcurrentTasks, Function1<T,R> op) {
          [
              pending: new AtomicInteger(),
              ready: new LinkedBlockingQueue<R>(),

              scheduleIfNeeded: {->
                while (self && ready.size() + pending.get() < maxConcurrentTasks) {
                  pending.incrementAndGet()
                  def nextElement = self.next()
                  executor.execute {-> ready << op(nextElement); pending.decrementAndGet() }
                }
              },

              next: {->
                def res = ready.take()
                scheduleIfNeeded()
                res
              },

              hasNext: {-> scheduleIfNeeded(); pending.get() > 0 || !ready.empty },

              remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
          ]
       }


        @Typed(value=TypePolicy.MIXED)
        void u () {
            new MarkupBuilder ().numbers {
                def divisors = { int n, Collection alreadyFound = [] ->
                    if (n > 3)
                        for(candidate in 2..<n)
                           if (n % candidate == 0)
                              return call (n / candidate, alreadyFound << candidate)

                    alreadyFound << n
                }

                (2..1500).iterator ().mapConcurrently (Executors.newFixedThreadPool(10), 50) {
                    [ it, divisors(it) ]
                }.each { pair ->
                    if (pair [1].size () == 1)
                        number ([value: pair[0], prime:true ])
                    else {
                        number ([value: pair[0], prime:false]) {
                          pair[1].each { div ->
                            divisor([value: div])
                          }
                        }
                    }
                }
            }
        }

        u ()
      """
  }

    void testConcurrentlyEMC () {
        shell.evaluate """
          import java.util.concurrent.*
          import java.util.concurrent.atomic.*
          import groovy.xml.*

          static <T,R> Iterator<R> mapConcurrently (Iterator<T> self, Executor executor, int maxConcurrentTasks, Function1<T,R> op) {
            [
                pending: new AtomicInteger(),
                ready: new LinkedBlockingQueue<R>(),

                scheduleIfNeeded: {->
                  while (self && ready.size() + pending.get() < maxConcurrentTasks) {
                    pending.incrementAndGet()
                    def nextElement = self.next()
                    executor.execute {-> ready << op(nextElement); pending.decrementAndGet() }
                  }
                },

                next: {->
                  def res = ready.take()
                  scheduleIfNeeded()
                  res
                },

                hasNext: {-> scheduleIfNeeded(); pending.get() > 0 || !ready.empty },

                remove: {-> throw new UnsupportedOperationException("remove () is unsupported by the iterator") },
            ]
         }


          @Typed(value=TypePolicy.MIXED)
          void u () {
              new MarkupBuilder ().numbers {
                  Integer.metaClass.getDivisors = { Collection alreadyFound = [] ->
                      int n = delegate
                      if (n > 3)
                          for(candidate in 2..<n)
                             if (n % candidate == 0)
                                return (n.intdiv(candidate)).getDivisors(alreadyFound << candidate)

                      alreadyFound << n
                  }

                  (2..1500).iterator ().mapConcurrently (Executors.newFixedThreadPool(10), 50) {
                      [ it, it.divisors ]
                  }.each { pair ->
                      if (pair [1].size () == 1)
                          number ([value: pair[0], prime:true ])
                      else {
                          number ([value: pair[0], prime:false]) {
                            pair[1].each { div ->
                              divisor([value: div])
                            }
                          }
                      }
                  }
              }
          }

          u ()
        """
    }
}