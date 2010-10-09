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

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.Verifier;
import org.mbte.groovypp.compiler.AccessibilityCheck;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.PresentationUtil;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.bytecode.*;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedFieldBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedGetterBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedPropertyBytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.UnresolvedLeftExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class PropertyUtil {
    public static final Object GET_MAP = new Object ();

    public static BytecodeExpr createGetProperty(final PropertyExpression exp, final CompilerTransformer compiler, String propName, ClassNode type, final BytecodeExpr object, Object prop) {
        if (prop instanceof MethodNode) {
            MethodNode method = (MethodNode) prop;
            if ((method.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && method.getDeclaringClass() != compiler.classNode) {
                MethodNode delegate = compiler.context.getMethodDelegate(method);
                new ResolvedGetterBytecodeExpr(exp, delegate, object, compiler, propName, type);
            }
            return new ResolvedGetterBytecodeExpr(exp, method, object, compiler, propName, type);
        }

        if (prop instanceof PropertyNode) {
            return new ResolvedPropertyBytecodeExpr(exp, (PropertyNode) prop, object, null, compiler);
        }

        if (prop instanceof FieldNode) {
            FieldNode field = (FieldNode) prop;
            if ((field.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && field.getDeclaringClass() != compiler.classNode
                    && AccessibilityCheck.isAccessible(field.getModifiers(), field.getDeclaringClass(), compiler.classNode, null)) {
                MethodNode getter = compiler.context.getFieldGetter(field);
                return new ResolvedGetterBytecodeExpr.Accessor(field, exp, getter, object, compiler, type);
            }
            return new ResolvedFieldBytecodeExpr(exp, field, object, null, compiler);
        }

        if (object == null && "this".equals(propName)) {
            ClassNode curr = compiler.classNode;
            while (curr != null) {
                final FieldNode field = curr.getDeclaredField("this$0");
                if (field == null)
                    break;

                compiler.context.setOuterClassInstanceUsed(curr);
                curr = field.getType();
                if (curr.equals(exp.getObjectExpression().getType())) {
                    return new BytecodeExpr(exp, curr){
                        protected void compile(MethodVisitor mv) {
                            ClassNode cur = compiler.classNode;
                            mv.visitVarInsn(ALOAD, 0);
                            while (!cur.equals(exp.getObjectExpression().getType())) {
                                final FieldNode field = cur.getDeclaredField("this$0");
                                mv.visitFieldInsn(GETFIELD, BytecodeHelper.getClassInternalName(cur), "this$0", BytecodeHelper.getTypeDescription(field.getType()));
                                cur = field.getType();
                            }
                        }
                    };
                }
            }
            return null;
        }

        if (object != null && object.getType().isArray() && "length".equals(propName)) {
            return new BytecodeExpr(exp, ClassHelper.int_TYPE) {
                protected void compile(MethodVisitor mv) {
                    object.visit(mv);
                    mv.visitInsn(ARRAYLENGTH);
                }
            };
        }

        if (prop == GET_MAP) {
            return new org.mbte.groovypp.compiler.bytecode.ResolvedLeftMapExpr(exp, object, propName);
        }

        final Expression anchor = exp.isImplicitThis() ? exp : exp.getProperty();
        return dynamicOrFail(anchor, compiler, propName, type, object, null, "find");
    }

    public static BytecodeExpr createSetProperty(ASTNode parent, CompilerTransformer compiler, String propName, BytecodeExpr object, BytecodeExpr value, Object prop) {
        if (prop instanceof MethodNode) {
            return new ResolvedMethodBytecodeExpr.Setter(parent, (MethodNode) prop, object, new ArgumentListExpression(value), compiler);
        }

        if (prop instanceof PropertyNode) {
            final PropertyNode propertyNode = (PropertyNode) prop;
            if ((propertyNode.getModifiers() & Opcodes.ACC_FINAL) != 0) {
                final FieldNode fieldNode = compiler.findField(propertyNode.getDeclaringClass(), propName);
                return new ResolvedFieldBytecodeExpr(parent, fieldNode, object, value, compiler);
            }

            return new ResolvedPropertyBytecodeExpr(parent, propertyNode, object, value, compiler);
        }

        if (prop instanceof FieldNode) {
            final FieldNode field = (FieldNode) prop;
            if ((field.getModifiers() & Opcodes.ACC_PRIVATE) != 0 && field.getDeclaringClass() != compiler.classNode) {
                MethodNode setter = compiler.context.getFieldSetter(field);
                return new ResolvedMethodBytecodeExpr.Setter(parent, setter, object, new ArgumentListExpression(value), compiler);
            }
            return new ResolvedFieldBytecodeExpr(parent, field, object, value, compiler);
        }

        final ClassNode type = object != null ? object.getType() : compiler.classNode;
        return dynamicOrFail(parent, compiler, propName, type, object, value, "assign");
    }

    public static EmptyStatement NO_CODE = new EmptyStatement();

    public static Object resolveGetProperty(ClassNode type, String name, CompilerTransformer compiler, boolean onlyStatic, boolean isSameObject) {
        final FieldNode field = compiler.findField(type, name);
        isSameObject &= !isTraitImpl(type);
        if (field != null && field.getDeclaringClass() == compiler.classNode && isSameObject) return field;

        String getterName = "get" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY, false);
        if (mn != null && !mn.isAbstract() && (!onlyStatic || mn.isStatic())) {
            return mn;
        }

        if (mn == null) {
            getterName = "is" + Verifier.capitalize(name);
            mn = compiler.findMethod(type, getterName, ClassNode.EMPTY_ARRAY, false);
            if (mn != null && !mn.isAbstract() &&
                mn.getReturnType().equals(ClassHelper.boolean_TYPE) && (!onlyStatic || mn.isStatic())) {
                return mn;
            }
        }

        final PropertyNode pnode = compiler.findProperty(type, name);
        if (pnode != null && (!onlyStatic || pnode.isStatic())) {
            return pnode;
        }

        if (mn != null && (!onlyStatic || mn.isStatic()))
            return mn;

        if (field != null && (!onlyStatic || field.isStatic()))
            return field;

        final String setterName = "set" + Verifier.capitalize(name);
        mn = compiler.findMethod(type, setterName, new ClassNode[]{TypeUtil.NULL_TYPE}, false);
        if (mn != null && (!onlyStatic || mn.isStatic()) && mn.getReturnType() == ClassHelper.VOID_TYPE) {
            final PropertyNode res = new PropertyNode(name, mn.getModifiers(), mn.getParameters()[0].getType(), mn.getDeclaringClass(), null, NO_CODE, null);
            res.setDeclaringClass(mn.getDeclaringClass());
            return res;
        }

        if (!onlyStatic && (type.implementsInterface(ClassHelper.MAP_TYPE) || type.equals(ClassHelper.MAP_TYPE)) ) {
            return GET_MAP;
        }
        
        return null;
    }

    public static Object resolveSetProperty(ClassNode type, String name, ClassNode arg, CompilerTransformer compiler, boolean isSameObject) {
        FieldNode field = compiler.findField(type, name);
        isSameObject &= !isTraitImpl(type);
        if (field != null && field.getDeclaringClass() == compiler.classNode && isSameObject) return field;

        final String setterName = "set" + Verifier.capitalize(name);
        MethodNode mn = compiler.findMethod(type, setterName, new ClassNode[]{arg}, false);
        if (mn != null && mn.getReturnType() == ClassHelper.VOID_TYPE) {
            return mn;
        }

        final PropertyNode pnode = type.getProperty(name);
        if (pnode != null && (pnode.getModifiers() & Opcodes.ACC_FINAL) == 0) {
            return pnode;
        }

        if (field != null && (field.getModifiers() & Opcodes.ACC_FINAL) != 0) {
            if (field.getDeclaringClass() != compiler.classNode && !isFieldInitializer(compiler.methodNode))
                return null;
        }
        return field;
    }

    private static boolean isFieldInitializer(MethodNode methodNode) {
        return methodNode instanceof ConstructorNode || methodNode.isStaticConstructor();
    }

    private static boolean isTraitImpl(ClassNode type) {
        return type instanceof InnerClassNode && type.getName().endsWith("$TraitImpl");
    }

    private static BytecodeExpr dynamicOrFail(ASTNode exp, CompilerTransformer compiler, String propName, ClassNode type, BytecodeExpr object, BytecodeExpr value, String cause) {
        if (compiler.policy == TypePolicy.STATIC) {
            compiler.addError("Cannot " + cause + " property " + propName + " of class " + PresentationUtil.getText(type), exp);
            return null;
        } else
            return createDynamicCall(exp, propName, object, value);
    }

    private static BytecodeExpr createDynamicCall(ASTNode exp, final String propName, final BytecodeExpr object, final BytecodeExpr value) {
        return new UnresolvedLeftExpr(exp, value, object, propName);
    }

    public static boolean isStatic(Object prop) {
        if (prop instanceof MethodNode) return ((MethodNode) prop).isStatic();
        if (prop instanceof PropertyNode) return ((PropertyNode) prop).isStatic();
        if (prop instanceof FieldNode) return ((FieldNode) prop).isStatic();
        return false;
    }

    public static ClassNode getPropertyType(Object prop) {
        if (prop == GET_MAP) return ClassHelper.OBJECT_TYPE;
        if (prop instanceof FieldNode) return ((FieldNode) prop).getType();
        if(prop instanceof PropertyNode) return ((PropertyNode) prop).getType();
        return ((MethodNode) prop).getReturnType();
    }
}
