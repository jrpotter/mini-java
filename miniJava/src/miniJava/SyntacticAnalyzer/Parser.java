package miniJava.SyntacticAnalyzer;

import java.io.*;
import java.util.LinkedList;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

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
	 * @throws IOException
	 */
	public Package parse() throws IOException {
		ClassDeclList decls = new ClassDeclList();
		while(peek(1).type == Token.TYPE.CLASS) {
			decls.add(parseClassDeclaration());
		}
		
		accept(Token.TYPE.EOT);
		return new Package(decls, null);
	}
			
	/**
	 * ClassDeclaration ::=   
	 *     class id  {       
	 *	       (Declarators id (; | MethodDeclaration))*   
	 *     }
	 * @return
	 * @throws IOException
	 */
	private ClassDecl parseClassDeclaration() throws IOException {
		
		// Class Header
		accept(Token.TYPE.CLASS);
		String cn = accept(Token.TYPE.ID).spelling;
		accept(Token.TYPE.LBRACKET);
		
		// Setup
		FieldDeclList fdl = new FieldDeclList();
		MethodDeclList mdl = new MethodDeclList();
		
		// Class Body
		while(peek(1).type != Token.TYPE.RBRACKET) {
			Declarators d = parseDeclarators();
			String name = accept(Token.TYPE.ID).spelling;
			FieldDecl f = new FieldDecl(d.isPrivate, d.isStatic, d.mt, name, null);
			
			// Field Declarations
			if(peek(1).type == Token.TYPE.SEMICOLON) {
				accept(Token.TYPE.SEMICOLON);
				fdl.add(f);
			} 
			
			// Method Declarations
			else mdl.add(parseMethodDeclaration(f));
		}
		
		accept(Token.TYPE.RBRACKET);
		return new ClassDecl(cn, fdl, mdl, null);
	}
	
	/**
	 * Declarators ::= (public | private)? static? Type
	 * @return
	 * @throws IOException
	 */
	private Declarators parseDeclarators() throws IOException {
		
		// Visibility
		boolean isPrivate = false;
		if(peek(1).type == Token.TYPE.PUBLIC) {
			accept(Token.TYPE.PUBLIC);
		} else if(peek(1).type == Token.TYPE.PRIVATE) {
			isPrivate = true;
			accept(Token.TYPE.PRIVATE); 
		}
		
		// Class Methods
		boolean isStatic = false;
		if(peek(1).type == Token.TYPE.STATIC) {
			isStatic = true;
			accept(Token.TYPE.STATIC);
		}
		
		Type t = parseType();
		return new Declarators(isPrivate, isStatic, t);
	}

	/**
	 * * MethodDeclaration ::=
	 *     (ParameterList?) {
	 *	       Statement* (return Expression ;)?
	 *	   }
	 * @param f describes the declaratory aspect of the method
	 * @return
	 * @throws IOException
	 */
	private MethodDecl parseMethodDeclaration(FieldDecl f) throws IOException {
		
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
		return new MethodDecl(f, pdl, stl, re, null);
	}
	
	/**
	 * Type ::= boolean | void | int ([])? | id ([])?
	 * @return
	 * @throws IOException
	 */
	private Type parseType() throws IOException {
		
		switch(peek(1).type) {
		
			case BOOLEAN:
				accept(Token.TYPE.BOOLEAN);
				return new BaseType(TypeKind.BOOLEAN, null);
				
			case VOID:
				accept(Token.TYPE.VOID);
				return new BaseType(TypeKind.VOID, null);
				
			case INT: {
				accept(Token.TYPE.INT);
				BaseType b = new BaseType(TypeKind.INT, null);
				
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					accept(Token.TYPE.RSQUARE);
					return new ArrayType(b, null);
				}
				
				return b;
			}
				
			case ID: {
				String cn = accept(peek(1).type).spelling;
				Identifier i = new Identifier(cn, null);
				ClassType c = new ClassType(i, null);
				
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					accept(Token.TYPE.RSQUARE);
					return new ArrayType(c, null);
				}
				
				return c;
			}
				
			default:
				throw new IOException();
		}
	}
	
	/**
	 * ParameterList ::= Type id (, Type id)*
	 * @return
	 * @throws IOException
	 */
	private ParameterDeclList parseParameterList() throws IOException {
		
		ParameterDeclList decls = new ParameterDeclList(); 
		
		// First Parameter
		Type t = parseType();
		String name = accept(Token.TYPE.ID).spelling;
		decls.add(new ParameterDecl(t, name, null));
		
		// Remainder of List
		while(peek(1).type == Token.TYPE.COMMA) {
			accept(Token.TYPE.COMMA);
			t = parseType();
			name = accept(Token.TYPE.ID).spelling;
			decls.add(new ParameterDecl(t, name, null));
		}
		
		return decls;
	}
	
	/**
	 * ArgumentList ::= Expression (, Expression)*
	 * @return
	 * @throws IOException
	 */
	private ExprList parseArgumentList() throws IOException {
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
	 * @throws IOException
	 */
	private Reference parseReference() throws IOException {
		Reference r = parseBaseRef();
		while(peek(1).type == Token.TYPE.PERIOD) {
			accept(Token.TYPE.PERIOD);
			String name = accept(Token.TYPE.ID).spelling;
			Identifier id = new Identifier(name, null);
			r = new QualifiedRef(r, id, null);
			
			if(peek(1).type == Token.TYPE.LSQUARE) {
				accept(Token.TYPE.LSQUARE);
				Expression e = parseExpression();
				accept(Token.TYPE.RSQUARE);
				r = new IndexedRef(r, e, null);
			}
		}		
		
		return r;
	}

	/**
	 * BaseRef ::= this | id ([ Expression])?
	 * @return
	 * @throws IOException
	 */
	private Reference parseBaseRef() throws IOException {
		
		switch(peek(1).type) {
			case THIS:
				accept(Token.TYPE.THIS);
				return new ThisRef(null);
				
			// id ([ Expression])?
			default: {
				String id = accept(Token.TYPE.ID).spelling;
				Identifier i = new Identifier(id, null);
				IdRef r = new IdRef(i, null);
				
				if(peek(1).type == Token.TYPE.LSQUARE) {
					accept(Token.TYPE.LSQUARE);
					Expression e = parseExpression();
					accept(Token.TYPE.RSQUARE);
					return new IndexedRef(r, e, null);
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
	 * @throws IOException
	 */
	private Statement parseStatement() throws IOException {
		
		switch(peek(1).type) {
		
			// { Statement* }
			case LBRACKET: {
				accept(Token.TYPE.LBRACKET);
				StatementList stl = new StatementList();
				while(peek(1).type != Token.TYPE.RBRACKET) {
					stl.add(parseStatement());
				}
				accept(Token.TYPE.RBRACKET);
				
				return new BlockStmt(stl, null);
			}
				
			// if (Expression) Statement (else Statement)?
			case IF: {
				accept(Token.TYPE.IF);
				accept(Token.TYPE.LPAREN);
				Expression e = parseExpression();
				accept(Token.TYPE.RPAREN);
				Statement s1 = parseStatement();
				if(peek(1).type == Token.TYPE.ELSE) {
					accept(Token.TYPE.ELSE);
					Statement s2 = parseStatement();
					return new IfStmt(e, s1, s2, null);
				}
				
				return new IfStmt(e, s1, null);
			}
				
			// while (Expression) Statement
			case WHILE: {
				accept(Token.TYPE.WHILE);
				accept(Token.TYPE.LPAREN);
				Expression e = parseExpression();
				accept(Token.TYPE.RPAREN);
				Statement s = parseStatement();
				
				return new WhileStmt(e, s, null);
			}
				
			// Type id = Expression ;
			case BOOLEAN: case VOID: case INT: case ID: {
				
				// Must be a type though there is a possibility of a reference
				if(peek(1).type != Token.TYPE.ID || peek(2).type == Token.TYPE.ID
				||(peek(2).type == Token.TYPE.LSQUARE && peek(3).type == Token.TYPE.RSQUARE)) {
					Type t = parseType();
					String name = accept(Token.TYPE.ID).spelling;
					VarDecl v = new VarDecl(t, name, null);
					
					accept(Token.TYPE.EQUALS);
					Expression e = parseExpression();
					accept(Token.TYPE.SEMICOLON);
					
					return new VarDeclStmt(v, e, null);
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
					s = new CallStmt(r, e, null);
				} 
				
				// Reference = Expression ;
				else {
					accept(Token.TYPE.EQUALS);
					Expression e = parseExpression();
					s = new AssignStmt(r, e, null);
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
	 *   | Expression binop Expression
	 *   | ( Expression )
	 *   | num | true | false
	 *   | new (id() | int [ Expression ] | id [ Expression ] )
	 * @return
	 * @throws IOException
	 */
	private Expression parseExpression() throws IOException {
		
		Expression e = null;
		switch(peek(1).type) {
		
			// num
			case NUM: {
				String number = accept(Token.TYPE.NUM).spelling;
				IntLiteral i = new IntLiteral(number, null);
				e = new LiteralExpr(i, null);
				break;
			}
			
			// true | false
			case TRUE:
			case FALSE: {
				String bool = accept(peek(1).type).spelling;
				BooleanLiteral b = new BooleanLiteral(bool, null);
				e = new LiteralExpr(b, null);
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
					Operator o = new Operator(accept(peek(1).type), null);
					e = new UnaryExpr(o, parseExpression(), null);
				} 
				else throw new IOException();
				break;
			}
				
			// new ( int [ Expression ]  | id ( ) | id [ Expression ] )
			case NEW: {
				accept(Token.TYPE.NEW);
				
				if(peek(1).type == Token.TYPE.INT) {
					accept(Token.TYPE.INT);
					accept(Token.TYPE.LSQUARE);
					Expression e2 = parseExpression();
					accept(Token.TYPE.RSQUARE);
					
					BaseType b = new BaseType(TypeKind.INT, null);
					e = new NewArrayExpr(b, e2, null);
				}
				
				else {
					String cn = accept(Token.TYPE.ID).spelling;
					Identifier i = new Identifier(cn, null);
					ClassType c = new ClassType(i, null);
					
					if(peek(1).type == Token.TYPE.LPAREN){
						accept(Token.TYPE.LPAREN);
						accept(Token.TYPE.RPAREN);
						e = new NewObjectExpr(c, null);
					} else {
						accept(Token.TYPE.LSQUARE);
						Expression e2 = parseExpression();
						accept(Token.TYPE.RSQUARE);
						e = new NewArrayExpr(c, e2, null);
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
					e = new CallExpr(r, el, null);
				} else {
					e = new RefExpr(r, null);
				}
				
				break;
			}
				
			default:
				throw new IOException();
		}
		
		// Expression binop Expression
		if(peek(1).type == Token.TYPE.BINOP) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseDExpression(), null);
		}
		
		return e;
	}
	
	/**
	 * Disjunction
	 * @throws IOException
	 */
	private Expression parseDExpression() throws IOException {
		Expression e = parseCExpression();
		while(peek(1).spelling.equals("||")) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseCExpression(), null);
		}
		
		return e;
	}
	
	/**
	 * Conjunction
	 * @throws IOException
	 */
	private Expression parseCExpression() throws IOException {
		Expression e = parseEExpression();
		while(peek(1).spelling.equals("&&")) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseEExpression(), null);
		}
		
		return e;
	}
	
	/**
	 * Equality
	 * @throws IOException
	 */
	private Expression parseEExpression() throws IOException {
		Expression e = parseRExpression();
		while(peek(1).spelling.equals("==") || peek(1).spelling.equals("!=")) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseRExpression(), null);
		}
		
		return e;
	}
	
	/**
	 * Relational
	 * @throws IOException
	 */
	private Expression parseRExpression() throws IOException {
		Expression e = parseAExpression();
		while(peek(1).spelling.equals("<=") || peek(1).spelling.equals(">=")
		   || peek(1).spelling.equals("<") || peek(1).spelling.equals(">")) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseAExpression(), null);
		}
		
		return e;
	}

	/**
	 * Additive
	 * @throws IOException
	 */
	private Expression parseAExpression() throws IOException {
		Expression e = parseMExpression();
		while(peek(1).spelling.equals("+") || peek(1).spelling.equals("-")) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseMExpression(), null);
		}
		
		return e;
	}

	/**
	 * Multiplicative
	 * @throws IOException
	 */
	private Expression parseMExpression() throws IOException {
		Expression e = parseExpression();
		while(peek(1).spelling.equals("*") || peek(1).spelling.equals("/")) {
			Operator o = new Operator(accept(Token.TYPE.BINOP), null);
			e = new BinaryExpr(o, e, parseExpression(), null);
		}
		
		return e;
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
	private Token accept(Token.TYPE type) throws IOException {
		Token next = peek(1);
		if(next.type == type) stream.poll();
		else throw new IOException();
		
		return next;
	}
}
