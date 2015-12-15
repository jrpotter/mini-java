package miniJava.SyntacticAnalyzer;

import java.io.IOException;
import java.util.LinkedList;

import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;

/**
 * Stratified Grammar (original grammar in PA1):
 * 
 * Program ::= (ClassDeclaration)* eot
 *
 * ClassDeclaration ::= class id { (Declarators id (; | MethodDeclaration))* }
 *
 * MethodDeclaration ::= (ParameterList?) { Statement* (return Expression ;)? }
 *
 * Declarators ::= (public | private)? static? Type
 *
 * Type ::= boolean | void | int ([])? | id ([])?
 *
 * ParameterList ::= Type id (, Type id)*
 *
 * ArgumentList ::= Expression (, Expression)*
 *
 * Reference ::= BaseRef (. id ([ Expression])?)*
 *
 * BaseRef ::= this | id ([ Expression])?
 *
 * Statement ::= {Statement*} | Type id = Expression; | Reference = Expression;
 * | Reference ( ArgumentList? ); | if (Expression) Statement (else Statement)?
 * | while (Expression) Statement
 *
 * Expression ::= num | true | false | ( Expression ) | new (id() | int [
 * Expression ] | id [ Expression ] ) | Reference ((ArgumentList?))? |
 * DExpression
 *
 * DExpression ::= CExpression (|| CExpression)*
 * 
 * CExpression ::= EExpression (&& EExpression)*
 * 
 * EExpression ::= RExpression ((==|!=) RExpression)*
 * 
 * RExpression ::= AExpression ((<=|<|>|>=) AExpression)*
 * 
 * AExpression ::= MExpression ((+|-) MExpression)*
 * 
 * MExpression ::= Expression ((*|/) Expression)*
 *
 */
public class Parser {

    private Scanner scanner;
    private LinkedList<Token> stream;

