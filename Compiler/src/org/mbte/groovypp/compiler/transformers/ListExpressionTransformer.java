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

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.InnerClassNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.ListClassNode;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.objectweb.asm.MethodVisitor;

import java.util.List;

public class ListExpressionTransformer extends ExprTransformer<ListExpression> {
    public Expression transform(final ListExpression exp, final CompilerTransformer compiler) {
        return new UntransformedListExpr(exp, compiler);
    }

    public static class UntransformedListExpr extends BytecodeExpr {
        public final ListExpression exp;

        public UntransformedListExpr(ListExpression exp, CompilerTransformer compiler) {
            super(exp, ClassHelper.OBJECT_TYPE);
            setType(new ListClassNode(this, compiler.methodNode, compiler.getNextClosureName()));
            this.exp = exp;
        }

        protected void compile(MethodVisitor mv) {
            throw new UnsupportedOperationException();
        }

        public BytecodeExpr transform (ClassNode collectionType, CompilerTransformer compiler) {
            return new TransformedListExpr(exp, collectionType, compiler, true);
        }
    }

    public static class TransformedListExpr extends BytecodeExpr {
        public final ListExpression exp;

        public TransformedListExpr(ListExpression exp, ClassNode collType, CompilerTransformer compiler, boolean needInference) {
            super(exp, collType);
            this.exp = exp;

            final List<Expression> list = exp.getExpressions();
            ClassNode genericArg = null;
            for (int i = 0; i != list.size(); ++i) {
                Expression transformed = compiler.transformToGround(list.get(i));
                list.set(i, transformed);

                genericArg = genericArg == null ? transformed.getType() :
                        TypeUtil.commonType(genericArg, transformed.getType());
            }

            if (needInference) {
                if (genericArg != null) {
                    if (genericArg == TypeUtil.NULL_TYPE) genericArg = ClassHelper.OBJECT_TYPE;
                    genericArg = TypeUtil.wrapSafely(genericArg);
                    setType(TypeUtil.withGenericTypes(collType, genericArg));
                } else {
                    setType(TypeUtil.eraseTypeArguments(collType));
                }
            }
        }

        protected void compile(MethodVisitor mv) {
            final List list = exp.getExpressions();
            String classInternalName = BytecodeHelper.getClassInternalName(getType());
            mv.visitTypeInsn(NEW, classInternalName);
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL,classInternalName,"<init>","()V");
            for (int i = 0; i != list.size(); ++i) {
                final BytecodeExpr be = (BytecodeExpr) list.get(i);
                mv.visitInsn(DUP);
                be.visit(mv);
                box(be.getType(), mv);
                mv.visitMethodInsn(INVOKEINTERFACE,"java/util/Collection","add","(Ljava/lang/Object;)Z");
                mv.visitInsn(POP);
            }
        }
    }
}
