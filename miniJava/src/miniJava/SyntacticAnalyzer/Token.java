package miniJava.SyntacticAnalyzer;

import java.util.HashMap;

public class Token {
	
	public enum TYPE {
		
		// Possible Terminals
		ID,
		NUM,
		UNOP,
		BINOP,
		
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
		
		// Declarators
		STATIC,
		PUBLIC,
		PRIVATE,
		
		// Other Terminals
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
		
		// End of Token Stream
		EOT
	};
	
	 public final static HashMap<String, TYPE> keywords;
	 static
     {
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
	 
	public final TYPE type;
	public final String attr;
		
	public Token(String attr, TYPE type) {
		this.type = type;
		this.attr = attr;
	}
}
