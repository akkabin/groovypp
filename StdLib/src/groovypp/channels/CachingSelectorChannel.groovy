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

package groovypp.channels

import java.util.concurrent.ConcurrentHashMap

/**
 * 
 */
abstract class CachingSelectorChannel<M,K> extends SelectorChannel<M> {

    private ConcurrentHashMap<K, MessageChannel<M>> cache = []

    Iterator<MessageChannel<M>> selectInterested(M message) {
        def key = messageKey(message)
        def c = cache.get(key)
        if (!c) {
          synchronized(cache) {

          }
        }
        c.singletonList().iterator()
    }

    /**
    * Maps message to key in the cache
    */
    abstract protected K messageKey(M message)
}
