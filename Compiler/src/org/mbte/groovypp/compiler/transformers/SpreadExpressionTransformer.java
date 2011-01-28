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

package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.SpreadExpression;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;

public class SpreadExpressionTransformer extends ExprTransformer<SpreadExpression> {
    public Expression transform(SpreadExpression exp, CompilerTransformer compiler) {
        compiler.addError("Spread operator is not supported", exp);
        return null;
    }
}