/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public class BooleanLiteral extends Literal {

  public BooleanLiteral(String spelling, SourcePosition posn) {
    super (spelling,posn);
  }
 
  public <A,R> R visit(Visitor<A,R> v, A o) {
      return v.visitBooleanLiteral(this, o);
  }
}
