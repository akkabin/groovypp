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
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.syntax.Token;
import org.codehaus.groovy.syntax.Types;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedArrayBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedArrayLikeBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedLeftExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.mbte.groovypp.compiler.transformers.ExprTransformer;
import org.mbte.groovypp.compiler.transformers.ListExpressionTransformer;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import static org.codehaus.groovy.ast.ClassHelper.int_TYPE;

public class BinaryExpressionTransformer extends ExprTransformer<BinaryExpression> {
    private static final Token INTDIV = Token.newSymbol(Types.INTDIV, -1, -1);
    private static final Token DIVIDE = Token.newSymbol(Types.DIVIDE, -1, -1);
    private static final Token RIGHT_SHIFT_UNSIGNED = Token.newSymbol(Types.RIGHT_SHIFT_UNSIGNED, -1, -1);
    private static final Token RIGHT_SHIFT = Token.newSymbol(Types.RIGHT_SHIFT, -1, -1);
    private static final Token LEFT_SHIFT = Token.newSymbol(Types.LEFT_SHIFT, -1, -1);
    private static final Token POWER = Token.newSymbol(Types.POWER, -1, -1);
    private static final Token MOD = Token.newSymbol(Types.MOD, -1, -1);
    private static final Token MULTIPLY = Token.newSymbol(Types.MULTIPLY, -1, -1);
    private static final Token BITWISE_XOR = Token.newSymbol(Types.BITWISE_XOR, -1, -1);
    private static final Token BITWISE_OR = Token.newSymbol(Types.BITWISE_OR, -1, -1);
    private static final Token BITWISE_AND = Token.newSymbol(Types.BITWISE_AND, -1, -1);
    private static final Token MINUS = Token.newSymbol(Types.MINUS, -1, -1);
    private static final Token PLUS = Token.newSymbol(Types.PLUS, -1, -1);

