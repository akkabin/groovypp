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

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.classgen.ClassGeneratorException;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.objectweb.asm.MethodVisitor;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ConstantExpressionTransformer extends ExprTransformer<ConstantExpression> {
    public BytecodeExpr transform(final ConstantExpression exp, CompilerTransformer compiler) {
        return new Constant(exp);
    }

    protected static ClassNode getConstantType(Object value) {
        if (value == null) {
            return TypeUtil.NULL_TYPE;
        } else if (value instanceof String) {
            return ClassHelper.STRING_TYPE;
        } else if (value instanceof Character) {
            return ClassHelper.char_TYPE;
        } else if (value instanceof Number) {
            Number n = (Number) value;
            if (n instanceof Integer) {
                return ClassHelper.int_TYPE;
            } else if (n instanceof Double) {
                return ClassHelper.double_TYPE;
            } else if (n instanceof Float) {
                return ClassHelper.float_TYPE;
            } else if (n instanceof Long) {
                return ClassHelper.long_TYPE;
            } else if (n instanceof BigDecimal) {
                return ClassHelper.BigDecimal_TYPE;
            } else if (n instanceof BigInteger) {
                return ClassHelper.BigInteger_TYPE;
            } else if (n instanceof Short) {
                return ClassHelper.short_TYPE;
            } else if (n instanceof Byte) {
                return ClassHelper.byte_TYPE;
            } else {
                throw new ClassGeneratorException(
                        "Cannot generate bytecode for constant: " + value
                                + " of type: " + value.getClass().getName()
                                + ".  Numeric constant type not supported.");
            }
        } else if (value instanceof Boolean) {
            return ClassHelper.boolean_TYPE;
        } else if (value instanceof Class) {
            Class vc = (Class) value;
            if (vc.getName().equals("java.lang.Void")) {
                // load nothing here for void
            } else {
                throw new ClassGeneratorException(
                        "Cannot generate bytecode for constant: " + value + " of type: " + value.getClass().getName());
            }
        }

        throw new ClassGeneratorException(
                    "Cannot generate bytecode for constant: " + value + " of type: " + value.getClass().getName());
    }

    public static final class Constant extends BytecodeExpr {
        public final Object value;

        public Constant(ConstantExpression exp) {
            this(exp, getConstantType(exp.getValue()), exp.getValue());
        }

        public Constant(ConstantExpression exp, ClassNode type, Object value) {
            super(exp, type);
            this.value = value;
        }

        public void compile(MethodVisitor mv) {
            if (value == null) {
                mv.visitInsn(ACONST_NULL);
            }
            else {
                mv.visitLdcInsn(value);
            }
        }
    }
}