package org.mbte.groovypp.compiler.transformers;

import org.codehaus.groovy.ast.expr.BitwiseNegationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.ClassHelper;
import org.mbte.groovypp.compiler.CompilerTransformer;
import org.mbte.groovypp.compiler.bytecode.BytecodeExpr;

public class BitwiseNegationExpressionTransformer extends ExprTransformer<BitwiseNegationExpression> {
    public Expression transform(BitwiseNegationExpression exp, CompilerTransformer compiler) {
        final BytecodeExpr obj0 = (BytecodeExpr) compiler.transform(exp.getExpression());
        final BytecodeExpr obj;
        if (ClassHelper.isPrimitiveType(obj0.getType()))
            obj = new BytecodeExpr(exp, ClassHelper.getWrapper(obj0.getType())){
                protected void compile() {
                    obj0.visit(mv);
                    box(obj0.getType());
                }
            };
        else
            obj = obj0;
        return compiler.transform(new MethodCallExpression(obj, "bitwiseNegate", new ArgumentListExpression()));
    }
}
