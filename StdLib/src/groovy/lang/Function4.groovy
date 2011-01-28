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

package groovy.lang

@Trait
abstract class Function4<T1,T2,T3,T4,R> {

    abstract R call (T1 param1, T2 param2, T3 param3, T4 param4)

    public <R1> Function4<T1,T2,T3,T4,R1> andThen (Function1<R,R1> g) {
        { T1 arg1, T2 arg2, T3 arg3, T4 arg4 -> g.call(call(arg1, arg2, arg3, arg4)) }
    }

    Function3<T2,T3,T4,R> curry (T1 arg1) {
        { T2 arg2, T3 arg3, T4 arg4 -> call(arg1, arg2, arg3, arg4) }
    }

    Function3<T2,T3,T4,R> getAt (T1 arg1) {
        curry(arg1)
    }
}