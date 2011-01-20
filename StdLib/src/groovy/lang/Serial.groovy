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

/**
 * Annotation to help compiler generate default implementation for {@link Externalizable}
 *
 * By default, compiler generates missing implementation of readExternal, writeExternal for
 * every class implementing {@link Externalizable} (even abstract one). Annotation @Serial helps
 * to tune this generation for your needs instead of providing full implementation.
 *
 * Groovy++ rules for class to have default implementation of {@link Externalizable} are
 * - class has public no-arg constructor
 * - super class is {@link Externalizable} and have non abstract implementation of readExternal and writeExternal
 * - all non-transient fields/ properties are one of primitive types or {@link Serializable} or array of such type
 *
 * If one of rules above is not fulfilled a compile time warning will be issued
 */
public @interface Serial {
    enum Policy {
       /**
        * all non-transient fields will be serialized
        */
        FIELDS,

       /**
        * all properties will be serialized
        */
        PROPERTIES,

       /**
        * neither fields nor properties will be serialized
        */
        NONE
    }

   /**
    * Serialization policy
    */
    Policy value () default Policy.FIELDS

   /**
    * Names of fields/properties to exclude from serialization
    */
    String [] exclude ()   
}