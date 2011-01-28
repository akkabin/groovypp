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

package groovy.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Methods annotated by @Async will lead to creating another one, which can be called asynchroniously
 * New method will always return void and have same parameters plus one additional parameter of type 
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
@GroovyASTTransformationClass("org.mbte.groovypp.compiler.AsyncASTTransform")
public @interface Async {
    public abstract String  messageClassName  () default "";
}