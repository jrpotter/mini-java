package miniJava.SyntacticAnalyzer;

import java.util.LinkedList;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.Exceptions.*;

public class Parser {
	
	private Scanner scanner;
	private LinkedList<Token> stream;
	
	public Parser(Scanner scanner) {
		this.scanner = scanner;
		this.stream = new LinkedList<Token>();
	}
		
	/**
	 * Program ::= (ClassDeclaration)* eot
	 * @return
	 * @throws ScanningException
	 */
	public Package parse() throws ParsingException, ScanningException {
		ClassDeclList decls = new ClassDeclList();
		while(peek(1).type == Token.TYPE.CLASS) {
			decls.add(parseClassDeclaration());
		}
		
		accept(Token.TYPE.EOT);
		return new Package(decls, new SourcePosition(0, 0));
	}
			
	/**
	 * ClassDeclaration ::=   
	 *     class id  {       
	 *	       (Declarators id (; | MethodDeclaration))*   
	 *     }
	 * @return
	 * @throws ScanningException
	 */
	private ClassDecl parseClassDeclaration() throws ParsingException, ScanningException {
		
		// Class Header
		Token classToken = accept(Token.TYPE.CLASS);
		String cn = accept(Token.TYPE.ID).spelling;
		accept(Token.TYPE.LBRACKET);
		
		// Setup
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		
		// Class Body
		while(peek(1).type != Token.TYPE.RBRACKET) {
			Declarators d = parseDeclarators();
			String name = accept(Token.TYPE.ID).spelling;
			FieldDecl f = new FieldDecl(d.isPrivate, d.isStatic, d.mt, name, d.posn);
			
			// Field Declarations
			if(peek(1).type == Token.TYPE.SEMICOLON) {
				accept(Token.TYPE.SEMICOLON);
				fdl.add(f);
			} 
			
			// Method Declarations
			else mdl.add(parseMethodDeclaration(f));
		}
		
		accept(Token.TYPE.RBRACKET);
		return new ClassDecl(cn, fdl, mdl, classToken.posn);
	}
	
	/**
	 * Declarators ::= (public | private)? static? Type
	 * @return
	 * @throws ScanningException
	 */
	private Declarators parseDeclarators() throws ParsingException, ScanningException {
		
		// Visibility
		SourcePosition start = null;
		boolean isPrivate = false;
		
		if(peek(1).type == Token.TYPE.PUBLIC) {
			start = accept(Token.TYPE.PUBLIC).posn;
		} else if(peek(1).type == Token.TYPE.PRIVATE) {
			isPrivate = true;
			start = accept(Token.TYPE.PRIVATE).posn; 
		}
		
		// Class Methods
		boolean isStatic = false;
		if(peek(1).type == Token.TYPE.STATIC) {
			isStatic = true;
			if(start == null) {
				start = accept(Token.TYPE.STATIC).posn;
			} else {
				accept(Token.TYPE.STATIC);
			}
		}
		
		Type t = parseType();
		if(start == null) {
			start = t.posn;
		}
		
		return new Declarators(isPrivate, isStatic, t, start);
	}

	/**
	 * * MethodDeclaration ::=
	 *     (ParameterList?) {
	 *	       Statement* (return Expression ;)?
	 *	   }
	 * @param f describes the declaratory aspect of the method
	 * @return
	 * @throws ScanningException
	 */
	private MethodDecl parseMethodDeclaration(FieldDecl f) throws ParsingException, ScanningException {
		
		// Method Header
		accept(Token.TYPE.LPAREN);
		
		// Parameter List
		ParameterDeclList pdl = new ParameterDeclList();
		if(peek(1).type != Token.TYPE.RPAREN) pdl = parseParameterList();
		
		accept(Token.TYPE.RPAREN);
		accept(Token.TYPE.LBRACKET);
		
		// Method Body
		Expression re = null;
		StatementList stl = new StatementList();
		while(peek(1).type != Token.TYPE.RBRACKET) {
			
			if(peek(1).type == Token.TYPE.RETURN) {
				accept(Token.TYPE.RETURN);
				re = parseExpression();
				accept(Token.TYPE.SEMICOLON);
				break;
			}
			
			stl.add(parseStatement());
		}
		
		accept(Token.TYPE.RBRACKET);
		return new MethodDecl(f, pdl, stl, re, f.posn);
	}
	
