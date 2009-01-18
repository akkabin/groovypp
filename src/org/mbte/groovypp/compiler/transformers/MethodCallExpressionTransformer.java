package org.mbte.groovypp.compiler.transformers;

import groovy.lang.CompilePolicy;
import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.TypeUtil;
import org.mbte.groovypp.compiler.ClosureMethodNode;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;
import org.mbte.groovypp.compiler.bytecode.ResolvedMethodBytecodeExpr;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.List;

public class MethodCallExpressionTransformer extends ExprTransformer<MethodCallExpression>{
    public Expression transform(final MethodCallExpression exp, final CompilerTransformer compiler) {
        Expression args = compiler.transform(exp.getArguments());
        exp.setArguments(args);

        if (exp.isSpreadSafe()) {
            compiler.addError("Spread operator is not supported by static compiler", exp);
            return null;
        }

        Object method = exp.getMethod();
        String methodName = null;
        if (!(method instanceof ConstantExpression) || !(((ConstantExpression) method).getValue() instanceof String)) {
          if (compiler.policy == CompilePolicy.STATIC) {
              compiler.addError("Non-static method name", exp);
              return null;
          }
          else {
              return createDynamicCall(exp, compiler);
          }
        }
        else {
          methodName = (String) ((ConstantExpression) method).getValue();
        }

        BytecodeExpr object;
        ClassNode type;
        MethodNode foundMethod = null;
        final ClassNode[] argTypes = compiler.exprToTypeArray(args);

        if (exp.getObjectExpression() instanceof ClassExpression) {
            object = null;

            type = ClassHelper.getWrapper(exp.getObjectExpression().getType());
            foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);
            if (foundMethod == null || !foundMethod.isStatic()) {
                return dynamicOrError(exp, compiler, methodName, type, argTypes, "Can't find static method ");
            }

            return new ResolvedMethodBytecodeExpr(exp, foundMethod, null, (ArgumentListExpression)args);
        }
        else {
            if (exp.getObjectExpression().equals(VariableExpression.THIS_EXPRESSION) && compiler.methodNode instanceof ClosureMethodNode) {
                int level = 0;
                for( ClosureMethodNode cmn = (ClosureMethodNode) compiler.methodNode; cmn != null; cmn = cmn.getOwner(), level++ ) {
                    ClassNode thisType = cmn.getParameters()[0].getType();
                    foundMethod = findMethodWithClosureCoercion(thisType, methodName, argTypes, compiler);
                    if (foundMethod != null) {
                        final int level1 = level;
                        object = new BytecodeExpr(exp.getObjectExpression(), thisType) {
                            protected void compile() {
                                mv.visitVarInsn(ALOAD, 0);
                                for (int i = 0; i != level1; ++i) {
                                    mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                    mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                                }
                                mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                            }
                        };
                        return new ResolvedMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression)args);
                    }

                    // checkDelegate
                    if (thisType.implementsInterface(TypeUtil.TCLOSURE)) {
                        final ClassNode tclosure = thisType.getInterfaces()[0];
                        final GenericsType[] genericsTypes = tclosure.getGenericsTypes();
                        if (genericsTypes != null) {
                            final ClassNode delegateType = genericsTypes[0].getType();
                            foundMethod = compiler.findMethod(delegateType, methodName, argTypes);
                            if (foundMethod != null) {
                                final int level3 = level;
                                object = new BytecodeExpr(exp.getObjectExpression(), delegateType) {
                                    protected void compile() {
                                        mv.visitVarInsn(ALOAD, 0);
                                        for (int i = 0; i != level3; ++i) {
                                            mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                            mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                                        }
                                        mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                        mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getDelegate", "()Ljava/lang/Object;");
                                        mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                                    }
                                };
                                return new ResolvedMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression)args);
                            }
                        }
                    }
                }

                foundMethod = findMethodWithClosureCoercion(compiler.classNode, methodName, argTypes, compiler);
                if (foundMethod != null) {
                    final int level2 = level;
                    object = new BytecodeExpr(exp.getObjectExpression(), compiler.classNode) {
                        protected void compile() {
                            mv.visitVarInsn(ALOAD, 0);
                            for (int i = 0; i != level2; ++i) {
                                mv.visitTypeInsn(CHECKCAST, "groovy/lang/Closure");
                                mv.visitMethodInsn(INVOKEVIRTUAL, "groovy/lang/Closure", "getOwner", "()Ljava/lang/Object;");
                            }
                            mv.visitTypeInsn(CHECKCAST, BytecodeHelper.getClassInternalName(getType()));
                        }
                    };
                    return new ResolvedMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression)args);
                }

                return dynamicOrError(exp, compiler, methodName, compiler.classNode, argTypes, "Can't find method ");
            } else {
                object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
                type = ClassHelper.getWrapper(object.getType());

                foundMethod = findMethodWithClosureCoercion(type, methodName, argTypes, compiler);

                if (foundMethod == null) {
                    return dynamicOrError(exp, compiler, methodName, type, argTypes, "Can't find method ");
                }

                return new ResolvedMethodBytecodeExpr(exp, foundMethod, object, (ArgumentListExpression)args);
            }
        }
    }

    private Expression dynamicOrError(MethodCallExpression exp, CompilerTransformer compiler, String methodName, ClassNode type, ClassNode[] argTypes, final String msg) {
        if (compiler.policy == CompilePolicy.STATIC) {
            compiler.addError(msg + getMethodDescr(type, methodName, argTypes), exp);
            return null;
        }
        else
            return createDynamicCall(exp, compiler);
    }

    private String getMethodDescr(ClassNode type, String methodName, ClassNode[] argTypes) {
        StringBuilder sb = new StringBuilder(type.getName())
                .append("#")
                .append(methodName)
                .append("(");
        for (int i = 0; i != argTypes.length; i++) {
            if (i!=0)
                sb.append(", ");
            sb.append(argTypes[i].getName());
        }
        sb.append(")");
        return sb.toString();
    }

    private void makeOneMethodClass(final ClassNode oarg, ClassNode tp, List am) {
        final ClassNode[] oifaces = oarg.getInterfaces();
        ClassNode [] ifaces = new ClassNode[oifaces.length+1];
        System.arraycopy(oifaces, 0, ifaces, 1, oifaces.length);
        ifaces [0] = tp;
        oarg.setInterfaces(ifaces);

        if (am.size() == 1) {
            final MethodNode missed = (MethodNode) am.get(0);
            oarg.addMethod(
            missed.getName(),
            ACC_PUBLIC,
            missed.getReturnType(),
            missed.getParameters(),
            ClassNode.EMPTY_ARRAY,
            new BytecodeSequence(
                new BytecodeInstruction() {
                    public void visit(MethodVisitor mv) {
                        mv.visitVarInsn(ALOAD, 0);
                        Parameter pp []  = missed.getParameters();
                        for (int i = 0, k = 1; i != pp.length; ++i) {
                            final ClassNode type = pp[i].getType();
                            if (ClassHelper.isPrimitiveType(type)) {
                                if (type == ClassHelper.long_TYPE) {
                                    mv.visitVarInsn(LLOAD, k++);
                                    k++;
                                }
                                else if (type == ClassHelper.double_TYPE) {
                                    mv.visitVarInsn(DLOAD, k++);
                                    k++;
                                }
                                else if (type == ClassHelper.float_TYPE) {
                                    mv.visitVarInsn(FLOAD, k++);
                                }
                                else {
                                    mv.visitVarInsn(ILOAD, k++);
                                }
                            }
                            else {
                                mv.visitVarInsn(ALOAD, k++);
                            }
                        }
                        mv.visitMethodInsn(
                                INVOKEVIRTUAL,
                                BytecodeHelper.getClassInternalName(oarg),
                                "doCall",
                                BytecodeHelper.getMethodDescriptor(ClassHelper.OBJECT_TYPE,pp)
                        );

                        if (ClassHelper.isPrimitiveType(missed.getReturnType())) {
                            String returnString = "(Ljava/lang/Object;)" + BytecodeHelper.getTypeDescription(missed.getReturnType());
                            mv.visitMethodInsn(
                                    Opcodes.INVOKESTATIC,
                                    BytecodeHelper.getClassInternalName(DefaultTypeTransformation.class.getName()),
                                    missed.getReturnType().getName() + "Unbox",
                                    returnString);
                        }
                        BytecodeExpr.doReturn(mv, missed.getReturnType());
                    }
                }
            ));
        }
    }

    private Expression createDynamicCall(final MethodCallExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr methodExpr = (BytecodeExpr) compiler.transform(exp.getMethod());
        final BytecodeExpr object = (BytecodeExpr) compiler.transform(exp.getObjectExpression());
        return new BytecodeExpr(exp, ClassHelper.OBJECT_TYPE) {
            protected void compile() {
                mv.visitInsn(ACONST_NULL);
                object.visit(mv);
                box(object.getType());

                methodExpr.visit(mv);

                final List args = ((ArgumentListExpression) exp.getArguments()).getExpressions();
                mv.visitLdcInsn(args.size());
                mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
                for (int j = 0; j != args.size(); ++j) {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(j);
                    ((BytecodeExpr)args.get(j)).visit(mv);
                    mv.visitInsn(AASTORE);
                }
                mv.visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/ScriptBytecodeAdapter", "invokeMethodN", "(Ljava/lang/Class;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;");
            }
        };
    }

    private MethodNode findMethodWithClosureCoercion (ClassNode type, String methodName, ClassNode [] argTypes, CompilerTransformer compiler) {
        MethodNode foundMethod = compiler.findMethod(type, methodName, argTypes);
        if (foundMethod == null) {
            if (argTypes.length > 0 && argTypes[argTypes.length-1].implementsInterface(TypeUtil.TCLOSURE)) {
                final ClassNode oarg = argTypes[argTypes.length-1];
                argTypes[argTypes.length-1] = null;
                foundMethod = compiler.findMethod(type, methodName, argTypes);
                if (foundMethod != null) {
                    Parameter p [] = foundMethod.getParameters();
                    if (p.length == argTypes.length) {
                        if (p[p.length-1].getType().isInterface()) {
                            final ClassNode tp = p[p.length - 1].getType();
                            final List am = tp.getAbstractMethods();
                            if (am.size() <= 1) {
                                makeOneMethodClass(oarg, tp, am);
                            }
                        }
                    }
                }
                argTypes[argTypes.length-1] = oarg;
            }
        }

        return foundMethod;
    }
}
