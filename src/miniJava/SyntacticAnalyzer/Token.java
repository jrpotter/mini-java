package miniJava.SyntacticAnalyzer;

import java.util.HashMap;

/**
 *
 */
public class Token {

	public enum TYPE {
		
		// Terminals
		ID, 
		NUM, 
		UNOP, 
		BINOP,
		EQUALS, 
		PERIOD, 
		COMMA, 
		LPAREN, 
		RPAREN, 
		LSQUARE, 
		RSQUARE, 
		LBRACKET, 
		RBRACKET, 
		SEMICOLON,

		// Keywords
		IF, 
		ELSE, 
		NEW, 
		INT, 
		VOID, 
		THIS, 
		TRUE, 
		FALSE, 
		CLASS, 
		WHILE, 
		RETURN, 
		BOOLEAN,
		STATIC,
		PUBLIC,
		PRIVATE,

		// End of Token Stream
		EOT
	};

	// Pair words with enumeration
	public final static HashMap<String, TYPE> keywords;
	static {
		keywords = new HashMap<String, TYPE>();
		keywords.put("class", TYPE.CLASS);
		keywords.put("return", TYPE.RETURN);
		keywords.put("public", TYPE.PUBLIC);
		keywords.put("private", TYPE.PRIVATE);
		keywords.put("static", TYPE.STATIC);
		keywords.put("int", TYPE.INT);
		keywords.put("boolean", TYPE.BOOLEAN);
		keywords.put("void", TYPE.VOID);
		keywords.put("this", TYPE.THIS);
		keywords.put("if", TYPE.IF);
		keywords.put("else", TYPE.ELSE);
		keywords.put("while", TYPE.WHILE);
		keywords.put("true", TYPE.TRUE);
		keywords.put("false", TYPE.FALSE);
		keywords.put("new", TYPE.NEW);
	}
	
	// Pair symbols with enumeration
	public final static HashMap<Integer, TYPE> symbols;
	static {
		symbols = new HashMap<Integer, TYPE>();
		symbols.put((int) '.', TYPE.PERIOD);
		symbols.put((int) ',', TYPE.COMMA);
		symbols.put((int) '[', TYPE.LSQUARE);
		symbols.put((int) ']', TYPE.RSQUARE);
		symbols.put((int) '{', TYPE.LBRACKET);
		symbols.put((int) '}', TYPE.RBRACKET);
		symbols.put((int) '(', TYPE.LPAREN);
		symbols.put((int) ')', TYPE.RPAREN);
		symbols.put((int) ';', TYPE.SEMICOLON);
	}

	public final TYPE type;
	public final String spelling;
	public final SourcePosition posn;

	public Token(String spelling, TYPE type, SourcePosition posn) {
		this.type = type;
		this.posn = posn;
		this.spelling = spelling;
	}
}
