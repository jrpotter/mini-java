/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {
	
	public Declaration(String name, Type type, SourcePosition posn) {
		super(posn);
		this.name = name;
		this.type = type;
	}
	
	public String name;
	public Type type;
}
