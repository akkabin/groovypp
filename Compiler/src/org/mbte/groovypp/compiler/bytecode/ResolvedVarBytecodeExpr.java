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

package org.mbte.groovypp.compiler.bytecode;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.*;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.RecordingVariableExpression;
import org.mbte.groovypp.compiler.Register;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedLeftExpr;
import org.mbte.groovypp.compiler.transformers.ListExpressionTransformer;
import org.mbte.groovypp.compiler.transformers.MapExpressionTransformer;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static org.codehaus.groovy.ast.ClassHelper.double_TYPE;
import static org.codehaus.groovy.ast.ClassHelper.long_TYPE;

public class ResolvedVarBytecodeExpr extends ResolvedLeftExpr {
    private final VariableExpression ve;
    private final Register var;
    private final BytecodeExpr record;

    public ResolvedVarBytecodeExpr(ClassNode type, VariableExpression ve, CompilerTransformer compiler) {
        super(ve, type);
        this.ve = ve;
        var = compiler.compileStack.getRegister(ve.getName(), true);

        if(ve instanceof RecordingVariableExpression) {
            final RecordingVariableExpression recordingVariableExpression = (RecordingVariableExpression) ve;
            final Register rvar = compiler.compileStack.getRegister(recordingVariableExpression.getRecorder().getName(), true);
            record = new BytecodeExpr(ve, getType()) {
                @Override
                protected void compile(MethodVisitor mv) {
                    dup(ResolvedVarBytecodeExpr.this.getType(), mv);
                    box(ResolvedVarBytecodeExpr.this.getType(), mv);
                    load(rvar.getType(), rvar.getIndex(), mv);
                    mv.visitInsn(Opcodes.DUP_X1);
                    mv.visitInsn(POP);
                    mv.visitLdcInsn(recordingVariableExpression.getColumn());
                    mv.visitMethodInsn(INVOKEVIRTUAL, "org/codehaus/groovy/transform/powerassert/ValueRecorder", "record", "(Ljava/lang/Object;I)Ljava/lang/Object;");
                    mv.visitInsn(POP);
                }
            };
        }
        else {
            record = null;
        }
    }

    protected void compile(MethodVisitor mv) {
        load(getType(), var.getIndex(), mv);
        if(record != null) {
            record.compile(mv);
        }
    }

    public BytecodeExpr createAssign(ASTNode parent, BytecodeExpr right, CompilerTransformer compiler) {
        final ClassNode vtype;
        if (ve.isDynamicTyped()) {
            right = compiler.transformSynthetic(right);
            
            vtype = right.getType();
            if (!compiler.getLocalVarInferenceTypes().add(ve, vtype)) {
                compiler.addError("Illegal inference inside the loop. Consider making the variable's type explicit.", ve);
            }
        } else {
            vtype = ve.getType();
            right = compiler.cast(right, vtype);
        }

        final BytecodeExpr finalRight = right;

        return new BytecodeExpr(parent, vtype) {
            protected void compile(MethodVisitor mv) {
                finalRight.visit(mv);
                if (ClassHelper.isPrimitiveType(finalRight.getType()) && !ClassHelper.isPrimitiveType(vtype))
                    box(finalRight.getType(), mv);
                store(vtype, var.getIndex(), mv);
                 load(vtype, var.getIndex(), mv);
            }
        };
    }

    public BytecodeExpr createBinopAssign(ASTNode parent, Token method, final BytecodeExpr right, CompilerTransformer compiler) {
        final BinaryExpression op = new BinaryExpression(this, method, right);
        op.setSourcePosition(parent);
        return createAssign(parent, (BytecodeExpr) compiler.transform(op), compiler);
    }

    public BytecodeExpr createPrefixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        final Register var = compiler.compileStack.getRegister(ve.getName(), false);

        if (var != null && var.getType().equals(ClassHelper.int_TYPE)) {
            return new BytecodeExpr(exp, ClassHelper.int_TYPE) {
                protected void compile(MethodVisitor mv) {
                    mv.visitIincInsn(var.getIndex(), type == Types.PLUS_PLUS ? 1 : -1);
                    mv.visitVarInsn(ILOAD, var.getIndex());
                }
            };
        }

        ClassNode vtype = compiler.getLocalVarInferenceTypes().get(ve);
        if (vtype == null)
            vtype = var.getType();

        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            return new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());
                    load(getType(), var.getIndex(), mv);
                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, type, mv);
                    if (getType() != primType)
                        box(primType, mv);
                    dup(getType(), mv);
                    store(getType(), var.getIndex(), mv);
                }
            };
        }

        if (ClassHelper.isPrimitiveType(vtype))
            vtype = TypeUtil.wrapSafely(vtype);

        String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
        final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
        if (methodNode == null) {
            compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
            return null;
        }

        final BytecodeExpr nextCall = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                new BytecodeExpr(exp, vtype) {
                    protected void compile(MethodVisitor mv) {
                        load(var.getType(), var.getIndex(), mv);
                    }
                },
                methodName,
                new ArgumentListExpression()
        ));

        return new BytecodeExpr(exp, vtype) {
            protected void compile(MethodVisitor mv) {
                nextCall.visit(mv);
                dup(getType(), mv);
                store(var.getType(), var.getIndex(), mv);
            }
        };
    }

    public BytecodeExpr createPostfixOp(ASTNode exp, final int type, CompilerTransformer compiler) {
        final Register var = compiler.compileStack.getRegister(ve.getName(), false);
        if (var != null && var.getType().equals(ClassHelper.int_TYPE)) {
            return new BytecodeExpr(exp, ClassHelper.int_TYPE) {
                protected void compile(MethodVisitor mv) {
                    mv.visitVarInsn(ILOAD, var.getIndex());
                    mv.visitIincInsn(var.getIndex(), type == Types.PLUS_PLUS ? 1 : -1);
                }
            };
        }

        ClassNode vtype = compiler.getLocalVarInferenceTypes().get(ve);
        if (vtype == null)
            vtype = var.getType();

        if (TypeUtil.isNumericalType(vtype) && !vtype.equals(TypeUtil.Number_TYPE)) {
            return new BytecodeExpr(exp, vtype) {
                protected void compile(MethodVisitor mv) {
                    final ClassNode primType = ClassHelper.getUnwrapper(getType());
                    load(getType(), var.getIndex(), mv);
                    dup(getType(), mv);
                    if (getType() != primType)
                        unbox(primType, mv);
                    incOrDecPrimitive(primType, type, mv);
                    if (getType() != primType)
                        box(primType, mv);
                    store(getType(), var.getIndex(), mv);
                }
            };
        }

        if (ClassHelper.isPrimitiveType(vtype))
            vtype = TypeUtil.wrapSafely(vtype);

        String methodName = type == Types.PLUS_PLUS ? "next" : "previous";
        final MethodNode methodNode = compiler.findMethod(vtype, methodName, ClassNode.EMPTY_ARRAY, false);
        if (methodNode == null) {
            compiler.addError("Cannot find method " + methodName + "() for type " + PresentationUtil.getText(vtype), exp);
            return null;
        }

        final BytecodeExpr nextCall = (BytecodeExpr) compiler.transform(new MethodCallExpression(
                new BytecodeExpr(exp, vtype) {
                    protected void compile(MethodVisitor mv) {
                        load(var.getType(), var.getIndex(), mv);
                        dup(getType(), mv);
                    }
                },
                methodName,
                new ArgumentListExpression()
        ));

        return new BytecodeExpr(exp, vtype) {
            protected void compile(MethodVisitor mv) {
                nextCall.visit(mv);
                store(getType(), var.getIndex(), mv);
            }
        };
    }
}
