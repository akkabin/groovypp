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

package org.mbte.groovypp.compiler.transformers;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.*;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.ClosureClassNode;
import org.mbte.groovypp.compiler.ClosureMethodNode;
import org.mbte.groovypp.compiler.CompiledClosureBytecodeExpr;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.Opcodes;

public class ClosureExpressionTransformer extends ExprTransformer<ClosureExpression> {
    public Expression transform(ClosureExpression ce, CompilerTransformer compiler) {

        if (ce.getParameters() != null && ce.getParameters().length == 0) {
            final VariableScope scope = ce.getVariableScope();
            ce = new ClosureExpression(new Parameter[1], ce.getCode());
            ce.setVariableScope(scope);
            ce.getParameters()[0] = new Parameter(ClassHelper.OBJECT_TYPE, "it", new ConstantExpression(null));
        }

        final ClosureClassNode newType = new ClosureClassNode(ce, compiler.methodNode, compiler.getNextClosureName());

        if(compiler.policy == TypePolicy.STATIC)
            CleaningVerifier.improveVerifier(newType);

        final ClosureMethodNode _doCallMethod = new ClosureMethodNode(
                "doCall",
                Opcodes.ACC_PUBLIC,
                ClassHelper.OBJECT_TYPE,
                ce.getParameters() == null ? Parameter.EMPTY_ARRAY : ce.getParameters(),
                ce.getCode());

        newType.addMethod(_doCallMethod);
        newType.setDoCallMethod(_doCallMethod);

        if (!compiler.methodNode.isStatic() || compiler.classNode.getName().endsWith("$TraitImpl"))
            newType.addField("this$0", Opcodes.ACC_PUBLIC, !compiler.methodNode.isStatic() ? compiler.classNode : compiler.methodNode.getParameters()[0].getType(), null);
        else if(compiler.methodNode.isStatic())
            newType.addField("this$0", Opcodes.ACC_PUBLIC, ClassHelper.CLASS_Type, new ClassExpression(newType.getOuterClass()));

        _doCallMethod.createDependentMethods(newType);

        newType.setModule(compiler.classNode.getModule());

        return CompiledClosureBytecodeExpr.createCompiledClosureBytecodeExpr(compiler, ce);
    }
}
