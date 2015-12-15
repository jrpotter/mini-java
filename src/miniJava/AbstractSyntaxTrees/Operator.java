/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;
import miniJava.SyntacticAnalyzer.Token;

public class Operator extends Terminal {

    public Operator(Token t, SourcePosition posn) {
        super(t.spelling, posn);
    }

    public <A, R> R visit(Visitor<A, R> v, A o) {
        return v.visitOperator(this, o);
    }

    public Token token;
}