	/**
	 * Type ::= boolean | void | int ([])? | id ([])?
	 * @return
	 * @throws ScanningException
	 */
	private Type parseType() throws ParsingException, ScanningException {
		
		SourcePosition posn = null;
		
		switch(peek(1).type) {
		
			case BOOLEAN:
				posn = accept(Token.TYPE.BOOLEAN).posn;
				return new BaseType(TypeKind.BOOLEAN, posn);
				
			case VOID:
				posn = accept(Token.TYPE.VOID).posn;
				return new BaseType(TypeKind.VOID, posn);
				
			case INT: {
				posn = accept(Token.TYPE.INT).posn;
				BaseType b = new BaseType(TypeKind.INT, posn);
				
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					accept(Token.TYPE.RSQUARE);
					return new ArrayType(b, posn);
				}
				
				return b;
			}
				
			case ID: {
				Token id = accept(peek(1).type);
				Identifier i = new Identifier(id.spelling, id.posn);
				ClassType c = new ClassType(i, id.posn);
				
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					accept(Token.TYPE.RSQUARE);
					return new ArrayType(c, id.posn);
				}
				
				return c;
			}
				
			default:
				throw new ParsingException();
		}
	}
	
	/**
	 * ParameterList ::= Type id (, Type id)*
	 * @return
	 * @throws ScanningException
	 */
	private ParameterDeclList parseParameterList() throws ParsingException, ScanningException {
		
		ParameterDeclList decls = new ParameterDeclList(); 
		
		// First Parameter
		Type t = parseType();
		Token id = accept(Token.TYPE.ID);
		decls.add(new ParameterDecl(t, id.spelling, id.posn));
		
		// Remainder of List
		while(peek(1).type == Token.TYPE.COMMA) {
			accept(Token.TYPE.COMMA);
			Type nextType = parseType();
			Token nextId = accept(Token.TYPE.ID);
			decls.add(new ParameterDecl(nextType, nextId.spelling, nextId.posn));
		}
		
		return decls;
	}
	
	/**
	 * ArgumentList ::= Expression (, Expression)*
	 * @return
	 * @throws ScanningException
	 */
	private ExprList parseArgumentList() throws ParsingException, ScanningException {
		ExprList e = new ExprList();
		e.add(parseExpression());
		while(peek(1).type == Token.TYPE.COMMA) {
			accept(Token.TYPE.COMMA);
			e.add(parseExpression());
		}
		
		return e;
	}
	
	/**
	 * Reference ::= BaseRef (. BaseRef)*
	 * @return
	 * @throws ScanningException
	 */
	private Reference parseReference() throws ParsingException, ScanningException {
		Reference r = parseBaseRef();
		while(peek(1).type == Token.TYPE.PERIOD) {
			accept(Token.TYPE.PERIOD);
			Token tokenId = accept(Token.TYPE.ID);
			Identifier id = new Identifier(tokenId.spelling, tokenId.posn);
			r = new QualifiedRef(r, id, tokenId.posn);
			
			if(peek(1).type == Token.TYPE.LSQUARE) {
				accept(Token.TYPE.LSQUARE);
				Expression e = parseExpression();
				accept(Token.TYPE.RSQUARE);
				r = new IndexedRef(r, e, tokenId.posn);
			}
		}		
		
		return r;
	}

	/**
	 * BaseRef ::= this | id ([ Expression])?
	 * @return
	 * @throws ScanningException
	 */
	private Reference parseBaseRef() throws ParsingException, ScanningException {
		
		switch(peek(1).type) {
			case THIS: {
				Token thisToken = accept(Token.TYPE.THIS);
				return new ThisRef(thisToken.posn);
			}
				
			// id ([ Expression])?
			default: {
				Token id = accept(Token.TYPE.ID);
				Identifier i = new Identifier(id.spelling, id.posn);
				IdRef r = new IdRef(i, id.posn);
				
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					Expression e = parseExpression();
					accept(Token.TYPE.RSQUARE);
					return new IndexedRef(r, e, id.posn);
				}
				
				return r;
			}
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
	 * @return
	 * @throws ScanningException
	 */
	private Statement parseStatement() throws ParsingException, ScanningException {
		
		switch(peek(1).type) {
		
			// { Statement* }
			case LBRACKET: {
				Token leftToken = accept(Token.TYPE.LBRACKET);
				StatementList stl = new StatementList();
				while(peek(1).type != Token.TYPE.RBRACKET) {
					stl.add(parseStatement());
				}
				accept(Token.TYPE.RBRACKET);
				
				return new BlockStmt(stl, leftToken.posn);
			}
				
			// if (Expression) Statement (else Statement)?
			case IF: {
				Token ifToken = accept(Token.TYPE.IF);
				accept(Token.TYPE.LPAREN);
				Expression e = parseExpression();
				accept(Token.TYPE.RPAREN);
				Statement s1 = parseStatement();
				if(peek(1).type == Token.TYPE.ELSE) {
					accept(Token.TYPE.ELSE);
					Statement s2 = parseStatement();
					return new IfStmt(e, s1, s2, ifToken.posn);
				}
				
				return new IfStmt(e, s1, ifToken.posn);
			}
				
			// while (Expression) Statement
			case WHILE: {
				Token whileToken = accept(Token.TYPE.WHILE);
				accept(Token.TYPE.LPAREN);
				Expression e = parseExpression();
				accept(Token.TYPE.RPAREN);
				Statement s = parseStatement();
				
				return new WhileStmt(e, s, whileToken.posn);
			}
				
			// Type id = Expression ;
			case BOOLEAN: case VOID: case INT: case ID: {
				
				// Must be a type though there is a possibility of a reference
				if(peek(1).type != Token.TYPE.ID || peek(2).type == Token.TYPE.ID
				||(peek(2).type == Token.TYPE.LSQUARE && peek(3).type == Token.TYPE.RSQUARE)) {
					Type t = parseType();
					String name = accept(Token.TYPE.ID).spelling;
					VarDecl v = new VarDecl(t, name, t.posn);
					
					accept(Token.TYPE.EQUALS);
					Expression e = parseExpression();
					accept(Token.TYPE.SEMICOLON);
					
					return new VarDeclStmt(v, e, t.posn);
				}
				
				/* Fall Through */
			}

			default: {
				Statement s = null;
				Reference r = parseReference();
				
				// Reference ( ArgumentList? ) ; 
				if(peek(1).type == Token.TYPE.LPAREN) {
					ExprList e = new ExprList();
					accept(Token.TYPE.LPAREN);
					if(peek(1).type != Token.TYPE.RPAREN) {
						e = parseArgumentList();
					}
					accept(Token.TYPE.RPAREN);
					s = new CallStmt(r, e, r.posn);
				} 
				
				// Reference = Expression ;
				else {
					accept(Token.TYPE.EQUALS);
					Expression e = parseExpression();
					s = new AssignStmt(r, e, r.posn);
				}
				
				accept(Token.TYPE.SEMICOLON);
				return s;
			}
		}
	}
	
	/**
	 * Expression ::=
	 *     Reference
	 *   | Reference ( ArgumentList? )
	 *	 | unop Expression
	 *   | ( Expression )
	 *   | num | true | false
	 *   | new (id() | int [ Expression ] | id [ Expression ] )
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseSingleExpression() throws ParsingException, ScanningException {
	
		Expression e = null;
		switch(peek(1).type) {
		
			// num
			case NUM: {
				Token number = accept(Token.TYPE.NUM);
				IntLiteral i = new IntLiteral(number.spelling, number.posn);
				e = new LiteralExpr(i, number.posn);
				break;
			}
			
			// true | false
			case TRUE:
			case FALSE: {
				Token bool = accept(peek(1).type);
				BooleanLiteral b = new BooleanLiteral(bool.spelling, bool.posn);
				e = new LiteralExpr(b, bool.posn);
				break;
			}
				
			// ( Expression )
			case LPAREN: {
				accept(Token.TYPE.LPAREN);
				e = parseExpression();
				accept(Token.TYPE.RPAREN);
				break;
			}
				
			// unop Expression
			case UNOP:
			case BINOP: {
				if(peek(1).spelling.equals("!") || peek(1).spelling.equals("-")) {
					Token opToken = accept(peek(1).type);
					Operator o = new Operator(opToken, opToken.posn);
					e = new UnaryExpr(o, parseSingleExpression(), opToken.posn);
				} 
				else throw new ParsingException();
				break;
			}
				
			// new ( int [ Expression ]  | id ( ) | id [ Expression ] )
			case NEW: {
				Token newToken = accept(Token.TYPE.NEW);
				
				if(peek(1).type == Token.TYPE.INT) {
					accept(Token.TYPE.INT);
					accept(Token.TYPE.LSQUARE);
					Expression e2 = parseExpression();
					accept(Token.TYPE.RSQUARE);
					
					BaseType b = new BaseType(TypeKind.INT, newToken.posn);
					e = new NewArrayExpr(b, e2, newToken.posn);
				}
				
				else {
					Token id = accept(Token.TYPE.ID);
					Identifier i = new Identifier(id.spelling, id.posn);
					ClassType c = new ClassType(i, id.posn);
					
					if(peek(1).type == Token.TYPE.LPAREN){
						accept(Token.TYPE.LPAREN);
						accept(Token.TYPE.RPAREN);
						e = new NewObjectExpr(c, id.posn);
					} else {
						accept(Token.TYPE.LSQUARE);
						Expression e2 = parseExpression();
						accept(Token.TYPE.RSQUARE);
						e = new NewArrayExpr(c, e2, id.posn);
					}
				}
				
				break;
			}
				
			// Reference ((ArgumentList?))?
			case THIS: case ID: {
				Reference r = parseReference();
				if(peek(1).type == Token.TYPE.LPAREN) {
					accept(Token.TYPE.LPAREN);
					ExprList el = new ExprList();
					if(peek(1).type != Token.TYPE.RPAREN) {
						el = parseArgumentList();
					}
					accept(Token.TYPE.RPAREN);
					e = new CallExpr(r, el, r.posn);
				} else {
					e = new RefExpr(r, r.posn);
				}
				
				break;
			}
				
			default:
				throw new ParsingException();
		}
		
		return e;
	}
	
	/**
	 * Disjunction & Initial Call:
	 * Expression ::= Expression binop Expression
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseExpression() throws ParsingException, ScanningException {
		
		Expression e = parseCExpression();
		while(peek(1).spelling.equals("||")) {
			Token opToken = accept(Token.TYPE.BINOP);
			Operator o = new Operator(opToken, opToken.posn);
			e = new BinaryExpr(o, e, parseCExpression(), e.posn);
		}
		
		return e;
	}
	
	/**
	 * Conjunction
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseCExpression() throws ParsingException, ScanningException {

		Expression e = parseEExpression();
		while(peek(1).spelling.equals("&&")) {
			Token opToken = accept(Token.TYPE.BINOP);
			Operator o = new Operator(opToken, opToken.posn);
			e = new BinaryExpr(o, e, parseEExpression(), e.posn);
		}
		
		return e;
	}
	
	/**
	 * Equality
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseEExpression() throws ParsingException, ScanningException {
		
		Expression e = parseRExpression();
		while(peek(1).spelling.equals("==") || peek(1).spelling.equals("!=")) {
			Token opToken = accept(Token.TYPE.BINOP);
			Operator o = new Operator(opToken, opToken.posn);
			e = new BinaryExpr(o, e, parseRExpression(), e.posn);
		}
		
		return e;
	}
	
	/**
	 * Relational
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseRExpression() throws ParsingException, ScanningException {
		
		Expression e = parseAExpression();
		while(peek(1).spelling.equals("<") || peek(1).spelling.equals("<=")
		   || peek(1).spelling.equals(">") || peek(1).spelling.equals(">=")) {
			Token opToken = accept(Token.TYPE.BINOP);
			Operator o = new Operator(opToken, opToken.posn);
			e = new BinaryExpr(o, e, parseAExpression(), e.posn);
		}
		
		return e;
	}
	
	/**
	 * Additive
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseAExpression() throws ParsingException, ScanningException {
		
		Expression e = parseMExpression();
		while(peek(1).spelling.equals("+") || peek(1).spelling.equals("-")) {
			Token opToken = accept(Token.TYPE.BINOP);
			Operator o = new Operator(opToken, opToken.posn);
			e = new BinaryExpr(o, e, parseMExpression(), e.posn);
		}
		
		return e;
	}
	
	/**
	 * Multiplicative
	 * @return
	 * @throws ScanningException
	 */
	private Expression parseMExpression() throws ParsingException, ScanningException {
		
		Expression e = parseSingleExpression();
		while(peek(1).spelling.equals("*") || peek(1).spelling.equals("/")) {
			Token opToken = accept(Token.TYPE.BINOP);
			Operator o = new Operator(opToken, opToken.posn);
			e = new BinaryExpr(o, e, parseSingleExpression(), e.posn);
		}
		
		return e;
	}
	
	/**
	 * Sees what the next token is, caching the result.
	 * @return
	 * @throws ScanningException
	 */
	private Token peek(int lookahead) throws ScanningException {
		
		// Cache tokens
		while(stream.size() < lookahead) {
			Token next = scanner.scan();
			stream.addLast(next);
		}
		
		return stream.get(lookahead - 1);
	}
	
	
	/**
	 * Consumes token or throws exception.
	 * @throws ScanningException
	 */
	private Token accept(Token.TYPE type) throws ParsingException, ScanningException {
		Token next = peek(1);
		if(next.type == type) stream.poll();
		else throw new ParsingException(next);
		
		return next;
	}
}
