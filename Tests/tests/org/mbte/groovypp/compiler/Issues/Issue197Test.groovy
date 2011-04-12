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

public class Issue197Test extends GroovyShellTestCase {
    void testImplicitThisPassingInInstanceInitBlock() {
        try {
            shell.evaluate """
                @Typed
                class Test {
                    {
                        new A()
                        throw new RuntimeException('Inner class instance got created correctly')
                    }
                    class A {}
                }
                new Test()
            """
            fail('Should have failed to indicate success if instance init block got executed as expected')
        } catch (RuntimeException e) {
            assert e.message.contains("Inner class instance got created correctly")
        }
    }
}