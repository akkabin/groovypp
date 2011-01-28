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

package groovypp.concurrent

@Typed class FHashMapSerialTest extends GroovyTestCase {

    void testEmpty() {
        def res = FHashMap.emptyMap.toSerialBytes().fromSerialBytes()
        assert res instanceof FHashMap
        FHashMap r = res
        assert r.size() == 0
        assert FHashMap.emptyMap === r
    }

    void testSeveralEl() {
        def res = FHashMap.emptyMap.put("1", "2").put("3","4").toSerialBytes().fromSerialBytes()
        assert res instanceof FHashMap
        FHashMap r = res
        assert r.size() == 2
        assert r["1"] == "2"
        assert r["3"] == "4"
    }
}