    /**
     * 
     * @param scanner
     */
    public Parser(Scanner scanner) {
        this.scanner = scanner;
        this.stream = new LinkedList<Token>();
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // Package
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    public Package parse() throws IOException {

        ClassDeclList decls = new ClassDeclList();
        while (peek(1).type == Token.TYPE.CLASS) {
            decls.add(parseClassDeclaration());
        }

        accept(Token.TYPE.EOT);
        return new Package(decls, new SourcePosition(0, 0));
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // DECLARATIONS
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    public ClassDecl parseClassDeclaration() throws IOException {

        // Class Header
        Token classToken = accept(Token.TYPE.CLASS);
        String className = accept(Token.TYPE.ID).spelling;

        // Class Body
        accept(Token.TYPE.LBRACKET);

        FieldDeclList fdl = new FieldDeclList();
        MethodDeclList mdl = new MethodDeclList();

        // Haven't reached end of body
        while (peek(1).type != Token.TYPE.RBRACKET) {
            Declarators d = parseDeclarators();
            String memberName = accept(Token.TYPE.ID).spelling;

            // Field Declaration
            FieldDecl fd = new FieldDecl(d, memberName);
            if (peek(1).type == Token.TYPE.SEMICOLON) {
                accept(Token.TYPE.SEMICOLON);
                fdl.add(fd);
            }

            // Method Declaration
            else {
                MethodDecl md = parseMethodDeclaration(fd);
                mdl.add(md);
            }
        }

        accept(Token.TYPE.RBRACKET);

        // Build Class
        ClassDecl decl = new ClassDecl(className, fdl, mdl, classToken.posn);
        Identifier ident = new Identifier(className, classToken.posn);
        decl.type = new ClassType(ident, classToken.posn);

        return decl;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Declarators parseDeclarators() throws IOException {

        boolean isStatic = false;
        boolean isPrivate = false;
        SourcePosition start = peek(1).posn;

        // Visibility
        if (peek(1).type == Token.TYPE.PUBLIC) {
            accept(Token.TYPE.PUBLIC);
        } else if (peek(1).type == Token.TYPE.PRIVATE) {
            isPrivate = true;
            accept(Token.TYPE.PRIVATE);
        }

        // Static
        if (peek(1).type == Token.TYPE.STATIC) {
            isStatic = true;
            accept(Token.TYPE.STATIC);
        }

        return new Declarators(isPrivate, isStatic, parseType(), start);
    }

    /**
     * 
     * @param f
     * @return
     * @throws IOException
     */
    private MethodDecl parseMethodDeclaration(FieldDecl f) throws IOException {

        Expression returnExpr = null;
        StatementList stl = new StatementList();

        // Parameters
        accept(Token.TYPE.LPAREN);

        ParameterDeclList pdl = new ParameterDeclList();
        if (peek(1).type != Token.TYPE.RPAREN) {
            pdl = parseParameterList();
        }

        accept(Token.TYPE.RPAREN);

        // Method Body
        accept(Token.TYPE.LBRACKET);

        while (peek(1).type != Token.TYPE.RBRACKET) {
            if (peek(1).type == Token.TYPE.RETURN) {
                accept(Token.TYPE.RETURN);
                returnExpr = parseExpression();
                accept(Token.TYPE.SEMICOLON);
                break;
            }

            stl.add(parseStatement());
        }

        accept(Token.TYPE.RBRACKET);

        return new MethodDecl(f, pdl, stl, returnExpr, f.posn);
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // TYPES
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    private Type parseType() throws IOException {

        switch (peek(1).type) {

        case VOID: {
            Token next = accept(Token.TYPE.VOID);
            return new BaseType(TypeKind.VOID, next.posn);
        }

        case BOOLEAN: {
            Token next = accept(Token.TYPE.BOOLEAN);
            return new BaseType(TypeKind.BOOLEAN, next.posn);
        }

        case INT: {
            Token next = accept(Token.TYPE.INT);
            BaseType bt = new BaseType(TypeKind.INT, next.posn);

            if (peek(1).type == Token.TYPE.LSQUARE) {
                accept(Token.TYPE.LSQUARE);
                accept(Token.TYPE.RSQUARE);
                return new ArrayType(bt, next.posn);
            }

            return bt;
        }

        case ID: {
            Token next = accept(Token.TYPE.ID);
            Identifier ident = new Identifier(next.spelling, next.posn);
            ClassType ct = new ClassType(ident, ident.posn);

            if (peek(1).type == Token.TYPE.LSQUARE) {
                accept(Token.TYPE.LSQUARE);
                accept(Token.TYPE.RSQUARE);
                return new ArrayType(ct, ident.posn);
            }

            return ct;
        }

        default: {
            Token next = peek(1);
            throw new ParsingException(next.posn);
        }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // Arguments
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    private ParameterDeclList parseParameterList() throws IOException {

        ParameterDeclList decls = new ParameterDeclList();

        // First Parameter
        Type fpt = parseType();
        Token next = accept(Token.TYPE.ID);
        decls.add(new ParameterDecl(fpt, next.spelling, fpt.posn));

        // Remainder of List
        while (peek(1).type == Token.TYPE.COMMA) {
            accept(Token.TYPE.COMMA);
            Type type = parseType();
            Token id = accept(Token.TYPE.ID);
            decls.add(new ParameterDecl(type, id.spelling, type.posn));
        }

        return decls;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private ExprList parseArgumentList() throws IOException {

        ExprList el = new ExprList();
        el.add(parseExpression());

        // Rest of argument list
        while (peek(1).type == Token.TYPE.COMMA) {
            accept(Token.TYPE.COMMA);
            el.add(parseExpression());
        }

        return el;
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // REFERENCES
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    private Reference parseReference() throws IOException {

        Reference ref = parseBaseRef();

        while (peek(1).type == Token.TYPE.PERIOD) {
            accept(Token.TYPE.PERIOD);
            Token next = accept(Token.TYPE.ID);
            Identifier ident = new Identifier(next.spelling, next.posn);
            ref = new QualifiedRef(ref, ident, next.posn);

            // Must be indexed
            if (peek(1).type == Token.TYPE.LSQUARE) {
                accept(Token.TYPE.LSQUARE);
                Expression expr = parseExpression();
                accept(Token.TYPE.RSQUARE);
                ref = new IndexedRef(ref, expr, next.posn);
            }
        }

        return ref;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Reference parseBaseRef() throws IOException {

        if (peek(1).type == Token.TYPE.THIS) {
            Token next = accept(Token.TYPE.THIS);
            return new ThisRef(next.posn);
        }

        Token next = accept(Token.TYPE.ID);
        Identifier ident = new Identifier(next.spelling, next.posn);
        IdRef ref = new IdRef(ident, ident.posn);

        // Must be indexed
        if (peek(1).type == Token.TYPE.LSQUARE) {
            accept(Token.TYPE.LSQUARE);
            Expression expr = parseExpression();
            accept(Token.TYPE.RSQUARE);
            return new IndexedRef(ref, expr, next.posn);
        }

        return ref;
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // STATEMENTS
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @return
     * @throws IOException
     */
    private Statement parseBlockStatement() throws IOException {

        StatementList stl = new StatementList();
        Token left = accept(Token.TYPE.LBRACKET);

        while (peek(1).type != Token.TYPE.RBRACKET) {
            stl.add(parseStatement());
        }

        accept(Token.TYPE.RBRACKET);

        return new BlockStmt(stl, left.posn);
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Statement parseIfStatement() throws IOException {

        Token next = accept(Token.TYPE.IF);

        // Conditional
        accept(Token.TYPE.LPAREN);
        Expression expr = parseExpression();
        accept(Token.TYPE.RPAREN);

        // Body
        Statement s1 = parseStatement();

        // Else Statement
        if (peek(1).type == Token.TYPE.ELSE) {
            accept(Token.TYPE.ELSE);
            Statement s2 = parseStatement();
            return new IfStmt(expr, s1, s2, next.posn);
        }

        return new IfStmt(expr, s1, next.posn);
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Statement parseWhileStatement() throws IOException {

        Token next = accept(Token.TYPE.WHILE);

        // Conditional
        accept(Token.TYPE.LPAREN);
        Expression e = parseExpression();
        accept(Token.TYPE.RPAREN);

        // Body
        Statement s = parseStatement();

        return new WhileStmt(e, s, next.posn);
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Statement parseVarDeclStatement() throws IOException {

        Type type = parseType();
        String name = accept(Token.TYPE.ID).spelling;
        VarDecl v = new VarDecl(type, name, type.posn);

        accept(Token.TYPE.EQUALS);
        Expression expr = parseExpression();
        accept(Token.TYPE.SEMICOLON);

        return new VarDeclStmt(v, expr, type.posn);
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Statement parseReferenceStatement() throws IOException {

        Statement stmt = null;
        Reference ref = parseReference();

        // Reference ( ArgumentList? ) ;
        if (peek(1).type == Token.TYPE.LPAREN) {
            ExprList el = new ExprList();
            accept(Token.TYPE.LPAREN);
            if (peek(1).type != Token.TYPE.RPAREN) {
                el = parseArgumentList();
            }
            accept(Token.TYPE.RPAREN);
            stmt = new CallStmt(ref, el, ref.posn);
        }

        // Reference = Expression;
        else {
            accept(Token.TYPE.EQUALS);
            Expression expr = parseExpression();
            stmt = new AssignStmt(ref, expr, ref.posn);
        }

        accept(Token.TYPE.SEMICOLON);
        return stmt;
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    private Statement parseStatement() throws IOException {

        switch (peek(1).type) {

        // { Statement* }
        case LBRACKET: {
            return parseBlockStatement();
        }

            // if (Expression) Statement (else Statement)?
        case IF: {
            return parseIfStatement();
        }

            // while (Expression) Statement
        case WHILE: {
            return parseWhileStatement();
        }

            // Type id = Expression;
        case BOOLEAN:
        case VOID:
        case INT:
        case ID: {

            Token.TYPE fst = peek(1).type;
            Token.TYPE snd = peek(2).type;
            Token.TYPE thd = peek(3).type;

            if (fst != Token.TYPE.ID || snd == Token.TYPE.ID) {
                return parseVarDeclStatement();
            } else if (snd == Token.TYPE.LSQUARE && thd == Token.TYPE.RSQUARE) {
                return parseVarDeclStatement();
            }

            /* Fall Through */
        }

        default: {
            return parseReferenceStatement();
        }
        }
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // EXPRESSIONS
    //
    // /////////////////////////////////////////////////////////////////////////////

    private Expression parseSingleExpression() throws IOException {

        switch (peek(1).type) {

        case NUM: {
            Token next = accept(Token.TYPE.NUM);
            IntLiteral il = new IntLiteral(next.spelling, next.posn);
            return new LiteralExpr(il, next.posn);
        }

        case TRUE:
        case FALSE: {
            Token next = accept(peek(1).type);
            BooleanLiteral bl = new BooleanLiteral(next.spelling, next.posn);
            return new LiteralExpr(bl, next.posn);
        }

            // ( Expression )
        case LPAREN: {
            accept(Token.TYPE.LPAREN);
            Expression expr = parseExpression();
            accept(Token.TYPE.RPAREN);

            return expr;
        }

            // unop Expression
        case UNOP:
        case BINOP: {
            if (peek(1).spelling.equals("!") || peek(1).spelling.equals("-")) {
                Token next = accept(peek(1).type);
                Operator o = new Operator(next, next.posn);
                Expression expr = parseSingleExpression();
                return new UnaryExpr(o, expr, next.posn);
            } else {
                throw new ParsingException(peek(1).posn);
            }
        }

            // new ( int [ Expression ] | id ( ) | id [ Expression ] )
        case NEW: {

            Token next = accept(Token.TYPE.NEW);

            if (peek(1).type == Token.TYPE.INT) {
                accept(Token.TYPE.INT);
                accept(Token.TYPE.LSQUARE);
                Expression expr = parseExpression();
                accept(Token.TYPE.RSQUARE);

                BaseType b = new BaseType(TypeKind.INT, next.posn);
                return new NewArrayExpr(b, expr, next.posn);
            }

            else {
                Token id = accept(Token.TYPE.ID);
                Identifier ident = new Identifier(id.spelling, id.posn);
                ClassType ct = new ClassType(ident, id.posn);

                if (peek(1).type == Token.TYPE.LPAREN) {
                    accept(Token.TYPE.LPAREN);
                    accept(Token.TYPE.RPAREN);
                    return new NewObjectExpr(ct, next.posn);
                } else {
                    accept(Token.TYPE.LSQUARE);
                    Expression expr = parseExpression();
                    accept(Token.TYPE.RSQUARE);
                    return new NewArrayExpr(ct, expr, next.posn);
                }
            }
        }

            // Reference ((ArgumentList?))?
        case THIS:
        case ID: {

            Reference ref = parseReference();

            if (peek(1).type == Token.TYPE.LPAREN) {
                accept(Token.TYPE.LPAREN);
                ExprList el = new ExprList();
                if (peek(1).type != Token.TYPE.RPAREN) {
                    el = parseArgumentList();
                }
                accept(Token.TYPE.RPAREN);
                return new CallExpr(ref, el, ref.posn);
            } else {
                return new RefExpr(ref, ref.posn);
            }
        }

        default:
            throw new ParsingException(peek(1).posn);
        }
    }

    /**
     * Disjunctive
     * 
     * @return
     * @throws IOException
     */
    private Expression parseExpression() throws IOException {

        Expression expr = parseCExpression();

        while (peek(1).spelling.equals("||")) {
            Token next = accept(Token.TYPE.BINOP);
            Operator o = new Operator(next, next.posn);
            expr = new BinaryExpr(o, expr, parseCExpression(), expr.posn);
        }

        return expr;
    }

    /**
     * Conjunctive
     * 
     * @return
     * @throws IOException
     */
    private Expression parseCExpression() throws IOException {

        Expression expr = parseEExpression();

        while (peek(1).spelling.equals("&&")) {
            Token next = accept(Token.TYPE.BINOP);
            Operator o = new Operator(next, next.posn);
            expr = new BinaryExpr(o, expr, parseEExpression(), expr.posn);
        }

        return expr;
    }

    /**
     * Equality
     * 
     * @return
     * @throws IOException
     */
    private Expression parseEExpression() throws IOException {

        Expression expr = parseRExpression();

        while (peek(1).spelling.equals("==") || peek(1).spelling.equals("!=")) {
            Token next = accept(Token.TYPE.BINOP);
            Operator o = new Operator(next, next.posn);
            expr = new BinaryExpr(o, expr, parseRExpression(), expr.posn);
        }

        return expr;
    }

    /**
     * Relational
     * 
     * @return
     * @throws IOException
     */
    private Expression parseRExpression() throws IOException {

        Expression expr = parseAExpression();

        while (peek(1).spelling.equals("<") || peek(1).spelling.equals("<=") || peek(1).spelling.equals(">")
                || peek(1).spelling.equals(">=")) {
            Token next = accept(Token.TYPE.BINOP);
            Operator o = new Operator(next, next.posn);
            expr = new BinaryExpr(o, expr, parseAExpression(), expr.posn);
        }

        return expr;
    }

    /**
     * Additive
     * 
     * @return
     * @throws IOException
     */
    private Expression parseAExpression() throws IOException {

        Expression expr = parseMExpression();

        while (peek(1).spelling.equals("+") || peek(1).spelling.equals("-")) {
            Token next = accept(Token.TYPE.BINOP);
            Operator o = new Operator(next, next.posn);
            expr = new BinaryExpr(o, expr, parseMExpression(), expr.posn);
        }

        return expr;
    }

    /**
     * Multiplicative
     * 
     * @return
     * @throws IOException
     */
    private Expression parseMExpression() throws IOException {

        Expression expr = parseSingleExpression();

        while (peek(1).spelling.equals("*") || peek(1).spelling.equals("/")) {
            Token next = accept(Token.TYPE.BINOP);
            Operator o = new Operator(next, next.posn);
            expr = new BinaryExpr(o, expr, parseSingleExpression(), expr.posn);
        }

        return expr;
    }

    // /////////////////////////////////////////////////////////////////////////////
    //
    // Convenience Methods
    //
    // /////////////////////////////////////////////////////////////////////////////

    /**
     * 
     * @param lookahead
     * @return
     * @throws IOException
     */
    private Token peek(int lookahead) throws IOException {

        // Cache tokens
        while (stream.size() < lookahead) {
            Token next = scanner.scan();
            stream.addLast(next);
        }

        return stream.get(lookahead - 1);
    }

    /**
     * 
     * @param type
     * @return
     * @throws IOException
     */
    private Token accept(Token.TYPE type) throws IOException {

        Token next = peek(1);
        if (next.type == type)
            stream.poll();
        else
            throw new ParsingException(next.posn);

        return next;
    }
}
