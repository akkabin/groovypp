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

package groovypp.concurrent

import java.util.concurrent.atomic.AtomicReference

@Typed
abstract class FQueue<T> implements Iterable<T>, Serializable {
    abstract boolean isEmpty ()

    T getFirst () { throw new NoSuchElementException() }

    Pair<T, FQueue<T>> removeFirst() { throw new NoSuchElementException() }

    abstract FQueue<T> addLast (T element)

    abstract FQueue<T> addFirst (T element)

    abstract FQueue<T> remove(T element)

    FQueue<T> plus (T element) {
        addLast(element)
    }

    static final FQueue emptyQueue = new EmptyQueue()

    abstract int size ()

    protected final Object writeReplace() {
        new Serial(fqueue:this)
    }

    static class Serial implements Externalizable {
        FQueue fqueue

        protected final Object readResolve() {
            fqueue
        }

        void writeExternal(ObjectOutput out) {
            out.writeInt fqueue.size()
            for(e in fqueue)
                out.writeObject(e)
        }

        void readExternal(ObjectInput input) {
            def sz = input.readInt()
            def res = FQueue.emptyQueue
            while(sz--) {
                res += input.readObject()
            }
            fqueue = res
        }
    }

    private static final class EmptyQueue<T> extends FQueue<T> {
        EmptyQueue(){
        }

        OneElementQueue<T> addLast (T element)  { [element] }

        OneElementQueue<T> addFirst (T element) { [element] }

        FQueue<T> remove(T element) { this }

        Iterator<T> iterator () {
            [
                    hasNext: { false },
                    next:    { throw new UnsupportedOperationException() },
                    remove:  { throw new UnsupportedOperationException() }
            ]
        }

        String toString () {
            "[[],[]]"
        }

        final int size () { 0 }

        boolean isEmpty () { true }
    }

    private static final class OneElementQueue<T> extends FQueue<T> {
        T head

        OneElementQueue(T head){
            this.head = head
        }

        MoreThanOneElementQueue<T> addLast (T element)  { [(FList.emptyList + element) + head, FList.emptyList] }

        MoreThanOneElementQueue<T> addFirst (T element) { [(FList.emptyList + head) + element, FList.emptyList] }

        FQueue<T> remove(T element) {
            head == element ? FQueue.emptyQueue : this
        }

        T getFirst () { head }

        Pair<T, FQueue<T>> removeFirst() {
            [head, FQueue.emptyQueue]
        }

        Iterator<T> iterator () {
            head.singleton().iterator()
        }

        String toString () {
            "[$head]".toString()
        }

        final int size () { 1 }

        boolean isEmpty () { false }
    }

    private static final class MoreThanOneElementQueue<T> extends FQueue<T> {
        private final FList<T> input, output

        MoreThanOneElementQueue (FList<T> output, FList<T> input) {
            this.input  = input
            this.output = output
        }

        MoreThanOneElementQueue<T> addLast (T element) {
            [output, input + element]
        }

        MoreThanOneElementQueue<T> addFirst (T element) {
            [output + element, input]
        }

        T getFirst () { output.head }

        FQueue<T> remove(T element) {
            if (size () == 2) {
                if (output.head == element) {
                    return new OneElementQueue(output.tail.head)
                }
                else {
                    if (output.tail.head == element) {
                        return new OneElementQueue(output.head)
                    }
                    else {
                        return this
                    }
                }
            }
            else {
                if(output.size > 2) {
                    def no = output.remove(element)
                    if (no != output) {
                        return (MoreThanOneElementQueue)[no, input]
                    }
                    else {
                        def ni = input.remove(element)
                        if (ni != input) {
                            return (MoreThanOneElementQueue)[output, ni]
                        }
                        else {
                            return this
                        }
                    }
                }
                else {
                    if (output.head == element) {
                        return (MoreThanOneElementQueue)[input.reverse() + output.tail.head, FList.emptyList]
                    }
                    else {
                        if (output.tail.head == element) {
                            return (MoreThanOneElementQueue)[input.reverse() + output.head, FList.emptyList]
                        }
                        else {
                            def ni = input.remove(element)
                            if (ni != input) {
                                return (MoreThanOneElementQueue)[output, ni]
                            }
                            else {
                                return this
                            }
                        }
                    }
                }
            }
        }

        Pair<T, FQueue<T>> removeFirst() {
            if (size () == 2)
                [output.head, new OneElementQueue(output.tail.head)]
            else {
                if(output.size > 2)
                    [output.head, (MoreThanOneElementQueue<T>)[output.tail, input]]
                else {
                    [output.head, (MoreThanOneElementQueue<T>)[input.reverse() + output.tail.head, FList.emptyList]]
                }
            }
        }

        Iterator<T> iterator () {
            output.iterator() | input.reverse().iterator()
        }

        String toString () {
            "[$output,$input]"
        }

        final int size () {
            input.size + output.size
        }

        boolean isEmpty () { false }
    }

    static class Ref<T> extends AtomicReference<FQueue<T>> {
        Ref (FQueue<T> init = FQueue.emptyQueue) {
            super(init)
        }

        FQueue<T> addFirstAndGet(T el) {
            for(;;) {
                def q = get ()
                def newQueue = q.addFirst(el)
                if (compareAndSet(q, newQueue))
                    return newQueue
            }
        }

        FQueue<T> getAndAddFirst(T el) {
            for(;;) {
                def q = get ()
                def newQueue = q.addFirst(el)
                if (compareAndSet(q, newQueue))
                    return q
            }
        }

        FQueue<T> addLastAndGet(T el) {
            for(;;) {
                def q = get ()
                def newQueue = q.addLast(el)
                if (compareAndSet(q, newQueue))
                    return newQueue
            }
        }

        FQueue<T> getAndAddLast(T el) {
            for(;;) {
                def q = get ()
                def newQueue = q.addLast(el)
                if (compareAndSet(q, newQueue))
                    return q
            }
        }

        Pair<T, FQueue<T>> removeFirst() {
            for (;;) {
                def q = get()
                def res = q.removeFirst()
                if (compareAndSet(q, res.second)) {
                    return res
                }
            }
        }
    }
}
