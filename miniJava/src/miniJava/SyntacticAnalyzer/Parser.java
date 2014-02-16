package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.util.LinkedList;

public class Parser {
	
	private Scanner scanner;
	private LinkedList<Token> stream;
	
	public Parser(Scanner scanner) {
		this.scanner = scanner;
		this.stream = new LinkedList<Token>();
	}
		
	
	/**
	 * Program ::= (ClassDeclaration)* eot
	 * @throws IOException
	 */
	public void parse() throws IOException {
		while(peek(1).type == Token.TYPE.CLASS) parseClassDeclaration();
		accept(Token.TYPE.EOT);
	}
			
	
	/**
	 * ClassDeclaration ::=   
	 *     class id  {       
	 *	       (Declarators id (; | MethodDeclaration))*   
	 *     }
	 * @throws IOException
	 */
	private void parseClassDeclaration() throws IOException {
		
		// Class Header
		accept(Token.TYPE.CLASS);
		accept(Token.TYPE.ID);
		accept(Token.TYPE.LBRACKET);
		
		// Class Body
		while(peek(1).type != Token.TYPE.RBRACKET) {
			parseDeclarators();
			accept(Token.TYPE.ID);
			if(peek(1).type == Token.TYPE.SEMICOLON) accept(Token.TYPE.SEMICOLON);
			else parseMethodDeclaration();
		}
		
		accept(Token.TYPE.RBRACKET);	
	}
	

	/**
	 * MethodDeclaration ::=
	 *     (ParameterList?) {
	 *	       Statement* (return Expression ;)?
	 *	   }
	 * @throws IOException
	 */
	private void parseMethodDeclaration() throws IOException {

		// Method Header
		accept(Token.TYPE.LPAREN);
		if(peek(1).type != Token.TYPE.RPAREN) parseParameterList();
		accept(Token.TYPE.RPAREN);
		accept(Token.TYPE.LBRACKET);
		
		// Method Body
		while(peek(1).type != Token.TYPE.RBRACKET) {
			
			if(peek(1).type == Token.TYPE.RETURN) {
				accept(Token.TYPE.RETURN);
				parseExpression();
				accept(Token.TYPE.SEMICOLON);
				break;
			}
			
			parseStatement();
		}
		
		accept(Token.TYPE.RBRACKET);
	}
	

	/**
	 * Declarators ::= (public | private)? static? Type
	 * @throws IOException
	 */
	private void parseDeclarators() throws IOException {
		
		if(peek(1).type == Token.TYPE.PUBLIC) accept(Token.TYPE.PUBLIC);
		else if(peek(1).type == Token.TYPE.PRIVATE) accept(Token.TYPE.PRIVATE); 
		
		if(peek(1).type == Token.TYPE.STATIC) accept(Token.TYPE.STATIC);
		parseType();
	}
	

