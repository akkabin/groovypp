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

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.classgen.Verifier;
import org.codehaus.groovy.classgen.BytecodeSequence;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.classgen.BytecodeHelper;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.reflection.ReflectionCache;
import org.mbte.groovypp.compiler.ClassNodeCache;
import org.mbte.groovypp.compiler.TypeUtil;
import org.objectweb.asm.MethodVisitor;

import java.lang.reflect.Modifier;
import java.util.List;

public class OpenVerifier extends Verifier {
    @Override
    public void visitClass(ClassNode classNode) {
        addMetaClassFieldIfNeeded(classNode);
        
        super.visitClass(classNode);

        for (FieldNode fieldNode : classNode.getFields()) {
            fieldNode.setInitialValueExpression(null);
        }
    }

    @Override
    protected void addGroovyObjectInterfaceAndMethods(ClassNode node, String classInternalName) {
        final List<AnnotationNode> list = node.getAnnotations(TypeUtil.TYPED);
        if (list == null || list.size() == 0 || list.get(0).getMember("value") != null)
            super.addGroovyObjectInterfaceAndMethods(node, classInternalName);
    }

    private void addMetaClassFieldIfNeeded(ClassNode node) {
        if (node.isInterface() || (node.getModifiers() & ACC_ABSTRACT) != 0)
            return;
        
        FieldNode ret = node.getDeclaredField("metaClass");
        if (ret != null) {
            // very dirty hack
            // stub generator for joint compiler calls Verifier, which adds properties/methods
            // we remove it initialization of metaClass here
            if (ret.getInitialExpression() != null && ret.getInitialExpression().getClass().getName().startsWith("org.codehaus.groovy.classgen.Verifier$")) {
                ret.setInitialValueExpression(null);
            }
            return;
        }

        ClassNode current = node;
        while (current != ClassHelper.OBJECT_TYPE) {
            current = current.getSuperClass();
            if (current == null) break;
            ret = current.getDeclaredField("metaClass");
            if (ret == null) continue;
            if (Modifier.isPrivate(ret.getModifiers())) continue;
            return;
        }

        node.addField("metaClass", ACC_PRIVATE | ACC_TRANSIENT | ACC_SYNTHETIC, ClassHelper.METACLASS_TYPE, null).setSynthetic(true);
    }

    protected void addTimeStamp(ClassNode node) {
    }
}
