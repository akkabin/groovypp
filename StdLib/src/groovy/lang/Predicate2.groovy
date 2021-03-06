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
abstract class Predicate2<T1,T2> {

    abstract boolean call (T1 param1, T2 param2)

    Predicate1<T2> curry (T1 arg1) {
        { T2 arg2 -> call(arg1, arg2) }
    }

    Predicate1<T2> getAt (T1 arg1) {
        curry arg1
    }
}