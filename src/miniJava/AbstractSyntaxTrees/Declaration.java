/**
 * miniJava Abstract Syntax Tree classes
 * @author prins
 * @version COMP 520 (v2.2)
 */
package miniJava.AbstractSyntaxTrees;

import miniJava.CodeGenerator.RuntimeEntity;
import miniJava.SyntacticAnalyzer.SourcePosition;

public abstract class Declaration extends AST {

	public Declaration(String name, Type type, SourcePosition posn) {
		super(posn);
		this.name = name;
		this.type = type;
	}

	@Override
	public String toString() {
		if (posn != null) {
			return this.name + "(Line: " + posn.line + ", Column: " + posn.col
					+ ")";
		} else {
			return super.toString();
		}
	}

	public RuntimeEntity entity;
	public String name;
	public Type type;
}
