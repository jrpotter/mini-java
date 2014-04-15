package miniJava.Exceptions;

import miniJava.SyntacticAnalyzer.Token;

public class ParsingException extends Exception {

	private static final long serialVersionUID = 1L;

	public ParsingException() {
		super("Unidentified parsing error!");
	}

	public ParsingException(Token t) {
		super("Parsing error with " + t.spelling + " at " + t.posn.toString());
	}

}
