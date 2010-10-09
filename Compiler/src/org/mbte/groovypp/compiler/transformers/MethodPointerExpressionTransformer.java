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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.MethodVisitor;

import java.util.ArrayList;
import java.util.List;

public class MethodPointerExpressionTransformer extends ExprTransformer<MethodPointerExpression> {
    public Expression transform(final MethodPointerExpression exp, final CompilerTransformer compiler) {

        final String methodName;
        if (!(exp.getMethodName() instanceof ConstantExpression) || !(((ConstantExpression) exp.getMethodName()).getValue() instanceof String)) {
            compiler.addError("Non-static method name", exp);
            return null;
        } else {
            methodName = (String) ((ConstantExpression) exp.getMethodName()).getValue();
        }

        final ClassNode type;
        final BytecodeExpr object;
        if (exp.getExpression() instanceof ClassExpression) {
            object = null;
            type = TypeUtil.wrapSafely(exp.getExpression().getType());
        } else {
            object = (BytecodeExpr) compiler.transform(exp.getExpression());
            type = object.getType();
        }

        final Object methods = ClassNodeCache.getMethods(type, methodName);
        // todo: dynamic dispatch
        if (methods == null) {
            compiler.addError("Cannot find method '" + methodName + "'", exp);
        } else if (!(methods instanceof MethodNode)) {
            compiler.addError("Multiple methods '" + methodName + "' referenced. Cannot take the pointer", exp);
        }
        final MethodNode method = (MethodNode) methods;
        final Parameter[] methodParameters = method.getParameters();
        final Parameter[] closureParameters = new Parameter[methodParameters.length];
        final GenericsType[] generics = method.getGenericsTypes();
        final ClassNode[] erasureBindings = generics == null ? null : new ClassNode[generics.length];  // All nulls.
        for (int i = 0; i < closureParameters.length; i++) {
            ClassNode t = methodParameters[i].getType();
            if (erasureBindings != null) t = TypeUtil.getSubstitutedType(t, method, erasureBindings);
            t = TypeUtil.getSubstitutedType(t, method.getDeclaringClass(), type);
            closureParameters[i] = new Parameter(t, methodParameters[i].getName());
        }

        final List<Expression> args = new ArrayList<Expression>();
        for (int i = 0; i < closureParameters.length; i++) {
            args.add(new VariableExpression(closureParameters[i].getName()));
        }

        return new BytecodeExpr(exp, ClassHelper.CLOSURE_TYPE) {
            protected void compile(MethodVisitor mv) {
                final VariableScope scope = new VariableScope(compiler.compileStack.getScope());
                final Expression receiver;
                if (object != null) {
                    object.visit(mv);
                    final String receiverName = compiler.context.getNextTempVarName();
                    final VariableExpression var = new VariableExpression(receiverName, type);
                    var.setClosureSharedVariable(true);
                    compiler.compileStack.defineVariable(var, true);
                    scope.putReferencedLocalVariable(var);
                    receiver = var;
                } else {
                   receiver = exp.getExpression();
                }
                final ExpressionStatement statement = new ExpressionStatement(
                        new MethodCallExpression(receiver, methodName, new ArgumentListExpression(args)));
                final ClosureExpression closure =
                        new ClosureExpression(closureParameters.length == 0 ? null : closureParameters, statement);
                closure.setVariableScope(scope);
                closure.setSourcePosition(exp);
                ((BytecodeExpr) compiler.transform(closure)).visit(mv);
            }
        };
    }
}