    public Expression transform(BinaryExpression exp, CompilerTransformer compiler) {
        switch (exp.getOperation().getType()) {
            case Types.COMPARE_EQUAL:
            case Types.COMPARE_NOT_EQUAL:
            case Types.LOGICAL_AND:
            case Types.LOGICAL_OR:
            case Types.KEYWORD_INSTANCEOF:
            case Types.COMPARE_IDENTICAL: // ===
            case Types.COMPARE_NOT_IDENTICAL: // ===
            case Types.COMPARE_GREATER_THAN:
            case Types.COMPARE_GREATER_THAN_EQUAL:
            case Types.COMPARE_LESS_THAN:
            case Types.COMPARE_LESS_THAN_EQUAL:
                return new Logical(exp, compiler);

            case Types.EQUAL:
                return evaluateAssign(exp, compiler);

            case Types.LEFT_SQUARE_BRACKET:
                return evaluateArraySubscript(exp, compiler);

            case Types.MULTIPLY:
            case Types.DIVIDE:
            case Types.MINUS:
            case Types.PLUS:
            case Types.BITWISE_XOR:
            case Types.BITWISE_AND:
            case Types.INTDIV:
            case Types.LEFT_SHIFT:
            case Types.RIGHT_SHIFT:
            case Types.RIGHT_SHIFT_UNSIGNED:
            case Types.MOD:
            case Types.BITWISE_OR:
            case Types.POWER:
                return evaluateMathOperation(exp, compiler);

            case Types.COMPARE_TO:
                return evaluateCompareTo(exp, compiler);

            case Types.PLUS_EQUAL:
                return evaluateMathOperationAssign(exp, PLUS, compiler);

            case Types.MINUS_EQUAL:
                return evaluateMathOperationAssign(exp, MINUS, compiler);

            case Types.BITWISE_AND_EQUAL:
                return evaluateMathOperationAssign(exp, BITWISE_AND, compiler);

            case Types.BITWISE_OR_EQUAL:
                return evaluateMathOperationAssign(exp, BITWISE_OR, compiler);

            case Types.BITWISE_XOR_EQUAL:
                return evaluateMathOperationAssign(exp, BITWISE_XOR, compiler);

            case Types.MULTIPLY_EQUAL:
                return evaluateMathOperationAssign(exp, MULTIPLY, compiler);

            case Types.MOD_EQUAL:
                return evaluateMathOperationAssign(exp, MOD, compiler);

            case Types.POWER_EQUAL:
                return evaluateMathOperationAssign(exp, POWER, compiler);

            case Types.LEFT_SHIFT_EQUAL:
                return evaluateMathOperationAssign(exp, LEFT_SHIFT, compiler);

            case Types.RIGHT_SHIFT_EQUAL:
                return evaluateMathOperationAssign(exp, RIGHT_SHIFT, compiler);

            case Types.RIGHT_SHIFT_UNSIGNED_EQUAL:
                return evaluateMathOperationAssign(exp, RIGHT_SHIFT_UNSIGNED, compiler);

            case Types.DIVIDE_EQUAL:
                return evaluateMathOperationAssign(exp, DIVIDE, compiler);

            case Types.INTDIV_EQUAL:
                return evaluateMathOperationAssign(exp, INTDIV, compiler);

            case Types.FIND_REGEX:
                return evaluateFindRegexp(exp, compiler);

            case Types.MATCH_REGEX:
				return evaluateMatchRegexp(exp, compiler);

            case Types.KEYWORD_IN: {
                final BytecodeExpr left = (BytecodeExpr) compiler.transform(exp.getLeftExpression());
                final BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());
                return callMethod(exp, "isCase", compiler, right, left);
            }

            default: {
                compiler.addError("Operation: " + exp.getOperation() + " not supported", exp);
                return null;
            }
        }
    }

    private Expression evaluateCompareTo(BinaryExpression be, CompilerTransformer compiler) {
        final Operands operands = new Operands(be, compiler);
        return new BytecodeExpr(be, ClassHelper.Integer_TYPE) {
            protected void compile(MethodVisitor mv) {
                operands.getLeft().visit(mv);
                if (ClassHelper.isPrimitiveType(operands.getLeft().getType()))
                    box(operands.getLeft().getType(), mv);
                operands.getRight().visit(mv);
                if (ClassHelper.isPrimitiveType(operands.getRight().getType()))
                    box(operands.getRight().getType(), mv);
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Integer;");
            }
        };
    }

    public BytecodeExpr transformLogical(BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        final int op = exp.getOperation().getType();
        switch (op) {
            case Types.LOGICAL_AND:
                return evaluateLogicalAnd(exp, compiler, label, onTrue);

            case Types.LOGICAL_OR:
                return evaluateLogicalOr(exp, compiler, label, onTrue);

            case Types.KEYWORD_INSTANCEOF:
                return evaluateInstanceof(exp, compiler, label, onTrue);

            case Types.COMPARE_NOT_EQUAL:
            case Types.COMPARE_EQUAL:
            case Types.COMPARE_IDENTICAL: // ===
            case Types.COMPARE_NOT_IDENTICAL: // !==
            case Types.COMPARE_GREATER_THAN:
            case Types.COMPARE_GREATER_THAN_EQUAL:
            case Types.COMPARE_LESS_THAN:
            case Types.COMPARE_LESS_THAN_EQUAL:
                return evaluateCompare(exp, compiler, label, onTrue, op);

            case Types.COMPARE_TO:
                throw new UnsupportedOperationException();

            default: {
                return super.transformLogical(exp, compiler, label, onTrue);
            }
        }
    }

    private BytecodeExpr evaluateInstanceof(BinaryExpression be, CompilerTransformer compiler, final Label label, final boolean onTrue) {
        final BytecodeExpr l = (BytecodeExpr) compiler.transform(be.getLeftExpression());
        final ClassNode type = be.getRightExpression().getType();
        return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
            protected void compile(MethodVisitor mv) {
                l.visit(mv);
                box(l.getType(), mv);
                mv.visitTypeInsn(INSTANCEOF, BytecodeHelper.getClassInternalName(type));
                mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
            }
        };
    }

    private BytecodeExpr unboxReference(BinaryExpression parent, BytecodeExpr left, CompilerTransformer compiler) {
        MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(left.getType());
        if (unboxing != null) {
            left = ResolvedMethodBytecodeExpr.create(parent, unboxing, left, new ArgumentListExpression(), compiler);
        }
        return left;
    }

    private String getMethod(int token) {
        switch (token) {
            case (Types.MULTIPLY): return "multiply";
            case (Types.DIVIDE): return "div";
            case (Types.MINUS): return "minus";
            case (Types.PLUS): return "plus";
            case (Types.BITWISE_XOR): return "xor";
            case (Types.BITWISE_AND): return "and";
            case (Types.BITWISE_OR): return "or";
            case (Types.INTDIV): return "intdiv";
            case (Types.LEFT_SHIFT): return "leftShift";
            case (Types.RIGHT_SHIFT): return "rightShift";
            case (Types.RIGHT_SHIFT_UNSIGNED): return "rightShiftUnsigned";
            case (Types.MOD): return "mod";
            case (Types.POWER): return "power";
            default:
                throw new IllegalStateException("Wrong token type");
        }
    }

    private Expression evaluateMathOperation(final BinaryExpression be, final CompilerTransformer compiler) {
        final Operands operands = new Operands(be, compiler);
        final BytecodeExpr l = operands.getLeft();
        final BytecodeExpr r = operands.getRight();

        final int tokenType = be.getOperation().getType();
        if (TypeUtil.isNumericalType(l.getType()) && TypeUtil.isNumericalType(r.getType())) {
            if (tokenType == Types.POWER)
                return callMethod(be, getMethod(tokenType), compiler, l, r);

            ClassNode mathType0 = TypeUtil.getMathType(l.getType(), r.getType());

            if (tokenType == Types.DIVIDE
                    && (
                    /*mathType0.equals(ClassHelper.int_TYPE) ||
                            mathType0.equals(ClassHelper.long_TYPE) ||*/
                            mathType0.equals(ClassHelper.BigInteger_TYPE)))
                mathType0 = ClassHelper.BigDecimal_TYPE;

            final ClassNode mathType = mathType0;

            if (mathType == ClassHelper.BigDecimal_TYPE || mathType == ClassHelper.BigInteger_TYPE || mathType == TypeUtil.Number_TYPE)
                return compiler.cast(callMethod(be, getMethod(tokenType), compiler, l, r), mathType);

            if (mathType != ClassHelper.int_TYPE && mathType != ClassHelper.long_TYPE) {
                switch (tokenType) {
                    case Types.BITWISE_XOR:
                    case Types.BITWISE_AND:
                    case Types.INTDIV:
                    case Types.LEFT_SHIFT:
                    case Types.RIGHT_SHIFT:
                    case Types.RIGHT_SHIFT_UNSIGNED:
                    case Types.BITWISE_OR:
                        return compiler.cast(callMethod(be, getMethod(tokenType), compiler, l, r), mathType);
                }
            }

            return new BytecodeExpr(be, mathType) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    box(l.getType(), mv);
                    cast(TypeUtil.wrapSafely(l.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    r.visit(mv);
                    box(r.getType(), mv);
                    cast(TypeUtil.wrapSafely(r.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    compiler.mathOp(mathType, be.getOperation(), be);
                }
            };
        } else if (l.getType() == ClassHelper.STRING_TYPE && tokenType == Types.PLUS) {
            Expression stringBuilder = new ConstructorCallExpression(TypeUtil.STRING_BUILDER,
                    new ArgumentListExpression());
            stringBuilder = stringBuilderAppend(l, r, stringBuilder);
            Expression res = new MethodCallExpression(stringBuilder, "toString", new ArgumentListExpression());
            return compiler.transform(res);   // NB: some of the terms are already transformed.
        } else {
            return callMethod(be, getMethod(tokenType), compiler, l, r);
        }
    }

    private Expression stringBuilderAppend(Expression l, Expression r, Expression stringBuilder) {
        if (l instanceof BinaryExpression && ((BinaryExpression) l).getOperation().getType() == Types.PLUS) {
            BinaryExpression be = (BinaryExpression) l;
            stringBuilder = stringBuilderAppend(be.getLeftExpression(), be.getRightExpression(), stringBuilder);
        } else {
            stringBuilder = new MethodCallExpression(stringBuilder, "append", new ArgumentListExpression(l));
        }
        return new MethodCallExpression(stringBuilder, "append", new ArgumentListExpression(r));
    }

    private Expression callMethod(BinaryExpression be, String method, CompilerTransformer compiler, BytecodeExpr l, BytecodeExpr r) {
        ConstantExpression methodExpression = new ConstantExpression(method);
        methodExpression.setLineNumber(be.getOperation().getStartLine());
        methodExpression.setColumnNumber(be.getOperation().getStartColumn());
        final MethodCallExpression mce = new MethodCallExpression(l, methodExpression, new ArgumentListExpression(r));
        mce.setSourcePosition(be);
        return compiler.transform(mce);
    }

    private Expression evaluateAssign(BinaryExpression be, CompilerTransformer compiler) {
        BytecodeExpr left = (BytecodeExpr) compiler.transform(be.getLeftExpression());

        if (!(left instanceof ResolvedLeftExpr)) {
            compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
            return null;
        }

        BytecodeExpr right = (BytecodeExpr) compiler.transform(be.getRightExpression());
        MethodNode boxing = TypeUtil.getReferenceBoxingMethod(left.getType(), right.getType());
        if (boxing != null && !TypeUtil.isDirectlyAssignableFrom(left.getType(), right.getType())) {
            return ResolvedMethodBytecodeExpr.create(be, boxing, left, new ArgumentListExpression(right), compiler);
        }
        return ((ResolvedLeftExpr) left).createAssign(be, right, compiler);
    }

    private Expression evaluateMathOperationAssign(BinaryExpression be, Token op, CompilerTransformer compiler) {
        Expression left = compiler.transform(be.getLeftExpression());

        if (!(left instanceof ResolvedLeftExpr)) {
            compiler.addError("Assignment operator is applicable only to variable or property or array element", be);
            return null;
        }
        final BytecodeExpr right = (BytecodeExpr) compiler.transform(be.getRightExpression());

        MethodNode lunboxing = TypeUtil.getReferenceUnboxingMethod(left.getType());
        MethodNode rboxing = TypeUtil.getReferenceBoxingMethod(left.getType(), right.getType());
        if (lunboxing != null && rboxing != null) {
            final ResolvedMethodBytecodeExpr oldValue = ResolvedMethodBytecodeExpr.create(be, lunboxing,
                    (BytecodeExpr) left, new ArgumentListExpression(), compiler);
            final BinaryExpression binary = new BinaryExpression(oldValue, op, right);
            binary.setSourcePosition(be);
            final Expression opApplied = evaluateMathOperation(binary, compiler);
            return ResolvedMethodBytecodeExpr.create(be, rboxing,
                    (BytecodeExpr) left, new ArgumentListExpression(new Expression[]{opApplied}), compiler);
        }

        return ((ResolvedLeftExpr) left).createBinopAssign(be, op, right, compiler);
    }

    private Expression evaluateArraySubscript(final BinaryExpression bin, CompilerTransformer compiler) {
        final BytecodeExpr object = (BytecodeExpr) compiler.transformToGround(bin.getLeftExpression());

        final BytecodeExpr indexExp = (BytecodeExpr) compiler.transform(bin.getRightExpression());
        if (object.getType().isArray() && TypeUtil.isAssignableFrom(int_TYPE, indexExp.getType()))
            return new ResolvedArrayBytecodeExpr(bin, object, indexExp, compiler);
        else {
            MethodNode getter = compiler.findMethod(object.getType(), "getAt", new ClassNode[]{indexExp.getType()}, false);
            if (getter == null) {
                MethodNode unboxing = TypeUtil.getReferenceUnboxingMethod(object.getType());
                if (unboxing != null) {
                    ClassNode t = TypeUtil.getSubstitutedType(unboxing.getReturnType(), unboxing.getDeclaringClass(), object.getType());
                    getter = compiler.findMethod(t, "getAt", new ClassNode[]{indexExp.getType()}, false);
                    if (getter != null) {
                        BytecodeExpr object1 = ResolvedMethodBytecodeExpr.create(bin, unboxing, object,
                                new ArgumentListExpression(), compiler);
                        return new ResolvedArrayLikeBytecodeExpr(bin, object1, indexExp, getter, compiler);
                    }
                }
            } else {
                return new ResolvedArrayLikeBytecodeExpr(bin, object, indexExp, getter, compiler);
            }

            if (indexExp instanceof ListExpressionTransformer.UntransformedListExpr) {
                MethodCallExpression mce = new MethodCallExpression(object, "getAt", new ArgumentListExpression(((ListExpressionTransformer.UntransformedListExpr) indexExp).exp.getExpressions()));
                mce.setSourcePosition(bin);
                return compiler.transform(mce);
            }

            if (compiler.policy == TypePolicy.STATIC) {
                compiler.addError("Cannot find method " + PresentationUtil.getText(object.getType()) + ".getAt(" + PresentationUtil.getText(indexExp.getType()) + ")", bin);
                return null;
            } else {
                return new UnresolvedArrayLikeBytecodeExpr(bin, object, indexExp, compiler);
            }
        }
    }

    private BytecodeExpr evaluateLogicalOr(final BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (onTrue) {
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), label, true), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, true), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        } else {
            final Label _true = new Label();
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), _true, true), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, false), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_true);
                }
            };
        }
    }

    private BytecodeExpr evaluateLogicalAnd(final BinaryExpression exp, CompilerTransformer compiler, Label label, boolean onTrue) {
        if (onTrue) {
            final Label _false = new Label();
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), _false, false), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, true), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                    mv.visitLabel(_false);
                }
            };
        } else {
            final BytecodeExpr l = unboxReference(exp, compiler.transformLogical(exp.getLeftExpression(), label, false), compiler);
            final BytecodeExpr r = unboxReference(exp, compiler.transformLogical(exp.getRightExpression(), label, false), compiler);
            return new BytecodeExpr(exp, ClassHelper.VOID_TYPE) {
                protected void compile(MethodVisitor mv) {
                    l.visit(mv);
                    r.visit(mv);
                }
            };
        }
    }

    private void intCmp(int op, boolean onTrue, MethodVisitor mv, Label label) {
        switch (op) {
            case Types.COMPARE_NOT_EQUAL:
                mv.visitJumpInsn(onTrue ? IFNE : IFEQ, label);
                break;

            case Types.COMPARE_EQUAL:
                mv.visitJumpInsn(onTrue ? IFEQ : IFNE, label);
                break;

            case Types.COMPARE_LESS_THAN:
                mv.visitJumpInsn(onTrue ? IFLT : IFGE, label);
                break;

            case Types.COMPARE_LESS_THAN_EQUAL:
                mv.visitJumpInsn(onTrue ? IFLE : IFGT, label);
                break;

            case Types.COMPARE_GREATER_THAN:
                mv.visitJumpInsn(onTrue ? IFGT : IFLE, label);
                break;

            case Types.COMPARE_GREATER_THAN_EQUAL:
                mv.visitJumpInsn(onTrue ? IFGE : IFLT, label);
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private BytecodeExpr evaluateCompare(final BinaryExpression be, final CompilerTransformer compiler, final Label label, final boolean onTrue, final int op) {
        final Operands operands = new Operands(be, compiler);
        BytecodeExpr l = compiler.transformSynthetic(operands.getLeft());

        BytecodeExpr r = compiler.transformSynthetic(operands.getRight());

        if (TypeUtil.isNumericalType(l.getType()) && TypeUtil.isNumericalType(r.getType())) {
            final ClassNode mathType = TypeUtil.getMathType(l.getType(), r.getType());
            final BytecodeExpr l1 = l;
            final BytecodeExpr r1 = r;
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    l1.visit(mv);
                    box(l1.getType(), mv);
                    cast(TypeUtil.wrapSafely(l1.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    r1.visit(mv);
                    box(r1.getType(), mv);
                    cast(TypeUtil.wrapSafely(r1.getType()), TypeUtil.wrapSafely(mathType), mv);
                    if (ClassHelper.isPrimitiveType(mathType))
                        unbox(mathType, mv);

                    if (mathType == ClassHelper.int_TYPE) {
                        switch (op) {
                            case Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPEQ : IF_ICMPNE, label);
                                break;

                            case Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPNE : IF_ICMPEQ, label);
                                break;

                            case Types.COMPARE_LESS_THAN:
                                mv.visitJumpInsn(onTrue ? IF_ICMPLT : IF_ICMPGE, label);
                                break;

                            case Types.COMPARE_LESS_THAN_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPLE : IF_ICMPGT, label);
                                break;

                            case Types.COMPARE_GREATER_THAN:
                                mv.visitJumpInsn(onTrue ? IF_ICMPGT : IF_ICMPLE, label);
                                break;

                            case Types.COMPARE_GREATER_THAN_EQUAL:
                                mv.visitJumpInsn(onTrue ? IF_ICMPGE : IF_ICMPLT, label);
                                break;

                            default:
                                throw new IllegalStateException();
                        }
                    } else if (mathType == ClassHelper.double_TYPE) {
                        mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "compare", "(DD)I");
                        intCmp(op, onTrue, mv, label);
                    } else if (mathType == ClassHelper.long_TYPE) {
                        mv.visitInsn(LCMP);
                        intCmp(op, onTrue, mv, label);
                    } else if (mathType == ClassHelper.BigInteger_TYPE) {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigInteger", "compareTo", "(Ljava/math/BigInteger;)I");
                        intCmp(op, onTrue, mv, label);
                    } else if (mathType == ClassHelper.BigDecimal_TYPE) {
                        mv.visitMethodInsn(INVOKEVIRTUAL, "java/math/BigDecimal", "compareTo", "(Ljava/math/BigDecimal;)I");
                        intCmp(op, onTrue, mv, label);
                    } else {
                        mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "compareTo", "(Ljava/lang/Number;Ljava/lang/Number;)I");
                        intCmp(op, onTrue, mv, label);
                    }
                }
            };
        } else {
            final BytecodeExpr l2 = l;
            final BytecodeExpr r2 = r;

            final boolean leftNull  = l2.getType().equals(TypeUtil.NULL_TYPE);
            final boolean rightNull = r2.getType().equals(TypeUtil.NULL_TYPE);

            final int opType = be.getOperation().getType();
            if ((leftNull || rightNull) && (opType == Types.COMPARE_EQUAL || opType == Types.COMPARE_NOT_EQUAL || opType == Types.COMPARE_IDENTICAL || opType == Types.COMPARE_NOT_IDENTICAL)) {
                return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                    public void compile(MethodVisitor mv) {
                        if (rightNull) {
                            l2.visit(mv);
                            box(l2.getType(), mv);
                        }
                        else {
                            r2.visit(mv);
                            box(r2.getType(), mv);
                        }

                        switch (opType) {
                            case  Types.COMPARE_EQUAL:
                            case  Types.COMPARE_IDENTICAL:
                                mv.visitJumpInsn(onTrue ? IFNULL : IFNONNULL, label);
                                break;

                            case  Types.COMPARE_NOT_EQUAL:
                            case  Types.COMPARE_NOT_IDENTICAL:
                                mv.visitJumpInsn(onTrue ? IFNONNULL : IFNULL, label);
                                break;
                        }
                    }
                };
            }

            if (opType == Types.COMPARE_EQUAL || opType == Types.COMPARE_NOT_EQUAL) {
                int opType1 = opType;
                if (!onTrue) {
                    if(opType == Types.COMPARE_EQUAL)
                        opType1 = Types.COMPARE_NOT_EQUAL;
                    else
                        opType1 = Types.COMPARE_EQUAL;
                }
                return evaluateEqualNotEqual(be, compiler, label, l2, r2, opType1, false);
            }

            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    l2.visit(mv);
                    box(l2.getType(), mv);

                    r2.visit(mv);
                    box(r2.getType(), mv);

                    switch (opType) {
                        case  Types.COMPARE_IDENTICAL:
                            mv.visitJumpInsn(onTrue ? IF_ACMPEQ : IF_ACMPNE, label);
                            break;

                        case  Types.COMPARE_NOT_IDENTICAL:
                            mv.visitJumpInsn(onTrue ? IF_ACMPNE : IF_ACMPEQ, label);
                            break;

                        case Types.COMPARE_LESS_THAN:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFLT : IFGE, label);
                            break;

                        case Types.COMPARE_LESS_THAN_EQUAL:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFLE : IFGT, label);
                            break;

                        case Types.COMPARE_GREATER_THAN:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFGT : IFLE, label);
                            break;

                        case Types.COMPARE_GREATER_THAN_EQUAL:
                            mv.visitMethodInsn(INVOKESTATIC, TypeUtil.DTT_INTERNAL, "compareTo", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                            mv.visitJumpInsn(onTrue ? IFGE : IFLT, label);
                            break;

                        default:
                            throw new UnsupportedOperationException();
                    }
                }
            };
        }
    }

    /*
    The logic below is following:
    - Object.equals(Object) evaluated statically
    - Object.equals(T) evaluated as T.equals(Object)
    - equals selection takes care for extension methods
     */
    private BytecodeExpr evaluateEqualNotEqual(final BinaryExpression be, CompilerTransformer compiler, final Label label, final BytecodeExpr left, final BytecodeExpr right, final int opType, final boolean swap) {
        if (left.getType().equals(ClassHelper.OBJECT_TYPE)) {
            if (right.getType().equals(ClassHelper.OBJECT_TYPE)) {
                return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                    public void compile(MethodVisitor mv) {
                        swapIfNeeded(mv, swap, right, left);

                        mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/DefaultGroovyPPMethods", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");

                        switch (opType) {
                            case  Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(IFNE, label);
                                break;

                            case  Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(IFEQ, label);
                                break;
                        }
                    }
                };
            }
            else {
                return evaluateEqualNotEqual(be, compiler, label, right, left, opType, true);
            }
        }

        final ClassNode wrapper = ClassHelper.getWrapper(left.getType());
        if (wrapper.implementsInterface(TypeUtil.COMPARABLE) || wrapper.equals(TypeUtil.COMPARABLE)) {
            return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                public void compile(MethodVisitor mv) {
                    swapIfNeeded(mv, swap, right, left);

                    mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/DefaultGroovyPPMethods", "compareToWithEqualityCheck", "(Ljava/lang/Object;Ljava/lang/Object;)I");
                    switch (opType) {
                        case  Types.COMPARE_EQUAL:
                            mv.visitJumpInsn(IFEQ, label);
                            break;

                        case  Types.COMPARE_NOT_EQUAL:
                            mv.visitJumpInsn(IFNE, label);
                            break;
                    }
                }
            };
        }
        else {
            final BytecodeExpr ldummy = new BytecodeExpr(left, TypeUtil.wrapSafely(left.getType())) {
                protected void compile(MethodVisitor mv) {
                }
            };
            final BytecodeExpr rdummy = new BytecodeExpr(right, TypeUtil.wrapSafely(right.getType())) {
                protected void compile(MethodVisitor mv) {
                }
            };
            final MethodCallExpression safeCall = new MethodCallExpression(ldummy, "equals", new ArgumentListExpression(rdummy));
            safeCall.setSourcePosition(be);

            final ResolvedMethodBytecodeExpr call = (ResolvedMethodBytecodeExpr) compiler.transform(safeCall);
            final boolean staticEquals = call.getMethodNode().isStatic() || (call.getMethodNode() instanceof ClassNodeCache.DGM);
            final boolean defaultEquals = !staticEquals && call.getMethodNode().getParameters()[0].getType().equals(ClassHelper.OBJECT_TYPE);

            if(staticEquals || defaultEquals) {
                return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                    public void compile(MethodVisitor mv) {
                        swapIfNeeded(mv, swap, right, left);

                        if(defaultEquals)
                            mv.visitMethodInsn(INVOKESTATIC, "org/mbte/groovypp/runtime/DefaultGroovyPPMethods", "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
                        else {
                            call.visit(mv);
                        }

                        switch (opType) {
                            case  Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(IFNE, label);
                                break;

                            case  Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(IFEQ, label);
                                break;
                        }
                    }
                };
            }
            else {
                // case of multi-method
                return new BytecodeExpr(be, ClassHelper.boolean_TYPE) {
                    public void compile(MethodVisitor mv) {
                        swapIfNeeded(mv, swap, right, left);

                        Label end = new Label(), trueLabelPop2 = new Label (), falseLabelPop2 = new Label ();

                        mv.visitInsn(DUP2);

                        switch (opType) {
                            case  Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(IF_ACMPEQ, trueLabelPop2);
                                break;

                            case  Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(IF_ACMPEQ, falseLabelPop2);
                                break;
                        }

                        mv.visitInsn(DUP2);
                        mv.visitInsn(POP);

                        switch (opType) {
                            case  Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(IFNULL, falseLabelPop2);
                                break;

                            case  Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(IFNULL, trueLabelPop2);
                                break;
                        }

                        call.visit(mv);

                        switch (opType) {
                            case  Types.COMPARE_EQUAL:
                                mv.visitJumpInsn(IFNE, label);
                                break;

                            case  Types.COMPARE_NOT_EQUAL:
                                mv.visitJumpInsn(IFEQ, label);
                                break;
                        }

                        mv.visitJumpInsn(GOTO, end);

                        mv.visitLabel(trueLabelPop2);
                        mv.visitInsn(POP2);
                        mv.visitJumpInsn(GOTO, label);

                        mv.visitLabel(falseLabelPop2);
                        mv.visitInsn(POP2);

                        mv.visitLabel(end);
                    }
                };
            }
        }
    }

    private void swapIfNeeded(MethodVisitor mv, boolean swap, BytecodeExpr right, BytecodeExpr left) {
        if(swap) {
            right.visit(mv);
            BytecodeExpr.box(right.getType(), mv);

            left.visit(mv);
            BytecodeExpr.box(left.getType(), mv);

            mv.visitInsn(SWAP);
        }
        else {
            left.visit(mv);
            BytecodeExpr.box(left.getType(), mv);

            right.visit(mv);
            BytecodeExpr.box(right.getType(), mv);
        }
    }

    private static BytecodeExpr evaluateFindRegexp(final BinaryExpression exp, final CompilerTransformer compiler) {
        final BytecodeExpr left = (BytecodeExpr) compiler.transform(exp.getLeftExpression());
        final BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());

        return new BytecodeExpr(exp, TypeUtil.MATCHER) {
            protected void compile(MethodVisitor mv) {
                left.visit(mv);
                if (ClassHelper.isPrimitiveType(left.getType()))
                    box(left.getType(), mv);
                right.visit(mv);
                if (ClassHelper.isPrimitiveType(right.getType()))
                    box(right.getType(), mv);

                mv.visitMethodInsn(
                        INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper",
                        "findRegex", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/regex/Matcher;");
            }
        };
    }

    private static BytecodeExpr evaluateMatchRegexp(final BinaryExpression exp, final CompilerTransformer compiler) {
        final BytecodeExpr left = (BytecodeExpr) compiler.transform(exp.getLeftExpression());
        final BytecodeExpr right = (BytecodeExpr) compiler.transform(exp.getRightExpression());

        return new BytecodeExpr(exp, ClassHelper.boolean_TYPE) {
            protected void compile(MethodVisitor mv) {
                left.visit(mv);
                if (ClassHelper.isPrimitiveType(left.getType()))
                    box(left.getType(), mv);
                right.visit(mv);
                if (ClassHelper.isPrimitiveType(right.getType()))
                    box(right.getType(), mv);

                mv.visitMethodInsn(
                        INVOKESTATIC, "org/codehaus/groovy/runtime/InvokerHelper",
                        "matchRegex", "(Ljava/lang/Object;Ljava/lang/Object;)Z");

            }
        };
    }

    private static class Logical extends BytecodeExpr {
        private final Label _false = new Label(), _end = new Label();
        private final BytecodeExpr be;

        public Logical(Expression parent, CompilerTransformer compiler) {
            super(parent, ClassHelper.boolean_TYPE);
            be = compiler.transformLogical(parent, _false, false);
        }

        protected void compile(MethodVisitor mv) {
            be.visit(mv);
            mv.visitInsn(ICONST_1);
            mv.visitJumpInsn(GOTO, _end);
            mv.visitLabel(_false);
            mv.visitInsn(ICONST_0);
            mv.visitLabel(_end);
        }
    }

    private class Operands {
        private BytecodeExpr left;
        private BytecodeExpr right;

        public Operands(BinaryExpression be, CompilerTransformer compiler) {
            left = (BytecodeExpr) compiler.transform(be.getLeftExpression());
            right = (BytecodeExpr) compiler.transform(be.getRightExpression());
            if (!TypeUtil.areTypesDirectlyConvertible(left.getType(), right.getType())) {
                left = unboxReference(be, left, compiler);
                right = unboxReference(be, right, compiler);
            }
        }

        public BytecodeExpr getLeft() {
            return left;
        }

        public BytecodeExpr getRight() {
            return right;
        }
    }
}
