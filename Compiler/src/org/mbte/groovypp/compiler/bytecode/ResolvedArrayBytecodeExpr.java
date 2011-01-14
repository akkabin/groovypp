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

package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedLeftExpr;
import org.mbte.groovypp.compiler.transformers.ConstantExpressionTransformer;
import org.objectweb.asm.MethodVisitor;

public class ResolvedArrayBytecodeExpr extends ResolvedLeftExpr {
    private final BytecodeExpr array;
    private final BytecodeExpr index;

    private final CompilerTransformer compiler;

    public ResolvedArrayBytecodeExpr(ASTNode parent, BytecodeExpr array, BytecodeExpr index, CompilerTransformer compiler) {
        super(parent, array.getType().getComponentType());
        this.array = array;
        this.index = compiler.cast(index, ClassHelper.int_TYPE);
        this.compiler = compiler;
    }

    protected void compile(MethodVisitor mv) {
        array.visit(mv);
        index.visit(mv);
        if(compiler.fastArrays && !(index instanceof ConstantExpressionTransformer.Constant && ((ConstantExpressionTransformer.Constant)index).value instanceof Integer && ((Integer)((ConstantExpressionTransformer.Constant)index).value) < 0)) {
            int op = AALOAD;
            if (ClassHelper.isPrimitiveType(getType())) {
                if(getType().equals(ClassHelper.byte_TYPE) || getType().equals(ClassHelper.boolean_TYPE))
                    op = BALOAD;
                else {
                    if(getType().equals(ClassHelper.short_TYPE))
                        op = SALOAD;
                    else {
                        if(getType().equals(ClassHelper.int_TYPE))
                            op = IALOAD;
                        else {
                            if(getType().equals(ClassHelper.long_TYPE))
                                op = LALOAD;
                            else {
                                if(getType().equals(ClassHelper.float_TYPE))
                                    op = FALOAD;
                                else {
                                    if(getType().equals(ClassHelper.double_TYPE))
                                        op = DALOAD;
                                    else {
                                        op = CALOAD;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            mv.visitInsn(op);
        }
        else {
            if (ClassHelper.isPrimitiveType(getType()))
                mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I)" + BytecodeHelper.getTypeDescription(getType()));
            else {
                mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "([Ljava/lang/Object;I)Ljava/lang/Object;");
                checkCast(getType(), mv);
            }
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right0, final CompilerTransformer compiler) {
        final BytecodeExpr right = compiler.cast(right0, getType());
        return new BytecodeExpr(parent, getType()) {
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                right.visit(mv);
                dup_x2(getType(), mv);
                if(compiler.fastArrays && !(index instanceof ConstantExpressionTransformer.Constant && ((ConstantExpressionTransformer.Constant)index).value instanceof Integer && ((Integer)((ConstantExpressionTransformer.Constant)index).value) < 0)) {
                    int op = AASTORE;
                    if (ClassHelper.isPrimitiveType(getType())) {
                        if(getType().equals(ClassHelper.byte_TYPE) || getType().equals(ClassHelper.boolean_TYPE))
                            op = BASTORE;
                        else {
                            if(getType().equals(ClassHelper.short_TYPE))
                                op = SASTORE;
                            else {
                                if(getType().equals(ClassHelper.int_TYPE))
                                    op = IASTORE;
                                else {
                                    if(getType().equals(ClassHelper.long_TYPE))
                                        op = LASTORE;
                                    else {
                                        if(getType().equals(ClassHelper.float_TYPE))
                                            op = FASTORE;
                                        else {
                                            if(getType().equals(ClassHelper.double_TYPE))
                                                op = DASTORE;
                                            else {
                                                op = CASTORE;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    mv.visitInsn(op);
                }
                else {
                    if (ClassHelper.isPrimitiveType(getType()))
                        mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I" + BytecodeHelper.getTypeDescription(getType())+ ")V");
                    else
                        mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "([Ljava/lang/Object;ILjava/lang/Object;)V");
                }
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BytecodeExpr opLeft = new BytecodeExpr(this, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
            }
        };

        final BinaryExpression op = new BinaryExpression(opLeft, method, right);
        op.setSourcePosition(parent);
        final BytecodeExpr transformedOp = compiler.cast((BytecodeExpr) compiler.transform(op), getType());

        return new BytecodeExpr(parent, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I)" + BytecodeHelper.getTypeDescription(getType()));
                else {
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "([Ljava/lang/Object;I)Ljava/lang/Object;");
                }

                transformedOp.visit(mv);

                dup_x2(getType(), mv);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I" + BytecodeHelper.getTypeDescription(getType())+ ")V");
                else
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "([Ljava/lang/Object;ILjava/lang/Object;)V");
            }
        };
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();
        final BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, type, mv);
                    if (getType() != primType)
                        box(primType, mv);
                }
            };
        }
        else {
            if (ClassHelper.isPrimitiveType(vtype))
                vtype = TypeUtil.wrapSafely(vtype);

            String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
            if (methodNode == null) {
                compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
                return null;
            }

            incDec = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                    new BytecodeExpr(exp, vtype) {
                        protected void compile(MethodVisitor mv) {
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        return new BytecodeExpr(exp, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I)" + BytecodeHelper.getTypeDescription(getType()));
                else {
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "([Ljava/lang/Object;I)Ljava/lang/Object;");
                    cast(ClassHelper.OBJECT_TYPE, getType(), mv);
                }

                incDec.visit(mv);
                dup_x2(incDec.getType(), mv);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I" + BytecodeHelper.getTypeDescription(getType())+ ")V");
                else
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "([Ljava/lang/Object;ILjava/lang/Object;)V");
            }
        };
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        ClassNode vtype = getType();
        final BytecodeExpr incDec;
        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            incDec = new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());

                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, type, mv);
                    if (getType() != primType)
                        box(primType, mv);
                }
            };
        }
        else {
            if (ClassHelper.isPrimitiveType(vtype))
                vtype = TypeUtil.wrapSafely(vtype);

            String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
            final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
            if (methodNode == null) {
                compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
                return null;
            }

            incDec = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                    new BytecodeExpr(exp, vtype) {
                        protected void compile(MethodVisitor mv) {
                        }
                    },
                    methodName,
                    new ArgumentListExpression()
            ));
        }

        return new BytecodeExpr(exp, getType()) {
            @Override
            protected void compile(MethodVisitor mv) {
                array.visit(mv);
                index.visit(mv);
                mv.visitInsn(DUP2);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I)" + BytecodeHelper.getTypeDescription(getType()));
                else {
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "getAt", "([Ljava/lang/Object;I)Ljava/lang/Object;");
                    cast(ClassHelper.OBJECT_TYPE, getType(), mv);
                }

                dup_x2(getType(), mv);

                incDec.visit(mv);

                if (ClassHelper.isPrimitiveType(getType()))
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "("+BytecodeHelper.getTypeDescription(array.getType()) + "I" + BytecodeHelper.getTypeDescription(getType())+ ")V");
                else
                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/ArraysMethods", "putAt", "([Ljava/lang/Object;ILjava/lang/Object;)V");
            }
        };
    }
}
