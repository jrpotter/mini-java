package miniJava.ContextualAnalyzer;

import java.io.IOException;
import java.util.ArrayList;

import miniJava.Compiler;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.SyntacticAnalyzer.Parser;
import miniJava.SyntacticAnalyzer.Scanner;

public class Analyzer implements Visitor<IdTable, Type> {
	
	public Reporter reporter = new Reporter();
	
	// Required Methods
	public static MethodDecl main;
	public static MethodDecl println;
	
	// Predefined
	private static ArrayList<String> predefined;
	static 
	{
		predefined = new ArrayList<String>();
		predefined.add("class String { }");
		predefined.add("class _PrintStream { public void println(int n){} }");
		predefined.add("class System { public static _PrintStream out; }");
	}
	
	// Keep Track Of Declarations
	private IdTable top = null;
	private VarDecl currentVarDecl = null;
	private ClassDecl currentClassDecl = null;
	private MethodDecl currentMethodDecl = null;
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// PACKAGE
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Type visitPackage(Package prog, IdTable table) {
		
		top = table;
		
		// Begin predefinitions
		try {
			for(String pd : predefined) {
				Scanner scanner = new Scanner(pd, true);
				Parser parser = new Parser(scanner);
				ClassDecl cd = parser.parseClassDeclaration();
				
				addDeclarations(cd, table);
				cd.visit(this, table);
			}
		} catch(IOException e) {
			Reporter.emit("System Predefinitions Error!");
			System.exit(Compiler.rc);
		}
		
		// Add all second level declarations
		for(ClassDecl cd : prog.classDeclList) {
			addDeclarations(cd, table);
		}
		
		// Begin traversal
		for(ClassDecl cd : prog.classDeclList) {
			cd.visit(this, table);
		}
		
		// Check a main exists
		if(main == null) {
			Reporter.emit("Main method undeclared.");
		}

		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// DECLARATIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Type visitClassDecl(ClassDecl cd, IdTable table) {
		
		currentClassDecl = cd;

		for(FieldDecl fd : cd.fieldDeclList) {
			fd.visit(this, cd.table);
		}
		
		for(MethodDecl md : cd.methodDeclList) {
			md.visit(this, cd.table);
		}
		
		return null;
	}

	@Override
	public Type visitFieldDecl(FieldDecl fd, IdTable table) {
		
		fd.type.visit(this, table);
		if(fd.type.typeKind == TypeKind.VOID) {
			Reporter.emit("Field " + fd.name + " at " + fd.posn + " cannot have type 'void'");
		}
		
		return null;
	}

	@Override
	public Type visitMethodDecl(MethodDecl md, IdTable table) {
		
		currentMethodDecl = md;
		
		// Save println method
		if(println == null && md.name.equals("println")) {
			println = md;
		}
		
		// Save main method
		if(isMain(md)) {
			if(main != null) {
				Reporter.emit("Main at " + md.posn + " previously defined at " + main.posn);
			} else {
				main = md;
			}
		}
		
		// Continue Traversal
		md.type.visit(this, table);
		
		for(ParameterDecl pd : md.parameterDeclList) {
			pd.visit(this, md.table);
		}
		
		for(Statement s : md.statementList) {
			s.visit(this, md.table);
		}
		
		// Check return status
		if(md.type.typeKind != TypeKind.VOID && md.returnExp == null) {
			Reporter.emit("Method " + md.name + " at " + md.posn + " must have a return expression");
		} else if(md.type.typeKind == TypeKind.VOID && md.returnExp != null) {
			Reporter.emit("Method " + md.name + " at " + md.posn + " cannot have a return expression");
		}
		
		if(md.returnExp != null) {
			Type returnType = md.returnExp.visit(this, md.table);
			if(!match(returnType, md.type)) {
				Reporter.emit("Expected " + md.type + " but got " + returnType + " at " + md.returnExp.posn);
			}
		}

		return null;
	}

	@Override
	public Type visitParameterDecl(ParameterDecl pd, IdTable table) {
		
		table.add(pd);	
		pd.type.visit(this, table);
		if(pd.type.typeKind == TypeKind.VOID) {
			Reporter.emit("Parameter " + pd.name + " at " + pd.posn + " cannot have type 'void'");
		}
		
		return null;
	}

