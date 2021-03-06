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

package org.mbte.groovypp.compiler;

import groovy.lang.TypePolicy;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeInstruction;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.MultipleCompilationErrorsException;
import org.mbte.groovypp.compiler.asm.StoringMethodVisitor;
import org.mbte.groovypp.compiler.asm.UnneededLoadPopRemoverMethodAdapter;
import org.objectweb.asm.MethodVisitor;

import java.util.Iterator;
import java.util.List;

public class StaticMethodBytecode extends StoredBytecodeInstruction {
    final MethodNode methodNode;
    final SourceUnit su;
    Statement code;
    final StaticCompiler compiler;

    public StaticMethodBytecode(MethodNode methodNode, SourceUnitContext context, SourceUnit su, Statement code, CompilerStack compileStack, int debug, boolean fastArrays, TypePolicy policy, String baseClosureName) {
        this.methodNode = methodNode;
        this.su = su;
        this.code = code;

        StoringMethodVisitor storage = (StoringMethodVisitor) createStorage();
        MethodVisitor mv = storage;
        if (debug != -1) {
            try {
                mv = DebugMethodAdapter.create(mv, debug);
            }
            catch (Throwable t) {
                t.printStackTrace();
                throw new RuntimeException(t);
            }
        }
        compiler = new StaticCompiler(
                su,
                context,
                this,
                new UnneededLoadPopRemoverMethodAdapter(mv),
                compileStack,
                debug,
                fastArrays,
                policy, baseClosureName);
//
        if (debug != -1)
            org.mbte.groovypp.compiler.DebugContext.outputStream.println("-----> " + methodNode.getDeclaringClass().getName() + "#" + methodNode.getName() + "(" + BytecodeHelper.getMethodDescriptor(methodNode.getReturnType(), methodNode.getParameters()) + ") " + BytecodeHelper.getGenericsMethodSignature(methodNode));

        try {
            compiler.execute();
            storage.redirect();
        }
        catch (MultipleCompilationErrorsException me) {
            clear ();
            throw me;
        }
        catch (Throwable t) {
            clear ();
            t.printStackTrace();
            compiler.addError("Internal Error: " + t.toString(), methodNode);
        }

        if (debug != -1)
            DebugContext.outputStream.println("------------");
    }

    public static void replaceMethodCode(SourceUnit source, SourceUnitContext context, MethodNode methodNode, CompilerStack compileStack, int debug, boolean fastArrays, TypePolicy policy, String baseClosureName) {
        if(!methodNode.getAnnotations(TypeUtil.IMPROVED_TYPES).isEmpty()) {
            for(Iterator<AnnotationNode> it = methodNode.getAnnotations().iterator(); it.hasNext(); ) {
                if(it.next().getClassNode().equals(TypeUtil.IMPROVED_TYPES)) {
                    it.remove();
                    break;
                }
            }
        }

        if (methodNode instanceof ClosureMethodNode.Dependent)
            methodNode = ((ClosureMethodNode.Dependent)methodNode).getMaster();
        
        final Statement code = methodNode.getCode();
        if (!(code instanceof BytecodeSequence)) {
            try {
                final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(methodNode, context, source, code, compileStack, debug, fastArrays, policy, baseClosureName);
                if(!methodNode.getName().equals("<clinit>"))
                    methodNode.setCode(new MyBytecodeSequence(methodBytecode));
                else {
                    BlockStatement blockStatement = new BlockStatement();
                    blockStatement.getStatements().add(new MyBytecodeSequence(methodBytecode));
                    methodNode.setCode(blockStatement);
                }
                if (methodBytecode.compiler.shouldImproveReturnType && !TypeUtil.NULL_TYPE.equals(methodBytecode.compiler.calculatedReturnType))
                    methodNode.setReturnType(methodBytecode.compiler.calculatedReturnType);
            }
            catch (MultipleCompilationErrorsException ce) {
                handleCompilationError(methodNode, ce);
                throw ce;
            }
        }

        if (methodNode instanceof ClosureMethodNode) {
            ClosureMethodNode closureMethodNode = (ClosureMethodNode) methodNode;
            List<ClosureMethodNode.Dependent> dependentMethods = closureMethodNode.getDependentMethods();
            if (dependentMethods != null)
                for (ClosureMethodNode.Dependent dependent : dependentMethods) {
                    final Statement mCode = dependent.getCode();
                    if (!(mCode instanceof BytecodeSequence)) {
                        try {
                            final StaticMethodBytecode methodBytecode = new StaticMethodBytecode(dependent, context, source, mCode, compileStack, debug, fastArrays, policy, baseClosureName);
                            dependent.setCode(new MyBytecodeSequence(methodBytecode));
                        }
                        catch (MultipleCompilationErrorsException ce) {
                            handleCompilationError(methodNode, ce);
                            throw ce;
                        }
                    }
                }
        }
    }

    private static void handleCompilationError(MethodNode methodNode, MultipleCompilationErrorsException ce) {
        methodNode.setCode(new BytecodeSequence(new BytecodeInstruction(){public void visit(MethodVisitor mv) {}}));
        if (methodNode instanceof ClosureMethodNode) {
            ClosureMethodNode closureMethodNode = (ClosureMethodNode) methodNode;
            List<ClosureMethodNode.Dependent> dependentMethods = closureMethodNode.getDependentMethods();
            if (dependentMethods != null)
                for (ClosureMethodNode.Dependent dependent : dependentMethods) {
                    final Statement mCode = dependent.getCode();
                    if (!(mCode instanceof BytecodeSequence)) {
                            dependent.setCode(new BytecodeSequence(new BytecodeInstruction(){public void visit(MethodVisitor mv) {}}));
                    }
                }
        }
    }

    private static class MyBytecodeSequence extends BytecodeSequence {
        public MyBytecodeSequence(StaticMethodBytecode instruction) {
            super(instruction);
        }

        @Override
        public void visit(GroovyCodeVisitor visitor) {
            if(visitor instanceof AsmClassGenerator)
                ((AsmClassGenerator)visitor).visitBytecodeSequence(this);
        }
    }
}