	/**
	 * Type ::= boolean | void | int ([])? | id ([])?
	 * @throws IOException
	 */
	private void parseType() throws IOException {
		
		Token.TYPE type = peek(1).type;
		
		switch(type) {
		
			case BOOLEAN:
			case VOID:
				accept(type);
				break;
				
			case INT:
			case ID:
				accept(type);
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					accept(Token.TYPE.RSQUARE);
				}
				break;
				
			default:
				throw new IOException();
		}
	}
	

	/**
	 * ParameterList ::= Type id (, Type id)*
	 * @throws IOException
	 */
	private void parseParameterList() throws IOException {
		parseType();
		accept(Token.TYPE.ID);
		while(peek(1).type == Token.TYPE.COMMA) {
			accept(Token.TYPE.COMMA);
			parseType();
			accept(Token.TYPE.ID);
		}
	}
	

	/**
	 * ArgumentList ::= Expression (, Expression)*
	 * @throws IOException
	 */
	private void parseArgumentList() throws IOException {
		parseExpression();
		while(peek(1).type == Token.TYPE.COMMA) {
			accept(Token.TYPE.COMMA);
			parseExpression();
		}
	}
	

	/**
	 * Reference ::= BaseRef (. BaseRef)*
	 * @throws IOException
	 */
	private void parseReference() throws IOException {
		parseBaseRef();
		while(peek(1).type == Token.TYPE.PERIOD) {
			accept(Token.TYPE.PERIOD);
			accept(Token.TYPE.ID);
			if(peek(1).type == Token.TYPE.LSQUARE) {
				accept(Token.TYPE.LSQUARE);
				parseExpression();
				accept(Token.TYPE.RSQUARE);
			}
		}
	}
	
	
	/**
	 * BaseRef ::= this | id ([ Expression])?
	 * @throws IOException
	 */
	private void parseBaseRef() throws IOException {
		
		switch(peek(1).type) {
		
			// this
			case THIS:
				accept(Token.TYPE.THIS);
				break;
				
			// id ([ Expression])?
			default:
				accept(Token.TYPE.ID);
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					parseExpression();
					accept(Token.TYPE.RSQUARE);
				}
				break;
		}
	}
	

	/**
	 * Statement ::=
	 *     {Statement*}
	 *	 | Type id = Expression;
	 *	 | Reference = Expression;
	 *	 | Reference ( ArgumentList? );
	 *	 | if (Expression) Statement (else Statement)?
	 *	 | while (Expression) Statement
	 * @throws IOException
	 */
	private void parseStatement() throws IOException {
		
		switch(peek(1).type) {
		
			// { Statement* }
			case LBRACKET:
				accept(Token.TYPE.LBRACKET);
				while(peek(1).type != Token.TYPE.RBRACKET) {
					parseStatement();
				}
				accept(Token.TYPE.RBRACKET);
				break;
				
			// if (Expression) Statement (else Statement)?
			case IF:
				accept(Token.TYPE.IF);
				accept(Token.TYPE.LPAREN);
				parseExpression();
				accept(Token.TYPE.RPAREN);
				parseStatement();
				if(peek(1).type == Token.TYPE.ELSE) {
					accept(Token.TYPE.ELSE);
					parseStatement();
				}
				break;
				
			// while (Expression) Statement
			case WHILE:
				accept(Token.TYPE.WHILE);
				accept(Token.TYPE.LPAREN);
				parseExpression();
				accept(Token.TYPE.RPAREN);
				parseStatement();
				break;
				
			// Type id = Expression ;
			case BOOLEAN:
			case VOID:
			case INT:
			case ID:
				
				// Must be a type though there is a possibility of a reference
				if(peek(1).type != Token.TYPE.ID
				|| peek(2).type == Token.TYPE.ID
				||(peek(2).type == Token.TYPE.LSQUARE && peek(3).type == Token.TYPE.RSQUARE)) {
					parseType();
					accept(Token.TYPE.ID);
					accept(Token.TYPE.EQUALS);
					parseExpression();
					accept(Token.TYPE.SEMICOLON);
					break;
				}
				
				/* Fall Through */
				
			// Reference = Expression ; | Reference ( ArgumentList? ) ; 
			default:
				parseReference();
				
				if(peek(1).type == Token.TYPE.LPAREN) {
					accept(Token.TYPE.LPAREN);
					if(peek(1).type != Token.TYPE.RPAREN) parseArgumentList();
					accept(Token.TYPE.RPAREN);
				} else {
					accept(Token.TYPE.EQUALS);
					parseExpression();
				}
				
				accept(Token.TYPE.SEMICOLON);
				break;
		}		
	}
	
	
	/**
	 * Expression ::=
	 *     Reference
	 *   | Reference ( ArgumentList? )
	 *	 | unop Expression
	 *   | Expression binop Expression
	 *   | ( Expression )
	 *   | num | true | false
	 *   | new (id() | int [ Expression ] | id [ Expression ] )
	 * @throws IOException
	 */
	private void parseExpression() throws IOException {
		
		switch(peek(1).type) {
		
			// num | true | false
			case NUM:
			case TRUE:
			case FALSE:
				accept(peek(1).type);
				break;
				
			// ( Expression )
			case LPAREN:
				accept(Token.TYPE.LPAREN);
				parseExpression();
				accept(Token.TYPE.RPAREN);
				break;
				
			// unop Expression
			case UNOP:
				accept(Token.TYPE.UNOP);
				parseExpression();
				break;
				
			// Must be a minus sign
			case BINOP:
				if(peek(1).attr.equals("-")) {
					accept(Token.TYPE.BINOP);
					parseExpression();
				}
				else throw new IOException();
				break;
				
			// new ( int [ Expression ]  | id ( ) | id [ Expression ] )
			case NEW:
				accept(Token.TYPE.NEW);
				
				if(peek(1).type == Token.TYPE.INT) {
					accept(Token.TYPE.INT);
					accept(Token.TYPE.LSQUARE);
					parseExpression();
					accept(Token.TYPE.RSQUARE);
				}
				
				else {
					accept(Token.TYPE.ID);
					
					if(peek(1).type == Token.TYPE.LPAREN){
						accept(Token.TYPE.LPAREN);
						accept(Token.TYPE.RPAREN);
					}
					
					else {
						accept(Token.TYPE.LSQUARE);
						parseExpression();
						accept(Token.TYPE.RSQUARE);
					}
				}
				
				break;
				
			// Reference | Reference (ArgumentList?)
			case THIS:
			case ID:
				parseReference();
				if(peek(1).type == Token.TYPE.LPAREN) {
					accept(Token.TYPE.LPAREN);
					if(peek(1).type != Token.TYPE.RPAREN) parseArgumentList();
					accept(Token.TYPE.RPAREN);
				}
				break;
				
			// Expression binop Expression
			default:
				accept(Token.TYPE.BINOP);
				parseExpression();
				break;
		}
		
		// Expression binop Expression
		if(peek(1).type == Token.TYPE.BINOP) {
			accept(Token.TYPE.BINOP);
			parseExpression();
		}
	}
	
	
	/**
	 * Sees what the next token is, caching the result.
	 * @return
	 * @throws IOException
	 */
	private Token peek(int lookahead) throws IOException {
		
		// Cache tokens
		while(stream.size() < lookahead) {
			Token next = scanner.scan();
			stream.addLast(next);
		}
		
		return stream.get(lookahead - 1);
	}
	
	
	/**
	 * Consumes token or throws exception.
	 * @throws IOException
	 */
	private void accept(Token.TYPE type) throws IOException {
		Token next = peek(1);
		if(next.type == type) stream.poll();
		else throw new IOException();
	}
}
