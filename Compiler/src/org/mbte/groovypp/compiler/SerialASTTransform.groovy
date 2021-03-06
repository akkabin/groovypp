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





package org.mbte.groovypp.compiler

import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.syntax.SyntaxException
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.mbte.groovypp.compiler.TypeUtil
import org.objectweb.asm.Opcodes
import org.codehaus.groovy.ast.*
import static org.codehaus.groovy.ast.ClassHelper.make
import org.codehaus.groovy.ast.expr.*

@Typed
@GroovyASTTransformation (phase = CompilePhase.CANONICALIZATION)
class SerialASTTransform implements ASTTransformation, Opcodes {
    static final ClassNode EXTERNALIZABLE = make(Externalizable)
    static final ClassNode OBJECT_INPUT = make(ObjectInput)
    static final ClassNode OBJECT_OUTPUT = make(ObjectOutput)

    static final Parameter [] readExternalParams = [[OBJECT_INPUT, "__input__"]]
    static final Parameter [] writeExternalParams = [[OBJECT_OUTPUT, "__output__"]]

    void visit(ASTNode[] nodes, SourceUnit source) {
        ModuleNode module = nodes[0]
        for (ClassNode classNode: module.classes) {
            processClass classNode, source
        }
    }

    public static void processClass (ClassNode classNode, SourceUnit source) {
        for( c in classNode.innerClasses)
            processClass(c, source)
        
        if (!classNode.implementsInterface(EXTERNALIZABLE))
            return

        def hasDefConstructor = false
        def declaredConstructors = classNode.declaredConstructors
        for(constructor in declaredConstructors) {
            if (!constructor.parameters.length && constructor.public) {
                hasDefConstructor = true
                break
            }
        }

        if (!hasDefConstructor) {
            if (!declaredConstructors?.empty)
                source.addError(new SyntaxException("Class implementing java.io.Externalizable must have public no-arg constructor", classNode.lineNumber, classNode.columnNumber))
            else {
                ConstructorNode constructor = [Opcodes.ACC_PUBLIC, null]
                constructor.synthetic = true
                classNode.addConstructor(constructor)
            }
        }

        def readMethod = classNode.getDeclaredMethod("readExternal", readExternalParams)
        def writeMethod = classNode.getDeclaredMethod("writeExternal", writeExternalParams)

        if (!readMethod && !writeMethod) {
            addReadWriteExternal(classNode)
        }
    }

    private static def addReadWriteExternal(ClassNode classNode) {
        def readCode = new BlockStatement()
        def writeCode = new BlockStatement()

        if (classNode.superClass != ClassHelper.OBJECT_TYPE) {
            readCode.addStatement(new ExpressionStatement(new MethodCallExpression(VariableExpression.SUPER_EXPRESSION, "readExternal", new ArgumentListExpression(new VariableExpression("__input__")))))
            writeCode.addStatement(new ExpressionStatement(new MethodCallExpression(VariableExpression.SUPER_EXPRESSION, "writeExternal", new ArgumentListExpression(new VariableExpression("__output__")))))
        }

        for (f in classNode.fields) {
            if (f.static || (f.modifiers & Opcodes.ACC_TRANSIENT) != 0)
                continue

            def tname = f.type.name
            if (ClassHelper.isPrimitiveType(f.type))
                tname = tname[0].toUpperCase() + tname.substring(1)
            else {
                if (f.type == ClassHelper.STRING_TYPE)
                    tname = "UTF"
                else
                    tname = "Object"
            }

            // this.prop = __input__.readXXX()
            readCode.addStatement(new ExpressionStatement(
                    new BinaryExpression(
                            new PropertyExpression(VariableExpression.THIS_EXPRESSION, f.name),
                            Token.newSymbol(Types.ASSIGN, -1, -1),
                            new MethodCallExpression(
                                    new VariableExpression(readExternalParams[0]),
                                    "read" + tname,
                                    new ArgumentListExpression()
                            )
                    )
            ))
            // __output__.writeXXX(this.prop)
            writeCode.addStatement(new ExpressionStatement(
                    new MethodCallExpression(
                            new VariableExpression(writeExternalParams[0]),
                            "write" + tname,
                            new ArgumentListExpression(
                                    new PropertyExpression(VariableExpression.THIS_EXPRESSION, f.name),
                            )
                    )
            ))
        }

        def readMethod = classNode.addMethod("readExternal", Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, readExternalParams, ClassNode.EMPTY_ARRAY, readCode)
        readMethod.addAnnotation(new AnnotationNode(TypeUtil.TYPED))

        def writeMethod = classNode.addMethod("writeExternal", Opcodes.ACC_PUBLIC, ClassHelper.VOID_TYPE, writeExternalParams, ClassNode.EMPTY_ARRAY, writeCode)
        writeMethod.addAnnotation(new AnnotationNode(TypeUtil.TYPED))
    }
}
