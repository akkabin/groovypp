package groovy.lang;

import org.codehaus.groovy.transform.GroovyASTTransformationClass;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.TYPE})
@GroovyASTTransformationClass("org.mbte.groovypp.compiler.impl.CompileStaticASTTransform")
public @interface Compile {
    boolean debug () default false;

    CompileType value () default CompileType.STATIC;
}