	@Override
	public Type visitVarDecl(VarDecl decl, IdTable table) {
		
		table.add(decl);
		decl.type.visit(this, table);
		if(decl.type.typeKind == TypeKind.VOID) {
			Reporter.emit("Variable " + decl.name + " at " + decl.posn + " cannot have type 'void'");
		}
		
		return null;
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// TYPES
	//
	// /////////////////////////////////////////////////////////////////////////////
	
	@Override
	public Type visitBaseType(BaseType type, IdTable table) {
		
		return type;
	}

	@Override
	public Type visitClassType(ClassType type, IdTable table) {
		
		type.className.visit(this, table);
		
		// Must check that the type is valid
		String cn = type.className.spelling;
		if(top.getDeclarationAtScope(cn) == null) {
			Reporter.emit("Class '" + cn + "' undeclared at " + type.posn);
		}
		
		return type;
	}

	@Override
	public Type visitArrayType(ArrayType type, IdTable table) {
		
		type.eltType.visit(this, table);
		
		return type;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// STATEMENTS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Type visitBlockStmt(BlockStmt stmt, IdTable table) {
		
		table.push();
		
		for(Statement s : stmt.sl) {
			s.visit(this, table);
		}
		
		table.pop();
		
		return null;
	}

	@Override
	public Type visitVardeclStmt(VarDeclStmt stmt, IdTable table) {
		
		stmt.varDecl.visit(this, table);
		
		currentVarDecl = stmt.varDecl;
		Type exprType = stmt.initExp.visit(this, table);
		currentVarDecl = null;
		
		// Types must match
		if(!match(stmt.varDecl.type, exprType)) {
			Reporter.emit("Expected " + stmt.varDecl.type + " but got " + exprType + " at " + stmt.initExp.posn);
		}
		
		return null;
	}

	@Override
	public Type visitAssignStmt(AssignStmt stmt, IdTable table) {
		
		Type refType = stmt.ref.visit(this, table);
		Type valType = stmt.val.visit(this, table);
		
		if(!match(refType, valType)) {
			Reporter.emit("Expected " + refType + " but got " + valType + " at " + stmt.posn);
		}
		
		// Should never assign 'this' to anything
		if(stmt.ref instanceof ThisRef) {
			Reporter.emit("Cannot assign 'this' to a value at " + stmt.posn);
		}
		
		// Should never assign a method a value
		if(stmt.ref.decl instanceof MethodDecl) {
			Reporter.emit("Cannot assign a method a value at " + stmt.posn);
		}
		
		// Cannot assign array length field
		if(stmt.ref instanceof QualifiedRef) {
			QualifiedRef ref = (QualifiedRef) stmt.ref;
			if(ref.ref.decl.type.typeKind == TypeKind.ARRAY && ref.id.spelling.equals("length")) {
				Reporter.emit("'length' field cannot be changed at " + stmt.posn);
			}
		}
		
		return null;
	}

	@Override
	public Type visitCallStmt(CallStmt stmt, IdTable table) {
		
		stmt.methodRef.visit(this, table);
		
		// Check valid parameter count
		MethodDecl md = (MethodDecl) stmt.methodRef.decl;
		if(md.parameterDeclList.size() != stmt.argList.size()) {
			Reporter.emit("Expected " + md.parameterDeclList.size() + " parameters at " + stmt.posn);
			return null;
		} 
		
		// Check parameter types match
		for (int i = 0; i < stmt.argList.size(); i++) {
			Expression e = stmt.argList.get(i);
			Type exprType = e.visit(this, table);
			ParameterDecl pd = md.parameterDeclList.get(i);

			if(!match(pd.type, exprType)) {
				Reporter.emit("Parameter " + pd.name + " is of type " + pd.type + " but got " + exprType + " at " + e.posn);
			}
		}

		return null;
	}

	@Override
	public Type visitIfStmt(IfStmt stmt, IdTable table) {
		
		// Conditional statement must have type boolean
		Type condType = stmt.cond.visit(this, table);
		if(condType.typeKind != TypeKind.BOOLEAN) {
			Reporter.emit("Expected boolean but got " + condType + " at " + condType.posn);
		}
		
		// A single variable declaration not allowed after conditional statement
		if(stmt.thenStmt instanceof VarDeclStmt) {
			Reporter.emit("Variable declaration not allowed solely in 'then' clause at " + stmt.thenStmt.posn);
			return null;
		}
		
		stmt.thenStmt.visit(this, table);
		
		// Check else statement
		if (stmt.elseStmt != null) {
			if (stmt.elseStmt instanceof VarDeclStmt) {
				Reporter.emit("Variable declaration not allowed solely in 'else' clause at " + stmt.elseStmt.posn);
				return null;
			} 
			
			stmt.elseStmt.visit(this, table);
		}

		return null;
	}

	@Override
	public Type visitWhileStmt(WhileStmt stmt, IdTable table) {
		
		// Conditional statement must have type boolean
		Type condType = stmt.cond.visit(this, table);
		if(condType.typeKind != TypeKind.BOOLEAN) {
			Reporter.emit("Expected boolean but got " + condType + " at " + condType.posn);
		}
		
		if(stmt.body instanceof VarDeclStmt) {
			Reporter.emit("Variable declaration not allowed solely in 'while' clause at " + stmt.body.posn);
			return null;
		}

		stmt.body.visit(this, table);
		
		return null;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// EXPRESSIONS
	//
	// /////////////////////////////////////////////////////////////////////////////

	@Override
	public Type visitUnaryExpr(UnaryExpr expr, IdTable table) {
		
		Type exprType = expr.expr.visit(this, table);
		Type opType = expr.operator.visit(this, table);
		if(!match(exprType, opType)) {
			String op = expr.operator.spelling;
			Reporter.emit("Operator " + op + " applies to " + opType + " but got " + exprType + " at " + expr.posn);
		}
		
		return opType;
	}

	@Override
	public Type visitBinaryExpr(BinaryExpr expr, IdTable table) {
		
		Type lType = expr.left.visit(this, table);
		Type rType = expr.right.visit(this, table);
		
		BaseType type = null;
		String op = expr.operator.spelling;
		switch(op) {
			
			// Equality Operators
			case "==":
			case "!=":
				if(!match(lType, rType)) {
					Reporter.emit("Left hand side must be of right hand side's type " + rType + " at " + expr.posn);
				}
				return new BaseType(TypeKind.BOOLEAN, expr.posn);
			
			// Boolean Operators
			case "&&":
			case "||":
				type = new BaseType(TypeKind.BOOLEAN, expr.posn);
				break;
			
			// Integer operators
			case ">":
			case "<":
			case "+":
			case "-":
			case "*":
			case "/":
			case "<=":
			case ">=":
				type = new BaseType(TypeKind.INT, expr.posn);
				break;
			
			default:
				return new BaseType(TypeKind.ERROR, expr.posn);
		}
		
		// Make sure types match
		if(!match(lType, type)) {
			Reporter.emit("Expected " + type + " but got " + lType + " at " + expr.posn);
		} 
		
		if(!match(rType, type)) {
			Reporter.emit("Expected " + type + " but got " + lType + " at " + expr.posn);
		}
		
		// Resulting type
		return expr.operator.visit(this, table);
	}

	@Override
	public Type visitRefExpr(RefExpr expr, IdTable table) {
		
		Type refType = expr.ref.visit(this, table);
		Declaration decl = table.getDeclaration(expr.ref.spelling);
		
		// Should never reference a class name
		if(decl instanceof ClassDecl) {
			Reporter.emit("Class declaration referenced at " + expr.posn);
		}

		// Should never reference a method name
		else if(decl instanceof MethodDecl) {
			Reporter.emit("Method declaration referenced at " + expr.posn);
		}
		
		// Cannot access instance members in static context
		if(!(expr.ref instanceof QualifiedRef) && decl instanceof FieldDecl) {
			FieldDecl fd = (FieldDecl) decl;
			if(currentMethodDecl.isStatic && !fd.isStatic) {
				Reporter.emit("Cannot access instance member in static context at " + expr.posn);
			}
		}

		return refType;
	}

	@Override
	public Type visitCallExpr(CallExpr expr, IdTable table) {
		
		Type refType = expr.functionRef.visit(this, table);
		
		// Check parameter count matches
		MethodDecl md = (MethodDecl) expr.functionRef.decl;
		if(md.parameterDeclList.size() != expr.argList.size()) {
			Reporter.emit("Expected " + md.parameterDeclList.size() + " parameters at " + expr.posn);
			return refType;
		}
		
		// Check parameter types match
		for (int i = 0; i < expr.argList.size(); i++) {
			Expression e = expr.argList.get(i);
			Type exprType = e.visit(this, table);
			ParameterDecl pd = md.parameterDeclList.get(i);

			if(!match(pd.type, exprType)) {
				Reporter.emit("Parameter " + pd.name + " is of type " + pd.type + " but got " + exprType + " at " + e.posn);
			}
		}
		
		return refType;
	}

	@Override
	public Type visitLiteralExpr(LiteralExpr expr, IdTable table) {
		
		return expr.literal.visit(this, table);
	}

	@Override
	public Type visitNewObjectExpr(NewObjectExpr expr, IdTable table) {
		
		return expr.classtype.visit(this, table);
	}

	@Override
	public Type visitNewArrayExpr(NewArrayExpr expr, IdTable table) {
		
		// Arrays only of classes and integers
		Type eltType = expr.eltType.visit(this, table);
		if(eltType.typeKind != TypeKind.INT && eltType.typeKind != TypeKind.CLASS) {
			Reporter.emit("Cannot create array of type " + eltType + " at " + expr.posn);
		}
		
		// Size must be an integer value
		Type sizeType = expr.sizeExpr.visit(this, table);
		if (sizeType.typeKind != TypeKind.INT) {
			BaseType type = new BaseType(TypeKind.INT, expr.sizeExpr.posn);
			Reporter.emit("Expected " + type + " but got " + sizeType + " at " + type.posn);
		}

		return new ArrayType(eltType, expr.posn);
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// REFERENCES
	//
	// /////////////////////////////////////////////////////////////////////////////
		
	@Override
	public Type visitQualifiedRef(QualifiedRef ref, IdTable table) {
		
		Type refType = ref.ref.visit(this, table);
		
		// Should not try and access a method in qualified manner
		if(ref.ref.decl instanceof MethodDecl) {
			Reporter.emit("Cannot use method " + ref.ref.decl.name + " in qualified manner at " + ref.ref.posn);
			return new BaseType(TypeKind.ERROR, ref.ref.posn);
		}
		
		// Array types can have a length field
		if(refType.typeKind == TypeKind.ARRAY) {
			if(!ref.id.spelling.equals("length")) {
				Reporter.emit(ref.id.spelling + " attribute not available with arrays at " + ref.id.posn);
				return new BaseType(TypeKind.ERROR, ref.id.posn);
			}
			
			return new BaseType(TypeKind.INT, ref.id.posn);
		}
		
		// Otherwise, should have a class type
		if(refType.typeKind != TypeKind.CLASS) {
			Reporter.emit("Qualified reference not of type class or array at " + ref.posn);
			return new BaseType(TypeKind.ERROR, ref.posn);
		}
		
		// Get member in question
		ClassType ct = (ClassType) refType;
		ClassDecl cd = (ClassDecl) top.getDeclarationAtScope(ct.className.spelling);
		if(cd == null) {
			Reporter.emit("Accessing non-existant class at " + ct.posn);
			return new BaseType(TypeKind.ERROR, ref.posn);
		}

		MemberDecl md = (MemberDecl) cd.table.getDeclarationAtScope(ref.id.spelling);
		if(md == null) {
			Reporter.emit("Accessing non-existant field at " + ref.id.posn);
			return new BaseType(TypeKind.ERROR, ref.posn);
		}
		
		// Check if static member
		Declaration decl = table.getDeclaration(ref.ref.spelling);
		if(decl instanceof ClassDecl && !md.isStatic) {
			Reporter.emit("Cannot access non-static member at " + ref.id.posn);
		}
		
		// Check Visibility Status
		if(md.isPrivate && cd != currentClassDecl) {
			Reporter.emit("Cannot access private member at " + ref.id.posn);
		}
		
		ref.decl = md;
		ref.id.decl = md;
		ref.spelling = ref.id.spelling;
		
		return md.type;
	}

	@Override
	public Type visitIndexedRef(IndexedRef ref, IdTable table) {
		
		Type refType = ref.ref.visit(this, table);
		ref.spelling = ref.ref.spelling;
		
		// Must be an array type
		if(refType.typeKind != TypeKind.ARRAY) {
			Reporter.emit("Trying to index " + refType + " at " + ref.ref.posn);
			return refType;
		}
		
		// Index must be an integer value
		Type indexType = ref.indexExpr.visit(this, table);
		if(indexType.typeKind != TypeKind.INT) {
			BaseType type = new BaseType(TypeKind.INT, ref.indexExpr.posn);
			Reporter.emit("Expected " + type + " but got " + indexType + " at " + type.posn);
		}
		
		// Array types can be of either classes or ints
		ArrayType at = (ArrayType) refType;
		if(at.eltType.typeKind == TypeKind.CLASS) {
			ClassType ct = (ClassType) at.eltType;
			ref.decl = table.getDeclaration(ct.className.spelling);
			return ref.decl.type;
		} else {
			ref.decl = null;
			return new BaseType(TypeKind.INT, ref.posn);
		}
	}

	@Override
	public Type visitIdRef(IdRef ref, IdTable table) {
		
		ref.id.visit(this, table);
		ref.decl = ref.id.decl;
		ref.spelling = ref.id.spelling;
		
		return ref.decl.type;
	}

	@Override
	public Type visitThisRef(ThisRef ref, IdTable table) {
		
		ref.spelling = "this";
		ref.decl = currentClassDecl;
		if(currentMethodDecl.isStatic) {
			Reporter.emit("'this' cannot be referenced in a static context at " + ref.posn);
		}
		
		return ref.decl.type;
	}
	
	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// TERMINALS
	//
	// /////////////////////////////////////////////////////////////////////////////
		
	@Override
	public Type visitIdentifier(Identifier id, IdTable table) {
		
		// An variable cannot be used in its declaration
		if(currentVarDecl != null && currentVarDecl.name.equals(id.spelling)) {
			Reporter.emit("Cannot use variable in declaration at " + id.posn);
		}
		
		// Try to find declaration (and abort if not found)
		id.decl = table.getDeclaration(id.spelling);
		if(id.decl == null) {
			Reporter.emit(id.spelling + " undefined at " + id.posn);
			System.exit(Compiler.rc);
		}
		
		// Cannot access instance method from static context
		if(id.decl instanceof MemberDecl) {
			MemberDecl md = (MemberDecl) id.decl;
			if(currentMethodDecl.isStatic && !md.isStatic) {
				Reporter.emit("Cannot access instance member " + md.name + " in a static context at " + id.posn);
			}
		}
		
		return id.decl.type;
		
	}

	@Override
	public Type visitOperator(Operator op, IdTable table) {
		switch(op.spelling) {
			case "!":
			case "&&":
			case "||":
			case ">":
			case "<":
			case "<=":
			case ">=":
			case "==":
			case "!=":
				return new BaseType(TypeKind.BOOLEAN, op.posn);
				
			case "+":
			case "-":
			case "*":
			case "/":
				return new BaseType(TypeKind.INT, op.posn);
				
			default:
				return new BaseType(TypeKind.ERROR, op.posn);
		}
	}

	@Override
	public Type visitIntLiteral(IntLiteral num, IdTable table) {
		
		return new BaseType(TypeKind.INT, num.posn);
	}

	@Override
	public Type visitBooleanLiteral(BooleanLiteral bool, IdTable table) {
		
		return new BaseType(TypeKind.BOOLEAN, bool.posn);
	}

	
	// /////////////////////////////////////////////////////////////////////////////
	//
	// Convenience Functions
	//
	// /////////////////////////////////////////////////////////////////////////////
		
	/**
	 * 
	 * @param cd
	 * @param table
	 */
	private void addDeclarations(ClassDecl cd, IdTable table) {
		
		table.add(cd);
		cd.table = new IdTable(table);
		
		for(FieldDecl fd : cd.fieldDeclList) {
			cd.table.add(fd);
		}
		
		for(MethodDecl md : cd.methodDeclList) {
			cd.table.add(md);
			md.table = new IdTable(cd.table);
		}
	}
	
	/**
	 * 
	 * @param md
	 * @return
	 */
	private boolean isMain(MethodDecl md) {
		
		// Correct Signature
		if(!md.isPrivate && md.isStatic) {
			if(md.type.typeKind == TypeKind.VOID) {
				if(md.name.equals("main") && md.parameterDeclList.size() == 1) {
					
					// Check parameter declaration
					ParameterDecl pd = md.parameterDeclList.get(0);
					if(pd.type.typeKind == TypeKind.ARRAY) {
						ArrayType at = (ArrayType) pd.type;
						if(at.eltType.typeKind == TypeKind.CLASS) {
							ClassType ct = (ClassType) at.eltType;
							
							return ct.className.spelling.equals("String");
						}
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * 
	 * @param t1
	 * @param t2
	 * @return
	 */
	private boolean match(Type t1, Type t2) {
		
		if(t1.typeKind != t2.typeKind) {
			return false;
		}

		// Check Class Types match
		else if(t1.typeKind == TypeKind.CLASS) {
			ClassType c1 = (ClassType) t1;
			ClassType c2 = (ClassType) t2;
			
			return c1.className.spelling.equals(c2.className.spelling);
		}

		// Check Array Types match
		else if(t1.typeKind == TypeKind.ARRAY) {
			ArrayType a1 = (ArrayType) t1;
			ArrayType a2 = (ArrayType) t2;
			
			return match(a1.eltType, a2.eltType);
		}

		return true;
	}
}
