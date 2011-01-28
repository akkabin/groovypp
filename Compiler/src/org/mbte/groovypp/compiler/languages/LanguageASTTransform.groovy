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
@Typed package org.mbte.groovypp.compiler.languages

import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.transform.ASTTransformation
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.control.SourceUnit

import org.codehaus.groovy.ast.ModuleNode

abstract class LanguageASTTransform {

    @GroovyASTTransformation(phase = CompilePhase.CONVERSION)
    static class Conversion implements ASTTransformation {
        void visit(ASTNode[] nodes, SourceUnit source) {
            ASTNode astNode = nodes[0];

            if (!(astNode instanceof ModuleNode)) {
                return;
            }

            ModuleNode moduleNode = astNode

            Class<LanguageDefinition> scriptLanguageClass
            for(p in ScriptLanguageProvider.findProviders(source.classLoader)) {
                p.findScriptLanguage(moduleNode)?.newInstance()?.apply(moduleNode)
            }
        }
    }
}
