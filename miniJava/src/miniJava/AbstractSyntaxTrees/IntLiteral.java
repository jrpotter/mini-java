/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class IntLiteral extends Literal {

  public IntLiteral(String s, SourcePosition posn) {
    super(s, posn);
  }
 
  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitIntLiteral(this, o);
  }
}