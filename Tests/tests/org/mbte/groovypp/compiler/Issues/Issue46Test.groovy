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





package org.mbte.groovypp.compiler.Issues

@Typed
class Issue46Test extends GroovyShellTestCase {

    void testMe()
    {
        shell.evaluate """
    @Typed package pack

import java.util.concurrent.Executor
import java.util.concurrent.Executors

    class MyThread extends Thread
    {
        MyThread ( Runnable target )
        {
            super( target );
            println "MyThread created"
        }

        String toString () { "thread: \${super.toString()}"}
    }


    def pool = Executors.newFixedThreadPool( 3, [
        newThread : { Runnable r ->
           println "new thread";
           new MyThread( r )
        } ] 
    );

    [ 1, 2, 3 ].iterator().each(pool) { it ->
        try {
            println Thread.currentThread()
        }
        catch (Throwable t) {
            t.printStackTrace ()
        }
    }.get()
        """
    }
}

